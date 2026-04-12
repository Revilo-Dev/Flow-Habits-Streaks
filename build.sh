#!/bin/bash
# Copyright (C) 2016-2025 Álinson Santos Xavier <isoron@gmail.com>
# This file is part of Loop Habit Tracker.
#
# Loop Habit Tracker is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by the
# Free Software Foundation, either version 3 of the License, or (at your
# option) any later version.
#
# Loop Habit Tracker is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
# or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
# more details.
#
# You should have received a copy of the GNU General Public License along
# with this program. If not, see <http://www.gnu.org/licenses/>.

cd "$(dirname "$0")" || exit

ADB="${ANDROID_HOME}/platform-tools/adb"
ANDROID_OUTPUTS_DIR="uhabits-android/build/outputs"
AVDMANAGER="${ANDROID_HOME}/cmdline-tools/latest/bin/avdmanager"
AVD_PREFIX="uhabitsTest"
EMULATOR="${ANDROID_HOME}/emulator/emulator"
GRADLE="./gradlew --stacktrace --quiet --console=plain"
GRADLE_LOG="build/gradle-output.log"
PACKAGE_NAME=org.isoron.uhabits
SDKMANAGER="${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"
VERSION=$(grep versionName uhabits-android/build.gradle.kts | sed -e 's/.*"\([^"]*\)".*/\1/g')
ATTEMPTS=1
BOOT_TIMEOUT=360
case "$(uname -m)" in
    arm64|aarch64) ARCH="arm64-v8a" ;;
    *)             ARCH="x86_64" ;;
esac

# Logging
# -----------------------------------------------------------------------------

log_error() {
    local COLOR='\033[1;31m'
    local NC='\033[0m'
    echo -e "$COLOR* $1 $NC"
}

log_info() {
    local COLOR='\033[1;32m'
    local NC='\033[0m'
    echo -e "$COLOR* $1 $NC"
}

log_debug() {
    local COLOR='\033[0;90m'
    local NC='\033[0m'
    echo -e "${COLOR}$1 $NC"
}

run() {
    log_debug "$*"
    "$@"
}

fail() {
    log_error "BUILD FAILED"
    exit 1
}

# Validation
# -----------------------------------------------------------------------------

if [ -z $VERSION ]; then
    log_error "Could not parse app version from: uhabits-android/build.gradle.kts"
    exit 1
fi

if [ ! -f "${ANDROID_HOME}/platform-tools/adb" ]; then
    log_error "ANDROID_HOME is not set correctly; ${ANDROID_HOME}/platform-tools/adb not found"
    exit 1
fi

if [ ! -f "$EMULATOR" ]; then
    log_error "Not found: $EMULATOR"
    exit 1
fi

MISSING_DEPS=0
IS_MACOS=0
if [[ "$(uname)" == "Darwin" ]]; then
    IS_MACOS=1
fi

check_cmd() {
    local cmd=$1
    local brew_pkg=$2
    if ! command -v "$cmd" &>/dev/null; then
        if [ $IS_MACOS -eq 1 ] && [ -n "$brew_pkg" ]; then
            log_error "Required command not found: $cmd (try: brew install $brew_pkg)"
        else
            log_error "Required command not found: $cmd"
        fi
        MISSING_DEPS=1
    fi
}

check_cmd flock flock
check_cmd timeout coreutils
check_cmd ts moreutils
check_cmd rsync rsync
check_cmd pgrep ""
check_cmd pkill ""

if [ $MISSING_DEPS -ne 0 ]; then
    exit 1
fi


gradle_run() {
    log_debug "./gradlew $*"
    mkdir -p build
    if ! $GRADLE "$@" > "$GRADLE_LOG" 2>&1; then
        log_error "Gradle command failed: $*"
        grep -E "^e:|^w:|^FAILURE|^> " "$GRADLE_LOG" | head -40
        log_error "Full log: $GRADLE_LOG"
        return 1
    fi
}

# Core
# -----------------------------------------------------------------------------

core_build() {
    log_info "Formatting code..."
    gradle_run ktlintFormat || fail
    log_info "Building uhabits-core..."
    gradle_run kotlinUpgradeYarnLock || fail
    gradle_run :uhabits-core:build || fail
}

# Android
# -----------------------------------------------------------------------------

android_accept_licenses() {
    log_info "Accepting Android SDK licenses..."
    yes | run $SDKMANAGER --licenses
}

android_setup() {
    local API=$1
    local AVDNAME=${AVD_PREFIX}${API}

    (
        flock 10

        log_info "Stopping Android emulator..."
        while [[ -n $(pgrep -f ${AVDNAME}) ]]; do
            pkill -9 -f ${AVDNAME}
        done

        log_info "Removing existing Android virtual device..."
        run $AVDMANAGER delete avd --name $AVDNAME

        log_info "Creating new Android virtual device (API $API)..."
        run $SDKMANAGER --install "system-images;android-$API;google_apis;$ARCH" || return 1
        run $AVDMANAGER create avd \
                --name $AVDNAME \
                --package "system-images;android-$API;google_apis;$ARCH" \
                --device "Nexus 4" || return 1

        flock -u 10
    ) 10>/tmp/uhabitsTest.lock
}

android_launch() {
    local API=$1
    local AVDNAME=${AVD_PREFIX}${API}
    local PORT=6${API}0

    export ADB="${ANDROID_HOME}/platform-tools/adb -s emulator-${PORT}"

    if [ -n "$KILL_EMU" ]; then
        log_info "Stopping Android emulator..."
        while [[ -n $(pgrep -f ${AVDNAME}) ]]; do
            pkill -9 -f ${AVDNAME}
            sleep 1
        done
    fi

    if pgrep -f "${AVDNAME}" > /dev/null; then
        log_info "Emulator already running (API $API), reusing..."
        return 0
    fi

    log_info "Launching emulator (API $API)..."
    local EMULATOR_LOG="build/emulator-${API}.log"
    $EMULATOR \
        -avd $AVDNAME \
        -port $PORT \
        -no-snapshot \
        1>"$EMULATOR_LOG" 2>&1 &

    log_info "Waiting for emulator to boot..."
    timeout $BOOT_TIMEOUT $ADB wait-for-device shell \
        'while [[ -z "$(getprop sys.boot_completed)" ]]; do sleep 1; done; input keyevent 82' &
    local WAIT_PID=$!

    while kill -0 $WAIT_PID 2>/dev/null; do
        if grep -q "FATAL" "$EMULATOR_LOG" 2>/dev/null; then
            log_error "Emulator crashed:"
            grep "FATAL" "$EMULATOR_LOG"
            kill $WAIT_PID 2>/dev/null
            wait $WAIT_PID 2>/dev/null
            return 1
        fi
        sleep 2
    done

    wait $WAIT_PID
    if [ $? -ne 0 ]; then
        log_error "Emulator failed to boot after $BOOT_TIMEOUT seconds."
        return 1
    fi

    log_info "Disabling animations..."
    run $ADB root || return 1
    sleep 5
    run $ADB shell settings put global window_animation_scale 0 || return 1
    run $ADB shell settings put global transition_animation_scale 0 || return 1
    run $ADB shell settings put global animator_duration_scale 0 || return 1

    log_info "Acquiring wake lock..."
    run $ADB shell 'echo android-test > /sys/power/wake_lock' || return 1
}

# shellcheck disable=SC2016
android_test() {
    API=$1
    AVDNAME=${AVD_PREFIX}${API}

    android_launch $API || return 1

    if [ -n "$RELEASE" ]; then
        log_info "Installing release APK..."
        run $ADB install -r ${ANDROID_OUTPUTS_DIR}/apk/release/uhabits-android-release.apk || return 1
    else
        log_info "Installing debug APK..."
        run $ADB install -t -r ${ANDROID_OUTPUTS_DIR}/apk/debug/uhabits-android-debug.apk || return 1
    fi
    log_info "Installing test APK..."
    run $ADB install -r ${ANDROID_OUTPUTS_DIR}/apk/androidTest/debug/uhabits-android-debug-androidTest.apk || return 1

    for size in medium large; do
        OUT_INSTRUMENT=${ANDROID_OUTPUTS_DIR}/instrument-${API}.txt
        OUT_LOGCAT=${ANDROID_OUTPUTS_DIR}/logcat-${API}.txt
        FAILED_TESTS=""
        for ((i=1; i<=ATTEMPTS; i++)); do
            log_info "Running $size instrumented tests (attempt $i)..."
            $ADB shell am instrument \
                -r -e coverage true -e size "$size" $FAILED_TESTS \
                -w ${PACKAGE_NAME}.test/androidx.test.runner.AndroidJUnitRunner \
                | ts "%.s" > "$OUT_INSTRUMENT"

            FAILED_TESTS=$(tools/parseInstrument.py "$OUT_INSTRUMENT")
            SUCCESS=$?
            if [ $SUCCESS -eq 0 ]; then
                log_debug "$size tests passed"
                break
            fi
        done

        if [ $SUCCESS -ne 0 ]; then
            log_error "Some $size instrumented tests failed."
            log_error "Saving logcat: $OUT_LOGCAT..."
            $ADB logcat -d > $OUT_LOGCAT
            log_error "Fetching test screenshots..."
            rm -rf ${ANDROID_OUTPUTS_DIR}/test-screenshots
            run $ADB pull /sdcard/Android/data/${PACKAGE_NAME}/files/test-screenshots ${ANDROID_OUTPUTS_DIR}/
            run $ADB shell rm -r /sdcard/Android/data/${PACKAGE_NAME}/files/test-screenshots/
            return 1
        fi
    done

    return 0
}

android_test_parallel() {
    # Launch background processes
    PIDS=""
    for API in $*; do
        (
            LOG=build/android-test-$API.log
	    mkdir -p build
            log_info "API $API: Running tests..."
            android_test $API 1>$LOG 2>&1
            ret_code=$?
            if [ $ret_code = 0 ]; then
                log_info "API $API: Passed"
            else
                log_error "API $API: Failed"
            fi
            pkill -9 -f ${AVD_PREFIX}${API}
            exit $ret_code
        )&
	PIDS+=" $!"
    done

    # Check exit codes
    success=0
    for pid in $PIDS; do
        wait $pid
        ret_code=$?
        if [ $ret_code != 0 ]; then
            success=1
        fi
    done

    # Print all logs
    for API in $*; do
        echo "::group::Android Tests (API $API)"
        cat build/android-test-$API.log
        echo "::endgroup::"
    done

    return $success
}

android_build() {
    log_info "Building uhabits-android..."

    if [ -n "$RELEASE" ]; then
        log_info "Reading secret..."
        # shellcheck disable=SC1091
        source .secret/env || fail
    fi

    log_info "Removing old APKs..."
    rm -f uhabits-android/build/*.apk

    if [ -n "$RELEASE" ]; then
        log_info "Building release APK..."
        gradle_run updateTranslators
        gradle_run :uhabits-android:assembleRelease
        log_info "Copying release APK..."
        cp  uhabits-android/build/outputs/apk/release/uhabits-android-release.apk \
            uhabits-android/build/loop-"$VERSION"-release.apk
    fi

    log_info "Building debug APK..."
    gradle_run :uhabits-android:assembleDebug || fail
    log_info "Copying debug APK..."
    cp  uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk \
        uhabits-android/build/loop-"$VERSION"-debug.apk

    log_info "Building instrumentation APK..."
    if [ -n "$RELEASE" ]; then
        gradle_run :uhabits-android:assembleAndroidTest  \
            -Pandroid.injected.signing.store.file="$LOOP_KEY_STORE" \
            -Pandroid.injected.signing.store.password="$LOOP_STORE_PASSWORD" \
            -Pandroid.injected.signing.key.alias="$LOOP_KEY_ALIAS" \
            -Pandroid.injected.signing.key.password="$LOOP_KEY_PASSWORD" || fail
    else
        gradle_run assembleAndroidTest || fail
    fi

    return 0
}

android_accept_images() {
    log_info "Accepting test screenshots..."
    run find ${ANDROID_OUTPUTS_DIR}/test-screenshots -name '*.expected*' -delete
    run rsync -av ${ANDROID_OUTPUTS_DIR}/test-screenshots/ uhabits-android/src/androidTest/assets/
}

# General
# -----------------------------------------------------------------------------

_parse_opts() {
    while [ $# -gt 0 ]; do
        case "$1" in
            -r ) RELEASE=1; shift ;;
            -c ) CLEAN=1; shift ;;
            -k ) KILL_EMU=1; shift ;;
            -n ) ATTEMPTS=$2; shift 2 ;;
            * ) shift ;;
        esac
    done
}

_print_usage() {
    cat <<END
CI/CD script for Loop Habit Tracker.

Usage:
    build.sh build [options]
    build.sh android-accept-licenses
    build.sh android-setup <API>
    build.sh android-tests <API> [options]
    build.sh android-tests-parallel <API> <API>... [options]
    build.sh android-accept-images [options]

Commands:
    build                   Build the app and run small tests
    android-accept-licenses Accept all Android SDK licenses
    android-setup           Create Android virtual machine
    android-tests           Run medium and large Android tests on an emulator
    android-tests-parallel  Tests multiple API levels simultaneously
    android-accept-images   Copy fetched images to corresponding assets folder

Options:
    -c      Remove build folders before building
    -k      Kill running emulator before tests (default: reuse if running)
    -n N    Number of test attempts per size (default: 1)
    -r      Build and test release version, instead of debug
END
}

clean() {
    log_info "Cleaning build folders..."
    rm -rf uhabits-android/.gradle
    rm -rf uhabits-android/android-pickers/build
    rm -rf uhabits-android/build
    rm -rf uhabits-android/uhabits-android/build
    rm -rf uhabits-core-legacy/.gradle
    rm -rf uhabits-core-legacy/build
    rm -rf uhabits-core/.gradle
    rm -rf uhabits-core/build
    rm -rf uhabits-server/.gradle
    rm -rf uhabits-server/build
    rm -rf uhabits-web/build
    rm -rf uhabits-web/node_modules
    rm -rf uhabits-web/node_modules/core-js/build
    rm -rf uhabits-web/node_modules/upath/build
    rm -rf .gradle
}

main() {
    case "$1" in
        build)
            shift; _parse_opts "$@"
            if [ -n "$CLEAN" ]; then clean; fi
            core_build
            android_build
            ;;
        android-accept-licenses)
            android_accept_licenses
            ;;
        android-setup)
            shift; _parse_opts "$@"
            android_setup $1
            ;;
        android-tests)
            shift; _parse_opts "$@"
            if [ -z $1 ]; then
                _print_usage
                exit 1
            fi
            android_test $1
            ;;
        android-tests-parallel)
            shift; _parse_opts "$@"
            android_test_parallel $*
            ;;
        android-accept-images)
            android_accept_images
            ;;
        *)
            _print_usage
            exit 1
            ;;
    esac
}

main "$@"

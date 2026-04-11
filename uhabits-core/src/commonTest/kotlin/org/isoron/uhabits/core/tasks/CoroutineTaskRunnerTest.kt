/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.tasks

import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.order
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class CoroutineTaskRunnerTest : BaseUnitTest() {
    private lateinit var runner: CoroutineTaskRunner
    private var task: Task = mock()

    @BeforeTest
    override fun setUp() {
        super.setUp()
        runner = CoroutineTaskRunner(
            mainDispatcher = UnconfinedTestDispatcher(),
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun test() {
        runner.execute(task)
        verifySuspend(order) {
            task.onAttached(runner)
            task.onPreExecute()
            task.doInBackground()
            task.onPostExecute()
        }
    }
}

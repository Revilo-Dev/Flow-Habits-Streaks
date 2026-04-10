package org.isoron.platform

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

actual fun <T> runSuspend(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(r: Result<T>) {
                result = r
            }
        }
    )
    return result?.getOrThrow()
        ?: throw IllegalStateException(
            "runSuspend: coroutine did not complete synchronously"
        )
}

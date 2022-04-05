package eucalypt.utils

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

suspend fun every(interval: Long, block: suspend () -> Unit) = coroutineScope {
    while (isActive) {
        block()
        delay(interval)
    }
}
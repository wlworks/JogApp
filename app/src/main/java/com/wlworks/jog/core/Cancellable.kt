package com.wlworks.jog.core

import kotlin.coroutines.cancellation.CancellationException

/**
 * 和 runCatching 一樣，但不吞 CancellationException —— 否則 scope 被取消時
 * 會被當成一般失敗處理，破壞結構化並行。
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

package app.quran.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Domain-level error. Messages must never contain tokens, identifiers, or other private data. */
public sealed interface AppError {
    public data object Offline : AppError
    public data class Server(val code: Int) : AppError
    public data object InvalidResponse : AppError
    public data object Unknown : AppError
}

public sealed interface AppResult<out T> {
    public data class Success<T>(val value: T) : AppResult<T>
    public data class Failure(val error: AppError) : AppResult<Nothing>
}

public inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

/** Injectable dispatchers so I/O never runs on Main and tests can swap them. */
public interface DispatcherProvider {
    public val main: CoroutineDispatcher
    public val io: CoroutineDispatcher
    public val default: CoroutineDispatcher
}

public object DefaultDispatchers : DispatcherProvider {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val io: CoroutineDispatcher get() = Dispatchers.IO
    override val default: CoroutineDispatcher get() = Dispatchers.Default
}

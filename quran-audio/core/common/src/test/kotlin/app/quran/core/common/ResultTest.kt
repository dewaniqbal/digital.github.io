package app.quran.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResultTest {
    @Test fun `map transforms success`() {
        assertEquals(AppResult.Success(4), AppResult.Success(2).map { it * 2 })
    }

    @Test fun `map passes failure through`() {
        val f = AppResult.Failure(AppError.Offline)
        assertEquals(f, f.map { 1 })
    }
}

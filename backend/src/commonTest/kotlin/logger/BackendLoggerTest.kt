package logger

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackendLoggerTest {

    @Test
    fun `logger is enabled by default`() {
        assertTrue(BackendLogger.enabled)
    }

    @Test
    fun `logger can be disabled`() {
        BackendLogger.enabled = false
        assertFalse(BackendLogger.enabled)
        BackendLogger.enabled = true // restore
    }

    @Test
    fun `all log levels run without throwing when enabled`() {
        BackendLogger.enabled = true
        BackendLogger.d("debug message")
        BackendLogger.i("info message")
        BackendLogger.w("warn message")
        BackendLogger.e("error message")
        BackendLogger.e("error with throwable", RuntimeException("test"))
    }

    @Test
    fun `all log levels run without throwing when disabled`() {
        BackendLogger.enabled = false
        BackendLogger.d("debug message")
        BackendLogger.i("info message")
        BackendLogger.w("warn message")
        BackendLogger.e("error message")
        BackendLogger.e("error with throwable", RuntimeException("test"))
        BackendLogger.enabled = true // restore
    }
}

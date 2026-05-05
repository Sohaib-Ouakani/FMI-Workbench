package logger

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FmuKtLoggerTest {

    @Test
    fun `logger is enabled by default`() {
        assertTrue(FmuKtLogger.enabled)
    }

    @Test
    fun `logger can be disabled`() {
        FmuKtLogger.enabled = false
        assertFalse(FmuKtLogger.enabled)
        FmuKtLogger.enabled = true // restore
    }

    @Test
    fun `all log levels run without throwing when enabled`() {
        FmuKtLogger.enabled = true
        FmuKtLogger.d("debug")
        FmuKtLogger.i("info")
        FmuKtLogger.w("warn")
        FmuKtLogger.e("error")
        FmuKtLogger.e("error with cause", RuntimeException("test"))
    }

    @Test
    fun `all log levels run without throwing when disabled`() {
        FmuKtLogger.enabled = false
        FmuKtLogger.d("debug")
        FmuKtLogger.i("info")
        FmuKtLogger.w("warn")
        FmuKtLogger.e("error")
        FmuKtLogger.e("error with cause", RuntimeException("test"))
        FmuKtLogger.enabled = true // restore
    }
}

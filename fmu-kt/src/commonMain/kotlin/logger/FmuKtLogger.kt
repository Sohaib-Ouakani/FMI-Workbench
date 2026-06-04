package logger

object FmuKtLogger {
    var enabled = true   // toggle off for release builds

    fun d(msg: String) = if (enabled) println("FMU-KT -- DEBUG: $msg") else Unit
    fun i(msg: String) = if (enabled) println("FMU-KT -- INFO: $msg") else Unit
    fun w(msg: String) = if (enabled) println("FMU-KT -- WARN: $msg") else Unit
    fun e(msg: String, th: Throwable? = null) {
        if (enabled) {
            println("FMU-KT -- ERROR: $msg${th?.let { " | $it" } ?: ""}")
            th?.printStackTrace()
        }
    }
}

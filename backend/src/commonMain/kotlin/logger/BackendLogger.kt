package logger

object BackendLogger {
    var enabled = true   // toggle off for release builds

    fun d(msg: String) = if (enabled) println("BACKEND -- DEBUG: $msg") else Unit
    fun i(msg: String) = if (enabled) println("BACKEND -- INFO: $msg") else Unit
    fun w(msg: String) = if (enabled) println("BACKEND -- WARN: $msg") else Unit
    fun e(msg: String, th: Throwable? = null) {
        if (enabled) {
            println("BACKEND -- ERROR: $msg${th?.let { " | $it" } ?: ""}")
            th?.printStackTrace()
        }
    }
}

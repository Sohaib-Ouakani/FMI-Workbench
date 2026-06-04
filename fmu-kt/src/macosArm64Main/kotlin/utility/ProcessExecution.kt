package utility

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen
import platform.posix.system
/** Buffer size for reading command output, defined as an integer. */
const val BUFFER_SIZE = 4096

/**
 * Executes system processes and commands.
 * Provides methods to run commands with and without capturing output.
 */
class ProcessExecution {
    /**
     * Executes a system command with the given arguments.
     *
     * @param args The command and arguments to execute, where the first element
     *             should be the command name and subsequent elements should be the arguments.
     * @return The exit status code of the executed command. Returns 0 on success.
     */
    fun run (vararg args: String): Int {
        val cmd = args.joinToString(" ") { if (it.contains(" ")) "\"$it\"" else it }
        return system(cmd)
    }

    /**
     * Executes a system command and captures its standard output.
     *
     * @param args The command and arguments to execute, where the first element
     *              should be the command name and subsequent elements should be the arguments.
     * @return The standard output from the command, trimmed of leading/trailing whitespace.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun runWithOutput(vararg args: String): String {
        val cmd = args.joinToString(" ") { if (it.contains(" ")) "\"$it\"" else it }
        val sb: StringBuilder = StringBuilder()
        val pipe = popen(cmd, "r")
        memScoped {
            val buff = allocArray<ByteVar>(BUFFER_SIZE)
            while (fgets(buff, BUFFER_SIZE, pipe) != null) {
                sb.append(buff.toKString())
            }
        }
        pclose(pipe)
        return sb.toString().trim()
    }
}

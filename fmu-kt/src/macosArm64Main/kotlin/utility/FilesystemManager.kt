package utility

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.F_OK
import platform.posix.access
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.getcwd
/** Buffer size for file operations, defined as an unsigned long. */
const val UNSGN_BUFFER_SIZE = 4096uL

/**
 * Manages file system operations including reading, writing, and checking file existence.
 * Provides utilities for path manipulation and absolute path resolution.
 */
class FilesystemManager {

    /**
     * Reads the complete contents of a file as a string.
     *
     * @param fileName The path to the file to read.
     * @return The file contents as a trimmed string, or an empty string if the file cannot be opened.
     * @throws IllegalStateException if the file does not exist.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun readFile(fileName: String): String {
        check(fileExists(fileName)) { "File does not exist: $fileName" }
        val file = fopen(fileName, "r") ?: return ""
        val sb = StringBuilder()

        memScoped {
            val buff = allocArray<ByteVar>(BUFFER_SIZE)
            while (fgets(buff, BUFFER_SIZE, file) != null) {
                sb.append(buff.toKString())
            }
        }
        fclose(file)

        return sb.toString().trim()
    }

    /**
     * Writes content to a file, overwriting it if it already exists.
     *
     * @param fileName The path to the file to write to.
     * @param content The content to write to the file.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun writeFile(fileName: String, content: String) {
        val file = fopen(fileName, "w") ?: return
        fputs(content, file)
        fclose(file)
    }

    /**
     * Converts a file path to an absolute path.
     * If the path is already absolute (starts with "/"), it is returned unchanged.
     *
     * @param path The file path to convert.
     * @return The absolute path as a string.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun pathAbsolute(path: String): String {
        if (path.startsWith("/")) return path
        return memScoped {
            val buf = allocArray<ByteVar>(BUFFER_SIZE)
            val cwd = getcwd(buf, UNSGN_BUFFER_SIZE)?.toKString() ?: "."
            "$cwd/$path"
        }
    }

    /**
     * Checks whether a file or directory exists at the given path.
     *
     * @param name The file or directory path to check.
     * @return True if the file or directory exists, false otherwise.
     */
    fun fileExists(name: String): Boolean {
        return access(name, F_OK) == 0
    }
}

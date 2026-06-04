package resources.manager

import fmu.FmuPaths
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem


class DefaultResourceManager(arg: String?): ResourceManagerService {
    private val baseDir: Path = arg
        ?.let { Path(it).parent }          // executable path → its parent dir
        ?: SystemFileSystem.resolve(Path("."))  // fallback to CWD
    private val resourceDir: Path = Path("$baseDir/resources/")
    private val uploadDir = Path("$resourceDir/models/")
    private val extractedDirPath = Path("$resourceDir/extracted")
    private var fmuPath: Path? = null

    init {
        SystemFileSystem.createDirectories(resourceDir)
        SystemFileSystem.createDirectories(uploadDir)
        SystemFileSystem.createDirectories(extractedDirPath)
    }

    private fun deleteRecursively(path: Path) {
        if (SystemFileSystem.metadataOrNull(path)?.isDirectory == true) {
            SystemFileSystem.list(path).forEach { deleteRecursively(it) }
        }
        SystemFileSystem.delete(path)
    }

    private fun resetUploadDirectory(): Unit =
        SystemFileSystem.list(uploadDir)
            .filter { it.name.endsWith(".fmu", ignoreCase = true) }
            .forEach { SystemFileSystem.delete(it) }

    private fun resetExtractedDirectory(): Unit =
        SystemFileSystem.list(extractedDirPath)
            .filter {
                SystemFileSystem.metadataOrNull(it)?.isDirectory == true
                    || it.name.endsWith(".xml", ignoreCase = true)
            }
            .forEach { deleteRecursively(it) }

    private fun resetResourcesDirectory() {
        resetExtractedDirectory()
        resetUploadDirectory()
    }

    override fun cleanup() {
        deleteRecursively(resourceDir)
    }

    override fun fmuPaths(): FmuPaths {
        val path = fmuPath ?: throw NoSuchElementException("No FMU uploaded yet")

        return FmuPaths(
            fmuPath = path.toString(),
            extractedDir = extractedDirPath.toString(),
            modelsDir = uploadDir.toString()
        )
    }

    override fun saveUpload(fileName: String, data: ByteArray) {
        resetResourcesDirectory()
        val filePath = Path(uploadDir, fileName)

        SystemFileSystem.sink(filePath).buffered().use { it.write(data) }
        fmuPath = filePath
    }
}

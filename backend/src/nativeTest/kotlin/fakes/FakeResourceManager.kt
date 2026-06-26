package fakes

import fmu.FmuPaths
import resources.manager.ResourceManagerService

class FakeResourceManager : ResourceManagerService {

    var lastSavedFileName: String? = null
    var lastSavedData: ByteArray? = null
    var cleanupCalled = false
    var throwOnFmuPaths: Exception? = null
    var fmuPathsToReturn: FmuPaths = FmuPaths(
        fmuPath = "/fake/path/model.fmu",
        extractedDir = "/fake/path/extracted",
        modelsDir = "/fake/path/models",
    )

    override fun fmuPaths(): FmuPaths {
        throwOnFmuPaths?.let { throw it }
        return fmuPathsToReturn
    }

    override fun saveUpload(fileName: String, data: ByteArray) {
        lastSavedFileName = fileName
        lastSavedData = data
    }

    override fun cleanup() {
        cleanupCalled = true
    }

    fun reset() {
        lastSavedFileName = null
        lastSavedData = null
        cleanupCalled = false
        throwOnFmuPaths = null
    }
}

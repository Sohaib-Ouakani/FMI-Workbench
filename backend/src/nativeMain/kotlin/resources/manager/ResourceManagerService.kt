package resources.manager

import fmu.FmuPaths

interface ResourceManagerService {
    fun fmuPaths(): FmuPaths
    fun saveUpload(fileName: String, data: ByteArray)
    fun cleanup()
}

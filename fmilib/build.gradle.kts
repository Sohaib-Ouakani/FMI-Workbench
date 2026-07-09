import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    alias(libs.plugins.gradle.cmake)
    `lifecycle-base`
}

cmake {
    targets {
        create("fmilib") {
            cmakeLists = file("lib/CMakeLists.txt")
            targetMachines.add(machines.host)
        }
    }
}


val fmilibBuildDir = layout.buildDirectory.dir("cmake/fmilib").get().asFile
val fmilibInstallDir = layout.buildDirectory.dir("fmilib-install").get().asFile

val cmakeInstall by tasks.registering {
    dependsOn("cmakeBuild")
    outputs.dir(fmilibInstallDir)
    val execOps = project.serviceOf<ExecOperations>()
    doLast {
        val platformDirs = fmilibBuildDir.listFiles()?.filter { it.isDirectory }
            ?: error("cmake build dir not found in $fmilibBuildDir")
        platformDirs.forEach { platformDir ->
            val platformInstallDir = fmilibInstallDir.resolve(platformDir.name)
            execOps.exec {
                commandLine(
                    "cmake", "--install", platformDir.absolutePath,
                    "--prefix", platformInstallDir.absolutePath
                )
            }
        }
    }
}

val cmakeInstallClean by tasks.registering(Delete::class) {
    doLast {
        delete(fmilibInstallDir)
    }
}

tasks.clean.configure {
    dependsOn(cmakeInstallClean)
}

tasks.build.configure {
    dependsOn(cmakeInstall)
}

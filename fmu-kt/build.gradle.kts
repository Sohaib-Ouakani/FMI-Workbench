import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

group = "org.findaname"

repositories {
    google()
    mavenCentral()
}

val cLibrary by configurations.creating
val fmilib = project(":fmilib")

dependencies {
    cLibrary(fmilib)
}
val platformDirName = mapOf(
    "macosArm64" to "mac-aarch64",
    "linuxX64" to "linux-amd64",
    "mingwX64" to "windows-amd64"
)

val fmilibInstallDir = project(":fmilib").layout.buildDirectory.dir("fmilib-install").get().asFile

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
    val nativeSetup: KotlinNativeTarget.() -> Unit = {
        val platformDir = fmilibInstallDir
            .resolve(platformDirName[targetName] ?: error("Platform $targetName sconosciuta"))
        val includeDir = platformDir.resolve("include")
        val libDir = platformDir.resolve("lib")

        compilations["main"].cinterops {
            val libfmi by creating {
                headers = files(includeDir.resolve("fmilib.h"))
                compilerOpts("-I${includeDir}")
            }
        }
        binaries {
            all {
                if (targetName == "linuxX64") {
                    linkerOpts(
                        "-L${libDir}",
                        "-lfmilib_shared",
                        "-Wl,-rpath,${libDir}",
                        "-L/usr/lib/x86_64-linux-gnu",
                        "-Wl,--allow-shlib-undefined",
                        "-Wl,--unresolved-symbols=ignore-all",
                        "-Wl,--warn-unresolved-symbols"
                    )
                } else {
                    linkerOpts(
                        "-L${libDir}",
                        "-lfmilib_shared",
                        "-Wl,-rpath,${libDir}"
                    )
                }
            }
            staticLib()
        }
    }

    applyDefaultHierarchyTemplate()

    val os = OperatingSystem.current()
    when {
        os.isMacOsX -> macosArm64(nativeSetup)
        os.isLinux -> linuxX64(nativeSetup)
        os.isWindows -> mingwX64(nativeSetup)
        else -> error("Unsupported OS: $os")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinxSerializationJson)

                implementation(libs.ksoup.core)
                implementation(libs.ksoup.kotlinxIo)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.framework.engine)
            }
        }
    }

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    tasks.withType<KotlinNativeTest>().configureEach {
        testLogging {
            showStandardStreams = true
        }
    }
}

// Funzione che restituisce sia lib (per le .dll.a) che bin (per le .dll)
fun getNativeBinDirs(targetName: String): List<File> {
    val platformDir = fmilibInstallDir.resolve(platformDirName[targetName] ?: error("Unknown target $targetName"))
    return listOf(platformDir.resolve("lib"), platformDir.resolve("bin"))
}

tasks.withType<KotlinNativeTest>().configureEach {
    if (name == "mingwX64Test") {
        val binDirs = getNativeBinDirs("mingwX64")
        doFirst {
            val currentPath = environment["PATH"] ?: ""
            val newPath = binDirs.joinToString(File.pathSeparator) { it.absolutePath } +
                File.pathSeparator + currentPath
            environment("PATH", newPath)
            logger.lifecycle("PATH per mingwX64Test: $newPath")
        }
    }
}

tasks.matching { it.name.startsWith("cinterop") }.configureEach {
    dependsOn(fmilib.tasks.build)
}

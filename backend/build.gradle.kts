import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
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

val platformDirName = mapOf(
    "macosArm64"  to "mac-aarch64",
    "linuxX64"    to "linux-amd64",
    "mingwX64"    to "windows-amd64"
)
val fmilibInstallDir = project(":fmilib").layout.buildDirectory.dir("fmilib-install").get().asFile

kotlin {
    val nativeSetup: KotlinNativeTarget.() -> Unit = {
        val platformDir = fmilibInstallDir
            .resolve(platformDirName[targetName] ?: error("Platform $targetName sconosciuta"))
        val libDir = platformDir.resolve("lib")

        binaries {
            executable {
                entryPoint = "main"
                runTaskProvider?.configure {
                    args(projectDir.absolutePath)
                }
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
        }
    }

    applyDefaultHierarchyTemplate()

    val os = OperatingSystem.current()
    when {
        os.isMacOsX  -> macosArm64(nativeSetup)
        os.isLinux   -> linuxX64(nativeSetup)
        os.isWindows -> mingwX64(nativeSetup)
        else -> error("Unsupported OS: $os")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":fmu-kt"))
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.host.common)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.server.cors)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val nativeTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.ktor.client.cio) // for the test HTTP client
            }
        }
    }

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    allWarningsAsErrors = true
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}

// in backend/build.gradle.kts
val copyFmilibDllForWindows by tasks.registering(Copy::class) {
    val binDir = fmilibInstallDir.resolve("windows-amd64/bin")
    from(binDir) {
        include("*.dll")
    }
    into(layout.buildDirectory.dir("bin/mingwX64/releaseExecutable"))
    dependsOn(project(":fmilib").tasks.named("build"))
}

tasks.matching { it.name == "linkReleaseExecutableMingwX64" }.configureEach {
    finalizedBy(copyFmilibDllForWindows)
}

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.travelingtunes.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.travelingtunes.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            if (output != null) {
                output.outputFileName = "traveling-tunes-${output.outputFileName}"
            }
        }

        val variantNameCap = variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val copyApkTask = tasks.register("copyApkFor$variantNameCap") {
            dependsOn(variant.packageApplicationProvider)
            outputs.upToDateWhen { false }
            doLast {
                val destDir = file("/Users/buck/Library/CloudStorage/GoogleDrive-r.buck.bailey@gmail.com/My Drive/APK")
                destDir.mkdirs()

                val apkFiles = mutableListOf<File>()
                variant.outputs.forEach { output ->
                    val file = output.outputFile
                    if (file != null && file.exists()) {
                        apkFiles.add(file)
                    }
                }

                val apkDir = File(project.rootDir, "app/build/outputs/apk/${variant.name}")
                if (apkDir.exists()) {
                    apkDir.walkTopDown().filter { it.isFile && it.extension == "apk" }.forEach { file ->
                        if (!apkFiles.contains(file)) {
                            apkFiles.add(file)
                        }
                    }
                }

                logger.quiet("DEBUG DEST_DIR=${destDir.canonicalPath}")
                logger.quiet("DEBUG APK_FILES=${apkFiles.map { it.absolutePath }}")

                apkFiles.forEach { file ->
                    val destFile = File(destDir, file.name)
                    try {
                        if (destFile.exists()) {
                            destFile.delete()
                        }
                        Files.copy(
                            file.toPath(),
                            destFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                        logger.quiet("COPIED APK: ${file.name} -> ${destFile.absolutePath}")
                    } catch (e: Exception) {
                        logger.error("Failed to copy APK ${file.name} to ${destFile.absolutePath}: ${e.message}")
                    }
                }
            }
        }

        variant.assembleProvider.configure {
            dependsOn(copyApkTask)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.palette.ktx)

    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)

    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.11.0")
    debugImplementation(libs.androidx.ui.tooling)
}

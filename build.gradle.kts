@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.util.*

repositories {
    google()
    mavenCentral()
}

plugins {
    id("maven-publish")
    id("signing")
//    id("com.android.library") version "7.2.1"
    kotlin("multiplatform") version "1.7.20-Beta"
}

group = "io.maryk.rocksdb"
version = "7.3.1-1"

val rocksDBJVMVersion = "7.3.1"
val rocksDBAndroidVersion = "7.3.1"

val kotlinNativeDataPath = System.getenv("KONAN_DATA_DIR")?.let { File(it) }
    ?: File(System.getProperty("user.home")).resolve(".konan")

val objectiveRocksHome = "./xcodeBuild/Build/Products/Release"

val buildMacOS by tasks.creating(Exec::class) {
    workingDir = projectDir
    commandLine("./buildObjectiveRocksMac.sh")
}

val buildIOS by tasks.creating(Exec::class) {
    workingDir = projectDir
    commandLine("./buildObjectiveRocksiOS.sh", "iphoneos")
}

val buildIOSSimulator by tasks.creating(Exec::class) {
    workingDir = projectDir
    commandLine("./buildObjectiveRocksiOS.sh", "iphonesimulator")
}
//
//android {
//    buildToolsVersion = "32.0.0"
//    compileSdk = 32
//    defaultConfig {
//        minSdk = 21
//        targetSdk = 32
//        multiDexEnabled = true
//        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
//    }
//}

kotlin {
    fun KotlinTarget.setupJvmTarget() {
        compilations.all {
            kotlinOptions {
                allWarningsAsErrors = true
                if (this is KotlinJvmOptions) {
                    jvmTarget = "1.8"
                }
            }
        }
    }
//    jvm {
//        setupJvmTarget()
//    }
//    android {
//        setupJvmTarget()
//        publishAllLibraryVariants()
//        publishLibraryVariantsGroupedByFlavor = true
//    }

    sourceSets {
        all {
            languageSettings.apply {
                languageVersion = "1.7"
                apiVersion = "1.7"
                progressiveMode = true
            }
        }
        val commonMain by getting {}
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
//        val jvmMain by getting {
//            dependencies {
//                api("org.rocksdb:rocksdbjni:$rocksDBJVMVersion")
//            }
//        }
//        val jvmTest by getting {
//            dependencies {
//                implementation(kotlin("test"))
//                implementation(kotlin("test-junit"))
//            }
//        }
//        val androidMain by getting {
//            dependencies {
//                implementation(kotlin("stdlib"))
//                api("io.maryk.rocksdb:rocksdb-android:$rocksDBAndroidVersion")
//            }
//        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val nativeTest by creating {
            dependsOn(commonTest)
        }
        val darwinMain by creating {
            kotlin.apply {
                srcDir("src/appleMain/kotlin")
            }
            dependsOn(nativeMain)
        }
        val darwinTest by creating {
            kotlin.apply {
                srcDir("src/appleTest/kotlin")
            }
            dependsOn(nativeTest)
        }
    }

    fun KotlinNativeTarget.setupAppleTarget(definitionName: String, buildTask: Exec, folderExtension: String = "") {
        binaries {
            getTest("DEBUG").linkerOpts = mutableListOf(
                "-L$objectiveRocksHome${folderExtension}", "-lobjectiveRocks-$definitionName"
            )
        }

        compilations["main"].apply {
            defaultSourceSet {
                val darwinMain by sourceSets.getting {}
                dependsOn(darwinMain)
            }
            cinterops {
                this.create("rocksdb") {
                    defFile = project.projectDir.resolve("src/nativeInterop/cinterop/rocksdb${definitionName.capitalize()}$folderExtension.def")
                    tasks[interopProcessingTaskName].dependsOn(buildTask)
                    includeDirs("./xcodeBuild/Build/Products/Release/usr/local/include")
                }
            }
        }

        compilations["test"].apply {
            defaultSourceSet {
                val darwinTest by sourceSets.getting {}
                dependsOn(darwinTest)
            }
        }
    }

//    ios {
//        if (this.name == "iosX64") {
//            setupAppleTarget("iOS", buildIOSSimulator, "-iphonesimulator")
//        } else {
//            setupAppleTarget("iOS", buildIOS, "-iphoneos")
//        }
//    }
//
//    macosX64 {
//        setupAppleTarget("macOS", buildMacOS)
//    }
//
    macosArm64 {
        setupAppleTarget("macOS", buildMacOS)
    }
}

// Creates the folders for the database
val createOrEraseDBFolders = task("createOrEraseDBFolders") {
    group = "verification"

    val subdir = File(project.buildDir, "test-database")

    if (!subdir.exists()) {
        subdir.deleteOnExit()
        subdir.mkdirs()
    } else {
        subdir.deleteRecursively()
        subdir.mkdirs()
    }
}

tasks.getByName("clean", Delete::class) {
    delete("xcodeBuild")
}

tasks.withType<Test> {
    this.dependsOn(createOrEraseDBFolders)
    this.doLast {
        File(project.buildDir, "test-database").deleteRecursively()
    }
}

// Stub secrets to let the project sync and build without the publication values set up
ext["signing.keyId"] = null
ext["signing.password"] = null
ext["signing.secretKeyRingFile"] = null
ext["ossrhUsername"] = null
ext["ossrhPassword"] = null

// Grabbing secrets from local.properties file or from environment variables, which could be used on CI
val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use {
        Properties().apply {
            load(it)
        }
    }.onEach { (name, value) ->
        ext[name.toString()] = value
    }
} else {
    ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
    ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
    ext["signing.secretKeyRingFile"] = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
}

fun getExtraString(name: String) = ext[name]?.toString()

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "sonatype"
                setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = getExtraString("ossrhUsername")
                    password = getExtraString("ossrhPassword")
                }
            }
        }

        publications.withType<MavenPublication> {
            artifact(emptyJavadocJar.get())

            pom {
                name.set(project.name)
                description.set("Kotlin multiplatform RocksDB interface")
                url.set("https://github.com/marykdb/rocksdb-multiplatform")

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("jurmous")
                        name.set("Jurriaan Mous")
                    }
                }
                scm {
                    url.set("https://github.com/marykdb/rocksdb-multiplatform")
                }
            }
        }
    }

    signing {
        sign(publishing.publications)
    }
}

tasks {
    "wrapper"(Wrapper::class) {
        distributionType = Wrapper.DistributionType.ALL
    }
}

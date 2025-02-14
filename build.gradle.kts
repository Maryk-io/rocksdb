@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.util.*

repositories {
    google()
    mavenCentral()
}

plugins {
    id("maven-publish")
    id("signing")
    id("com.android.library") version "8.7.0"
    kotlin("multiplatform") version "2.1.10"
}

group = "io.maryk.rocksdb"
version = "9.10.0"

val rocksDBJVMVersion = "9.10.0"
val rocksDBAndroidVersion = "9.10.0"

val kotlinXDateTimeVersion = "0.6.1"

val rocksdbBuildPath = "./rocksdb/build"

android {
    namespace = "io.maryk.rocksdb"
    buildToolsVersion = "34.0.0"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        multiDexEnabled = true
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
}

kotlin {
    jvmToolchain(17)

    // configure all Kotlin/JVM Tests to use JUnit Jupiter
    targets.withType<KotlinJvmTarget>().configureEach {
        testRuns.configureEach {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
    jvm()
    androidTarget {
        publishAllLibraryVariants()
        publishLibraryVariantsGroupedByFlavor = true
    }

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.get().compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                languageVersion = "2.1"
                apiVersion = "2.1"
                progressiveMode = true
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.cinterop.BetaInteropApi")
            }
        }
        commonMain {}
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinXDateTimeVersion")
            }
        }
        jvmMain {
            dependencies {
                api("org.rocksdb:rocksdbjni:$rocksDBJVMVersion")
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        androidMain {
            dependencies {
                api("io.maryk.rocksdb:rocksdb-android:$rocksDBAndroidVersion")
            }
        }
        androidUnitTest {
            kotlin.srcDir("src/jvmTest/kotlin")
        }
    }

    val isMacOs = System.getProperty("os.name").contains("Mac", ignoreCase = true)

    fun KotlinNativeTarget.setupTarget(buildName: String, buildTask: Exec, extraCFlags: String = "", extraCmakeFlags: String = "") {
        binaries {
            executable {
                freeCompilerArgs += listOf("-g")
            }
            getTest("DEBUG").linkerOpts = mutableListOf(
                "-L$rocksdbBuildPath/${buildName}"
            )
        }

        val dependencyTask = tasks.create("buildDependencies_$buildName", Exec::class) {
            workingDir = projectDir
            val options = buildList<String> {
                if (buildName.startsWith("linux") && isMacOs) {
                    addAll(listOf("docker", "run", "--rm", "--platform"))
                    if (extraCFlags == "-march=x86-64") {
                        add("linux/amd64")
                    } else {
                        add("linux/arm64/v8")
                    }
                    addAll(listOf("-v", "${project.projectDir}:/rocks", "-w", "/rocks", "buildpack-deps"))
                }
                addAll(listOf("./buildDependencies.sh", "--extra-cflags", extraCFlags, "--output-dir", "rocksdb/build/$buildName", "--extra-cmakeflags", extraCmakeFlags))
            }
            commandLine(*options.toTypedArray())
        }

        compilations["main"].apply {
            cinterops {
                create("rocksdb") {
                    defFile("src/nativeInterop/cinterop/rocksdb.def")
                    includeDirs("rocksdb/include/rocksdb")
                    tasks[interopProcessingTaskName].dependsOn(buildTask)
                    tasks[interopProcessingTaskName].dependsOn(dependencyTask)
                }
            }
        }
    }

    iosArm64 {
        val sdkPathProvider = providers.exec {
            commandLine("xcrun", "--sdk", "iphoneos", "--show-sdk-path")
        }.standardOutput.asText
        val sdkPath: String by lazy {
            if (isMacOs) {
                sdkPathProvider.get().trim()
            } else ""
        }
        val buildTask = tasks.create("buildLib-"+this.name, Exec::class) {
            workingDir = projectDir
            commandLine("./buildRocksdbApple.sh", "--platform=ios", "--arch=arm64")
        }
        setupTarget("ios_arm64", buildTask, "-arch arm64 -target arm64-apple-ios13.0 -isysroot $sdkPath", "-DPLATFORM=OS64")
    }
    iosSimulatorArm64 {
        val sdkPathProvider = providers.exec {
            commandLine("xcrun", "--sdk", "iphonesimulator", "--show-sdk-path")
        }.standardOutput.asText
        val sdkPath: String by lazy {
            if (isMacOs) {
                sdkPathProvider.get().trim()
            } else ""
        }
        val buildTask = tasks.create("buildLib-"+this.name, Exec::class) {
            workingDir = projectDir
            commandLine("./buildRocksdbApple.sh", "--platform=ios", "--simulator", "--arch=arm64")
        }
        setupTarget("ios_simulator_arm64", buildTask, "-arch arm64 -target arm64-apple-ios13.0-simulator -isysroot $sdkPath", "-DPLATFORM=SIMULATORARM64")
    }
    macosX64 {
        val buildTask = tasks.create("buildLib-"+this.name, Exec::class) {
            workingDir = projectDir
            commandLine("./buildRocksdbApple.sh", "--platform=macos", "--arch=x86_64")
        }
        setupTarget("macos_x86_64", buildTask, "-arch x86_64 -target x86_64-apple-macos11.0", "-DPLATFORM=OS64")
    }
    macosArm64 {
        val buildTask = tasks.create("buildLib-"+this.name, Exec::class) {
            workingDir = projectDir
            commandLine("./buildRocksdbApple.sh", "--platform=macos", "--arch=arm64")
        }
        setupTarget("macos_arm64", buildTask, "-arch arm64 -target arm64-apple-macos11.0", "-DPLATFORM=MAC")
    }
    linuxX64 {
        val buildTask = tasks.create("buildLib-"+this.name, Exec::class) {
            workingDir = projectDir
            commandLine("./buildRocksdbLinux.sh", "--arch=x86-64")
        }
        setupTarget("linux_x86_64", buildTask, "-march=x86-64")
    }
    linuxArm64 {
        val buildTask = tasks.create("buildLib-"+this.name, Exec::class) {
            workingDir = projectDir
            commandLine("./buildRocksdbLinux.sh", "--arch=arm64")
        }
        setupTarget("linux_arm64", buildTask, "-march=armv8-a")
    }
}

// Creates the folders for the database
val createOrEraseDBFolders = task("createOrEraseDBFolders") {
    group = "verification"

    val subdir = project.layout.buildDirectory.dir("test-database").get().asFile

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
        project.layout.buildDirectory.dir("test-database").get().asFile.deleteRecursively()
    }
}

kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    binaries.withType<TestExecutable>().all {
        tasks.findByName("${this.target.name}Test")?.apply {
            dependsOn(createOrEraseDBFolders)
            doLast {
                project.layout.buildDirectory.dir("test-database").get().asFile.deleteRecursively()
            }
        }
    }
}

// Placeholder secrets to allow project sync and build without publication setup
ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
ext["signing.secretKeyRingFile"] = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")

// Load secrets from local.properties if available, otherwise fallback to environment variables (useful for CI/CD)
val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use { reader ->
        Properties().apply {
            load(reader)
            forEach { name, value ->
                ext[name.toString()] = value.toString()
            }
        }
    }
}

fun getExtraString(name: String): String? = ext[name]?.toString()

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "sonatype"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = getExtraString("ossrhUsername")
                    password = getExtraString("ossrhPassword")
                }
            }
        }

        publications.withType<MavenPublication> {
            if (this.name == "jvm") {
                artifact(emptyJavadocJar.get())
            }
            pom {
                name = project.name
                description = "Kotlin multiplatform RocksDB interface"
                url = "https://github.com/marykdb/rocksdb-multiplatform"

                licenses {
                    license {
                        name = "The Apache Software License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "jurmous"
                        name = "Jurriaan Mous"
                    }
                }
                scm {
                    url = "https://github.com/marykdb/rocksdb-multiplatform"
                }
            }
        }
    }

    signing {
        println(publishing.publications)
        sign(publishing.publications)
    }
}

tasks {
    "wrapper"(Wrapper::class) {
        distributionType = Wrapper.DistributionType.ALL
    }
}

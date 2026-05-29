import org.gradle.api.GradleException
import org.gradle.api.file.DuplicatesStrategy
import java.util.Properties
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

val bundleAllPayload = providers
    .gradleProperty("deepmule.bundleAllPayload")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)

val secretsProperties = Properties().apply {
    val file = rootProject.file("secrets.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun resolveSecret(name: String): String? {
    return providers.gradleProperty(name).orNull
        ?: System.getenv(name)
        ?: secretsProperties.getProperty(name)
}

val releaseStoreFilePath = resolveSecret("DEEPMULE_RELEASE_STORE_FILE")
val releaseStorePassword = resolveSecret("DEEPMULE_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = resolveSecret("DEEPMULE_RELEASE_KEY_ALIAS")
val releaseKeyPassword = resolveSecret("DEEPMULE_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

val fullPayloadAssetsDir = layout.buildDirectory.dir("generated/assets/fullPayload/main")
val payloadOptimizationReportFile = layout.buildDirectory.file("reports/payload-optimization-summary.txt")
val payloadSourceDirs = mapOf(
    "bios" to rootProject.layout.projectDirectory.dir("bios-pack"),
    "roms" to rootProject.layout.projectDirectory.dir("Roms"),
    "cores" to rootProject.layout.projectDirectory.dir("cores-pack"),
    "retroarch" to rootProject.layout.projectDirectory.dir("retroarch-pack")
)

val bundledCoreNames = linkedSetOf(
    "stella",
    "gambatte",
    "mgba",
    "fceumm",
    "snes9x",
    "mupen64plus_next",
    "genesis_plus_gx",
    "fbneo"
)

val bundledCoreInfoNames = bundledCoreNames.map { "${it}_libretro.info" }.toSet()

fun File.totalBytes(): Long =
    if (!exists()) 0L
    else if (isFile) length()
    else walkTopDown()
        .filter { it.isFile }
        .sumOf { it.length() }

fun File.fileCount(): Int =
    if (!exists()) 0
    else if (isFile) 1
    else walkTopDown().count { it.isFile }

fun formatMb(bytes: Long): String =
    String.format("%.2f", bytes / (1024.0 * 1024.0))

fun File.ensureEmptyDirectory() {
    if (exists()) {
        deleteRecursively()
    }
    mkdirs()
}

fun sha1(file: File): String {
    val digest = MessageDigest.getInstance("SHA-1")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun stripKnownExtensions(name: String): String {
    var result = name
    val suffixRegex = Regex(
        """\.(zip|7z|rar|gz|bz2|smc|sfc|gba|gb|gbc|nes|z64|v64|n64|sms|gg|md|gen|smd|a26|bin|rom|cue|iso|chd|pbp)$""",
        RegexOption.IGNORE_CASE
    )
    while (true) {
        val stripped = result.replace(suffixRegex, "")
        if (stripped == result) {
            return result
        }
        result = stripped
    }
}

fun normalizedRomKey(fileName: String): String {
    val noExtensions = stripKnownExtensions(fileName)
    val withoutTags = noExtensions.replace(Regex("""[\[\(\{][^\]\)\}]*[\]\)\}]"""), " ")
    val normalized = withoutTags
        .replace("&", " and ")
        .replace(Regex("""[^a-z0-9]+""", RegexOption.IGNORE_CASE), " ")
        .trim()
        .replace(Regex("""\s+"""), "")
    return normalized.ifBlank { fileName.lowercase() }
}

fun isUnusedRomSidecar(file: File): Boolean {
    val name = file.name.lowercase()
    val ext = file.extension.lowercase()
    val unusedExtensions = setOf(
        "txt", "nfo", "xml", "json", "md", "pdf",
        "jpg", "jpeg", "png", "gif", "bmp", "webp",
        "db", "ini", "sfv", "md5"
    )
    if (ext in unusedExtensions) {
        return true
    }
    if (
        name.contains(".srm.") ||
        name.contains(".sav.") ||
        name.contains(".state.") ||
        name.contains(".rtc.")
    ) {
        return true
    }
    return false
}

fun regionRank(name: String): Int {
    val lower = name.lowercase()
    return when {
        lower.contains("(u)") || lower.contains("(usa)") || lower.contains("[u]") || lower.contains(" usa ") -> 0
        lower.contains("(e)") || lower.contains("(europe)") || lower.contains("[e]") || lower.contains(" europe ") -> 1
        lower.contains("(world)") || lower.contains("[world]") || lower.contains(" world ") -> 2
        lower.contains("(j)") || lower.contains("(japan)") || lower.contains("[j]") || lower.contains(" japan ") -> 3
        else -> 4
    }
}

fun stabilityRank(name: String): Int {
    val lower = name.lowercase()
    return when {
        Regex("""(\(|\[)(beta|proto|sample|demo|test)(\)|\])""").containsMatchIn(lower) -> 4
        Regex("""(\(|\[)(hack|translation|homebrew|pirate|unl)(\)|\])""").containsMatchIn(lower) -> 3
        Regex("""\[(b|o|h)\d*\]""").containsMatchIn(lower) -> 2
        Regex("""\[(a|p)\d+\]""").containsMatchIn(lower) -> 1
        else -> 0
    }
}

fun dumpRank(name: String): Int {
    val lower = name.lowercase()
    return if (lower.contains("[!]") || lower.contains("(!)")) 0 else 1
}

fun revisionRank(name: String): Int {
    val lower = name.lowercase()
    return when {
        Regex("""(\(|\[)(rev|v|version)\s*[0-9a-z\.\-]+""").containsMatchIn(lower) -> 1
        else -> 0
    }
}

val preferredRomComparator = compareBy<File>(
    { regionRank(it.name) },
    { stabilityRank(it.name) },
    { dumpRank(it.name) },
    { revisionRank(it.name) },
    { it.name.length },
    { -it.length() }
)

if (bundleAllPayload.get()) {
    payloadSourceDirs.forEach { (assetFolder, sourceDir) ->
        val folder = sourceDir.asFile
        if (!folder.exists() || !folder.isDirectory) {
            throw GradleException("Full payload mode requires directory: ${folder.absolutePath} (assets/$assetFolder)")
        }
        val fileCount = rootProject.fileTree(folder).files.size
        if (fileCount == 0) {
            throw GradleException("Full payload mode found empty directory: ${folder.absolutePath} (assets/$assetFolder)")
        }
        println("[full-payload] assets/$assetFolder <- $fileCount files from ${folder.absolutePath}")
    }
}

if (bundleAllPayload.get()) {
    val prepareFullPayloadAssets = tasks.register("prepareFullPayloadAssets") {
        outputs.dir(fullPayloadAssetsDir)
        outputs.file(payloadOptimizationReportFile)

        doLast {
            val targetRoot = fullPayloadAssetsDir.get().asFile
            targetRoot.ensureEmptyDirectory()
            val summaryLines = mutableListOf<String>()

            val biosSource = payloadSourceDirs.getValue("bios").asFile
            val biosTarget = targetRoot.resolve("bios")
            copy {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                from(biosSource)
                into(biosTarget)
            }
            summaryLines += "bios: kept ${biosTarget.fileCount()} files (${formatMb(biosTarget.totalBytes())} MB)"

            val romsSource = payloadSourceDirs.getValue("roms").asFile
            val romsTarget = targetRoot.resolve("roms")
            romsTarget.ensureEmptyDirectory()

            var romInputFiles = 0
            var romInputBytes = 0L
            var romOutputFiles = 0
            var romOutputBytes = 0L
            var romRemovedBySidecar = 0
            var romRemovedByHash = 0
            var romRemovedByEdition = 0

            romsSource.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedBy { it.name.lowercase() }
                .orEmpty()
                .forEach { systemDir ->
                val sourceFiles = systemDir.walkTopDown().filter { it.isFile }.toList()
                    romInputFiles += sourceFiles.size
                    romInputBytes += sourceFiles.sumOf { it.length() }

                    val playableCandidates = sourceFiles.filterNot(::isUnusedRomSidecar)
                    romRemovedBySidecar += (sourceFiles.size - playableCandidates.size)

                    val uniqueByHash = linkedMapOf<String, File>()
                    playableCandidates.forEach { file ->
                        val hash = sha1(file)
                        val current = uniqueByHash[hash]
                        if (current == null || preferredRomComparator.compare(file, current) < 0) {
                            uniqueByHash[hash] = file
                        }
                    }
                    romRemovedByHash += (playableCandidates.size - uniqueByHash.size)

                    val uniqueByTitle = uniqueByHash.values
                        .groupBy { normalizedRomKey(it.name) }
                        .values
                        .mapNotNull { variants -> variants.minWithOrNull(preferredRomComparator) }

                    romRemovedByEdition += (uniqueByHash.size - uniqueByTitle.size)

                    val systemTarget = romsTarget.resolve(systemDir.name).apply { mkdirs() }
                    uniqueByTitle
                        .sortedBy { it.name.lowercase() }
                        .forEach { file ->
                            val relative = file.relativeTo(systemDir)
                            val destination = systemTarget.resolve(relative.path)
                            destination.parentFile?.mkdirs()
                            file.copyTo(destination, overwrite = true)
                        }
                }

            romOutputFiles = romsTarget.fileCount()
            romOutputBytes = romsTarget.totalBytes()
            summaryLines += buildString {
                append("roms: kept $romOutputFiles/$romInputFiles files ")
                append("(${formatMb(romOutputBytes)} MB from ${formatMb(romInputBytes)} MB)")
                append(" | removed sidecars=$romRemovedBySidecar hash-dupes=$romRemovedByHash edition-dupes=$romRemovedByEdition")
            }

            val coresSource = payloadSourceDirs.getValue("cores").asFile
            val coresTarget = targetRoot.resolve("cores")
            coresTarget.ensureEmptyDirectory()
            val keptCoreFiles = coresSource.listFiles()
                ?.filter { it.isFile }
                .orEmpty()
            val copiedCoreNames = mutableListOf<String>()

            bundledCoreNames.forEach { core ->
                val canonicalTargetName = "${core}_libretro_android.so"
                val sourceFile = when (core) {
                    "mupen64plus_next" -> keptCoreFiles.firstOrNull {
                        val lower = it.name.lowercase()
                        lower == "mupen64plus_next_libretro_android.so" ||
                            lower == "mupen64plus_next_gles3_libretro_android.so" ||
                            lower == "mupen64plus_next_gles2_libretro_android.so"
                    }
                    else -> keptCoreFiles.firstOrNull {
                        val lower = it.name.lowercase()
                        lower == canonicalTargetName || lower == "${core}_libretro.so" || lower == "$core.so"
                    }
                }

                if (sourceFile == null) {
                    throw GradleException("Core obrigatorio nao encontrado para payload otimizado: $core")
                }

                sourceFile.copyTo(coresTarget.resolve(canonicalTargetName), overwrite = true)
                copiedCoreNames += canonicalTargetName
            }
            val coreInputFiles = coresSource.fileCount()
            val coreInputBytes = coresSource.totalBytes()
            val coreOutputFiles = coresTarget.fileCount()
            val coreOutputBytes = coresTarget.totalBytes()
            summaryLines += "cores: kept $coreOutputFiles/$coreInputFiles files (${formatMb(coreOutputBytes)} MB from ${formatMb(coreInputBytes)} MB)"
            summaryLines += "cores-list: ${copiedCoreNames.joinToString(", ")}"

            val retroarchSource = payloadSourceDirs.getValue("retroarch").asFile
            val retroarchTarget = targetRoot.resolve("retroarch")
            retroarchTarget.ensureEmptyDirectory()

            copy {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                from(retroarchSource.resolve("config")) {
                    include("retroarch.cfg", "retroarch-core-options.cfg")
                }
                into(retroarchTarget.resolve("config"))
            }
            copy {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                from(retroarchSource.resolve("autoconfig")) {
                    include("android/**")
                }
                into(retroarchTarget.resolve("autoconfig"))
            }
            copy {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                from(retroarchSource.resolve("info")) {
                    include(*bundledCoreInfoNames.toTypedArray())
                }
                into(retroarchTarget.resolve("info"))
            }
            copy {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                from(retroarchSource.resolve("assets")) {
                    include("glui/**", "pkg/**", "sounds/**")
                    exclude("**/*.md", "**/*.txt", "**/README*", "**/LICENCE*", "**/LICENSE*")
                }
                into(retroarchTarget.resolve("assets"))
            }
            val retroarchInputFiles = retroarchSource.fileCount()
            val retroarchInputBytes = retroarchSource.totalBytes()
            val retroarchOutputFiles = retroarchTarget.fileCount()
            val retroarchOutputBytes = retroarchTarget.totalBytes()
            summaryLines += "retroarch: kept $retroarchOutputFiles/$retroarchInputFiles files (${formatMb(retroarchOutputBytes)} MB from ${formatMb(retroarchInputBytes)} MB)"

            copy {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                from("src/main/assets") {
                    exclude("bios/**", "roms/**", "cores/**", "retroarch/**")
                }
                into(targetRoot)
            }

            val sourceTotalBytes = payloadSourceDirs.values.sumOf { it.asFile.totalBytes() }
            val targetTotalBytes =
                biosTarget.totalBytes() +
                romsTarget.totalBytes() +
                coresTarget.totalBytes() +
                retroarchTarget.totalBytes()
            val savedBytes = sourceTotalBytes - targetTotalBytes
            summaryLines += "payload-total: ${formatMb(targetTotalBytes)} MB from ${formatMb(sourceTotalBytes)} MB (saved ${formatMb(savedBytes)} MB)"

            val reportFile = payloadOptimizationReportFile.get().asFile
            reportFile.parentFile.mkdirs()
            reportFile.writeText(summaryLines.joinToString(System.lineSeparator()))

            summaryLines.forEach { println("[payload-opt] $it") }
        }
    }

    tasks.configureEach {
        if (name == "preBuild") {
            dependsOn(prepareFullPayloadAssets)
        }
    }
}

android {
    namespace = "com.example.deepmule"
    compileSdk = 36

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(requireNotNull(releaseStoreFilePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.deepdarknessstudios.deepmule"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        // Token para reporter de crashes/análises no GitHub
        // Em produção: definir GITHUB_REPORTER_TOKEN como secret no GitHub Actions
        // Localmente: criar secrets.properties com GITHUB_REPORTER_TOKEN=ghp_...
        val reporterToken = (project.findProperty("GITHUB_REPORTER_TOKEN")
            ?: System.getenv("GITHUB_REPORTER_TOKEN")
            ?: "none").toString()
        buildConfigField("String", "GITHUB_REPORTER_TOKEN", "\"$reporterToken\"")
        multiDexEnabled = true
        testFunctionalTest = true
        testHandleProfiling = true
        testApplicationId = "com.example.deepmule.teste_int"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".ddsemulador"
            versionNameSuffix = "-test_beta_01_mule"
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("Release signing secrets are missing. Falling back to debug keystore for local builds.")
                signingConfigs.getByName("debug")
            }
            isDebuggable = false
            isJniDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            if (bundleAllPayload.get()) {
                // Keep stable paths in APK: assets/bios, assets/roms, assets/cores, assets/retroarch.
                assets.directories.clear()
                assets.directories.add(fullPayloadAssetsDir.get().asFile.absolutePath)
            }
        }
    }

    androidResources {
        noCompress += setOf(
            "so", "bin", "rom", "iso", "cue", "chd", "pbp", "zip", "7z",
            "gba", "gb", "gbc", "nes", "sfc", "smc", "md", "gen", "n64", "z64", "v64",
            "nds", "a26", "a78", "lnx", "pce", "ngp", "ngpc", "ws", "wsc"
        )
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }
    ndkVersion = "28.2.13676358"
}

tasks.register("buildAndInstallRelease") {
    group = "distribution"
    description = "Builds full-payload release APK, installs on connected device, and launches MainActivity."
    dependsOn("assembleRelease")

    doLast {
        if (!bundleAllPayload.get()) {
            throw GradleException("buildAndInstallRelease requires -Pdeepmule.bundleAllPayload=true")
        }

        val apkCandidates = listOf(
            layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile,
            layout.buildDirectory.file("outputs/apk/release/app-release-unsigned.apk").get().asFile
        )
        val apkFile = apkCandidates.firstOrNull { it.exists() }
            ?: throw GradleException("Release APK not found under app/build/outputs/apk/release")

        val adbPath = resolveSecret("DEEPMULE_ADB_PATH")?.takeIf { it.isNotBlank() } ?: "adb"
        val deviceSerial = resolveSecret("DEEPMULE_DEVICE_SERIAL")?.takeIf { it.isNotBlank() }
        val packageName = "com.deepdarknessstudios.deepmule"
        val mainActivity = "$packageName/com.example.deepmule.MainActivity"

        fun runAdb(vararg args: String): Int {
            val command = mutableListOf(adbPath)
            if (!deviceSerial.isNullOrBlank()) {
                command += listOf("-s", deviceSerial)
            }
            command += args
            return ProcessBuilder(command)
                .inheritIO()
                .start()
                .waitFor()
        }

        if (runAdb("start-server") != 0) {
            throw GradleException("Failed to start adb server")
        }

        var installCode = runAdb("install", "-r", apkFile.absolutePath)
        if (installCode != 0) {
            logger.warn("adb install -r failed. Trying uninstall/reinstall fallback.")
            runAdb("uninstall", packageName)
            installCode = runAdb("install", "-r", apkFile.absolutePath)
        }

        if (installCode != 0) {
            throw GradleException("Failed to install release APK on device")
        }

        if (runAdb("shell", "am", "start", "-n", mainActivity) != 0) {
            throw GradleException("APK installed, but failed to launch MainActivity")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.accompanist.permissions)
    implementation(libs.play.services.location)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.core)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)
    implementation(libs.moshi.kotlin)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)
}

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.opencloudgaming.buildlogic.WebRtcAudioGuardFactory
import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

androidComponents.onVariants { variant ->
    variant.instrumentation.transformClassesWith(
        WebRtcAudioGuardFactory::class.java,
        InstrumentationScope.ALL,
    ) {}
    variant.instrumentation.setAsmFramesComputationMode(
        FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS,
    )
}

/**
 * Telemetry is opt-in and off unless a token is supplied at build time. There is deliberately no
 * committed default: a token baked into a public repo is an active, writable ingest credential.
 *
 * Supply one of, in priority order:
 *   ./gradlew assembleRelease -Pposthog.projectToken=phc_... -Pposthog.host=https://...
 *   POSTHOG_PROJECT_TOKEN / POSTHOG_HOST environment variables
 *   posthog.projectToken / posthog.host in local.properties (gitignored)
 */
val posthogProjectToken: String = sequenceOf(
    providers.gradleProperty("posthog.projectToken").orNull,
    providers.environmentVariable("POSTHOG_PROJECT_TOKEN").orNull,
    rootProject.file("local.properties").takeIf { it.isFile }?.let { file ->
        Properties().apply { file.inputStream().use(::load) }.getProperty("posthog.projectToken")
    },
).filterNotNull().firstOrNull { it.isNotBlank() }.orEmpty()

val posthogHost: String = sequenceOf(
    providers.gradleProperty("posthog.host").orNull,
    providers.environmentVariable("POSTHOG_HOST").orNull,
    rootProject.file("local.properties").takeIf { it.isFile }?.let { file ->
        Properties().apply { file.inputStream().use(::load) }.getProperty("posthog.host")
    },
).filterNotNull().firstOrNull { it.isNotBlank() } ?: "https://us.i.posthog.com"

/**
 * Release signing, read from a keystore.properties that is deliberately kept out of version control.
 * Credentials never live in this file: point storeFile at a keystore on disk and supply the
 * passwords through the NEXPLAY_STORE_PASSWORD / NEXPLAY_KEY_PASSWORD environment variables (or an
 * untracked keystore.properties). With no credentials present the release build still succeeds, it
 * just produces an unsigned APK, which is the correct failure mode for a contributor without the key.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) file.inputStream().use(::load)
}
val signingSecrets = mapOf(
    "storePassword" to (System.getenv("NEXPLAY_STORE_PASSWORD") ?: keystoreProperties.getProperty("storePassword")),
    "keyPassword" to (System.getenv("NEXPLAY_KEY_PASSWORD") ?: keystoreProperties.getProperty("keyPassword")),
)
val keystorePath = keystoreProperties.getProperty("storeFile")?.let { configured ->
    // Accept a path relative to this file, to the project root, or absolute.
    sequenceOf(
        configured,
        rootProject.file(configured).path,
        rootProject.projectDir.resolve(configured).path,
    ).firstOrNull { File(it).isFile } ?: configured
}
val hasSigningMaterial = keystorePath != null &&
    signingSecrets.values.all { !it.isNullOrBlank() } &&
    keystoreProperties.getProperty("keyAlias")?.isNotBlank() == true

val buildingPlayReleaseBundle =
    providers.gradleProperty("distribution").orNull.equals("play-store", ignoreCase = true) ||
        gradle.startParameter.taskNames.any { taskName ->
            taskName.substringAfterLast(":").equals("bundleRelease", ignoreCase = true)
        }

android {
    namespace = "com.nexplay"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nexplay"
        minSdk = 24
        // Android 17
        // target changes are audited; LAN access is permission-gated at its feature boundary.
        //noinspection EditedTargetSdkVersion
        targetSdk = 37
        versionCode = 159
        versionName = "2.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("boolean", "APK_UPDATES_SUPPORTED", "true")
        buildConfigField("boolean", "PLAY_STORE_RELEASE", "false")
        buildConfigField("boolean", "LOCAL_APP_LAUNCHER_SUPPORTED", "true")
        buildConfigField("String", "POSTHOG_PROJECT_TOKEN", "\"${posthogProjectToken}\"")
        buildConfigField("String", "POSTHOG_HOST", "\"$posthogHost\"")

        ndk {
            // Keep legacy Intel TV devices eligible; App Bundles deliver only the matching ABI.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }

    }

    // Declared before buildTypes so the named lookup there resolves. Gradle registers named
    // containers in evaluation order, so a later signingConfigs{} would still be missing.
    if (hasSigningMaterial) {
        signingConfigs {
            create("release") {
                storeFile = File(keystorePath!!)
                storePassword = signingSecrets["storePassword"]
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = signingSecrets["keyPassword"]
                // v1/v2 keep older install paths working; v3 is required from API 28.
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            if (hasSigningMaterial) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "APK_UPDATES_SUPPORTED", (!buildingPlayReleaseBundle).toString())
            buildConfigField("boolean", "PLAY_STORE_RELEASE", buildingPlayReleaseBundle.toString())
            buildConfigField("boolean", "LOCAL_APP_LAUNCHER_SUPPORTED", (!buildingPlayReleaseBundle).toString())
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    sourceSets {
        getByName("debug").manifest.srcFile("src/sideload/AndroidManifest.xml")
        getByName("release").manifest.srcFile(
            if (buildingPlayReleaseBundle) {
                "src/playBundle/AndroidManifest.xml"
            } else {
                "src/sideload/AndroidManifest.xml"
            },
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
            )
        }
    }

    lint {
        checkReleaseBuilds = false
    }
}

// New Android copy stays in the shared English source; Android XML is generated.
val touchButtonResources = layout.buildDirectory.dir("generated/touchButtonResources")
val touchButtonEnglishSource = rootProject.file("../locales/en.json")
val generateTouchButtonResources = tasks.register("generateTouchButtonResources") {
    inputs.file(touchButtonEnglishSource)
    outputs.dir(touchButtonResources)
    doLast {
        val source = groovy.json.JsonSlurper().parse(inputs.files.singleFile) as Map<*, *>
        val strings = (source["androidTouchButtons"] as Map<*, *>) +
            (source["androidStreamInput"] as Map<*, *>) +
            (source["androidSetup"] as Map<*, *>)
        fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "\\\"").replace("'", "\\'")
        val output = outputs.files.singleFile.resolve("values/touch_buttons.xml")
        output.parentFile.mkdirs()
        output.writeText("<resources>\n" + strings.entries.joinToString("\n") { (key, value) ->
            "    <string name=\"$key\">${xml(value as String)}</string>"
        } + "\n</resources>\n")
    }
}
android.sourceSets.getByName("main").res.directories.add(touchButtonResources.get().asFile.absolutePath)
tasks.named("preBuild").configure { dependsOn(generateTouchButtonResources) }

val nvstJniOutput = layout.buildDirectory.dir("generated/nvstJniLibs")
val buildNvst = tasks.register<Exec>("buildNvst") {
    inputs.files(fileTree(rootProject.file("nvst")) { exclude("target/**") })
    inputs.file(rootProject.file("scripts/build_nvst.py"))
    outputs.dir(nvstJniOutput)
    val ndkDirectory = androidComponents.sdkComponents.ndkDirectory
    commandLine(
        if (System.getProperty("os.name").startsWith("Windows")) "python" else "python3",
        rootProject.file("scripts/build_nvst.py").absolutePath,
        ndkDirectory.get().asFile.absolutePath,
        nvstJniOutput.get().asFile.absolutePath,
    )
}
android.sourceSets.getByName("main").jniLibs.directories.add(nvstJniOutput.get().asFile.absolutePath)
tasks.named("preBuild").configure { dependsOn(buildNvst) }

kotlin {
    jvmToolchain(17)
    compilerOptions {
        // K2's FIR data-flow analysis is pathological on the streaming state machine. Kotlin's
        // supported 1.9 language mode keeps the stable frontend until that class is fully decomposed.
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        // This app is one large Kotlin module. Parallelize JVM code generation so cold builds
        // do not leave the machine idling on a single backend thread.
        freeCompilerArgs.add("-Xbackend-threads=0")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.08.00"))

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.browser:browser:1.10.0")
    // Play App Update still requests Fragment 1.0.0 transitively through Play Services.
    implementation("androidx.fragment:fragment:1.9.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.media3:media3-common:1.11.0")
    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")
    implementation("androidx.work:work-runtime:2.11.2")

    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")

    implementation("com.squareup.okhttp3:logging-interceptor:5.5.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:5.5.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.google.mlkit:language-id:17.0.6")
    // Includes upstream Android AudioRecord restart and stopped-transceiver stats crash fixes.
    implementation("io.github.webrtc-sdk:android:144.7559.14")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.snakeyaml:snakeyaml-engine:3.1.1")
    // Optional YouTube Live rebroadcast: RTMP ingest plus MediaProjection screen capture.
    implementation("com.github.pedroSG94.RootEncoder:library:2.8.0")
    // Opt-in analytics. Inert unless a project token is supplied at build time.
    implementation("com.posthog:posthog-android:3.51.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

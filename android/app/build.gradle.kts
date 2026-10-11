import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

/**
 * Paquets publics construits par la chaîne de contenu (content/pqt.py, ADR 003) au moment du build : jamais de binaire
 * commité. Seuls les droits « domaine-public » et « licence-libre » passent (--public-only). Le catalogue est signé avec
 * la clé de test ; les paquets privés de la saveur « personnel » arrivent à part, dans packs-private/ (ADR 001).
 */
abstract class BuildContentPacks : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    abstract val pqt: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val signKey: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun build() {
        val packs = outputDir.get().dir("packs").asFile
        packs.deleteRecursively()
        packs.mkdirs()
        exec.exec {
            commandLine("python3", "-I", pqt.get().asFile.absolutePath, "build", "--public-only",
                "--sign-key", signKey.get().asFile.absolutePath, "--out", packs.absolutePath)
        }
    }
}

val content = rootProject.layout.projectDirectory.dir("../content")
val buildContentPacks = tasks.register<BuildContentPacks>("buildContentPacks") {
    sources.from(fileTree(content) {
        include("data/**", "packages/**", "pipeline/**", "schema/**", "pqt.py")
        exclude("**/__pycache__/**")
    })
    pqt.set(content.file("pqt.py"))
    signKey.set(content.file("schema/keys/test-p256.pem"))
    outputDir.set(layout.buildDirectory.dir("generated/content-packs"))
}

androidComponents {
    onVariants { variant -> variant.sources.assets?.addGeneratedSourceDirectory(buildContentPacks, BuildContentPacks::outputDir) }
}

// Clé de publication hors du dépôt, comme pour les autres projets (~/.gradle/gradle.properties). Absente : APK release non signé.
val releaseKeystore = providers.gradleProperty("PAROISSIEN_KEYSTORE").orNull
// Paquets privés de la saveur « personnel » (assets packs-private/) : lus hors du dépôt public (ADR 001).
val personnelPacksDir = providers.gradleProperty("paroissien.personnelPacksDir").orNull

android {
    namespace = "app.paroissien"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.paroissien" // figé (ADR 002) : ne plus changer après la première installation
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 5
        versionName = "0.3.1"
    }

    // ADR 001 : seule « domainePublic » est construite par la CI publique ; « personnel » peut embarquer
    // des contenus réservés à l'usage privé (Clairval, Barroux, AELF) et ne se construit qu'en privé.
    flavorDimensions += "distribution"
    productFlavors {
        create("domainePublic") { dimension = "distribution" }
        create("personnel") { dimension = "distribution"; versionNameSuffix = "-perso" }
    }
    if (personnelPacksDir != null) sourceSets.getByName("personnel").assets.directories.add(personnelPacksDir)

    signingConfigs {
        // Clé de débogage commitée : un APK de CI se met à jour par-dessus un build local sans perdre user.db.
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (releaseKeystore != null) create("release") {
            storeFile = file(releaseKeystore)
            storePassword = providers.gradleProperty("PAROISSIEN_KEYSTORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("PAROISSIEN_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("PAROISSIEN_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev" // cohabite avec la version d'usage quotidien
            versionNameSuffix = "-dev"
            signingConfig = signingConfigs.getByName("debug")
            ndk { abiFilters += "arm64-v8a" } // le Z Flip6 : un empaquetage plus rapide sur la VM
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        }
        // ADR 002 et 005 : mesures de la porte A sur un build optimisé, sans la clé de release.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            applicationIdSuffix = ".bench"
            versionNameSuffix = "-bench"
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions { unitTests.isIncludeAndroidResources = true }
}

// Pages du livre d'essai : références commitées, vérifiées en CI (verifyRoborazziDomainePublicDebug).
roborazzi { outputDir.set(layout.projectDirectory.dir("src/test/screenshots")) }

tasks.withType<Test>().configureEach {
    maxParallelForks = 1
    maxHeapSize = "1536m"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:liturgy"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:reading"))
    implementation(libs.compose.foundation)
    implementation(libs.androidx.metrics.performance)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

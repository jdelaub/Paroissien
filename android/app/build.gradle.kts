plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Clé de publication hors du dépôt, comme pour les autres projets (~/.gradle/gradle.properties). Absente : APK release non signé.
val releaseKeystore = providers.gradleProperty("PAROISSIEN_KEYSTORE").orNull
// Paquets privés de la saveur « personnel » : lus depuis un dossier hors du dépôt public (ADR 001).
val personnelPacksDir = providers.gradleProperty("paroissien.personnelPacksDir").orNull

android {
    namespace = "app.paroissien"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.paroissien" // figé (ADR 002) : ne plus changer après la première installation
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.0.1"
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:liturgy"))
    implementation(project(":core:data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)
}

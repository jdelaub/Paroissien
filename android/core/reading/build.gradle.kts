// Moteur de lecture : tourne-page (repris de Mekuri, voir THIRD_PARTY_NOTICES.md), puis pagination (phase 1b).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.paroissien.core.reading"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.animation.core)
    implementation(libs.compose.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}

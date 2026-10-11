// Données de l'utilisateur (ADR 008) : Room 3 + SQLite embarqué. KSP n'est appliqué qu'ici.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

android {
    namespace = "app.paroissien.core.data"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Schémas exportés et commités : chaque migration est testée depuis chacun d'eux (ADR 008).
room3 { schemaDirectory("$projectDir/schemas") }

dependencies {
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.androidx.room3.compiler)
}

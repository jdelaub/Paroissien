// Données de l'utilisateur (ADR 008) : Room 3 + SQLite embarqué, réglages DataStore, export JSON. KSP n'est appliqué qu'ici.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.paroissien.core.data"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

// Schémas exportés et commités : chaque migration est testée depuis chacun d'eux (ADR 008).
room3 { schemaDirectory("$projectDir/schemas") }

dependencies {
    api(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room3.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.sqlite.framework)
}

val schemas = layout.projectDirectory.dir("schemas")
tasks.withType<Test>().configureEach {
    inputs.dir(schemas).withPropertyName("schemas")
    systemProperty("room.schemas", schemas.asFile.absolutePath)
    maxHeapSize = "1536m"
}

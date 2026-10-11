// Paquets de contenu (ADR 003) : catalogue signé, installation vérifiée, lecture en SQLite embarqué.
// Kotlin pur : les tests ouvrent le paquet golden sur la JVM avec le même pilote que l'app.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    api(project(":core:model"))
    api(libs.androidx.sqlite)
    implementation(libs.androidx.sqlite.bundled)
    testImplementation(libs.junit)
}

val golden = rootProject.layout.projectDirectory.dir("../content/golden/dist")
val keys = rootProject.layout.projectDirectory.dir("../content/schema/keys")

tasks.test {
    inputs.dir(golden).withPropertyName("golden")
    inputs.dir(keys).withPropertyName("keys")
    systemProperty("golden.dir", golden.asFile.absolutePath)
    systemProperty("keys.dir", keys.asFile.absolutePath)
    testLogging { events("passed", "failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

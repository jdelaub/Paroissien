// Modèle partagé et contrats avec la chaîne de contenu. Kotlin pur sur la JVM, testé en quelques secondes.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

// Vecteurs de pliage partagés avec content/pipeline (ADR 004) : la même table fait foi des deux côtés.
val foldVectors = rootProject.layout.projectDirectory.file("../content/schema/fold-vectors.json")

tasks.test {
    inputs.file(foldVectors).withPropertyName("foldVectors")
    systemProperty("fold.vectors", foldVectors.asFile.absolutePath)
    testLogging { events("passed", "failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

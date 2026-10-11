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

// Contrats partagés avec content/ (ADR 004) : vecteurs de pliage et textes canoniques du paquet golden.
val foldVectors = rootProject.layout.projectDirectory.file("../content/schema/fold-vectors.json")
val goldenExpected = rootProject.layout.projectDirectory.file("../content/golden/dist/golden-test.expected.json")

tasks.test {
    inputs.file(foldVectors).withPropertyName("foldVectors")
    inputs.file(goldenExpected).withPropertyName("goldenExpected")
    systemProperty("fold.vectors", foldVectors.asFile.absolutePath)
    systemProperty("golden.expected", goldenExpected.asFile.absolutePath)
    testLogging { events("passed", "failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

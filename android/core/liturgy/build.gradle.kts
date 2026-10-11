// Calendrier liturgique hors ligne (1962 et 1969, France). Kotlin pur, sous Apache-2.0 pour être réutilisable (rapport 12) :
// il ne dépend donc d'aucun module GPL du projet.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    testLogging { events("passed", "failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

// Numérisation des images pieuses personnelles (rapport 09, §3.3) : détection des bords, réglages automatiques.
// Kotlin pur sur des tableaux de pixels, testé sur la JVM ; le rendu Android (perspective, WebP) reste dans :app.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

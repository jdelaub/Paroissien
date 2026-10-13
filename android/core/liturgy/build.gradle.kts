// Calendrier liturgique hors ligne (1962 et 1969, France). Kotlin pur, sous Apache-2.0 pour être réutilisable (rapport 12) :
// il ne dépend donc d'aucun module GPL du projet. ADR 009.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

// Le sanctoral (content/schema/calendar-v1.md) voyage dans le jar comme ressource : calendar/<rite>/<calendrier>.json.
val calendarData = rootProject.layout.projectDirectory.dir("../content/data")
sourceSets.main {
    resources.srcDir(calendarData)
    resources.include("calendar/**/*.json")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

// Oracles des tests G3 (romcal, Missalemeum), lus sur le disque : ils ne vont pas dans le jar.
val golden = rootProject.layout.projectDirectory.dir("../content/golden/calendar")

// Les formulaires du missel (content/sources/missel/cles.txt) : chaque jour doit trouver sa messe.
val missalKeys = rootProject.layout.projectDirectory.file("../content/sources/missel/cles.txt")

tasks.test {
    inputs.dir(golden).optional().withPropertyName("golden")
    systemProperty("calendar.golden", golden.asFile.absolutePath)
    inputs.files(missalKeys).optional().withPropertyName("missalKeys")
    systemProperty("missal.keys", missalKeys.asFile.absolutePath)
    maxHeapSize = "1g"
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

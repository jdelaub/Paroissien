pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "paroissien"
// Un module naît d'un besoin (rapport 10) : today et reader arrivent avec leurs phases.
include(":app")
include(":core:model")
include(":core:liturgy")
include(":core:data")
include(":core:designsystem")
include(":core:reading")
include(":core:content")

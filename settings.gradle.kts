pluginManagement {
    repositories {
        google()
        maven { url = uri("https://maven.google.com") } // Wymuszenie bezpośredniego adresu
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        maven { url = uri("https://maven.google.com") } // Tutaj również
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "StockAnalysisAssistant"
include(":app")
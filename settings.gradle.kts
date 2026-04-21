pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/releases/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://teavm.org/maven/repository/")
    }
}
rootProject.name = "starshard"
include("core", "desktop", "android", "tools", "teavm")

plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion = "1.12.1"
val natives: Configuration by configurations.creating

android {
    compileSdk = 35
    namespace = "com.palacesoft.starshard"
    defaultConfig {
        applicationId = "com.palacesoft.starshard"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "1.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    sourceSets["main"].apply {
        assets.srcDirs(rootProject.files("assets"))
        jniLibs.srcDir("libs")
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.google.android.gms:play-services-games-v2:19.0.0")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")
}

tasks.register("copyAndroidNatives") {
    doFirst {
        natives.files.forEach { jar ->
            val name = jar.nameWithoutExtension
            val abi = name.substringAfter("natives-")
            val outputDir = file("libs/$abi")
            outputDir.mkdirs()
            copy { from(zipTree(jar)); into(outputDir); include("*.so") }
        }
    }
}
tasks.named("preBuild") { dependsOn("copyAndroidNatives") }

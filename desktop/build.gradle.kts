plugins {
    kotlin("jvm")
    application
}

val gdxVersion = "1.12.1"

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass.set("com.palacesoft.starshard.desktop.DesktopLauncher")
    applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("assets")
}

tasks.jar {
    manifest { attributes["Main-Class"] = "com.palacesoft.starshard.desktop.DesktopLauncher" }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

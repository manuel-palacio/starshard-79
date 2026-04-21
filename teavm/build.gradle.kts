plugins {
    kotlin("jvm")
}

val gdxTeaVMVersion = "1.5.4"

sourceSets.main {
    resources.srcDirs(rootProject.file("assets").path)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val gdxVersion = "1.14.0"

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.github.xpenatan.gdx-teavm:backend-web:$gdxTeaVMVersion")
}

val mainClassName = "com.palacesoft.starshard.teavm.TeaVMBuilder"

tasks.register<JavaExec>("buildRelease") {
    description = "Build the TeaVM app to build/dist"
    group = "build"
    dependsOn(tasks.classes)
    mainClass.set(mainClassName)
    classpath = sourceSets.main.get().runtimeClasspath
}

tasks.register<JavaExec>("runRelease") {
    description = "Run the TeaVM app via local Jetty at http://localhost:8080/"
    group = "application"
    dependsOn(tasks.classes)
    mainClass.set(mainClassName)
    classpath = sourceSets.main.get().runtimeClasspath
    args("run")
}

tasks.register("run") {
    dependsOn("runRelease")
}

tasks.build {
    dependsOn("buildRelease")
}

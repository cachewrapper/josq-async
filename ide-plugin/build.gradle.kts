plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"

    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()

    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
}

dependencies {
    implementation(project(":private-api"))
    compileOnly(project(":annotation-processor"))

    implementation("org.jetbrains:annotations:24.0.1")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

intellij {
    version.set("2023.3")
    updateSinceUntilBuild.set(false)
    plugins.set(listOf("java"))
    type.set("IC")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.register("prepareKotlinBuildScriptModel") {}
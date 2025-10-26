plugins {
    id("java-library")
    id("com.github.johnrengelman.shadow") version "8.1.1"

    id("maven-publish")
}

group = "org.cachewrapper"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.google.auto.service:auto-service:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")

    implementation(project(":private-api"))
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"
        )
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks.named("shadowJar").get())
            groupId = "org.cachewrapper"
            artifactId = "josq-async"
            version = project.version.toString()
        }
    }

    repositories {
        mavenLocal()
    }
}


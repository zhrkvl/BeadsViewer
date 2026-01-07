plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.10.5"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
}

group = "me.zkvl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2025.3.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:

        composeUI()

    }

    // kotlinx.serialization for JSONL parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // kotlinx-datetime for type-safe date/time handling
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.1")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    // Configure test task for IntelliJ Platform
    withType<Test> {
        useJUnitPlatform()
        systemProperty("idea.is.internal", "true")
        systemProperty("idea.system.path", "${project.buildDir}/idea-system")
        systemProperty("idea.config.path", "${project.buildDir}/idea-config")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

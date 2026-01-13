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

dependencies {
    intellijPlatform {
        intellijIdea("2025.3.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        composeUI()
    }

    // kotlinx.serialization for JSONL parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // kotlinx-datetime for type-safe date/time handling
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    // SQLite JDBC driver for reading .beads/beads.db database
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")

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

    signing {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<Test> {
        useJUnitPlatform()
        systemProperty("idea.is.internal", "true")
        systemProperty("idea.system.path", "${layout.buildDirectory.get()}/idea-system")
        systemProperty("idea.config.path", "${layout.buildDirectory.get()}/idea-config")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

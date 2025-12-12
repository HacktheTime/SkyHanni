import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.21"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://jitpack.io") {
        content {
            includeGroupByRegex("com\\.github\\..*")
        }
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.13.1")
    // Pin Kotlin stdlib to match plugin version
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.21")
    implementation("com.github.SkyHanniStudios:SkyHanniChangelogBuilder:1.1.2")
    implementation(files("../sharedVariables/build/libs/sharedVariables.jar"))
    implementation("com.github.mizosoft.methanol:methanol:1.8.3")
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget("21"))
}

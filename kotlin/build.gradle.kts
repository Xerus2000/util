import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.2.31"
}

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib"))
    compile("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "0.+")
}

java.sourceSets {
    "main" {
        java.srcDirs("src")
        resources.srcDirs("resources")
    }
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin.experimental.coroutines = Coroutines.ENABLE
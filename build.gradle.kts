group = "com.iptiq.infrastructure.loadbalancer"
version = "0.0.1-SNAPSHOT"

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.0")
    }
}

plugins {
    kotlin("jvm") version "1.7.0"
    id("io.gitlab.arturbosch.detekt") version "1.20.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.20.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.2")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("io.ktor:ktor-server-core-jvm:2.0.2")
    implementation("io.ktor:ktor-server-host-common-jvm:2.0.2")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.0.2")
    implementation("io.ktor:ktor-server-netty-jvm:2.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.7.0")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src/main/kotlin")
kotlin.sourceSets["test"].kotlin.srcDirs("src/test/kotlin")

detekt {
    config = files("detekt.yml")
    autoCorrect = true
}

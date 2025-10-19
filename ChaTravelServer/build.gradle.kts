plugins {
    kotlin("jvm") version "2.2.10"
    alias(libs.plugins.kotlin.serialization)
}

group = "com.sjaindl.chatravelserver"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.cio)
    implementation(ktorLibs.client.json)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.client.apache)
    implementation(ktorLibs.client.encoding)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.websockets)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.htmlBuilder)
    implementation(ktorLibs.server.resources)
    implementation(ktorLibs.server.callLogging)

    implementation(libs.mongo)
    implementation(libs.kmongo.coroutine.serialization)
    implementation(libs.logback)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

import com.google.protobuf.gradle.id

plugins {
    kotlin("jvm") version "2.2.10"
    alias(libs.plugins.kotlin.serialization)
    id("com.google.protobuf") version "0.9.5"
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
    implementation(ktorLibs.serialization.jackson)
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
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.grpc.netty)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.services)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.protobuf)
    implementation(libs.grpc.protobuf)

    implementation(libs.graphql.java)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    implementation(libs.firebase.admin.sdk)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.76.0"
        }

        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.3:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

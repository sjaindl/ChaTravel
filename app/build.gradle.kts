import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.graphQL)
}

android {
    namespace = "com.sjaindl.chatravel"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sjaindl.chatravel"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // one of: SHORT_POLL, LONG_POLL, WEBSOCKETS
        buildConfigField("String", "MESSAGE_NETWORK_TYPE", "\"WEBSOCKETS\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material.icons.extended.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logging.napier)
    implementation(libs.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.android)

    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.protobuf.javalite)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin.stub)

    implementation(libs.apollo.runtime)
    //implementation(libs.apollo.coroutines.support)

    testImplementation(libs.junit)
    testImplementation(libs.koin.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.33.0"
    }

    plugins {
        id("grpc")  { artifact = "io.grpc:protoc-gen-grpc-java:1.76.0" }
        id("grpckt"){ artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.3:jdk8@jar" }
    }
    // Android needs LITE messages for size; generate Java lite + gRPC Java + gRPC Kotlin
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // Generate Java messages as *lite*
                id("java") { option("lite") }
            }
            task.plugins {
                id("grpc") { option("lite") }  // generates Java gRPC stubs (lite-compatible)
                id("grpckt") { option("lite") }  // generates Kotlin coroutine stubs (ChatServiceGrpcKt)
            }
        }
    }
}

apollo {
    service("chatravel") {
        packageName.set("com.sjaindl.chatravel.gql")

        // During development, fetching schema from server:
        introspection {
            endpointUrl.set("http://0.0.0.0:8080/graphql")
            // headers.put("Authorization", "Bearer ...")
        }
    }
}

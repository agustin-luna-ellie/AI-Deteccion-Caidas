plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.pruebared"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pruebared"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")

        }
        androidResources {
            noCompress += "tflite"
        }
    }
}

dependencies {



    // GPU Acceleration (optional)
    implementation("com.google.ai.edge.litert:litert-api:1.2.0")

    // Si usas operaciones avanzadas como GRU/RNN
    implementation("com.google.ai.edge.litert:litert-gpu:1.2.0")

    // (Opcional) Si usas aceleración por GPU

    implementation("com.google.ai.edge.litert:litert-support:1.2.0")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0")



    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

}


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "io.fallcare"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.fallcare"
        minSdk = 30
        targetSdk = 35
        versionCode = 101
        versionName = "1.0.1"

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    androidResources {
        noCompress += "tflite"
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
        compose = true
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")

        }
    }
}

dependencies {

    // Wear OS
    implementation (libs.wear)
    implementation(libs.wear.tooling.preview)
    //implementation (libs.wear.ongoing)
    //implementation(libs.watchface.complications.data.source.ktx)
    //implementation(libs.watchface)

    implementation(libs.play.services.wearable)


    // compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.core.splashscreen)
    implementation(libs.runtime.livedata)
    implementation(libs.material3.android)

    // tensorflow
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.select.tf.ops)
    implementation(libs.tensorflow.lite.gpu)

    implementation(libs.lifecycle.service)
    implementation(libs.lifecycle.livedata.ktx)

    // UI
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)

    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    //implementation(libs.tiles)
    //implementation(libs.tiles.material)
    //implementation(libs.horologist.compose.tools)
    //implementation(libs.horologist.tiles)
    //implementation(libs.horologist.composables)
    //implementation(libs.datastore.core.android)

    implementation(libs.accompanist.permissions)
    implementation (libs.mpandroidchart)
    implementation (libs.datastore.preferences)

    // Firebase
    implementation (libs.firebase.firestore.ktx) // Firestore

}


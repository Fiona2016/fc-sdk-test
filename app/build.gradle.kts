plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("cloud.flashcat.android-gradle-plugin") version "1.0.0"
}

flashcat {
    site = "STAGING"
    serviceName = "com.example.fc_sdk_test"
    versionName = "1.0.2"
}

android {
    namespace = "com.example.fc_sdk_test"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fc_sdk_test"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Use debug keystore for testing R8 minified builds
        create("release") {
            val debugKeystorePath = "${System.getProperty("user.home")}/.android/debug.keystore"
            storeFile = file(debugKeystorePath)
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation ("cloud.flashcat:fc-sdk-android-core:0.1.0")
    implementation ("cloud.flashcat:fc-sdk-android-logs:0.1.0")
    implementation ("cloud.flashcat:fc-sdk-android-rum:0.1.0")

    // FlashCat SDK
//    implementation(libs.flashcat.sdk.core)
//    implementation(libs.flashcat.sdk.logs)
//    implementation(libs.flashcat.sdk.rum)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
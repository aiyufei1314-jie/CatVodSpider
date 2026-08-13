plugins {
    alias(libs.plugins.android.application)
}


android {
    namespace = "com.github.catvod"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.github.catvod.demo"
        minSdk = 28
        targetSdk = 37
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}


dependencies {
    implementation(libs.okhttp3)
    implementation(libs.gson)
    implementation(libs.jsoup)
    implementation(libs.quickjs)
   // implementation(libs.javascriptengine)
}
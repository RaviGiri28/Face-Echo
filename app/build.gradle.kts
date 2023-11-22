plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.faceecho"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.faceecho"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility= JavaVersion.VERSION_1_8
        targetCompatibility= JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity:1.8.0")
    implementation ("com.google.android.material:material:1.10.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-auth:22.2.0")
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.google.android.gms:play-services-vision:20.1.3")
    implementation ("com.google.firebase:firebase-ml-vision:24.1.0")
        implementation ("androidx.camera:camera-core:1.4.0-alpha02")
        implementation ("androidx.camera:camera-camera2:1.4.0-alpha02")
        implementation ("androidx.camera:camera-lifecycle:1.4.0-alpha02")
    dependencies {
        // Other dependencies

        implementation ("androidx.core:core-ktx:1.12.0")


        implementation ("androidx.camera:camera-view:1.4.0-alpha02")
        implementation ("androidx.camera:camera-extensions:1.4.0-alpha02")
        // Other dependencies
    }

    // CameraX View class

    // If using the CameraX Extensions library, add the following dependency

}

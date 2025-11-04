plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.saamael.infinitylive"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.saamael.infinitylive"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

// --- DEPENDENCIAS DE FIREBASE ACTUALIZADAS ---

    // Importa el BOM (Bill of Materials) - ACTUALIZADO
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))

    // Auth - Ya no se usa el sufijo '-ktx'
    implementation("com.google.firebase:firebase-auth")

    // Firestore - Ya no se usa el sufijo '-ktx'
    implementation("com.google.firebase:firebase-firestore")

    // Google Sign-In - ACTUALIZADO
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")

    // --- AÑADE ESTA LÍNEA ---
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // ---

    implementation(libs.androidx.lifecycle.livedata.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
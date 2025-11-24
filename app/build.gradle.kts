plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.myapplicationv2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplicationv2"
        minSdk = 24
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Material icons
    implementation("androidx.compose.material:material-icons-extended")

    // Dagger-Hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-android-compiler:2.57.2")
    ksp("androidx.hilt:hilt-compiler:1.3.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    // Room
    val room_version = "2.8.2"
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:${room_version}")

    // Compose Destination
    val destinationVersion = "2.3.0"
    implementation("io.github.raamcosta.compose-destinations:core:$destinationVersion")
    ksp("io.github.raamcosta.compose-destinations:ksp:$destinationVersion")

    //fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.9.3")

    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // FusedLocationProvider
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // DataStore for home prefs
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
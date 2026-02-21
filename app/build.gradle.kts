plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    //
     // <-- ADDED to enable kapt()
    // <-- EDIT: ADDED to enable kapt

}



android {
    namespace = "com.example.tourist_main"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.tourist_main"
        minSdk = 26
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase BoM (controls all versions)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    // Firebase KTX versions (REQUIRED for Firebase.ktx)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    // Optional analytics
    implementation("com.google.firebase:firebase-analytics-ktx")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // weather
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Google Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Glide (image loading)
    implementation("com.github.bumptech.glide:glide:4.15.1")


    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")

    // CardView (if not already)
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("io.coil-kt:coil:2.4.0")

    // Qr code
    implementation("com.google.zxing:core:3.5.2")
    //
    implementation("org.maplibre.gl:android-plugin-annotation-v9:3.0.0")
    implementation("org.maplibre.gl:android-sdk:11.8.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    //grofence
    implementation("com.google.android.gms:play-services-location:21.0.1")


        // ... other dependencies
      implementation("io.radar:sdk:3.21.+")
    //implementation("io.radar:sdk-android:3.21.+")


    // img
    implementation("io.github.jan-tennert.supabase:supabase-kt:2.0.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.0.0")
    implementation("io.ktor:ktor-client-okhttp:2.3.4")




}
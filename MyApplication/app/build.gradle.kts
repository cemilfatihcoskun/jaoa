plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    alias(libs.plugins.kotlin.kapt)

    id("com.google.dagger.hilt.android") version "2.57" apply true

    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.sstek.jaoa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sstek.jaoa"
        minSdk = 30
        targetSdk = 35
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
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    // Dagger Hilt
    implementation("com.google.dagger:hilt-android:2.57")
    kapt("com.google.dagger:hilt-compiler:2.57")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.57")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.57")
    testImplementation("com.google.dagger:hilt-android-testing:2.57")
    kaptTest("com.google.dagger:hilt-compiler:2.57")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")


    // Jetpack Compose
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.navigation:navigation-compose:2.9.2")
    implementation("androidx.compose.ui:ui:1.8.3")

    // Jsoup
    implementation("org.jsoup:jsoup:1.21.1")

    // POI
    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")

    implementation("jp.wasabeef:richeditor-android:2.0.0")

    // Icons
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation("com.google.code.gson:gson:2.10.1")
}

kapt {
    correctErrorTypes = true
}

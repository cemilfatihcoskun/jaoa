plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.sstek.jaoa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sstek.jaoa"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.firebase.database.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-auth-ktx")

    implementation("io.github.webrtc-sdk:android:125.6422.07")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.guolindev.permissionx:permissionx:1.8.1")

    implementation("com.google.dagger:hilt-android:2.56.2")
    kapt("com.google.dagger:hilt-compiler:2.56.2")

    implementation("androidx.activity:activity-ktx:1.10.1")
    runtimeOnly("androidx.fragment:fragment-ktx:1.8.8")

    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("com.google.dagger:hilt-android-testing:2.56.2")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.56.2")
    testImplementation(kotlin("test"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    implementation("org.apache.xmlbeans:xmlbeans:5.3.0")
    implementation("org.apache.commons:commons-collections4:4.5.0")

    implementation("com.guolindev.permissionx:permissionx:1.8.1")

    implementation("com.google.dagger:hilt-android:2.56.2")
    kapt("com.google.dagger:hilt-compiler:2.56.2")


    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")

    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jsoup:jsoup:1.21.1")

    implementation("com.github.skydoves:colorpicker-compose:1.1.2")

    implementation("jp.wasabeef:richeditor-android:2.0.0")
    /*
    runtimeOnly("androidx.fragment:fragment-ktx:1.8.8")

    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("com.google.dagger:hilt-android-testing:2.56.2")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.56.2")
    testImplementation(kotlin("test"))
     */

}

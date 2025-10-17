plugins {
    // Version Catalog (libs) kullanıldığı varsayılmıştır
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    alias(libs.plugins.kotlin.kapt)

    // Hilt doğrudan versiyonla tanımlanmıştır
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

    // Java uyumluluğu Kotlin DSL'de `JavaVersion.VERSION_11` olarak ayarlanmıştır.
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
    // AndroidX & Temel Kütüphaneler
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

    // --- POI & Opensagres Bağımlılıkları (Çakışma Çözümü) ---
    // 1. poi-ooxml modülünden, çakışmaya neden olan lite versiyonunu ve potansiyel xmlbeans çekirdeğini hariç tutuyoruz.
    implementation("org.apache.poi:poi-ooxml:5.4.1") {
        // Kotlin DSL'de hariç tutma için `exclude` fonksiyonu kullanılır.
        exclude(group = "org.apache.poi", module = "poi-ooxml-lite")
        exclude(group = "org.apache.xmlbeans", module = "xml-schema-core")
    }
    // 2. Base POI
    implementation("org.apache.poi:poi:5.4.1")

    // 3. POI'nin gerektirdiği ek XMLBeans desteği için (Stabil versiyon pini)
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")

    // 4. curvesapi (POI transitif bağımlılığı) - SLF4J çakışmasını önlemek için
    implementation("com.github.virtuald:curvesapi:1.06") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    // AWT
    implementation("ro.andob.androidawt:androidawt:1.0.4")

    // Opensagres
    implementation("fr.opensagres.xdocreport:fr.opensagres.xdocreport.document:2.1.0")
    implementation("fr.opensagres.xdocreport:fr.opensagres.poi.xwpf.converter.pdf:2.1.0")
    //implementation("fr.opensagres.xdocreport:fr.opensagres.xdocreport.itext5.extension:2.1.0")
    //implementation("fr.opensagres.xdocreport:fr.opensagres.poi.xwpf.converter.pdf.itext5:2.1.0")
    //implementation("com.itextpdf:itextpdf:5.5.13.3")



    // Icons
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.google.accompanist:accompanist-flowlayout:0.36.0")

    //implementation("org.apache.pdfbox:pdfbox:3.0.5")

    //implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

// Kapt yapılandırması Kotlin DSL sözdizimine uygun hale getirilmiştir.
// Bu genellikle kapt eklentisi tarafından sağlanan bir uzantı bloğudur.
kapt {
    correctErrorTypes = true
}

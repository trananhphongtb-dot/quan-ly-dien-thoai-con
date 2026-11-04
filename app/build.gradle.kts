plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "vn.phong.parental"
    compileSdk = 36

    // Ký debug bằng key ổn định khi workflow khôi phục
    // app/parental-signing.jks và cấp mật khẩu qua biến môi trường.
    // Không có key thì Gradle dùng key debug mặc định như trước.
    val parentalKeystore = file("parental-signing.jks")
    val parentalStorePassword = System.getenv("SIGNING_STORE_PASSWORD")
    val parentalKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")
    val useParentalSigning =
        parentalKeystore.exists() && parentalStorePassword != null && parentalKeyPassword != null
    signingConfigs {
        create("parental") {
            if (useParentalSigning) {
                storeFile = parentalKeystore
                storePassword = parentalStorePassword
                keyAlias = "parental"
                keyPassword = parentalKeyPassword
            }
        }
    }
    buildTypes {
        debug {
            if (useParentalSigning) {
                signingConfig = signingConfigs.getByName("parental")
            }
        }
    }

    defaultConfig {
        applicationId = "vn.phong.parental"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-frame"
    }

    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Firebase thật — cần app/google-services.json mới build được.
    // BoM từ 34.0.0 trở đi không còn module -ktx, API KTX đã nằm
    // trong module chính, nên dùng tên không có -ktx.
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
}

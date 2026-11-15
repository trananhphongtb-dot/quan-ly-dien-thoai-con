plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "vn.phong.parental"
    compileSdk = 36

    // Ký debug luôn bằng cùng một key (app/debug.keystore, mật khẩu android).
    // Nhờ vậy dấu vân tay SHA-1 không đổi giữa các lần build trên GitHub và
    // chỉ cần đăng ký 1 lần trong Firebase cho Đăng nhập Google.
    // Đây là key thử nghiệm, KHÔNG dùng để phát hành lên Google Play.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Khóa tải lên Google Play (upload key). Giữ kỹ file app/upload.keystore và mật khẩu:
        // mất thì phải nhờ Google đặt lại khóa tải lên. Có thể ghi đè mật khẩu bằng biến
        // môi trường UPLOAD_KS_PASSWORD (ví dụ từ GitHub secret).
        create("release") {
            storeFile = file("upload.keystore")
            storePassword = System.getenv("UPLOAD_KS_PASSWORD") ?: "CaKzgKDBxKUcjJSPtP5cqE-n"
            keyAlias = "upload"
            keyPassword = System.getenv("UPLOAD_KS_PASSWORD") ?: "CaKzgKDBxKUcjJSPtP5cqE-n"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    defaultConfig {
        applicationId = "vn.phong.parental"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"
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
    // Đăng nhập Google kiểu mới (Credential Manager): lấy ID token mới mỗi
    // lần bấm, tránh token cũ/rỗng của cách GoogleSignIn cũ gây lỗi
    // "Given String is empty or null" khi đưa vào Firebase.
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
}

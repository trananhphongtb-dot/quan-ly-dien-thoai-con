plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false
    // Cần file app/google-services.json từ project Firebase của anh Phong
    // thì plugin này mới build được. Xem FIREBASE_SETUP.md.
    id("com.google.gms.google-services") version "4.4.2" apply false
}

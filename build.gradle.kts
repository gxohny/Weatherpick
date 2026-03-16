plugins {
    id("com.android.application") version "8.11.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    // Firebase Gradle 플러그인 명시적으로 버전 선언 + apply false (루트)
    id("com.google.gms.google-services") version "4.4.1" apply false
}

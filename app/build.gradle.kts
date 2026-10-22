plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val configuredApiBaseUrl = providers.gradleProperty("campusConnectApiBaseUrl")
    .orElse(providers.environmentVariable("CAMPUSCONNECT_API_BASE_URL"))
    .orNull
val debugApiBaseUrl = configuredApiBaseUrl ?: "http://10.0.2.2:3000/api/v1/"
val releaseApiBaseUrl = configuredApiBaseUrl ?: "https://api.example.invalid/api/v1/"

fun validateApiBaseUrl(value: String, requireHttps: Boolean) {
    val validScheme = if (requireHttps) value.startsWith("https://") else
        (value.startsWith("https://") || value.startsWith("http://"))
    if (!validScheme || !value.endsWith("/api/v1/")) {
        throw GradleException("API URL must use ${if (requireHttps) "HTTPS" else "HTTP or HTTPS"} and end with /api/v1/.")
    }
}

android {
    namespace   = "com.campusconnect.app"
    compileSdk  = 35

    defaultConfig {
        applicationId = "com.campusconnect.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", debugApiBaseUrl.asBuildConfigString())
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE_URL", releaseApiBaseUrl.asBuildConfigString())
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild") {
        doFirst {
            val apiUrl = configuredApiBaseUrl
                ?: throw GradleException("Set CAMPUSCONNECT_API_BASE_URL (or -PcampusConnectApiBaseUrl) before building a release.")
            validateApiBaseUrl(apiUrl, requireHttps = true)
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.core:core-ktx:1.15.0")

    // ── Networking (backend wiring) ─────────────────────────────────────
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ── Local persistence — auth token storage ──────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ── Real-time — WebSocket client for chat/notifications ─────────────
    implementation("io.socket:socket.io-client:2.1.1") {
        exclude(group = "org.json", module = "json")
    }

    // ── Security hardening ───────────────────────────────────────────────
    implementation("com.google.android.play:integrity:1.4.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
}

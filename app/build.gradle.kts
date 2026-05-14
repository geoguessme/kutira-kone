import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val envProperties = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}

fun envValue(name: String): String = (envProperties.getProperty(name) ?: System.getenv(name) ?: "").trim()

fun envString(name: String): String {
    val value = envValue(name)
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$value\""
}

android {
    namespace = "kutira.kone.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "kutira.kone.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", envString("SUPABASE_URL"))
        buildConfigField("String", "SUPABASE_KEY", envString("SUPABASE_KEY"))
        buildConfigField("String", "SUPABASE_BUCKET", envString("SUPABASE_BUCKET"))
        buildConfigField("String", "FIREBASE_API_KEY", envString("FIREBASE_API_KEY"))
        buildConfigField("String", "FIREBASE_PROJECT_ID", envString("FIREBASE_PROJECT_ID"))
        buildConfigField("String", "FIREBASE_APPLICATION_ID", envString("FIREBASE_APPLICATION_ID"))
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", envString("FIREBASE_STORAGE_BUCKET"))
        buildConfigField("String", "FIREBASE_GCM_SENDER_ID", envString("FIREBASE_GCM_SENDER_ID"))
        resValue("string", "default_web_client_id", envString("FIREBASE_WEB_CLIENT_ID"))
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
        buildConfig = true
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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)
    implementation(libs.osmdroid.android)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// release 簽章資訊放在 gitignore 的 keystore.properties（範本見 keystore.properties.example）。
// 檔案不存在時仍可 build，只是 release 產物不簽章 —— CI 或第一次 clone 不會因此失敗。
val keystoreProperties: Properties? = rootProject.file("keystore.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use { load(it) } } }

// 版本只改這一行。MAJOR：使用方式或 Play 申報內容改變；MINOR：任何使用者看得到的新東西；
// PATCH：只修行為不加 UI。Play 不接受重複的 versionCode，所以同一個版本號不能上傳兩次 ——
// 包錯了就升 PATCH 重包，不要想「同版本重傳」。
val appVersion = "1.0.0"

// versionCode 由 appVersion 算出（1.0.0 → 10000、1.2.3 → 10203），Console 上的數字能直接對回版本，
// 也不會出現 versionName 升了但 versionCode 忘了升。每段上限 99，超過就讓 build 失敗而不是默默溢位。
val appVersionCode = appVersion.split('.').map(String::toInt).also { parts ->
    require(parts.size == 3 && parts.all { it in 0..99 }) {
        "appVersion must be MAJOR.MINOR.PATCH with each part in 0..99, got \"$appVersion\""
    }
}.let { (major, minor, patch) -> major * 10_000 + minor * 100 + patch }

android {
    namespace = "com.wlworks.jog"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wlworks.jog"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
    }

    signingConfigs {
        keystoreProperties?.let { props ->
            create("release") {
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.location)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)

    // 本 App 不用 Fragment，但 play-services 會傳遞帶進舊版 androidx.fragment；
    // release 的 lintVitalRelease 會因此擋下 registerForActivityResult（InvalidFragmentVersionForActivityResult）。
    // 用 constraint 只抬版本、不新增相依。
    constraints {
        implementation(libs.androidx.fragment)
    }
}

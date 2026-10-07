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

// 廣告關卡的總開關，正式上架前維持 false。開法：gradle.properties 改 jog.ads=true，或命令列加 -Pjog.ads=true。
// false 時編 src/noAds/（不含任何廣告 SDK、不會多出 INTERNET / AD_ID 權限），true 時編 src/ads/ 並帶進 AdMob。
// 不用 product flavor 是為了讓 task 名稱維持 installDebug / bundleRelease，不必跟著改指令與文件。
val adsEnabled = providers.gradleProperty("jog.ads").map(String::toBoolean).getOrElse(false)
val adsSourceDir = if (adsEnabled) "src/ads" else "src/noAds"

// AdMob 的 App id 與獎勵廣告單元 id。
// - 正式 id 放在 gitignore 的 admob.properties（範本 admob.properties.example），**只給 release 用**。
//   repo 是公開的，廣告上線前不想讓 id 提早曝光；做法和 keystore.properties 一樣。
// - debug 一律用 Google 公開的測試 id —— 開發者用正式 id 在自己手機上看、點廣告會被 AdMob 判成
//   無效流量，輕則限制放送、重則停權，所以測試流程從設定上就碰不到正式 id。
// - 廣告開啟的 release 找不到正式 id 時 build 失敗（見 checkAdmobReleaseIds），不默默退回測試 id。
//   廣告關閉時不需要這個檔，clone 下來就能 build。
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestRewardedUnitId = "ca-app-pub-3940256099942544/5224354917"
val admobProperties: Properties? = rootProject.file("admob.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use { load(it) } } }
val admobAppId: String? = admobProperties?.getProperty("appId")?.trim()?.takeIf { it.isNotEmpty() }
val admobRewardedUnitId: String? =
    admobProperties?.getProperty("rewardedUnitId")?.trim()?.takeIf { it.isNotEmpty() }

android {
    namespace = "com.wlworks.jog"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wlworks.jog"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion

        // 預設（debug）用測試 id，release 在下面的 buildTypes 換成正式 id
        buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"$admobTestRewardedUnitId\"")
        manifestPlaceholders["admobAppId"] = admobTestAppId
    }

    sourceSets["main"].apply {
        kotlin.srcDir("$adsSourceDir/java")
        res.srcDir("$adsSourceDir/res")
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
        debug {
            // 手機上的封測版是 Play 重新簽過的，本機 build 簽章對不上、蓋不上去。
            // debug 版另起一個 applicationId 和它並存，實機測試才不必先移除封測版。
            applicationIdSuffix = ".debug"
        }
        release {
            // 缺 id 時先填測試 id 讓設定階段跑得完；真正打包前 checkAdmobReleaseIds 會擋下來
            buildConfigField(
                "String",
                "ADMOB_REWARDED_UNIT_ID",
                "\"${admobRewardedUnitId ?: admobTestRewardedUnitId}\""
            )
            manifestPlaceholders["admobAppId"] = admobAppId ?: admobTestAppId
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// 廣告開啟的 release 必須有正式 id。放在 task 執行階段檢查，而不是設定階段直接丟例外：
// 設定階段會評估所有 variant，那樣連 `-Pjog.ads=true installDebug` 也會因為沒有 admob.properties 而失敗。
val admobReleaseIdsMissing = adsEnabled && (admobAppId == null || admobRewardedUnitId == null)
val checkAdmobReleaseIds = tasks.register("checkAdmobReleaseIds") {
    // 先抄成區域變數：doLast 直接引用 script 頂層的 val 會讓 configuration cache 無法序列化
    val missing = admobReleaseIdsMissing
    doLast {
        if (missing) {
            throw GradleException(
                "jog.ads=true 的 release 需要正式的 AdMob id：把 admob.properties.example 複製成 " +
                    "admob.properties 並填入 appId 與 rewardedUnitId（或改回 jog.ads=false）。"
            )
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkAdmobReleaseIds) }

// AdMob 規定 manifest 要有 APPLICATION_ID，少了它 SDK 一初始化就崩潰。只在廣告開著時疊上去，
// 關著的 build 連這條 meta-data 都不會有。
androidComponents {
    onVariants { variant ->
        if (adsEnabled) variant.sources.manifests.addStaticManifestFile("src/ads/AndroidManifest.xml")
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
    implementation(libs.androidx.health.connect)
    if (adsEnabled) implementation(libs.play.services.ads)

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

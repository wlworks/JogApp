# Jog 實作步驟拆解

> 假設：從空專案開始逐步實作。已附的骨架 zip 當參考解答用 —— 卡住時再對照，不要一開始就整包貼上。
> 每一個 `S-` 步驟設計成可以單獨貼進 Code 頁面當一個 task，做完就能驗證。

## 怎麼用這份文件

- **一次做一步**。每步都有「驗收」，沒過就不要往下。
- **Phase 2 是整個專案的風險集中點**，排在 UI 之前。如果模擬定位在你的機器上根本推不動，後面做再多 UI 都是白工。
- 標 `⚠️` 的步驟是實作起來最容易卡住的，文件裡有對應的排雷說明。

## 依賴順序

```
Phase 0 專案基礎
    └─ Phase 1 純邏輯（可平行、可單測）
            └─ Phase 2 定位注入 ⚠️ 最高風險，先做
                    └─ Phase 3 狀態與移動迴圈
                            ├─ Phase 4 懸浮視窗 ⚠️
                            │       └─ Phase 5 UI 元件
                            └─ Phase 6 Geocoding（可與 4/5 平行）
                                    └─ Phase 7 權限引導頁
                                            └─ Phase 8 收尾與驗收
```

---

# Phase 0 · 專案基礎

## S0.1 建立專案與 Gradle 設定

**目標**：`./gradlew :app:assembleDebug` 能跑過。

**檔案**：`settings.gradle.kts`、`build.gradle.kts`、`app/build.gradle.kts`、`gradle/libs.versions.toml`、`gradle.properties`

**實作要點**
- namespace / applicationId：`com.wlworks.jog`
- `minSdk = 26`、`compileSdk = 36`、`targetSdk = 36`（Google Play 2026/8/31 起的新 App 門檻）
- 開 `buildFeatures { compose = true; buildConfig = true }`
- 套用 `kotlin-compose` plugin
- 相依：core-ktx、lifecycle-runtime-ktx、**lifecycle-service**、**lifecycle-viewmodel**、**savedstate-ktx**、activity-compose、compose BOM（ui / material3 / material-icons-extended）、coroutines-android、play-services-location
- 不需要網路相依：地點解析只用系統 Geocoder

**排雷**
- `lifecycle-viewmodel` 要**明確宣告**，不要靠 `lifecycle-viewmodel-compose` 傳遞進來。Phase 4 的 `setViewTreeViewModelStoreOwner` 住在那個 artifact，哪天相依被清掉就變 unresolved reference。
- AGP 8.9 需要 Gradle ≥ 8.11.1。

**驗收**：`./gradlew :app:assembleDebug` 成功，沒有 warning 以外的輸出。

---

## S0.2 Manifest 權限與 Application

**目標**：安裝後，App 會出現在「開發者選項 → 選取模擬位置資訊應用程式」的清單裡。

**檔案**：`AndroidManifest.xml`、`JogApp.kt`、`res/values/strings.xml`、`res/drawable/ic_pin.xml`

**實作要點**
- 權限：`ACCESS_MOCK_LOCATION`（配 `tools:ignore="MockLocation,ProtectedPermissions"`）、`ACCESS_FINE_LOCATION`、`ACCESS_COARSE_LOCATION`、`SYSTEM_ALERT_WINDOW`、`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_LOCATION`、`POST_NOTIFICATIONS`、`INTERNET`、`ACCESS_NETWORK_STATE`
- 先放一個空的 `MainActivity` 當 launcher，能開就好
- 通知字串與一個 vector icon（前景服務通知要用）

**排雷**：**沒有宣告 `ACCESS_MOCK_LOCATION`，App 不會出現在開發者選項清單裡**。這是整條路的入場券，先驗證這件事再往下。

**驗收**：`adb install` 後，手動到開發者選項確認清單中看得到 Jog，並選起來。

---

# Phase 1 · 純邏輯

這一階段完全不碰 Android API，全部可以用 JVM 單元測試驗證。

## S1.1 LatLng 與大圓航線推算

**目標**：給起點、方位角、距離，算出終點座標。

**檔案**：`core/GeoMath.kt`

**實作要點**
- `data class LatLng(lat: Double, lng: Double)` 含 `isValid()`
- `destination(from, bearingDeg, distanceM)`：球面 destination point 公式，地球半徑取 6,371,008.8 m
- 經度結果要正規化回 −180..180（`((deg + 540) % 360) - 180`）
- `distanceM == 0.0` 直接回原點，省掉三角函數

**驗收**（單元測試）
- 從 (25.0330, 121.5654) 往正東（90°）走 1000 m：經度增加 **≈ 0.00992°**，緯度變化 **< 0.00001°**
- 往正北（0°）走 1000 m：經度不變，緯度增加 **≈ 0.00899°**
- 距離 0 回傳原座標
- 跨換日線：從 (0, 179.99) 往東走 5000 m，結果經度應為負值（約 −179.96），不是 180.04

---

## S1.2 搖桿向量換方位角

**目標**：把螢幕座標系的 (x, y) 轉成正北為 0、順時針遞增的方位角。

**檔案**：`core/GeoMath.kt`

**實作要點**：`atan2(x, -y)` 轉度數後 `+360 % 360`。注意螢幕的 y 軸向下為正，所以要取負。

**驗收**（單元測試）

| 輸入 (x, y) | 期望 |
|---|---|
| (0, −1) | 0°（北） |
| (1, 0) | 90°（東） |
| (0, 1) | 180°（南） |
| (−1, 0) | 270°（西） |
| (1, −1) | 45° |

---

## S1.3 速度檔次

**檔案**：`core/SpeedTier.kt`

**實作要點**：enum，每檔帶 `@StringRes labelRes` 與 m/s。**label 不要寫死文字**，UI 用 `stringResource(tier.labelRes)` 取。建議六檔：Creep 0.5、Walk 1.4、Run 3.0、Bike 6.0、Drive 16.7、Rail 83.0。附一個 `DEFAULT` 與 `next()` 方便之後做快捷切換。

**驗收**：能編譯；`SpeedTier.entries` 順序是由慢到快。

---

## S1.4 座標字串解析

**目標**：判斷使用者輸入是座標還是地名。

**檔案**：`core/CoordinateParser.kt`

**實作要點**
- 十進位：`25.0330, 121.5654`、`25.033 121.5654`、含正負號
- DMS：`25°2'0"N 121°33'0"E`
- 解析不出來或超出範圍一律回 `null`，交給 geocoder

**驗收**（單元測試）
- `"25.033, 121.5654"` → 解析成功
- `"25.033 121.5654"`（空白分隔）→ 解析成功
- `"-33.8688, 151.2093"` → 解析成功
- `"25°2'0\"N 121°33'0\"E"` → lat ≈ 25.0333、lng ≈ 121.55
- `"台北101"` → `null`
- `"200, 300"`（超出範圍）→ `null`
- `""`、`"abc"` → `null`

---

## S1.5 不吞取消的 runCatching

**檔案**：`core/Cancellable.kt`

**實作要點**：`runCatchingCancellable`，先 catch `CancellationException` 原樣拋出，再 catch `Throwable` 包成 `Result.failure`。之後所有 suspend 函式裡的 `runCatching` 都換成這個。

**排雷**：標準庫的 `runCatching` 會把 `CancellationException` 當一般失敗吞掉。Service 關閉導致 scope 取消時，這會讓取消訊號消失、破壞結構化並行。

**驗收**：寫一個測試，在已取消的 scope 裡呼叫，確認例外有往上拋。

---

# Phase 2 · 定位注入 ⚠️

**整個專案最高風險的一段，一定要先做完並在實機上驗證通過，再碰任何 UI。**

## S2.0 臨時測試載具

**目標**：一個最陽春的 Activity，兩顆按鈕：「定位到台北101」「停止」。純粹為了驗證 Phase 2，做完 Phase 5 之後刪掉。

**檔案**：`MainActivity.kt`（暫時版）

**驗收**：按鈕能按，log 有印東西。

---

## S2.1 註冊 test provider

**目標**：`MockLocationEngine.start()` 能成功註冊 test provider。

**檔案**：`mock/MockLocationEngine.kt`

**實作要點**
- providers 清單：`GPS_PROVIDER`、`NETWORK_PROVIDER`，API 31+ 再加 `FUSED_PROVIDER`
- 每個 provider 先 `removeTestProvider`（吞例外）再 `addTestProvider`，避免重複註冊殘留屬性
- **API 31+ 用 `ProviderProperties` 那個 overload；以下用舊的 int 版本**
- 註冊完要 `setTestProviderEnabled(provider, true)`

**排雷**
- `android.location.provider.ProviderProperties` 與 `LocationManager.FUSED_PROVIDER` 都是 **API 31**，不是 30。版本判斷寫錯會在 Android 11 機器上 `NoClassDefFoundError`。
- 沒被選為模擬位置 App 時，`addTestProvider` 丟 `SecurityException`。

**驗收**：`adb shell dumpsys location | grep -i "test"` 看得到註冊的 test provider。

---

## S2.2 組裝 Location 並推送 ⚠️

**目標**：`push()` 一次，手機定位真的跳過去。

**檔案**：`mock/MockLocationEngine.kt`

**實作要點**：`Location(provider)` 上必須設好
- `latitude` / `longitude` / `altitude` / `accuracy`
- `bearing` / `speed`
- `time = System.currentTimeMillis()`
- **`elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()`**
- API 26+ 再補 `verticalAccuracyMeters` / `bearingAccuracyDegrees` / `speedAccuracyMetersPerSecond`

**排雷**：**漏掉 `elapsedRealtimeNanos`，Android 8+ 會直接丟棄整筆 Location，而且完全不報錯。** 症狀是「程式跑得好好的，log 也正常，但定位一動也不動」。這一條卡住過無數人。

**驗收**：按下測試按鈕後，用一個走 `android.location` 的 App（例如 GPS Test、或系統設定裡的定位資訊）確認座標變成台北101。

---

## S2.3 Fused 路徑

**目標**：Google Maps 也跟著騙到。

**檔案**：`mock/MockLocationEngine.kt`

**實作要點**
- `FusedLocationProviderClient.setMockMode(true)` 在 `start()`、`setMockMode(false)` 在 `stop()`
- `push()` 裡額外呼叫 `setMockLocation(location)`
- 兩條路徑各自包 `runCatching` + log，其中一條失敗不要讓另一條也掛掉

**排雷**：走 `android.location` 和走 Play Services 是**兩套互不相通的供應鏈**。只做 S2.2 的話，Google Maps 會顯示真實位置。

**驗收**：開 Google Maps，按測試按鈕，藍點跳到台北101。

---

## S2.4 錯誤收斂與停止

**目標**：權限沒給時給得出可行動的訊息，停止時完整復原。

**檔案**：`mock/MockLocationEngine.kt`

**實作要點**
- `sealed interface StartResult { Ok / NotMockApp / Failed(throwable) }`
- `SecurityException` → `NotMockApp`，其餘 → `Failed`
- `stop()`：`setMockMode(false)` + 每個 provider `setTestProviderEnabled(false)` + `removeTestProvider`，全部包 `runCatching`

**排雷**：**沒有正確 `removeTestProvider`，手機定位會卡在假座標**，使用者得重開機或手動取消勾選才救得回來。這是最容易被客訴的 bug。

**驗收**
- 到開發者選項取消勾選 Jog，按測試按鈕 → 拿到 `NotMockApp`，不崩
- 按「停止」後，Google Maps 回到真實位置

---

# Phase 3 · 狀態與移動迴圈

## S3.1 共用狀態

**目標**：Service 與 overlay UI 共用同一份狀態，不經過 Activity。

**檔案**：`state/MockState.kt`

**實作要點**
- `StickInput(x, y)` 帶 `magnitude`（`hypot` 後 clamp 到 1）與 `isIdle`（< 0.05）
- `MockUiState`：running、current、query、searching、speedTier、stick、bearing、message、needsMockAppSetup
- `MockStateHolder` 用 object + `MutableStateFlow`，提供 `setStick` / `setSpeed` / `setQuery` / `message`

**驗收**：能編譯；從不同執行緒更新不會出問題（`MutableStateFlow` 本身 thread-safe）。

---

## S3.2 移動迴圈 ⚠️

**目標**：推桿時座標連續移動，放手就停。

**檔案**：`mock/MovementController.kt`

**實作要點**
- 100 ms 一個 tick（10 Hz）
- 每個 tick：讀 stick → 算 bearing → `speedTier.mps × throttle` → `GeoMath.destination(position, bearing, speed × 0.1)` → `engine.push(...)`
- throttle 用 `magnitude.coerceIn(0.15f, 1f)`，避免微推完全不動
- **stick idle 時速度算 0，但照樣 push**
- `teleport(target)` = 以新座標為起點重啟迴圈

**排雷**
- **idle 時不推送，幾秒後消費端就會判定定位過期、退回真實位置。** 一定要照推。
- 迴圈跑 `Dispatchers.Default`，不要用 `lifecycleScope` 的預設 Main —— 每個 tick 是 4 次 binder call。
- UI 狀態每 3 個 tick 才更新一次；面板一秒重組 10 次沒有意義。

**驗收**：在測試 Activity 裡硬寫 `stick = (0, -1)`、`speedTier = WALK`，開 Google Maps 看藍點以步行速度穩定往北移動；把 stick 設回 (0,0) 後藍點立刻停住。

---

# Phase 4 · 懸浮視窗 ⚠️

## S4.1 Compose overlay 宿主 ⚠️

**目標**：在 Service 裡把一個 ComposeView 掛到 WindowManager 上不崩。

**檔案**：`service/OverlayHost.kt`

**實作要點**
- class 同時實作 `LifecycleOwner`、`ViewModelStoreOwner`、`SavedStateRegistryOwner`
- 內含 `LifecycleRegistry`、`ViewModelStore`、`SavedStateRegistryController`（建構時 `performRestore(null)`）
- `show()`：建 `ComposeView` → `setViewTreeLifecycleOwner` / `setViewTreeViewModelStoreOwner` / `setViewTreeSavedStateRegistryOwner` → `setContent` → `windowManager.addView`
- lifecycle 從 `CREATED` 推到 `RESUMED`
- LayoutParams：`TYPE_APPLICATION_OVERLAY`、`WRAP_CONTENT`、`TRANSLUCENT`、`Gravity.TOP or START`

**排雷**
- **ComposeView attach 時會去 ViewTree 上找那三個 owner，Service 裡沒有 Activity 提供，缺任一個就 `IllegalStateException`。** 這是這條路上最常見的崩潰。
- `dismiss()` 要先 removeView、再把 lifecycle 設 `DESTROYED`、再 `store.clear()`。一旦 DESTROYED 就**不能再 `show()`**，`LifecycleRegistry` 會丟例外 —— 一個 host 實例只用一次。

**驗收**：Service 裡 `show { Text("hello") }`，實機上看到浮在其他 App 之上的文字，不崩。

---

## S4.2 拖曳移動

**檔案**：`service/OverlayHost.kt`

**實作要點**：`moveBy(dx, dy)` 改 `layoutParams.x/y` 後 `windowManager.updateViewLayout`。Compose 那側用 `Modifier.pointerInput { detectDragGestures { change, delta -> change.consume(); moveBy(delta.x, delta.y) } }`，當成 Modifier 傳進面板。

**驗收**：手指拖著標題列，視窗跟著走，放手停在原地。

---

## S4.3 鍵盤焦點切換 ⚠️

**目標**：overlay 上的輸入框點了能叫出鍵盤，不打字時不干擾底層 App。

**檔案**：`service/OverlayHost.kt`

**實作要點**
- 平常 flags：`FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_NO_LIMITS or FLAG_WATCH_OUTSIDE_TOUCH`
- 打字時：把 `FLAG_NOT_FOCUSABLE` 拿掉，並設 `softInputMode = SOFT_INPUT_ADJUST_PAN`
- 提供 `setInputFocusable(Boolean)`，改完 flags 後 `updateViewLayout`
- Compose 那側用 `Modifier.onFocusChanged { setInputFocusable(it.isFocused) }` 呼叫

**排雷 1**：**這是兩難。** 帶 `FLAG_NOT_FOCUSABLE` 就叫不出鍵盤；不帶就會吃掉返回鍵、讓底下的 App 完全沒法操作。唯一解是動態切換。

**排雷 2 ⚠️ 會做出「輸入一次之後就再也不能編輯」的 bug**：不要在 Service 裡直接呼叫 `setInputFocusable(false)`。那樣只關掉了視窗焦點，Compose 那側的 `TextField` 仍以為自己持有焦點 —— 使用者再點輸入框時 `onFocusChanged` **不會再觸發**，視窗就永遠回不到 focusable，看起來就是輸入框壞掉。

正確做法：讓 `onFocusChanged` 成為**唯一**改動視窗旗標的路徑。需要程式化收鍵盤時（搜尋送出、收合面板）改成清除 Compose 焦點，讓 `onFocusChanged(false)` 自然發生。

**排雷 3**：承載這段邏輯的 `LaunchedEffect` 要放在**收合分支之外**。放在面板裡的話，收合時整棵子樹被移除，effect 是被丟棄而不是以新 key 重啟，收合路徑就變成死碼。

**排雷 4**：使用者可能聚焦輸入框後直接去推搖桿或切速度檔。那些控制項的 callback 要先收鍵盤，否則視窗停在 focusable 持續吃返回鍵。用一個面板層級的 `queryFocused` 旗標做守衛，沒聚焦時是 no-op，搖桿每幀呼叫也不會有成本。

**驗收**
- 點輸入框 → 鍵盤跳出來，能打字
- **送出搜尋後再點一次輸入框 → 鍵盤要能再次跳出來**（這條會抓到排雷 2）
- 收合成泡泡再展開 → 輸入框仍可編輯（這條會抓到排雷 3）
- 聚焦輸入框後直接推搖桿 → 鍵盤收起
- 收起鍵盤 / 收合面板 → 按返回鍵，作用在底層 App 而不是 overlay

---

## S4.4 前景服務

**目標**：切到背景、鎖螢幕再回來，overlay 和模擬都還在。

**檔案**：`service/FloatingWindowService.kt`

**實作要點**
- 繼承 `LifecycleService`（才有 `lifecycleScope` 給 MovementController 用）
- 建立 `NotificationChannel`（IMPORTANCE_LOW）+ 常駐通知，帶「停止」action 指回自己的 `ACTION_STOP`
- API 29+ 用 `startForeground(id, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)`
- 靜態 `start(context)` / `stop(context)` 方便呼叫

**驗收**：啟動後把 App 切到背景、鎖螢幕 30 秒再解鎖，overlay 還在、座標還在動。

---

## S4.5 啟動防護

**目標**：權限被事後撤銷時不會無限崩潰重啟。

**檔案**：`service/FloatingWindowService.kt`

**實作要點**
- `onCreate` 裡 `startForeground` **之前**先查 `Settings.canDrawOverlays()` 與 `ACCESS_FINE_LOCATION`，任一不過就 `stopSelf(); return`
- `startForeground` 包 `runCatching`，失敗也 `stopSelf()`
- `onDestroy`：`movement.stop()` → `engine.stop()` → `overlay.dismiss()`

**排雷**：`START_STICKY` 的回馬槍 —— 使用者事後撤銷定位權限 → 程序被殺 → 系統自動重啟 service → Android 14 上 `startForeground(TYPE_LOCATION)` 直接 `SecurityException` → **反覆崩潰重啟**。

**驗收**：服務跑著的時候到系統設定撤銷定位權限，服務應該安靜地自己停掉，不進崩潰迴圈。

---

# Phase 5 · UI 元件

## S5.1 搖桿

**檔案**：`ui/Joystick.kt`

**實作要點**
- `Canvas` 畫外圈、十字參考線、旋鈕
- `pointerInput` + `detectDragGestures`，旋鈕位移用 `clampToCircle` 限制在半徑 72% 內
- 回報正規化向量（除以 maxTravel）
- `onDragEnd` / `onDragCancel` 一律歸零，**不做慣性**

**驗收**
- 旋鈕不會被拖出圓外
- 放手立刻回中心且回報 (0, 0)
- 按住不動時方向維持不變

---

## S5.2 速度選擇器

**檔案**：`ui/SpeedSelector.kt`

**實作要點**：一排 pill，選中的填 accent 色 + 粗體。六個檔次在 268dp 寬的面板裡要塞得下，字級 11sp、左右 padding 9dp。

**驗收**：點擊切換，選中狀態明顯；在最窄的面板寬度下不會被切掉。

---

## S5.3 面板組裝

**檔案**：`ui/FloatingPanel.kt`

**實作要點**
- 標題列（同時是拖曳把手）+ 收合 / 關閉按鈕
- 地點輸入框（`imeAction = Search`，`onFocusChanged` 回報）
- 「定位到這裡」按鈕 + 搜尋中轉圈
- 目前座標顯示（`%.6f, %.6f · %.0f°`）
- 速度選擇器 → 搖桿 → 開始/停止按鈕 → 錯誤訊息
- 另外做 `CollapsedBubble`：52dp 圓球，點一下展開

**驗收**：所有元件在 268dp 寬度下排得下，深色底上文字都讀得清楚。

---

## S5.4 接線

**檔案**：`service/FloatingWindowService.kt`

**實作要點**
- 用 `PanelActions` data class 把所有 callback 一次傳下去，不要傳七八個 lambda 參數
- `collapsed` 用 `remember { mutableStateOf(false) }` 存在 composable 裡
- 收合時主動 `setInputFocusable(false)`
- 刪掉 S2.0 的臨時測試 Activity

**驗收**：搖桿推動 → 座標顯示跟著跳 → Google Maps 藍點同步移動。切速度檔立即生效。

---

## S5.5 字串外部化與語系

**目標**：預設英文，支援繁體中文，使用者可在系統設定裡單獨切換 Jog 的語言。

**檔案**：`res/values/strings.xml`（英文）、`res/values-zh-rTW/strings.xml`、`res/xml/locales_config.xml`、`AndroidManifest.xml`

**實作要點**
- 所有使用者可見字串進 `values/`（英文為預設），繁中放 `values-zh-rTW/`
- `app_name` 與純數字格式字串標 `translatable="false"`
- Manifest 加 `android:localeConfig="@xml/locales_config"`，Android 13+ 就會出現每應用程式語言選單
- `locales_config` 的中文寫 `zh-Hant-TW`（只寫 `zh-TW` 的話，系統語系是 `zh-Hant-HK` 或純 `zh-Hant` 時比不到）
- 純符號按鈕（`—`、`✕`）補 `contentDescription`，否則 TalkBack 唸不出來

**排雷 ⚠️ 座標格式化不能跟著語系走**：`stringResource(id, args)` 與 Kotlin 的 `String.format` 都用系統語系。法語、德語會把小數點印成逗號，和座標本身的逗號分隔符撞在一起，變成 `25,033000, 121,565000` 完全無法閱讀。座標一律 `String.format(Locale.US, ...)`。

**驗收**
- 系統語系英文 → 全英文；切繁中 → 全中文；切日文（無翻譯）→ fallback 到英文
- 把裝置語系切到法文或德文，座標顯示仍是 `25.033000, 121.565400 · 90°`
- Android 13+ 的「應用程式語言」設定裡看得到 Jog 且可單獨切換

---

# Phase 6 · Geocoding

## S6.1 解析優先序骨架

**檔案**：`data/GeocodeRepository.kt`

**實作要點**：`resolve(query)` 先 trim，空字串回空清單；先試 `CoordinateParser`，中了就直接包成 `GeocodeHit` 回傳（離線可用、最快）。

**驗收**：輸入 `25.033, 121.5654`，飛航模式下也能定位過去。

---

## S6.2 系統 Geocoder

**檔案**：`data/GeocodeRepository.kt`

**實作要點**
- `Geocoder.isPresent()` 為 false 時直接回空
- API 33+ 用 `GeocodeListener` 非同步版，包 `suspendCancellableCoroutine`
- 以下用同步版，**必須丟 `Dispatchers.IO`**
- 整段包 `withTimeoutOrNull(4000)`

**排雷**：`Geocoder` 在部分無 GMS 或特定地區的機型即使 `isPresent()` 回 true 也可能長時間沒回應，沒有 timeout 就是 UI 卡住。

**驗收**：輸入「台北101」能定位過去；輸入亂碼回空、不卡。

---

## S6.3 裝置能力判斷

**目標**：這台裝置沒有 geocoding backend 時，給得出可行動的提示。

**檔案**：`data/GeocodeRepository.kt`

**實作要點**
- 用 `sealed interface Outcome { Found / NoMatch / Unavailable }` 取代 `Result<List<…>>`
- `Geocoder.isPresent()` 為 false，或 4 秒逾時 → `Unavailable`
- 對外開一個 `canSearchByName` 屬性，UI 可以據此調整輸入框的 placeholder

**排雷**：`NoMatch`（查得動但沒結果）與 `Unavailable`（根本查不動）要分開。
前者叫使用者換個地名，後者只能叫他改輸入座標 —— 混在一起會讓人一直重打地名。

**驗收**：在沒有 GMS 的模擬器或 `Geocoder.isPresent()` 回 false 的環境下，訊息是「請直接輸入經緯度」而不是「找不到」。

---

## S6.4 錯誤訊息

**檔案**：`service/FloatingWindowService.kt`

**實作要點**：`NoMatch` → 「找不到『xxx』」；`Unavailable` → 「這台裝置無法搜尋地名，請直接輸入經緯度」；`NotMockApp` → 「請到開發者選項選擇 Jog」。搜尋送出時主動收鍵盤。

**排雷**：查詢成功後把結果 label 回填到輸入框時，**座標來源的結果不要回填**。座標 label 帶有「座標 / Coordinates」前綴，寫回去之後再按一次搜尋，`CoordinateParser` 會因為前綴而解析失敗，整串被丟去查地名，必定 NoMatch。在 `GeocodeHit` 上加一個 `fromCoordinates` 旗標區分。

**驗收**
- 三種錯誤各觸發一次，訊息都出現在面板上且可行動
- 輸入座標搜尋 → 再按一次搜尋 → 仍然成功（這條會抓到上面的排雷）

---

# Phase 7 · 權限引導頁

## S7.1 四道關卡 checklist

**檔案**：`MainActivity.kt`

**實作要點**
- 四列，各自顯示狀態圖示（✓ / ! / →）與點擊行為
- 懸浮視窗：`ACTION_MANAGE_OVERLAY_PERMISSION` + `package:` Uri
- 定位、通知：`RequestMultiplePermissions`
- 開發者選項：`ACTION_APPLICATION_DEVELOPMENT_SETTINGS`，失敗 fallback 到 `ACTION_SETTINGS`
- 用一個 `mutableStateOf` 的 tick 在 `onResume` 遞增，強迫重查權限狀態

**排雷**：`SYSTEM_ALERT_WINDOW` **不能**用 runtime dialog 要，只能跳系統設定；第四道關卡**沒有任何 API 可以代勞**，只能引導。

**驗收**：四項全部走一遍，回到 App 時勾勾狀態正確更新。

---

## S7.2 啟動控制

**檔案**：`MainActivity.kt`

**實作要點**：前三項都過才 enable「啟動懸浮視窗」；「關閉懸浮視窗」永遠可按。

**驗收**：缺任一權限時啟動鈕是 disabled 狀態。

---

# Phase 8 · 收尾

## S8.1 完整復原檢查

**驗收清單**
- 面板按 ✕ → overlay 消失、通知消失、Google Maps 回真實位置
- 通知上的「停止」→ 同上
- 從最近工作清單滑掉 App → 同上
- `adb shell dumpsys location | grep -i test` 應該查不到殘留的 test provider

## S8.2 補測試

`GeoMath`、`CoordinateParser`、`SpeedTier` 三個純邏輯檔的單元測試補齊（Phase 1 的驗收條件直接變成測試案例）。

## S8.3 實機驗收

- [ ] 走 `android.location` 的 App 定位正確
- [ ] 走 Play Services 的 App（Google Maps）定位正確
- [ ] 消費端讀得到非零的 bearing 與 speed，不只是座標在跳
- [ ] 放開搖桿座標立刻停住，不飄
- [ ] 切背景 / 鎖螢幕 / 解鎖後模擬仍在運作
- [ ] 停止後定位完整復原
- [ ] 六個速度檔次移動速率明顯有別
- [ ] 面板在最小寬度手機上排版正常

---

## 建議的 commit 切點

一個 Phase 一個 commit，Phase 2 和 Phase 4 因為風險高各自再切細一點：

```
feat: 專案骨架與權限宣告              (S0)
feat: 座標運算與解析                  (S1)
feat: test provider 註冊與座標推送     (S2.1–S2.2)
feat: fused 路徑注入                  (S2.3–S2.4)
feat: 移動控制迴圈                    (S3)
feat: Compose overlay 宿主            (S4.1–S4.3)
feat: 前景服務                        (S4.4–S4.5)
feat: 懸浮面板 UI                     (S5)
feat: 地名搜尋                        (S6)
feat: 權限引導頁                      (S7)
test: 補上純邏輯單元測試              (S8)
```

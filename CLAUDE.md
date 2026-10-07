# CLAUDE.md

給在這個 repo 工作的 AI agent 的指引。檔案地圖與設計取捨見 [README.md](README.md)。

## Coding 格式

所有 `app/src/main/java/` 底下的 Kotlin 檔一律遵守下列四條：

1. **`companion object` 放在宣告區塊的最前面**，在其他所有成員之前。
2. **變數依字母排序**（大小寫不敏感）。
3. **函式依字母排序**（大小寫不敏感），且**每個函式上方都要有一行簡短描述**。
4. **常數上方也要有註解**。

排序的範圍是**同一個宣告區塊**，各自獨立互不混排：

- top-level 宣告
- class / object / interface 的主體
- companion object 的主體
- 主建構子的參數列（`data class` 的 `val` 也算）
- 函式主體內的區域變數**不受規則約束**

排序刻意用大小寫不敏感 —— 用 ASCII 排的話，所有大寫開頭的 Composable 會整批被推到私有 helper 前面，失去意義。

因為建構子參數也排序，`data class` 的參數順序會和語意順序脫鉤，**呼叫端一律用具名引數**，不要用位置引數。

### 已知例外

`SpeedTier` 破了規則 1 與 2，原因寫在它的 KDoc 裡，不要「順手修正」：

- entries 依速度由慢到快排，不依字母 —— `next()` 靠 `ordinal` 遞增，UI 也依這個順序呈現。
- `companion object` 只能放在 entries 之後 —— Kotlin 規定 enum entries 必須是主體第一個元素。

新增例外時，一律在該處留下說明原因的註解。

### 驗證

改完跑一次，不要只靠目測 —— 排序很容易漏，特別是巢狀 scope：

```bash
python tools/check_style.py
```

上面四條規則它都會查，違規回傳 1。

`SpeedTier` 的兩個例外之所以過關，是因為腳本**完全不解析 enum entries** ——
不是有意豁免，只是剛好看不到。之後若加了別的例外，腳本大概會擋下來，
到時要嘛改腳本、要嘛重新考慮那個例外是否站得住腳。

## Commit message

格式如下，**不要**加 `Co-Authored-By` 尾行，**不要**寫裝置型號／OS 版本／螢幕規格：

```
[JogApp] 一行簡短說明

[Description]
做了什麼、為什麼這樣做。

[Test]
實際驗證過什麼。沒驗到的寫進 Not covered，不要含糊帶過。
```

開發過程中的來回修正不必寫進去，只描述最終狀態。

## 建置與測試

```bash
./gradlew :app:testDebugUnitTest    # 單元測試
./gradlew :app:installDebug         # 裝到已連線的裝置
./gradlew :app:bundleRelease        # 簽章的 release AAB；簽章讀 keystore.properties（gitignore），沒有就不簽
./gradlew :app:installDebug -Pjog.ads=true   # 含 AdMob 的 build；預設（jog.ads=false）不含廣告 SDK
```

`jog.ads` 決定編 `app/src/noAds/` 還是 `app/src/ads/`（各有一份 `RewardedGate` 與 `setup_purpose` 字串）。
改 `RewardedGate` 的公開介面時兩份要一起改，並且**兩種 build 都編一次**。`check_style.py` 只掃 `src/main/java/`，
這兩個目錄要自己照同樣的格式寫。出貨與封測的版本一律用預設值，不要把 `jog.ads=true` commit 進 `gradle.properties`。

AdMob 正式 id 在 gitignore 的 `admob.properties`（範本 `admob.properties.example`），**不要寫進任何會 commit 的檔案**
（repo 是公開的）。debug 不讀它、一律用測試 id；廣告開啟的 release 缺這個檔會 build 失敗，這是刻意的。

單元測試只涵蓋 `core/`（`GeoMath`、`CoordinateParser`、`AutoRoam`、`HealthTally` 等純邏輯）。`MockLocationEngine`、
`MovementController`、`HealthConnectStore`、廣告關卡與 Compose UI 沒有自動化覆蓋，改動這些要實機驗證。

編譯時 AGP 8.9.1 會警告 `compileSdk = 36` 超出測試範圍，這是已知的，不影響建置。

## 實機測試

debug 版的 applicationId 是 **`com.wlworks.jog.debug`**（桌面上叫 Jog Debug），和 Play 裝的封測版
`com.wlworks.jog` 並存。封測版是 Play 重新簽過的，本機 build 不論 debug 或 release 都蓋不上去
（`INSTALL_FAILED_UPDATE_INCOMPATIBLE`），所以實機測試一律對 debug 套件名下指令，不要去動封測版。

測試用的權限全部可以用 adb 給，**不需要**手動點開發者選項：

```bash
adb shell appops set com.wlworks.jog.debug SYSTEM_ALERT_WINDOW allow
adb shell appops set com.wlworks.jog.debug android:mock_location allow
adb shell pm grant com.wlworks.jog.debug android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.wlworks.jog.debug android.permission.POST_NOTIFICATIONS
adb shell pm grant com.wlworks.jog.debug android.permission.health.WRITE_STEPS      # Health 同步才需要
adb shell pm grant com.wlworks.jog.debug android.permission.health.WRITE_DISTANCE
```

用 adb 給 appop 不會動到封測版的設定（實測兩個套件可以同時是 allow）；開發者選項的選單才是單選。

設定畫面第四列（開發者選項）永遠顯示箭頭而非打勾，那是 `granted = null` 的
刻意設計 —— App 偵測不到那個選擇，不是 bug。

驗證注入是否生效看 `adb shell dumpsys location`，`network` / `fused` / `gps`
三個 provider 都該標上 `[mock]`。

幾個踩過的坑：

- **`adb shell input text` 全速送字會讓 Compose 輸入框吃到亂序的字**。分段送、
  每段之間留 0.5 秒才會正確。看到亂碼先懷疑這個，不要當成 App 的 bug。
- **`input motionevent DOWN` 之後指標還按著**，中間插入的 `input tap` 不會被
  當成點擊。要按別的東西前先送 `UP`。
- **force-stop 會讓 test provider 留在系統裡**，裝置定位繼續卡在假座標。
  `JogApp.onCreate` 的 `clearStaleProviders()` 會在下次開啟 App 時清掉，但
  行程不在的那段期間沒救。測完要還原裝置就走 App 內的 Stop，或：

  ```bash
  adb shell appops set com.android.shell android:mock_location allow
  adb shell cmd location providers remove-test-provider gps
  adb shell appops set com.android.shell android:mock_location default
  ```

- **Health 同步寫進去的資料只能到 Health Connect 自己的畫面看**（Jog 沒有讀取權限）：
  `adb shell am start -a android.health.connect.action.HEALTH_HOME_SETTINGS` → Data and access →
  Steps / Distance，來源會標 Jog Debug。每 60 秒才寫一次，關掉同步或關面板會立刻補寫。
  測試資料會留在裝置的 Health Connect 裡，要清掉得在同一個畫面手動刪。
- **測廣告關卡要用 `-Pjog.ads=true` 的 debug build，手機要連得上網**。debug 一律用 Google 的測試 id；
  **不要在自己的手機上跑含廣告的 release build**，那會用正式 id，看、點廣告都算無效流量。Google 的測試獎勵廣告大約 5 秒就給獎勵，
  之後按返回算「看完」；要測「沒看完」得在廣告出現後 1 秒內按返回。測沒網路用
  `adb shell svc wifi disable`，測完記得 `enable`。測完把手機裝回預設（不含廣告）的 build。
- 測繁中不必動系統語系，用 per-app locale：
  `adb shell cmd locale set-app-locales com.wlworks.jog.debug --locales zh-TW`，
  還原傳空字串。

測完請把裝置還原：mock provider 清空、`mock_location` appop 設回 `default`。

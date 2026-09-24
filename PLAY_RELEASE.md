# Jog · Google Play 上架檢查清單

> 更新於 2026-09。Play 政策變動頻繁，實際以 Play Console 當下顯示的欄位與政策頁為準。

---

## ⚠️ 第一順位：政策風險評估

**先確認這件事再投入上架成本。**

模擬定位類 App 在 Play 上**不是禁止**的 —— 商店裡有相當多 Fake GPS / Mock Location 類產品。但這類 App 活在灰色地帶，**審核看的不是功能，是你怎麼包裝它**。

觸犯的話會落在「Device and Network Abuse」與「Deceptive Behavior」兩條政策。

### 站得住腳的定位

Jog 有一個很好的天然辯護：**必須先到開發者選項把本 App 選為模擬位置應用程式才能運作**。這本身就證明它是開發測試工具，不是給一般使用者偷偷換位置用的。把這點寫進商店文案。

### 商店素材的紅線

| 絕對不要出現 | 原因 |
|---|---|
| 「遊戲作弊」「抓寶」「刷寶可夢」 | 直接命中 Device and Network Abuse |
| 「打卡」「考勤」「上班定位」 | 協助詐欺雇主 |
| 「交友軟體換位置」「Tinder」 | 協助欺騙他人 |
| 截圖裡有任何第三方 App 畫面 | 暗示用途 + 商標問題 |
| 「繞過」「偵測不到」「反偵測」 | 明示規避意圖 |

### 建議的文案方向

用「開發者 / QA 工具」框架：測試定位相關功能、驗證地理圍欄、模擬移動軌跡、不需要真的跑到現場。App 內加一段簡短的用途說明與免責聲明。

- [x] 商店標題、簡短說明、完整說明通篇不含上表任一詞彙 —— 草稿見 `store/listing.md`，改文案後重查
- [ ] 截圖只有 Jog 自己的畫面
- [x] 說明文字裡明講「需要啟用開發者選項」（`store/listing.md` 兩個語系都有）
- [x] App 內首頁有用途說明（`setup_purpose`），底部附隱私權政策連結

---

## 1. Target API 與 Android 16 行為變更

Google Play 規定 **2026-08-31 起新 App 與更新都必須 target Android 16 / API 36**（可申請延期到 2026-11-01）。

改 `targetSdk` 數字只是第一步，真正要處理的是行為變更：

### 1.1 Edge-to-edge 強制 ✅ 已處理

`windowOptOutEdgeToEdgeEnforcement` 在 Android 16 上已失效，無法退出。`MainActivity` 已加上 `enableEdgeToEdge()` 與 `safeDrawingPadding()`。

- [x] 在手勢導覽列的實機上確認過：引導頁標題在狀態列下方、底部連結在導覽列上方，內容改為可捲動

### 1.2 Predictive back ⚠️ 要驗

`onBackPressed()` 不再被呼叫，`KeyEvent.KEYCODE_BACK` 不再派送。

對 Jog 的具體影響：**懸浮視窗在 focusable 狀態（打字中）會接到返回鍵**。2026-09 實測 release build 確實中招：
鍵盤收起後輸入框仍保有焦點、視窗持續可聚焦，之後的返回鍵全被 overlay 吞掉，底下 App 收不到；點面板外面也不會放掉焦點。
已在 `OverlayHost` 修正：可聚焦期間向 `OnBackInvokedDispatcher` 註冊回呼（API 33+，API 32 以下走 KeyEvent），
並處理 `ACTION_OUTSIDE`，兩者都呼叫 `releaseInputFocus()` 讓視窗回到不可聚焦。

- [x] 輸入框打字中按返回 → 收起鍵盤，不是關掉 overlay
- [x] 鍵盤收起後再按返回 → 放掉輸入框焦點，第三次返回才作用在底層 App
- [x] 打字中點面板外面 → 收鍵盤、放掉焦點（需要 FLAG_NOT_TOUCH_MODAL，否則可聚焦的視窗會吃掉面板外所有觸控）
- [x] 非打字狀態按返回 → 作用在底層 App
- [x] 權限引導頁的返回手勢有預期的動畫（左緣拖曳到一半截圖：畫面縮小、圓角、左側返回箭頭，放開後回桌面）

### 1.3 大螢幕方向與尺寸限制

螢幕最小寬度 ≥ 600dp 時，`android:screenOrientation`、`android:resizableActivity`、`minAspectRatio` / `maxAspectRatio` 全部被忽略。

- [x] `MainActivity` 在 sw840dp（`wm density 240` 模擬）直向與橫向排版正常；分割視窗未測（adb 無法直接進入，實機沒有平板）
- [x] 懸浮面板 268dp 在 sw840dp 上約佔螢幕 1/4 寬，偏小但每個控制項都還能點；上架不擋，之後可考慮依螢幕寬度放大

### 1.4 建置工具鏈

- [x] `compileSdk = 36` 以 AGP 8.9.0 + Gradle 8.11.1 + JBR 17 成功產出 release AAB

---

## 2. 開發者帳號（新帳號的最大卡點）

`com.wlworks` 看起來是新的個人開發者帳號。如果是，這一關比寫程式還花時間：

### 2.1 封閉測試門檻

**新建立的個人開發者帳號**必須先跑封閉測試：至少 **12 位測試者持續選擇加入滿 14 天**，才能申請正式版存取權。

- [ ] 先確認手上湊得到 12 個願意裝、而且 14 天內不移除的人（多找 2、3 個備用）
- [ ] 測試者要用 Google 帳號 email 加入清單
- [ ] 14 天是**連續**的，中途掉人會重來
- [ ] 提早把這件事排進時程 —— 這是純等待時間，不能壓縮
- [x] 給測試者的指南：<https://wlworks.github.io/JogApp/testing>（`docs/testing.md`，繁中 + 英文，含加入連結、開發者選項設定、14 天提醒、回報方式）
- [x] 加入連結：<https://play.google.com/apps/testing/com.wlworks.jog>
- [x] Release notes 文字：`store/release-notes.md`；Console 操作步驟：`store/console-answers.md` 的 Closed testing 一節

### 2.2 帳號驗證

- [ ] 開發者身分驗證（個人帳號需驗證姓名、地址、電話）
- [ ] 付款設定檔（即使 App 免費也可能需要）
- [ ] 開發者名稱與聯絡 email 會公開顯示在商店頁

---

## 3. 權限申報

### 3.1 前景服務類型 ⚠️ 必填

targeting Android 14+ 起，Play Console 要求申報每一個前景服務類型的用途。Jog 用了 `location`。

- [x] Console 前景服務用途說明已填（Background location updates → Other），2026-09-18 隨首次封閉測試送審
- [x] 示範影片已錄在 `store/fgs-demo.mp4`（720×1600，約 64 秒）：以 Google 地圖為背景，啟動面板 → 輸入座標瞬移到東京澀谷、地圖跟著跳 →
  切 Drive、把面板拖到下方、地圖放大 → 搖桿往北再往東推，地圖跟著捲動 → 拉下通知列看到「Simulating · Drive」→ 按通知的 Stop → 面板消失。
  **這支只給前景服務申報用**，畫面裡有第三方 App，不要放到商店頁的宣傳影片或截圖
- [x] 影片已上傳 YouTube（不公開）並貼進申報
- [x] 說明要具體：「模擬定位持續運作時顯示常駐通知，讓使用者隨時可停止」

### 3.2 定位權限

**好消息：Jog 沒有用 `ACCESS_BACKGROUND_LOCATION`**，省掉最麻煩的背景定位審核（那個要另外填申報表、拍影片、而且經常被退）。

- [ ] **絕對不要為了方便加上 `ACCESS_BACKGROUND_LOCATION`**
- [x] 請求定位權限前要有 prominent disclosure：`MainActivity` 的 `LocationDisclosureDialog` 先跳出，按繼續才呼叫系統請求
- [x] 說明要講清楚「用來建立模擬定位供應者」，不是「追蹤你的位置」（`disclosure_location_body`）

### 3.3 其他權限

| 權限 | 狀態 |
|---|---|
| `ACCESS_MOCK_LOCATION` | 不需 Console 申報。它在現代 Android 上是宣告用的，實際授權走開發者選項 |
| `SYSTEM_ALERT_WINDOW` | 沒有專門申報表，但**會引起人工審核注意**（惡意軟體常用）。說明文字要交代用途 |
| `POST_NOTIFICATIONS` | 前景服務必需，無特殊要求 |
| `INTERNET` | **已移除**，見第 4 節 |

---

## 4. Geocoding ✅ 已處理

**原本的 Nominatim 備援已經移除。** 地點解析現在只走座標解析與系統 `Geocoder`。

這不只是換個服務，而是把整條外部相依砍掉，連帶省下的：

| 項目 | 結果 |
|---|---|
| API key 管理 | 不需要 |
| `INTERNET` / `ACCESS_NETWORK_STATE` 權限 | **已從 Manifest 移除** |
| Data Safety 的資料傳輸申報 | 不適用 |
| 第三方服務的使用條款與費用 | 無 |
| 隱私權政策要交代的第三方 | 無 |
| OSM / ODbL 出處標示 | 不適用 |

代價：少數沒有 geocoding backend 的機型查不到地名。UI 會回「這台裝置無法搜尋地名，請直接輸入經緯度」，
功能不會因此不可用 —— 座標輸入永遠有效，而且是離線的。

- [x] 已確認 release 合併後的 manifest 沒有 `INTERNET`（2026-09 以 `bundleRelease` 產物核對，`app/build/intermediates/merged_manifests/release/`）。相依升版後要重看一次
- [ ] 如果之後加回遠端備援，記得同步補回權限、Data Safety 申報與隱私權政策內容

---

## 5. Data Safety 與隱私權政策

隱私權政策仍是必填，Data Safety 表單也一定要送 —— 但因為移除了遠端 geocoding，內容變得非常單純。

**Jog 的實際資料行為：不蒐集、不傳輸任何使用者資料。** 定位權限只用來建立模擬定位供應者，
不讀取也不上傳使用者的真實位置；輸入的地名交給系統 `Geocoder`，不經過本 App 的任何伺服器。

- [x] Data Safety 已申報「不蒐集資料」。依據：相依裡沒有 analytics / crash reporting SDK，也沒有 INTERNET 權限。唯一的持久化是 `LastLocationStore` 存的上次**模擬**座標，只在裝置本機的 App 私有空間，不離開裝置 —— Data Safety 的「蒐集」定義是傳出裝置，所以不必申報；隱私權政策「資料儲存」一節已對應更新
- [ ] 若之後加了 Crashlytics 或 analytics，申報內容要跟著改
- [x] 隱私權政策寫在 `docs/privacy-policy.md`（英文 + 繁中），網址 <https://wlworks.github.io/JogApp/privacy-policy>
- [x] GitHub Pages 已啟用（main / docs），網址已確認可開，App 內連結實機點過會跳到瀏覽器
- [x] 政策裡明講定位權限的用途是建立模擬定位供應者，不是追蹤使用者
- [x] 政策頁的聯絡方式是 willy78831@gmail.com；Console 商店資訊的公開聯絡 email 要填同一個，開發者名稱要是 WLWorks 才與政策文一致
- [x] 沒有帳號系統 → 帳號刪除要求不適用，Data Safety 選 No 後不會出現

---

## 6. 一般上架項目

- [x] 已以 AAB 上傳到 Closed testing（Alpha）：versionCode 1 / 0.1.0，2026-09-18 送審中
- [x] Play App Signing 已啟用（Google 產生金鑰，手上的是 upload key）
- [x] 內容分級問卷：All Other App Types，全部 No
- [x] 目標客群：只勾 18 and over
- [x] 512×512 圖示與 1024×500 主題圖片：`python tools/render_store_assets.py` 產到 `store/`
- [x] 手機截圖各 3 張在 `store/screenshots/en/` 與 `store/screenshots/zh-TW/`（設定頁、面板運行中、速度檔切換），已裁成 2:1、去 alpha，只有 Jog 自己的畫面
- [x] App 圖示已換成 adaptive icon（`mipmap-anydpi-v26/ic_launcher.xml`，含 monochrome 層）；`ic_pin.xml` 只剩通知在用
- [x] 廣告聲明：無廣告；Advertising ID：不使用
- [x] 資料刪除網址：不適用
- [x] 商店資訊預設 en-US，另加 zh-TW，兩個都已隨首次送審
- [x] 兩個語系的商店文案都用 `store/listing.md` 的版本，紅線檢查已過

### 6.1 簽章與產出 AAB

upload key 與密碼放在 gitignore 的 `keystore/` 與 `keystore.properties`（範本 `keystore.properties.example`）。
`app/build.gradle.kts` 有這個檔就簽章，沒有就照樣 build 但不簽，所以 clone 下來的環境不會因此失敗。

```bash
./gradlew :app:bundleRelease
# 產出：app/build/outputs/bundle/release/app-release.aab
```

- [x] 已備份 `keystore/upload-keystore.jks` 與 `keystore.properties` 到 repo 以外（2026-09-18）。啟用 Play App Signing 後這把只是 upload key，遺失可以向 Google 申請重設，但要等好幾天
- [ ] 第一次上傳前想換自己的密碼就現在換：上傳後 upload key 就綁定了，之後只能走重設流程
- [ ] 每次上傳前 `versionCode` +1，Play 不接受重複值
- [x] release 開了 R8 與 resource shrinking，2026-09 已用 release build 實機跑過完整流程：權限引導、懸浮面板、
  輸入座標瞬移、Start、切換速度檔、搖桿推 3 秒座標往北位移、Stop、關閉面板後 `dumpsys location` 無殘留 `[mock]`，
  logcat 無例外。每次改動相依後要重跑一遍，不能只測 debug

### 16 KB page size

Play 要求 App 相容 16 KB 記憶體分頁。這條只影響**含原生 `.so` 的 App**。

- [x] AAB 裡**有** `.so`：Compose 透過 `androidx.graphics:graphics-path:1.0.1` 帶進 `libandroidx.graphics.path.so`（四個 ABI）。已檢查四個檔的 ELF `PT_LOAD` 對齊都是 16384，符合要求。升 Compose BOM 後要重驗（`unzip -l app-release.aab | grep .so` 再看 ELF 對齊）

---

## 7. 上線後

- [ ] 盯 Play Console 的**政策狀態頁**，不要只看 email 通知
- [ ] 收到警告時先讀完整的違規說明再改，盲目重送會累積違規紀錄
- [ ] 使用者評論裡如果出現「拿來玩 XX 遊戲很好用」，**不要回覆認同** —— 那會被當成你認可該用途的證據

---

## 時程估計

| 項目 | 時間 |
|---|---|
| 新個人帳號封閉測試 | **14 天以上**（純等待，不可壓縮） |
| 首次上架審核 | 數天到數週，敏感權限會拉長 |
| 政策問題往返 | 每輪數天 |

**結論：封閉測試的 14 天是關鍵路徑。程式還在寫的時候就可以先開帳號、建 App、把 12 個測試者找齊。**

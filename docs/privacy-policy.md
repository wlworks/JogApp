---
layout: default
title: Jog Privacy Policy
---

# Jog Privacy Policy

**Effective date: 2026-10-20**

Jog ("the app") is a mock location tool for development and QA testing, published by WLWorks. This policy explains what the app does with data. The short version: **Jog itself does not collect, store or transmit your personal data. The app shows ads through Google AdMob, and AdMob collects some device data to do so, as described below.**

## Data collection

Jog itself does not collect any personal or device data, and has no analytics or crash reporting of its own.

Jog shows rewarded ads through **Google AdMob**. Auto move and Health sync are unlocked by watching an ad; the rest of the app works without ads. To request and show ads, the AdMob SDK connects to the internet and may collect and share with Google and its advertising partners:

- device and advertising identifiers (such as the Android advertising ID and app set ID),
- IP address, from which an approximate location may be derived,
- how you interact with ads and with the app (for example ad views and taps),
- diagnostic information about app and SDK performance.

Google uses this data for advertising, analytics and fraud prevention. Details: <https://policies.google.com/technologies/partner-sites>

The simulated coordinates you enter in Jog, your real location, and anything Jog writes to Health Connect are **not** shared with AdMob.

### Your choices

- You can reset or delete your advertising ID, or opt out of ad personalisation, in Android Settings → Privacy → Ads.
- If you are in the European Economic Area, the UK or Switzerland, Jog asks for your consent through Google's consent form before requesting ads. You can change your choice at any time under **Ad privacy choices** on the setup screen. If you do not consent, any ads you watch are non-personalised.
- If you do not watch an ad, auto move and Health sync stay locked; no ad is requested until you choose to watch one.

## Permissions and why they are needed

- **Location** (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`). Android requires this permission to register a mock location provider. Jog uses it only to publish the simulated coordinates you enter. It does not read, store or transmit your real location.
- **Mock location** (`ACCESS_MOCK_LOCATION`). Lets Jog appear under Developer options → Select mock location app. Jog can only simulate locations after you choose it there.
- **Display over other apps** (`SYSTEM_ALERT_WINDOW`). Shows the floating control panel while you test another app.
- **Notifications** (`POST_NOTIFICATIONS`) and **foreground service**. Keeps a persistent notification visible while simulation is running so you can stop it at any time.
- **Health Connect write access** (`WRITE_STEPS`, `WRITE_DISTANCE`). Optional, and only used by the Health sync feature described below.
- **Internet access** (`INTERNET`, `ACCESS_NETWORK_STATE`) and **advertising ID** (`AD_ID`). Used only by the Google AdMob SDK to load and show ads.

## Health Connect

Health sync is optional and off by default. If you grant write access and turn it on in the floating panel, Jog writes step counts and distance derived from the simulated movement to Health Connect on your device, about once a minute while the simulated position is moving. These are simulated test values, marked as manually entered, intended for testing apps that read Health Connect — they are not measurements of your activity.

Jog requests write access only. It never reads any data from Health Connect, and the values it writes are not sent to WLWorks or to any third party. You can revoke the permission or delete the records Jog wrote at any time in Health Connect's own settings.

## Place name lookup

When you type a place name, Jog hands the text to the Android system Geocoder on your device. That lookup is performed by the operating system and is governed by the privacy policy of your device manufacturer or of Google, not by Jog. Jog does not send the query to any server of its own and does not keep a history of searches.

## Data storage

Jog remembers the last simulated coordinate you set, so you can pick up where you left off the next time you open the app. This is the coordinate you typed or steered to — never your device's real location. It is stored in the app's private storage on your device, is never transmitted anywhere, and is removed when you uninstall the app or clear its data. The on-screen position of the floating panel is stored the same way, so the panel reopens where you left it. Everything else (place-name queries, speed tier) is kept in memory only and is discarded when the app process ends.

## Children

Jog is a developer tool and is not directed at children under 13.

## Changes to this policy

Changes will be published on this page with an updated effective date.

## Contact

Questions about this policy can be sent to WLWorks at <willy78831@gmail.com>, or raised on the project issue tracker: <https://github.com/wlworks/JogApp/issues>

---

# Jog 隱私權政策

**生效日期：2026-10-20**

Jog（以下稱「本 App」）是 WLWorks 發行的開發與 QA 測試用模擬定位工具。本政策說明本 App 如何處理資料。一句話版本：**Jog 本身不蒐集、不儲存、不傳輸你的個人資料。App 透過 Google AdMob 顯示廣告，AdMob 為此會蒐集部分裝置資料，詳見下文。**

## 資料蒐集

Jog 本身不蒐集任何個人或裝置資料，也沒有自己的分析或當機回報。

Jog 透過 **Google AdMob** 顯示獎勵廣告。自動移動與 Health 同步需要先看一則廣告才能使用，其餘功能不需要看廣告。為了請求與顯示廣告，AdMob SDK 會連線到網際網路，並可能蒐集下列資料、與 Google 及其廣告合作夥伴分享：

- 裝置與廣告識別碼（例如 Android 廣告 ID、app set ID）
- IP 位址（可據以推得概略位置）
- 你與廣告及 App 的互動（例如廣告的曝光與點擊）
- App 與 SDK 效能相關的診斷資訊

Google 將這些資料用於廣告、分析與防止詐欺。詳情：<https://policies.google.com/technologies/partner-sites>

你在 Jog 輸入的模擬座標、你的真實位置、以及 Jog 寫入 Health Connect 的資料，都**不會**提供給 AdMob。

### 你的選擇

- 你可以在 Android 設定 → 隱私權 → 廣告 重設或刪除廣告 ID，或停用個人化廣告。
- 如果你在歐洲經濟區、英國或瑞士，Jog 會在請求廣告前透過 Google 的同意表單詢問你的選擇，之後可隨時在設定畫面的**廣告隱私設定**更改。不同意的話，你看到的廣告都是非個人化廣告。
- 不看廣告的話，自動移動與 Health 同步會維持鎖定；在你選擇觀看之前，App 不會請求任何廣告。

## 權限與用途

- **定位**（`ACCESS_FINE_LOCATION`、`ACCESS_COARSE_LOCATION`）。Android 規定要有定位權限才能註冊模擬定位供應者。Jog 只用這個權限發布你輸入的模擬座標，不會讀取、儲存或傳輸你的真實位置。
- **模擬位置**（`ACCESS_MOCK_LOCATION`）。讓 Jog 出現在「開發者選項 → 選取模擬位置資訊應用程式」清單中。你在那裡選擇 Jog 之後它才能模擬定位。
- **顯示在其他應用程式上層**（`SYSTEM_ALERT_WINDOW`）。在你測試其他 App 時顯示懸浮控制面板。
- **通知**（`POST_NOTIFICATIONS`）與**前景服務**。模擬進行中持續顯示常駐通知，讓你隨時可以停止。
- **Health Connect 寫入權限**（`WRITE_STEPS`、`WRITE_DISTANCE`）。選用，只有下方說明的 Health 同步功能會用到。
- **網路存取**（`INTERNET`、`ACCESS_NETWORK_STATE`）與**廣告 ID**（`AD_ID`）。只由 Google AdMob SDK 用來載入與顯示廣告。

## Health Connect

Health 同步是選用功能，預設關閉。當你授予寫入權限並在懸浮面板開啟後，Jog 會在模擬位置移動的期間，約每分鐘一次把由模擬移動換算出的步數與距離寫入裝置上的 Health Connect。這些是模擬出來的測試數值，標記為手動輸入，用途是測試會讀取 Health Connect 的 App，並不是你實際活動的量測結果。

Jog 只申請寫入權限，不會從 Health Connect 讀取任何資料；寫入的數值也不會傳送給 WLWorks 或任何第三方。你隨時可以在 Health Connect 的設定中撤銷權限，或刪除 Jog 寫入的紀錄。

## 地名查詢

當你輸入地名，Jog 會把文字交給裝置上的 Android 系統 Geocoder。這項查詢由作業系統執行，受你的裝置製造商或 Google 的隱私權政策規範，與 Jog 無關。Jog 不會把查詢送到任何自己的伺服器，也不保留搜尋記錄。

## 資料儲存

Jog 會記住你最後設定的模擬座標，下次開啟 App 時可以直接接續。這是你輸入或推桿移動到的座標，絕不是裝置的真實位置。它存放在 App 在裝置上的私有儲存空間，不會傳送到任何地方，解除安裝或清除 App 資料時一併移除。懸浮面板在螢幕上的位置也以同樣方式保存，下次開啟時面板會出現在上次的位置。其餘內容（地名查詢、速度檔次）只保存在記憶體中，App 行程結束即丟棄。

## 兒童

Jog 是開發者工具，不以 13 歲以下兒童為對象。

## 政策變更

變更會公告在本頁並更新生效日期。

## 聯絡方式

關於本政策的問題請寄至 <willy78831@gmail.com>，或至專案的問題追蹤頁提出：<https://github.com/wlworks/JogApp/issues>

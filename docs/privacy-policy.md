---
layout: default
title: Jog Privacy Policy
---

# Jog Privacy Policy

**Effective date: 2026-09-18**

Jog ("the app") is a mock location tool for development and QA testing, published by WLWorks. This policy explains what the app does with data. The short version: **Jog does not collect, store or transmit any personal data.**

## Data collection

Jog does not collect any personal or device data. It contains no analytics, crash reporting, advertising or any other third-party SDK that collects data. The app does not request the INTERNET permission, so it has no way to send data anywhere.

## Permissions and why they are needed

- **Location** (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`). Android requires this permission to register a mock location provider. Jog uses it only to publish the simulated coordinates you enter. It does not read, store or transmit your real location.
- **Mock location** (`ACCESS_MOCK_LOCATION`). Lets Jog appear under Developer options → Select mock location app. Jog can only simulate locations after you choose it there.
- **Display over other apps** (`SYSTEM_ALERT_WINDOW`). Shows the floating control panel while you test another app.
- **Notifications** (`POST_NOTIFICATIONS`) and **foreground service**. Keeps a persistent notification visible while simulation is running so you can stop it at any time.

## Place name lookup

When you type a place name, Jog hands the text to the Android system Geocoder on your device. That lookup is performed by the operating system and is governed by the privacy policy of your device manufacturer or of Google, not by Jog. Jog does not send the query to any server of its own and does not keep a history of searches.

## Data storage

Coordinates and settings you enter are kept in memory only and are discarded when the app process ends. Jog writes nothing to persistent storage.

## Children

Jog is a developer tool and is not directed at children under 13.

## Changes to this policy

Changes will be published on this page with an updated effective date.

## Contact

Questions about this policy can be sent to WLWorks at <willy78831@gmail.com>, or raised on the project issue tracker: <https://github.com/wlworks/JogApp/issues>

---

# Jog 隱私權政策

**生效日期：2026-09-18**

Jog（以下稱「本 App」）是 WLWorks 發行的開發與 QA 測試用模擬定位工具。本政策說明本 App 如何處理資料。一句話版本：**Jog 不蒐集、不儲存、不傳輸任何個人資料。**

## 資料蒐集

Jog 不蒐集任何個人或裝置資料。App 內沒有分析、當機回報、廣告或任何其他會蒐集資料的第三方 SDK。App 沒有申請 INTERNET 權限，因此沒有任何管道可以把資料送到外部。

## 權限與用途

- **定位**（`ACCESS_FINE_LOCATION`、`ACCESS_COARSE_LOCATION`）。Android 規定要有定位權限才能註冊模擬定位供應者。Jog 只用這個權限發布你輸入的模擬座標，不會讀取、儲存或傳輸你的真實位置。
- **模擬位置**（`ACCESS_MOCK_LOCATION`）。讓 Jog 出現在「開發者選項 → 選取模擬位置資訊應用程式」清單中。你在那裡選擇 Jog 之後它才能模擬定位。
- **顯示在其他應用程式上層**（`SYSTEM_ALERT_WINDOW`）。在你測試其他 App 時顯示懸浮控制面板。
- **通知**（`POST_NOTIFICATIONS`）與**前景服務**。模擬進行中持續顯示常駐通知，讓你隨時可以停止。

## 地名查詢

當你輸入地名，Jog 會把文字交給裝置上的 Android 系統 Geocoder。這項查詢由作業系統執行，受你的裝置製造商或 Google 的隱私權政策規範，與 Jog 無關。Jog 不會把查詢送到任何自己的伺服器，也不保留搜尋記錄。

## 資料儲存

你輸入的座標與設定只保存在記憶體中，App 行程結束即丟棄。Jog 不寫入任何持久儲存。

## 兒童

Jog 是開發者工具，不以 13 歲以下兒童為對象。

## 政策變更

變更會公告在本頁並更新生效日期。

## 聯絡方式

關於本政策的問題請寄至 <willy78831@gmail.com>，或至專案的問題追蹤頁提出：<https://github.com/wlworks/JogApp/issues>

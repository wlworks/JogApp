---
layout: default
title: Jog
---

# Jog

**Mock location tool for development and QA testing on Android.**

Jog feeds simulated coordinates to the device so that location-aware features such as geofences, distance calculations and movement handling can be exercised without physically going anywhere. A floating panel with a joystick nudges the simulated position in real time while the app under test stays in the foreground.

## Requirements

- Android 8.0 (API 26) or newer.
- Developer options enabled, with Jog selected as the mock location app. Jog cannot simulate anything until you make that selection yourself.

## What Jog does not do

- It does not read, store or upload your real location.
- It does not connect to the internet. The app does not hold the INTERNET permission.
- It has no accounts, analytics, advertising or third-party SDKs.

## Links

- [Privacy policy](privacy-policy)
- [Source code and issue tracker](https://github.com/wlworks/JogApp)

---

# Jog（繁體中文）

**Android 開發與 QA 測試用的模擬定位工具。**

Jog 把模擬座標餵給裝置，讓地理圍欄、距離計算、移動處理等定位相關功能不必真的跑到現場就能測試。懸浮面板上的搖桿可以在受測 App 停留在前景時即時微調模擬位置。

## 需求

- Android 8.0（API 26）以上。
- 已啟用開發者選項，並把 Jog 選為模擬位置資訊應用程式。沒有這一步，Jog 什麼都做不了。

## Jog 不做的事

- 不讀取、不儲存、不上傳你的真實位置。
- 不連網。App 沒有 INTERNET 權限。
- 沒有帳號、分析、廣告或任何第三方 SDK。

## 連結

- [隱私權政策](privacy-policy)
- [原始碼與問題回報](https://github.com/wlworks/JogApp)

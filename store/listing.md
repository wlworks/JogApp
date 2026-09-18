# Play 商店文案草稿

兩個語系都已對照 [PLAY_RELEASE.md](../PLAY_RELEASE.md) 第一節的紅線檢查過：沒有遊戲、打卡、交友、繞過、偵測相關字眼，
也沒有提到任何第三方 App。修改時請重跑一次那張表。

素材由 `python tools/render_store_assets.py` 產生：`icon-512.png`、`feature-graphic-1024x500.png`。
截圖至少 2 張手機截圖，只能拍 Jog 自己的畫面（設定頁、懸浮面板）。

---

## English（預設語言）

**Title**（≤ 30 字元）

```
Jog – Mock Location Dev Tool
```

**Short description**（≤ 80 字元）

```
Developer tool for testing location features. Requires Developer options.
```

**Full description**（≤ 4000 字元）

```
Jog is a mock location tool for Android developers and QA testers. It feeds simulated coordinates to the device so you can exercise location-aware features – geofences, distance calculations, movement handling – without leaving your desk.

HOW IT WORKS
• Enter a place name or latitude/longitude and teleport the simulated position there.
• A floating panel with a joystick nudges the position in real time while the app under test stays in the foreground.
• Six speed tiers, from a slow creep to rail speed, for testing movement at realistic rates.
• A persistent notification shows while simulation is running so you can stop it at any time.

REQUIREMENTS
Jog only works after you enable Developer options and select Jog as the mock location app. Jog cannot make that selection for you, and it cannot simulate anything until you do.

PRIVACY
• Jog never reads, stores or uploads your real location.
• Jog does not connect to the internet and does not hold the INTERNET permission.
• No accounts, analytics, advertising or third-party SDKs.

PERMISSIONS
• Location – required by Android to register a mock location provider.
• Display over other apps – the floating control panel.
• Notifications – the stop control while simulation runs.

Jog is intended for software development and testing. Use it responsibly and only with apps you are testing.
```

---

## 繁體中文

**標題**（≤ 30 字元）

```
Jog 開發測試用模擬定位工具
```

**簡短說明**（≤ 80 字元）

```
給開發者與 QA 的定位功能測試工具，需啟用開發者選項。
```

**完整說明**（≤ 4000 字元）

```
Jog 是給 Android 開發者與 QA 測試人員用的模擬定位工具。它把模擬座標餵給裝置，讓你不必離開座位就能測試地理圍欄、距離計算、移動處理等定位相關功能。

運作方式
• 輸入地名或經緯度，把模擬位置瞬移到那裡。
• 懸浮面板上的搖桿可以在受測 App 停留在前景時即時微調位置。
• 六個速度檔次，從蝸行到高鐵，以合理的速率測試移動。
• 模擬進行中顯示常駐通知，隨時可以停止。

需求
Jog 必須在你啟用開發者選項並把 Jog 選為模擬位置資訊應用程式之後才能運作。Jog 無法替你完成這個選擇，選擇之前也無法模擬任何東西。

隱私
• Jog 不會讀取、儲存或上傳你的真實位置。
• Jog 不連網，也沒有 INTERNET 權限。
• 沒有帳號、分析、廣告或第三方 SDK。

權限
• 定位：Android 規定註冊模擬定位供應者必須有此權限。
• 顯示在其他應用程式上層：懸浮控制面板。
• 通知：模擬進行中的停止控制。

Jog 是為軟體開發與測試設計的工具，請負責任地使用，並只用在你正在測試的 App 上。
```

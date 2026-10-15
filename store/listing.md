# Play 商店文案草稿

兩個語系都已對照 [PLAY_RELEASE.md](../PLAY_RELEASE.md) 第一節的紅線檢查過：沒有遊戲、打卡、交友、繞過、偵測相關字眼，
也沒有提到任何第三方 App。修改時請重跑一次那張表。

素材由 `python tools/render_store_assets.py` 產生：`icon-512.png`、`feature-graphic-1024x500.png`。
截圖至少 2 張手機截圖，只能拍 Jog 自己的畫面（設定頁、懸浮面板）。

---

## English（預設語言）

**Title**（≤ 30 字元）

```
Jog: Mock GPS Location Tester
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
• A floating panel with a joystick nudges the position in real time while the app under test stays in the foreground. The panel uses a compact landscape layout that covers only a small part of the screen, with the speed tiers always in view. Collapse the panel to a compact bubble and the joystick stays available.
• Six speed tiers, from a slow creep to rail speed, for testing movement at realistic rates.
• Jump to a random nearby spot. The distance follows the speed tier: about 80–250 m when walking, up to 5–15 km at rail speed.
• Auto move: the simulated position roams on its own at the selected speed tier, turning gradually like a real walk or drive. Push the joystick at any time to steer.
• Health sync (optional): while the position moves at creep, walk or run speed, Jog writes the simulated steps and distance to Health Connect once a minute, so you can test apps that read them.
• Auto move and Health sync are unlocked by watching one short rewarded ad, valid until you close the floating panel. Teleport, the joystick, random jump and everything else work without ads.
• Jog remembers the last simulated position and restores it the next time you open the app.
• A persistent notification shows while simulation is running so you can stop it at any time.

REQUIREMENTS
Jog only works after you enable Developer options and select Jog as the mock location app. Jog cannot make that selection for you, and it cannot simulate anything until you do.

PRIVACY
• Jog never reads, stores or uploads your real location.
• The last simulated position is saved only on your device, so you can pick up where you left off.
• Health sync is off by default. It only writes simulated steps and distance to Health Connect on your device, marked as manually entered. Jog never reads any health data.
• No accounts and no analytics of Jog's own.
• Ads are served by Google AdMob, which may collect device identifiers and approximate location to show ads; see the privacy policy for details. Your simulated coordinates and Health Connect data are never shared with AdMob.
• Users in the EEA, UK and Switzerland are asked for ad consent and can change it anytime under Ad privacy choices.

PERMISSIONS
• Location – required by Android to register a mock location provider.
• Display over other apps – the floating control panel.
• Notifications – the stop control while simulation runs.
• Health Connect, write steps and distance (optional) – used only when you turn on Health sync.
• Internet and advertising ID – used only by Google AdMob to show ads.

Jog is intended for software development and testing. Use it responsibly and only with apps you are testing.
```

---

## 繁體中文

**標題**（≤ 30 字元）

```
Jog 模擬 GPS 定位・開發測試工具
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
• 懸浮面板上的搖桿可以在受測 App 停留在前景時即時微調位置。面板採橫式精簡版面，只佔螢幕一小塊，速度檔一直顯示在面板上。面板收合成小泡泡後，搖桿依然可以用。
• 六個速度檔次，從蝸行到高鐵，以合理的速率測試移動。
• 隨機跳到附近。距離跟著速度檔走：步行約 80–250 公尺，高鐵可達 5–15 公里。
• 自動移動：模擬位置依目前的速度檔自行隨機移動，像真的走路或開車一樣逐漸轉彎，隨時可以推搖桿接手方向。
• Health 同步（選用）：以蝸行、步行或慢跑速度移動時，每分鐘把模擬的步數與距離寫入 Health Connect，方便測試會讀取這些資料的 App。
• 自動移動與 Health 同步需觀看一則短獎勵廣告解鎖，效期到關閉懸浮面板為止。瞬移、搖桿、隨機跳等其他功能都不需要看廣告。
• 記住上次的模擬位置，下次開啟時直接接續。
• 模擬進行中顯示常駐通知，隨時可以停止。

需求
Jog 必須在你啟用開發者選項並把 Jog 選為模擬位置資訊應用程式之後才能運作。Jog 無法替你完成這個選擇，選擇之前也無法模擬任何東西。

隱私
• Jog 不會讀取、儲存或上傳你的真實位置。
• 上次的模擬位置只存在你的裝置上，方便下次接續。
• Health 同步預設關閉，只會把模擬的步數與距離寫入你裝置上的 Health Connect，並標記為手動輸入。Jog 不會讀取任何健康資料。
• 沒有帳號，Jog 本身也沒有任何分析功能。
• 廣告由 Google AdMob 提供，AdMob 為了顯示廣告可能會蒐集裝置識別碼與概略位置，詳見隱私權政策。你的模擬座標與 Health Connect 資料都不會提供給 AdMob。
• 歐洲經濟區、英國與瑞士的使用者會被詢問廣告同意，並可隨時在「廣告隱私設定」更改。

權限
• 定位：Android 規定註冊模擬定位供應者必須有此權限。
• 顯示在其他應用程式上層：懸浮控制面板。
• 通知：模擬進行中的停止控制。
• Health Connect 寫入步數與距離（選用）：只在你開啟 Health 同步時使用。
• 網路與廣告 ID：只由 Google AdMob 用來顯示廣告。

Jog 是為軟體開發與測試設計的工具，請負責任地使用，並只用在你正在測試的 App 上。
```

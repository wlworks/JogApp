# Release notes

Play Console「Release notes」欄位接受多語系標記，整段貼進去即可。每個語系 500 字元上限。
語系代碼要和商店資訊已加入的語言一致：`en-US`、`zh-TW`。

## 0.1.0（versionCode 1）— 封閉測試第一版

```
<en-US>
First closed test build.
• Floating panel with a joystick for moving the simulated position in real time.
• Teleport by coordinates or place name.
• Six speed tiers from creep to rail.
• Persistent notification with a Stop action while simulating.
Requires Developer options → Select mock location app → Jog.
</en-US>
<zh-TW>
第一個封閉測試版本。
• 懸浮面板與搖桿，即時移動模擬位置。
• 輸入座標或地名瞬移。
• 六個速度檔次，從蝸行到高鐵。
• 模擬進行中顯示常駐通知，可隨時停止。
需先在開發者選項選取 Jog 為模擬位置資訊應用程式。
</zh-TW>
```

## 1.0.0（versionCode 10000）— 第一個正式版

封測中的同一個 build 直接推上 Production，不重新上傳。

```
<en-US>
First public release of Jog, a mock location tool for developers and QA.
• Floating panel with a joystick to move the simulated position in real time, also on the collapsed panel.
• Teleport by coordinates or place name, or jump to a random nearby spot scaled to the speed tier.
• Six speed tiers from creep to rail. The last position is restored on launch.
Requires Developer options → Select mock location app → Jog.
</en-US>
<zh-TW>
Jog 第一個正式版，給開發者與 QA 用的模擬定位工具。
• 懸浮面板與搖桿，即時移動模擬位置，收合後也能用。
• 輸入座標或地名瞬移，或依速度檔隨機跳到附近。
• 六個速度檔次，從蝸行到高鐵；重新開啟時回到上次的位置。
需先在開發者選項選取 Jog 為模擬位置資訊應用程式。
</zh-TW>
```

之後每次上傳新版：`versionCode` +1，在這裡加一段，只寫這版改了什麼。

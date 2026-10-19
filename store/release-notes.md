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

## 2.0.0（versionCode 20000）— 自動移動與 Health 同步

MAJOR：新增 Health Connect 寫入權限，Play Console 要另外填健康資料申報（見 PLAY_RELEASE.md 第 8 節）。

```
<en-US>
• Auto move: the simulated position roams on its own at the selected speed tier. Push the joystick at any time to steer.
• Health sync (optional): while moving at walking speed, Jog writes the simulated steps and distance to Health Connect once a minute, for testing apps that read them. Write-only; Jog never reads health data.
• Jog now tells you when it is not selected as the mock location app, instead of silently doing nothing.
</en-US>
<zh-TW>
• 自動移動：模擬位置依目前的速度檔自行隨機移動，隨時可以推搖桿接手方向。
• Health 同步（選用）：以步行移動時，每分鐘把模擬的步數與距離寫入 Health Connect，方便測試會讀取這些資料的 App。只寫不讀，Jog 不會讀取任何健康資料。
• 沒有被選為模擬位置資訊應用程式時，會直接提示，不再默默沒有作用。
</zh-TW>
```

## 3.0.0（versionCode 30000）— 獎勵廣告解鎖自動移動與 Health 同步

MAJOR：開始含廣告（Google AdMob），Play Console 的廣告聲明、Advertising ID、Data Safety 都要重新申報，
隱私權政策也要先換成含廣告的版本（見 PLAY_RELEASE.md 第 9 節）。

```
<en-US>
• Auto move and Health sync are now unlocked by watching one short rewarded ad, valid until you close the floating panel. Teleport, the joystick, random jump and everything else work without ads.
• Users in the EEA, UK and Switzerland are asked for ad consent first and can change their choice anytime under Ad privacy choices on the setup screen.
</en-US>
<zh-TW>
• 自動移動與 Health 同步改為觀看一則短獎勵廣告後解鎖，效期到關閉懸浮面板為止。瞬移、搖桿、隨機跳等其他功能都不需要看廣告。
• 歐洲經濟區、英國與瑞士的使用者會先被詢問廣告同意，之後可隨時在設定畫面的「廣告隱私設定」更改。
</zh-TW>
```

## 3.1.0（versionCode 30100）— 橫式面板、Health 同步開放蝸行與慢跑

```
<en-US>
• The floating panel is now a shorter landscape layout: the joystick and Start/Stop stay on the left, a row of speed tier icons sits on the right, and Location and Move tabs below it hold everything else.
• Health sync now works at creep, walk and run speed, with steps counted from each tier's stride. Bike and faster tiers stay off while it is on.
</en-US>
<zh-TW>
• 懸浮面板改為較矮的橫式版面：搖桿與開始／停止固定在左側，右側上方是一列速度檔圖示，其他功能收進下方的「位置」與「移動」分頁。
• Health 同步開放蝸行、步行與慢跑，步數依各檔次的步幅換算；同步期間單車以上的檔次維持停用。
</zh-TW>
```

## 3.2.0（versionCode 30200）— 面板任意處拖曳、記住位置、廣告錯誤碼

```
<en-US>
• Drag the floating panel from anywhere on it, not just the title. The joystick still steers as before.
• The panel reopens where you last left it, and moves back on screen after rotating or expanding.
• If an ad can't be shown, the unlock screen now explains why and shows a short error code to include when reporting a problem.
</en-US>
<zh-TW>
• 懸浮面板任何空白處都能拖曳，不必再對準標題；搖桿照常操作。
• 面板會在上次的位置重新開啟，旋轉螢幕或展開後超出畫面會自動移回。
• 廣告無法播放時，解鎖畫面會說明原因並顯示錯誤碼，回報問題時附上即可。
</zh-TW>
```

之後每次上傳新版：`versionCode` +1，在這裡加一段，只寫這版改了什麼。

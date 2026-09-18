---
layout: default
title: Jog 封閉測試指南
---

# Jog 封閉測試指南

感謝你幫忙測試 Jog。這是一個給開發者用的模擬定位工具，目前在 Google Play 做封閉測試。
整個流程大約 5 分鐘，**最重要的一件事寫在最前面：**

> **加入測試後，請至少 14 天內不要退出測試、也不要移除 App。**
> Google Play 規定新開發者帳號要有 12 位測試者連續加入滿 14 天才能正式上架，中途少一個人就要重算。
> 你不需要每天打開它，裝著就好。

## 1. 加入測試

1. 用你要裝 App 的那台 Android 手機，以**同一個 Google 帳號**登入。
2. 打開加入連結：<https://play.google.com/apps/testing/com.wlworks.jog>
3. 點「Become a tester」（成為測試人員）。
4. 點頁面上的「Download it on Google Play」，或直接到 Play 商店搜尋 Jog 安裝。
   剛加入後 Play 商店可能要幾分鐘到幾小時才會出現，看不到就晚點再試。

只有事先被加進測試名單的 Google 帳號才能加入。連結打開顯示找不到，請確認登入的帳號是你提供給我們的那一個。

## 2. 第一次設定

Jog 必須先被選為系統的「模擬位置資訊應用程式」才能運作，這一步 App 無法替你完成：

1. **開啟開發者選項**：設定 → 關於手機 → 連續點「版本號碼」7 次。
2. 設定 → 系統 → 開發者選項 → 找到「**選取模擬位置資訊應用程式**」→ 選 **Jog**。
3. 打開 Jog，依畫面上的三個項目逐一授權：顯示在其他應用程式上層、定位權限、通知權限。
4. 按「啟動懸浮視窗」。

## 3. 試用

1. 在懸浮面板輸入座標，例如 `25.033, 121.565`，或輸入地名，按「定位到這裡」。
2. 打開任何地圖 App，位置應該已經跳到那裡。
3. 用面板上的搖桿推動，位置會朝推的方向移動；上方六個檔次決定移動速度。
4. 測完按面板的「停止模擬」再關閉面板，或從通知列按「停止」。**沒停掉的話手機定位會一直卡在假座標。**

## 4. 回報問題

遇到崩潰、面板消失、位置不動、按鈕沒反應等，請告訴我們：

- 手機型號與 Android 版本
- 做了什麼、預期看到什麼、實際看到什麼
- 有截圖或錄影更好

寄到 <willy78831@gmail.com>，或到 <https://github.com/wlworks/JogApp/issues> 開一則。

## 5. 隱私

Jog 不連網、不蒐集任何資料，也不會讀取或上傳你的真實位置。細節見[隱私權政策](privacy-policy)。

---

# Jog closed testing guide

Thanks for helping test Jog, a mock location tool for developers, currently in closed testing on Google Play.
Setup takes about 5 minutes. **The single most important rule:**

> **Stay opted in and keep the app installed for at least 14 days.**
> Google Play requires new developer accounts to have 12 testers opted in for 14 consecutive days before production access is granted. Dropping out resets the clock. You do not need to open the app every day.

## 1. Join

1. On the Android phone you will use, sign in with the **same Google account** you gave us.
2. Open <https://play.google.com/apps/testing/com.wlworks.jog> and tap **Become a tester**.
3. Tap **Download it on Google Play**, or search for Jog in the Play Store. It can take a while to appear after joining.

## 2. One-time setup

1. Enable Developer options: Settings → About phone → tap **Build number** 7 times.
2. Settings → System → Developer options → **Select mock location app** → choose **Jog**.
3. Open Jog and grant the three permissions shown: display over other apps, location, notifications.
4. Tap **Start floating panel**.

## 3. Try it

1. Enter coordinates such as `25.033, 121.565` or a place name in the floating panel and tap **Teleport here**.
2. Open any maps app; the position should now be there.
3. Push the joystick to move; the six chips set the speed.
4. When done, tap **Stop** and close the panel, or tap **Stop** in the notification. **If you leave it running, your phone's location stays fake.**

## 4. Report problems

Send device model, Android version, what you did, what you expected, what happened, plus a screenshot if possible,
to <willy78831@gmail.com> or <https://github.com/wlworks/JogApp/issues>.

## 5. Privacy

Jog has no network access and collects nothing. It never reads or uploads your real location. See the [privacy policy](privacy-policy).

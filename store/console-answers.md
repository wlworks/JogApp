# Play Console 申報答案草稿

對照 Play Console 左側「App content」與「Store presence」各表單，照著填即可。
所有答案都以 App 目前的實際行為為準：**不連網、不蒐集任何資料、沒有帳號、沒有廣告、沒有第三方 SDK。**
日後若加了 analytics、遠端 geocoding 或任何 SDK，這份要跟著改。

---

## App access（審核人員如何使用 App）

選 **「All or some functionality is restricted」**，因為審核人員不做開發者選項設定就什麼都測不到。
Instructions 欄位填：

```
Jog is a mock location tool for developers. It requires a one-time device setting before it can do anything:

1. Enable Developer options (Settings → About phone → tap "Build number" 7 times).
2. Open Settings → System → Developer options → "Select mock location app" and choose Jog.
3. Open Jog and grant the three permissions shown on the setup screen (display over other apps, location, notifications).
4. Tap "Start floating panel". A floating panel appears; enter coordinates such as 25.033, 121.565, tap "Teleport here", then "Start".
5. Any maps app will now show the simulated position. Use the joystick to move it.

No account or login is required. The app does not connect to the internet.
```

## Ads

**No, my app does not contain ads.**

## Content rating（內容分級問卷）

- Category：**Utility, Productivity, Communication, or Other**
- 所有問題（暴力、性、語言、管制物質、賭博、使用者互動、分享位置、購買數位商品等）全部答 **No**。
  - 「Does the app share the user's current location with other users?」→ **No**。App 不讀取真實位置，也沒有使用者之間的互動。
- 預期分級：Everyone / PEGI 3 / 全年齡。

## Target audience and content

- Target age group：**18 and over** 只勾這一個。
- 不要勾任何 13 歲以下的級距，否則會落入 Families 政策，要多填一大堆。
- 「Appeal to children」問題答 **No**。

## News apps

**No.**

## COVID-19 contact tracing and status apps

**No.**

## Data safety

- Does your app collect or share any of the required user data types? → **No**
- 選了 No 之後其餘資料類型都不必填。
- Is all of the user data collected by your app encrypted in transit? → 不會出現（沒有資料）。
- Do you provide a way for users to request that their data is deleted? → 不會出現（沒有資料）。

依據：App 沒有 INTERNET 權限；沒有 analytics、crash reporting、廣告 SDK；沒有任何持久化儲存；
定位權限只用來註冊模擬定位供應者，不讀取真實位置。

## Government apps

**No.**

## Financial features

**My app doesn't provide any financial features.**

## Health

**My app does not have any health features.**

## Privacy policy

```
https://wlworks.github.io/JogApp/privacy-policy
```

## Foreground service permissions（前景服務類型申報）

Console 會列出 manifest 裡宣告的類型，Jog 只有 **location**。

Description 欄位填：

```
Jog runs a foreground service of type "location" while the user has started the floating mock-location panel. The service registers a mock location provider and pushes the simulated coordinates the user has entered at 10 Hz so that the app under test always has a fresh fix. A persistent notification with a "Stop" action is shown for the whole time the service runs, so the user can end the simulation at any moment, even while another app is in the foreground. The service stops and the notification is removed when the user taps Stop, closes the floating panel, or the app is removed from the mock location setting. The service does not read the device's real location and does not access the network.
```

Video link：把 `store/fgs-demo.mp4` 上傳到 YouTube（設為「不公開」即可），連結貼在這裡。
影片內容（約 64 秒，Google 地圖為背景）：啟動懸浮面板 → 輸入座標瞬移到東京澀谷，地圖跟著跳 → 切 Drive、面板拖到下方、
地圖放大 → 搖桿往北再往東推，地圖隨之捲動 → 拉下通知列看到常駐通知「Simulating · Drive」→ 按通知的 Stop → 面板與通知消失。
這支影片只用在這個申報欄位；商店頁的截圖與宣傳影片不能出現第三方 App 畫面。

## Photo and video permissions

不會出現，App 沒有相關權限。

## Location permissions

這一項只針對 `ACCESS_BACKGROUND_LOCATION`。Jog 沒有宣告，Console 不會要求填寫。**不要為了方便加上這個權限。**

---

## Store presence → Main store listing

文案與素材見 [listing.md](listing.md)：

| 欄位 | 來源 |
|---|---|
| App name / Short description / Full description | `listing.md`，預設語言 English，再加 Chinese (Traditional) |
| App icon 512×512 | `icon-512.png` |
| Feature graphic 1024×500 | `feature-graphic-1024x500.png` |
| Phone screenshots | `screenshots/en/` 與 `screenshots/zh-TW/` |
| App category | Application → **Tools** |
| Tags | Developer tools 類 |
| Contact details | Email：willy78831@gmail.com；Website：https://wlworks.github.io/JogApp/ |

## Store settings

- App category：**Tools**
- Contact email：willy78831@gmail.com（會公開）
- External marketing：關掉也無妨

---

## Testing → Closed testing

1. 建一個 track（預設 Alpha 即可）。
2. Testers 分頁 → Create email list → 貼 12 位以上測試者的 Google 帳號 email。
3. 上傳 `app-release.aab`，Release notes 隨便寫一行。
4. Review 後 Rollout。把「Join on the web」連結發給測試者，請他們**加入後 14 天內不要移除 App**。
5. 14 天後 Dashboard 會出現 **Apply for production access**，填問卷後等審核。

## Production 上傳前

- `versionCode` 比封閉測試用的那版大。
- 第一次上傳時勾 **Use Play App Signing**（現在是預設，Google 會產正式簽章金鑰，你手上的是 upload key）。

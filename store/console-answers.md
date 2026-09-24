# Play Console 申報答案草稿

對照 Play Console 左側「App content」與「Store presence」各表單，照著填即可。
所有答案都以 App 目前的實際行為為準：**不連網、不蒐集任何資料、沒有帳號、沒有廣告、沒有第三方 SDK。**
日後若加了 analytics、遠端 geocoding 或任何 SDK，這份要跟著改。

---

## Sign in details（舊稱 App access）

**Is any part of your app restricted? → No。**

這個表單 2026 年改版後只問「登入資訊」：帳號登入、付款、推薦碼、一次性 PIN、生物辨識、要在另一台裝置操作。
Jog 一項都沒有。開發者選項的「選取模擬位置資訊應用程式」是系統設定，不是這裡定義的 restricted；
選 Yes 會被要求填測試帳號與密碼，填不出來。

審核人員需要的前置步驟已經放在兩個地方，不必再另外交代：

- 商店完整說明的 REQUIREMENTS 段落（`listing.md`）明講要先在開發者選項選 Jog。
- App 內設定頁第四列會直接帶進開發者選項頁面。

若日後 Google 來信問怎麼測，把下面這段回給他們：

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

- Email address：`willy78831@gmail.com`，與商店資訊的聯絡信箱一致。
- Category：**All Other App Types**（舊介面叫 Utility, Productivity, Communication, or Other）。
- 勾 IARC Terms of Use。
- 問卷所有問題（暴力、性、語言、管制物質、賭博、使用者互動、分享位置、購買數位商品、不受限制的網路瀏覽等）全部答 **No**。
  - 「Does the app share the user's current location with other users?」→ **No**。App 不讀取真實位置，也沒有使用者之間的互動。
  - 網路瀏覽／連網相關 → **No**，App 沒有 INTERNET 權限。
- 預期分級：Everyone / PEGI 3 / 全年齡。

## Target audience and content

- Target age group：**18 and over** 只勾這一個。
- 不要勾任何 13 歲以下的級距，否則會落入 Families 政策，要多填一大堆。
- 「Appeal to children」問題答 **No**。

## Advertising ID

**Does your app use advertising ID? → No。**

release 合併後的 manifest 沒有 `com.google.android.gms.permission.AD_ID`，相依裡也沒有任何廣告或分析 SDK。
日後若加了會用到廣告 ID 的 SDK，這裡要改 Yes，manifest 也要補權限，否則 Console 會擋 release。

## News apps

**No.**

## COVID-19 contact tracing and status apps

**No.**

## Data safety

- Does your app collect or share any of the required user data types? → **No**
- 選了 No 之後其餘資料類型都不必填。
- Is all of the user data collected by your app encrypted in transit? → 不會出現（沒有資料）。
- Do you provide a way for users to request that their data is deleted? → 不會出現（沒有資料）。

依據：App 沒有 INTERNET 權限；沒有 analytics、crash reporting、廣告 SDK；唯一的持久化是上次的模擬座標，
存在裝置本機 App 私有空間、不傳出裝置，不算 Data Safety 定義的「蒐集」；
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

「What tasks require your app to use the FOREGROUND_SERVICE_LOCATION permission?」
勾 **Background location updates → Other**，其他不勾。服務的工作就是持續發布（模擬的）定位更新，
不是分享、導航或地理圍欄；「Other tasks → Other」是給與定位無關的工作用的，會和 location 類型對不上。

勾了之後展開的 Description 欄位填：

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

上傳前要先把左側 **Dashboard** 上「Set up your app」那串全部打勾（Sign in details、Ads、Content rating、
Target audience、News、Data safety、Government、Financial、Health、Privacy policy、Store listing），
答案都在上面。沒填完 Console 不讓你建 release。

### 建立 track 與測試名單

1. 左側 **Testing → Closed testing** → 預設有一個 Alpha track，直接用，按 **Manage track**。
2. **Testers** 分頁 → **Create email list**：
   - List name 隨意，例如 `Jog testers`。
   - Add email addresses：一次貼一個，或用逗號分開；也可以上傳 CSV（一行一個 email，沒有標題列）。
   - 至少 **12 個**不同的 Google 帳號。多找 2、3 個備用，有人中途退出才不會重算。
   - 存檔後勾選這個名單。
3. **Feedback URL or email address** 填 `willy78831@gmail.com`。
4. 同一頁 **Countries / regions**：至少選 Taiwan；測試者有人在國外就把那些國家也加進去，或直接選 all。

### 建立 release

1. **Releases** 分頁 → **Create new release**。
2. 第一次會問 **Play App Signing**：選 **Use Google-generated key**（預設）→ Continue。你手上的 keystore 從此是 upload key。
3. **App bundles** → 上傳 `app/build/outputs/bundle/release/app-release.aab`（versionCode 1, 0.1.0）。
   上傳後 Console 會顯示警告清單，常見的：
   - 「沒有 deobfuscation file」→ 可以不理，或把 `app/build/outputs/mapping/release/mapping.txt` 一起上傳，之後崩潰報告會好讀。
   - 「App 支援 XX 台裝置」→ 資訊而已。
4. **Release name** 會自動帶 `1 (0.1.0)`，不用改。
5. **Release notes** 貼 `store/release-notes.md` 的那段（含 `<en-US>` `<zh-TW>` 標記）。
6. **Next** → 看 errors / warnings → **Save and publish**（舊介面叫 Start rollout to Closed testing）。
7. 狀態會先是 **In review**。第一次 review 通常幾小時到幾天；通過後測試者才能加入。

### 通知測試者

- 加入連結（Console 的 Testers 分頁「How testers join your test」→ Join on the web）：
  `https://play.google.com/apps/testing/com.wlworks.jog`
- 直接把測試指南丟給他們：`https://wlworks.github.io/JogApp/testing`，裡面有加入步驤、開發者選項設定與「14 天不要移除」的提醒。
- 請他們用**你加進名單的那個 Google 帳號**登入手機再開連結，帳號不對會顯示找不到。

### 等 14 天

- Closed testing 頁面會顯示目前 opt-in 人數與天數。12 人以上連續 14 天，Dashboard 出現 **Apply for production access**。
- 期間可以照常上傳新版到同一個 track（`versionCode` +1），不影響計時。
- 申請時要回答一份問卷：測試者怎麼招募、收到什麼回饋、改了什麼、App 已 ready 的理由。把測試者的回饋留下來，到時候有東西寫。

## Production 上傳前

- `versionCode` 比封閉測試用的那版大。
- 第一次上傳時勾 **Use Play App Signing**（現在是預設，Google 會產正式簽章金鑰，你手上的是 upload key）。

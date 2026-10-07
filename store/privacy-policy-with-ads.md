# 隱私權政策：打開廣告時要換上的段落（草稿）

> **這份還沒生效，也不會被發佈**（`store/` 不在 GitHub Pages 的發佈範圍內）。
> 線上的政策是 `docs/privacy-policy.md`，描述的是目前不含廣告的版本。
> 要出 `jog.ads=true` 的版本時，把下面的段落換進 `docs/privacy-policy.md`、更新生效日期，
> 並在含廣告的版本送審**之前** push。流程見 `PLAY_RELEASE.md` 第 9 節。
>
> 蒐集項目依據 Google 公布的 AdMob SDK 資料揭露說明撰寫；換上去之前請對照 AdMob 當時的官方說明再核對一次，
> 加了 UMP 同意流程後「你的選擇」一節也要補上實際的入口位置。

---

## English

Replace the opening summary with:

> The short version: **Jog itself does not collect, store or transmit your personal data. The app shows ads through Google AdMob, and AdMob collects some device data to do so, as described below.**

Replace the whole "Data collection" section with:

### Data collection

Jog itself does not collect any personal or device data, and has no analytics or crash reporting of its own.

Jog shows rewarded ads through **Google AdMob**. Auto move and Health sync are unlocked by watching an ad; the rest of the app works without ads. To request and show ads, the AdMob SDK connects to the internet and may collect and share with Google and its advertising partners:

- device and advertising identifiers (such as the Android advertising ID and app set ID),
- IP address, from which an approximate location may be derived,
- how you interact with ads and with the app (for example ad views and taps),
- diagnostic information such as crash and performance data.

Google uses this data for advertising, analytics and fraud prevention. Details: <https://policies.google.com/technologies/partner-sites>

The simulated coordinates you enter in Jog, your real location, and anything Jog writes to Health Connect are **not** shared with AdMob.

### Your choices

- You can reset or delete your advertising ID, or opt out of ad personalisation, in Android Settings → Privacy → Ads.
- In regions where consent is required, Jog asks for your choice before requesting ads, and you can change it later from the setup screen.
- If you do not watch an ad, auto move and Health sync stay locked; no ad is requested until you choose to watch one.

In the "Permissions" list, add:

- **Internet access** (`INTERNET`, `ACCESS_NETWORK_STATE`) and **advertising ID** (`AD_ID`). Used only by the Google AdMob SDK to load and show ads.

---

## 繁體中文

開頭的一句話版本改成：

> 一句話版本：**Jog 本身不蒐集、不儲存、不傳輸你的個人資料。App 透過 Google AdMob 顯示廣告，AdMob 為此會蒐集部分裝置資料，詳見下文。**

「資料蒐集」整節換成：

### 資料蒐集

Jog 本身不蒐集任何個人或裝置資料，也沒有自己的分析或當機回報。

Jog 透過 **Google AdMob** 顯示獎勵廣告。自動移動與 Health 同步需要先看一則廣告才能使用，其餘功能不需要看廣告。為了請求與顯示廣告，AdMob SDK 會連線到網際網路，並可能蒐集下列資料、與 Google 及其廣告合作夥伴分享：

- 裝置與廣告識別碼（例如 Android 廣告 ID、app set ID）
- IP 位址（可據以推得概略位置）
- 你與廣告及 App 的互動（例如廣告的曝光與點擊）
- 診斷資訊，例如當機與效能資料

Google 將這些資料用於廣告、分析與防止詐欺。詳情：<https://policies.google.com/technologies/partner-sites>

你在 Jog 輸入的模擬座標、你的真實位置、以及 Jog 寫入 Health Connect 的資料，都**不會**提供給 AdMob。

### 你的選擇

- 你可以在 Android 設定 → 隱私權 → 廣告 重設或刪除廣告 ID，或停用個人化廣告。
- 在需要取得同意的地區，Jog 會在請求廣告前先詢問你的選擇，之後也能從設定畫面更改。
- 不看廣告的話，自動移動與 Health 同步會維持鎖定；在你選擇觀看之前，App 不會請求任何廣告。

「權限與用途」清單加上：

- **網路存取**（`INTERNET`、`ACCESS_NETWORK_STATE`）與**廣告 ID**（`AD_ID`）。只由 Google AdMob SDK 用來載入與顯示廣告。

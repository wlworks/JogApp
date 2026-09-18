# Jog — 懸浮視窗模擬定位工具（骨架）

開發測試用。透過開發者選項的 Mock Location App 機制注入假座標，
搭配懸浮視窗上的搖桿做即時微調移動。

## 開起來的步驟

1. `./gradlew :app:installDebug`
2. 打開 App，依畫面上的四個勾勾逐一完成：
   - 懸浮視窗權限
   - 定位權限
   - 通知權限（Android 13+）
   - **開發者選項 → 選取模擬位置資訊應用程式 → Jog**（這步無法程式代勞）
3. 按「啟動懸浮視窗」。

## 檔案地圖

| 路徑 | 職責 |
|---|---|
| `core/GeoMath.kt` | 大圓航線推算、搖桿向量 → 方位角 |
| `core/SpeedTier.kt` | 速度檔次定義 |
| `core/CoordinateParser.kt` | 十進位 / DMS 座標解析 |
| `data/GeocodeRepository.kt` | 座標解析 → 系統 Geocoder（4s timeout）。不打任何外部服務 |
| `mock/MockLocationEngine.kt` | LocationManager test provider + Fused mock mode |
| `mock/MovementController.kt` | 10Hz tick，把搖桿輸入換成座標位移 |
| `state/MockState.kt` | 程序內唯一狀態來源 |
| `service/OverlayHost.kt` | Compose 掛進 WindowManager 的宿主 |
| `service/FloatingWindowService.kt` | 前景服務，串起以上所有東西 |
| `ui/` | 懸浮面板、搖桿、速度選擇器 |
| `res/values/` | 英文字串（預設）；繁中在 `values-zh-rTW/` |
| `res/mipmap-anydpi-v26/` | adaptive icon；前景向量在 `drawable/ic_launcher_foreground.xml` |
| `docs/` | GitHub Pages：首頁與隱私權政策（英文 + 繁中） |
| `store/` | Play 商店文案草稿與 `render_store_assets.py` 產出的圖示／主題圖 |
| `tools/` | `check_style.py`（格式檢查）、`render_store_assets.py`（商店素材） |

## Release build

```bash
./gradlew :app:bundleRelease   # → app/build/outputs/bundle/release/app-release.aab
```

簽章資訊讀自 repo 根目錄的 `keystore.properties`（已 gitignore，範本見 `keystore.properties.example`）。
沒有這個檔也能 build，只是產物未簽章。上架流程與政策檢查清單見 [PLAY_RELEASE.md](PLAY_RELEASE.md)。

## 刻意不做的

**沒有遠端 geocoding 備援。** 地點解析只靠座標解析與系統 `Geocoder`，所以整個 App
**不需要 INTERNET 權限**、沒有 API key 要管、Data Safety 也不必申報資料傳輸。
代價是少數沒有 geocoding backend 的機型查不到地名 —— 那些情況下 UI 會提示改輸入經緯度。

如果之後真的需要遠端備援，在 `GeocodeRepository` 的 `Outcome.Unavailable` 分支接上去即可，
其餘程式不必動。記得同時補回 INTERNET 權限與 Data Safety 申報。

## 還沒做的（刻意留白）

- 座標持久化 / 我的最愛
- 路線錄製與回放
- 抖動模擬（固定座標太乾淨，容易被反作弊偵測；測試場景多半不需要）
- 高度／室內樓層
- 單元測試（`GeoMath`、`CoordinateParser` 最值得先補）

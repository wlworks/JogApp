# Release notes

Play Console「Release notes」欄位接受多語系標記，整段貼進去即可。每個語系 500 字元上限。
語系代碼要和商店資訊已加入的語言一致：`en-US`、`zh-TW`；3.2.0 起再加 `ja-JP`、`ko-KR`、`es-419`、`pt-BR`、`de-DE`
（2026-10-15 新增的商店語系，文案見 `store/listing-locales.md`，未經母語校對）。

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
<ja-JP>
• フローティングパネルはタイトル以外のどこをドラッグしても動かせます。ジョイスティックの操作はこれまでどおりです。
• パネルは前回の位置で開き、画面の回転や展開で画面外にはみ出した場合は自動で戻ります。
• 広告を表示できないとき、ロック解除画面に理由と短いエラーコードを表示します。問題を報告する際に添えてください。
</ja-JP>
<ko-KR>
• 플로팅 패널을 제목뿐 아니라 아무 곳이나 드래그해 옮길 수 있습니다. 조이스틱은 이전과 똑같이 작동합니다.
• 패널이 마지막 위치에서 열리며, 화면을 회전하거나 펼쳐서 화면 밖으로 벗어나면 자동으로 돌아옵니다.
• 광고를 표시할 수 없을 때 잠금 해제 화면에 이유와 짧은 오류 코드가 표시됩니다. 문제를 신고할 때 함께 알려 주세요.
</ko-KR>
<es-419>
• Arrastra el panel flotante desde cualquier punto, no solo desde el título. El joystick funciona como antes.
• El panel se vuelve a abrir donde lo dejaste y regresa a la pantalla si queda fuera al girar el dispositivo o expandirlo.
• Si no se puede mostrar un anuncio, la pantalla de desbloqueo explica el motivo y muestra un código de error corto para incluir al reportar un problema.
</es-419>
<pt-BR>
• Arraste o painel flutuante de qualquer ponto, não só pelo título. O joystick funciona como antes.
• O painel reabre onde você o deixou e volta para a tela se ficar de fora ao girar o aparelho ou expandi-lo.
• Se não for possível exibir um anúncio, a tela de desbloqueio explica o motivo e mostra um código de erro curto para incluir ao relatar um problema.
</pt-BR>
<de-DE>
• Das schwebende Bedienfeld lässt sich jetzt an jeder Stelle verschieben, nicht nur am Titel. Der Joystick funktioniert wie bisher.
• Das Bedienfeld öffnet sich dort, wo du es zuletzt gelassen hast, und kehrt auf den Bildschirm zurück, wenn es nach dem Drehen oder Aufklappen herausragt.
• Kann keine Anzeige gezeigt werden, nennt der Freischaltbildschirm den Grund und einen kurzen Fehlercode, den du beim Melden eines Problems angeben kannst.
</de-DE>
```

## 3.3.0（versionCode 30300）— 最愛地點、模擬位置設定引導

```
<en-US>
• Favorites: tap the star next to the coordinates to save the current spot, then jump back to it from the new star tab. Up to 20 places, stored only on your device.
• The setup screen now checks whether Jog is selected as the mock location app and shows the exact steps for what is missing, including turning on Developer options.
• If you press Start before that is done, the panel links straight to the steps.
</en-US>
<zh-TW>
• 最愛地點：點座標旁的星號存下目前位置，之後從新的星號分頁一鍵跳回去。最多 20 筆，只存在你的裝置上。
• 設定畫面會偵測 Jog 是否已選為模擬位置應用程式，並依目前卡住的步驟顯示說明，包含如何開啟開發者選項。
• 還沒設定就按開始時，面板會直接提供設定步驟的入口。
</zh-TW>
<ja-JP>
• お気に入り：座標の横の星をタップして現在地を保存し、新しい星のタブからすぐに戻れます。最大 20 件、端末内にのみ保存されます。
• 設定画面で Jog が仮の現在地情報アプリに選択されているかを確認し、足りない手順（開発者向けオプションの有効化を含む）を表示します。
• 設定前に開始を押した場合、パネルから手順をすぐに開けます。
</ja-JP>
<ko-KR>
• 즐겨찾기: 좌표 옆의 별을 눌러 현재 위치를 저장하고 새 별 탭에서 바로 돌아갈 수 있습니다. 최대 20개이며 기기에만 저장됩니다.
• 설정 화면에서 Jog가 모의 위치 앱으로 선택되었는지 확인하고, 개발자 옵션 켜기를 포함해 남은 단계를 안내합니다.
• 설정 전에 시작을 누르면 패널에서 바로 안내를 열 수 있습니다.
</ko-KR>
<es-419>
• Favoritos: toca la estrella junto a las coordenadas para guardar el lugar actual y vuelve a él desde la nueva pestaña de estrella. Hasta 20 lugares, guardados solo en tu dispositivo.
• La pantalla de configuración comprueba si Jog es la app de ubicación simulada y muestra los pasos que faltan, incluido cómo activar las Opciones para desarrolladores.
• Si tocas Iniciar antes de completarlo, el panel lleva directo a esos pasos.
</es-419>
<pt-BR>
• Favoritos: toque na estrela ao lado das coordenadas para salvar o local atual e volte a ele pela nova aba de estrela. Até 20 locais, salvos apenas no seu dispositivo.
• A tela de configuração verifica se o Jog é o app de local fictício e mostra os passos que faltam, incluindo como ativar as Opções do desenvolvedor.
• Se você tocar em Iniciar antes disso, o painel leva direto a esses passos.
</pt-BR>
<de-DE>
• Favoriten: Tippe auf den Stern neben den Koordinaten, um den aktuellen Ort zu speichern, und springe über den neuen Stern-Tab dorthin zurück. Bis zu 20 Orte, nur auf deinem Gerät gespeichert.
• Der Einrichtungsbildschirm prüft, ob Jog als App für simulierte Standorte ausgewählt ist, und zeigt die fehlenden Schritte an, auch wie du die Entwickleroptionen aktivierst.
• Tippst du vorher auf Start, führt das Bedienfeld direkt zu diesen Schritten.
</de-DE>
```

之後每次上傳新版：`versionCode` +1，在這裡加一段，只寫這版改了什麼。

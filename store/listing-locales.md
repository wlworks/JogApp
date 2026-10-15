# 商店文案：簡短說明候選與新增語系

`store/listing.md` 是 en-US 與 zh-TW 的正式版本；這份是 2026-10 為了提高曝光準備的草稿：

1. 簡短說明的候選版本（en-US、zh-TW）。2026-10-15 選定 **A 案**，已回寫到 `store/listing.md`；B、C 留作之後商店資訊實驗（A/B 測試）的對照。
2. 新增五個語系的完整商店文案：ja-JP、ko-KR、es-419、pt-BR、de-DE。

所有文字都對照過 [PLAY_RELEASE.md](../PLAY_RELEASE.md)「商店素材的紅線」：不提遊戲、打卡、交友、繞過、偵測，
也不提任何第三方 App；每個語系都明講「需要啟用開發者選項」。修改後請重查。

新語系的簡短說明是照下面 **A 案**翻的；之後若改用別的候選，這五個語系也要跟著改。

截圖先沿用 en-US 那一組（Play 會在沒有在地化截圖的語系顯示預設語系的截圖）。

---

## 1. 簡短說明候選（≤ 80 字元）

目前的版本 *"Developer tool for testing location features. Requires Developer options."*
幾乎沒有會被搜尋的字，「Requires Developer options」放在第一眼也會勸退人。這個條件完整說明裡已經寫了，簡短說明不必再提。

### English

| 案 | 文字 | 字元 |
|---|---|---|
| **A（推薦）** | `Mock GPS location with joystick, teleport and auto move for testing apps` | 72 |
| B | `Test location features at your desk: mock GPS, joystick and auto move` | 69 |
| C | `Mock location tool for developers: joystick, teleport, random jump, auto move` | 77 |

A 把 *mock GPS*、*location*、*joystick*、*teleport* 都放進去，而且開頭就是最常被搜尋的 *Mock GPS location*。
B 比較像標語，關鍵字少一點。C 把 *developers* 放進去，定位最清楚，但 *GPS* 不見了（標題已經有）。

刻意**沒有**用 *fake GPS*：雖然搜尋量大，但「fake」容易被審核解讀成欺騙用途，和開發工具的定位衝突。

### 繁體中文

| 案 | 文字 | 字元 |
|---|---|---|
| **A（推薦）** | `模擬 GPS 定位：搖桿即時移動、瞬移、自動移動，在座位上測試定位功能` | 35 |
| B | `開發測試用模擬定位工具：搖桿、瞬移、隨機跳與自動移動` | 26 |
| C | `在座位上測試定位功能：模擬 GPS、搖桿移動、瞬移與自動移動` | 30 |

---

## 2. 日本語（ja-JP）

**タイトル**（≤ 30 字元）

```
Jog：GPS 位置情報モックテスター
```

**簡単な説明**（≤ 80 字元）

```
ジョイスティック・瞬間移動・自動移動で GPS 位置情報をモックしてアプリをテスト
```

**詳しい説明**（≤ 4000 字元）

```
Jog は Android の開発者・QA テスター向けのモック位置情報ツールです。端末にシミュレーションした座標を送り、ジオフェンス、距離計算、移動処理など位置情報に関わる機能を、席を離れずにテストできます。

主な機能
• 地名または緯度・経度を入力して、シミュレーション位置をその場所へ瞬間移動。
• フローティングパネルのジョイスティックで、テスト対象のアプリを前面に出したまま位置をリアルタイムに微調整。パネルは画面の一部だけを使うコンパクトな横長レイアウトで、速度の段階は常に表示されます。小さなバブルに折りたたんでもジョイスティックはそのまま使えます。
• ゆっくり歩きから鉄道まで 6 段階の速度で、現実的な速さの移動をテスト。
• 近くのランダムな地点へジャンプ。距離は速度の段階に連動し、徒歩なら約 80〜250 m、鉄道なら 5〜15 km まで。
• 自動移動：選んだ速度でシミュレーション位置が自ら動き回り、実際の歩行や運転のように少しずつ向きを変えます。ジョイスティックでいつでも方向を変えられます。
• Health 同期（任意）：ゆっくり歩き・徒歩・ランニングの速度で移動している間、シミュレーションした歩数と距離を 1 分ごとに Health Connect に書き込みます。これらのデータを読み取るアプリのテストに使えます。
• 自動移動と Health 同期は、短いリワード広告を 1 本見るとフローティングパネルを閉じるまで使えます。瞬間移動、ジョイスティック、ランダムジャンプなどその他の機能は広告なしで使えます。
• 最後のシミュレーション位置を記憶し、次回起動時に復元します。
• シミュレーション中は常駐通知を表示し、いつでも停止できます。

必要な設定
Jog は、開発者向けオプションを有効にして Jog を「仮の現在地情報アプリ」に選択した後でのみ動作します。この選択を Jog が代わりに行うことはできず、選択するまでは何もシミュレーションできません。

プライバシー
• Jog があなたの実際の位置情報を読み取ったり、保存したり、アップロードしたりすることはありません。
• 最後のシミュレーション位置は続きから使えるよう端末内にのみ保存されます。
• Health 同期は初期状態でオフです。端末上の Health Connect にシミュレーションした歩数と距離を「手動入力」として書き込むだけで、健康データを読み取ることはありません。
• アカウントは不要で、Jog 独自の分析機能もありません。
• 広告は Google AdMob が配信します。AdMob は広告表示のために端末識別子やおおよその位置情報を収集することがあります。詳しくはプライバシー ポリシーをご覧ください。シミュレーション座標と Health Connect のデータが AdMob に共有されることはありません。
• 欧州経済領域、英国、スイスのユーザーには広告への同意を確認し、「広告のプライバシー設定」からいつでも変更できます。

権限
• 位置情報：Android がモック位置情報プロバイダの登録に必要とします。
• 他のアプリの上に重ねて表示：フローティング操作パネル。
• 通知：シミュレーション中の停止操作。
• Health Connect の歩数と距離の書き込み（任意）：Health 同期をオンにしたときだけ使用します。
• インターネットと広告 ID：Google AdMob の広告表示にのみ使用します。

Jog はソフトウェアの開発とテストのためのツールです。責任を持って、テスト対象のアプリでのみご使用ください。
```

---

## 3. 한국어（ko-KR）

**제목**（≤ 30 字元）

```
Jog: 모의 GPS 위치 테스터
```

**간단한 설명**（≤ 80 字元）

```
조이스틱, 순간 이동, 자동 이동으로 GPS 위치를 모의하여 앱을 테스트하세요
```

**자세한 설명**（≤ 4000 字元）

```
Jog는 Android 개발자와 QA 테스터를 위한 모의 위치 도구입니다. 시뮬레이션 좌표를 기기에 전달하여 지오펜스, 거리 계산, 이동 처리 등 위치 기반 기능을 자리를 떠나지 않고 테스트할 수 있습니다.

주요 기능
• 장소 이름이나 위도/경도를 입력해 시뮬레이션 위치를 그곳으로 순간 이동합니다.
• 테스트 중인 앱을 앞에 띄운 채로 플로팅 패널의 조이스틱으로 위치를 실시간으로 조정합니다. 패널은 화면의 일부만 차지하는 작은 가로형 레이아웃이며 속도 단계가 항상 표시됩니다. 패널을 작은 버블로 접어도 조이스틱은 계속 사용할 수 있습니다.
• 아주 느린 걸음부터 철도까지 6단계 속도로 현실적인 속도의 이동을 테스트합니다.
• 근처의 임의 지점으로 점프합니다. 거리는 속도 단계에 따라 달라져 걷기는 약 80–250m, 철도는 최대 5–15km입니다.
• 자동 이동: 선택한 속도로 시뮬레이션 위치가 스스로 돌아다니며 실제 걷기나 운전처럼 조금씩 방향을 바꿉니다. 언제든 조이스틱으로 방향을 바꿀 수 있습니다.
• Health 동기화(선택): 아주 느린 걸음, 걷기, 달리기 속도로 이동하는 동안 시뮬레이션한 걸음 수와 거리를 1분마다 Health Connect에 기록하여 이 데이터를 읽는 앱을 테스트할 수 있습니다.
• 자동 이동과 Health 동기화는 짧은 보상형 광고를 한 번 시청하면 플로팅 패널을 닫을 때까지 사용할 수 있습니다. 순간 이동, 조이스틱, 임의 점프 등 다른 기능은 모두 광고 없이 사용할 수 있습니다.
• 마지막 시뮬레이션 위치를 기억해 다음에 앱을 열 때 복원합니다.
• 시뮬레이션 중에는 상시 알림이 표시되어 언제든 중지할 수 있습니다.

요구 사항
Jog는 개발자 옵션을 활성화하고 Jog를 모의 위치 앱으로 선택한 후에만 작동합니다. Jog가 이 선택을 대신할 수 없으며, 선택하기 전에는 아무것도 시뮬레이션할 수 없습니다.

개인정보 보호
• Jog는 실제 위치를 읽거나 저장하거나 업로드하지 않습니다.
• 마지막 시뮬레이션 위치는 이어서 사용할 수 있도록 기기에만 저장됩니다.
• Health 동기화는 기본적으로 꺼져 있습니다. 기기의 Health Connect에 시뮬레이션한 걸음 수와 거리를 '직접 입력'으로 표시해 기록할 뿐이며, 건강 데이터를 읽지 않습니다.
• 계정이 필요 없으며 Jog 자체의 분석 기능도 없습니다.
• 광고는 Google AdMob이 제공하며, AdMob은 광고 표시를 위해 기기 식별자와 대략적인 위치를 수집할 수 있습니다. 자세한 내용은 개인정보처리방침을 참고하세요. 시뮬레이션 좌표와 Health Connect 데이터는 AdMob과 공유되지 않습니다.
• 유럽 경제 지역, 영국, 스위스의 사용자에게는 광고 동의를 요청하며, '광고 개인정보 설정'에서 언제든 변경할 수 있습니다.

권한
• 위치: Android에서 모의 위치 제공자를 등록하려면 필요합니다.
• 다른 앱 위에 표시: 플로팅 제어 패널.
• 알림: 시뮬레이션 중 중지 컨트롤.
• Health Connect 걸음 수 및 거리 쓰기(선택): Health 동기화를 켰을 때만 사용합니다.
• 인터넷 및 광고 ID: Google AdMob의 광고 표시에만 사용합니다.

Jog는 소프트웨어 개발과 테스트를 위한 도구입니다. 책임감 있게, 테스트 중인 앱에만 사용하세요.
```

---

## 4. Español (Latinoamérica)（es-419）

**Título**（≤ 30 字元）

```
Jog: Probador de GPS simulado
```

**Descripción breve**（≤ 80 字元）

```
Simula la ubicación GPS con joystick, teletransporte y movimiento automático
```

**Descripción completa**（≤ 4000 字元）

```
Jog es una herramienta de ubicación simulada para desarrolladores y testers de QA de Android. Envía coordenadas simuladas al dispositivo para que puedas probar funciones basadas en la ubicación (geocercas, cálculo de distancias, manejo del movimiento) sin levantarte del escritorio.

CÓMO FUNCIONA
• Escribe un lugar o una latitud y longitud y teletransporta allí la posición simulada.
• Un panel flotante con joystick mueve la posición en tiempo real mientras la app que pruebas sigue en primer plano. El panel tiene un diseño horizontal compacto que ocupa solo una pequeña parte de la pantalla, con los niveles de velocidad siempre a la vista. Contrae el panel en una burbuja y el joystick sigue disponible.
• Seis niveles de velocidad, desde un paso muy lento hasta velocidad de tren, para probar el movimiento a ritmos realistas.
• Salta a un punto cercano al azar. La distancia depende del nivel de velocidad: unos 80–250 m al caminar y hasta 5–15 km a velocidad de tren.
• Movimiento automático: la posición simulada se desplaza sola al nivel de velocidad elegido y gira poco a poco como en una caminata o un trayecto en auto reales. Usa el joystick en cualquier momento para cambiar de dirección.
• Sincronización con Health (opcional): mientras la posición se mueve a paso lento, caminando o corriendo, Jog escribe cada minuto los pasos y la distancia simulados en Health Connect, para que puedas probar apps que leen esos datos.
• El movimiento automático y la sincronización con Health se desbloquean al ver un anuncio corto con recompensa, válido hasta que cierres el panel flotante. El teletransporte, el joystick, el salto aleatorio y todo lo demás funcionan sin anuncios.
• Jog recuerda la última posición simulada y la restaura la próxima vez que abres la app.
• Mientras la simulación está activa se muestra una notificación persistente para que puedas detenerla en cualquier momento.

REQUISITOS
Jog solo funciona después de activar las Opciones para desarrolladores y seleccionar Jog como app de ubicación de prueba. Jog no puede hacer esa selección por ti y no puede simular nada hasta que la hagas.

PRIVACIDAD
• Jog nunca lee, guarda ni sube tu ubicación real.
• La última posición simulada se guarda solo en tu dispositivo para que puedas continuar donde lo dejaste.
• La sincronización con Health está desactivada de forma predeterminada. Solo escribe pasos y distancia simulados en Health Connect de tu dispositivo, marcados como ingresados manualmente. Jog nunca lee datos de salud.
• Sin cuentas y sin analíticas propias de Jog.
• Los anuncios los muestra Google AdMob, que puede recopilar identificadores del dispositivo y la ubicación aproximada para mostrar anuncios; consulta la política de privacidad para más detalles. Tus coordenadas simuladas y tus datos de Health Connect nunca se comparten con AdMob.
• A los usuarios del EEE, el Reino Unido y Suiza se les pide su consentimiento para los anuncios y pueden cambiarlo en cualquier momento en Opciones de privacidad de anuncios.

PERMISOS
• Ubicación: Android lo exige para registrar un proveedor de ubicación de prueba.
• Mostrar sobre otras apps: el panel de control flotante.
• Notificaciones: el control para detener la simulación mientras está activa.
• Health Connect, escribir pasos y distancia (opcional): solo se usa cuando activas la sincronización con Health.
• Internet e ID de publicidad: solo los usa Google AdMob para mostrar anuncios.

Jog está pensado para el desarrollo y las pruebas de software. Úsalo de forma responsable y solo con las apps que estás probando.
```

---

## 5. Português (Brasil)（pt-BR）

**Título**（≤ 30 字元）

```
Jog: Testador de GPS simulado
```

**Descrição curta**（≤ 80 字元）

```
Simule a localização GPS com joystick, teletransporte e movimento automático
```

**Descrição completa**（≤ 4000 字元）

```
Jog é uma ferramenta de localização simulada para desenvolvedores e testadores de QA Android. Ele envia coordenadas simuladas ao dispositivo para que você teste recursos baseados em localização (geofences, cálculo de distâncias, tratamento de movimento) sem sair da mesa.

COMO FUNCIONA
• Digite o nome de um lugar ou uma latitude e longitude e teletransporte a posição simulada para lá.
• Um painel flutuante com joystick move a posição em tempo real enquanto o app em teste continua em primeiro plano. O painel tem um layout horizontal compacto que ocupa só uma pequena parte da tela, com os níveis de velocidade sempre à vista. Recolha o painel em uma bolha e o joystick continua disponível.
• Seis níveis de velocidade, de um passo bem lento até velocidade de trem, para testar o movimento em ritmos realistas.
• Pule para um ponto próximo aleatório. A distância acompanha o nível de velocidade: cerca de 80–250 m caminhando e até 5–15 km na velocidade de trem.
• Movimento automático: a posição simulada se desloca sozinha no nível de velocidade escolhido, virando aos poucos como em uma caminhada ou viagem de carro reais. Use o joystick a qualquer momento para mudar a direção.
• Sincronização com o Health (opcional): enquanto a posição se move a passo lento, caminhando ou correndo, o Jog grava a cada minuto os passos e a distância simulados no Health Connect, para você testar apps que leem esses dados.
• O movimento automático e a sincronização com o Health são liberados ao assistir a um anúncio curto premiado, válido até você fechar o painel flutuante. Teletransporte, joystick, pulo aleatório e todo o resto funcionam sem anúncios.
• O Jog lembra a última posição simulada e a restaura na próxima vez que você abrir o app.
• Durante a simulação, uma notificação fixa é exibida para que você possa pará-la a qualquer momento.

REQUISITOS
O Jog só funciona depois que você ativa as Opções do desenvolvedor e seleciona o Jog como app de local fictício. O Jog não pode fazer essa seleção por você e não consegue simular nada até que você a faça.

PRIVACIDADE
• O Jog nunca lê, armazena nem envia sua localização real.
• A última posição simulada é salva apenas no seu dispositivo, para você continuar de onde parou.
• A sincronização com o Health vem desativada. Ela só grava passos e distância simulados no Health Connect do seu dispositivo, marcados como inseridos manualmente. O Jog nunca lê dados de saúde.
• Sem contas e sem análises próprias do Jog.
• Os anúncios são exibidos pelo Google AdMob, que pode coletar identificadores do dispositivo e a localização aproximada para mostrar anúncios; veja a política de privacidade para mais detalhes. Suas coordenadas simuladas e seus dados do Health Connect nunca são compartilhados com o AdMob.
• Usuários do EEE, do Reino Unido e da Suíça recebem um pedido de consentimento para anúncios e podem alterá-lo a qualquer momento em Opções de privacidade de anúncios.

PERMISSÕES
• Localização: exigida pelo Android para registrar um provedor de local fictício.
• Sobrepor a outros apps: o painel de controle flutuante.
• Notificações: o controle para parar a simulação enquanto ela está ativa.
• Health Connect, gravar passos e distância (opcional): usado apenas quando você ativa a sincronização com o Health.
• Internet e ID de publicidade: usados apenas pelo Google AdMob para exibir anúncios.

O Jog foi criado para desenvolvimento e testes de software. Use com responsabilidade e apenas com os apps que você está testando.
```

---

## 6. Deutsch（de-DE）

**Titel**（≤ 30 字元）

```
Jog: Mock-GPS-Standorttester
```

**Kurzbeschreibung**（≤ 80 字元）

```
GPS-Standort simulieren mit Joystick, Teleport und Auto-Bewegung für App-Tests
```

**Vollständige Beschreibung**（≤ 4000 字元）

```
Jog ist ein Tool für simulierte Standorte für Android-Entwickler und QA-Tester. Es übergibt dem Gerät simulierte Koordinaten, damit du standortbezogene Funktionen wie Geofences, Entfernungsberechnungen und Bewegungsverarbeitung testen kannst, ohne den Schreibtisch zu verlassen.

SO FUNKTIONIERT ES
• Gib einen Ortsnamen oder Breiten- und Längengrad ein und teleportiere die simulierte Position dorthin.
• Ein schwebendes Bedienfeld mit Joystick verschiebt die Position in Echtzeit, während die getestete App im Vordergrund bleibt. Das Bedienfeld nutzt ein kompaktes Querformat, belegt nur einen kleinen Teil des Bildschirms und zeigt die Geschwindigkeitsstufen immer an. Klappst du es zu einer kleinen Blase zusammen, bleibt der Joystick verfügbar.
• Sechs Geschwindigkeitsstufen, vom langsamen Schleichen bis zur Bahngeschwindigkeit, um Bewegung in realistischem Tempo zu testen.
• Springe zu einem zufälligen Ort in der Nähe. Die Entfernung hängt von der Geschwindigkeitsstufe ab: etwa 80–250 m beim Gehen, bis zu 5–15 km bei Bahngeschwindigkeit.
• Auto-Bewegung: Die simulierte Position bewegt sich mit der gewählten Stufe von selbst und ändert die Richtung allmählich wie bei einem echten Spaziergang oder einer Autofahrt. Mit dem Joystick kannst du jederzeit lenken.
• Health-Synchronisierung (optional): Solange sich die Position mit Schleich-, Geh- oder Laufgeschwindigkeit bewegt, schreibt Jog einmal pro Minute die simulierten Schritte und die Strecke in Health Connect, damit du Apps testen kannst, die diese Daten lesen.
• Auto-Bewegung und Health-Synchronisierung werden durch eine kurze Videoanzeige mit Prämie freigeschaltet und gelten, bis du das schwebende Bedienfeld schließt. Teleport, Joystick, Zufallssprung und alles andere funktionieren ohne Werbung.
• Jog merkt sich die letzte simulierte Position und stellt sie beim nächsten Öffnen der App wieder her.
• Während die Simulation läuft, wird eine dauerhafte Benachrichtigung angezeigt, über die du sie jederzeit beenden kannst.

VORAUSSETZUNGEN
Jog funktioniert erst, nachdem du die Entwickleroptionen aktiviert und Jog als App für simulierte Standorte ausgewählt hast. Jog kann diese Auswahl nicht für dich treffen und erst danach etwas simulieren.

DATENSCHUTZ
• Jog liest, speichert oder überträgt niemals deinen echten Standort.
• Die letzte simulierte Position wird nur auf deinem Gerät gespeichert, damit du dort weitermachen kannst.
• Die Health-Synchronisierung ist standardmäßig aus. Sie schreibt nur simulierte Schritte und Strecken in Health Connect auf deinem Gerät, gekennzeichnet als manuell eingegeben. Jog liest niemals Gesundheitsdaten.
• Keine Konten und keine eigenen Analysen von Jog.
• Anzeigen werden von Google AdMob ausgeliefert, das zur Anzeigenauslieferung Gerätekennungen und den ungefähren Standort erfassen kann; Details findest du in der Datenschutzerklärung. Deine simulierten Koordinaten und Health-Connect-Daten werden niemals an AdMob weitergegeben.
• Nutzer im EWR, im Vereinigten Königreich und in der Schweiz werden um ihre Einwilligung zu Anzeigen gebeten und können sie jederzeit unter „Datenschutzoptionen für Anzeigen“ ändern.

BERECHTIGUNGEN
• Standort: von Android vorgeschrieben, um einen Anbieter für simulierte Standorte zu registrieren.
• Über anderen Apps einblenden: das schwebende Bedienfeld.
• Benachrichtigungen: die Stopp-Steuerung während der Simulation.
• Health Connect, Schritte und Strecke schreiben (optional): nur bei aktivierter Health-Synchronisierung.
• Internet und Werbe-ID: nur von Google AdMob zur Anzeigenauslieferung verwendet.

Jog ist für Softwareentwicklung und -tests gedacht. Bitte verantwortungsvoll und nur mit Apps verwenden, die du gerade testest.
```

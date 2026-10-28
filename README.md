# MeiLeseAbenteuer · Der Buchstabenwald

Offline-Androidspiel ohne Werbung oder Bezahlschranken. Der aktuelle Entwicklungsstand beginnt bei den ersten Buchstaben und führt anschließend in eine kurze Waldgeschichte mit ersten Wörtern.

## Einstieg

Acht kleine Waldkapitel führen **A, E, I, O, U, M, N und L** ein. Jedes Kapitel verbindet eine Begegnung mit einem Waldtier mit fünf Aufgaben: beide Buchstabenformen kennenlernen und den Laut hören, Großbuchstaben wiedererkennen, Kleinbuchstaben zuordnen, Großbuchstaben nachfahren, Kleinbuchstaben nachfahren. Startpunkte und nummerierte Spuren zeigen die Schreibbewegung. Die Vorführung löst eine Aufgabe nicht; das Kind muss die Form selbst nachfahren. Das kleine a ist einstöckig. Die Formen sind vorbereitende Schreibübungen, keine schulformspezifische Schriftvorlage oder freie Handschrifterkennung.

Erst nach allen acht Kapiteln startet **Die erste Sternenpost**: Nila übergibt dem Fuchs einen Brief. Taro möchte LIMO; am Baumhaus wird OMA zusammengesetzt. Drei versteckte Glühwürmchen, bewegte Figuren, Licht und Glockenklänge verbinden die Aufgaben mit der Waldwelt. Ausschließlich vorhandene Grafiken werden genutzt.

Geschaffte Buchstabenkapitel sind über das Blattsymbol wiederholbar. Der Stand wird nach jeder Aufgabe gespeichert. Eine unfertige Schreibform beginnt nach einem Neustart wieder am ersten Strich. Der Wortspielstand und alte Lerndaten bleiben unabhängig davon erhalten. Unter **Für Große** stehen Informationen, Sprachtest, Updates und ein geschützter Neustart bereit.

## Audio

Die vorhandenen Dateien `sounds/a.ogg`, `i.ogg`, `o.ogg`, `u.ogg`, `m.ogg`, `n.ogg` und `l.ogg` sind wieder eingebunden. E und die Erzählung werden mit einer deutschen Geräte-Offlinestimme gesprochen. Bei fehlender Stimme im Elternbereich eine deutsche Offline-Stimme einrichten. Die Lauttaste für Konsonanten verwendet die Aufnahme, nicht den Namen „em“, „en“ oder „el“. Die Dateien stammen unverändert aus der bisherigen App; Quellenhinweise sind unter `audio-originals` bzw. in der Git-Historie dokumentiert.

## Entwicklung und Prüfungen

- `bash test.sh`: Kapitel-/Lernfolge, Speicherformate und geometrische Schreibprüfung.
- `bash scripts/test-update-contract.sh`: Installationsquelle und APK-Verifikation.
- `python3 scripts/check-release-metadata.py`: Der bisherige öffentliche Feed bleibt konsistent.
- `PLAYWRIGHT_MODULE=/path/to/playwright node tests/browser.cjs`: Alle acht Kapitel mit echten Zeigereingaben, Wiederholung/Neustart, Handylayouts und anschließende Worträtsel.
- `ANDROID_SDK_ROOT=/path/to/sdk bash build-local.sh` und `bash test-android-build.sh`: Nativer APK- und Instrumentation-Build; Android 37.0 / Build Tools 36.0.0 / Java 17.
- `bash gradlew assembleDebug lintDebug`: Gradle und striktes Android-Lint.
- `bash scripts/run-emulator.sh`: Echte Android-WebView, alle Buchstabenformen über Touch-Ereignisse, Sternenpost, Update und Prozessneustart; CI auf API 30 und 37.0.
- `python3 scripts/prepare-release.py`: Nur eine zum frischen Quellbuild passende APK mit der ursprünglichen Update-Signatur verpacken.

Die Signatur wird lokal aus `.signing/` oder in Release-CI aus dem vorhandenen GitHub-Secret geladen. Private Schlüssel werden nicht eingecheckt. Pull-Request-CI verwendet einen temporären Testschlüssel. Die Testversion verändert den öffentlichen Update-Feed nicht.

Die Wortgeschichte nach dem Buchstabenpfad verwendet einen neuen Speicherplatz. Ein fertiger 0.4-Teststand überspringt weder die Buchstaben noch die erste Sternenpost dieser Lernfolge; die gespeicherten Daten der alten Testfassung werden dabei nicht verändert.

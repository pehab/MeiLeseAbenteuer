# Updates und Signierung

## Installationsquelle

Die Auswahl ist unabhängig davon, ob es ein Debug- oder Release-Build ist:

| Installation | Updateweg |
| --- | --- |
| Google Play (`com.android.vending`) | Google Play; kein GitHub-Fallback |
| Direkt installierte APK, ADB oder anderer Installer | Verifizierter GitHub-Feed |
| Quelle konnte nicht ermittelt werden | Keine GitHub-Abfrage; erneute Prüfung statt stiller Quelländerung |

Der Feed ist https://github.com/pehab/MeiLeseAbenteuer/releases/latest/download/MeiLeseAbenteuer-update.json.

Wie in den anderen MeiApps ist die tägliche Prüfung aktiv und abschaltbar. In dieser Kinder-App ist sie zusätzlich an den geöffneten Elternbereich gebunden. Hintergrundresultate werden zurückgehalten, sobald man wieder auf der Lichtung oder in einem Spiel ist. Erst ein erneutes Öffnen des Elternbereichs darf den Dialog zeigen. Das Zurückkehren aus Androids Installationsfreigabe wird ebenfalls nur dort verarbeitet. Es gibt keine Änderungen an anderen Apps.

Vor der Installation werden Dateigröße, SHA-256, Paketname, Versionscode, Versionsname und Signatur mit der installierten App verglichen. Ältere Versionen und fremde Signaturen werden abgelehnt. Der Android-Installer verlangt die normale Nutzerbestätigung. Gradle-Store-Release-Builds entfernen `REQUEST_INSTALL_PACKAGES`; Play-Installationen öffnen den Store.

## Releases

0.1.1 / Code 2 aktualisiert die vorab bereitgestellte APK 0.1.0 / Code 1. Beide tragen denselben privaten Signierschlüssel. Neue Releases müssen immer einen größeren Versionscode erhalten.

CI baut den nativen APK-Payload selbst, prüft ihn auf Android 11 und 17 und verifiziert anschließend das signierte Release-Paket. Wenn CI keinen privaten Schlüssel hat, darf sie ausschließlich eine bereits offline signierte APK verwenden, deren vollständiger Nutzinhalt mit dem frischen Build übereinstimmt und deren Signatur zur unveränderlichen 0.1.0-Baseline passt. Eine zufällige CI-Testsignatur wird niemals veröffentlicht.

Der `publish`-Job läuft nach den erfolgreichen Prüfungen nur bei Push auf `main` mit `[release]` in der Commit-Nachricht. APK, Prüfsumme und Update-JSON werden gemeinsam am Versionstag veröffentlicht. Vorhandene Versionstags werden nicht still überschrieben.

Für serverseitige Signierung kann der Repository-Owner `MEILESE_SIGNING_JSON` hinterlegen. Der private Ordner `.signing/` und sein Backup gehören niemals in Git. Das JSON besteht aus `alias` (`leseabenteuer`), `keystore` (Base64 der vorhandenen PKCS12-Datei) und `password`. Ohne dieses Secret bleibt der geprüfte Offline-Signaturweg möglich.

## Prüfungen

- `scripts/test-update-contract.sh`: echte Entscheidungen der Quellenauswahl sowie fremde URLs, manipulierte und abgeschnittene Dateien, ungültige Metadaten und Downgrades.
- `scripts/check-release-metadata.py`: tatsächlich ausgelieferter Feed gegen Repository, Version, APK-Größe, SHA-256 und vorhandene Offline-Audioassets.
- Android-Gerätetest: Installation einer auf den CI-Testschlüssel umsignierten Kopie der unveränderten 0.1.0-Baseline, Anlage eines Kinderprofils und Lernfortschritts, Update auf 0.1.1 und Prüfung der erhaltenen Daten. Die neue App durchläuft anschließend separat alle Spiele und einen Prozessneustart.
- Die Baseline wird nur für die lokalen Emulator-Schlüssel umsigniert. Die Veröffentlichungsprüfung verwendet die ursprünglichen Release-Signaturen.

Ein echter Play-Store-Installationstest ist erst nach Bereitstellung eines Play-Testtracks möglich. Die Unterscheidung der Installationsquellen wird bis dahin durch die Vertragstests geprüft.

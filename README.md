# MeiLeseAbenteuer

Eine native Android-App für einen spielerischen Einstieg ins deutsche Lesen. Milo, ein kleiner Fuchs, begleitet kurze Runden mit sechs Aufgaben. Erste Repo-Fassung **0.1.1** (Update der vorab bereitgestellten 0.1.0); noch kein auf einem Kindergerät erprobtes Lernprogramm.

## Spielarten

- **Laute lauschen:** einen aufgenommenen Laut hören und den passenden Buchstaben antippen.
- **Bildersuche:** zum gezeigten und gehörten Anfangslaut das richtige Bild finden. Bildnamen sind dabei nicht als Text sichtbar.
- **Laute verbinden:** zwei Laute einzeln anhören, mit einem Schieberegler verbinden und die passende Silbe auswählen.
- **Wörter entdecken:** ein Wort selbst lesen und dem Bild zuordnen. Erst die Hilfefunktion spricht das gesuchte Wort vor.

Beginn mit **M, A, O**, danach **L, I, N, U, F**. Es kommen nur Silben und Wörter mit bereits eingeführten Buchstaben vor. Erste Wörter sind Mama und Oma, später Lama, Milo, Limo und Ufo. Keine geratenen Fantasiewörter und keine Sätze mit noch unbekannten Buchstaben.

Es gibt keine Uhr, Leben, tägliche Serien, Werbung, Käufe oder Konten. Für jede vollständige Runde gibt es einen Abenteuerstern. Neue Buchstaben folgen nach jeweils drei Runden mit mindestens vier ohne Fehler oder Hilfestellung gelösten Aufgaben. Aufgaben mit Hilfebedarf werden höher gewichtet. Das ist eine einfache Einstiegsregel und ausdrücklich keine Lerndiagnostik.

## Einmalige Einrichtung

1. App installieren und zunächst gemeinsam öffnen.
2. Unter **Für Große** die Rechenfrage beantworten.
3. Ein Kinderprofil auswählen und optional einen Spitznamen vergeben. Drei Profile haben getrennte Fortschritte.
4. **Sprachausgabe testen**. Falls nötig über die Android-Spracheinstellungen eine deutsche Offline-Stimme herunterladen. Danach zurückkehren.
5. Gemeinsam die Lautaufnahmen unter **Meine Buchstaben entdecken** anhören. Insbesondere die bearbeiteten Konsonanten vor einem eigenständigen Einsatz mit dem Kind auf Verständlichkeit prüfen.

Die acht Lautdateien liegen fest in der APK. Gesprochene Anleitungen, ganze Wörter und Silben verwenden nur eine installierte deutsche TTS-Stimme, die keine Netzwerkverbindung voraussetzt. Ohne passende Stimme bleiben die Bilder und aufgenommenen Laute benutzbar; selbstständige gesprochene Anleitung ist dann noch nicht vollständig verfügbar. Bei stummgeschaltetem Ton sind die Hörspiele gesperrt.

## Elternbereich und Updates

Profile, Namen, Fortschritt, Ton, Sprachtest und das Zurücksetzen eines einzelnen Profils befinden sich im Elternbereich. Die Rechenfrage verhindert versehentliche Zugriffe; sie ist keine Authentifizierung.

Der vorbereitete Updater verwendet die tatsächliche Installationsquelle: Google Play öffnet Google Play, direkt installierte APKs verwenden GitHub. Paketname, Version, Signatur, Größe und SHA-256 werden geprüft. Wie in den anderen MeiApps ist die tägliche Prüfung standardmäßig aktiv und abschaltbar. Manuelle und tägliche Prüfungen werden nur im geöffneten Elternbereich aktiv. Lernspiele brauchen keine Verbindung. Kamera, Mikrofon und Standort werden nicht angefragt.

Repository: https://github.com/pehab/MeiLeseAbenteuer. Der Feed liegt unter `releases/latest/download/MeiLeseAbenteuer-update.json` und wird mit jeder verifizierten Veröffentlichung zusammen mit APK und Prüfsumme aktualisiert.

## Bauen und prüfen

```sh
bash test.sh
bash scripts/test-update-contract.sh
ANDROID_SDK_ROOT=/path/to/android-sdk bash build-local.sh
```

Benötigt werden Java 17, Android-Plattform 37.0 und Build Tools 36.0.0. Der direkte Build benötigt keine Laufzeitbibliotheken. Die Gradle-Konfiguration ist ebenfalls enthalten. Die Signierung wird bei der ersten lokalen Erstellung in `.signing/` erzeugt; diesen privaten Ordner niemals committen oder öffentlich teilen. Für updatesichere Folgereleases muss derselbe Schlüssel weiterverwendet werden.

- Die pure Java-Suite prüft unter anderem zulässige Wörter, Distraktoren, Wiederholungen, Fehlversuche und doppelte Antworten.
- Die Update-Vertragssuite prüft Quellenauswahl, Metadaten und manipulierte APK-Inhalte.
- Der native Build und die enthaltenen, für Android direkt lesbaren Audiodateien wurden geprüft.
- Die CI führt Build, Gradle/Lint und echte Interaktionstests auf Android 11 und 17 aus: alle vier Spiele, Fehlversuche und Hilfestellungen, getrennte Profile, Belohnungen und Wiederherstellung nach Prozessneustart.
- Der Update-Gerätetest legt Daten unter der vorherigen 0.1.0-Version an, installiert 0.1.1 darüber und prüft den erhaltenen Namen, Fortschritt und Lernverlauf.
- Screenshots aus den Gerätetests werden vor dem Merge visuell geprüft. Release-Paket und Metadaten werden anschließend erneut geprüft; die Veröffentlichung erfolgt nur nach erfolgreicher CI.
- **Gemeinsam noch zu erproben:** Verständlichkeit der Lautaufnahmen, Qualität der installierten deutschen Stimme und der passende Schwierigkeitsgrad für das Kind. Ein grüner Emulator-Test ersetzt diesen Hör- und Praxistest nicht.

Details zum Update-Verhalten, zur Signierung und zu den Prüfgrenzen stehen in [docs/UPDATES.md](docs/UPDATES.md).

## Inhalte und Quellen

Alle Illustrationen sind im Projekt gezeichnet und benötigen keine Bilddownloads. Die menschlichen Lautaufnahmen stammen von Wikimedia Commons; Lizenz **CC BY-SA 3.0**. Der Anhang `app/src/main/assets/AUDIO-CREDITS.txt` enthält Urheber, Originalquellen und Bearbeitungen. Er ist im Elternbereich der App lesbar. Vier Konsonanten wurden aus Referenzaufnahmen isoliert, mit Überblendungen verlängert und normalisiert; die Originale und Bearbeitungsschritte sind unter `audio-originals/` und `docs/` dokumentiert. Für einen späteren größeren Lernumfang sollten einheitlich eingesprochene deutsche Lernaufnahmen folgen.

Pädagogische Orientierung für die Lautspiele: LISUM, *Auf die Plätze! Fertig! Los!* (2021), Abschnitt „Wörter lautieren an der Bushaltestelle“: https://bildungsserver.berlin-brandenburg.de/fileadmin/bbb/schule/grundschulportal/Schriftspracherwerb/Lese-Schreibideen_zu_Hause.final.pdf

Das Projekt ist ein spielerisches Ergänzungsangebot. Inhalt und Lernfolge sollten nach den ersten gemeinsamen Testrunden weiter abgestimmt werden.

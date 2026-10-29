# MeiLeseAbenteuer · Der Geschichtenwald

**Neustart als Spiel.** Aus dem vorherigen Entwurf wurden ausschließlich die vorhandenen Wald-, Tier- und Gegenstandsgrafiken übernommen. Geschichte, Navigation, Begegnungen und Rätsel sind neu. Der erste spielbare Abschnitt heißt **Die erste Sternenpost** (0.4.0, Prototyp).

Die Android-App öffnet direkt den illustrierten Wald. Ein Fuchs trägt die Sternenpost von Nila zum Baumhaus. Der Weg führt zum Frosch Taro am Bach und weiter zur Empfängerin. Die Figur läuft, die Kamera folgt ihr, Licht und Glühwürmchen bewegen sich. Das Kind kann die Landschaft mit Wischen ansehen, auf dem Weg laufen und drei versteckte Glühwürmchen entdecken. Gelöste Rätsel schalten die Überfahrt frei und lassen am Baumhaus Licht aufgehen.

## Der erste Abschnitt

1. **Nila begegnen:** den Auftrag für die Sternenpost annehmen.
2. **Taros Wunschzettel:** das Wort **LIMO** lesen und das passende Gegenstandsbild wählen. Falsche Antworten lassen das Rätsel offen. „Hilf mir“ spricht die Lösung vor; die normale Wiederholung erklärt nur den Auftrag.
3. **Der Brief am Baumhaus:** den Namen **OMA** aus Buchstaben zusammensetzen, durch Antippen oder Ziehen. Buchstaben können zurückgelegt werden. Nur die richtige Reihenfolge stellt die Post zu.
4. **Ankunft:** die Sternenpost kommt an, im Baumhaus wird es hell. Das Kind kann weiter die Landschaft erkunden oder den Abschnitt neu spielen.

Der kurze Abschnitt erprobt **Spielgefühl, Atmosphäre und Bedienung für Kinder mit ersten Wortkenntnissen**. Er ist noch kein vollständiger Lesekurs: Es fehlen insbesondere eine geführte Einführung der Buchstaben, ein systematisches Lautieren und Zusammenschleifen, eine größere Lernfolge und mehr Kapitel. Einzelne Buchstaben werden nicht mit einer allgemeinen TTS-Stimme als vermeintliche Laute vorgesprochen. Die vorhandenen Tierbilder bewegen sich als ganze Figuren; echte Lauf- und Gesichtsanimationsphasen sind noch nicht enthalten.

## Android und Ton

Die gesamte Spieloberfläche und alle Bilder liegen in der APK. Eine Android-WebView stellt die lokale Szene dar; sie lädt ausschließlich über einen abgeriegelten internen HTTPS-Ursprung aus den eingebauten Assets. Datei- und Inhaltszugriffe sowie externe Navigation sind gesperrt. Zum Spielen ist keine Netzwerkverbindung nötig.

Deutsche Dialoge, Anleitungen und Hilfen verwenden eine installierte **deutsche Offline-Stimme**. Ohne passende Stimme bitte zunächst gemeinsam spielen. Über das Schloss oben rechts und die Erwachsenenfrage findet man Informationen, den Sprachtest bzw. die Einrichtung, Updates und den Neustart dieses Abschnitts. Glockenklänge werden lokal erzeugt. Der Ton ist abschaltbar und die Einstellung wird gespeichert.

Der neue Kapitelstand wird getrennt als `forest-story-v1` gespeichert. Abgeschlossene Begegnungen, benutzte Hilfen und entdeckte Glühwürmchen bleiben erhalten. Alte Lernstände werden weder umgerechnet noch gelöscht. Dieser erste Entwurf verwendet einen gemeinsamen Kapitelstand; mehrere Kinderprofile sind hier noch nicht umgesetzt.

Paketname, Signierschlüssel und die Update-Infrastruktur bleiben kompatibel. Der öffentliche Update-Feed wird durch einen Entwicklungsbranch nicht umgestellt. Ein Release wird erst nach erfolgreicher Prüfung veröffentlicht. Details der bestehenden Update-Sicherheitsprüfung: [docs/UPDATES.md](docs/UPDATES.md).

## Bauen und prüfen

```sh
bash test.sh
bash scripts/test-update-contract.sh
python3 scripts/check-release-metadata.py
ANDROID_SDK_ROOT=/path/to/android-sdk bash build-local.sh
ANDROID_SDK_ROOT=/path/to/android-sdk bash test-android-build.sh
```

Der direkte Build verwendet Java 17, Android-Plattform 37.0 und Build Tools 36.0.0. Node.js ab Version 18 wird für die Kapiteltests benötigt. Der private Signierordner `.signing/` darf niemals veröffentlicht werden. Updates benötigen denselben Schlüssel wie bisher.

Zusätzliche Prüfungen:

- `tests/story.test.cjs`: Kapitelreihenfolge, falsche und doppelte Antworten, Hilfen, beschädigte Speicherstände und Wiederherstellung nach gelösten Rätseln.
- `tests/browser.cjs`: echte Chromium-Interaktion mit dem vollständigen Kapitel, Buchstaben per Drag-and-drop, Hilfen, Neustart, allen Glühwürmchen und drei Bildschirmformaten. Benötigt Playwright 1.51.1 und Chromium. Optional `PLAYWRIGHT_MODULE=/absolute/path/to/playwright node tests/browser.cjs`.
- `tests/SmokeInstrumentation.java`: reale Android-WebView, gebündelte Assets ohne Verbindung, Kapitelabschluss, Neustart und Erhalt alter Daten bei APK-Update. Ausführbar mit `scripts/run-emulator.sh`.
- CI: Kapiteltests, Browserprüfungen, nativer APK-Build, Gradle/Lint und Android-Geräteprüfungen auf API 30 und 37.0. Screenshots werden als Prüfarbeitsdateien bereitgestellt.

Vor einer Freigabe sind die Screenshots visuell sowie Aussprache, Schriftgröße, Berührung und Tempo auf dem Kindergerät zu prüfen. Ein erfolgreicher Build allein belegt diese Qualität nicht.

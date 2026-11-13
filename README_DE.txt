ShadowAI 1.4.0 – AndroidIDE-Projekt

VERBESSERT: Gemini wertet gespeicherte persönliche Notizen bei passenden Fragen ausdrücklich aus und fasst bei Fragen über den Nutzer alle relevanten Angaben zusammen.

NEU: Lokale GGUF-Inferenz direkt in ShadowAI
- llama.cpp wird als Android-Bibliothek eingebunden (kein lokaler HTTP-Server).
- GGUF-Dateien werden über „GGUF-Modell importieren“ ausgewählt und in den privaten App-Speicher kopiert.
- In „KI & Verbindung“ den Anbieter „On-Device (GGUF, ohne Termux)“ auswählen.
- Gemini und eigene OpenAI-kompatible APIs bleiben als alternative Anbieter vorhanden.

Voraussetzungen zum Bauen
- Android Gradle Plugin 8.6.1
- Gradle 8.9
- JDK 17
- Android SDK Platform 34
- Internetverbindung beim ersten Gradle-Build, damit Kotlin und llama-android aus Maven Central geladen werden.
- Ziel-ABI dieser Version: arm64-v8a.

Build in AndroidIDE-Terminal
  export JAVA_HOME="/opt/java/jdk-17.0.20.1+1"
  export PATH="$JAVA_HOME/bin:$PATH"
  ~/gradle-8.9/bin/gradle :app:assembleDebug

APK
  app/build/outputs/apk/debug/app-debug.apk

Modell
- Die GGUF-Datei ist nicht in der APK enthalten. Sie wird vom Nutzer separat ausgewählt.
- Auf einem Gerät mit 4 GB RAM nur kleine quantisierte Modelle verwenden. Der Speicherbedarf umfasst Modellgewichte plus Kontext und Laufzeit-Overhead.
- Das Modell wird in den App-spezifischen Speicher kopiert. Bei Deinstallation der App wird diese Kopie gelöscht.

Hinweise
- Termux ist für Chat und lokale GGUF-Inferenz nicht erforderlich.
- Die bisherigen optionalen Terminal-/Python-/SQLmap-Menüpunkte sind keine integrierten On-Device-Werkzeuge und werden nicht ausgeführt.
- Diese Quelländerung wurde hier nicht mit Android SDK/NDK gebaut oder auf einem Gerät getestet. Der erste Gradle-Build muss die Abhängigkeiten herunterladen; mögliche Kompilierungsfehler müssen anhand der AndroidIDE-Ausgabe korrigiert werden.

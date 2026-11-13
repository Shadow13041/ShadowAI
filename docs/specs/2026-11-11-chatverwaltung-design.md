# ShadowAI 1.7 – Chatverwaltung

## Ziel
Erweiterung der bestehenden Chatverwaltung ohne Verlust vorhandener Daten.

## Datenbank
- DB_VERSION auf 2 erhöhen.
- Tabelle chats um pinned INTEGER NOT NULL DEFAULT 0 erweitern.
- Migration mit ALTER TABLE.
- Bestehende Chats und Nachrichten bleiben erhalten.

## Chatfunktionen
- Chats umbenennen.
- Chats anheften und lösen.
- Angeheftete Chats zuerst anzeigen.
- Chats löschen.

## Nachrichtenfunktionen
- Nachrichten kopieren.
- Eigene Nachrichten bearbeiten.
- Änderungen dauerhaft in SQLite speichern.
- Nachrichten anderer Rollen nicht bearbeitbar machen.

## Suche
- Chatnamen und Nachrichteninhalte durchsuchen.
- Ergebnisse dem jeweiligen Chat zuordnen.
- Suche ohne externe Dienste.

## Darstellung
- AnswerFormatter für KI-Antworten verwenden.
- Markdown-Formatierung korrekt darstellen.
- Bestehende Chatoberfläche möglichst beibehalten.

## Technische Anforderungen
- Keine Abhängigkeit von anderen Android-Apps.
- Bestehende Android- und Gradle-Konfiguration berücksichtigen.
- Datenbankmigration rückwärtsverträglich gestalten.
- Änderungen schrittweise testen.

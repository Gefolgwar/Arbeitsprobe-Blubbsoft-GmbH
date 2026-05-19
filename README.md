# Fragebogen-Webanwendung — Arbeitsprobe Blubbsoft GmbH

Eine Java-Webanwendung auf Basis von Spring Boot, die Fragebögen aus JSON-Dateien lädt und Frage für Frage anzeigt. Die Anwendung unterstützt bedingte Logik (Fragen überspringen je nach vorherigen Antworten), Vor- und Zurück-Navigation sowie eine Ergebnisübersicht mit JSON-Export und -Import.

---

## Voraussetzungen

- **Java 17** oder höher
- Keine separate Maven-Installation nötig — der **Maven Wrapper** (`mvnw` / `mvnw.cmd`) ist im Projekt enthalten

---

## Starten

### Linux / macOS

```bash
./mvnw spring-boot:run
```

### Windows (PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

### Windows (CMD)

```cmd
mvnw.cmd spring-boot:run
```

Die Anwendung läuft anschließend unter **http://localhost:8081**.

### Projekt bauen

```bash
./mvnw clean package
```

Die erzeugte JAR-Datei liegt danach unter `target/`.

### Tests ausführen

```bash
./mvnw test
```

---

## Fragebogen wechseln

Der aktive Fragebogen wird in `src/main/resources/application.properties` konfiguriert:

```properties
survey.questionnaire-file=questionnaires/kinder-und-stadtteil.json
```

Zwei Fragebögen sind enthalten:

| Datei | Thema |
|---|---|
| `kinder-und-stadtteil.json` | Kinder, Einrichtungen, Schulessen (Matrix), Stadtteil |
| `verkehr.json` | Verkehrsmittel, Ticketart, ÖPNV-Verspätungen, Firmenwagen |

Einfach den Wert von `survey.questionnaire-file` ändern und die Anwendung neu starten.

---

## JSON-Datenstruktur

Fragebogen-Dateien liegen im Verzeichnis `src/main/resources/questionnaires/`. Jede Datei hat folgende Struktur:

```json
{
  "title": "String — Titel des Fragebogens",
  "questions": [
    {
      "id": "String — Eindeutige ID (z.B. q1, v1)",
      "type": "SINGLE_CHOICE | MULTIPLE_CHOICE | MATRIX",
      "text": "String — Fragetext",
      "options": ["String — Antwortoptionen (bei SINGLE_CHOICE / MULTIPLE_CHOICE)"],
      "rows": ["String — Zeilen (nur bei MATRIX, z.B. Preis, Qualität)"],
      "columns": ["String — Spalten (nur bei MATRIX, z.B. Sehr zufrieden, Zufrieden)"],
      "condition": {
        "questionId": "String — ID der referenzierten Frage",
        "operator": "EQUALS | NOT_EQUALS | CONTAINS | ANY_OF",
        "value": "String — Vergleichswert (bei EQUALS, NOT_EQUALS, CONTAINS)",
        "values": ["String — Liste von Werten (nur bei ANY_OF)"]
      }
    }
  ]
}
```

### Fragetypen

| Typ | Darstellung | Verhalten |
|---|---|---|
| `SINGLE_CHOICE` | Radio Buttons | Genau eine Antwort auswählbar |
| `MULTIPLE_CHOICE` | Checkboxes | Mehrere Antworten auswählbar |
| `MATRIX` | Tabelle mit Radio Buttons pro Zeile | Jede Zeile (`rows`) wird gegen alle Spalten (`columns`) bewertet; `options` wird nicht verwendet |

### Bedingungs-Operatoren

Über das `condition`-Objekt lässt sich steuern, wann eine Frage angezeigt wird.

| Operator | Bedeutung | Beispiel |
|---|---|---|
| `EQUALS` | Antwort ist exakt gleich dem Wert | `"operator": "EQUALS", "value": "Ja"` — Frage erscheint nur, wenn die referenzierte Frage mit „Ja“ beantwortet wurde |
| `NOT_EQUALS` | Antwort ist ungleich dem Wert | `"operator": "NOT_EQUALS", "value": "Nein"` — Frage erscheint, solange die Antwort nicht „Nein“ ist |
| `CONTAINS` | Antwortliste enthält den Wert (für `MULTIPLE_CHOICE`) | `"operator": "CONTAINS", "value": "Bus"` — Frage erscheint, wenn „Bus“ unter den gewählten Antworten ist |
| `ANY_OF` | Antwortliste enthält mindestens einen der Werte (für `MULTIPLE_CHOICE`) | `"operator": "ANY_OF", "values": ["Bus", "Bahn"]` — Frage erscheint, wenn „Bus“ oder „Bahn“ (oder beide) gewählt wurden |

**Sonderregeln:**

- `condition` ist `null` oder fehlt → Die Frage wird **immer** angezeigt.
- Die referenzierte Frage wurde noch nicht beantwortet → Bedingung gilt als **nicht erfüllt** → Frage wird übersprungen.
- Die Antwortliste ist leer → Bedingung gilt als **nicht erfüllt** → Frage wird übersprungen.

---

## Eigenen Fragebogen erstellen

Eine neue JSON-Datei im Verzeichnis `src/main/resources/questionnaires/` anlegen. Hier ein vollständiges Minimalbeispiel mit drei Fragetypen und einer Bedingung:

```json
{
  "title": "Kundenzufriedenheit",
  "questions": [
    {
      "id": "k1",
      "type": "SINGLE_CHOICE",
      "text": "Wie oft nutzen Sie unseren Service?",
      "options": ["Täglich", "Wöchentlich", "Monatlich", "Selten"]
    },
    {
      "id": "k2",
      "type": "MULTIPLE_CHOICE",
      "text": "Welche Bereiche nutzen Sie?",
      "options": ["Beratung", "Support", "Schulung", "Online-Portal"]
    },
    {
      "id": "k3",
      "type": "MATRIX",
      "text": "Wie zufrieden sind Sie mit den folgenden Aspekten?",
      "rows": ["Freundlichkeit", "Kompetenz", "Erreichbarkeit"],
      "columns": ["Sehr zufrieden", "Zufrieden", "Neutral", "Unzufrieden"],
      "condition": {
        "questionId": "k1",
        "operator": "NOT_EQUALS",
        "value": "Selten"
      }
    }
  ]
}
```

In diesem Beispiel wird Frage `k3` nur angezeigt, wenn bei `k1` **nicht** „Selten“ gewählt wurde.

Anschließend in `application.properties` den Dateinamen eintragen:

```properties
survey.questionnaire-file=questionnaires/kundenzufriedenheit.json
```

---

## Export / Import

### Ergebnisse exportieren

Auf der Zusammenfassungsseite den Button **„JSON speichern“** klicken. Die Antworten werden als JSON-Datei heruntergeladen. Der Dateiname folgt dem Muster `ergebnisse-{titel}-{zeitstempel}.json`. Das Exportformat sieht beispielsweise so aus:

```json
{
  "questionnaire": "Kinder und Stadtteil",
  "exportedAt": "2025-07-15T14:30:00",
  "results": [
    {
      "questionId": "q1",
      "questionText": "Haben Sie Kinder?",
      "questionType": "SINGLE_CHOICE",
      "answers": ["Ja"]
    },
    {
      "questionId": "q4",
      "questionText": "Wie zufrieden sind Sie mit dem Schulessen?",
      "questionType": "MATRIX",
      "answers": ["Preis: Sehr zufrieden", "Qualität: Zufrieden", "Auswahl: Neutral", "Portionsgröße: Zufrieden"]
    }
  ]
}
```

Der Fragetext (`questionText`) wird mitgespeichert, damit die exportierte Datei auch ohne die Originaldaten verständlich bleibt.

### Ergebnisse importieren

Auf der Zusammenfassungsseite den Button **„JSON öffnen“** klicken und eine zuvor exportierte JSON-Datei auswählen. Die importierten Ergebnisse werden **schreibgeschützt** angezeigt — die aktuelle Sitzung wird dabei **nicht** verändert.

---

## Projektstruktur

```
src/main/java/com/blubbsoft/survey/
├── SurveyApplication.java          — Spring Boot Einstiegspunkt
├── model/                           — Domänenobjekte
│   ├── Questionnaire.java           — Fragebogen (Titel + Fragenliste)
│   ├── Question.java                — Einzelne Frage
│   ├── QuestionType.java            — Enum: SINGLE_CHOICE, MULTIPLE_CHOICE, MATRIX
│   ├── Condition.java               — Anzeigebedingung
│   ├── ConditionOperator.java       — Enum: EQUALS, NOT_EQUALS, CONTAINS, ANY_OF
│   ├── SurveyResult.java            — Export/Import DTO
│   └── ResultEntry.java             — Einzelner Ergebniseintrag
├── session/                         — Sitzungsverwaltung
│   └── SurveySession.java           — Session-Scoped Bean (Antworten + Verlauf)
├── service/                         — Geschäftslogik
│   ├── QuestionnaireService.java    — Laden, Navigation, Bedingungen
│   ├── ConditionEvaluator.java      — Bedingungs-Engine (M1)
│   └── ExportImportService.java     — JSON Export/Import (M5)
└── controller/                      — Web-Schicht
    └── SurveyController.java        — HTTP-Endpunkte

src/main/resources/
├── application.properties           — Konfiguration (u.a. aktiver Fragebogen)
├── questionnaires/                  — JSON-Fragebogendateien
│   ├── kinder-und-stadtteil.json
│   └── verkehr.json
├── templates/                       — Thymeleaf-Templates
│   ├── survey.html                  — Frageansicht
│   └── summary.html                 — Zusammenfassung / Export / Import
└── static/css/
    └── style.css                    — Stylesheet
```

---

## Lizenz

Erstellt als Arbeitsprobe für die Blubbsoft GmbH.

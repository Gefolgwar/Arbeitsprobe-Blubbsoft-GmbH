# PRD: Dynamische Fragebogen-Webanwendung

## 1. Zusammenfassung

Es soll eine **Java-Webanwendung** entwickelt werden, die beliebige Fragebögen aus einer menschenlesbaren Datendatei (JSON) lädt und sie dem Benutzer **Frage für Frage** anzeigt. Die Anwendung unterstützt **bedingte Logik** (Fragen werden übersprungen, wenn sie aufgrund vorheriger Antworten keinen Sinn ergeben), eine **Zurück-Funktion** und zeigt am Ende eine **Zusammenfassung** aller beantworteten Fragen.

---

## 2. Technologie-Stack

| Komponente | Technologie | Begründung |
|---|---|---|
| Backend | **Spring Boot 3.x** (Java 17+) | Einfache Konfiguration, eingebauter Tomcat, ein JAR zum Starten |
| Templating | **Thymeleaf** | Server-Side Rendering, kein separates Frontend-Build nötig |
| Datenformat | **JSON** | Menschenlesbar, von Jackson nativ unterstützt |
| Build-Tool | **Maven** (mit Maven Wrapper `mvnw`) | Standardisiertes Build, keine lokale Maven-Installation nötig |
| Styling | **Einfaches CSS** (classless/minimal) | Saubere Darstellung ohne Framework-Overhead |

---

## 3. Datenstruktur (JSON-Format)

### 3.1 Fragebogen-Datei

Die Fragebogen-Dateien liegen unter `src/main/resources/questionnaires/`. Beim Start der Anwendung wird eine Datei geladen (konfigurierbar über `application.properties`).

### 3.2 Aufbau der JSON-Struktur

```json
{
  "title": "Fragebogen-Titel",
  "questions": [
    {
      "id": "q1",
      "type": "SINGLE_CHOICE | MULTIPLE_CHOICE | MATRIX",
      "text": "Fragetext",
      "options": ["Option A", "Option B"],
      "condition": {
        "questionId": "q_prev",
        "operator": "EQUALS | NOT_EQUALS | CONTAINS | ANY_OF",
        "value": "erwarteter Wert",
        "values": ["Wert1", "Wert2"]
      },
      "rows": ["Zeile1", "Zeile2"],
      "columns": ["Spalte1", "Spalte2"]
    }
  ]
}
```

### 3.3 Feld-Beschreibung

| Feld | Pflicht | Beschreibung |
|---|---|---|
| `id` | Ja | Eindeutige Kennung der Frage (z.B. `"q1"`, `"v3"`) |
| `type` | Ja | Fragetyp: `SINGLE_CHOICE`, `MULTIPLE_CHOICE` oder `MATRIX` |
| `text` | Ja | Der Fragetext, der dem Benutzer angezeigt wird |
| `options` | Ja* | Liste der Antwortmöglichkeiten (*nicht bei MATRIX) |
| `condition` | Nein | Optionale Bedingung, wann die Frage angezeigt wird |
| `rows` | Nur MATRIX | Zeilenbeschriftungen der Matrix (z.B. `["Preis", "Qualität"]`) |
| `columns` | Nur MATRIX | Spaltenbeschriftungen der Matrix (z.B. `["Sehr zufrieden", "Unzufrieden"]`) |

### 3.4 Fragetypen

| Typ | Beschreibung | HTML-Darstellung |
|---|---|---|
| `SINGLE_CHOICE` | Genau eine Antwort möglich | Radio-Buttons |
| `MULTIPLE_CHOICE` | Mehrere Antworten möglich | Checkboxen |
| `MATRIX` | Bewertungsmatrix mit Zeilen und Spalten (z.B. Zufriedenheit) | Tabelle mit Radio-Buttons pro Zeile |

### 3.5 Bedingte Logik (Condition)

Das `condition`-Objekt ist **optional**. Wenn vorhanden, wird die Frage **nur angezeigt**, wenn die Bedingung erfüllt ist.

| Feld | Beschreibung |
|---|---|
| `questionId` | ID der Frage, von der die Bedingung abhängt |
| `operator` | Vergleichsoperator (siehe unten) |
| `value` | Erwarteter Wert (für `EQUALS`, `NOT_EQUALS`, `CONTAINS`) |
| `values` | Liste erwarteter Werte (nur für `ANY_OF`) |

**Operatoren:**

| Operator | Beschreibung | Beispiel |
|---|---|---|
| `EQUALS` | Antwort ist exakt gleich dem Wert | q1 = "Ja" |
| `NOT_EQUALS` | Antwort ist nicht gleich dem Wert | q1 != "Nein" |
| `CONTAINS` | Antwortliste enthält den Wert (für MULTIPLE_CHOICE) | q3 enthält "Schule" |
| `ANY_OF` | Antwortliste enthält mindestens einen der Werte | v1 enthält "Bus" ODER "U-Bahn" ODER "Regionalzug" |

**Beispiel — einfache Bedingung:**

```json
{
  "id": "q2",
  "type": "SINGLE_CHOICE",
  "text": "Wie viele Kinder haben Sie?",
  "options": ["1", "2", "3", "4", "5", "mehr"],
  "condition": {
    "questionId": "q1",
    "operator": "EQUALS",
    "value": "Ja"
  }
}
```

**Beispiel — ANY_OF-Bedingung:**

```json
{
  "id": "v2",
  "type": "SINGLE_CHOICE",
  "text": "Welche Fahrscheinart benutzen Sie hauptsächlich?",
  "options": ["Deutschlandticket", "4-Fahrten-Karte", "Einzelkarte", "fahre schwarz", "keine Angabe"],
  "condition": {
    "questionId": "v1",
    "operator": "ANY_OF",
    "values": ["Bus", "U-Bahn", "Regionalzug"]
  }
}
```

---

## 4. Architektur und Komponenten

### 4.1 Domänenmodell (POJOs)

- **`Questionnaire`** — Wrapper mit `title` und `List<Question>`
- **`Question`** — Enthält `id`, `type` (Enum), `text`, `options`, optionales `condition`, sowie optionale `rows`/`columns` für MATRIX
- **`QuestionType`** — Enum: `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `MATRIX`
- **`Condition`** — Enthält `questionId`, `operator` (Enum), `value`, `values`
- **`ConditionOperator`** — Enum: `EQUALS`, `NOT_EQUALS`, `CONTAINS`, `ANY_OF`
- **`SurveySession`** — Session-Scoped Bean, hält:
  - `LinkedHashMap<String, List<String>> answers` — Antworten (Key = questionId)
  - `Deque<String> history` — Stack der tatsächlich angezeigten Frage-IDs (für Zurück-Navigation)

### 4.2 Service-Schicht

**`QuestionnaireService`**
- Lädt die JSON-Datei beim Start (`@PostConstruct`) mittels Jackson `ObjectMapper`
- `Questionnaire getQuestionnaire()` — Gibt den geladenen Fragebogen zurück
- `Question getNextQuestion(Map<String, List<String>> answers, Deque<String> history)` — Iteriert durch alle Fragen, prüft Bedingungen, findet die nächste unbeantwortete, relevante Frage
- `boolean isConditionMet(Condition condition, Map<String, List<String>> answers)` — Zentrale Methode zur Auswertung der bedingten Logik

### 4.3 Controller

**`SurveyController`**

| Endpunkt | HTTP | Beschreibung |
|---|---|---|
| `GET /` | GET | Startseite: Redirect auf `/survey` |
| `GET /survey` | GET | Ermittelt die nächste relevante Frage und zeigt sie an. Wenn keine Fragen mehr → Redirect auf `/summary` |
| `POST /survey/answer` | POST | Nimmt Antwort entgegen, speichert in Session, pusht Frage-ID auf History-Stack, Redirect auf `GET /survey` |
| `POST /survey/back` | POST | Poppt letzte Frage-ID vom History-Stack, entfernt deren Antwort, Redirect auf `GET /survey` |
| `GET /summary` | GET | Zeigt Zusammenfassung aller beantworteten Fragen mit Antworten |
| `POST /survey/restart` | POST | Setzt Session zurück, startet Fragebogen von vorn |

### 4.4 Thymeleaf-Templates

| Template | Beschreibung |
|---|---|
| `survey.html` | Dynamische Darstellung einer Frage. Rendert je nach `type`: Radio-Buttons (`SINGLE_CHOICE`), Checkboxen (`MULTIPLE_CHOICE`) oder Matrix-Tabelle (`MATRIX`). Enthält „Weiter"- und „Zurück"-Buttons. |
| `summary.html` | Zusammenfassungsseite mit allen beantworteten Fragen und Antworten. Enthält „Neu starten"-Button. |

---

## 5. Beispiel-Fragebögen (Eingabedateien)

### 5.1 `kinder-und-stadtteil.json`

| ID | Frage | Typ | Bedingung |
|---|---|---|---|
| `q1` | Haben Sie Kinder? | SINGLE_CHOICE | — |
| `q2` | Wie viele Kinder haben Sie? | SINGLE_CHOICE | q1 EQUALS "Ja" |
| `q3` | Welche Einrichtungen besuchen Ihre Kinder? | MULTIPLE_CHOICE | q1 EQUALS "Ja" |
| `q4` | Wie zufrieden sind Sie mit dem Schulessen? (Preis / Qualität) | MATRIX | q3 CONTAINS "Schule" |
| `q5` | In welchem Stadtteil wohnen Sie? | SINGLE_CHOICE | — |

### 5.2 `verkehr.json`

| ID | Frage | Typ | Bedingung |
|---|---|---|---|
| `v1` | Mit welchen Verkehrsmitteln fahren Sie zur Arbeit? | MULTIPLE_CHOICE | — |
| `v2` | Welche Fahrscheinart benutzen Sie hauptsächlich? | SINGLE_CHOICE | v1 ANY_OF ["Bus", "U-Bahn", "Regionalzug"] |
| `v3` | Wie häufig kommen Sie wegen des ÖPNV zu spät? | SINGLE_CHOICE | v1 ANY_OF ["Bus", "U-Bahn", "Regionalzug"] |
| `v4` | Nutzen Sie Ihr eigenes Auto oder einen Dienstwagen? | SINGLE_CHOICE | v1 CONTAINS "Auto" |

---

## 6. Benutzerfluss (User Flow)

```
[Start: http://localhost:8080]
        |
        v
[Frage anzeigen] <-------.
        |                 |
        v                 |
[Antwort wählen]          |
        |                 |
        v                 |
[Weiter klicken]          |
        |                 |
        v                 |
{Nächste relevante        |
 Frage vorhanden?}        |
   |           |          |
  Ja          Nein        |
   |           |          |
   v           v          |
[Nächste    [Zusammen-    |
 Frage]      fassung]     |
                          |
[Zurück klicken] ---------'
```

1. Benutzer öffnet `http://localhost:8080`
2. Die erste Frage des Fragebogens wird angezeigt
3. Benutzer wählt eine Antwort und klickt „Weiter"
4. Die nächste **sinnvolle** Frage wird angezeigt (übersprungene Fragen werden nicht gezeigt)
5. Benutzer kann jederzeit „Zurück" klicken → vorherige Frage wird erneut angezeigt, die alte Antwort ist vorausgewählt
6. Nach der letzten relevanten Frage wird die Zusammenfassung angezeigt
7. Benutzer kann den Fragebogen neu starten

---

## 7. Projektstruktur

```
Arbeitsprobe-Blubbsoft-GmbH/
├── mvnw, mvnw.cmd                          # Maven Wrapper
├── pom.xml                                 # Maven-Konfiguration
├── README.md                               # Startanleitung + Datenstruktur-Doku
├── src/
│   ├── main/
│   │   ├── java/com/blubbsoft/survey/
│   │   │   ├── SurveyApplication.java      # Spring Boot Hauptklasse
│   │   │   ├── model/
│   │   │   │   ├── Questionnaire.java
│   │   │   │   ├── Question.java
│   │   │   │   ├── QuestionType.java        # Enum
│   │   │   │   ├── Condition.java
│   │   │   │   └── ConditionOperator.java   # Enum
│   │   │   ├── session/
│   │   │   │   └── SurveySession.java       # Session-Scoped Bean
│   │   │   ├── service/
│   │   │   │   └── QuestionnaireService.java
│   │   │   └── controller/
│   │   │       └── SurveyController.java
│   │   ├── resources/
│   │   │   ├── application.properties
│   │   │   ├── questionnaires/
│   │   │   │   ├── kinder-und-stadtteil.json
│   │   │   │   └── verkehr.json
│   │   │   ├── templates/
│   │   │   │   ├── survey.html
│   │   │   │   └── summary.html
│   │   │   └── static/
│   │   │       └── css/style.css
│   └── test/
│       └── java/com/blubbsoft/survey/
│           └── service/
│               └── QuestionnaireServiceTest.java
```

---

## 8. Konfiguration (`application.properties`)

```properties
survey.questionnaire-file=questionnaires/kinder-und-stadtteil.json
server.port=8080
```

Der Dateiname kann geändert werden, um einen anderen Fragebogen zu laden. Nach dem Ändern muss die Anwendung neu gestartet werden.

---

## 9. Akzeptanzkriterien

- [ ] **AC-1:** Die Anwendung startet mit `./mvnw spring-boot:run` und ist unter `http://localhost:8080` erreichbar
- [ ] **AC-2:** Beim Start wird der in `application.properties` konfigurierte Fragebogen aus einer JSON-Datei geladen
- [ ] **AC-3:** Fragen werden einzeln (eine pro Seite) angezeigt
- [ ] **AC-4:** Bei `SINGLE_CHOICE` werden Radio-Buttons, bei `MULTIPLE_CHOICE` Checkboxen und bei `MATRIX` eine Tabelle mit Radio-Buttons gerendert
- [ ] **AC-5:** Fragen mit nicht erfüllter `condition` werden automatisch übersprungen
- [ ] **AC-6:** Die „Zurück"-Taste navigiert zur vorherigen tatsächlich angezeigten Frage; die vorherige Antwort ist vorausgewählt
- [ ] **AC-7:** Nach der letzten relevanten Frage wird eine Zusammenfassung aller beantworteten Fragen mit Antworten angezeigt
- [ ] **AC-8:** Der Fragebogen kann neu gestartet werden
- [ ] **AC-9:** Beide Beispiel-Fragebögen (Kinder/Stadtteil und Verkehr) funktionieren korrekt mit bedingter Logik
- [ ] **AC-10:** Die JSON-Datenstruktur ist im `README.md` dokumentiert, sodass ein neuer Fragebogen ohne Programmierkenntnisse erstellt werden kann
- [ ] **AC-11:** Das Projekt enthält den Maven Wrapper, sodass keine lokale Maven-Installation nötig ist

---

## 10. Liefergegenstände (ZIP-Datei)

1. Quellcode des Programms
2. Beide Fragebögen als JSON-Eingabedateien
3. Dokumentation der Datenstruktur (im `README.md`)
4. Maven Wrapper + `pom.xml` mit allen Abhängigkeiten
5. Startanleitung (im `README.md`)

---

## 11. Umsetzungsreihenfolge

| Schritt | Aufgabe | Geschätzte Zeit |
|---|---|---|
| 1 | Projekt-Setup: Spring Boot + Maven + Thymeleaf + Jackson | 15 min |
| 2 | JSON-Datenstruktur + Modellklassen (`Questionnaire`, `Question`, `Condition`) | 20 min |
| 3 | JSON-Eingabedateien für beide Fragebögen erstellen | 15 min |
| 4 | `QuestionnaireService`: JSON laden + Bedingungsauswertung | 30 min |
| 5 | `SurveySession`: Session-Scoped Bean mit Answers + History | 10 min |
| 6 | `SurveyController`: Alle Endpunkte | 25 min |
| 7 | `survey.html`: Template mit SINGLE_CHOICE, MULTIPLE_CHOICE, MATRIX | 30 min |
| 8 | `summary.html`: Zusammenfassungsseite | 15 min |
| 9 | CSS-Styling | 15 min |
| 10 | Unit-Tests für Bedingungslogik | 20 min |
| 11 | `README.md`: Startanleitung + JSON-Doku | 15 min |
| 12 | Finaler Test, Cleanup, ZIP-Paketierung | 15 min |
| | **Gesamt** | **~3,5 Stunden** |

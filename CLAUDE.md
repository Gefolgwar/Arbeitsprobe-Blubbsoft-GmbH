# Arbeitsprobe Blubbsoft GmbH — Fragebogen-Webanwendung

## Project Overview

Java web application that loads questionnaires from JSON files and displays them question-by-question with conditional logic, back-navigation, and a results summary. Built as a work sample (Arbeitsprobe) for Blubbsoft GmbH.

## Tech Stack

- **Java 17+**, **Spring Boot 3.x**, **Thymeleaf** (SSR), **Jackson** (JSON), **Maven** (with Wrapper)
- No frontend build tools, no database, no Docker — one JAR, one command to run

## Architecture — 5 Modules

| Module | Interface | Purpose |
|---|---|---|
| **M1 Condition Engine** | `boolean isConditionMet(Condition, Map<String, List<String>>)` | Evaluates EQUALS, NOT_EQUALS, CONTAINS, ANY_OF operators |
| **M2 Navigation Engine** | `getNextQuestion(session)`, `getQuestionById(id)` | Determines next/previous relevant question, skips conditions not met |
| **M3 Questionnaire Loader** | `Questionnaire loadFromResource(path)` | Parses JSON → domain objects via Jackson |
| **M4 Survey Session** | `addAnswer()`, `removeLastAnswer()`, `reset()` | Session-scoped bean: `LinkedHashMap<String, List<String>>` answers + `Deque<String>` history |
| **M5 Export/Import Service** | `exportToJson()`, `importFromJson()` | Serializes/deserializes survey results for JSON download/upload |

**Web Layer** (thin): `SurveyController` (8 endpoints) + `survey.html` + `summary.html`

## JSON Data Structure

Questionnaire files live in `src/main/resources/questionnaires/`. Active file configured via `application.properties` key `survey.questionnaire-file`.

### Question Types
- `SINGLE_CHOICE` → radio buttons
- `MULTIPLE_CHOICE` → checkboxes
- `MATRIX` → table with radio buttons per row (rows + columns fields)

### Condition Operators
- `EQUALS` — answer equals value
- `NOT_EQUALS` — answer does not equal value
- `CONTAINS` — answer list contains value (for MULTIPLE_CHOICE)
- `ANY_OF` — answer list contains at least one of values[] (for MULTIPLE_CHOICE)

### Condition rules
- `null` condition → always show the question
- Referenced question not answered → condition NOT met → skip
- Empty answer list → condition NOT met → skip

## Controller Endpoints

| Endpoint | Method | Purpose |
|---|---|---|
| `/` | GET | Redirect → `/survey` |
| `/survey` | GET | Show next relevant question (or redirect → `/summary`) |
| `/survey/answer` | POST | Save answer, push to history, redirect → `/survey` |
| `/survey/back` | POST | Pop history, remove answer, redirect → `/survey` |
| `/summary` | GET | Show all answered questions |
| `/summary/export` | GET | Download results as JSON file |
| `/summary/import` | POST | Upload JSON file, display imported results read-only |
| `/survey/restart` | POST | Clear session, redirect → `/survey` |

## Two Example Questionnaires

1. **kinder-und-stadtteil.json** — 5 questions (q1–q5): children, facilities, school meal satisfaction (MATRIX), district
2. **verkehr.json** — 4 questions (v1–v4): transport modes, ticket type, ÖPNV delays, own car vs company car

## Key Behaviors

- Questions displayed one per page
- Conditional questions skipped automatically when condition not met
- Back button returns to the previously *shown* question (not just previous in list)
- Back + change answer → subsequent dependent answers invalidated
- Summary shows only answered questions (skipped ones excluded)
- MATRIX answers displayed as "Row: Column" format (e.g., "Preis: Sehr zufrieden")
- Export includes questionText so imported results are self-contained
- Import is read-only — does NOT modify current session

## Testing Strategy

Unit tests for M1 (Condition Engine), M2 (Navigation Engine), M3 (Loader), M5 (Export/Import). Test external behavior, not implementation details. M4 (Session) is too simple to test.

## Project Structure

```
src/main/java/com/blubbsoft/survey/
├── SurveyApplication.java
├── model/          Questionnaire, Question, QuestionType, Condition, ConditionOperator
├── session/        SurveySession
├── service/        QuestionnaireService
└── controller/     SurveyController

src/main/resources/
├── application.properties
├── questionnaires/   kinder-und-stadtteil.json, verkehr.json
├── templates/        survey.html, summary.html
└── static/css/       style.css
```

## GitHub Issues

- **#1** — PRD (parent)
- **#2** — S1: Project Setup (Spring Boot skeleton)
- **#3** — S2: Domain Model + JSON Loader
- **#4** — S3: Condition Engine
- **#5** — S4: Navigation Engine + Session
- **#6** — S5: Survey UI (Weiter/Zurück)
- **#7** — S6: Summary + Restart
- **#8** — S7: JSON Export/Import
- **#9** — S8: Styling + README + Finalisierung

Dependency chain: #2 → #3 → #4 → #5 → #6 → #7 → #8 → #9

## Deliverables (ZIP to jobs@blubbsoft.de)

1. Source code
2. Both questionnaire JSON files
3. JSON data structure documentation (in README.md)
4. Maven Wrapper + pom.xml
5. Start instructions (in README.md)

## Commands

```bash
./mvnw spring-boot:run          # Start the application
./mvnw clean package            # Build JAR
./mvnw test                     # Run tests
# App runs at http://localhost:8080
```

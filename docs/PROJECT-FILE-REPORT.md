# Звіт по файлах проекту — Fragebogen-Webanwendung

> Детальне пояснення призначення кожного файлу в проекті Blubbsoft Survey.
> Дата створення: 2025-07-15

---

## Зміст

1. [Кореневі файли проекту](#1-кореневі-файли-проекту)
2. [Вхідна точка додатка](#2-вхідна-точка-додатка)
3. [Модель (Domain Objects)](#3-модель-domain-objects)
4. [Сервіси (Business Logic)](#4-сервіси-business-logic)
5. [Сесія (Session Management)](#5-сесія-session-management)
6. [Контролер (Web Layer)](#6-контролер-web-layer)
7. [Ресурси](#7-ресурси)
8. [Видалений мертвий код](#8-видалений-мертвий-код)

---

## 1. Кореневі файли проекту

### `pom.xml`

**Призначення:** Центральний конфігураційний файл Maven, який описує проект, його залежності та процес збірки.

**Навіщо потрібен:**
- Визначає Java 17 як мінімальну версію.
- Наслідує `spring-boot-starter-parent` (3.4.1), що забезпечує управління версіями всіх Spring-залежностей.
- Містить три runtime-залежності:
  - `spring-boot-starter-web` — вбудований Tomcat + Spring MVC для HTTP-обробки.
  - `spring-boot-starter-thymeleaf` — серверний рендеринг HTML-шаблонів.
  - `jackson-databind` — серіалізація/десеріалізація JSON (завантаження опитувальників, експорт/імпорт результатів).
- Містить `spring-boot-maven-plugin` для упаковки в виконуваний JAR (`./mvnw clean package`).

### `mvnw` / `mvnw.cmd`

**Призначення:** Maven Wrapper — скрипти для Linux/macOS (`mvnw`) і Windows (`mvnw.cmd`).

**Навіщо потрібен:** Дозволяє запускати Maven без його глобальної установки. Wrapper автоматично завантажує потрібну версію Maven. Це гарантує, що всі розробники та CI-середовища використовують однакову версію Maven.

### `.mvn/`

**Призначення:** Директорія конфігурації Maven Wrapper.

**Навіщо потрібна:** Містить `maven-wrapper.jar` і `maven-wrapper.properties` з URL для завантаження потрібної версії Maven. Є частиною Maven Wrapper механізму.

### `.gitignore`

**Призначення:** Визначає файли та директорії, які Git має ігнорувати.

**Навіщо потрібен:** Запобігає потраплянню в репозиторій:
- `target/` — артефакти збірки Maven.
- `.idea/`, `*.iml`, `.vscode/` — файли IDE (IntelliJ IDEA, VS Code).
- `.DS_Store`, `Thumbs.db` — системні файли macOS/Windows.
- `*.log` — лог-файли.
- `graphify-out/` — артефакти зовнішніх аналітичних інструментів.

### `README.md`

**Призначення:** Головна документація проекту для розробників та рецензентів.

**Навіщо потрібен:** Містить:
- Інструкції запуску для Linux, macOS і Windows.
- Документацію JSON-структури опитувальників (обов'язковий deliverable).
- Опис фрагетипів та операторів умов.
- Повний приклад створення власного опитувальника.
- Документацію формату експорту/імпорту.
- Огляд структури проекту.

### `CLAUDE.md`

**Призначення:** Інструкція для AI-асистента Claude з описом архітектури, модулів та конвенцій проекту.

**Навіщо потрібен:** Забезпечує контекст для AI-assisted розробки — описує 5 модулів (M1–M5), ендпоінти, правила умов, і ланцюг залежностей між GitHub Issues.

---

## 2. Вхідна точка додатка

### `src/main/java/com/blubbsoft/survey/SurveyApplication.java`

**Призначення:** Точка входу Spring Boot додатка.

**Навіщо потрібен:** Анотація `@SpringBootApplication` активує:
- **Auto-configuration** — автоматичне налаштування Tomcat, Thymeleaf, Jackson.
- **Component scanning** — автоматичне знаходження `@Controller`, `@Service`, `@Component` класів.
- **Configuration** — дозволяє зчитування `application.properties`.

Метод `main()` запускає вбудований сервер на порту, визначеному в `application.properties`.

---

## 3. Модель (Domain Objects)

### `src/main/java/com/blubbsoft/survey/model/Questionnaire.java`

**Призначення:** Кореневий доменний об'єкт — представляє весь опитувальник.

**Навіщо потрібен:** Java record з полями:
- `title` (String) — назва опитувальника (відображається в заголовку сторінки).
- `questions` (List\<Question\>) — впорядкований список питань.

`@JsonIgnoreProperties(ignoreUnknown = true)` забезпечує forward-compatibility — нові поля в JSON не зламають десеріалізацію.

### `src/main/java/com/blubbsoft/survey/model/Question.java`

**Призначення:** Представляє одне питання з усіма його параметрами.

**Навіщо потрібен:** Java record з полями:
- `id` — унікальний ідентифікатор (напр. `"q1"`, `"v1"`), використовується як ключ у Map відповідей.
- `type` — тип питання (визначає рендеринг у шаблоні).
- `text` — текст питання для відображення.
- `options` — варіанти відповідей (для SINGLE_CHOICE / MULTIPLE_CHOICE).
- `condition` — умова відображення (nullable — null = завжди показувати).
- `rows` / `columns` — рядки та стовпці для MATRIX-типу.

### `src/main/java/com/blubbsoft/survey/model/QuestionType.java`

**Призначення:** Enum трьох типів питань.

**Навіщо потрібен:** Визначає поведінку в трьох місцях:
- **Шаблон `survey.html`**: яким HTML-елементом рендерити (radio / checkbox / table).
- **Контролер**: як парсити відповідь із POST-запиту (один рядок / список / "Row: Column").
- **Шаблон `summary.html`**: як відображати відповідь у зведенні.

| Значення | HTML | Формат відповіді |
|---|---|---|
| `SINGLE_CHOICE` | `<input type="radio">` | `["Ja"]` |
| `MULTIPLE_CHOICE` | `<input type="checkbox">` | `["Bus", "U-Bahn"]` |
| `MATRIX` | `<table>` з radio per row | `["Preis: Sehr zufrieden", "Qualität: Zufrieden"]` |

### `src/main/java/com/blubbsoft/survey/model/Condition.java`

**Призначення:** Умова показу питання залежно від попередніх відповідей.

**Навіщо потрібен:** Java record з полями:
- `questionId` — ID питання, відповідь на яке перевіряється.
- `operator` — логічний оператор порівняння.
- `value` — значення для порівняння (EQUALS / NOT_EQUALS / CONTAINS).
- `values` — список значень (тільки для ANY_OF).

Якщо `condition` дорівнює `null`, питання показується завжди.

### `src/main/java/com/blubbsoft/survey/model/ConditionOperator.java`

**Призначення:** Enum чотирьох операторів порівняння для умов.

**Навіщо потрібен:**
- `EQUALS` — точна рівність (один відповідь = одне значення).
- `NOT_EQUALS` — нерівність.
- `CONTAINS` — список відповідей містить значення (для MULTIPLE_CHOICE).
- `ANY_OF` — список відповідей перетинається з `values[]` (для MULTIPLE_CHOICE).

### `src/main/java/com/blubbsoft/survey/model/ResultEntry.java`

**Призначення:** DTO для одного запису в JSON-файлі експорту/імпорту.

**Навіщо потрібен:** Представляє одну відповідь у серіалізованому форматі:
- `questionId` — ідентифікатор питання.
- `questionText` — текст питання (зберігається, щоб імпортовані результати були самодостатніми).
- `questionType` — тип питання (впливає на відображення в зведенні).
- `answers` — список відповідей.

Використовується `ExportImportService` при серіалізації та `SurveyController.toSummaryEntries()` при імпорті.

### `src/main/java/com/blubbsoft/survey/model/SummaryEntry.java`

**Призначення:** Presentation DTO для рядка таблиці на сторінці зведення.

**Навіщо потрібен:** Розширює `ResultEntry` полем `displayAnswer` — форматований рядок для відображення:
- SINGLE_CHOICE → просто значення (`"Ja"`).
- MULTIPLE_CHOICE → через кому (`"Bus, U-Bahn"`).
- MATRIX → через кому (`"Preis: Sehr zufrieden, Qualität: Zufrieden"`), але в шаблоні MATRIX рендериться як `<ul>` з окремих `answers`.

Розділення `ResultEntry` (persistence) і `SummaryEntry` (presentation) — принцип Single Responsibility.

### `src/main/java/com/blubbsoft/survey/model/SurveyResult.java`

**Призначення:** DTO для повного результату опитування (обгортка для JSON-файлу).

**Навіщо потрібен:** Структура верхнього рівня JSON-файлу:
- `questionnaire` — назва опитувальника.
- `exportedAt` — мітка часу експорту (ISO_LOCAL_DATE_TIME).
- `results` — список `ResultEntry`.

Використовується для серіалізації (експорт) та десеріалізації (імпорт).

---

## 4. Сервіси (Business Logic)

### `src/main/java/com/blubbsoft/survey/service/ConditionEvaluator.java`

**Призначення:** Модуль M1 — рушій умов (Condition Engine).

**Навіщо потрібен:** Єдиний метод `isConditionMet(Condition, Map<String, List<String>>)` інкапсулює всю логіку перевірки умов:
1. `null` condition → `true` (безумовне питання).
2. Питання-залежність не відповідне → `false`.
3. Порожній список відповідей → `false`.
4. Перевірка оператора (EQUALS / NOT_EQUALS / CONTAINS / ANY_OF).

Виділений в окремий `@Component` для ін'єкції в `QuestionnaireService`.

### `src/main/java/com/blubbsoft/survey/service/QuestionnaireService.java`

**Призначення:** Об'єднує модулі M2 (Navigation Engine) і M3 (Questionnaire Loader).

**Навіщо потрібен:**
- **Loader (`@PostConstruct init()`)**: завантажує JSON-файл опитувальника при старті Spring-контексту. Шлях береться з `application.properties`.
- **`loadFromResource(path)`**: парсить JSON → `Questionnaire` через Jackson `ObjectMapper`.
- **`getNextQuestion(session)`**: M2 — визначає наступне питання. Ітерує по всіх питаннях, пропускає вже відповідні та ті, чия умова не виконана. Повертає `null`, коли всі питання пройдені (сигнал для redirect на `/summary`).
- **`getQuestionById(id)`**: пошук питання за ID (використовується контролером для парсингу MATRIX-відповідей та побудови зведення).
- **`getQuestionnaire()`**: доступ до завантаженого об'єкту опитувальника.

### `src/main/java/com/blubbsoft/survey/service/ExportImportService.java`

**Призначення:** Модуль M5 — сервіс експорту/імпорту результатів.

**Навіщо потрібен:**
- **`exportToJson(session, questionnaire)`**: збирає відповіді з сесії, зіставляє їх з питаннями для отримання `questionText`, формує `SurveyResult` з міткою часу, серіалізує в prettified JSON `byte[]`.
- **`importFromJson(MultipartFile)`**: десеріалізує завантажений JSON-файл назад у `SurveyResult`. Кидає `IllegalArgumentException` з повідомленням німецькою при помилці парсингу.
- **`generateExportFilename(questionnaire)`**: генерує ім'я файлу у форматі `ergebnisse-{title}-{timestamp}.json` з нормалізацією спецсимволів.

Використовує копію `ObjectMapper` з увімкненим `INDENT_OUTPUT` для читабельного JSON.

---

## 5. Сесія (Session Management)

### `src/main/java/com/blubbsoft/survey/session/SurveySession.java`

**Призначення:** Модуль M4 — session-scoped bean зі станом опитування.

**Навіщо потрібен:** Зберігає стан одного проходження опитувальника:
- **`answers`** (`LinkedHashMap<String, List<String>>`) — відповіді, зберігаючи порядок вставки.
  - Ключ: ID питання (`"q1"`).
  - Значення: список відповідей (`["Ja"]`, `["Bus", "U-Bahn"]`, `["Preis: Sehr zufrieden"]`).
- **`history`** (`Deque<String>`) — стек показаних питань для back-навігації.

Ключові операції:
- `addAnswer()` — зберігає відповідь і додає ID у history-стек.
- `removeLastAnswer()` — знімає останнє питання зі стеку і видаляє відповідь (кнопка "Zurück").
- `invalidateAnswersAfter(questionId)` — при зміні відповіді (back + change) видаляє всі наступні відповіді для забезпечення консистентності.
- `reset()` — очищує все (кнопка "Neu starten").

Анотація `@SessionScope` гарантує, що кожен HTTP-сесія має свій екземпляр.

---

## 6. Контролер (Web Layer)

### `src/main/java/com/blubbsoft/survey/controller/SurveyController.java`

**Призначення:** Тонкий web-шар, який зв'язує HTTP-запити з бізнес-логікою.

**Навіщо потрібен:** Реалізує 8 ендпоінтів:

| Ендпоінт | Метод | Опис |
|---|---|---|
| `/` | GET | Redirect → `/survey` |
| `/survey` | GET | Показати наступне питання (або redirect → `/summary`) |
| `/survey/answer` | POST | Зберегти відповідь, додати до history |
| `/survey/back` | POST | Повернутися до попереднього питання |
| `/summary` | GET | Сторінка зведення (якщо всі питання пройдені) |
| `/summary/export` | GET | Завантажити JSON-файл результатів |
| `/summary/import` | POST | Завантажити JSON-файл (read-only перегляд) |
| `/survey/restart` | POST | Очистити сесію, почати з початку |

Контролер містить приватні helper-методи:
- `buildSummaryResults()` — збирає дані для зведення із сесії та `QuestionnaireService`.
- `toSummaryEntries()` / `toSummaryEntry()` — перетворює `ResultEntry` (імпортовані) у `SummaryEntry` (для відображення).
- `formatDisplayAnswer()` — форматує відповідь для відображення залежно від типу питання.
- `getQuestionNumber()` — визначає номер питання для прогрес-індикатора "Frage X von Y".

---

## 7. Ресурси

### `src/main/resources/application.properties`

**Призначення:** Конфігурація Spring Boot.

**Навіщо потрібен:** Два параметри:
- `server.port=8081` — порт HTTP-сервера.
- `survey.questionnaire-file=questionnaires/kinder-und-stadtteil.json` — шлях до активного опитувальника в classpath. Змінюючи цей рядок, можна переключатися між опитувальниками без перекомпіляції.

### `src/main/resources/questionnaires/kinder-und-stadtteil.json`

**Призначення:** Опитувальник #1 — "Kinder und Stadtteil" (5 питань: q1–q5).

**Навіщо потрібен:** Демонструє всі три типи питань:
- q1: SINGLE_CHOICE (без умови) — "Haben Sie Kinder?"
- q2: SINGLE_CHOICE (EQUALS q1="Ja") — "Wie viele Kinder?"
- q3: MULTIPLE_CHOICE (EQUALS q1="Ja") — "Welche Einrichtungen?"
- q4: MATRIX (CONTAINS q3="Grundschule") — "Schulessen-Zufriedenheit?"
- q5: SINGLE_CHOICE (без умови) — "In welchem Stadtteil?"

Ланцюг залежностей: q1 → q2, q3 → q4 демонструє вкладену умовну логіку.

### `src/main/resources/questionnaires/verkehr.json`

**Призначення:** Опитувальник #2 — "Verkehr" (4 питання: v1–v4).

**Навіщо потрібен:** Демонструє оператор `ANY_OF`:
- v1: MULTIPLE_CHOICE (без умови) — "Welche Verkehrsmittel?"
- v2: SINGLE_CHOICE (ANY_OF v1=["Bus","U-Bahn","Regionalzug"]) — "Welche Ticketart?"
- v3: SINGLE_CHOICE (ANY_OF v1=["Bus","U-Bahn","Regionalzug"]) — "Wie oft Verspätungen?"
- v4: SINGLE_CHOICE (CONTAINS v1="Auto") — "Eigenes Auto oder Firmenwagen?"

### `src/main/resources/templates/survey.html`

**Призначення:** Thymeleaf-шаблон для сторінки з одним питанням.

**Навіщо потрібен:** Рендерить:
- Заголовок опитувальника та прогрес ("Frage X von Y").
- Питання з відповідним типом введення (radio / checkbox / matrix table).
- Кнопку "Weiter" (submit відповіді).
- Кнопку "Zurück" (тільки якщо є history).
- Попередньо обрані відповіді при навігації назад (`previousAnswer`).

### `src/main/resources/templates/summary.html`

**Призначення:** Thymeleaf-шаблон для сторінки зведення.

**Навіщо потрібен:** Рендерить:
- Список усіх відповідей з форматуванням за типом (MATRIX → `<ul>`, інші → текст).
- Бейдж "📥 Importierte Ergebnisse" для імпортованих результатів.
- Блок помилки при невалідному імпорті.
- Кнопки: "JSON speichern" (експорт), "JSON öffnen" (імпорт), "Neu starten".

### `src/main/resources/static/css/style.css`

**Призначення:** Єдиний CSS-файл для стилізації всього додатку.

**Навіщо потрібен:** Реалізує чистий, мінімалістичний дизайн:
- Reset стилів та базова типографіка (system fonts).
- `.container` — центрований контейнер 720px.
- `.question-card` — картка з тінню для питання.
- `.option label` — інтерактивні елементи з hover-ефектами та підсвіткою вибраного.
- `.matrix` — стилізація таблиці з hover на рядках.
- `.btn-*` — система кнопок (primary/secondary/warning/export/import).
- `.summary-*` — стилізація зведення.
- `.imported-badge` / `.error-message` — информаційні блоки.
- Responsive дизайн (`@media max-width: 600px`).

---

## 8. Видалений мертвий код

### Видалені тести (`src/test/`)

| Файл | Що тестував |
|---|---|
| `ConditionEvaluatorTest.java` | M1 — 12 тестів усіх операторів та edge cases |
| `NavigationEngineTest.java` | M2 — 7 тестів лінійного потоку, conditional skip, back navigation |
| `QuestionnaireLoaderTest.java` | M3 — 9 тестів завантаження обох JSON-файлів |
| `ExportImportServiceTest.java` | M5 — 6 тестів export/import round-trip |

### Видалена тестова залежність (`pom.xml`)

- `spring-boot-starter-test` (scope: test) — JUnit 5, Mockito, AssertJ, Spring Test.

### Видалені методи (мертвий код після видалення тестів)

1. **`QuestionnaireService.setQuestionnaire(Questionnaire)`** — package-private setter, створений виключно для unit-тестів (`NavigationEngineTest`). В production-коді `questionnaire` встановлюється тільки через `@PostConstruct init()`.

2. **`ExportImportService.importFromJson(byte[])`** — перевантажений метод, який приймав `byte[]`. Використовувався тільки в `ExportImportServiceTest` для round-trip тестування. В production-коді контролер використовує лише `importFromJson(MultipartFile)`.

---

## Підсумок

Після чистки проект містить **14 production-файлів** (без рахування Maven Wrapper та `.gitignore`):

| Категорія | К-ть | Файли |
|---|---|---|
| Entry point | 1 | `SurveyApplication.java` |
| Model | 7 | `Questionnaire`, `Question`, `QuestionType`, `Condition`, `ConditionOperator`, `ResultEntry`, `SummaryEntry`, `SurveyResult` |
| Service | 3 | `ConditionEvaluator`, `QuestionnaireService`, `ExportImportService` |
| Session | 1 | `SurveySession` |
| Controller | 1 | `SurveyController` |
| Templates | 2 | `survey.html`, `summary.html` |
| Config | 1 | `application.properties` |
| Data | 2 | `kinder-und-stadtteil.json`, `verkehr.json` |
| Style | 1 | `style.css` |
| Docs | 2 | `README.md`, `CLAUDE.md` |

Жодного мертвого коду, невикористаних залежностей чи файлів не залишилось.

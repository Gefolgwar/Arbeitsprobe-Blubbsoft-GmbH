package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.*;
import com.blubbsoft.survey.session.SurveySession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportImportServiceTest {

    private ExportImportService exportImportService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exportImportService = new ExportImportService(objectMapper);
    }

    @Test
    void export_producesValidJsonWithCorrectStructure() throws Exception {
        SurveySession session = new SurveySession();
        session.addAnswer("q1", List.of("Ja"));
        session.addAnswer("q2", List.of("2"));

        Questionnaire questionnaire = buildTestQuestionnaire();

        byte[] json = exportImportService.exportToJson(session, questionnaire);
        String jsonString = new String(json);

        // Parse and verify structure
        SurveyResult result = objectMapper.readValue(json, SurveyResult.class);

        assertEquals("Test Fragebogen", result.questionnaire());
        assertNotNull(result.exportedAt());
        assertEquals(2, result.results().size());

        // Verify first result entry
        ResultEntry entry1 = result.results().get(0);
        assertEquals("q1", entry1.questionId());
        assertEquals("Haben Sie Kinder?", entry1.questionText());
        assertEquals("SINGLE_CHOICE", entry1.questionType());
        assertEquals(List.of("Ja"), entry1.answers());

        // Verify second result entry
        ResultEntry entry2 = result.results().get(1);
        assertEquals("q2", entry2.questionId());
        assertEquals("Wie viele?", entry2.questionText());
        assertEquals("SINGLE_CHOICE", entry2.questionType());
        assertEquals(List.of("2"), entry2.answers());
    }

    @Test
    void export_skipsUnansweredQuestions() throws Exception {
        SurveySession session = new SurveySession();
        session.addAnswer("q1", List.of("Nein"));
        // q2 not answered (skipped due to condition)

        Questionnaire questionnaire = buildTestQuestionnaire();

        byte[] json = exportImportService.exportToJson(session, questionnaire);
        SurveyResult result = objectMapper.readValue(json, SurveyResult.class);

        assertEquals(1, result.results().size());
        assertEquals("q1", result.results().get(0).questionId());
    }

    @Test
    void roundTrip_exportThenImport_producesEqualData() throws Exception {
        SurveySession session = new SurveySession();
        session.addAnswer("q1", List.of("Ja"));
        session.addAnswer("q2", List.of("2"));

        Questionnaire questionnaire = buildTestQuestionnaire();

        // Export
        byte[] json = exportImportService.exportToJson(session, questionnaire);

        // Import
        SurveyResult imported = exportImportService.importFromJson(json);

        // Verify round-trip
        assertEquals("Test Fragebogen", imported.questionnaire());
        assertNotNull(imported.exportedAt());
        assertEquals(2, imported.results().size());

        assertEquals("q1", imported.results().get(0).questionId());
        assertEquals("Haben Sie Kinder?", imported.results().get(0).questionText());
        assertEquals(List.of("Ja"), imported.results().get(0).answers());

        assertEquals("q2", imported.results().get(1).questionId());
        assertEquals(List.of("2"), imported.results().get(1).answers());
    }

    @Test
    void import_invalidJson_throwsMeaningfulError() {
        byte[] invalidJson = "{ this is not valid json".getBytes();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> exportImportService.importFromJson(invalidJson)
        );

        assertTrue(ex.getMessage().contains("Ungültige JSON-Datei"));
    }

    @Test
    void export_matrixAnswers_includedCorrectly() throws Exception {
        SurveySession session = new SurveySession();
        session.addAnswer("q3", List.of("Preis: Sehr zufrieden", "Qualität: Zufrieden"));

        Question q3 = new Question("q3", QuestionType.MATRIX, "Zufriedenheit?",
                null, null, List.of("Preis", "Qualität"), List.of("Sehr zufrieden", "Zufrieden"));
        Questionnaire questionnaire = new Questionnaire("Test", List.of(q3));

        byte[] json = exportImportService.exportToJson(session, questionnaire);
        SurveyResult result = objectMapper.readValue(json, SurveyResult.class);

        assertEquals("MATRIX", result.results().get(0).questionType());
        assertEquals(List.of("Preis: Sehr zufrieden", "Qualität: Zufrieden"),
                result.results().get(0).answers());
    }

    @Test
    void generateExportFilename_followsPattern() {
        Questionnaire questionnaire = new Questionnaire("Kinder und Stadtteil", List.of());

        String filename = exportImportService.generateExportFilename(questionnaire);

        assertTrue(filename.startsWith("ergebnisse-kinder-und-stadtteil-"));
        assertTrue(filename.endsWith(".json"));
    }

    // --- Helpers ---

    private Questionnaire buildTestQuestionnaire() {
        Question q1 = new Question("q1", QuestionType.SINGLE_CHOICE, "Haben Sie Kinder?",
                List.of("Ja", "Nein"), null, null, null);
        Question q2 = new Question("q2", QuestionType.SINGLE_CHOICE, "Wie viele?",
                List.of("1", "2", "3"), null, null, null);
        return new Questionnaire("Test Fragebogen", List.of(q1, q2));
    }
}

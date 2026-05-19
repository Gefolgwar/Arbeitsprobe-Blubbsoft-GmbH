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

        assertEquals("Test Fragebogen", result.getQuestionnaire());
        assertNotNull(result.getExportedAt());
        assertEquals(2, result.getResults().size());

        // Verify first result entry
        ResultEntry entry1 = result.getResults().get(0);
        assertEquals("q1", entry1.getQuestionId());
        assertEquals("Haben Sie Kinder?", entry1.getQuestionText());
        assertEquals("SINGLE_CHOICE", entry1.getQuestionType());
        assertEquals(List.of("Ja"), entry1.getAnswers());

        // Verify second result entry
        ResultEntry entry2 = result.getResults().get(1);
        assertEquals("q2", entry2.getQuestionId());
        assertEquals("Wie viele?", entry2.getQuestionText());
        assertEquals("SINGLE_CHOICE", entry2.getQuestionType());
        assertEquals(List.of("2"), entry2.getAnswers());
    }

    @Test
    void export_skipsUnansweredQuestions() throws Exception {
        SurveySession session = new SurveySession();
        session.addAnswer("q1", List.of("Nein"));
        // q2 not answered (skipped due to condition)

        Questionnaire questionnaire = buildTestQuestionnaire();

        byte[] json = exportImportService.exportToJson(session, questionnaire);
        SurveyResult result = objectMapper.readValue(json, SurveyResult.class);

        assertEquals(1, result.getResults().size());
        assertEquals("q1", result.getResults().get(0).getQuestionId());
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
        assertEquals("Test Fragebogen", imported.getQuestionnaire());
        assertNotNull(imported.getExportedAt());
        assertEquals(2, imported.getResults().size());

        assertEquals("q1", imported.getResults().get(0).getQuestionId());
        assertEquals("Haben Sie Kinder?", imported.getResults().get(0).getQuestionText());
        assertEquals(List.of("Ja"), imported.getResults().get(0).getAnswers());

        assertEquals("q2", imported.getResults().get(1).getQuestionId());
        assertEquals(List.of("2"), imported.getResults().get(1).getAnswers());
    }

    @Test
    void import_invalidJson_throwsMeaningfulError() {
        byte[] invalidJson = "{ this is not valid json".getBytes();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> exportImportService.importFromJson(invalidJson)
        );

        assertTrue(ex.getMessage().contains("Ung\u00fcltige JSON-Datei"));
    }

    @Test
    void export_matrixAnswers_includedCorrectly() throws Exception {
        SurveySession session = new SurveySession();
        session.addAnswer("q3", List.of("Preis: Sehr zufrieden", "Qualit\u00e4t: Zufrieden"));

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setTitle("Test");
        Question q3 = new Question();
        q3.setId("q3");
        q3.setType(QuestionType.MATRIX);
        q3.setText("Zufriedenheit?");
        q3.setRows(List.of("Preis", "Qualit\u00e4t"));
        q3.setColumns(List.of("Sehr zufrieden", "Zufrieden"));
        questionnaire.setQuestions(List.of(q3));

        byte[] json = exportImportService.exportToJson(session, questionnaire);
        SurveyResult result = objectMapper.readValue(json, SurveyResult.class);

        assertEquals("MATRIX", result.getResults().get(0).getQuestionType());
        assertEquals(List.of("Preis: Sehr zufrieden", "Qualit\u00e4t: Zufrieden"),
                result.getResults().get(0).getAnswers());
    }

    @Test
    void generateExportFilename_followsPattern() {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setTitle("Kinder und Stadtteil");

        String filename = exportImportService.generateExportFilename(questionnaire);

        assertTrue(filename.startsWith("ergebnisse-kinder-und-stadtteil-"));
        assertTrue(filename.endsWith(".json"));
    }

    // --- Helpers ---

    private Questionnaire buildTestQuestionnaire() {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setTitle("Test Fragebogen");

        Question q1 = new Question();
        q1.setId("q1");
        q1.setType(QuestionType.SINGLE_CHOICE);
        q1.setText("Haben Sie Kinder?");
        q1.setOptions(List.of("Ja", "Nein"));

        Question q2 = new Question();
        q2.setId("q2");
        q2.setType(QuestionType.SINGLE_CHOICE);
        q2.setText("Wie viele?");
        q2.setOptions(List.of("1", "2", "3"));

        questionnaire.setQuestions(List.of(q1, q2));
        return questionnaire;
    }
}

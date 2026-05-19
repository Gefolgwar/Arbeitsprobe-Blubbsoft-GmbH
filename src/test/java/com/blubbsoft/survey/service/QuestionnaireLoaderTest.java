package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionnaireLoaderTest {

    private QuestionnaireService service;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireService(new ObjectMapper());
    }

    @Test
    void kinderUndStadtteil_loadsCorrectly_5Questions() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");

        assertEquals("Kinder und Stadtteil", q.getTitle());
        assertEquals(5, q.getQuestions().size());
    }

    @Test
    void kinderUndStadtteil_questionTypes_areCorrect() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");

        assertEquals(QuestionType.SINGLE_CHOICE, q.getQuestions().get(0).getType()); // q1
        assertEquals(QuestionType.SINGLE_CHOICE, q.getQuestions().get(1).getType()); // q2
        assertEquals(QuestionType.MULTIPLE_CHOICE, q.getQuestions().get(2).getType()); // q3
        assertEquals(QuestionType.MATRIX, q.getQuestions().get(3).getType()); // q4
        assertEquals(QuestionType.SINGLE_CHOICE, q.getQuestions().get(4).getType()); // q5
    }

    @Test
    void kinderUndStadtteil_conditions_presentWhereExpected() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");

        assertNull(q.getQuestions().get(0).getCondition()); // q1 has no condition
        assertNotNull(q.getQuestions().get(1).getCondition()); // q2 conditional on q1
        assertNotNull(q.getQuestions().get(2).getCondition()); // q3 conditional on q1
        assertNotNull(q.getQuestions().get(3).getCondition()); // q4 conditional on q3
        assertNull(q.getQuestions().get(4).getCondition()); // q5 has no condition
    }

    @Test
    void kinderUndStadtteil_equalsCondition_parsedCorrectly() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");
        Condition cond = q.getQuestions().get(1).getCondition(); // q2

        assertEquals("q1", cond.getQuestionId());
        assertEquals(ConditionOperator.EQUALS, cond.getOperator());
        assertEquals("Ja", cond.getValue());
    }

    @Test
    void kinderUndStadtteil_containsCondition_parsedCorrectly() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");
        Condition cond = q.getQuestions().get(3).getCondition(); // q4

        assertEquals("q3", cond.getQuestionId());
        assertEquals(ConditionOperator.CONTAINS, cond.getOperator());
        assertEquals("Grundschule", cond.getValue());
    }

    @Test
    void kinderUndStadtteil_matrixQuestion_hasRowsAndColumns() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");
        Question matrix = q.getQuestions().get(3); // q4

        assertNotNull(matrix.getRows());
        assertNotNull(matrix.getColumns());
        assertEquals(4, matrix.getRows().size()); // Preis, Qualität, Auswahl, Portionsgröße
        assertEquals(5, matrix.getColumns().size()); // Sehr zufrieden .. Sehr unzufrieden
        assertTrue(matrix.getRows().contains("Preis"));
        assertTrue(matrix.getColumns().contains("Sehr zufrieden"));
    }

    @Test
    void verkehr_loadsCorrectly_4Questions() {
        Questionnaire q = service.loadFromResource("questionnaires/verkehr.json");

        assertEquals("Verkehr", q.getTitle());
        assertEquals(4, q.getQuestions().size());
    }

    @Test
    void verkehr_anyOfCondition_parsedCorrectly() {
        Questionnaire q = service.loadFromResource("questionnaires/verkehr.json");
        Condition cond = q.getQuestions().get(1).getCondition(); // v2

        assertEquals("v1", cond.getQuestionId());
        assertEquals(ConditionOperator.ANY_OF, cond.getOperator());
        assertNotNull(cond.getValues());
        assertEquals(3, cond.getValues().size());
        assertTrue(cond.getValues().contains("Bus"));
        assertTrue(cond.getValues().contains("U-Bahn"));
        assertTrue(cond.getValues().contains("Regionalzug"));
    }

    @Test
    void verkehr_containsCondition_forAutoQuestion() {
        Questionnaire q = service.loadFromResource("questionnaires/verkehr.json");
        Condition cond = q.getQuestions().get(3).getCondition(); // v4

        assertEquals("v1", cond.getQuestionId());
        assertEquals(ConditionOperator.CONTAINS, cond.getOperator());
        assertEquals("Auto", cond.getValue());
    }
}

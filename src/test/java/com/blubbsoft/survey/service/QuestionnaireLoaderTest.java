package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.Condition;
import com.blubbsoft.survey.model.ConditionOperator;
import com.blubbsoft.survey.model.QuestionType;
import com.blubbsoft.survey.model.Questionnaire;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionnaireLoaderTest {

    private QuestionnaireService service;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireService(new ObjectMapper(), new ConditionEvaluator());
    }

    @Test
    void kinderUndStadtteil_loadsCorrectly_5Questions() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");

        assertEquals("Kinder und Stadtteil", q.title());
        assertEquals(5, q.questions().size());
    }

    @Test
    void kinderUndStadtteil_questionTypes_areCorrect() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");

        assertEquals(QuestionType.SINGLE_CHOICE, q.questions().get(0).type());
        assertEquals(QuestionType.SINGLE_CHOICE, q.questions().get(1).type());
        assertEquals(QuestionType.MULTIPLE_CHOICE, q.questions().get(2).type());
        assertEquals(QuestionType.MATRIX, q.questions().get(3).type());
        assertEquals(QuestionType.SINGLE_CHOICE, q.questions().get(4).type());
    }

    @Test
    void kinderUndStadtteil_conditions_presentWhereExpected() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");

        assertNull(q.questions().get(0).condition());
        assertNotNull(q.questions().get(1).condition());
        assertNotNull(q.questions().get(2).condition());
        assertNotNull(q.questions().get(3).condition());
        assertNull(q.questions().get(4).condition());
    }

    @Test
    void kinderUndStadtteil_equalsCondition_parsedCorrectly() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");
        Condition cond = q.questions().get(1).condition();

        assertEquals("q1", cond.questionId());
        assertEquals(ConditionOperator.EQUALS, cond.operator());
        assertEquals("Ja", cond.value());
    }

    @Test
    void kinderUndStadtteil_containsCondition_parsedCorrectly() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");
        Condition cond = q.questions().get(3).condition();

        assertEquals("q3", cond.questionId());
        assertEquals(ConditionOperator.CONTAINS, cond.operator());
        assertEquals("Grundschule", cond.value());
    }

    @Test
    void kinderUndStadtteil_matrixQuestion_hasRowsAndColumns() {
        Questionnaire q = service.loadFromResource("questionnaires/kinder-und-stadtteil.json");
        var matrix = q.questions().get(3);

        assertNotNull(matrix.rows());
        assertNotNull(matrix.columns());
        assertEquals(4, matrix.rows().size());
        assertEquals(5, matrix.columns().size());
        assertTrue(matrix.rows().contains("Preis"));
        assertTrue(matrix.columns().contains("Sehr zufrieden"));
    }

    @Test
    void verkehr_loadsCorrectly_4Questions() {
        Questionnaire q = service.loadFromResource("questionnaires/verkehr.json");

        assertEquals("Verkehr", q.title());
        assertEquals(4, q.questions().size());
    }

    @Test
    void verkehr_anyOfCondition_parsedCorrectly() {
        Questionnaire q = service.loadFromResource("questionnaires/verkehr.json");
        Condition cond = q.questions().get(1).condition();

        assertEquals("v1", cond.questionId());
        assertEquals(ConditionOperator.ANY_OF, cond.operator());
        assertNotNull(cond.values());
        assertEquals(3, cond.values().size());
        assertTrue(cond.values().contains("Bus"));
        assertTrue(cond.values().contains("U-Bahn"));
        assertTrue(cond.values().contains("Regionalzug"));
    }

    @Test
    void verkehr_containsCondition_forAutoQuestion() {
        Questionnaire q = service.loadFromResource("questionnaires/verkehr.json");
        Condition cond = q.questions().get(3).condition();

        assertEquals("v1", cond.questionId());
        assertEquals(ConditionOperator.CONTAINS, cond.operator());
        assertEquals("Auto", cond.value());
    }
}

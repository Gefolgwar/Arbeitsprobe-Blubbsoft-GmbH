package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.Condition;
import com.blubbsoft.survey.model.ConditionOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConditionEvaluatorTest {

    private ConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ConditionEvaluator();
    }

    // --- null condition ---

    @Test
    void nullCondition_returnsTrue() {
        assertTrue(evaluator.isConditionMet(null, Map.of()));
    }

    // --- referenced question not answered ---

    @Test
    void referencedQuestionNotAnswered_returnsFalse() {
        Condition condition = makeCondition("q1", ConditionOperator.EQUALS, "Ja", null);
        assertFalse(evaluator.isConditionMet(condition, Map.of()));
    }

    @Test
    void referencedQuestionEmptyAnswer_returnsFalse() {
        Condition condition = makeCondition("q1", ConditionOperator.EQUALS, "Ja", null);
        Map<String, List<String>> answers = Map.of("q1", List.of());
        assertFalse(evaluator.isConditionMet(condition, answers));
    }

    // --- EQUALS ---

    @Test
    void equals_answerMatches_returnsTrue() {
        Condition condition = makeCondition("q1", ConditionOperator.EQUALS, "Ja", null);
        Map<String, List<String>> answers = Map.of("q1", List.of("Ja"));
        assertTrue(evaluator.isConditionMet(condition, answers));
    }

    @Test
    void equals_answerDoesNotMatch_returnsFalse() {
        Condition condition = makeCondition("q1", ConditionOperator.EQUALS, "Ja", null);
        Map<String, List<String>> answers = Map.of("q1", List.of("Nein"));
        assertFalse(evaluator.isConditionMet(condition, answers));
    }

    @Test
    void equals_multipleAnswers_returnsFalse() {
        // EQUALS requires exactly one answer matching
        Condition condition = makeCondition("q1", ConditionOperator.EQUALS, "Ja", null);
        Map<String, List<String>> answers = Map.of("q1", List.of("Ja", "Nein"));
        assertFalse(evaluator.isConditionMet(condition, answers));
    }

    // --- NOT_EQUALS ---

    @Test
    void notEquals_answerDiffers_returnsTrue() {
        Condition condition = makeCondition("q1", ConditionOperator.NOT_EQUALS, "Ja", null);
        Map<String, List<String>> answers = Map.of("q1", List.of("Nein"));
        assertTrue(evaluator.isConditionMet(condition, answers));
    }

    @Test
    void notEquals_answerMatches_returnsFalse() {
        Condition condition = makeCondition("q1", ConditionOperator.NOT_EQUALS, "Ja", null);
        Map<String, List<String>> answers = Map.of("q1", List.of("Ja"));
        assertFalse(evaluator.isConditionMet(condition, answers));
    }

    // --- CONTAINS ---

    @Test
    void contains_answerListContainsValue_returnsTrue() {
        Condition condition = makeCondition("q3", ConditionOperator.CONTAINS, "Grundschule", null);
        Map<String, List<String>> answers = Map.of("q3", List.of("Kindergarten", "Grundschule"));
        assertTrue(evaluator.isConditionMet(condition, answers));
    }

    @Test
    void contains_answerListDoesNotContainValue_returnsFalse() {
        Condition condition = makeCondition("q3", ConditionOperator.CONTAINS, "Grundschule", null);
        Map<String, List<String>> answers = Map.of("q3", List.of("Kindergarten", "Hort"));
        assertFalse(evaluator.isConditionMet(condition, answers));
    }

    // --- ANY_OF ---

    @Test
    void anyOf_answerContainsOneOfValues_returnsTrue() {
        Condition condition = makeCondition("v1", ConditionOperator.ANY_OF, null,
                List.of("Bus", "U-Bahn", "Regionalzug"));
        Map<String, List<String>> answers = Map.of("v1", List.of("Bus", "Auto"));
        assertTrue(evaluator.isConditionMet(condition, answers));
    }

    @Test
    void anyOf_answerContainsNoneOfValues_returnsFalse() {
        Condition condition = makeCondition("v1", ConditionOperator.ANY_OF, null,
                List.of("Bus", "U-Bahn", "Regionalzug"));
        Map<String, List<String>> answers = Map.of("v1", List.of("Auto", "Fahrrad"));
        assertFalse(evaluator.isConditionMet(condition, answers));
    }

    @Test
    void anyOf_answerContainsMultipleMatches_returnsTrue() {
        Condition condition = makeCondition("v1", ConditionOperator.ANY_OF, null,
                List.of("Bus", "U-Bahn", "Regionalzug"));
        Map<String, List<String>> answers = Map.of("v1", List.of("Bus", "U-Bahn"));
        assertTrue(evaluator.isConditionMet(condition, answers));
    }

    // --- Helper ---

    private Condition makeCondition(String questionId, ConditionOperator operator,
                                    String value, List<String> values) {
        Condition c = new Condition();
        c.setQuestionId(questionId);
        c.setOperator(operator);
        c.setValue(value);
        c.setValues(values);
        return c;
    }
}

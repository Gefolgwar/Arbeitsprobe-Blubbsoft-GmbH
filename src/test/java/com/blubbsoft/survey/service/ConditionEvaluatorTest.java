package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.Condition;
import com.blubbsoft.survey.model.ConditionOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        var condition = new Condition("q1", ConditionOperator.EQUALS, "Ja", null);
        assertFalse(evaluator.isConditionMet(condition, Map.of()));
    }

    @Test
    void referencedQuestionEmptyAnswer_returnsFalse() {
        var condition = new Condition("q1", ConditionOperator.EQUALS, "Ja", null);
        assertFalse(evaluator.isConditionMet(condition, Map.of("q1", List.of())));
    }

    // --- EQUALS ---

    @Test
    void equals_answerMatches_returnsTrue() {
        var condition = new Condition("q1", ConditionOperator.EQUALS, "Ja", null);
        assertTrue(evaluator.isConditionMet(condition, Map.of("q1", List.of("Ja"))));
    }

    @Test
    void equals_answerDoesNotMatch_returnsFalse() {
        var condition = new Condition("q1", ConditionOperator.EQUALS, "Ja", null);
        assertFalse(evaluator.isConditionMet(condition, Map.of("q1", List.of("Nein"))));
    }

    @Test
    void equals_multipleAnswers_returnsFalse() {
        var condition = new Condition("q1", ConditionOperator.EQUALS, "Ja", null);
        assertFalse(evaluator.isConditionMet(condition, Map.of("q1", List.of("Ja", "Nein"))));
    }

    // --- NOT_EQUALS ---

    @Test
    void notEquals_answerDiffers_returnsTrue() {
        var condition = new Condition("q1", ConditionOperator.NOT_EQUALS, "Ja", null);
        assertTrue(evaluator.isConditionMet(condition, Map.of("q1", List.of("Nein"))));
    }

    @Test
    void notEquals_answerMatches_returnsFalse() {
        var condition = new Condition("q1", ConditionOperator.NOT_EQUALS, "Ja", null);
        assertFalse(evaluator.isConditionMet(condition, Map.of("q1", List.of("Ja"))));
    }

    // --- CONTAINS ---

    @Test
    void contains_answerListContainsValue_returnsTrue() {
        var condition = new Condition("q3", ConditionOperator.CONTAINS, "Grundschule", null);
        assertTrue(evaluator.isConditionMet(condition, Map.of("q3", List.of("Kindergarten", "Grundschule"))));
    }

    @Test
    void contains_answerListDoesNotContainValue_returnsFalse() {
        var condition = new Condition("q3", ConditionOperator.CONTAINS, "Grundschule", null);
        assertFalse(evaluator.isConditionMet(condition, Map.of("q3", List.of("Kindergarten", "Hort"))));
    }

    // --- ANY_OF ---

    @Test
    void anyOf_answerContainsOneOfValues_returnsTrue() {
        var condition = new Condition("v1", ConditionOperator.ANY_OF, null, List.of("Bus", "U-Bahn", "Regionalzug"));
        assertTrue(evaluator.isConditionMet(condition, Map.of("v1", List.of("Bus", "Auto"))));
    }

    @Test
    void anyOf_answerContainsNoneOfValues_returnsFalse() {
        var condition = new Condition("v1", ConditionOperator.ANY_OF, null, List.of("Bus", "U-Bahn", "Regionalzug"));
        assertFalse(evaluator.isConditionMet(condition, Map.of("v1", List.of("Auto", "Fahrrad"))));
    }

    @Test
    void anyOf_answerContainsMultipleMatches_returnsTrue() {
        var condition = new Condition("v1", ConditionOperator.ANY_OF, null, List.of("Bus", "U-Bahn", "Regionalzug"));
        assertTrue(evaluator.isConditionMet(condition, Map.of("v1", List.of("Bus", "U-Bahn"))));
    }
}

package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.*;
import com.blubbsoft.survey.session.SurveySession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NavigationEngineTest {

    private QuestionnaireService service;
    private SurveySession session;

    @BeforeEach
    void setUp() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        service = new QuestionnaireService(new com.fasterxml.jackson.databind.ObjectMapper(), evaluator);
        session = new SurveySession();
    }

    // --- Linear flow (no conditions) ---

    @Test
    void linearFlow_returnsQuestionsInOrder() {
        // Use a questionnaire with no conditions for linear test
        Question q1 = makeQuestion("q1", QuestionType.SINGLE_CHOICE, null);
        Question q2 = makeQuestion("q2", QuestionType.SINGLE_CHOICE, null);
        Question q3 = makeQuestion("q3", QuestionType.SINGLE_CHOICE, null);
        Questionnaire q = new Questionnaire("Test", List.of(q1, q2, q3));

        service.setQuestionnaire(q);

        assertEquals("q1", service.getNextQuestion(session).id());

        session.addAnswer("q1", List.of("A"));
        assertEquals("q2", service.getNextQuestion(session).id());

        session.addAnswer("q2", List.of("B"));
        assertEquals("q3", service.getNextQuestion(session).id());

        session.addAnswer("q3", List.of("C"));
        assertNull(service.getNextQuestion(session));
    }

    // --- Conditional skip ---

    @Test
    void conditionalSkip_q1Nein_skipsQ2_showsQ3() {
        Question q1 = makeQuestion("q1", QuestionType.SINGLE_CHOICE, null);
        Question q2 = makeQuestion("q2", QuestionType.SINGLE_CHOICE,
                new Condition("q1", ConditionOperator.EQUALS, "Ja", null));
        Question q3 = makeQuestion("q3", QuestionType.SINGLE_CHOICE, null);
        Questionnaire q = new Questionnaire("Test", List.of(q1, q2, q3));

        service.setQuestionnaire(q);

        session.addAnswer("q1", List.of("Nein"));
        assertEquals("q3", service.getNextQuestion(session).id());
    }

    @Test
    void conditionalShow_q1Ja_showsQ2() {
        Question q1 = makeQuestion("q1", QuestionType.SINGLE_CHOICE, null);
        Question q2 = makeQuestion("q2", QuestionType.SINGLE_CHOICE,
                new Condition("q1", ConditionOperator.EQUALS, "Ja", null));
        Question q3 = makeQuestion("q3", QuestionType.SINGLE_CHOICE, null);
        Questionnaire q = new Questionnaire("Test", List.of(q1, q2, q3));

        service.setQuestionnaire(q);

        session.addAnswer("q1", List.of("Ja"));
        assertEquals("q2", service.getNextQuestion(session).id());
    }

    // --- ANY_OF condition ---

    @Test
    void anyOfCondition_busSelected_showsV2() {
        Question v1 = makeQuestion("v1", QuestionType.MULTIPLE_CHOICE, null);
        Question v2 = makeQuestion("v2", QuestionType.SINGLE_CHOICE,
                new Condition("v1", ConditionOperator.ANY_OF, null, List.of("Bus", "U-Bahn", "Regionalzug")));
        Questionnaire q = new Questionnaire("Verkehr", List.of(v1, v2));

        service.setQuestionnaire(q);

        session.addAnswer("v1", List.of("Bus", "Auto"));
        assertEquals("v2", service.getNextQuestion(session).id());
    }

    @Test
    void anyOfCondition_onlyAuto_skipsV2() {
        Question v1 = makeQuestion("v1", QuestionType.MULTIPLE_CHOICE, null);
        Question v2 = makeQuestion("v2", QuestionType.SINGLE_CHOICE,
                new Condition("v1", ConditionOperator.ANY_OF, null, List.of("Bus", "U-Bahn", "Regionalzug")));
        Question v3 = makeQuestion("v3", QuestionType.SINGLE_CHOICE, null);
        Questionnaire q = new Questionnaire("Verkehr", List.of(v1, v2, v3));

        service.setQuestionnaire(q);

        session.addAnswer("v1", List.of("Auto", "Fahrrad"));
        assertEquals("v3", service.getNextQuestion(session).id());
    }

    // --- Back navigation ---

    @Test
    void backNavigation_returnsToPreviousQuestion() {
        Question q1 = makeQuestion("q1", QuestionType.SINGLE_CHOICE, null);
        Question q2 = makeQuestion("q2", QuestionType.SINGLE_CHOICE, null);
        Questionnaire q = new Questionnaire("Test", List.of(q1, q2));

        service.setQuestionnaire(q);

        session.addAnswer("q1", List.of("A"));
        session.addAnswer("q2", List.of("B"));

        assertEquals("q2", session.removeLastAnswer());
        assertEquals("q2", service.getNextQuestion(session).id());
    }

    @Test
    void backNavigation_goBackTwice_returnsToQ1() {
        Question q1 = makeQuestion("q1", QuestionType.SINGLE_CHOICE, null);
        Question q2 = makeQuestion("q2", QuestionType.SINGLE_CHOICE, null);
        Questionnaire q = new Questionnaire("Test", List.of(q1, q2));

        service.setQuestionnaire(q);

        session.addAnswer("q1", List.of("A"));
        session.addAnswer("q2", List.of("B"));

        session.removeLastAnswer();
        session.removeLastAnswer();
        assertEquals("q1", service.getNextQuestion(session).id());
    }

    // --- Helper ---

    private Question makeQuestion(String id, QuestionType type, Condition condition) {
        return new Question(id, type, "Question " + id, List.of("A", "B"), condition, null, null);
    }
}

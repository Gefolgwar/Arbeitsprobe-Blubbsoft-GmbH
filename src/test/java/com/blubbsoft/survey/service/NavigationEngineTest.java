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
        Questionnaire q = new Questionnaire();
        q.setTitle("Test");

        Question q1 = makeQuestion("q1", QuestionType.SINGLE_CHOICE, null);
        Question q2 = makeQuestion("q2", QuestionType.SINGLE_CHOICE, null);
        Question q3 = makeQuestion("q3", QuestionType.SINGLE_CHOICE, null);
        q.setQuestions(List.of(q1, q2, q3));

        setQuestionnaireDirectly(q);

        // First question
        assertEquals("q1", service.getNextQuestion(session).getId());

        // Answer q1, get q2
        session.addAnswer("q1", List.of("A"));
        assertEquals("q2", service.getNextQuestion(session).getId());

        // Answer q2, get q3
        session.addAnswer("q2", List.of("B"));
        assertEquals("q3", service.getNextQuestion(session).getId());

        // Answer q3, done
        session.addAnswer("q3", List.of("C"));
        assertNull(service.getNextQuestion(session));
    }

    // --- Conditional skip ---

    @Test
    void conditionalSkip_q1Nein_skipsQ2_showsQ3() {
        Questionnaire q = new Questionnaire();
        q.setTitle("Test");

        Question q1 = makeQuestion("q1", QuestionType.SINGLE_CHOICE, null);
        Question q2 = makeQuestion("q2", QuestionType.SINGLE_CHOICE,
                makeCondition("q1", ConditionOperator.EQUALS, "Ja", null));
        Question q3 = makeQuestion("q3", QuestionType.SINGLE_CHOICE, null);
        q.setQuestions(List.of(q1, q2, q3));

        setQuestionnaireDirectly(q);

        // Answer q1 = "Nein" -> q2 condition (q1=Ja) not met -> skip to q3
        session.addAnswer("q1", List.of("Nein"));
        Question next = service.getNextQuestion(session);
        assertEquals("q3", next.getId());
    }

    @Test
    void conditionalShow_q1Ja_showsQ2() {
        Questionnaire q = new Questionnaire();
        q.setTitle("Test");

        Question q1 = makeQuestion("q1", QuestionType.SINGLE_CHOICE, null);
        Question q2 = makeQuestion("q2", QuestionType.SINGLE_CHOICE,
                makeCondition("q1", ConditionOperator.EQUALS, "Ja", null));
        Question q3 = makeQuestion("q3", QuestionType.SINGLE_CHOICE, null);
        q.setQuestions(List.of(q1, q2, q3));

        setQuestionnaireDirectly(q);

        session.addAnswer("q1", List.of("Ja"));
        Question next = service.getNextQuestion(session);
        assertEquals("q2", next.getId());
    }

    // --- ANY_OF condition ---

    @Test
    void anyOfCondition_busSelected_showsV2() {
        Questionnaire q = new Questionnaire();
        q.setTitle("Verkehr");

        Question v1 = makeQuestion("v1", QuestionType.MULTIPLE_CHOICE, null);
        Question v2 = makeQuestion("v2", QuestionType.SINGLE_CHOICE,
                makeCondition("v1", ConditionOperator.ANY_OF, null, List.of("Bus", "U-Bahn", "Regionalzug")));
        q.setQuestions(List.of(v1, v2));

        setQuestionnaireDirectly(q);

        session.addAnswer("v1", List.of("Bus", "Auto"));
        Question next = service.getNextQuestion(session);
        assertEquals("v2", next.getId());
    }

    @Test
    void anyOfCondition_onlyAuto_skipsV2() {
        Questionnaire q = new Questionnaire();
        q.setTitle("Verkehr");

        Question v1 = makeQuestion("v1", QuestionType.MULTIPLE_CHOICE, null);
        Question v2 = makeQuestion("v2", QuestionType.SINGLE_CHOICE,
                makeCondition("v1", ConditionOperator.ANY_OF, null, List.of("Bus", "U-Bahn", "Regionalzug")));
        Question v3 = makeQuestion("v3", QuestionType.SINGLE_CHOICE, null);
        q.setQuestions(List.of(v1, v2, v3));

        setQuestionnaireDirectly(q);

        session.addAnswer("v1", List.of("Auto", "Fahrrad"));
        Question next = service.getNextQuestion(session);
        assertEquals("v3", next.getId());
    }

    // --- Back navigation ---

    @Test
    void backNavigation_returnsToPreviousQuestion() {
        Questionnaire q = new Questionnaire();
        q.setTitle("Test");

        Question q1 = makeQuestion("q1", QuestionType.SINGLE_CHOICE, null);
        Question q2 = makeQuestion("q2", QuestionType.SINGLE_CHOICE, null);
        q.setQuestions(List.of(q1, q2));

        setQuestionnaireDirectly(q);

        session.addAnswer("q1", List.of("A"));
        session.addAnswer("q2", List.of("B"));

        // Go back: removes q2 answer
        String removedId = session.removeLastAnswer();
        assertEquals("q2", removedId);

        // Next question should be q2 again
        Question next = service.getNextQuestion(session);
        assertEquals("q2", next.getId());
    }

    @Test
    void backNavigation_goBackTwice_returnsToQ1() {
        Questionnaire q = new Questionnaire();
        q.setTitle("Test");

        Question q1 = makeQuestion("q1", QuestionType.SINGLE_CHOICE, null);
        Question q2 = makeQuestion("q2", QuestionType.SINGLE_CHOICE, null);
        q.setQuestions(List.of(q1, q2));

        setQuestionnaireDirectly(q);

        session.addAnswer("q1", List.of("A"));
        session.addAnswer("q2", List.of("B"));

        session.removeLastAnswer(); // remove q2
        session.removeLastAnswer(); // remove q1

        Question next = service.getNextQuestion(session);
        assertEquals("q1", next.getId());
    }

    // --- Helpers ---

    private void setQuestionnaireDirectly(Questionnaire questionnaire) {
        // Use reflection to set the private field for testing
        try {
            var field = QuestionnaireService.class.getDeclaredField("questionnaire");
            field.setAccessible(true);
            field.set(service, questionnaire);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Question makeQuestion(String id, QuestionType type, Condition condition) {
        Question q = new Question();
        q.setId(id);
        q.setType(type);
        q.setText("Question " + id);
        q.setOptions(List.of("A", "B"));
        q.setCondition(condition);
        return q;
    }

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

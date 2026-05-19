package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.Question;
import com.blubbsoft.survey.model.Questionnaire;
import com.blubbsoft.survey.session.SurveySession;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class QuestionnaireService {

    private final ObjectMapper objectMapper;
    private final ConditionEvaluator conditionEvaluator;

    @Value("${survey.questionnaire-file}")
    private String questionnaireFile;

    private Questionnaire questionnaire;

    public QuestionnaireService(ObjectMapper objectMapper, ConditionEvaluator conditionEvaluator) {
        this.objectMapper = objectMapper;
        this.conditionEvaluator = conditionEvaluator;
    }

    @PostConstruct
    public void init() {
        this.questionnaire = loadFromResource(questionnaireFile);
    }

    public Questionnaire loadFromResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readValue(is, Questionnaire.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load questionnaire from: " + path, e);
        }
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public Question getQuestionById(String id) {
        return questionnaire.questions().stream()
                .filter(q -> q.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the next relevant question for the survey.
     * Skips already-answered questions and those whose condition is not met.
     */
    public Question getNextQuestion(SurveySession session) {
        Map<String, List<String>> answers = session.getAnswers();

        for (Question question : questionnaire.questions()) {
            if (answers.containsKey(question.id())) {
                continue;
            }
            if (!conditionEvaluator.isConditionMet(question.condition(), answers)) {
                continue;
            }
            return question;
        }
        return null;
    }
}

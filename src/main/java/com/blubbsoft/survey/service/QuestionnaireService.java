package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.Question;
import com.blubbsoft.survey.model.Questionnaire;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class QuestionnaireService {

    private final ObjectMapper objectMapper;

    @Value("${survey.questionnaire-file}")
    private String questionnaireFile;

    private Questionnaire questionnaire;

    public QuestionnaireService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
        return questionnaire.getQuestions().stream()
                .filter(q -> q.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}

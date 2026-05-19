package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.Question;
import com.blubbsoft.survey.model.Questionnaire;
import com.blubbsoft.survey.model.ResultEntry;
import com.blubbsoft.survey.model.SurveyResult;
import com.blubbsoft.survey.session.SurveySession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for exporting and importing survey results as JSON.
 */
@Service
public class ExportImportService {

    private final ObjectMapper objectMapper;

    public ExportImportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public byte[] exportToJson(SurveySession session, Questionnaire questionnaire) {
        try {
            SurveyResult result = buildSurveyResult(session, questionnaire);
            return objectMapper.writeValueAsBytes(result);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export survey results", e);
        }
    }

    public SurveyResult importFromJson(MultipartFile file) {
        try {
            return objectMapper.readValue(file.getInputStream(), SurveyResult.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Ungültige JSON-Datei: " + e.getMessage(), e);
        }
    }

    private SurveyResult buildSurveyResult(SurveySession session, Questionnaire questionnaire) {
        List<ResultEntry> entries = new ArrayList<>();
        Map<String, List<String>> answers = session.getAnswers();

        for (Question question : questionnaire.questions()) {
            List<String> answer = answers.get(question.id());
            if (answer != null && !answer.isEmpty()) {
                entries.add(new ResultEntry(
                        question.id(), question.text(), question.type().name(), answer));
            }
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new SurveyResult(questionnaire.title(), timestamp, entries);
    }

    public String generateExportFilename(Questionnaire questionnaire) {
        String title = questionnaire.title()
                .toLowerCase()
                .replaceAll("[^a-z0-9äöüß]", "-")
                .replaceAll("-+", "-");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return "ergebnisse-" + title + "-" + timestamp + ".json";
    }
}

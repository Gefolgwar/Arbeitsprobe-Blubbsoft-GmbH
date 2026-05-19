package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.*;
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
        this.objectMapper = objectMapper;
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Exports the current survey session as JSON bytes.
     */
    public byte[] exportToJson(SurveySession session, Questionnaire questionnaire) {
        try {
            SurveyResult result = buildSurveyResult(session, questionnaire);
            return objectMapper.writeValueAsBytes(result);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export survey results", e);
        }
    }

    /**
     * Imports survey results from an uploaded JSON file.
     */
    public SurveyResult importFromJson(MultipartFile file) {
        try {
            return objectMapper.readValue(file.getInputStream(), SurveyResult.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Ung\u00fcltige JSON-Datei: " + e.getMessage(), e);
        }
    }

    /**
     * Imports survey results from a JSON byte array (for testing).
     */
    public SurveyResult importFromJson(byte[] jsonBytes) {
        try {
            return objectMapper.readValue(jsonBytes, SurveyResult.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Ung\u00fcltige JSON-Datei: " + e.getMessage(), e);
        }
    }

    private SurveyResult buildSurveyResult(SurveySession session, Questionnaire questionnaire) {
        List<ResultEntry> entries = new ArrayList<>();
        Map<String, List<String>> answers = session.getAnswers();

        for (Question question : questionnaire.getQuestions()) {
            List<String> answer = answers.get(question.getId());
            if (answer != null && !answer.isEmpty()) {
                entries.add(new ResultEntry(
                        question.getId(),
                        question.getText(),
                        question.getType().name(),
                        answer
                ));
            }
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new SurveyResult(questionnaire.getTitle(), timestamp, entries);
    }

    /**
     * Generates a filename for the export: ergebnisse-{title}-{timestamp}.json
     */
    public String generateExportFilename(Questionnaire questionnaire) {
        String title = questionnaire.getTitle()
                .toLowerCase()
                .replaceAll("[^a-z0-9\u00e4\u00f6\u00fc\u00df]", "-")
                .replaceAll("-+", "-");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return "ergebnisse-" + title + "-" + timestamp + ".json";
    }
}

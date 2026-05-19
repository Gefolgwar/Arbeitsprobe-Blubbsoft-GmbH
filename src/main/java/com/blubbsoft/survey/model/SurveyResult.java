package com.blubbsoft.survey.model;

import java.util.List;

/**
 * DTO for the full survey result (export/import JSON structure).
 */
public class SurveyResult {

    private String questionnaire;
    private String exportedAt;
    private List<ResultEntry> results;

    public SurveyResult() {
    }

    public SurveyResult(String questionnaire, String exportedAt, List<ResultEntry> results) {
        this.questionnaire = questionnaire;
        this.exportedAt = exportedAt;
        this.results = results;
    }

    public String getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(String questionnaire) {
        this.questionnaire = questionnaire;
    }

    public String getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(String exportedAt) {
        this.exportedAt = exportedAt;
    }

    public List<ResultEntry> getResults() {
        return results;
    }

    public void setResults(List<ResultEntry> results) {
        this.results = results;
    }
}

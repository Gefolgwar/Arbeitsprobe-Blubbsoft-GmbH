package com.blubbsoft.survey.model;

import java.util.List;

/**
 * DTO for the full survey result (export/import JSON structure).
 */
public record SurveyResult(String questionnaire, String exportedAt, List<ResultEntry> results) {
}

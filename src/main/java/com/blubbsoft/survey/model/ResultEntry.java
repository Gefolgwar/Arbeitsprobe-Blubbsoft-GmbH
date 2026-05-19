package com.blubbsoft.survey.model;

import java.util.List;

/**
 * DTO for a single result entry in the export/import JSON.
 */
public record ResultEntry(String questionId, String questionText, String questionType, List<String> answers) {
}

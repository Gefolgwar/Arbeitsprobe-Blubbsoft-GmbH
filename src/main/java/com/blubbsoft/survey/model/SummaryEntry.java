package com.blubbsoft.survey.model;

import java.util.List;

/**
 * Presentation DTO for a single result row on the summary page.
 */
public record SummaryEntry(String questionId, String questionText, String questionType,
                           List<String> answers, String displayAnswer) {
}

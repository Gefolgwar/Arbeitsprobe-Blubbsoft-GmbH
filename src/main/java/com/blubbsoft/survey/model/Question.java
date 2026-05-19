package com.blubbsoft.survey.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Question(String id, QuestionType type, String text, List<String> options,
                       Condition condition, List<String> rows, List<String> columns) {
}

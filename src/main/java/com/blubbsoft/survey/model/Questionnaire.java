package com.blubbsoft.survey.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Questionnaire(String title, List<Question> questions) {
}

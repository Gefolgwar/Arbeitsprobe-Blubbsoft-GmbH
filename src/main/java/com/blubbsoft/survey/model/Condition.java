package com.blubbsoft.survey.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Condition(String questionId, ConditionOperator operator, String value, List<String> values) {
}

package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.Condition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Evaluates whether a question's condition is satisfied based on previously given answers.
 * <p>
 * Rules:
 * - null condition → always met (question has no condition)
 * - Referenced question not answered → condition NOT met
 * - Empty answer list → condition NOT met
 */
@Component
public class ConditionEvaluator {

    public boolean isConditionMet(Condition condition, Map<String, List<String>> answers) {
        if (condition == null) {
            return true;
        }

        List<String> answerValues = answers.get(condition.questionId());
        if (answerValues == null || answerValues.isEmpty()) {
            return false;
        }

        return switch (condition.operator()) {
            case EQUALS -> answerValues.contains(condition.value()) && answerValues.size() == 1;
            case NOT_EQUALS -> !answerValues.contains(condition.value());
            case CONTAINS -> answerValues.contains(condition.value());
            case ANY_OF -> condition.values().stream().anyMatch(answerValues::contains);
        };
    }
}

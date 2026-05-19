package com.blubbsoft.survey.service;

import com.blubbsoft.survey.model.Condition;
import com.blubbsoft.survey.model.ConditionOperator;
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

    /**
     * Evaluates whether the given condition is met based on the current answers.
     *
     * @param condition the condition to evaluate (may be null)
     * @param answers   map of questionId → list of answer values
     * @return true if condition is met (or null), false otherwise
     */
    public boolean isConditionMet(Condition condition, Map<String, List<String>> answers) {
        if (condition == null) {
            return true;
        }

        List<String> answerValues = answers.get(condition.getQuestionId());
        if (answerValues == null || answerValues.isEmpty()) {
            return false;
        }

        return switch (condition.getOperator()) {
            case EQUALS -> answerValues.contains(condition.getValue()) && answerValues.size() == 1;
            case NOT_EQUALS -> !answerValues.contains(condition.getValue());
            case CONTAINS -> answerValues.contains(condition.getValue());
            case ANY_OF -> condition.getValues().stream().anyMatch(answerValues::contains);
        };
    }
}

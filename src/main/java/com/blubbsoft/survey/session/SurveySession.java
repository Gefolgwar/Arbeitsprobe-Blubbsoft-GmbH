package com.blubbsoft.survey.session;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.*;

/**
 * Session-scoped bean that holds the current survey state.
 * <p>
 * - answers: LinkedHashMap preserving insertion order (questionId → list of answer values)
 * - history: Deque (stack) of actually-shown question IDs for back-navigation
 */
@Component
@SessionScope
public class SurveySession {

    private final LinkedHashMap<String, List<String>> answers = new LinkedHashMap<>();
    private final Deque<String> history = new ArrayDeque<>();

    public void addAnswer(String questionId, List<String> values) {
        answers.put(questionId, values);
        history.push(questionId);
    }

    public String removeLastAnswer() {
        if (history.isEmpty()) {
            return null;
        }
        String lastQuestionId = history.pop();
        answers.remove(lastQuestionId);
        return lastQuestionId;
    }

    public Map<String, List<String>> getAnswers() {
        return Collections.unmodifiableMap(answers);
    }

    public Deque<String> getHistory() {
        return history;
    }

    public void reset() {
        answers.clear();
        history.clear();
    }

    /**
     * Invalidates all answers that come after the given questionId in the answer map.
     * Used when going back and changing an answer to maintain consistency.
     */
    public void invalidateAnswersAfter(String questionId) {
        boolean found = false;
        Iterator<String> it = answers.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (found) {
                it.remove();
                history.remove(key);
            } else if (key.equals(questionId)) {
                found = true;
            }
        }
    }
}

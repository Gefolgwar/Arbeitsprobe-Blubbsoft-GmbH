package com.blubbsoft.survey.model;

import java.util.List;

/**
 * DTO for a single result entry in the export/import JSON.
 */
public class ResultEntry {

    private String questionId;
    private String questionText;
    private String questionType;
    private List<String> answers;

    public ResultEntry() {
    }

    public ResultEntry(String questionId, String questionText, String questionType, List<String> answers) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.answers = answers;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }
}

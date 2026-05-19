package com.blubbsoft.survey.controller;

import com.blubbsoft.survey.model.Question;
import com.blubbsoft.survey.model.QuestionType;
import com.blubbsoft.survey.service.QuestionnaireService;
import com.blubbsoft.survey.session.SurveySession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SurveyController {

    private final QuestionnaireService questionnaireService;
    private final SurveySession session;

    public SurveyController(QuestionnaireService questionnaireService, SurveySession session) {
        this.questionnaireService = questionnaireService;
        this.session = session;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/survey";
    }

    @GetMapping("/survey")
    public String survey(Model model) {
        Question nextQuestion = questionnaireService.getNextQuestion(session);

        if (nextQuestion == null) {
            return "redirect:/summary";
        }

        model.addAttribute("title", questionnaireService.getQuestionnaire().getTitle());
        model.addAttribute("question", nextQuestion);
        model.addAttribute("questionNumber", getQuestionNumber(nextQuestion.getId()));
        model.addAttribute("totalQuestions", questionnaireService.getQuestionnaire().getQuestions().size());
        model.addAttribute("canGoBack", !session.getHistory().isEmpty());

        // Pre-select previous answer if going back
        List<String> previousAnswer = session.getAnswers().get(nextQuestion.getId());
        model.addAttribute("previousAnswer", previousAnswer);

        return "survey";
    }

    @PostMapping("/survey/answer")
    public String answer(@RequestParam Map<String, String> allParams) {
        String questionId = allParams.get("questionId");
        String questionType = allParams.get("questionType");

        if (questionId == null) {
            return "redirect:/survey";
        }

        List<String> answerValues;

        if ("MATRIX".equals(questionType)) {
            answerValues = new ArrayList<>();
            Question question = questionnaireService.getQuestionById(questionId);
            if (question != null && question.getRows() != null) {
                for (String row : question.getRows()) {
                    String value = allParams.get("row_" + row);
                    if (value != null) {
                        answerValues.add(row + ": " + value);
                    }
                }
            }
        } else if ("MULTIPLE_CHOICE".equals(questionType)) {
            answerValues = new ArrayList<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (entry.getKey().startsWith("answer")) {
                    answerValues.add(entry.getValue());
                }
            }
        } else {
            String answer = allParams.get("answer");
            answerValues = answer != null ? List.of(answer) : List.of();
        }

        if (!answerValues.isEmpty()) {
            if (session.getAnswers().containsKey(questionId)) {
                session.invalidateAnswersAfter(questionId);
            }
            session.addAnswer(questionId, answerValues);
        }

        return "redirect:/survey";
    }

    @PostMapping("/survey/back")
    public String back() {
        session.removeLastAnswer();
        return "redirect:/survey";
    }

    @GetMapping("/summary")
    public String summary(Model model) {
        // If survey not complete, redirect back
        Question nextQuestion = questionnaireService.getNextQuestion(session);
        if (nextQuestion != null) {
            return "redirect:/survey";
        }

        model.addAttribute("title", questionnaireService.getQuestionnaire().getTitle());
        model.addAttribute("results", buildSummaryResults());
        model.addAttribute("imported", false);

        return "summary";
    }

    @PostMapping("/survey/restart")
    public String restart() {
        session.reset();
        return "redirect:/survey";
    }

    /**
     * Builds a list of summary entries from the answered questions.
     * Each entry contains the question text and formatted answers.
     */
    private List<Map<String, Object>> buildSummaryResults() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, List<String>> answers = session.getAnswers();

        for (Map.Entry<String, List<String>> entry : answers.entrySet()) {
            Question question = questionnaireService.getQuestionById(entry.getKey());
            if (question == null) continue;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("questionId", question.getId());
            result.put("questionText", question.getText());
            result.put("questionType", question.getType().name());
            result.put("answers", entry.getValue());

            // Format display string
            if (question.getType() == QuestionType.MATRIX) {
                result.put("displayAnswer", String.join(", ", entry.getValue()));
            } else if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
                result.put("displayAnswer", String.join(", ", entry.getValue()));
            } else {
                result.put("displayAnswer", entry.getValue().isEmpty() ? "" : entry.getValue().get(0));
            }

            results.add(result);
        }
        return results;
    }

    private int getQuestionNumber(String questionId) {
        var questions = questionnaireService.getQuestionnaire().getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getId().equals(questionId)) {
                return i + 1;
            }
        }
        return 0;
    }
}

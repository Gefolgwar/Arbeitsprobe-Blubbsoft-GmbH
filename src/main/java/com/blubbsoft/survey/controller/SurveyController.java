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
            // Matrix answers come as row_RowName=ColumnValue
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
            // Multiple choice: collect all 'answer' params
            answerValues = new ArrayList<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (entry.getKey().startsWith("answer")) {
                    answerValues.add(entry.getValue());
                }
            }
        } else {
            // Single choice
            String answer = allParams.get("answer");
            answerValues = answer != null ? List.of(answer) : List.of();
        }

        if (!answerValues.isEmpty()) {
            // Invalidate subsequent answers if changing a previous answer
            if (session.getAnswers().containsKey(questionId)) {
                session.invalidateAnswersAfter(questionId);
                // Remove the current answer too so we can re-add it
                session.getAnswers();
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

    private int getQuestionNumber(String questionId) {
        List<Question> questions = questionnaireService.getQuestionnaire().getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getId().equals(questionId)) {
                return i + 1;
            }
        }
        return 0;
    }
}

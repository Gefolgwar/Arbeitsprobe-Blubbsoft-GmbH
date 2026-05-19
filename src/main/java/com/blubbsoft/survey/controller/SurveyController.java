package com.blubbsoft.survey.controller;

import com.blubbsoft.survey.model.Question;
import com.blubbsoft.survey.model.ResultEntry;
import com.blubbsoft.survey.model.SummaryEntry;
import com.blubbsoft.survey.model.SurveyResult;
import com.blubbsoft.survey.service.ExportImportService;
import com.blubbsoft.survey.service.QuestionnaireService;
import com.blubbsoft.survey.session.SurveySession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class SurveyController {

    private final QuestionnaireService questionnaireService;
    private final SurveySession session;
    private final ExportImportService exportImportService;

    public SurveyController(QuestionnaireService questionnaireService,
                            SurveySession session,
                            ExportImportService exportImportService) {
        this.questionnaireService = questionnaireService;
        this.session = session;
        this.exportImportService = exportImportService;
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

        model.addAttribute("title", questionnaireService.getQuestionnaire().title());
        model.addAttribute("question", nextQuestion);
        model.addAttribute("questionNumber", getQuestionNumber(nextQuestion.id()));
        model.addAttribute("totalQuestions", questionnaireService.getQuestionnaire().questions().size());
        model.addAttribute("canGoBack", !session.getHistory().isEmpty());
        model.addAttribute("previousAnswer", session.getAnswers().get(nextQuestion.id()));

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
            if (question != null && question.rows() != null) {
                for (String row : question.rows()) {
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
        if (questionnaireService.getNextQuestion(session) != null) {
            return "redirect:/survey";
        }

        model.addAttribute("title", questionnaireService.getQuestionnaire().title());
        model.addAttribute("results", buildSummaryResults());
        model.addAttribute("imported", false);

        return "summary";
    }

    @GetMapping("/summary/export")
    public ResponseEntity<byte[]> exportJson() {
        byte[] json = exportImportService.exportToJson(session, questionnaireService.getQuestionnaire());
        String filename = exportImportService.generateExportFilename(questionnaireService.getQuestionnaire());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @PostMapping("/summary/import")
    public String importJson(@RequestParam("file") MultipartFile file, Model model) {
        try {
            SurveyResult surveyResult = exportImportService.importFromJson(file);

            model.addAttribute("title", surveyResult.questionnaire());
            model.addAttribute("results", toSummaryEntries(surveyResult.results()));
            model.addAttribute("imported", true);
            model.addAttribute("exportedAt", surveyResult.exportedAt());

            return "summary";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", questionnaireService.getQuestionnaire().title());
            model.addAttribute("results", buildSummaryResults());
            model.addAttribute("imported", false);
            return "summary";
        }
    }

    @PostMapping("/survey/restart")
    public String restart() {
        session.reset();
        return "redirect:/survey";
    }

    // --- Summary helpers ---

    private List<SummaryEntry> buildSummaryResults() {
        List<SummaryEntry> results = new ArrayList<>();

        for (var entry : session.getAnswers().entrySet()) {
            Question question = questionnaireService.getQuestionById(entry.getKey());
            if (question == null) continue;

            results.add(toSummaryEntry(
                    question.id(), question.text(), question.type().name(), entry.getValue()));
        }
        return results;
    }

    private List<SummaryEntry> toSummaryEntries(List<ResultEntry> entries) {
        return entries.stream()
                .map(e -> toSummaryEntry(e.questionId(), e.questionText(), e.questionType(), e.answers()))
                .toList();
    }

    private SummaryEntry toSummaryEntry(String questionId, String questionText,
                                        String questionType, List<String> answers) {
        String displayAnswer = formatDisplayAnswer(questionType, answers);
        return new SummaryEntry(questionId, questionText, questionType, answers, displayAnswer);
    }

    private String formatDisplayAnswer(String questionType, List<String> answers) {
        if (answers.isEmpty()) return "";
        if ("MATRIX".equals(questionType) || "MULTIPLE_CHOICE".equals(questionType)) {
            return String.join(", ", answers);
        }
        return answers.get(0);
    }

    private int getQuestionNumber(String questionId) {
        var questions = questionnaireService.getQuestionnaire().questions();
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).id().equals(questionId)) {
                return i + 1;
            }
        }
        return 0;
    }
}

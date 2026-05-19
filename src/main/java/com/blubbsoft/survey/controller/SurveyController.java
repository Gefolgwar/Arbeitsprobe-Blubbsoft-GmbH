package com.blubbsoft.survey.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SurveyController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}

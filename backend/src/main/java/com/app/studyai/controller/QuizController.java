package com.app.studyai.controller;

import com.app.studyai.model.Quiz;
import com.app.studyai.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/generate/{documentId}")
    public ResponseEntity<Quiz> generateQuiz(@PathVariable Long documentId) {
        return ResponseEntity.ok(quizService.generateQuiz(documentId));
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<Quiz>> getQuizzes(@PathVariable Long documentId) {
        return ResponseEntity.ok(quizService.getQuizzesByDocument(documentId));
    }
}

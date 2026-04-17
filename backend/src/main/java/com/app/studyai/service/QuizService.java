package com.app.studyai.service;

import com.app.studyai.model.Document;
import com.app.studyai.model.Question;
import com.app.studyai.model.Quiz;
import com.app.studyai.repository.DocumentRepository;
import com.app.studyai.repository.QuizRepository;
import com.app.studyai.rag.LLMService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class QuizService {
    private final DocumentRepository documentRepository;
    private final QuizRepository quizRepository;
    private final LLMService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizService(DocumentRepository documentRepository, QuizRepository quizRepository, LLMService llmService) {
        this.documentRepository = documentRepository;
        this.quizRepository = quizRepository;
        this.llmService = llmService;
    }

    @Transactional
    public Quiz generateQuiz(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (document.getContent() == null || document.getContent().isEmpty()) {
            throw new RuntimeException("Document content is empty");
        }

        String json = llmService.generateQuiz(document.getContent());
        
        try {
            List<Map<String, Object>> questionsData = objectMapper.readValue(json, new TypeReference<>() {});
            
            if (questionsData == null || questionsData.isEmpty()) {
                throw new RuntimeException("AI returned an empty or invalid question set.");
            }

            Quiz quiz = new Quiz(documentId, "Quiz for " + document.getFilename());
            List<Question> questions = new ArrayList<>();
            
            for (Map<String, Object> qData : questionsData) {
                // More flexible validation
                if (!qData.containsKey("text") || !qData.containsKey("options")) {
                    log.warn("Skipping malformed quiz question: {}", qData);
                    continue; 
                }

                Question q = new Question();
                q.setQuiz(quiz);
                q.setText(qData.get("text").toString());
                q.setOptions((List<String>) qData.get("options"));
                
                // Safer Integer parsing in case AI returns index as string
                Object rawIndex = qData.get("correctAnswerIndex");
                if (rawIndex instanceof Number) {
                    q.setCorrectAnswerIndex(((Number) rawIndex).intValue());
                } else if (rawIndex != null) {
                    try {
                        q.setCorrectAnswerIndex(Integer.parseInt(rawIndex.toString()));
                    } catch (NumberFormatException nfe) {
                        log.warn("Invalid index format: {}", rawIndex);
                        q.setCorrectAnswerIndex(0);
                    }
                } else {
                    q.setCorrectAnswerIndex(0);
                }
                
                q.setExplanation((String) qData.get("explanation"));
                questions.add(q);
            }
            
            if (questions.isEmpty()) {
                throw new RuntimeException("Failed to generate valid questions. Please try again.");
            }

            quiz.setQuestions(questions);
            return quizRepository.save(quiz);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate quiz: " + e.getMessage(), e);
        }
    }

    public List<Quiz> getQuizzesByDocument(Long documentId) {
        return quizRepository.findByDocumentId(documentId);
    }
}

package com.app.studyai.controller;

import com.app.studyai.model.Document;
import com.app.studyai.repository.DocumentRepository;
import com.app.studyai.rag.LLMService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/youtube")
public class YouTubeController {

    private final LLMService llmService;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YouTubeController(LLMService llmService, DocumentRepository documentRepository) {
        this.llmService = llmService;
        this.documentRepository = documentRepository;
    }

    @GetMapping("/recommendations/{documentId}")
    public ResponseEntity<?> getRecommendations(@PathVariable Long documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        if (!"READY".equals(doc.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Document is not ready yet."));
        }

        try {
            String json = llmService.generateYouTubeSearchQueries(doc.getContent());
            List<?> queries = objectMapper.readValue(json, List.class);
            return ResponseEntity.ok(queries);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate recommendations: " + e.getMessage()));
        }
    }
}

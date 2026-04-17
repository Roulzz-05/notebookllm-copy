package com.app.studyai.service;

import com.app.studyai.rag.LLMService;
import com.app.studyai.rag.VectorService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatService {
    
    private final VectorService vectorService;
    private final LLMService llmService;

    public ChatService(VectorService vectorService, LLMService llmService) {
        this.vectorService = vectorService;
        this.llmService = llmService;
    }

    public String generateChatResponse(Long documentId, String query, String mode) {
        List<String> relatedChunks = vectorService.searchSimilar(documentId, query, 5);
        return llmService.generateChatResponse(query, relatedChunks, mode);
    }
}

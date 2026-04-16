package com.app.studyai.controller;

import com.app.studyai.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, Object> request) {
        Object rawId = request.get("documentId");
        if (rawId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "documentId is required"));
        }
        Long documentId = Long.valueOf(rawId.toString());
        String query = (String) request.get("query");
        
        String response = chatService.generateChatResponse(documentId, query);
        return ResponseEntity.ok(Map.of("response", response));
    }
}

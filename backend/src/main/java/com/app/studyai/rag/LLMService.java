package com.app.studyai.rag;

import java.util.List;

public interface LLMService {
    String generateChatResponse(String query, List<String> contextChunks);
    String generateStudyTopics(String documentContent);
}

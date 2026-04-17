package com.app.studyai.rag;

import java.util.List;

public interface LLMService {
    String generateChatResponse(String query, List<String> contextChunks, String mode);
    String generateStudyTopics(String documentContent);
    String generateQuiz(String documentContent);
    String generateStory(String documentContent);
    String generateYouTubeSearchQueries(String documentContent);
    String generateSummary(String documentContent);
    String generateStudyRoadmap(String documentContent);
}

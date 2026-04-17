package com.app.studyai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Service
public class GeminiLLMService implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(GeminiLLMService.class);

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model}")
    private String model;

    @Value("${app.gemini.fallback-model:gemini-1.5-flash}")
    private String fallbackModel;

    @Value("${app.gemini.base-url}")
    private String baseUrl;

    @Value("${app.gemini.mock-mode:false}")
    private boolean mockMode;

    @Value("${app.gemini.auto-mock-on-quota:true}")
    private boolean autoMockOnQuota;

    private final RestTemplate restTemplate = new RestTemplate();

    // ========================= CORE GEMINI CALL =========================

    private String callGemini(String modelName, String promptText) {
        String[] versions = {"v1beta", "v1"};
        Exception lastException = null;

        for (String version : versions) {
            String urlString = "https://generativelanguage.googleapis.com/" + version + "/models/" + modelName + ":generateContent?key=" + apiKey;
            URI url = UriComponentsBuilder.fromHttpUrl(urlString).build(true).toUri();
            
            log.info("Attempting Gemini API ({}/{}): models/{}:generateContent", version, modelName, modelName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> textPart = Map.of("text", promptText);
            Map<String, Object> content = Map.of("parts", List.of(textPart));
            Map<String, Object> body = Map.of("contents", List.of(content));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            int maxRetries = 3;
            long waitMs = 1000;

            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                    if (response.getBody() != null) {
                        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                        if (candidates != null && !candidates.isEmpty()) {
                            Map<String, Object> contentMap = (Map<String, Object>) candidates.get(0).get("content");
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                            return (String) parts.get(0).get("text");
                        }
                    }
                    throw new RuntimeException("Empty response from Gemini");
                } catch (HttpClientErrorException.NotFound e) {
                    log.warn("Model '{}' not found on endpoint '{}'. Trying next version...", modelName, version);
                    lastException = e;
                    break; // Move to next version
                } catch (HttpClientErrorException.TooManyRequests e) {
                    if (attempt < maxRetries) {
                        log.warn("Rate limit exceeded for Gemini API on attempt {}. Retrying in {}ms...", attempt + 1, waitMs);
                        sleep(waitMs);
                        waitMs *= 2; // Exponential backoff
                        continue;
                    }
                    log.error("Rate limit exceeded for Gemini API after {} retries", maxRetries);
                    throw new RuntimeException("AI service limit exceeded. Please try again in a few moments.", e);
                } catch (Exception e) {
                    log.error("Error calling Gemini API on {}: {}", version, e.getMessage());
                    lastException = e;
                    break; // Move to next version
                }
            }
        }
        throw new RuntimeException("Gemini call failed after trying all versions and retries: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    private String callWithFallback(String prompt, String originalContent, String feature) {
        if (mockMode) return getMockForFeature(feature, originalContent);

        // Extended fallback chain to handle quota/availability issues across different model tiers
        List<String> modelChain = new ArrayList<>();
        modelChain.add(model);
        modelChain.add(fallbackModel);
        modelChain.add("gemini-1.5-flash");
        modelChain.add("gemini-1.5-pro");
        modelChain.add("gemini-1.5-flash-8b");
        modelChain.add("gemini-2.0-flash-lite");

        Exception lastEx = null;
        for (String m : modelChain) {
            try {
                log.info("Trying AI model: {} for feature: {}", m, feature);
                return callGemini(m, prompt);
            } catch (Exception e) {
                lastEx = e;
                log.warn("Model {} failed for feature {}: {}", m, feature, e.getMessage());
                // If it's a 401/403 (Auth), don't bother trying other models with same key
                if (e.getMessage().contains("401") || e.getMessage().contains("403")) {
                    break;
                }
            }
        }

        if (autoMockOnQuota) {
            log.error("All AI models failed or were blocked. Activating DOCUMENT-AWARE SMART MOCK for feature: {}", feature);
            return getMockForFeature(feature, originalContent);
        }
        
        throw new RuntimeException("AI Service unavailable after trying multiple models. Last error: " + (lastEx != null ? lastEx.getMessage() : "Unknown"), lastEx);
    }

    private String getMockForFeature(String feature, String documentContent) {
        // Simple heuristic extraction from the document content (if provided)
        String snippet = "the document";
        List<String> keywords = new ArrayList<>();
        if (documentContent != null && documentContent.length() > 50) {
            snippet = documentContent.trim().substring(0, Math.min(200, documentContent.length()))
                    .replaceAll("[\\r\\n]+", " ").replaceAll("\"", "'");
            
            String[] lines = documentContent.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                // Filter for lines that look like headings
                if (trimmed.length() > 5 && trimmed.length() < 60 && Character.isUpperCase(trimmed.charAt(0)) && !trimmed.contains("{")) {
                    keywords.add(trimmed.replaceAll("\"", "'"));
                }
                if (keywords.size() > 10) break;
            }
        }
        
        if (keywords.isEmpty()) {
            keywords.add("Core Subject Concepts");
            keywords.add("Practical Implementation");
            keywords.add("Advanced Analysis");
            keywords.add("Case Study Findings");
            keywords.add("Summary and Review");
        }

        switch (feature) {
            case "chat":
                return "The AI service is currently at its limit. However, the document mentions: '" + snippet + "...'. Specifically, topics like " + keywords.get(0) + " are covered. [TUTOR MODE ACTIVE]";
            
            case "topics":
            case "roadmap":
                return buildJsonArray(keywords, "importance", "DEFAULT");
            
            case "quiz":
                return buildQuizJson(keywords);
            
            case "story":
                return "# The Tale of " + keywords.get(0) + "\n\nThis story begins with the foundational concepts of " + keywords.get(0) + ". As we delve deeper, we encounter " + (keywords.size() > 1 ? keywords.get(1) : "the unknown") + ". This path leads to a greater mastery of the subject matter discussed in your PDF.";
            
            case "summary":
                return "This document provides a detailed exploration of " + keywords.get(0) + ". It systematically covers key aspects including " + (keywords.size() > 2 ? keywords.get(1) + " and " + keywords.get(2) : "core principles") + ". It is ideal for students who need a structured overview of these specific technical areas.";
            
            case "youtube":
                return buildYouTubeJson(keywords);
            
            default:
                return "Local data for " + feature;
        }
    }

    private String buildJsonArray(List<String> items, String key, String value) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            sb.append("{\"title\":\"").append(items.get(i)).append("\", \"").append(key).append("\":\"").append(value).append("\", \"children\":[]}");
            if (i < items.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildQuizJson(List<String> keywords) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(5, keywords.size()); i++) {
            String q = "What is the primary role of " + keywords.get(i) + " as discussed in the text?";
            sb.append("{\"text\":\"").append(q).append("\", \"options\":[\"Primary Function\",\"Secondary Application\",\"Irrelevant Factor\",\"None of the above\"], \"correctAnswerIndex\":0, \"explanation\":\"This topic is central to the document's analysis.\"");
            sb.append("}");
            if (i < Math.min(5, keywords.size()) - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildYouTubeJson(List<String> keywords) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(6, keywords.size()); i++) {
            sb.append("{\"title\":\"Learn about ").append(keywords.get(i)).append("\", \"query\":\"").append(keywords.get(i)).append("\", \"description\":\"In-depth tutorial on ").append(keywords.get(i)).append("\", \"category\":\"Education\"}");
            if (i < Math.min(6, keywords.size()) - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    // ========================= FEATURES =========================

    @Override
    public String generateChatResponse(String query, List<String> contextChunks, String mode) {
        String persona;
        switch (mode != null ? mode.toLowerCase() : "teacher") {
            case "beginner":
                persona = "You are a friendly and patient tutor explaining things to a complete beginner. " +
                        "Use very simple words, relatable everyday analogies, and avoid jargon. ";
                break;
            case "meme":
                persona = "You are a hilariously funny AI who explains things using internet meme culture, pop-culture references, emojis, and humor. ";
                break;
            case "interview":
                persona = "You are a strict senior engineer conducting a technical interview. " +
                        "Answer the question concisely and precisely like a model answer in an interview. ";
                break;
            default: // "teacher"
                persona = "You are an expert teacher delivering a well-structured lesson. " +
                        "Start with a clear definition, explain the concept thoroughly with examples. ";
        }

        String prompt = persona + "\n\n" +
                "Use the document context below to ground your answer.\n" +
                "---------------------\n" +
                String.join("\n\n", contextChunks) +
                "\n---------------------\n" +
                "User Question: " + query + "\nAnswer:";
        
        if (mockMode) {
            return getMockForFeature("chat", query);
        }
        return callWithFallback(prompt, query, "chat");
    }

    @Override
    public String generateStudyTopics(String documentContent) {
        String truncated = documentContent.length() > 50000 ? documentContent.substring(0, 50000) : documentContent;
        String prompt = "Generate a JSON array of study topics from the following document:\n" + truncated + 
                        "\nFormat: [{\"title\": \"Topic\", \"importance\": \"HIGH|DEFAULT\", \"children\": []}]";

        if (mockMode)
            return getMockForFeature("topics", documentContent);
 
        String json = callWithFallback(prompt, documentContent, "topics");
        return scrubJson(json);
    }

    @Override
    public String generateQuiz(String documentContent) {
        String truncated = documentContent.length() > 50000 ? documentContent.substring(0, 50000) : documentContent;
        String prompt = "Generate 5 MCQs in JSON format:\n" + truncated +
                        "\nFormat: [{\"text\": \"Q\", \"options\": [\"A\",\"B\",\"C\",\"D\"], \"correctAnswerIndex\": 0, \"explanation\": \"E\"}]";

        if (mockMode)
            return getMockForFeature("quiz", documentContent);
 
        String json = callWithFallback(prompt, documentContent, "quiz");
        return scrubJson(json);
    }

    @Override
    public String generateStory(String documentContent) {
        String truncated = documentContent.length() > 50000 ? documentContent.substring(0, 50000) : documentContent;
        String prompt = "Convert into an engaging story:\n" + truncated;
        if (mockMode) return getMockForFeature("story", documentContent);
        return callWithFallback(prompt, documentContent, "story");
    }

    @Override
    public String generateSummary(String documentContent) {
        String truncated = documentContent.length() > 50000 ? documentContent.substring(0, 50000) : documentContent;
        String prompt = "Summarize the document concisely:\n" + truncated;
        if (mockMode) return getMockForFeature("summary", documentContent);
        return callWithFallback(prompt, documentContent, "summary");
    }

    @Override
    public String generateYouTubeSearchQueries(String documentContent) {
        String truncated = documentContent.length() > 8000 ? documentContent.substring(0, 8000) : documentContent;
        String prompt = "Generate 6 YouTube search queries in JSON:\n" + truncated +
                        "\nFormat: [{\"title\":\"T\", \"query\":\"Q\", \"description\":\"D\", \"category\":\"C\"}]";
        if (mockMode) return getMockForFeature("youtube", documentContent);
        String json = callWithFallback(prompt, documentContent, "youtube");
        return scrubJson(json);
    }

    @Override
    public String generateStudyRoadmap(String documentContent) {
        String truncated = documentContent.length() > 50000 ? documentContent.substring(0, 50000) : documentContent;
        String prompt = "Generate a study roadmap as a JSON array of topics:\n" + truncated +
                        "\nFormat: [{\"title\":\"T\", \"importance\":\"HIGH|DEFAULT\", \"children\":[]}]";
        if (mockMode) return getMockForFeature("roadmap", documentContent);
        String json = callWithFallback(prompt, documentContent, "roadmap");
        return scrubJson(json);
    }

    private String scrubJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "[]";
        String scrubbed = raw.trim();
        scrubbed = scrubbed.replaceAll("(?s)^.*?```(?:json)?\\s*", "");
        scrubbed = scrubbed.replaceAll("(?s)\\s*```.*$", "");
        scrubbed = scrubbed.trim();
        int startBracket = scrubbed.indexOf('[');
        int endBracket = scrubbed.lastIndexOf(']');
        int startBrace = scrubbed.indexOf('{');
        int endBrace = scrubbed.lastIndexOf('}');
        int start = -1; int end = -1;
        if (startBracket != -1) { start = startBracket; end = endBracket; }
        else if (startBrace != -1) { start = startBrace; end = endBrace; }
        if (start != -1 && end != -1 && end > start) return scrubbed.substring(start, end + 1);
        return scrubbed;
    }
}
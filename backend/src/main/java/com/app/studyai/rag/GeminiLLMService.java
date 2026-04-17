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
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.error("Rate limit exceeded for Gemini API");
                throw new RuntimeException("Rate limit exceeded", e);
            } catch (Exception e) {
                log.error("Error calling Gemini API on {}: {}", version, e.getMessage());
                lastException = e;
            }
        }
        throw new RuntimeException("Gemini call failed after trying all versions: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    private String callWithFallback(String prompt) {
        try {
            return callGemini(model, prompt);
        } catch (Exception e) {
            log.warn("Primary failed, switching to fallback model");
            return callGemini(fallbackModel, prompt);
        }
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
            return "Mock response from AI tutor.";
        }
        return callWithFallback(prompt);
    }

    @Override
    public String generateStudyTopics(String documentContent) {
        String truncated = documentContent.length() > 50000 ? documentContent.substring(0, 50000) : documentContent;
        String prompt = "Generate a JSON array of study topics from the following document:\n" + truncated + 
                        "\nFormat: [{\"title\": \"Topic\", \"importance\": \"HIGH|DEFAULT\", \"children\": []}]";

        if (mockMode)
            return "[{\"title\":\"Sample Topic\", \"importance\":\"HIGH\", \"children\":[]}]";

        String json = callWithFallback(prompt);
        return scrubJson(json);
    }

    @Override
    public String generateQuiz(String documentContent) {
        String truncated = documentContent.length() > 50000 ? documentContent.substring(0, 50000) : documentContent;
        String prompt = "Generate 5 MCQs in JSON format:\n" + truncated +
                        "\nFormat: [{\"text\": \"Q\", \"options\": [\"A\",\"B\",\"C\",\"D\"], \"correctAnswerIndex\": 0, \"explanation\": \"E\"}]";

        if (mockMode)
            return "[{\"text\":\"Mock?\", \"options\":[\"A\",\"B\",\"C\",\"D\"], \"correctAnswerIndex\":0, \"explanation\":\"E\"}]";

        String json = callWithFallback(prompt);
        return scrubJson(json);
    }

    @Override
    public String generateStory(String documentContent) {
        String truncated = documentContent.length() > 50000 ? documentContent.substring(0, 50000) : documentContent;
        String prompt = "Convert into an engaging story:\n" + truncated;
        if (mockMode) return "Mock story content";
        return callWithFallback(prompt);
    }

    @Override
    public String generateSummary(String documentContent) {
        String truncated = documentContent.length() > 50000 ? documentContent.substring(0, 50000) : documentContent;
        String prompt = "Summarize the document concisely:\n" + truncated;
        if (mockMode) return "Mock summary content";
        return callWithFallback(prompt);
    }

    @Override
    public String generateYouTubeSearchQueries(String documentContent) {
        String truncated = documentContent.length() > 8000 ? documentContent.substring(0, 8000) : documentContent;
        String prompt = "Generate 6 YouTube search queries in JSON:\n" + truncated +
                        "\nFormat: [{\"title\":\"T\", \"query\":\"Q\", \"description\":\"D\", \"category\":\"C\"}]";
        if (mockMode) return "[{\"title\":\"T\", \"query\":\"Q\", \"description\":\"D\", \"category\":\"C\"}]";
        String json = callWithFallback(prompt);
        return scrubJson(json);
    }

    @Override
    public String generateStudyRoadmap(String documentContent) {
        String truncated = documentContent.length() > 50000 ? documentContent.substring(0, 50000) : documentContent;
        String prompt = "Generate a study roadmap as a JSON array of topics:\n" + truncated +
                        "\nFormat: [{\"title\":\"T\", \"importance\":\"HIGH|DEFAULT\", \"children\":[]}]";
        if (mockMode) return "[{\"title\":\"Mock Roadmap\", \"importance\":\"HIGH\", \"children\":[]}]";
        String json = callWithFallback(prompt);
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
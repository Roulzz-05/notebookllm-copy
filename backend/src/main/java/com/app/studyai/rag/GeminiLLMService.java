package com.app.studyai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiLLMService implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(GeminiLLMService.class);

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model}")
    private String model;

    @Value("${app.gemini.fallback-model:gemini-1.5-flash-8b}")
    private String fallbackModel;

    @Value("${app.gemini.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String generateChatResponse(String query, List<String> contextChunks) {
        String prompt = "You are a helpful study AI assistant answering questions based on provided document context.\n\n" +
                "Context information is below.\n" +
                "---------------------\n" +
                String.join("\n\n", contextChunks) +
                "\n---------------------\n" +
                "Given the context information and no prior knowledge, answer the user's query.\n" +
                "Query: " + query + "\nAnswer:";
        return callGeminiWithFallback(prompt);
    }

    @Override
    public String generateStudyTopics(String documentContent) {
        String truncated = documentContent.length() > 50000
                ? documentContent.substring(0, 50000)
                : documentContent;

        String prompt = "You are an AI generating structured learning topics from a document.\n" +
                "Given the document content below, generate a JSON array of topics with dependencies.\n" +
                "Format: [ { \"title\": \"Topic Name\", \"importance\": \"HIGH|DEFAULT\", \"children\": [ { \"title\": \"Nested Topic\", \"importance\": \"DEFAULT\", \"children\": [] } ] } ]\n" +
                "Return ONLY valid JSON array and absolutely no other text, markdown, or explanation.\n\n" +
                "Document Content:\n" + truncated + "\n\nJSON array:";

        String json = callGeminiWithFallback(prompt);
        // Strip any markdown code fences and extraneous text the model might add
        json = scrubJson(json);
        
        log.info("Cleaned topics JSON (first 300 chars): {}",
                json.substring(0, Math.min(300, json.length())));
        return json;
    }

    private String scrubJson(String raw) {
        if (raw == null) return "[]";
        String scrubbed = raw.trim();
        // Remove markdown code fences: ```json ... ``` or ``` ... ```
        scrubbed = scrubbed.replaceAll("(?s)^.*?```(?:json)?\\s*", "");
        scrubbed = scrubbed.replaceAll("(?s)\\s*```.*$", "");
        scrubbed = scrubbed.trim();
        
        // If it starts with [ and ends with ], it's likely our array
        int start = scrubbed.indexOf('[');
        int end = scrubbed.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            scrubbed = scrubbed.substring(start, end + 1);
        }
        return scrubbed;
    }

    /**
     * Try primary model first, then fallback model if we get a quota error.
     */
    private String callGeminiWithFallback(String promptText) {
        try {
            return callGeminiModel(model, promptText);
        } catch (RuntimeException primaryEx) {
            if (primaryEx.getMessage() != null && primaryEx.getMessage().contains("429")) {
                log.warn("Primary model '{}' hit quota limit, trying fallback '{}'", model, fallbackModel);
                try {
                    return callGeminiModel(fallbackModel, promptText);
                } catch (RuntimeException fallbackEx) {
                    log.error("Fallback model '{}' also failed", fallbackModel, fallbackEx);
                    throw new RuntimeException(
                        "Both Gemini models are quota-limited. Please wait a minute and retry, or update your API key.",
                        fallbackEx);
                }
            }
            throw primaryEx;
        }
    }

    private String callGeminiModel(String modelName, String promptText) {
        String url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + modelName + ":generateContent?key=" + apiKey;
        log.info("Calling Gemini model '{}' ...", modelName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", promptText);

        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("parts", Collections.singletonList(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(contentMap));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // Simple retry: up to 3 attempts for transient server errors
        int maxRetries = 2;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> candidates =
                            (List<Map<String, Object>>) response.getBody().get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> content =
                                (Map<String, Object>) candidates.get(0).get("content");
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> parts =
                                (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
                log.warn("Gemini returned unexpected response body: {}", response.getBody());
                return "No response generated.";

            } catch (HttpClientErrorException.TooManyRequests e) {
                // 429 – quota; no point retrying with same model
                log.error("Gemini model '{}' quota exceeded: {}", modelName, e.getResponseBodyAsString());
                throw new RuntimeException("Gemini API error 429 TOO_MANY_REQUESTS: " + e.getResponseBodyAsString(), e);

            } catch (HttpClientErrorException e) {
                log.error("Gemini API client error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new RuntimeException("Gemini API error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);

            } catch (HttpServerErrorException e) {
                if (attempt < maxRetries) {
                    long delay = (long) Math.pow(2, attempt) * 1000;
                    log.warn("Gemini server error {} on attempt {}; retrying in {}ms", e.getStatusCode(), attempt + 1, delay);
                    try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.error("Gemini API server error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("Gemini server error " + e.getStatusCode(), e);
                }

            } catch (Exception e) {
                log.error("Error calling Gemini API", e);
                throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Gemini call failed after " + maxRetries + " retries");
    }
}

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
    public String generateChatResponse(String query, List<String> contextChunks, String mode) {
        String persona;
        switch (mode != null ? mode.toLowerCase() : "teacher") {
            case "beginner":
                persona = "You are a friendly and patient tutor explaining things to a complete beginner. " +
                        "Use very simple words, relatable everyday analogies, and avoid jargon. " +
                        "Break every concept down step-by-step so a 10-year-old could understand it. " +
                        "Be encouraging and warm in tone.";
                break;
            case "meme":
                persona = "You are a hilariously funny AI who explains things using internet meme culture, pop-culture references, emojis, and humor. " +
                        "Make the explanation entertaining and memorable. Use meme references where relevant (e.g. 'This is basically the dark souls of biology 💀'). " +
                        "Keep it actually educational but make it VERY fun to read. Use emojis liberally. 😂🔥";
                break;
            case "interview":
                persona = "You are a strict senior engineer conducting a technical interview. " +
                        "Answer the question concisely and precisely like a model answer in an interview. " +
                        "After your answer, pose ONE tricky follow-up question back to the user to test their deeper understanding. " +
                        "Be professional, direct, and demanding. No fluff.";
                break;
            default: // "teacher"
                persona = "You are an expert teacher delivering a well-structured lesson. " +
                        "Start with a clear definition, explain the concept thoroughly with examples, " +
                        "and end with a concise summary of key takeaways. " +
                        "Be professional, thorough, and organized.";
        }

        String prompt = persona + "\n\n" +
                "Use the document context below to ground your answer. Do not use knowledge outside this context.\n" +
                "---------------------\n" +
                String.join("\n\n", contextChunks) +
                "\n---------------------\n" +
                "User Question: " + query + "\nAnswer:";
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

    @Override
    public String generateQuiz(String documentContent) {
        String truncated = documentContent.length() > 50000
                ? documentContent.substring(0, 50000)
                : documentContent;

        String prompt = "You are an AI generating a high-quality multiple choice test from a document.\n" +
                "Given the document content below, generate a JSON array of 5 questions.\n" +
                "Each question must have exactly 4 options, a correct answer index (0-3), and a brief explanation.\n" +
                "Format precisely: [ { \"text\": \"Question text?\", \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"], \"correctAnswerIndex\": 0, \"explanation\": \"Why this is correct...\" } ]\n" +
                "Return ONLY a valid JSON array. Do not enclose it in an object like {\"quiz\": []}. Absolutely no other text.\n\n" +
                "Document Content:\n" + truncated + "\n\nJSON array:";

        String json = callGeminiWithFallback(prompt);
        return scrubJson(json);
    }

    @Override
    public String generateStory(String documentContent) {
        String truncated = documentContent.length() > 50000
                ? documentContent.substring(0, 50000)
                : documentContent;

        String prompt = "You are an expert storyteller and podcaster. Transform the following document content into a captivating, highly engaging narrative story.\n" +
                "Use analogies, a conversational yet professional tone, and vivid descriptions to make the topic come alive.\n" +
                "Structure the story with a catchy title (# Heading 1), a hook, clear narrative arcs, and a meaningful conclusion.\n" +
                "Format the story beautifully in Markdown, using formatting like bold reading and lists where appropriate. Do NOT use JSON.\n\n" +
                "Document Content:\n" + truncated + "\n\nStory in Markdown:";

        return callGeminiWithFallback(prompt);
    }

    @Override
    public String generateYouTubeSearchQueries(String documentContent) {
        String truncated = documentContent.length() > 8000
                ? documentContent.substring(0, 8000)
                : documentContent;

        String prompt = "You are an expert educational content curator. Analyze the document below and generate exactly 6 highly specific YouTube search queries that a student could use to find the best video explanations for the core concepts.\n" +
                "Each query should be precise enough to surface high-quality educational videos (e.g. 'Krebs cycle explained step by step animation', not just 'biology').\n" +
                "Also provide a short title (max 5 words) and a one-sentence description for each.\n" +
                "Return ONLY a valid JSON array. No markdown, no extra text.\n" +
                "Format: [{\"title\": \"Topic Title\", \"query\": \"exact youtube search query\", \"description\": \"Why this video helps\", \"category\": \"Fundamentals|Deep Dive|Visual|Practice|Overview|Advanced\"}]\n\n" +
                "Document Content:\n" + truncated + "\n\nJSON array:";

        String json = callGeminiWithFallback(prompt);
        return scrubJson(json);
    }
    @Override
    public String generateSummary(String documentContent) {
        String truncated = documentContent.length() > 50000
                ? documentContent.substring(0, 50000)
                : documentContent;

        String prompt = "You are an expert academic summarizer. Provide a concise, professional, and well-structured summary of the document below.\n" +
                "Focus on the main objectives, key findings, and core themes. Keep it under 300 words and use clear, professional English.\n" +
                "Format as plain text with clean paragraphs. No markdown bold or headers. Just the text.\n\n" +
                "Document Content:\n" + truncated + "\n\nSummary:";

        return callGeminiWithFallback(prompt);
    }

    @Override
    public String generateStudyRoadmap(String documentContent) {
        String truncated = documentContent.length() > 50000
                ? documentContent.substring(0, 50000)
                : documentContent;

        String prompt = "You are an AI generating a complete study roadmap for a student from a document.\n" +
                "You must provide BOTH a high-level summary and a structured list of study topics.\n" +
                "Format your response as a SINGLE JSON object with two fields:\n" +
                "1. 'summary': A concise, professional summary (max 300 words) of the core objectives and themes.\n" +
                "2. 'topics': A nested array of study topics. Each topic should have 'title', 'importance' (HIGH|DEFAULT), and 'children' (array of nested topics).\n\n" +
                "Format: { \"summary\": \"...\", \"topics\": [ { \"title\": \"Topic\", \"importance\": \"HIGH\", \"children\": [] } ] }\n" +
                "Return ONLY a valid JSON object. Absolutely no other text or explanation.\n\n" +
                "Document Content:\n" + truncated + "\n\nJSON Roadmap Object:";

        String json = callGeminiWithFallback(prompt);
        return scrubJson(json);
    }

    private String scrubJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "[]";
        String scrubbed = raw.trim();
        
        // Remove markdown code fences: ```json ... ``` or ``` ... ```
        scrubbed = scrubbed.replaceAll("(?s)^.*?```(?:json)?\\s*", "");
        scrubbed = scrubbed.replaceAll("(?s)\\s*```.*$", "");
        scrubbed = scrubbed.trim();
        
        // Attempt to extract the primary JSON array if model buried it in text
        int start = scrubbed.indexOf('[');
        int end = scrubbed.lastIndexOf(']');
        
        if (start != -1 && end != -1 && end > start) {
            String extracted = scrubbed.substring(start, end + 1);
            if (extracted.contains("{") && extracted.contains("}")) {
                return extracted;
            }
        }
        
        // Fallback for objects if model returns single object instead of array
        int startObj = scrubbed.indexOf('{');
        int endObj = scrubbed.lastIndexOf('}');
        if (startObj != -1 && endObj != -1 && endObj > startObj) {
            String obj = scrubbed.substring(startObj, endObj + 1);
            return "[" + obj + "]"; // Wrap it in an array so parser succeeds
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
            String msg = primaryEx.getMessage() != null ? primaryEx.getMessage() : "";
            if (msg.contains("429") || msg.contains("404")) {
                log.warn("Primary model '{}' hit error (429/404), trying fallback '{}'", model, fallbackModel);
                try {
                    return callGeminiModel(fallbackModel, promptText);
                } catch (RuntimeException fallbackEx) {
                    log.error("Fallback model '{}' also failed", fallbackModel, fallbackEx);
                    throw new RuntimeException(
                        "Both Gemini models failed (Quota or Not Found). Please update your API key or model name.",
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

        // Retry logic: 
        // - Up to 3 attempts for transient server errors (5xx)
        // - Up to 2 attempts for Rate Limit errors (429) with a 2s delay
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
                if (attempt < maxRetries) {
                    long delay = 2000; // 2 seconds delay for rate limit
                    log.warn("Gemini model '{}' rate limited (429) on attempt {}; retrying in {}ms", modelName, attempt + 1, delay);
                    try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.error("Gemini model '{}' quota exceeded after retries: {}", modelName, e.getResponseBodyAsString());
                    throw new RuntimeException("Gemini API error 429 TOO_MANY_REQUESTS: " + e.getResponseBodyAsString(), e);
                }

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

package com.app.studyai.service;

import com.app.studyai.model.Document;
import com.app.studyai.model.Topic;
import com.app.studyai.repository.DocumentRepository;
import com.app.studyai.repository.TopicRepository;
import com.app.studyai.rag.LLMService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StudySessionService {
    private static final Logger log = LoggerFactory.getLogger(StudySessionService.class);

    private final DocumentRepository documentRepository;
    private final TopicRepository topicRepository;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    public StudySessionService(DocumentRepository documentRepository,
                               TopicRepository topicRepository,
                               LLMService llmService) {
        this.documentRepository = documentRepository;
        this.topicRepository = topicRepository;
        this.llmService = llmService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public List<Topic> generateInitialTopics(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        if (document.getContent() == null || document.getContent().trim().length() < 10) {
            throw new RuntimeException(
                "Document content is too short or empty - please ensure the document is fully indexed before generating topics");
        }

        // Delete existing topics so "Regenerate" works correctly
        topicRepository.deleteByDocumentId(documentId);
        topicRepository.flush();

        log.info("Calling Gemini LLM to generate topics for document id={}", documentId);
        String jsonResponse;
        try {
            jsonResponse = llmService.generateStudyTopics(document.getContent());
        } catch (Exception e) {
            log.error("LLM call failed for document id={}", documentId, e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }

        log.debug("Gemini raw JSON response: {}", jsonResponse);

        List<Topic> rootTopics;
        try {
            List<Map<String, Object>> parsedTopics =
                    objectMapper.readValue(jsonResponse, new TypeReference<>() {});
            rootTopics = parseAndSaveTopics(document, null, parsedTopics);
        } catch (Exception e) {
            log.error("Failed to parse topics JSON for doc {}. Raw response was: [{}]", documentId, jsonResponse, e);
            String preview = jsonResponse.length() > 100 ? jsonResponse.substring(0, 100) + "..." : jsonResponse;
            throw new RuntimeException("AI returned invalid data format. Preview: " + preview);
        }

        if (rootTopics.isEmpty()) {
            throw new RuntimeException("AI returned an empty topic list");
        }

        // Re-fetch with EntityGraph so children are eagerly loaded for the controller
        return topicRepository.findByDocumentIdAndParentIsNull(documentId);
    }

    private List<Topic> parseAndSaveTopics(Document doc, Topic parent,
                                           List<Map<String, Object>> topicsData) {
        List<Topic> savedList = new ArrayList<>();
        if (topicsData == null) return savedList;

        for (Map<String, Object> data : topicsData) {
            Topic t = new Topic();
            t.setDocument(doc);
            t.setParent(parent);
            t.setTitle((String) data.get("title"));
            t.setImportance(data.getOrDefault("importance", "DEFAULT").toString());
            t.setCompleted(false);

            Topic savedTopic = topicRepository.save(t);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> childrenData =
                    (List<Map<String, Object>>) data.get("children");
            savedTopic.setChildren(parseAndSaveTopics(doc, savedTopic, childrenData));
            savedList.add(savedTopic);
        }
        return savedList;
    }

    @Transactional
    public Topic markCompleted(Long topicId) {
        Topic t = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));
        t.setCompleted(true);
        return topicRepository.save(t);
    }
}

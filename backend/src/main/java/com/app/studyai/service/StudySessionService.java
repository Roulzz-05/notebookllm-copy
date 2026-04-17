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

        log.info("Calling Gemini LLM to generate consolidated roadmap for document id={}", documentId);
        String roadmapJson;
        try {
            roadmapJson = llmService.generateStudyRoadmap(document.getContent());
        } catch (Exception e) {
            log.error("LLM roadmap call failed for document id={}", documentId, e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }

        log.debug("Gemini raw roadmap JSON: {}", roadmapJson);

        List<Topic> rootTopics;
        try {
            List<Map<String, Object>> topicsData = objectMapper.readValue(roadmapJson, new TypeReference<>() {});
            
            if (topicsData == null || topicsData.isEmpty()) {
                log.warn("AI returned an empty topics list for document id={}", documentId);
                rootTopics = new ArrayList<>();
            } else {
                rootTopics = parseAndSaveTopics(document, null, topicsData);
            }
        } catch (Exception e) {
            log.error("Failed to parse roadmap JSON for doc {}. Raw response: [{}]", documentId, roadmapJson, e);
            throw new RuntimeException("AI returned invalid roadmap format: " + e.getMessage());
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

    @Transactional(readOnly = true)
    public String getDocumentSummary(Long documentId) {
        return documentRepository.findById(documentId)
                .map(Document::getSummary)
                .orElse(null);
    }

    @Transactional
    public Topic toggleTopicCompletion(Long topicId) {
        Topic t = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));
        t.setCompleted(!t.isCompleted());
        return topicRepository.save(t);
    }
}

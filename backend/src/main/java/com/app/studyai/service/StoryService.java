package com.app.studyai.service;

import com.app.studyai.model.Document;
import com.app.studyai.model.Story;
import com.app.studyai.repository.DocumentRepository;
import com.app.studyai.repository.StoryRepository;
import com.app.studyai.rag.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StoryService {
    private static final Logger log = LoggerFactory.getLogger(StoryService.class);

    private final DocumentRepository documentRepository;
    private final StoryRepository storyRepository;
    private final LLMService llmService;

    public StoryService(DocumentRepository documentRepository, StoryRepository storyRepository, LLMService llmService) {
        this.documentRepository = documentRepository;
        this.storyRepository = storyRepository;
        this.llmService = llmService;
    }

    @Transactional
    public Story generateStory(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (document.getContent() == null || document.getContent().isEmpty()) {
            throw new RuntimeException("Document content is empty");
        }

        String storyMarkdown = llmService.generateStory(document.getContent());
        
        Story story = new Story(documentId, "Story for " + document.getFilename(), storyMarkdown);
        return storyRepository.save(story);
    }

    public List<Story> getStoriesByDocument(Long documentId) {
        return storyRepository.findByDocumentId(documentId);
    }
}

package com.app.studyai.rag;

import com.app.studyai.repository.DocumentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MockVectorService implements VectorService {

    private static final Logger log = LoggerFactory.getLogger(MockVectorService.class);

    // Document ID -> List of Text Chunks
    private final Map<Long, List<String>> chunkStorage = new ConcurrentHashMap<>();

    private final DocumentRepository documentRepository;

    public MockVectorService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * On startup: re-chunk any READY documents from the DB so the in-memory
     * store is not empty after a backend restart.
     */
    @PostConstruct
    public void rehydrateFromDatabase() {
        documentRepository.findAll().stream()
                .filter(doc -> "READY".equals(doc.getStatus()) && doc.getContent() != null)
                .forEach(doc -> {
                    List<String> chunks = chunkText(doc.getContent(), 1000, 200);
                    chunkStorage.put(doc.getId(), chunks);
                    log.info("Rehydrated {} chunks for document id={} ({})", chunks.size(), doc.getId(), doc.getFilename());
                });
    }

    @Override
    public void storeChunks(Long documentId, List<String> chunks) {
        chunkStorage.put(documentId, new ArrayList<>(chunks));
    }

    @Override
    public List<String> searchSimilar(Long documentId, String query, int topK) {
        List<String> docChunks = chunkStorage.getOrDefault(documentId, Collections.emptyList());
        if (docChunks.isEmpty()) return Collections.emptyList();

        String[] queryTokens = query.toLowerCase().split("\\W+");

        return docChunks.stream()
                .sorted(Comparator.comparingInt(chunk -> {
                    int score = 0;
                    String lowerChunk = chunk.toLowerCase();
                    for (String t : queryTokens) {
                        if (lowerChunk.contains(t)) score++;
                    }
                    return -score; // sort descending
                }))
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteChunks(Long documentId) {
        chunkStorage.remove(documentId);
        log.info("Deleted chunks for document id={}", documentId);
    }

    private List<String> chunkText(String text, int chunkSize, int overlapSize) {
        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlapSize;
        if (step <= 0) step = chunkSize;
        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
        }
        return chunks;
    }
}

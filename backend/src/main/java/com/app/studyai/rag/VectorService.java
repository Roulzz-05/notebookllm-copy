package com.app.studyai.rag;

import java.util.List;

public interface VectorService {
    void storeChunks(Long documentId, List<String> chunks);
    List<String> searchSimilar(Long documentId, String query, int topK);
    void deleteChunks(Long documentId);
}

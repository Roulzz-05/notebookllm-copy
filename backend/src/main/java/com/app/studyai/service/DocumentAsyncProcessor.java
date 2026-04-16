package com.app.studyai.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class DocumentAsyncProcessor {

    @Async("taskExecutor")
    public void processDocumentAsync(Long documentId, byte[] fileData, DocumentService documentService) {
        documentService.doProcess(documentId, fileData);
    }
}

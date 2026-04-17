package com.app.studyai.service;

import com.app.studyai.dto.DocumentResponse;
import com.app.studyai.model.Document;
import com.app.studyai.repository.DocumentRepository;
import com.app.studyai.repository.TopicRepository;
import com.app.studyai.rag.LLMService;
import com.app.studyai.rag.VectorService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    
    private final DocumentRepository documentRepository;
    private final TopicRepository topicRepository;
    private final VectorService vectorService;
    private final LLMService llmService;
    private final DocumentAsyncProcessor documentAsyncProcessor;

    public DocumentService(DocumentRepository documentRepository, 
                           TopicRepository topicRepository,
                           VectorService vectorService, 
                           LLMService llmService,
                           DocumentAsyncProcessor documentAsyncProcessor) {
        this.documentRepository = documentRepository;
        this.topicRepository = topicRepository;
        this.vectorService = vectorService;
        this.llmService = llmService;
        this.documentAsyncProcessor = documentAsyncProcessor;
    }

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file) {
        Document doc = Document.builder()
                .filename(file.getOriginalFilename())
                .status("UPLOADING")
                .uploadedAt(LocalDateTime.now())
                .build();
        
        doc = documentRepository.save(doc);

        try {
            documentAsyncProcessor.processDocumentAsync(doc.getId(), file.getBytes(), this);
        } catch (Exception e) {
            log.error("Failed to read bytes for doc {}", doc.getId(), e);
            updateStatus(doc.getId(), "FAILED");
        }

        return mapToResponse(doc);
    }

    public void doProcess(Long documentId, byte[] fileData) {
        log.info("Starting async processing for document id: {}", documentId);
        updateStatus(documentId, "PROCESSING");

        try (PDDocument pdfDocument = Loader.loadPDF(fileData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(pdfDocument);

            List<String> chunks = chunkText(rawText, 1000, 200);
            
            // Store Document with full content
            updateDocumentContent(documentId, rawText, chunks.size());

            // Generate and store summary (using LLM or fallback)
            String summary = llmService.generateSummary(rawText);
            updateDocumentSummary(documentId, summary);

            // Store in Vector DB (Mocked)
            vectorService.storeChunks(documentId, chunks);

            log.info("Finished processing for document id: {}", documentId);
            updateStatus(documentId, "READY");

        } catch (Exception e) {
            log.error("Error processing document id: {}", documentId, e);
            updateStatus(documentId, "FAILED");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(Long id, String status) {
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setStatus(status);
            documentRepository.save(doc);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDocumentContent(Long id, String content, int chunks) {
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setContent(content);
            doc.setTotalChunks(chunks);
            documentRepository.save(doc);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDocumentSummary(Long id, String summary) {
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setSummary(summary);
            documentRepository.save(doc);
        });
    }

    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream().map(this::mapToResponse).toList();
    }
 
    @Transactional
    public void deleteDocument(Long id) {
        log.info("Deleting document id: {}", id);
        // 1. Clean up topics (Study Sessions)
        topicRepository.deleteByDocumentId(id);
        // 2. Clean up vector chunks
        vectorService.deleteChunks(id);
        // 3. Remove document metadata and full content
        documentRepository.deleteById(id);
    }

    // Very naive chunking for demonstration
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

    private DocumentResponse mapToResponse(Document doc) {
        DocumentResponse res = new DocumentResponse();
        res.setId(doc.getId());
        res.setFilename(doc.getFilename());
        res.setStatus(doc.getStatus());
        res.setTotalChunks(doc.getTotalChunks());
        res.setUploadedAt(doc.getUploadedAt());
        return res;
    }
}

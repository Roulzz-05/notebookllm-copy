package com.app.studyai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String status; // UPLOADING, PROCESSING, READY, FAILED
    private int totalChunks;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private LocalDateTime uploadedAt;

    // ── No-arg constructor ──────────────────────────────────────
    public Document() {}

    // ── All-arg constructor ─────────────────────────────────────
    public Document(Long id, String filename, String status, int totalChunks, String content, LocalDateTime uploadedAt) {
        this.id = id;
        this.filename = filename;
        this.status = status;
        this.totalChunks = totalChunks;
        this.content = content;
        this.uploadedAt = uploadedAt;
    }

    // ── Builder ─────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String filename;
        private String status;
        private int totalChunks;
        private String content;
        private LocalDateTime uploadedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder filename(String filename) { this.filename = filename; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder totalChunks(int totalChunks) { this.totalChunks = totalChunks; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder uploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; return this; }

        public Document build() {
            return new Document(id, filename, status, totalChunks, content, uploadedAt);
        }
    }

    // ── Getters ─────────────────────────────────────────────────
    public Long getId() { return id; }
    public String getFilename() { return filename; }
    public String getStatus() { return status; }
    public int getTotalChunks() { return totalChunks; }
    public String getContent() { return content; }
    public String getSummary() { return summary; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }

    // ── Setters ─────────────────────────────────────────────────
    public void setId(Long id) { this.id = id; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setStatus(String status) { this.status = status; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    public void setContent(String content) { this.content = content; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}

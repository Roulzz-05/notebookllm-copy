package com.app.studyai.dto;

import java.time.LocalDateTime;

public class DocumentResponse {
    private Long id;
    private String filename;
    private String status;
    private int totalChunks;
    private LocalDateTime uploadedAt;

    public Long getId() { return id; }
    public String getFilename() { return filename; }
    public String getStatus() { return status; }
    public int getTotalChunks() { return totalChunks; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }

    public void setId(Long id) { this.id = id; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setStatus(String status) { this.status = status; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}

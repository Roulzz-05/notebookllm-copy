package com.app.studyai.dto;

import java.util.List;

public class TopicResponse {
    private Long id;
    private String title;
    private String importance;
    private boolean completed;
    private List<TopicResponse> children;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getImportance() { return importance; }
    public boolean isCompleted() { return completed; }
    public List<TopicResponse> getChildren() { return children; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setImportance(String importance) { this.importance = importance; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setChildren(List<TopicResponse> children) { this.children = children; }
}

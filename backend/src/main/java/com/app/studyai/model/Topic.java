package com.app.studyai.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Entity
@Table(name = "topics")
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    @JsonIgnore
    private Document document;

    private String title;
    private String importance; // HIGH, DEFAULT

    private boolean completed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private Topic parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<Topic> children;

    // ── Constructors ────────────────────────────────────────────
    public Topic() {}

    public Topic(Long id, Document document, String title, String importance, boolean completed, Topic parent, List<Topic> children) {
        this.id = id;
        this.document = document;
        this.title = title;
        this.importance = importance;
        this.completed = completed;
        this.parent = parent;
        this.children = children;
    }

    // ── Builder ──────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Document document;
        private String title;
        private String importance;
        private boolean completed;
        private Topic parent;
        private List<Topic> children;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder document(Document document) { this.document = document; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder importance(String importance) { this.importance = importance; return this; }
        public Builder completed(boolean completed) { this.completed = completed; return this; }
        public Builder parent(Topic parent) { this.parent = parent; return this; }
        public Builder children(List<Topic> children) { this.children = children; return this; }

        public Topic build() {
            return new Topic(id, document, title, importance, completed, parent, children);
        }
    }

    // ── Getters ──────────────────────────────────────────────────
    public Long getId() { return id; }
    public Document getDocument() { return document; }
    public String getTitle() { return title; }
    public String getImportance() { return importance; }
    public boolean isCompleted() { return completed; }
    public Topic getParent() { return parent; }
    public List<Topic> getChildren() { return children; }

    // ── Setters ──────────────────────────────────────────────────
    public void setId(Long id) { this.id = id; }
    public void setDocument(Document document) { this.document = document; }
    public void setTitle(String title) { this.title = title; }
    public void setImportance(String importance) { this.importance = importance; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setParent(Topic parent) { this.parent = parent; }
    public void setChildren(List<Topic> children) { this.children = children; }
}

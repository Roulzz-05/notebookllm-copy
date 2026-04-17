package com.app.studyai.controller;

import com.app.studyai.dto.TopicResponse;
import com.app.studyai.model.Topic;
import com.app.studyai.service.StudySessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class StudySessionController {

    private final StudySessionService studySessionService;

    public StudySessionController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    @GetMapping("/study-session/{documentId}")
    public ResponseEntity<Map<String, Object>> getStudySession(@PathVariable Long documentId) {
        List<Topic> topics = studySessionService.generateInitialTopics(documentId);
        List<TopicResponse> response = topics.stream().map(this::mapToResponse).collect(Collectors.toList());

        // Also fetch document to get its summary
        String summary = studySessionService.getDocumentSummary(documentId);

        return ResponseEntity.ok(Map.of(
            "topics", response,
            "summary", summary != null ? summary : ""
        ));
    }

    @PostMapping("/topic/complete/{id}")
    public ResponseEntity<TopicResponse> completeTopic(@PathVariable Long id) {
        Topic t = studySessionService.toggleTopicCompletion(id);
        return ResponseEntity.ok(mapToResponse(t));
    }

    private TopicResponse mapToResponse(Topic t) {
        TopicResponse tr = new TopicResponse();
        tr.setId(t.getId());
        tr.setTitle(t.getTitle());
        tr.setImportance(t.getImportance());
        tr.setCompleted(t.isCompleted());
        if (t.getChildren() != null) {
            tr.setChildren(t.getChildren().stream().map(this::mapToResponse).collect(Collectors.toList()));
        }
        return tr;
    }
}

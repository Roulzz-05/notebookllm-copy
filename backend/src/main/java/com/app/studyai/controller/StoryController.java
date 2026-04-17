package com.app.studyai.controller;

import com.app.studyai.model.Story;
import com.app.studyai.service.StoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/story")
@CrossOrigin(origins = "*")
public class StoryController {

    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping("/generate/{documentId}")
    public ResponseEntity<Story> generateStory(@PathVariable Long documentId) {
        return ResponseEntity.ok(storyService.generateStory(documentId));
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<Story>> getStories(@PathVariable Long documentId) {
        return ResponseEntity.ok(storyService.getStoriesByDocument(documentId));
    }
}

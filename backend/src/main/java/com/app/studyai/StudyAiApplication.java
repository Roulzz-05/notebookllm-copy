package com.app.studyai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class StudyAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudyAiApplication.class, args);
    }
}

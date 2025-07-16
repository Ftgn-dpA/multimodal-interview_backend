package com.example.interview.model;

import org.springframework.web.multipart.MultipartFile;

public class AudioInteractRequest {
    private String sessionId;
    private MultipartFile audio;

    public AudioInteractRequest() {}

    public AudioInteractRequest(String sessionId, MultipartFile audio) {
        this.sessionId = sessionId;
        this.audio = audio;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public MultipartFile getAudio() {
        return audio;
    }

    public void setAudio(MultipartFile audio) {
        this.audio = audio;
    }
} 
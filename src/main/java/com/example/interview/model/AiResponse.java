package com.example.interview.model;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "ai_responses")
public class AiResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_record_id")
    private InterviewRecord interviewRecord;

    @Column(name = "ai_response", columnDefinition = "JSON")
    private String aiResponse; // AI回复数组，每轮对话的完整回复作为一个JSON数组元素

    public AiResponse() {
    }

    public AiResponse(InterviewRecord interviewRecord, String aiResponse) {
        this.interviewRecord = interviewRecord;
        this.aiResponse = aiResponse;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InterviewRecord getInterviewRecord() {
        return interviewRecord;
    }

    public void setInterviewRecord(InterviewRecord interviewRecord) {
        this.interviewRecord = interviewRecord;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }
} 
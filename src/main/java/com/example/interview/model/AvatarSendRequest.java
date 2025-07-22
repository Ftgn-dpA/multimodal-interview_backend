package com.example.interview.model;

public class AvatarSendRequest {
    private String sessionId;
    private String text;
    private String role;
    private String type;
    private Long interviewRecordId;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getInterviewRecordId() { return interviewRecordId; }
    public void setInterviewRecordId(Long interviewRecordId) { this.interviewRecordId = interviewRecordId; }
} 
package com.example.interview.model;

public class ScoreResponse {
    private double score;
    private String feedback;

    public ScoreResponse() {
    }

    public ScoreResponse(double score, String feedback) {
        this.score = score;
        this.feedback = feedback;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
} 
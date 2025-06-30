package com.example.interview.model;

public class ScoreFeedback {
    private double score;
    private String feedback;

    public ScoreFeedback() {
    }

    public ScoreFeedback(double score, String feedback) {
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
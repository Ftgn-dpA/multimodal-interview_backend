package com.example.interview.model;

public class ProgressInfo {
    private int percent;
    private String stage;

    public ProgressInfo(int percent, String stage) {
        this.percent = percent;
        this.stage = stage;
    }
    public int getPercent() { return percent; }
    public String getStage() { return stage; }
    public void setPercent(int percent) { this.percent = percent; }
    public void setStage(String stage) { this.stage = stage; }
} 
package com.example.interview.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_reports")
public class InterviewReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_record_id")
    private InterviewRecord interviewRecord;

    // 总体评分
    private Double overallScore;
    private String overallFeedback;
    
    // 能力雷达图数据（JSON格式）
    @Lob
    private String skillRadarData; // 包含各项技能的评分数据
    
    // 关键问题定位
    @Lob
    private String keyIssues; // JSON格式存储关键问题
    
    // 改进建议
    @Lob
    private String improvementSuggestions; // JSON格式存储改进建议
    
    // 面试表现分析
    @Lob
    private String performanceAnalysis; // 详细的表现分析
    
    // 技术能力评估
    @Lob
    private String technicalAssessment; // 技术能力评估
    
    // 软技能评估
    @Lob
    private String softSkillAssessment; // 软技能评估
    
    // 报告生成时间
    private LocalDateTime generatedAt;
    
    // 报告文件路径
    private String reportFilePath; // 生成的PDF报告文件路径
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InterviewReport() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public String getOverallFeedback() {
        return overallFeedback;
    }

    public void setOverallFeedback(String overallFeedback) {
        this.overallFeedback = overallFeedback;
    }

    public String getSkillRadarData() {
        return skillRadarData;
    }

    public void setSkillRadarData(String skillRadarData) {
        this.skillRadarData = skillRadarData;
    }

    public String getKeyIssues() {
        return keyIssues;
    }

    public void setKeyIssues(String keyIssues) {
        this.keyIssues = keyIssues;
    }

    public String getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public void setImprovementSuggestions(String improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
    }

    public String getPerformanceAnalysis() {
        return performanceAnalysis;
    }

    public void setPerformanceAnalysis(String performanceAnalysis) {
        this.performanceAnalysis = performanceAnalysis;
    }

    public String getTechnicalAssessment() {
        return technicalAssessment;
    }

    public void setTechnicalAssessment(String technicalAssessment) {
        this.technicalAssessment = technicalAssessment;
    }

    public String getSoftSkillAssessment() {
        return softSkillAssessment;
    }

    public void setSoftSkillAssessment(String softSkillAssessment) {
        this.softSkillAssessment = softSkillAssessment;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getReportFilePath() {
        return reportFilePath;
    }

    public void setReportFilePath(String reportFilePath) {
        this.reportFilePath = reportFilePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
} 
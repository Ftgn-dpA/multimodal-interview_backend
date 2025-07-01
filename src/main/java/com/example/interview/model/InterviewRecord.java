package com.example.interview.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_records")
public class InterviewRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 面试基本信息
    private String interviewType; // 面试类型：AI_ENGINEER, DATA_ENGINEER, IOT_ENGINEER, SYSTEM_ENGINEER, PRODUCT_MANAGER
    private String position; // 具体岗位名称
    private String aiModel; // 使用的AI模型
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // 视频和音频文件路径
    private String videoFilePath; // 面试视频文件路径
    private String audioFilePath; // 音频文件路径
    
    // 面试状态
    private String status; // PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    
    // 评分和反馈
    private Double overallScore; // 总体评分
    private String overallFeedback; // 总体反馈
    
    // 能力评估（JSON格式存储）
    @Lob
    private String skillAssessment; // 技能评估JSON
    
    // 问题记录（JSON格式存储）
    @Lob
    private String questionAnswers; // 问答记录JSON
    
    // 改进建议（JSON格式存储）
    @Lob
    private String improvementSuggestions; // 改进建议JSON
    
    // 报告文件路径
    private String reportFilePath; // 生成的报告文件路径
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InterviewRecord() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public InterviewRecord(Long id, User user, String interviewType, String position, String aiModel, LocalDateTime startTime, LocalDateTime endTime, String videoFilePath, String audioFilePath, String status, Double overallScore, String overallFeedback, String skillAssessment, String questionAnswers, String improvementSuggestions, String reportFilePath) {
        this.id = id;
        this.user = user;
        this.interviewType = interviewType;
        this.position = position;
        this.aiModel = aiModel;
        this.startTime = startTime;
        this.endTime = endTime;
        this.videoFilePath = videoFilePath;
        this.audioFilePath = audioFilePath;
        this.status = status;
        this.overallScore = overallScore;
        this.overallFeedback = overallFeedback;
        this.skillAssessment = skillAssessment;
        this.questionAnswers = questionAnswers;
        this.improvementSuggestions = improvementSuggestions;
        this.reportFilePath = reportFilePath;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static InterviewRecordBuilder builder() {
        return new InterviewRecordBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getInterviewType() {
        return interviewType;
    }

    public void setInterviewType(String interviewType) {
        this.interviewType = interviewType;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getVideoFilePath() {
        return videoFilePath;
    }

    public void setVideoFilePath(String videoFilePath) {
        this.videoFilePath = videoFilePath;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getSkillAssessment() {
        return skillAssessment;
    }

    public void setSkillAssessment(String skillAssessment) {
        this.skillAssessment = skillAssessment;
    }

    public String getQuestionAnswers() {
        return questionAnswers;
    }

    public void setQuestionAnswers(String questionAnswers) {
        this.questionAnswers = questionAnswers;
    }

    public String getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public void setImprovementSuggestions(String improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
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

    public static class InterviewRecordBuilder {
        private Long id;
        private User user;
        private String interviewType;
        private String position;
        private String aiModel;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String videoFilePath;
        private String audioFilePath;
        private String status;
        private Double overallScore;
        private String overallFeedback;
        private String skillAssessment;
        private String questionAnswers;
        private String improvementSuggestions;
        private String reportFilePath;

        public InterviewRecordBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public InterviewRecordBuilder user(User user) {
            this.user = user;
            return this;
        }

        public InterviewRecordBuilder interviewType(String interviewType) {
            this.interviewType = interviewType;
            return this;
        }

        public InterviewRecordBuilder position(String position) {
            this.position = position;
            return this;
        }

        public InterviewRecordBuilder aiModel(String aiModel) {
            this.aiModel = aiModel;
            return this;
        }

        public InterviewRecordBuilder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public InterviewRecordBuilder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public InterviewRecordBuilder videoFilePath(String videoFilePath) {
            this.videoFilePath = videoFilePath;
            return this;
        }

        public InterviewRecordBuilder audioFilePath(String audioFilePath) {
            this.audioFilePath = audioFilePath;
            return this;
        }

        public InterviewRecordBuilder status(String status) {
            this.status = status;
            return this;
        }

        public InterviewRecordBuilder overallScore(Double overallScore) {
            this.overallScore = overallScore;
            return this;
        }

        public InterviewRecordBuilder overallFeedback(String overallFeedback) {
            this.overallFeedback = overallFeedback;
            return this;
        }

        public InterviewRecordBuilder skillAssessment(String skillAssessment) {
            this.skillAssessment = skillAssessment;
            return this;
        }

        public InterviewRecordBuilder questionAnswers(String questionAnswers) {
            this.questionAnswers = questionAnswers;
            return this;
        }

        public InterviewRecordBuilder improvementSuggestions(String improvementSuggestions) {
            this.improvementSuggestions = improvementSuggestions;
            return this;
        }

        public InterviewRecordBuilder reportFilePath(String reportFilePath) {
            this.reportFilePath = reportFilePath;
            return this;
        }

        public InterviewRecord build() {
            return new InterviewRecord(id, user, interviewType, position, aiModel, startTime, endTime, videoFilePath, audioFilePath, status, overallScore, overallFeedback, skillAssessment, questionAnswers, improvementSuggestions, reportFilePath);
        }
    }
} 
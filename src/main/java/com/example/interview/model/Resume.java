package com.example.interview.model;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String filename;

    private String originalName;

    private Timestamp uploadTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public Timestamp getUploadTime() { return uploadTime; }
    public void setUploadTime(Timestamp uploadTime) { this.uploadTime = uploadTime; }
} 
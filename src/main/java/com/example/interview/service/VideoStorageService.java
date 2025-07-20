package com.example.interview.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.example.interview.config.VideoStorageConfig;
import com.example.interview.model.InterviewRecord;
import com.example.interview.repository.InterviewRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class VideoStorageService {

    private static final Logger logger = LoggerFactory.getLogger(VideoStorageService.class);

    @Value("${video.storage.path}")
    private String storagePath;

    @Value("${video.storage.max-size}")
    private String maxSize;

    @Autowired
    private VideoStorageConfig videoStorageConfig;
    @Autowired
    private InterviewRecordRepository interviewRecordRepository;

    /**
     * Save video file
     * @param videoFile Video file
     * @param recordId Interview record ID
     * @return Saved file path
     */
    public String saveVideo(MultipartFile videoFile, Long recordId) throws IOException {
        // Check if file is empty
        if (videoFile.isEmpty()) {
            logger.info("[saveVideo] Uploaded file is empty");
            throw new IllegalArgumentException("Video file cannot be empty");
        }

        // Check file size
        long fileSize = videoFile.getSize();
        long maxSizeBytes = parseSize(maxSize);
        logger.info("[saveVideo] File size: {} bytes, max allowed: {} bytes", fileSize, maxSizeBytes);
        if (fileSize > maxSizeBytes) {
            logger.info("[saveVideo] File too large");
            throw new IllegalArgumentException("Video file size exceeds limit: " + maxSize);
        }

        // Check file type
        String contentType = videoFile.getContentType();
        logger.info("[saveVideo] File type: {}", contentType);
        if (contentType == null || !contentType.startsWith("video/")) {
            logger.info("[saveVideo] Invalid file type");
            throw new IllegalArgumentException("File type must be video format");
        }

        // Create storage directory
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
            logger.info("[saveVideo] Created storage directory: {}", storageDir.toString());
        } else {
            logger.info("[saveVideo] Storage directory already exists: {}", storageDir.toString());
        }

        // 生成文件名
        String originalFilename = videoFile.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String filename = String.format("%d_%s_%s%s", recordId, timestamp, uuid, extension);

        // Save file
        Path filePath = storageDir.resolve(filename);
        logger.info("[saveVideo] Saving video to: {}", filePath.toString());
        try {
            Files.copy(videoFile.getInputStream(), filePath);
            logger.info("[saveVideo] File saved successfully");
        } catch (Exception e) {
            logger.error("[saveVideo] File save exception", e);
            throw e;
        }

        // Return relative path
        return "/videos/" + filename;
    }

    /**
     * Get full path of video file
     * @param relativePath Relative path
     * @return Full file path
     */
    public String getVideoFilePath(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }
        
        // Remove leading /videos/
        String filename = relativePath.replace("/videos/", "");
        return Paths.get(storagePath, filename).toString();
    }

    /**
     * Check if video file exists
     * @param relativePath Relative path
     * @return Whether exists
     */
    public boolean videoExists(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return false;
        }
        
        String fullPath = getVideoFilePath(relativePath);
        return Files.exists(Paths.get(fullPath));
    }

    /**
     * Delete video file
     * @param relativePath Relative path
     * @return Whether deletion successful
     */
    public boolean deleteVideo(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return false;
        }
        
        try {
            String fullPath = getVideoFilePath(relativePath);
            Path path = Paths.get(fullPath);
            if (Files.exists(path)) {
                Files.delete(path);
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".mp4"; // Default extension
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Parse file size string
     */
    private long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return 100 * 1024 * 1024; // Default 100MB
        }
        
        sizeStr = sizeStr.toUpperCase();
        if (sizeStr.endsWith("MB")) {
            return Long.parseLong(sizeStr.replace("MB", "")) * 1024 * 1024;
        } else if (sizeStr.endsWith("GB")) {
            return Long.parseLong(sizeStr.replace("GB", "")) * 1024 * 1024 * 1024;
        } else {
            return Long.parseLong(sizeStr);
        }
    }

    public String getStoragePath() {
        return videoStorageConfig.getStorage().getPath();
    }
    public String getAccessUrlPrefix() {
        return videoStorageConfig.getAccessUrlPrefix();
    }
    public boolean updateInterviewRecordVideoFilePath(Long recordId, String videoUrl) {
        InterviewRecord record = interviewRecordRepository.findById(recordId).orElse(null);
        if (record != null) {
            record.setVideoFilePath(videoUrl);
            interviewRecordRepository.save(record);
            return true;
        }
        return false;
    }
} 
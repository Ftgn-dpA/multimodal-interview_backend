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
     * 保存视频文件
     * @param videoFile 视频文件
     * @param recordId 面试记录ID
     * @return 保存后的文件路径
     */
    public String saveVideo(MultipartFile videoFile, Long recordId) throws IOException {
        // 检查文件是否为空
        if (videoFile.isEmpty()) {
            logger.info("[saveVideo] 上传文件为空");
            throw new IllegalArgumentException("视频文件不能为空");
        }

        // 检查文件大小
        long fileSize = videoFile.getSize();
        long maxSizeBytes = parseSize(maxSize);
        logger.info("[saveVideo] 文件大小: {} 字节，最大允许: {} 字节", fileSize, maxSizeBytes);
        if (fileSize > maxSizeBytes) {
            logger.info("[saveVideo] 文件过大");
            throw new IllegalArgumentException("视频文件大小超过限制: " + maxSize);
        }

        // 检查文件类型
        String contentType = videoFile.getContentType();
        logger.info("[saveVideo] 文件类型: {}", contentType);
        if (contentType == null || !contentType.startsWith("video/")) {
            logger.info("[saveVideo] 文件类型非法");
            throw new IllegalArgumentException("文件类型必须是视频格式");
        }

        // 创建存储目录
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
            logger.info("[saveVideo] 创建存储目录: {}", storageDir.toString());
        } else {
            logger.info("[saveVideo] 存储目录已存在: {}", storageDir.toString());
        }

        // 生成文件名
        String originalFilename = videoFile.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String filename = String.format("%d_%s_%s%s", recordId, timestamp, uuid, extension);

        // 保存文件
        Path filePath = storageDir.resolve(filename);
        logger.info("[saveVideo] 保存视频到: {}", filePath.toString());
        try {
            Files.copy(videoFile.getInputStream(), filePath);
            logger.info("[saveVideo] 文件保存成功");
        } catch (Exception e) {
            logger.error("[saveVideo] 文件保存异常", e);
            throw e;
        }

        // 返回相对路径
        return "/videos/" + filename;
    }

    /**
     * 获取视频文件的完整路径
     * @param relativePath 相对路径
     * @return 完整文件路径
     */
    public String getVideoFilePath(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }
        
        // 移除开头的 /videos/
        String filename = relativePath.replace("/videos/", "");
        return Paths.get(storagePath, filename).toString();
    }

    /**
     * 检查视频文件是否存在
     * @param relativePath 相对路径
     * @return 是否存在
     */
    public boolean videoExists(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return false;
        }
        
        String fullPath = getVideoFilePath(relativePath);
        return Files.exists(Paths.get(fullPath));
    }

    /**
     * 删除视频文件
     * @param relativePath 相对路径
     * @return 是否删除成功
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
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".mp4"; // 默认扩展名
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 解析文件大小字符串
     */
    private long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return 100 * 1024 * 1024; // 默认100MB
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
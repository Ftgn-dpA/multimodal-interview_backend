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

@Service
public class VideoStorageService {

    @Value("${video.storage.path}")
    private String storagePath;

    @Value("${video.storage.max-size}")
    private String maxSize;

    /**
     * 保存视频文件
     * @param videoFile 视频文件
     * @param recordId 面试记录ID
     * @return 保存后的文件路径
     */
    public String saveVideo(MultipartFile videoFile, Long recordId) throws IOException {
        // 检查文件是否为空
        if (videoFile.isEmpty()) {
            throw new IllegalArgumentException("视频文件不能为空");
        }

        // 检查文件大小
        long fileSize = videoFile.getSize();
        long maxSizeBytes = parseSize(maxSize);
        if (fileSize > maxSizeBytes) {
            throw new IllegalArgumentException("视频文件大小超过限制: " + maxSize);
        }

        // 检查文件类型
        String contentType = videoFile.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("文件类型必须是视频格式");
        }

        // 创建存储目录
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        // 生成文件名：recordId_时间戳_UUID.扩展名
        String originalFilename = videoFile.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String filename = String.format("%d_%s_%s%s", recordId, timestamp, uuid, extension);

        // 保存文件
        Path filePath = storageDir.resolve(filename);
        Files.copy(videoFile.getInputStream(), filePath);

        // 返回相对路径（用于数据库存储）
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
} 
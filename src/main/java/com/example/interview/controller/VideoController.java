package com.example.interview.controller;

import com.example.interview.service.VideoStorageService;
import com.example.interview.config.VideoStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    private VideoStorageService videoStorageService;

    @Autowired
    private com.example.interview.config.VideoStorageConfig videoStorageConfig;

    /**
     * 获取视频文件
     * @param filename 文件名
     * @return 视频文件流
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getVideo(@PathVariable String filename) {
        try {
            // Build file path
            String relativePath = "/videos/" + filename;
            String fullPath = videoStorageService.getVideoFilePath(relativePath);
            
            if (fullPath == null) {
                return ResponseEntity.notFound().build();
            }
            
            File file = new File(fullPath);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("video/mp4"));
            headers.setContentLength(file.length());
            headers.set("Accept-Ranges", "bytes");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 检查视频文件是否存在
     * @param filename 文件名
     * @return 是否存在
     */
    @GetMapping("/exists/{filename:.+}")
    public ResponseEntity<?> checkVideoExists(@PathVariable String filename) {
        try {
            String relativePath = "/videos/" + filename;
            boolean exists = videoStorageService.videoExists(relativePath);
            
            return ResponseEntity.ok().body(
                java.util.Map.of("exists", exists)
            );
        } catch (Exception e) {
            return ResponseEntity.ok().body(
                java.util.Map.of("exists", false)
            );
        }
    }

    @PostMapping("/upload-video/{recordId}")
    public ResponseEntity<?> uploadVideo(@PathVariable Long recordId, @RequestParam("video") MultipartFile videoFile) {
        try {
            // Get storage path
            String storagePath = videoStorageService.getStoragePath();
            String urlPrefix = videoStorageService.getAccessUrlPrefix();
            // Generate unique filename
            String filename = "record_" + recordId + "_" + System.currentTimeMillis() + ".mp4";
            File dest = new File(storagePath, filename);
            videoFile.transferTo(dest);
            // Update database
            boolean updated = videoStorageService.updateInterviewRecordVideoFilePath(recordId, urlPrefix + filename);
            if (updated) {
                return ResponseEntity.ok(Map.of("videoUrl", urlPrefix + filename));
            } else {
                return ResponseEntity.status(500).body("视频上传失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("视频上传失败");
        }
    }
} 
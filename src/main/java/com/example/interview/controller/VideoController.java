package com.example.interview.controller;

import com.example.interview.service.VideoStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    private VideoStorageService videoStorageService;

    /**
     * 获取视频文件
     * @param filename 文件名
     * @return 视频文件流
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getVideo(@PathVariable String filename) {
        try {
            // 构建文件路径
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
            
            // 设置响应头
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
} 
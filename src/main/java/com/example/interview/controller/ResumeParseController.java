package com.example.interview.controller;

import com.example.interview.service.ResumeParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
public class ResumeParseController {
    @Autowired
    private ResumeParseService resumeParseService;

    @PostMapping("/parse")
    public String parseResume(@RequestParam("file") MultipartFile file) {
        try {
            String result=resumeParseService.parseResume(file);
            // 简历解析完成
            return resumeParseService.parseResume(file);
        } catch (Exception e) {
            return "解析失败: " + e.getMessage();
        }
    }
} 
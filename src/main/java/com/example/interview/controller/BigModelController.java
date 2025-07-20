package com.example.interview.controller;

import com.example.interview.service.BigModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bigmodel")
public class BigModelController {
    @Autowired
    private BigModelService bigModelService;

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestParam String question) {
        try {
            String answer = bigModelService.askOnce(question);
            // 大模型回答已生成
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("大模型调用失败: " + e.getMessage());
        }
    }
}
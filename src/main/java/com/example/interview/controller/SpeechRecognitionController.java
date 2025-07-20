package com.example.interview.controller;

import com.example.interview.service.Mp3SpeechRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/speech")
public class SpeechRecognitionController {
    @Autowired
    private Mp3SpeechRecognitionService recognitionService;

    @PostMapping("/recognize")
    public ResponseEntity<?> recognize(@RequestParam String audioPath) {
        try {
            String result = recognitionService.recognizeAudio(audioPath);
            // 语音识别完成
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("识别失败: " + e.getMessage());
        }
    }
} 
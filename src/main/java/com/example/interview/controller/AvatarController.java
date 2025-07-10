package com.example.interview.controller;

import com.example.interview.service.AvatarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/avatar")
public class AvatarController {
    @Autowired
    private AvatarService avatarService;

    @PostMapping("/start")
    public Map<String, Object> startAvatar() {
        try {
            return avatarService.startSession();
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new java.util.HashMap<>();
            err.put("status", "fail");
            err.put("msg", "avatar启动失败: " + e.getMessage());
            return err;
        }
    }

    @PostMapping("/send")
    public String sendText(@RequestParam String sessionId, @RequestParam String text) {
        return avatarService.sendText(sessionId, text);
    }

    @PostMapping("/stop")
    public String stopAvatar(@RequestParam String sessionId) {
        return avatarService.stopSession(sessionId);
    }
} 
package com.example.interview.controller;

import com.example.interview.service.AvatarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/avatar")
@CrossOrigin(origins = "*")
public class AvatarController {
    @Autowired
    private AvatarService avatarService;

    @PostMapping(value = "/start", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> startAvatar() {
        try {
            Map<String, Object> result = avatarService.startSession();
            System.out.println("[AvatarController] startAvatar result: " + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new java.util.HashMap<>();
            err.put("status", "fail");
            err.put("msg", "avatar启动失败: " + e.getMessage());
            err.put("sessionId", null);
            System.out.println("[AvatarController] startAvatar error result: " + err);
            return err;
        }
    }

    @PostMapping(value = "/send", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> sendText(@RequestParam String sessionId, @RequestParam String text) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            String msg = avatarService.sendText(sessionId, text);
            result.put("status", "ok");
            result.put("msg", msg);
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", "发送文本失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping(value = "/stop", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> stopAvatar(@RequestParam String sessionId) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            String msg = avatarService.stopSession(sessionId);
            result.put("status", "ok");
            result.put("msg", msg);
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", "avatar关闭失败: " + e.getMessage());
        }
        return result;
    }
} 
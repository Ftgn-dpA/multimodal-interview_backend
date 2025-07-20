package com.example.interview.controller;

import com.example.interview.service.AvatarService;
import com.example.interview.service.AiResponseService;
import com.example.interview.util.JwtUtil;
import com.example.interview.model.User;
import com.example.interview.repository.UserRepository;
import com.example.interview.repository.InterviewRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/avatar")
@CrossOrigin(origins = "*")
public class AvatarController {
    @Autowired
    private AvatarService avatarService;
    
    @Autowired
    private AiResponseService aiResponseService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private InterviewRecordRepository interviewRecordRepository;
    
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping(value = "/start", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> startAvatar() {
        try {
            Map<String, Object> result = avatarService.startSession();
            // 虚拟人启动完成
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new java.util.HashMap<>();
            err.put("status", "fail");
            err.put("msg", "avatar启动失败: " + e.getMessage());
            err.put("session", null);
            // 虚拟人启动失败
            return err;
        }
    }

    @PostMapping(value = "/send", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> sendInteractText(
            @RequestParam String sessionId, 
            @RequestParam String text,
            @RequestParam(required = false) Long interviewRecordId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            String msg = avatarService.sendInteractText(sessionId, text);
            result.put("status", "ok");
            result.put("msg", msg);
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", "发送消息失败: " + e.getMessage());
            System.err.println("[AvatarController] 发送消息失败: " + e.getMessage());
            e.printStackTrace();
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

    @GetMapping(value = "/stop", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> stopAvatarGet(@RequestParam String sessionId) {
        // GET 请求的 stop 接口，用于页面刷新时的 session 清理
        return stopAvatar(sessionId);
    }

    @PostMapping(value = "/audio-interact", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> audioInteract(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(required = false) Long interviewRecordId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 调用AvatarService进行音频交互
        Map<String, Object> result = avatarService.audioInteract(sessionId, audioFile);
        

        
        return result;
    }
} 
package com.example.interview.controller;

import com.example.interview.model.AiResponse;
import com.example.interview.service.AiResponseService;
import com.example.interview.util.JwtUtil;
import com.example.interview.model.User;
import com.example.interview.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-responses")
public class AiResponseController {
    
    private static final Logger logger = LoggerFactory.getLogger(AiResponseController.class);
    
    @Autowired
    private AiResponseService aiResponseService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 获取所有AI回复列表
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllAiResponses(@RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            
            List<AiResponse> responses = aiResponseService.getAllAiResponses();
            logger.info("用户 {} 获取所有AI回复，共 {} 条", username, responses.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", responses,
                "total", responses.size()
            ));
        } catch (Exception e) {
            logger.error("获取所有AI回复失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "获取AI回复失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取所有AI回复列表（无需认证，用于调试）
     */
    @GetMapping("/debug/all")
    public ResponseEntity<?> getAllAiResponsesDebug() {
        try {
            List<AiResponse> responses = aiResponseService.getAllAiResponses();
            logger.info("调试：获取所有AI回复，共 {} 条", responses.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", responses,
                "total", responses.size()
            ));
        } catch (Exception e) {
            logger.error("调试：获取所有AI回复失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "获取AI回复失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 根据面试记录ID获取AI回复
     */
    @GetMapping("/interview/{recordId}")
    public ResponseEntity<?> getAiResponsesByInterviewRecord(
            @PathVariable Long recordId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            
            List<AiResponse> responses = aiResponseService.getAiResponsesByInterviewRecord(recordId);
            logger.info("用户 {} 获取面试记录 {} 的AI回复，共 {} 条", username, recordId, responses.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", responses,
                "total", responses.size(),
                "interviewRecordId", recordId
            ));
        } catch (Exception e) {
            logger.error("获取面试记录AI回复失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "获取AI回复失败: " + e.getMessage()
            ));
        }
    }
    

    
    /**
     * 获取当前用户的所有AI回复
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyAiResponses(@RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            
            List<AiResponse> responses = aiResponseService.getAiResponsesByUserId(user.getId());
            logger.info("用户 {} 获取自己的AI回复，共 {} 条", username, responses.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", responses,
                "total", responses.size(),
                "userId", user.getId()
            ));
        } catch (Exception e) {
            logger.error("获取用户AI回复失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "获取AI回复失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 统计某个面试记录的AI回复数量
     */
    @GetMapping("/count/{recordId}")
    public ResponseEntity<?> countAiResponsesByInterviewRecord(
            @PathVariable Long recordId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            
            Long count = aiResponseService.countAiResponsesByInterviewRecord(recordId);
            logger.info("用户 {} 统计面试记录 {} 的AI回复数量: {}", username, recordId, count);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", count,
                "interviewRecordId", recordId
            ));
        } catch (Exception e) {
            logger.error("统计AI回复数量失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "统计失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 简单测试接口：使用面试记录ID的保存测试
     */
    @PostMapping("/simple-test-save")
    public ResponseEntity<?> simpleTestSaveAiResponse(
            @RequestParam Long interviewRecordId,
            @RequestParam String aiResponse) {
        try {
            logger.info("简单测试保存AI回复: recordId={}", interviewRecordId);
            
            AiResponse savedResponse = aiResponseService.testSaveAiResponse(
                interviewRecordId, aiResponse
            );
            
            logger.info("简单测试保存成功: ID={}", savedResponse.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "AI回复保存成功",
                "savedId", savedResponse.getId(),
                "interviewRecordId", interviewRecordId
            ));
        } catch (Exception e) {
            logger.error("简单测试保存AI回复失败: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "保存失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 查看缓存状态
     */
    @GetMapping("/cache-status")
    public ResponseEntity<?> getCacheStatus() {
        try {
            Map<String, Integer> cacheStatus = aiResponseService.getAllCacheStatus();
            logger.info("获取缓存状态: {}", cacheStatus);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "cacheStatus", cacheStatus,
                "totalCachedSessions", cacheStatus.size()
            ));
        } catch (Exception e) {
            logger.error("获取缓存状态失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "获取缓存状态失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 手动批量保存指定sessionId的AI回复到指定面试记录
     */
    @PostMapping("/batch-save/{sessionId}/{recordId}")
    public ResponseEntity<?> batchSaveAiResponses(@PathVariable String sessionId, @PathVariable Long recordId) {
        try {
            int cachedCount = aiResponseService.getCachedResponseCount(sessionId);
            if (cachedCount == 0) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "没有缓存的AI回复需要保存",
                    "sessionId", sessionId,
                    "recordId", recordId
                ));
            }
            
            logger.info("手动批量保存AI回复: sessionId={}, recordId={}, 缓存数量={}", sessionId, recordId, cachedCount);
            aiResponseService.batchSaveAiResponses(sessionId, recordId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "AI回复批量保存成功",
                "sessionId", sessionId,
                "recordId", recordId,
                "savedCount", cachedCount
            ));
        } catch (Exception e) {
            logger.error("手动批量保存AI回复失败: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "批量保存失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 清理过期的缓存
     */
    @PostMapping("/cleanup-cache")
    public ResponseEntity<?> cleanupExpiredCache() {
        try {
            Map<String, Integer> beforeStatus = aiResponseService.getAllCacheStatus();
            int beforeCount = beforeStatus.size();
            
            aiResponseService.cleanupExpiredCache();
            
            logger.info("清理过期缓存完成，清理前: {} 个session", beforeCount);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "缓存清理完成",
                "cleanedCount", beforeCount
            ));
        } catch (Exception e) {
            logger.error("清理缓存失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "清理缓存失败: " + e.getMessage()
            ));
        }
    }
} 
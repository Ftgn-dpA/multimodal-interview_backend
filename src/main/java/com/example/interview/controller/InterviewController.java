package com.example.interview.controller;

import com.example.interview.model.AnswerRequest;
import com.example.interview.model.InterviewRecord;
import com.example.interview.model.InterviewReport;
import com.example.interview.model.QuestionResponse;
import com.example.interview.model.ScoreFeedback;
import com.example.interview.model.ScoreResponse;
import com.example.interview.model.User;
import com.example.interview.repository.InterviewRecordRepository;
import com.example.interview.repository.InterviewReportRepository;
import com.example.interview.repository.UserRepository;
import com.example.interview.service.LargeModelService;
import com.example.interview.service.VideoStorageService;
import com.example.interview.service.AiResponseService;
import com.example.interview.service.InterviewService;
import com.example.interview.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import javax.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {
    private static final Logger logger = LoggerFactory.getLogger(InterviewController.class);
    private final LargeModelService largeModelService;
    private final InterviewRecordRepository interviewRecordRepository;
    private final InterviewReportRepository interviewReportRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final VideoStorageService videoStorageService;
    private final AiResponseService aiResponseService;
    private final InterviewService interviewService;

    public InterviewController(LargeModelService largeModelService, 
                             InterviewRecordRepository interviewRecordRepository,
                             InterviewReportRepository interviewReportRepository,
                             UserRepository userRepository, 
                             JwtUtil jwtUtil,
                            VideoStorageService videoStorageService,
                            AiResponseService aiResponseService,
                            InterviewService interviewService) {
        this.largeModelService = largeModelService;
        this.interviewRecordRepository = interviewRecordRepository;
        this.interviewReportRepository = interviewReportRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.videoStorageService = videoStorageService;
        this.aiResponseService = aiResponseService;
        this.interviewService = interviewService;
    }

    @GetMapping("/types")
    public ResponseEntity<?> getInterviewTypes() {
        // 返回新的面试岗位类型列表
        List<Map<String, Object>> types = List.of(
            Map.of(
                "category", "人工智能",
                "icon", "robot",
                "color", "#3b82f6",
                "positions", List.of(
                    Map.of(
                        "type", "AI_ENGINEER",
                        "title", "AI工程师",
                        "description", "专注于机器学习、深度学习、自然语言处理等技术",
                        "skills", List.of("机器学习", "深度学习", "Python", "TensorFlow", "自然语言处理"),
                        "aiModel", "星火V4.0",
                        "difficulty", "高级"
                    ),
                    Map.of(
                        "type", "AI_RESEARCHER",
                        "title", "AI研究员",
                        "description", "专注于前沿AI算法研究和创新",
                        "skills", List.of("算法研究", "论文阅读", "数学基础", "创新思维", "实验设计"),
                        "aiModel", "星火V4.0",
                        "difficulty", "专家级"
                    )
                )
            ),
            Map.of(
                "category", "数据科学",
                "icon", "database",
                "color", "#10b981",
                "positions", List.of(
                    Map.of(
                        "type", "DATA_ENGINEER",
                        "title", "数据工程师",
                        "description", "负责数据管道构建、ETL流程设计和数据仓库管理",
                        "skills", List.of("SQL", "Python", "Spark", "Hadoop", "数据建模"),
                        "aiModel", "星火V4.0",
                        "difficulty", "中级"
                    )
                )
            ),
            Map.of(
                "category", "物联网",
                "icon", "wifi",
                "color", "#f59e0b",
                "positions", List.of(
                    Map.of(
                        "type", "IOT_ENGINEER",
                        "title", "物联网工程师",
                        "description", "负责IoT设备开发、传感器数据处理和边缘计算",
                        "skills", List.of("嵌入式开发", "传感器技术", "MQTT", "边缘计算", "硬件集成"),
                        "aiModel", "星火V4.0",
                        "difficulty", "中级"
                    )
                )
            ),
            Map.of(
                "category", "系统架构",
                "icon", "server",
                "color", "#8b5cf6",
                "positions", List.of(
                    Map.of(
                        "type", "SYSTEM_ENGINEER",
                        "title", "系统工程师",
                        "description", "负责系统架构设计、性能优化和运维自动化",
                        "skills", List.of("系统设计", "微服务", "Docker", "Kubernetes", "监控运维"),
                        "aiModel", "星火V4.0",
                        "difficulty", "高级"
                    )
                )
            ),
            Map.of(
                "category", "产品管理",
                "icon", "briefcase",
                "color", "#ef4444",
                "positions", List.of(
                    Map.of(
                        "type", "PRODUCT_MANAGER",
                        "title", "产品经理",
                        "description", "负责产品规划、需求分析和团队协作",
                        "skills", List.of("产品规划", "需求分析", "用户研究", "项目管理", "数据分析"),
                        "aiModel", "星火V4.0",
                        "difficulty", "中级"
                    )
                )
            )
        );
        return ResponseEntity.ok(types);
    }

    @GetMapping("/info/{type}")
    public ResponseEntity<?> getInterviewInfo(@PathVariable String type) {
        try {
            String position = getPositionByType(type);
            String aiModel = getAiModelByType(type);
            String question = largeModelService.generateQuestion(type);
            return ResponseEntity.ok(Map.of(
                "question", question,
                "position", position,
                "aiModel", aiModel
            ));
        } catch (Exception e) {
            logger.info("Failed to get interview info: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get interview info: " + e.getMessage()));
        }
    }

    @PostMapping("/start/{type}")
    public ResponseEntity<?> startInterview(@PathVariable String type,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            String position = getPositionByType(type);
            String aiModel = getAiModelByType(type);
            // 创建面试记录
            InterviewRecord record = InterviewRecord.builder()
                    .user(user)
                    .interviewType(type)
                    .position(position)
                    .aiModel(aiModel)
                    .build();
            InterviewRecord savedRecord = interviewRecordRepository.save(record);
            String question = largeModelService.generateQuestion(type);
            return ResponseEntity.ok(Map.of(
                "recordId", savedRecord.getId(),
                "question", question,
                "position", position,
                "aiModel", aiModel
            ));
        } catch (Exception e) {
            logger.info("Failed to start interview: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to start interview: " + e.getMessage()));
        }
    }

    @PostMapping("/end/{recordId}")
    public ResponseEntity<?> endInterview(@PathVariable Long recordId,
                                        @RequestParam(required = false) Integer actualDuration,
                                        @RequestParam(required = false) String sessionId,
                                        @RequestParam(required = false) Long resumeId,
                                        @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("=== Ending interview ===");
            logger.info("recordId: {}", recordId);
            logger.info("actualDuration: {}", actualDuration);
            logger.info("resumeId: {}", resumeId);
            
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            InterviewRecord record = interviewRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("Interview record not found"));
            if (!record.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "No permission to access this interview record"));
            }
            
            // Save actual interview duration
            if (actualDuration != null && actualDuration > 0) {
                logger.info("Setting actualDuration: {}", actualDuration);
                record.setActualDuration(actualDuration);
            } else {
                logger.info("actualDuration is null or invalid: {}", actualDuration);
            }
            
            // Batch save all cached AI responses (consistent with history record generation timing)
            // Note: Only normal interview endings will execute here, direct exits won't save AI responses
            try {
                if (sessionId != null && !sessionId.isEmpty()) {
                    logger.info("Normal interview ending, starting batch save of all responses, sessionId: {}", sessionId);
                    aiResponseService.batchSaveAllResponses(sessionId, recordId);
                    logger.info("All responses batch save completed");
                } else {
                    logger.info("Normal interview ending, sessionId is empty, skipping response save");
                }
            } catch (Exception e) {
                logger.error("Failed to batch save all responses: {}", e.getMessage());
                // 保存失败只记录日志
            }
            
            // Save interview record (without analysis results, waiting for subsequent analysis)
            InterviewRecord savedRecord = interviewRecordRepository.save(record);
            
            // Return basic info, frontend can call analysis endpoint
            return ResponseEntity.ok(Map.of(
                "message", "Interview ended, can start analysis",
                "recordId", savedRecord.getId(),
                "canAnalyze", savedRecord.getVideoFilePath() != null,
                "analysisEndpoint", "/api/interview/analyze/" + savedRecord.getId()
            ));
        } catch (Exception e) {
            logger.info("Failed to end interview: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to end interview: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-video/{recordId}")
    public ResponseEntity<?> uploadVideo(@PathVariable Long recordId,
                                       @RequestParam("video") MultipartFile videoFile,
                                       @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            
            InterviewRecord record = interviewRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("Interview record not found"));
            
            if (!record.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "No permission to access this interview record"));
            }
            
            // 使用VideoStorageService保存视频文件
            String videoPath = videoStorageService.saveVideo(videoFile, recordId);
            record.setVideoFilePath(videoPath);
            
            interviewRecordRepository.save(record);
            
            return ResponseEntity.ok(Map.of(
                "message", "Video upload successful", 
                "videoPath", videoPath,
                "fullUrl", "http://localhost:8080/api/video" + videoPath.replace("/videos/", "/")
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Video upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            List<InterviewRecord> records = interviewRecordRepository.findByUserOrderByCreatedAtDesc(user);
            // 只返回所有面试记录（或如需只保留COMPLETED，可直接返回records）
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get history records: " + e.getMessage()));
        }
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<?> getRecordDetail(@PathVariable Long recordId,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("=== Getting interview record details ===");
            logger.info("recordId: {}", recordId);
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            logger.info("username: {}", username);
            User user = userRepository.findByUsername(username).orElseThrow();
            InterviewRecord record = interviewRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("Interview record not found"));
            logger.info("Found record: {}, user: {}", record.getId(), record.getUser().getUsername());
            if (!record.getUser().getId().equals(user.getId())) {
                logger.info("Permission verification failed");
                return ResponseEntity.badRequest().body(Map.of("error", "No permission to access this interview record"));
            }
            logger.info("Returning record details");
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            logger.info("Failed to get record details: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get record details: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{recordId}")
    public ResponseEntity<?> deleteInterviewRecord(@PathVariable Long recordId,
                                                   @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();

            InterviewRecord record = interviewRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("Interview record not found"));

            if (!record.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "No permission to access this interview record"));
            }

            // 删除视频文件
            if (record.getVideoFilePath() != null) {
                videoStorageService.deleteVideo(record.getVideoFilePath());
            }
            // 删除面试报告
            interviewReportRepository.findByInterviewRecord(record)
                .ifPresent(interviewReportRepository::delete);
            // 删除面试记录
            interviewRecordRepository.delete(record);

            return ResponseEntity.ok(Map.of("message", "Delete successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Delete failed: " + e.getMessage()));
        }
    }

    @GetMapping("/prompt")
    public ResponseEntity<String> getPrompt(@RequestParam String scene, @RequestParam String position) {
        String prompt = interviewService.getPrompt(scene, position);
        return ResponseEntity.ok(prompt);
    }

    // 辅助方法
    private String getPositionByType(String type) {
        switch (type) {
            case "AI_ENGINEER": return "AI工程师";
            case "AI_RESEARCHER": return "AI研究员";
            case "DATA_ENGINEER": return "数据工程师";
            case "DATA_SCIENTIST": return "数据科学家";
            case "IOT_ENGINEER": return "物联网工程师";
            case "IOT_ARCHITECT": return "IoT架构师";
            case "SYSTEM_ENGINEER": return "系统工程师";
            case "DEVOPS_ENGINEER": return "DevOps工程师";
            case "PRODUCT_MANAGER": return "产品经理";
            case "TECHNICAL_PRODUCT_MANAGER": return "技术产品经理";
            default: return "未知岗位";
        }
    }

    private String getAiModelByType(String type) {
        // 根据岗位类型返回对应的AI模型（占位）
        if (type.startsWith("AI_") || type.equals("DATA_SCIENTIST") || type.equals("IOT_ARCHITECT") || 
            type.equals("SYSTEM_ENGINEER") || type.equals("TECHNICAL_PRODUCT_MANAGER")) {
            return "星火V4.0";
        } else {
            return "星火V4.0";
        }
    }
} 
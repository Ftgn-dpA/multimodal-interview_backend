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

    public InterviewController(LargeModelService largeModelService, 
                             InterviewRecordRepository interviewRecordRepository,
                             InterviewReportRepository interviewReportRepository,
                             UserRepository userRepository, 
                             JwtUtil jwtUtil,
                             VideoStorageService videoStorageService) {
        this.largeModelService = largeModelService;
        this.interviewRecordRepository = interviewRecordRepository;
        this.interviewReportRepository = interviewReportRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.videoStorageService = videoStorageService;
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
            logger.info("获取面试信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "获取面试信息失败: " + e.getMessage()));
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
            logger.info("启动面试失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "启动面试失败: " + e.getMessage()));
        }
    }

    @PostMapping("/end/{recordId}")
    public ResponseEntity<?> endInterview(@PathVariable Long recordId,
                                        @RequestParam(required = false) Integer actualDuration,
                                        @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("=== 结束面试 ===");
            logger.info("recordId: {}", recordId);
            logger.info("actualDuration: {}", actualDuration);
            
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            InterviewRecord record = interviewRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("面试记录不存在"));
            if (!record.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "无权访问此面试记录"));
            }
            // 保存实际面试时长
            if (actualDuration != null && actualDuration > 0) {
                logger.info("设置actualDuration: {}", actualDuration);
                record.setActualDuration(actualDuration);
            } else {
                logger.info("actualDuration为空或无效: {}", actualDuration);
            }
            // 生成AI评测报告（占位）
            record.setOverallScore(85.0);
            record.setOverallFeedback("整体表现良好，技术基础扎实，沟通能力有待提升");
            record.setSkillAssessment("{\"技术能力\":85,\"沟通能力\":70,\"问题解决\":80,\"学习能力\":90}");
            record.setImprovementSuggestions("{\"建议\":[\"加强STAR结构回答\",\"提升眼神交流\",\"增加具体案例\"]}");
            InterviewRecord savedRecord = interviewRecordRepository.save(record);
            // 创建面试报告
            InterviewReport report = new InterviewReport();
            report.setInterviewRecord(savedRecord);
            report.setOverallScore(85.0);
            report.setOverallFeedback("整体表现良好，技术基础扎实，沟通能力有待提升");
            report.setSkillRadarData("{\"技术能力\":85,\"沟通能力\":70,\"问题解决\":80,\"学习能力\":90,\"团队协作\":75,\"创新思维\":80}");
            report.setKeyIssues("{\"问题\":[\"回答结构不够清晰\",\"缺乏具体案例支撑\",\"技术深度有待提升\"]}");
            report.setImprovementSuggestions("{\"建议\":[\"加强STAR结构回答\",\"提升眼神交流\",\"增加具体案例\",\"深入学习相关技术\"]}");
            report.setPerformanceAnalysis("面试者在技术基础方面表现良好，能够回答大部分技术问题。但在表达和案例分享方面还有提升空间。建议加强结构化思维训练。");
            report.setTechnicalAssessment("技术基础扎实，对核心概念理解到位。在高级技术问题上需要更深入的学习和实践。");
            report.setSoftSkillAssessment("沟通能力中等，需要提升表达的逻辑性和条理性。团队协作意识良好，但缺乏具体案例支撑。");
            report.setGeneratedAt(LocalDateTime.now());
            report.setReportFilePath("/reports/" + savedRecord.getId() + "_report.pdf");
            interviewReportRepository.save(report);
            return ResponseEntity.ok(Map.of(
                "message", "面试结束",
                "recordId", savedRecord.getId(),
                "overallScore", savedRecord.getOverallScore(),
                "overallFeedback", savedRecord.getOverallFeedback()
            ));
        } catch (Exception e) {
            logger.info("结束面试失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "结束面试失败: " + e.getMessage()));
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
                    .orElseThrow(() -> new RuntimeException("面试记录不存在"));
            
            if (!record.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "无权访问此面试记录"));
            }
            
            // 使用VideoStorageService保存视频文件
            String videoPath = videoStorageService.saveVideo(videoFile, recordId);
            record.setVideoFilePath(videoPath);
            
            interviewRecordRepository.save(record);
            
            return ResponseEntity.ok(Map.of(
                "message", "视频上传成功", 
                "videoPath", videoPath,
                "fullUrl", "http://localhost:8080/api/video" + videoPath.replace("/videos/", "/")
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "视频上传失败: " + e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of("error", "获取历史记录失败: " + e.getMessage()));
        }
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<?> getRecordDetail(@PathVariable Long recordId,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("=== 获取面试记录详情 ===");
            logger.info("recordId: {}", recordId);
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            logger.info("username: {}", username);
            User user = userRepository.findByUsername(username).orElseThrow();
            InterviewRecord record = interviewRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("面试记录不存在"));
            logger.info("找到记录: {}, 用户: {}", record.getId(), record.getUser().getUsername());
            if (!record.getUser().getId().equals(user.getId())) {
                logger.info("权限验证失败");
                return ResponseEntity.badRequest().body(Map.of("error", "无权访问此面试记录"));
            }
            logger.info("返回记录详情");
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            logger.info("获取记录详情失败: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "获取记录详情失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{recordId}")
    public ResponseEntity<?> deleteInterviewRecord(@PathVariable Long recordId,
                                                   @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();

            InterviewRecord record = interviewRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("面试记录不存在"));

            if (!record.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "无权访问此面试记录"));
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

            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "删除失败: " + e.getMessage()));
        }
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
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {
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
                        "aiModel", "GPT-4 + Claude",
                        "difficulty", "高级"
                    ),
                    Map.of(
                        "type", "AI_RESEARCHER",
                        "title", "AI研究员",
                        "description", "专注于前沿AI算法研究和创新",
                        "skills", List.of("算法研究", "论文阅读", "数学基础", "创新思维", "实验设计"),
                        "aiModel", "Claude + GPT-4",
                        "difficulty", "专家级"
                    )
                )
            ),
            Map.of(
                "category", "大数据",
                "icon", "database",
                "color", "#10b981",
                "positions", List.of(
                    Map.of(
                        "type", "DATA_ENGINEER",
                        "title", "数据工程师",
                        "description", "专注于数据处理、ETL、数据仓库等技术",
                        "skills", List.of("SQL", "Python", "Spark", "Hadoop", "数据建模"),
                        "aiModel", "Claude + GPT-4",
                        "difficulty", "中级"
                    ),
                    Map.of(
                        "type", "DATA_SCIENTIST",
                        "title", "数据科学家",
                        "description", "专注于数据分析、统计建模、商业智能等",
                        "skills", List.of("统计分析", "机器学习", "数据可视化", "商业分析", "R/Python"),
                        "aiModel", "GPT-4 + Claude",
                        "difficulty", "高级"
                    )
                )
            ),
            Map.of(
                "category", "物联网",
                "icon", "cloud",
                "color", "#f59e0b",
                "positions", List.of(
                    Map.of(
                        "type", "IOT_ENGINEER",
                        "title", "物联网工程师",
                        "description", "专注于传感器、嵌入式系统、IoT平台开发",
                        "skills", List.of("嵌入式开发", "传感器技术", "IoT协议", "硬件设计", "云平台"),
                        "aiModel", "Claude + GPT-4",
                        "difficulty", "中级"
                    ),
                    Map.of(
                        "type", "IOT_ARCHITECT",
                        "title", "IoT架构师",
                        "description", "专注于IoT系统架构设计和优化",
                        "skills", List.of("系统架构", "物联网协议", "安全设计", "性能优化", "技术选型"),
                        "aiModel", "GPT-4 + Claude",
                        "difficulty", "高级"
                    )
                )
            ),
            Map.of(
                "category", "智能系统",
                "icon", "setting",
                "color", "#8b5cf6",
                "positions", List.of(
                    Map.of(
                        "type", "SYSTEM_ENGINEER",
                        "title", "系统工程师",
                        "description", "专注于系统设计、性能优化、架构规划",
                        "skills", List.of("系统设计", "性能优化", "架构规划", "技术选型", "团队协作"),
                        "aiModel", "Claude + GPT-4",
                        "difficulty", "高级"
                    ),
                    Map.of(
                        "type", "DEVOPS_ENGINEER",
                        "title", "DevOps工程师",
                        "description", "专注于自动化部署、监控、运维",
                        "skills", List.of("Docker", "Kubernetes", "CI/CD", "监控告警", "自动化运维"),
                        "aiModel", "GPT-4 + Claude",
                        "difficulty", "中级"
                    )
                )
            ),
            Map.of(
                "category", "产品管理",
                "icon", "user",
                "color", "#ef4444",
                "positions", List.of(
                    Map.of(
                        "type", "PRODUCT_MANAGER",
                        "title", "产品经理",
                        "description", "专注于产品规划、需求分析、用户体验",
                        "skills", List.of("产品规划", "需求分析", "用户体验", "数据分析", "项目管理"),
                        "aiModel", "Claude + GPT-4",
                        "difficulty", "中级"
                    ),
                    Map.of(
                        "type", "TECHNICAL_PRODUCT_MANAGER",
                        "title", "技术产品经理",
                        "description", "专注于技术产品规划和团队协作",
                        "skills", List.of("技术理解", "产品规划", "团队协作", "技术选型", "项目管理"),
                        "aiModel", "GPT-4 + Claude",
                        "difficulty", "高级"
                    )
                )
            )
        );
        return ResponseEntity.ok(types);
    }

    @PostMapping("/start/{type}")
    public ResponseEntity<?> startInterview(@PathVariable String type,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            
            // 根据面试类型获取岗位信息
            String position = getPositionByType(type);
            String aiModel = getAiModelByType(type);
            
            // 创建面试记录
            InterviewRecord record = InterviewRecord.builder()
                    .user(user)
                    .interviewType(type)
                    .position(position)
                    .aiModel(aiModel)
                    .startTime(LocalDateTime.now())
                    .status("IN_PROGRESS")
                    .build();
            
            InterviewRecord savedRecord = interviewRecordRepository.save(record);
            
            // 生成面试问题
            String question = largeModelService.generateQuestion(type);
            
            return ResponseEntity.ok(Map.of(
                "recordId", savedRecord.getId(),
                "question", question,
                "position", position,
                "aiModel", aiModel
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "启动面试失败: " + e.getMessage()));
        }
    }

    @PostMapping("/end/{recordId}")
    public ResponseEntity<?> endInterview(@PathVariable Long recordId,
                                        @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            
            InterviewRecord record = interviewRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("面试记录不存在"));
            
            if (!record.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "无权访问此面试记录"));
            }
            
            // 更新面试记录
            record.setEndTime(LocalDateTime.now());
            record.setStatus("COMPLETED");
            
            // 如果没有视频路径，设置一个默认值
            if (record.getVideoFilePath() == null || record.getVideoFilePath().trim().isEmpty()) {
                record.setVideoFilePath("/uploads/videos/default_" + recordId + ".mp4");
            }
            
            // 生成AI评测报告（这里先用占位数据）
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
            // 只返回已完成的面试
            List<InterviewRecord> completedRecords = records.stream()
                .filter(r -> "COMPLETED".equals(r.getStatus()))
                .toList();
            return ResponseEntity.ok(completedRecords);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "获取历史记录失败: " + e.getMessage()));
        }
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<?> getRecordDetail(@PathVariable Long recordId,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            System.out.println("=== 获取面试记录详情 ===");
            System.out.println("recordId: " + recordId);
            
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            System.out.println("username: " + username);
            User user = userRepository.findByUsername(username).orElseThrow();
            
            InterviewRecord record = interviewRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("面试记录不存在"));
            
            System.out.println("找到记录: " + record.getId() + ", 用户: " + record.getUser().getUsername());
            
            if (!record.getUser().getId().equals(user.getId())) {
                System.out.println("权限验证失败");
                return ResponseEntity.badRequest().body(Map.of("error", "无权访问此面试记录"));
            }
            
            System.out.println("返回记录详情");
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            System.out.println("获取记录详情失败: " + e.getMessage());
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
            return "GPT-4 + Claude";
        } else {
            return "Claude + GPT-4";
        }
    }
} 
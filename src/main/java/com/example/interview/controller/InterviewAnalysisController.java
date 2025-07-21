package com.example.interview.controller;

import com.example.interview.model.InterviewAnalysis;
import com.example.interview.model.InterviewRecord;
import com.example.interview.model.InterviewReport;
import com.example.interview.model.Resume;
import com.example.interview.model.ProgressInfo;
import com.example.interview.service.InterviewAnalysisMemoryService;
import com.example.interview.service.ResumeParseService;
import com.example.interview.service.PythonScriptService;
import com.example.interview.service.BigModelService;
import com.example.interview.service.VideoStorageService;
import com.example.interview.repository.InterviewRecordRepository;
import com.example.interview.repository.InterviewReportRepository;
import com.example.interview.repository.ResumeRepository;
import com.example.interview.repository.AiResponseRepository;
import com.example.interview.util.JwtUtil;
import com.example.interview.model.User;
import com.example.interview.repository.UserRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.example.interview.service.AiResponseService;

@RestController
@RequestMapping("/api/interview")
public class InterviewAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(InterviewAnalysisController.class);

    // 分析进度缓存Map
    public static final ConcurrentHashMap<Long, ProgressInfo> progressMap = new ConcurrentHashMap<>();

    @Autowired
    private InterviewAnalysisMemoryService interviewAnalysisMemoryService;
    @Autowired
    private ResumeParseService resumeParseService;
    @Autowired
    private PythonScriptService pythonScriptService;
    @Autowired
    private BigModelService bigModelService;
    @Autowired
    private VideoStorageService videoStorageService;
    @Autowired
    private InterviewRecordRepository interviewRecordRepository;
    @Autowired
    private InterviewReportRepository interviewReportRepository;
    @Autowired
    private ResumeRepository resumeRepository;
    @Autowired
    private AiResponseRepository aiResponseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AiResponseService aiResponseService;

    // 面试结束时的综合分析接口
    @PostMapping("/analyze/{recordId}")
    public Map<String, Object> analyzeInterviewComplete(
            @PathVariable Long recordId,
            @RequestParam(required = false) Long resumeId,
            @RequestHeader("Authorization") String authHeader
    ) throws Exception {
        
        logger.info("=== Starting interview analysis, record ID: {}, resume ID: {} ===", recordId, resumeId);
        
        // 1. 获取用户信息
        String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
        User user = userRepository.findByUsername(username).orElseThrow();
        logger.info("User information retrieved successfully: {}", username);
        
        // 2. 获取面试记录
        InterviewRecord record = interviewRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("面试记录不存在"));
        logger.info("Interview record retrieved successfully: position={}, type={}, duration={}s", 
                   record.getPosition(), record.getInterviewType(), record.getActualDuration());
        
        if (!record.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权访问此面试记录");
        }
        
        // 3. 获取简历信息
        String resumeText = "";
        if (resumeId != null) {
            Resume resume = resumeRepository.findById(resumeId).orElse(null);
            if (resume != null && resume.getUserId().equals(user.getId())) {
                // 这里可以调用简历解析服务，或者直接使用简历文件名
                resumeText = "简历文件：" + resume.getOriginalName() + "\n上传时间：" + resume.getUploadTime();
                logger.info("Resume information retrieved successfully: {}", resume.getOriginalName());
            } else {
                logger.warn("Resume not found or insufficient permissions: resumeId={}", resumeId);
            }
        } else {
            logger.info("No resume ID provided, skipping resume analysis");
        }
        
        // 4. 获取视频文件路径
        String videoPath = null;
        if (record.getVideoFilePath() != null) {
            videoPath = videoStorageService.getVideoFilePath(record.getVideoFilePath());
            logger.info("Video file path: {}", videoPath);
        }
        
        if (videoPath == null) {
            logger.error("Interview video file not found: recordId={}", recordId);
            throw new RuntimeException("Interview video file not found");
        }
        
        // 5. 获取AI回复数据
        String aiResponsesText = "";
        try {
            List<Map<String, Object>> aiResponses = aiResponseRepository.findRawByInterviewRecordId(recordId);
            if (aiResponses != null && !aiResponses.isEmpty()) {
                aiResponsesText = convertAiResponsesToString(aiResponses);
                logger.info("AI response data retrieved successfully, total {} records", aiResponses.size());
            } else {
                logger.warn("No AI response data found: recordId={}", recordId);
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve AI response data: {}", e.getMessage(), e);
        }
        // 5.1 获取面试人原始回答文本
        String userAnswersText = aiResponseService.aggregateUserAnswersByRecordId(recordId, "\n---\n");
        logger.info("[分析流程] 聚合面试人原始回答文本: {}", userAnswersText);
        // 6. 执行多模态分析
        logger.info("Starting multimodal analysis...");
        InterviewAnalysis analysis = performCompleteAnalysis(videoPath, resumeText, aiResponsesText, userAnswersText, record);
        
        // 7. 保存分析结果到数据库
        logger.info("Starting to save analysis results to database...");
        progressMap.put(record.getId(), new ProgressInfo(99, "正在保存分析结果..."));
        saveAnalysisToDatabase(record, analysis);
        progressMap.put(record.getId(), new ProgressInfo(100, "分析完成"));
        logger.info("Analysis results saved successfully");
        
        // 8. 返回分析结果
        logger.info("=== Interview analysis completed ===");
        return Map.of(
            "success", true,
            "message", "面试分析完成",
            "analysis", analysis
        );
    }

    // 执行完整的多模态分析
    private InterviewAnalysis performCompleteAnalysis(String videoPath, String resumeText, String aiResponsesText, String userAnswersText, InterviewRecord record) throws Exception {
        logger.info("=== Starting multimodal analysis execution ===");
        InterviewAnalysis analysis = new InterviewAnalysis();
        
        // 设置简历信息
        analysis.setResumetext(resumeText);
        logger.info("Resume information set, length: {} characters", resumeText != null ? resumeText.length() : 0);
        
        // 只用聚合的面试人原始回答文本
        analysis.setSpeechText(userAnswersText);
        logger.info("[分析流程] 使用聚合面试人原始回答文本，长度: {}", userAnswersText != null ? userAnswersText.length() : 0);
        
        // 视觉分析（Python脚本）
        logger.info("Starting visual analysis...");
        try {
        String visionAnalysis = pythonScriptService.runAllScripts(videoPath, record.getId());
        analysis.setVisionAnalysis(visionAnalysis);
            logger.info("Visual analysis successful, result length: {} characters", visionAnalysis != null ? visionAnalysis.length() : 0);
            if (visionAnalysis != null && visionAnalysis.length() > 100) {
                logger.info("Visual analysis result preview: {}", visionAnalysis.substring(0, 100) + "...");
            }
        } catch (Exception e) {
            logger.error("Visual analysis failed: {}", e.getMessage(), e);
            analysis.setVisionAnalysis("Visual analysis failed");
        }
        
        // 设置面试问题（从记录中获取）
        analysis.setModelQuestion("Interview Position: " + record.getPosition() + "\nInterview Type: " + record.getInterviewType());
        logger.info("Interview questions set: position={}, type={}", record.getPosition(), record.getInterviewType());
        
        // 构建大模型输入
        logger.info("Starting to build large model input...");
        String modelInput = buildModelInput(analysis, aiResponsesText, record);
        logger.info("Large model input built, length: {} characters", modelInput.length());
        if (modelInput.length() > 500) {
            logger.info("Large model input preview: {}", modelInput.substring(0, 500) + "...");
        }

        // 大模型评分与建议
        logger.info("Starting to call large model for analysis...");
        try {
        String modelOutput = bigModelService.askOnce(modelInput);
            logger.info("Large model call successful, output length: {} characters", modelOutput != null ? modelOutput.length() : 0);
            if (modelOutput != null && modelOutput.length() > 200) {
                logger.info("Large model output preview: {}", modelOutput.substring(0, 200) + "...");
            }
            parseModelOutput(modelOutput, analysis);
//            String score = parseScore(modelOutput);
//            String advice = parseAdvice(modelOutput);
//            analysis.setModelAdvice(advice);
//            logger.info("Score parsing result: {}, advice length: {} characters", score, advice != null ? advice.length() : 0);
        } catch (Exception e) {
            logger.error("Large model analysis failed: {}", e.getMessage(), e);
            analysis.setModelAdvice("Large model analysis failed, using default score");
        }
        
        logger.info("=== Multimodal analysis completed ===");
        return analysis;
    }

    // 构建大模型输入
    private String buildModelInput(InterviewAnalysis analysis, String aiResponsesText, InterviewRecord record) {
        StringBuilder input = new StringBuilder();
        input.append("请对以下面试表现进行专业评估，给出0-100的评分和具体建议。\n\n");
        
        input.append("=== 面试基本信息 ===\n");
        input.append("岗位：").append(record.getPosition()).append("\n");
        input.append("面试类型：").append(record.getInterviewType()).append("\n");
        input.append("面试时长：").append(record.getActualDuration()).append("秒\n\n");
        
        if (analysis.getResumetext() != null && !analysis.getResumetext().isEmpty()) {
            input.append("=== 简历信息 ===\n");
            input.append(analysis.getResumetext()).append("\n\n");
        }
        
        if (analysis.getSpeechText() != null && !analysis.getSpeechText().isEmpty()) {
            input.append("=== 面试回答内容 ===\n");
            input.append(analysis.getSpeechText()).append("\n\n");
        }
        
        if (aiResponsesText != null && !aiResponsesText.isEmpty()) {
            input.append("=== 面试官问题 ===\n");
            input.append(aiResponsesText).append("\n\n");
        }
        
        if (analysis.getVisionAnalysis() != null && !analysis.getVisionAnalysis().isEmpty()) {
            input.append("=== 多模态分析结果 ===\n");
            input.append(analysis.getVisionAnalysis()).append("\n\n");
        }
        
        input.append("请从以下维度进行评估：\n");
        input.append("1. 技术能力（0-100分）\n");
        input.append("2. 沟通表达能力（0-100分）\n");
        input.append("3. 问题解决能力（0-100分）\n");
        input.append("4. 学习能力（0-100分）\n");
        input.append("5. 总体评分（0-100分）\n");
        input.append("6. 具体改进建议\n");
        
        return input.toString();
    }

    // 将AI回复数组转换为字符串
    private String convertAiResponsesToString(List<Map<String, Object>> aiResponses) {
        logger.info("Starting to convert AI response data, total {} records", aiResponses.size());
        StringBuilder result = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        
        for (int i = 0; i < aiResponses.size(); i++) {
            Map<String, Object> response = aiResponses.get(i);
            logger.info("Processing {}th AI response: {}", i + 1, response.keySet());
            
            try {
                // 假设AI回复是JSON格式的数组
                if (response.containsKey("ai_response")) {
                    String aiResponseStr = response.get("ai_response").toString();
                    logger.info("AI response raw data: {}", aiResponseStr);
                    
                    List<String> responses = mapper.readValue(aiResponseStr, new TypeReference<List<String>>() {});
                    logger.info("Parsed {} questions", responses.size());
                    
                    for (int j = 0; j < responses.size(); j++) {
                        result.append("问题").append(i + 1).append("-").append(j + 1).append(": ");
                        result.append(responses.get(j));
                        result.append("\n");
                    }
                } else {
                    logger.warn("{}th record missing ai_response field", i + 1);
                }
            } catch (Exception e) {
                logger.error("Failed to parse {}th AI response: {}", i + 1, e.getMessage(), e);
                result.append("Question").append(i + 1).append(": [Parse failed]\n");
            }
        }
        
        String resultStr = result.toString();
        logger.info("AI response conversion completed, result length: {} characters", resultStr.length());
        return resultStr;
    }

    // 解析大模型输出的评分
    private String parseScore(String modelOutput) {
        logger.info("Starting to parse large model score, output length: {} characters", modelOutput != null ? modelOutput.length() : 0);
        
        try {
            // 简单的评分提取逻辑，可以根据实际输出格式调整
            if (modelOutput.contains("Overall Score") || modelOutput.contains("总体评分")) {
                logger.info("Found 'Overall Score' keyword, starting to parse...");
                String[] lines = modelOutput.split("\n");
                for (String line : lines) {
                    if ((line.contains("Overall Score") || line.contains("总体评分")) && line.matches(".*\\d+.*")) {
                        String score = line.replaceAll("[^0-9]", "");
                        if (!score.isEmpty()) {
                            logger.info("Parsed score from 'Overall Score' line: {}", score);
                            return score;
                        }
                    }
                }
            }
            
            // 如果没有找到特定格式，尝试提取数字
            logger.info("'Overall Score' not found, trying to extract numbers...");
            String[] words = modelOutput.split("\\s+");
            for (String word : words) {
                if (word.matches("\\d+")) {
                    int score = Integer.parseInt(word);
                    if (score >= 0 && score <= 100) {
                        logger.info("Extracted valid score: {}", score);
                        return String.valueOf(score);
                    }
                }
            }
            
            logger.warn("Failed to parse valid score, using default score 85");
        } catch (Exception e) {
            logger.error("Score parsing failed: {}", e.getMessage(), e);
        }
        
        return "85"; // 默认评分
    }

    // 解析大模型输出的建议
    private String parseAdvice(String modelOutput) {
        logger.info("Starting to parse large model advice...");
        
        try {
            // 提取建议部分
            if (modelOutput.contains("Advice") || modelOutput.contains("建议") || modelOutput.contains("Improvement") || modelOutput.contains("改进")) {
                logger.info("Found 'Advice' or 'Improvement' keyword, starting to extract...");
                String[] lines = modelOutput.split("\n");
                StringBuilder advice = new StringBuilder();
                boolean foundAdvice = false;
                
                for (String line : lines) {
                    if (line.contains("Advice") || line.contains("建议") || line.contains("Improvement") || line.contains("改进") || foundAdvice) {
                        if (!line.trim().isEmpty()) {
                            advice.append(line.trim()).append("\n");
                            foundAdvice = true;
                        }
                    }
                }
                
                if (advice.length() > 0) {
                    String result = advice.toString().trim();
                    logger.info("Successfully extracted advice, length: {} characters", result.length());
                    return result;
                } else {
                    logger.warn("No valid advice content found");
                }
            } else {
                logger.warn("'Advice' or 'Improvement' keyword not found");
            }
        } catch (Exception e) {
            logger.error("Advice parsing failed: {}", e.getMessage(), e);
        }
        
        logger.info("Using default advice");
        return "Suggest strengthening technical depth, improving communication skills, and adding specific case support.";
    }

    // 保存分析结果到数据库
    private void saveAnalysisToDatabase(InterviewRecord record, InterviewAnalysis analysis) {
        logger.info("Starting to save analysis results to database...");
        try {
            // 真实分数写入（6项）
            String skillJson;
            if (analysis.getKg() != null && analysis.getSl() != null && analysis.getEp() != null && analysis.getLo() != null && analysis.getIn() != null && analysis.getSt() != null) {
                skillJson = String.format(
                  "{\"专业知识\":%d,\"技能匹配\":%d,\"语言表达\":%d,\"逻辑思维\":%d,\"创新能力\":%d,\"应变抗压\":%d}",
                  analysis.getKg(), analysis.getSl(), analysis.getEp(), analysis.getLo(), analysis.getIn(), analysis.getSt()
                );
            } else {
                skillJson = "{\"专业知识\":\"待评测\",\"技能匹配\":\"待评测\",\"语言表达\":\"待评测\",\"逻辑思维\":\"待评测\",\"创新能力\":\"待评测\",\"应变抗压\":\"待评测\"}";
            }
            record.setSkillAssessment(skillJson);
            record.setOverallFeedback(analysis.getModelAdvice());
            record.setImprovementSuggestions("{\"建议\":[\"" + (analysis.getModelAdvice() != null ? analysis.getModelAdvice().replace("\n", "\",\"") : "待评测") + "\"]}");
            interviewRecordRepository.save(record);
            logger.info("Interview record updated successfully");

            InterviewReport report = interviewReportRepository.findByInterviewRecord(record)
                    .orElse(new InterviewReport());
            report.setInterviewRecord(record);
            report.setOverallFeedback(analysis.getModelAdvice());
            // 6项能力雷达
            report.setSkillRadarData(skillJson);
            report.setKeyIssues("{\"问题\":[\"" + (analysis.getModelAdvice() != null ? analysis.getModelAdvice().replace("\n", "\",\"") : "待评测") + "\"]}");
            report.setImprovementSuggestions("{\"建议\":[\"" + (analysis.getModelAdvice() != null ? analysis.getModelAdvice().replace("\n", "\",\"") : "待评测") + "\"]}");
            // 只保存JSON
            try {
                String analysisJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(analysis);
                report.setPerformanceAnalysis(analysisJson);
            } catch (Exception e) {
                logger.error("Failed to serialize analysis to JSON: {}", e.getMessage(), e);
                report.setPerformanceAnalysis(null);
            }
            report.setTechnicalAssessment("技术能力评估：" + (analysis.getVisionAnalysis() != null ? analysis.getVisionAnalysis() : "待评测"));
            report.setSoftSkillAssessment("软技能评估：" + (analysis.getModelAdvice() != null ? analysis.getModelAdvice() : "待评测"));
            report.setGeneratedAt(java.time.LocalDateTime.now());
            interviewReportRepository.save(report);
            logger.info("Interview report saved successfully");
            logger.info("All analysis results saved to database successfully");
        } catch (Exception e) {
            logger.error("Failed to save analysis results to database: {}", e.getMessage(), e);
            // 异常兜底，写入待评测
            try {
                record.setSkillAssessment("{\"专业知识\":\"待评测\",\"技能匹配\":\"待评测\",\"语言表达\":\"待评测\",\"逻辑思维\":\"待评测\",\"创新能力\":\"待评测\",\"应变抗压\":\"待评测\"}");
                record.setOverallFeedback("待评测");
                record.setImprovementSuggestions("{\"建议\":[\"待评测\"]}");
                interviewRecordRepository.save(record);
                InterviewReport report = interviewReportRepository.findByInterviewRecord(record)
                        .orElse(new InterviewReport());
                report.setInterviewRecord(record);
                report.setOverallFeedback("待评测");
                report.setSkillRadarData("{\"专业知识\":\"待评测\",\"技能匹配\":\"待评测\",\"语言表达\":\"待评测\",\"逻辑思维\":\"待评测\",\"创新能力\":\"待评测\",\"应变抗压\":\"待评测\"}");
                report.setKeyIssues("{\"问题\":[\"待评测\"]}");
                report.setImprovementSuggestions("{\"建议\":[\"待评测\"]}");
                report.setPerformanceAnalysis(null);
                report.setTechnicalAssessment("待评测");
                report.setSoftSkillAssessment("待评测");
                report.setGeneratedAt(java.time.LocalDateTime.now());
                interviewReportRepository.save(report);
                logger.info("兜底写入待评测标识");
            } catch (Exception ex) {
                logger.error("兜底写入待评测失败: {}", ex.getMessage(), ex);
            }
        }
    }

    // 获取分析结果
    @GetMapping("/analysis-result/{recordId}")
    public Map<String, Object> getAnalysisResult(
            @PathVariable Long recordId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.replace("Bearer ", ""));
            User user = userRepository.findByUsername(username).orElseThrow();
            
            InterviewRecord record = interviewRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("面试记录不存在"));
            
            if (!record.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权访问此面试记录");
    }

            InterviewReport report = interviewReportRepository.findByInterviewRecord(record)
                    .orElse(null);
            
            // 新增：从performanceAnalysis字段反序列化analysis
            InterviewAnalysis analysis = null;
            try {
                if (report != null && report.getPerformanceAnalysis() != null) {
                    analysis = new com.fasterxml.jackson.databind.ObjectMapper().readValue(report.getPerformanceAnalysis(), InterviewAnalysis.class);
                }
            } catch (Exception e) {
                logger.error("Failed to deserialize analysis from JSON: {}", e.getMessage(), e);
            }
            return Map.of(
                "success", true,
                "record", record,
                "report", report,
                "analysis", analysis
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    // 进度查询API
    @GetMapping("/analysis-progress/{recordId}")
    public Map<String, Object> getAnalysisProgress(@PathVariable Long recordId) {
        try {
            ProgressInfo info = progressMap.getOrDefault(recordId, new ProgressInfo(0, "等待分析"));
            return Map.of("progress", info.getPercent(), "stage", info.getStage());
        } catch (Exception e) {
            logger.error("[ProgressAPI] 获取进度异常: recordId={}, error={}", recordId, e.getMessage(), e);
            return Map.of("progress", 0, "stage", "进度获取异常");
        }
    }

    private void parseModelOutput(String modelOutput, InterviewAnalysis analysis) {
        try {
            logger.info("大模型原始输出：{}", modelOutput);
            String jsonStr = extractPureJson(modelOutput);
            logger.info("大模型提取后JSON：{}", jsonStr);
            // 字段名兼容：全部转小写
            org.json.JSONObject obj = new org.json.JSONObject(jsonStr.toLowerCase());
            analysis.setKg(obj.optInt("kg"));
            analysis.setSl(obj.optInt("sl"));
            analysis.setEp(obj.optInt("ep"));
            analysis.setLo(obj.optInt("lo"));
            analysis.setIn(obj.optInt("in"));
            analysis.setSt(obj.optInt("st"));
            analysis.setModelAdvice(obj.optString("advice"));
            analysis.setQuestion(obj.optString("question"));
            analysis.setModelPath(obj.optString("path"));
        } catch (Exception e) {
            logger.error("parseModelOutput异常：{}", e.getMessage(), e);
        }
    }

    private String extractPureJson(String modelOutput) {
        // 去除 markdown 代码块标识
        String cleaned = modelOutput.trim();
        // 去掉开头的 ```json 或 ```
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        // 去掉结尾的 ```
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }
}

package com.example.interview.service;


import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.springframework.beans.factory.annotation.Value;


@Service
public class PythonScriptService {
    
    private static final Logger logger = LoggerFactory.getLogger(PythonScriptService.class);

    @Value("${video.storage.path}")
    private String videoStoragePath;

    @Value("${python.interpreter}")
    private String pythonInterpreter;

    @Value("${python.base_script_path}")
    private String baseScriptPath;

    public String runAllScripts(String videoPath, Long recordId) throws IOException, InterruptedException {
        logger.info("=== Starting Python script analysis ===");
        logger.info("Video path: {}", videoPath);
        logger.info("Python interpreter: {}", pythonInterpreter);
        logger.info("Script base path: {}", baseScriptPath);
        
        StringBuilder resultBuilder = new StringBuilder();

        // 1. audioemal.py audio_path
        logger.info("Starting audio emotion analysis script...");
        com.example.interview.controller.InterviewAnalysisController.progressMap.put(recordId, new com.example.interview.model.ProgressInfo(10, "音频情感分析中"));
        String audioResult = runScript(baseScriptPath+"/audioemoal.py", videoPath);
        resultBuilder.append(audioResult);
        logger.info("Audio emotion analysis completed, result length: {} characters", audioResult.length());

        // 2. faceal.py --video video_path
        logger.info("Starting facial expression analysis script...");
        com.example.interview.controller.InterviewAnalysisController.progressMap.put(recordId, new com.example.interview.model.ProgressInfo(40, "面部表情分析中"));
        String faceResult = runScript(baseScriptPath+"/faceal.py", "--video", videoPath);
        resultBuilder.append(faceResult);
        logger.info("Facial expression analysis completed, result length: {} characters", faceResult.length());

        // 3. poseal.py --video video_path
        logger.info("Starting pose analysis script...");
        com.example.interview.controller.InterviewAnalysisController.progressMap.put(recordId, new com.example.interview.model.ProgressInfo(70, "姿态分析中"));
        String poseResult = runScript(baseScriptPath+"/poseal.py", "--video", videoPath);
        resultBuilder.append(poseResult);
        logger.info("Pose analysis completed, result length: {} characters", poseResult.length());
        
        com.example.interview.controller.InterviewAnalysisController.progressMap.put(recordId, new com.example.interview.model.ProgressInfo(90, "AI综合评估中"));
        String finalResult = resultBuilder.toString();
        logger.info("=== Python script analysis completed, total result length: {} characters ===", finalResult.length());
        com.example.interview.controller.InterviewAnalysisController.progressMap.put(recordId, new com.example.interview.model.ProgressInfo(100, "分析完成"));
        // 分析完成后延迟清理，防止前端还在轮询
        new Thread(() -> {
            try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
            com.example.interview.controller.InterviewAnalysisController.progressMap.remove(recordId);
        }).start();
        return finalResult;
    }

    private String runScript(String... command) throws IOException, InterruptedException {
        logger.info("Executing Python script: {}", String.join(" ", command));
        
        List<String> cmd = new ArrayList<>();
        cmd.add(pythonInterpreter);
        cmd.addAll(Arrays.asList(command));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // 设置工作目录为视频存储路径
        pb.directory(new File(videoStoragePath));
        pb.environment().put("PYTHONIOENCODING","utf-8");
        logger.info("Working directory: {}", videoStoragePath);
        
        // 获取原有 PATH
        // 加入 conda虚拟环境中的ffmpeg 路径
//        pb.environment().compute("PATH", (k, path) -> path + ";D:/miniconda3/envs/ship/Library/bin");
        Process process = pb.start();
        logger.info("Python process started, PID: {}", process.pid());

        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        // 读取标准输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // 读取错误输出
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        String result = output.toString();
        String errorResult = errorOutput.toString();
        
        logger.info("Python script execution completed, exit code: {}, output length: {} characters", exitCode, result.length());
        if (!errorResult.isEmpty()) {
            logger.warn("Python script error output: {}", errorResult);
        }
        
        if (exitCode != 0) {
            logger.error("Python script execution failed, exit code: {}, script: {}", exitCode, command[0]);
            logger.error("Error output: {}", errorResult);
            throw new RuntimeException("Script " + command[0] + " failed with exit code " + exitCode + ", error: " + errorResult);
        }
        
        return result;
    }
}
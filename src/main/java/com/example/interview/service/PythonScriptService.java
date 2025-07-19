package com.example.interview.service;


import org.springframework.stereotype.Service;

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

    @Value("${video.storage.path}")
    private String videoStoragePath;

    @Value("${python.interpreter}")
    private String pythonInterpreter;

    @Value("${python.base_script_path}")
    private String baseScriptPath;

    public String runAllScripts(String videoPath) throws IOException, InterruptedException {
        StringBuilder resultBuilder = new StringBuilder();

        // 1. audioemal.py audio_path
        resultBuilder.append(runScript(baseScriptPath+"/audioemoal.py", videoPath));

        // 2. faceal.py --viedo video_path
        resultBuilder.append(runScript(baseScriptPath+"/faceal.py", "--video", videoPath));

        // 3. poseal.py --viedo video_path
        resultBuilder.append(runScript(baseScriptPath+"/poseal.py", "--video", videoPath));
        return resultBuilder.toString();
    }

    private String runScript(String... command) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(pythonInterpreter);
        cmd.addAll(Arrays.asList(command));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // 设置工作目录为视频存储路径
        pb.directory(new File(videoStoragePath));
        pb.environment().put("PYTHONIOENCODING","utf-8");
        // 获取原有 PATH
        // 加入 conda虚拟环境中的ffmpeg 路径
//        pb.environment().compute("PATH", (k, path) -> path + ";D:/miniconda3/envs/ship/Library/bin");
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Script " + command[0] + " failed with exit code " + exitCode);
        }
        return output.toString();
    }
}
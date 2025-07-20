package com.example.interview.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.example.interview.util.AuthUtil;
import okhttp3.*;
import java.util.concurrent.CountDownLatch;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

@Service
public class Mp3SpeechRecognitionService {
    
    private static final Logger logger = LoggerFactory.getLogger(Mp3SpeechRecognitionService.class);
    private static final int SLICE_SECONDS = 60;
    private static final String FFMPEG_CMD = "D:/anaconda3/envs/interview/Library/bin/ffmpeg.exe";

    /**
     * 主入口：传入mp3或mp4路径，返回识别文本
     */
    public String recognizeAudio(String audioPath) throws Exception {
        logger.info("=== Starting speech recognition ===");
        logger.info("Audio file path: {}", audioPath);
        
        String ext = audioPath.substring(audioPath.lastIndexOf('.') + 1).toLowerCase();
        logger.info("File extension: {}", ext);
        
        String pcmPath = null;
        File tmpPcm = null;
        if ("mp4".equals(ext)) {
            logger.info("Detected MP4 file, starting audio extraction...");
            // 1. mp4先提取音频为16kHz单声道16bit pcm
            tmpPcm = File.createTempFile("audio_extract_", ".pcm");
            List<String> cmd = Arrays.asList(
                FFMPEG_CMD, "-i", audioPath,
                "-f", "s16le", "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1",
                tmpPcm.getAbsolutePath()
            );
            logger.info("FFmpeg command: {}", String.join(" ", cmd));
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            // 设置超时时间（60秒）- 增加超时时间，因为某些视频文件可能需要更长时间处理
            boolean completed = p.waitFor(60, TimeUnit.SECONDS);
            if (!completed) {
                logger.error("FFmpeg command execution timeout after 60 seconds");
                p.destroyForcibly();
                throw new RuntimeException("ffmpeg command execution timeout");
            }
            
            // 读取FFmpeg的输出和错误信息
            String output = new String(p.getInputStream().readAllBytes());
            int code = p.exitValue();
            
            if (code != 0) {
                logger.error("FFmpeg audio extraction failed, exit code: {}", code);
                logger.error("FFmpeg error output: {}", output);
                throw new RuntimeException("ffmpeg audio extraction failed: " + output);
            } else {
                logger.info("FFmpeg audio extraction output: {}", output);
            }
            pcmPath = tmpPcm.getAbsolutePath();
            logger.info("Audio extraction successful, PCM path: {}", pcmPath);
        } else if ("mp3".equals(ext)) {
            logger.info("Detected MP3 file, using directly");
            pcmPath = audioPath;
        } else if ("pcm".equals(ext)) {
            logger.info("Detected PCM file, using directly");
            pcmPath = audioPath;
        } else {
            logger.error("Unsupported file format: {}", ext);
            throw new IllegalArgumentException("Only mp3/mp4/pcm files are supported");
        }
        try {
            String result = recognizePcmOrMp3(pcmPath);
            logger.info("Speech recognition completed, recognized text length: {} characters", result.length());
            return result;
        } finally {
            if (tmpPcm != null && tmpPcm.exists()) {
                tmpPcm.delete();
                logger.info("Temporary PCM file deleted");
            }
        }
    }

    /**
     * 兼容mp3/pcm切片识别
     */
    private String recognizePcmOrMp3(String audioPath) throws Exception {
        logger.info("Starting audio slicing and recognition...");
        
        // 1. 切片
        List<File> slices = sliceMp3(audioPath, SLICE_SECONDS);
        logger.info("Audio slicing completed, total {} slices", slices.size());
        
        try {
            // 2. 并发识别
            int threadCount = Math.min(4, slices.size());
            logger.info("Using {} threads for concurrent recognition", threadCount);
            
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < slices.size(); i++) {
                File slice = slices.get(i);
                logger.info("Submitting {}th slice for recognition: {}", i + 1, slice.getName());
                futures.add(executor.submit(() -> WebIATWSUtil.recognizePcmFile(slice)));
            }
            
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < futures.size(); i++) {
                Future<String> f = futures.get(i);
                try {
                    String sliceResult = f.get();
                    result.append(sliceResult);
                    logger.info("{}th slice recognition completed, result length: {} characters", i + 1, sliceResult.length());
                } catch (Exception e) {
                    logger.error("{}th slice recognition failed: {}", i + 1, e.getMessage(), e);
                }
            }
            executor.shutdown();
            
            String finalResult = result.toString();
            logger.info("All slice recognition completed, total result length: {} characters", finalResult.length());
            return finalResult;
        } finally {
            // 3. 自动删除切片
            logger.info("Starting to clean up slice files...");
            for (File f : slices) {
                if (f.delete()) {
                    logger.info("Slice file deleted: {}", f.getName());
                } else {
                    logger.warn("Failed to delete slice file: {}", f.getName());
                }
            }
        }
    }

    /**
     * 用ffmpeg将mp3切片为60s一段pcm文件，返回切片文件列表
     */
    private List<File> sliceMp3(String mp3Path, int seconds) throws IOException, InterruptedException {
        logger.info("Starting audio slicing, file: {}, slice duration: {} seconds", mp3Path, seconds);
        
        List<File> result = new ArrayList<>();
        Path tmpDir = Files.createTempDirectory("mp3slice_");
        logger.info("Temporary directory: {}", tmpDir);
        
        // 先获取总时长
        ProcessBuilder pbDuration = new ProcessBuilder(FFMPEG_CMD, "-i", mp3Path);
        pbDuration.redirectErrorStream(true);
        Process proc = pbDuration.start();
        String durationStr = new String(proc.getInputStream().readAllBytes());
        proc.waitFor();
        double totalSeconds = parseDuration(durationStr);
        int sliceCount = (int) Math.ceil(totalSeconds / seconds);
        logger.info("Total audio duration: {} seconds, will be sliced into {} pieces", totalSeconds, sliceCount);
        
        for (int i = 0; i < sliceCount; i++) {
            String outName = String.format("slice_%03d.pcm", i);
            File outFile = tmpDir.resolve(outName).toFile();
            logger.info("Starting slice {}: {}", i + 1, outName);
            
            // ffmpeg -ss {start} -t {seconds} -i input.mp3 -f s16le -acodec pcm_s16le -ar 16000 -ac 1 out.pcm
            List<String> cmd = Arrays.asList(
                FFMPEG_CMD, "-ss", String.valueOf(i * seconds), "-t", String.valueOf(seconds),
                "-i", mp3Path,
                "-f", "s16le", "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1",
                outFile.getAbsolutePath()
            );
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().transferTo(OutputStream.nullOutputStream());
            int code = p.waitFor();
            if (code == 0) {
                result.add(outFile);
                logger.info("Slice {} completed, file size: {} bytes", i + 1, outFile.length());
            } else {
                logger.error("Slice {} failed, exit code: {}", i + 1, code);
            }
        }
        
        logger.info("Audio slicing completed, successful slices: {}/{}", result.size(), sliceCount);
        return result;
    }

    /**
     * 解析ffmpeg输出的时长
     */
    private double parseDuration(String ffmpegOutput) {
        // 查找 Duration: 00:01:23.45
        int idx = ffmpegOutput.indexOf("Duration: ");
        if (idx < 0) return 0;
        String time = ffmpegOutput.substring(idx + 10, idx + 21).trim();
        String[] parts = time.split(":");
        double sec = Double.parseDouble(parts[0]) * 3600 + Double.parseDouble(parts[1]) * 60 + Double.parseDouble(parts[2]);
        return sec;
    }

    /**
     * WebIATWS识别工具类（静态方法）
     */
    public static class WebIATWSUtil {
        // 这里填入你的讯飞云参数
        private static final String appid = "70506bae";
        private static final String apiSecret = "ZjVmZDEwZDg5MDc1M2IxMzE5MzllYTAz";
        private static final String apiKey = "8a61a91ce90d35e192c02606bc6bb158";
        private static final String hostUrl = "https://iat-api.xfyun.cn/v2/iat";

        public static String recognizePcmFile(File pcmFile) throws Exception {
            logger.info("Starting PCM file recognition: {}, size: {} bytes", pcmFile.getName(), pcmFile.length());
            
            String baseUrl = hostUrl;
            String authUrl = AuthUtil.assembleAuthUrl(baseUrl, "GET", apiKey, apiSecret);
            String wsUrl = authUrl.replace("http://", "ws://").replace("https://", "wss://");
            logger.info("Xunfei speech recognition WebSocket URL: {}", wsUrl);

            OkHttpClient client = new OkHttpClient.Builder().build();
            Request request = new Request.Builder().url(wsUrl).build();
            CountDownLatch finishLatch = new CountDownLatch(1);
            StringBuilder finalResult = new StringBuilder();
            Gson gson = new Gson();

            WebSocketListener listener = new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    logger.info("Xunfei speech recognition WebSocket connection established");
                    new Thread(() -> {
                        try (FileInputStream fs = new FileInputStream(pcmFile)) {
                            int frameSize = 1280; // 每帧大小
                            int status = 0;
                            byte[] buffer = new byte[frameSize];
                            int frameCount = 0;
                            logger.info("Starting to send audio frames...");
                            
                            while (true) {
                                int len = fs.read(buffer);
                                if (len == -1) {
                                    // 最后一帧
                                    sendFrame(webSocket, buffer, 0, 2);
                                    logger.info("Sent last frame, total frames: {}", frameCount);
                                    break;
                                }
                                if (status == 0) {
                                    sendFrame(webSocket, buffer, len, 0); // 第一帧
                                    status = 1;
                                } else {
                                    sendFrame(webSocket, buffer, len, 1); // 中间帧
                                }
                                frameCount++;
                                Thread.sleep(40);
                            }
                        } catch (Exception e) {
                            logger.error("Failed to send audio frames: {}", e.getMessage(), e);
                        }
                    }).start();
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    // 用Gson精准提取data.result.text
                    try {
                        JsonObject obj = gson.fromJson(text, JsonObject.class);
                        if (obj.has("code") && obj.get("code").getAsInt() == 0) {
                            JsonObject data = obj.has("data") ? obj.getAsJsonObject("data") : null;
                            if (data != null && data.has("result")) {
                                JsonObject result = data.getAsJsonObject("result");
                                if (result.has("ws")) {
                                    JsonArray wsArr = result.getAsJsonArray("ws");
                                    StringBuilder piece = new StringBuilder();
                                    for (int i = 0; i < wsArr.size(); i++) {
                                        JsonObject wsObj = wsArr.get(i).getAsJsonObject();
                                        if (wsObj.has("cw")) {
                                            JsonArray cwArr = wsObj.getAsJsonArray("cw");
                                            if (cwArr.size() > 0) {
                                                piece.append(cwArr.get(0).getAsJsonObject().get("w").getAsString());
                                            }
                                        }
                                    }
                                    finalResult.append(piece);
                                    if (piece.length() > 0) {
                                        logger.info("Received speech recognition fragment: {}", piece.toString());
                                    }
                                }
                            }
                        } else if (obj.has("code") && obj.get("code").getAsInt() != 0) {
                            logger.error("Xunfei speech recognition returned error, code: {}", obj.get("code").getAsInt());
                        }
                        if (obj.has("data") && obj.getAsJsonObject("data").has("status") &&
                            obj.getAsJsonObject("data").get("status").getAsInt() == 2) {
                            logger.info("Xunfei speech recognition completed, total result length: {} characters", finalResult.length());
                            finishLatch.countDown();
                            webSocket.close(1000, "done");
                        }
                    } catch (Exception e) {
                        logger.error("Failed to process Xunfei speech recognition response: {}", e.getMessage(), e);
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    logger.error("Xunfei speech recognition WebSocket connection failed: {}", t.getMessage(), t);
                    finishLatch.countDown();
                }
            };

            client.newWebSocket(request, listener);
            finishLatch.await();
            String result = finalResult.toString();
            logger.info("PCM file recognition completed: {}, result length: {} characters", pcmFile.getName(), result.length());
            return result;
        }

        private static void sendFrame(WebSocket ws, byte[] buffer, int len, int status) {
            // 构造json帧
            StringBuilder sb = new StringBuilder();
            sb.append("{\"common\":{\"app_id\":\"").append(appid).append("\"},");
            if (status == 0) {
                sb.append("\"business\":{\"language\":\"zh_cn\",\"domain\":\"iat\",\"accent\":\"mandarin\"},");
            }
            sb.append("\"data\":{");
            sb.append("\"status\":").append(status).append(",");
            sb.append("\"format\":\"audio/L16;rate=16000\",");
            sb.append("\"encoding\":\"raw\",");
            sb.append("\"audio\":\"");
            if (len > 0) {
                sb.append(Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
            }
            sb.append("\"}}");
            ws.send(sb.toString());
        }
    }
} 
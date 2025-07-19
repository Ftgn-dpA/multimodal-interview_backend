package com.example.interview.service;

import org.springframework.stereotype.Service;

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
    private static final int SLICE_SECONDS = 60;
    private static final String FFMPEG_CMD = "ffmpeg";

    /**
     * 主入口：传入mp3或mp4路径，返回识别文本
     */
    public String recognizeAudio(String audioPath) throws Exception {
        String ext = audioPath.substring(audioPath.lastIndexOf('.') + 1).toLowerCase();
        String pcmPath = null;
        File tmpPcm = null;
        if ("mp4".equals(ext)) {
            // 1. mp4先提取音频为16kHz单声道16bit pcm
            tmpPcm = File.createTempFile("audio_extract_", ".pcm");
            List<String> cmd = Arrays.asList(
                FFMPEG_CMD, "-i", audioPath,
                "-f", "s16le", "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1",
                tmpPcm.getAbsolutePath()
            );
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            p.getInputStream().transferTo(OutputStream.nullOutputStream());
            int code = p.waitFor();
            if (code != 0) throw new RuntimeException("ffmpeg 提取音频失败");
            pcmPath = tmpPcm.getAbsolutePath();
        } else if ("mp3".equals(ext)) {
            pcmPath = audioPath;
        } else if ("pcm".equals(ext)) {
            pcmPath = audioPath;
        } else {
            throw new IllegalArgumentException("仅支持mp3/mp4/pcm文件");
        }
        try {
            return recognizePcmOrMp3(pcmPath);
        } finally {
            if (tmpPcm != null && tmpPcm.exists()) tmpPcm.delete();
        }
    }

    /**
     * 兼容mp3/pcm切片识别
     */
    private String recognizePcmOrMp3(String audioPath) throws Exception {
        // 1. 切片
        List<File> slices = sliceMp3(audioPath, SLICE_SECONDS);
        try {
            // 2. 并发识别
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(4, slices.size()));
            List<Future<String>> futures = new ArrayList<>();
            for (File slice : slices) {
                futures.add(executor.submit(() -> WebIATWSUtil.recognizePcmFile(slice)));
            }
            StringBuilder result = new StringBuilder();
            for (Future<String> f : futures) {
                result.append(f.get());
            }
            executor.shutdown();
            return result.toString();
        } finally {
            // 3. 自动删除切片
            for (File f : slices) f.delete();
        }
    }

    /**
     * 用ffmpeg将mp3切片为60s一段pcm文件，返回切片文件列表
     */
    private List<File> sliceMp3(String mp3Path, int seconds) throws IOException, InterruptedException {
        List<File> result = new ArrayList<>();
        Path tmpDir = Files.createTempDirectory("mp3slice_");
        // 先获取总时长
        ProcessBuilder pbDuration = new ProcessBuilder(FFMPEG_CMD, "-i", mp3Path);
        pbDuration.redirectErrorStream(true);
        Process proc = pbDuration.start();
        String durationStr = new String(proc.getInputStream().readAllBytes());
        proc.waitFor();
        double totalSeconds = parseDuration(durationStr);
        int sliceCount = (int) Math.ceil(totalSeconds / seconds);
        for (int i = 0; i < sliceCount; i++) {
            String outName = String.format("slice_%03d.pcm", i);
            File outFile = tmpDir.resolve(outName).toFile();
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
            if (code == 0) result.add(outFile);
        }
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
            String baseUrl = hostUrl;
            String authUrl = AuthUtil.assembleAuthUrl(baseUrl, "GET", apiKey, apiSecret);
            String wsUrl = authUrl.replace("http://", "ws://").replace("https://", "wss://");

            OkHttpClient client = new OkHttpClient.Builder().build();
            Request request = new Request.Builder().url(wsUrl).build();
            CountDownLatch finishLatch = new CountDownLatch(1);
            StringBuilder finalResult = new StringBuilder();
            Gson gson = new Gson();

            WebSocketListener listener = new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    new Thread(() -> {
                        try (FileInputStream fs = new FileInputStream(pcmFile)) {
                            int frameSize = 1280; // 每帧大小
                            int status = 0;
                            byte[] buffer = new byte[frameSize];
                            while (true) {
                                int len = fs.read(buffer);
                                if (len == -1) {
                                    // 最后一帧
                                    sendFrame(webSocket, buffer, 0, 2);
                                    break;
                                }
                                if (status == 0) {
                                    sendFrame(webSocket, buffer, len, 0); // 第一帧
                                    status = 1;
                                } else {
                                    sendFrame(webSocket, buffer, len, 1); // 中间帧
                                }
                                Thread.sleep(40);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
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
                                }
                            }
                        }
                        if (obj.has("data") && obj.getAsJsonObject("data").has("status") &&
                            obj.getAsJsonObject("data").get("status").getAsInt() == 2) {
                            finishLatch.countDown();
                            webSocket.close(1000, "done");
                        }
                    } catch (Exception e) {
                        // 忽略解析异常
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    finishLatch.countDown();
                }
            };

            client.newWebSocket(request, listener);
            finishLatch.await();
            return finalResult.toString();
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
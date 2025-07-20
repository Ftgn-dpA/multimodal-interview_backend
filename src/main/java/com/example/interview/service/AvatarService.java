package com.example.interview.service;

import com.example.interview.config.AvatarConfig;
import com.example.interview.ws.AvatarWebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URI;

@Service
public class AvatarService {
    
    @Autowired
    private com.example.interview.service.AiResponseService aiResponseService;
    @Autowired
    private AvatarConfig config;

    // 多会话管理
    private final ConcurrentHashMap<String, AvatarWebSocketClient> sessionMap = new ConcurrentHashMap<>();

    public Map<String, Object> startSession() throws Exception {
        Map<String, Object> result = new HashMap<>();
        try {
            // AvatarService starting session
            String baseUrl = "wss://avatar.cn-huadong-1.xf-yun.com/v1/interact";
            // Virtual human service config loaded
            String authUrl = com.example.interview.util.AuthUtil.assembleAuthUrl(baseUrl, "GET", config.apiKey, config.apiSecret);
            CountDownLatch latch = new CountDownLatch(1);
            String tmpSessionId;
            do {
                tmpSessionId = UUID.randomUUID().toString();
            } while (sessionMap.containsKey(tmpSessionId));
            final String sessionId = tmpSessionId;
            // Session ID generated
            // Defensive: if exists, remove first
            AvatarWebSocketClient old = sessionMap.remove(sessionId);
            if (old != null) old.stopClient();
            AvatarWebSocketClient client = new AvatarWebSocketClient(
                    new URI(authUrl),
                    config.appId,
                    config.avatarId,
                    config.vcn,
                    config.sceneId,
                    latch,
                    sessionId
            );
            
            // Set AI response callback
            client.setAiResponseCallback(aiResponseService);
            sessionMap.put(sessionId, client);
            // Client added to session map
            Thread wsThread = new Thread(() -> {
                try {
                    // WebSocket connection starting
                    client.connectBlocking();
                    while (client.isActive() && !client.isClosed()) {
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    // Thread exception auto close
                    // client.status.set(false); // No longer directly access private field
                    // WebSocket connection exception
                    e.printStackTrace();
                }
            });
            wsThread.setDaemon(true);
            wsThread.start();
            // WebSocket thread started
            // Robust wait for streamUrl return, max 10 seconds
            boolean gotStream = latch.await(10, TimeUnit.SECONDS);
            // Wait for stream URL result
            result.put("session", sessionId);
            if (gotStream && client.getStreamUrl() != null) {
                // Stream URL obtained
                result.put("stream_url", client.getStreamUrl());
                result.put("api_url", "https://rtc-api.xf-yun.com/v1/rtc/play/"); // 信令API地址，按实际服务商填写
                result.put("status", "ok");
            } else {
                // Failed to get stream URL
                result.put("status", "fail");
                result.put("msg", "未获取到streamUrl，已自动关闭会话");
                // Timeout auto close and remove
                client.stopClient();
                sessionMap.remove(sessionId);
            }
        } catch (Exception e) {
            System.err.println("[AvatarService] startSession error: " + e.getMessage());
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "启动虚拟人失败: " + e.getMessage());
            result.put("session", null);
        }
        // Session startup completed
        return result;
    }

    public String sendInteractText(String sessionId, String text) {
        AvatarWebSocketClient client = sessionMap.get(sessionId);
        if (client != null) {
            client.sendInteractText(text);
            return "消息已发送";
        } else {
            return "avatar会话未启动或已关闭";
        }
    }

    public String stopSession(String sessionId) {
        AvatarWebSocketClient client = sessionMap.remove(sessionId);
        if (client != null) {
            client.stopClient();
            // 再次确认彻底移除
            sessionMap.remove(sessionId);
            return "avatar会话已关闭";
        } else {
            return "avatar会话未启动或已关闭";
        }
    }

    /**
     * 音频交互：极致性能优化，严格按照讯飞协议规范
     */
    public Map<String, Object> audioInteract(String sessionId, MultipartFile audioFile) {
        Map<String, Object> result = new HashMap<>();
        AvatarWebSocketClient client = sessionMap.get(sessionId);
        if (client == null) {
            result.put("status", "fail");
            result.put("msg", "avatar会话未启动或已关闭");
            // 未找到对应的WebSocket客户端
            return result;
        }
        try {
            // 读取音频数据
            byte[] audioBytes = audioFile.getBytes();
            // 严格按照协议：frame_size最大1024字节
            int frameSize = 1024; // 协议限制的最大帧大小
            int total = audioBytes.length;
            int seq = 0;
            String requestId = UUID.randomUUID().toString(); // 单次请求的唯一ID
            
            // Start pushing audio data
            
            for (int offset = 0; offset < total; offset += frameSize) {
                int len = Math.min(frameSize, total - offset);
                byte[] frame = new byte[len];
                System.arraycopy(audioBytes, offset, frame, 0, len);
                String base64Audio = Base64.getEncoder().encodeToString(frame);
                
                // Strictly follow protocol: 0=start, 1=middle, 2=end
                int status;
                if (offset == 0) {
                    status = 0; // First frame: start
                } else if (offset + len >= total) {
                    status = 2; // Last frame: end
                } else {
                    status = 1; // Middle frame: middle
                }
                
                Map<String, Object> msg = new HashMap<>();
                
                // Header part - strictly follow protocol
                Map<String, Object> header = new HashMap<>();
                header.put("app_id", client.getAppId());
                header.put("ctrl", "audio_interact");
                header.put("request_id", requestId);
                msg.put("header", header);
                
                // Parameter part - use asr instead of avatar_dispatch
                Map<String, Object> parameter = new HashMap<>();
                Map<String, Object> asr = new HashMap<>();
                asr.put("full_duplex", 0); // 0=hold to talk speech recognition
                parameter.put("asr", asr);
                msg.put("parameter", parameter);
                
                // Payload.audio part - strictly follow protocol
                Map<String, Object> payload = new HashMap<>();
                Map<String, Object> audio = new HashMap<>();
                audio.put("encoding", "raw"); // Use raw encoding
                audio.put("sample_rate", 16000);
                audio.put("channels", 1);
                audio.put("bit_depth", 16);
                audio.put("status", status);
                audio.put("seq", seq);
                audio.put("frame_size", len); // Actual frame size
                audio.put("audio", base64Audio);
                payload.put("audio", audio);
                msg.put("payload", payload);
                
                client.sendRawJson(msg);
                seq++;
                
                // Ultimate performance: minimal delay, but avoid platform throttling
                if (status != 2) { // Only delay if not last frame
                    Thread.sleep(10); // 10ms interval, ultimate speed
                }
            }
            
            // Audio push completed
            result.put("status", "ok");
            result.put("msg", "音频已发送，等待平台响应");
        } catch (IOException | InterruptedException e) {
            result.put("status", "fail");
            result.put("msg", "音频处理异常: " + e.getMessage());
            // Audio processing exception
        }
        return result;
    }
} 
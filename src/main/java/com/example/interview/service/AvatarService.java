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
            System.out.println("[AvatarService] startSession begin, config: " + config);
            String baseUrl = "wss://avatar.cn-huadong-1.xf-yun.com/v1/interact";
            System.out.println("[AvatarService] baseUrl: " + baseUrl);
            String authUrl = com.example.interview.util.AuthUtil.assembleAuthUrl(baseUrl, "GET", config.apiKey, config.apiSecret);
            System.out.println("[AvatarService] authUrl: " + authUrl);
            CountDownLatch latch = new CountDownLatch(1);
            String tmpSessionId;
            do {
                tmpSessionId = UUID.randomUUID().toString();
            } while (sessionMap.containsKey(tmpSessionId));
            final String sessionId = tmpSessionId;
            System.out.println("[AvatarService] generated sessionId: " + sessionId);
            // 防御性：如果已存在，先移除
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
            
            // 设置AI回复回调
            client.setAiResponseCallback(aiResponseService);
            sessionMap.put(sessionId, client);
            System.out.println("[AvatarService] put sessionId=" + sessionId + ", client hashCode=" + client.hashCode());
            Thread wsThread = new Thread(() -> {
                try {
                    System.out.println("[thread] connectBlocking() start, sessionId=" + sessionId + ", client hashCode=" + client.hashCode());
                    client.connectBlocking();
                    while (client.isActive() && !client.isClosed()) {
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    // 线程异常时自动关闭
                    // client.status.set(false); // 不再直接访问私有字段
                    System.out.println("[thread] run error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            wsThread.setDaemon(true);
            wsThread.start();
            System.out.println("[AvatarService] started thread for sessionId=" + sessionId + ", client hashCode=" + client.hashCode());
            // 健壮等待streamUrl返回，最多等待10秒
            boolean gotStream = latch.await(10, TimeUnit.SECONDS);
            System.out.println("[AvatarService] latch await result: " + gotStream);
            result.put("session", sessionId);
            if (gotStream && client.getStreamUrl() != null) {
                System.out.println("[AvatarService] got streamUrl: " + client.getStreamUrl());
                result.put("stream_url", client.getStreamUrl());
                result.put("api_url", "https://rtc-api.xf-yun.com/v1/rtc/play/"); // 信令API地址，按实际服务商填写
                result.put("status", "ok");
            } else {
                System.out.println("[AvatarService] failed to get streamUrl, gotStream: " + gotStream + ", streamUrl: " + client.getStreamUrl());
                result.put("status", "fail");
                result.put("msg", "未获取到streamUrl，已自动关闭会话");
                // 超时主动关闭并移除
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
        System.out.println("[AvatarService] startSession result: " + result);
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
            System.out.println("[audioInteract] sessionId=" + sessionId + " 未找到对应WebSocketClient");
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
            
            System.out.println("[audioInteract] 开始推送音频，总字节数=" + total + ", 预计帧数=" + ((total + frameSize - 1) / frameSize));
            
            for (int offset = 0; offset < total; offset += frameSize) {
                int len = Math.min(frameSize, total - offset);
                byte[] frame = new byte[len];
                System.arraycopy(audioBytes, offset, frame, 0, len);
                String base64Audio = Base64.getEncoder().encodeToString(frame);
                
                // 严格按照协议：0=开始，1=中间，2=结束
                int status;
                if (offset == 0) {
                    status = 0; // 首帧：开始
                } else if (offset + len >= total) {
                    status = 2; // 尾帧：结束
                } else {
                    status = 1; // 中间帧：中间
                }
                
                Map<String, Object> msg = new HashMap<>();
                
                // Header部分 - 严格按照协议
                Map<String, Object> header = new HashMap<>();
                header.put("app_id", client.getAppId());
                header.put("ctrl", "audio_interact");
                header.put("request_id", requestId);
                msg.put("header", header);
                
                // Parameter部分 - 使用asr而不是avatar_dispatch
                Map<String, Object> parameter = new HashMap<>();
                Map<String, Object> asr = new HashMap<>();
                asr.put("full_duplex", 0); // 0=按住说话语音识别
                parameter.put("asr", asr);
                msg.put("parameter", parameter);
                
                // Payload.audio部分 - 严格按照协议
                Map<String, Object> payload = new HashMap<>();
                Map<String, Object> audio = new HashMap<>();
                audio.put("encoding", "raw"); // 使用raw编码
                audio.put("sample_rate", 16000);
                audio.put("channels", 1);
                audio.put("bit_depth", 16);
                audio.put("status", status);
                audio.put("seq", seq);
                audio.put("frame_size", len); // 实际帧大小
                audio.put("audio", base64Audio);
                payload.put("audio", audio);
                msg.put("payload", payload);
                
                client.sendRawJson(msg);
                seq++;
                
                // 极致性能：最小延迟，但避免平台限流
                if (status != 2) { // 不是最后一帧才延迟
                    Thread.sleep(10); // 10ms间隔，极致速度
                }
            }
            
            System.out.println("[audioInteract] 音频推送完成，总帧数=" + seq);
            result.put("status", "ok");
            result.put("msg", "音频已发送，等待平台响应");
        } catch (IOException | InterruptedException e) {
            result.put("status", "fail");
            result.put("msg", "音频处理异常: " + e.getMessage());
            System.out.println("[audioInteract] 音频处理异常: " + e.getMessage());
        }
        return result;
    }
} 
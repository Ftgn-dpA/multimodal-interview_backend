package com.example.interview.service;

import com.example.interview.config.AvatarConfig;
import com.example.interview.ws.AvatarWebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AvatarService {
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
                    latch,
                    sessionId
            );
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
            result.put("sessionId", sessionId);
            if (gotStream && client.getStreamUrl() != null) {
                System.out.println("[AvatarService] got streamUrl: " + client.getStreamUrl());
                result.put("streamUrl", client.getStreamUrl());
                result.put("apiUrl", "https://rtc-api.xf-yun.com/v1/rtc/play/"); // 信令API地址，按实际服务商填写
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
            result.put("sessionId", null);
        }
        System.out.println("[AvatarService] startSession result: " + result);
        return result;
    }

    public String sendText(String sessionId, String text) {
        AvatarWebSocketClient client = sessionMap.get(sessionId);
        if (client != null) {
            client.sendDriverText(text);
            return "文本已发送";
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
} 
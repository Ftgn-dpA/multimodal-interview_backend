package com.example.interview.ws;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import java.util.Map;
import java.util.HashMap;

import java.nio.charset.StandardCharsets;

public class AvatarWebSocketClient extends WebSocketClient {
    private String appId;
    private String avatarId;
    private String vcn;
    private String sceneId;
    private CountDownLatch latch;
    private BlockingQueue<String> dataList = new LinkedBlockingQueue<>(100);
    private AtomicBoolean status = new AtomicBoolean(true);
    private volatile boolean linkConnected = false;
    private volatile boolean avatarLinked = false;
    private Thread sendThread;
    private volatile String streamUrl = null;
    private String sessionId;

    public AvatarWebSocketClient(URI serverUri, String appId, String avatarId, String vcn, String sceneId, CountDownLatch latch, String sessionId) {
        super(serverUri);
        this.appId = appId;
        this.avatarId = avatarId;
        this.vcn = vcn;
        this.sceneId = sceneId;
        this.latch = latch;
        this.sessionId = sessionId;
        System.out.println("[构造] AvatarWebSocketClient created, sessionId=" + sessionId + ", hashCode=" + this.hashCode());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        linkConnected = true;
        connectAvatar();
        sendThread = new Thread(this::sendMessage);
        sendThread.setDaemon(true);
        sendThread.start();
    }

    @Override
    public void onMessage(String message) {
        System.out.println("[原始返回] " + message);
        try {
            Map<String, Object> data = parseJson(message);
            Map<String, Object> header = (Map<String, Object>) data.get("header");
            
            // 处理header响应字段
            if (header != null) {
                int code = ((Number)header.getOrDefault("code", 0)).intValue();
                String messageText = (String) header.getOrDefault("message", "");
                String sid = (String) header.getOrDefault("sid", "");
                String session = (String) header.getOrDefault("session", "");
                
                System.out.println("[响应头] code=" + code + ", message=" + messageText + ", sid=" + sid + ", session=" + session);
                
                if (code != 0) {
                    status.set(false);
                    System.out.println("receive error msg: " + message);
                    latch.countDown();
                    return;
                }
            }
            
            Map<String, Object> payload = (Map<String, Object>) data.get("payload");
            if (payload != null && payload.containsKey("avatar")) {
                Map<String, Object> avatar = (Map<String, Object>) payload.get("avatar");
                String eventType = (String) avatar.get("event_type");
                if ("stop".equals(eventType)) {
                    status.set(false);
                    System.out.println("avatar stop event received");
                    latch.countDown();
                } else if ("stream_info".equals(eventType)) {
                    avatarLinked = true;
                    if (avatar.get("stream_url") != null) {
                        streamUrl = avatar.get("stream_url").toString();
                        latch.countDown(); // 拿到stream_url时立即唤醒主线程
                    }
                    System.out.println("avatar ws connected: " + message);
                    System.out.println("stream url: " + streamUrl);
                } else if ("pong".equals(eventType)) {
                    // 心跳响应
                    System.out.println("收到心跳响应");
                } else {
                    System.out.println("收到其他avatar事件: " + eventType);
                }
            } else {
                System.out.println("收到非avatar消息或payload为空");
            }
        } catch (Exception e) {
            System.out.println("消息解析异常: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket 关闭: " + reason);
        status.set(false);
        latch.countDown();
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        status.set(false);
        latch.countDown();
    }

    public void stopClient() {
        status.set(false);
        if (this.isOpen()) {
            this.close();
        }
    }

    public boolean isActive() {
        return status.get();
    }

    // 文本交互协议 - 会走到交互平台中配置的大模型进行语义理解
    public void sendInteractText(String interactText) {
        try {
            Map<String, Object> textMsg = new HashMap<>();
            Map<String, Object> header = new HashMap<>();
            header.put("app_id", appId);
            header.put("request_id", UUID.randomUUID().toString());
            header.put("ctrl", "text_interact");
            textMsg.put("header", header);
            Map<String, Object> parameter = new HashMap<>();
            Map<String, Object> tts = new HashMap<>();
            tts.put("vcn", vcn);
            tts.put("speed", 50);
            tts.put("pitch", 50);
            tts.put("audio", new HashMap<String, Object>() {{
                put("sample_rate", 16000);
            }});
            parameter.put("tts", tts);
            Map<String, Object> air = new HashMap<>();
            air.put("air", 1); // 是否开启自动动作，0关闭/1开启，自动动作只有开启交互走到大模型时才生效
            air.put("add_nonsemantic", 1); // 是否开启无指向性动作，0关闭，1开启（需配合nlp=true时生效)，虚拟人会做没有意图指向性的动作
            parameter.put("air", air);
            textMsg.put("parameter", parameter);
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> text = new HashMap<>();
            text.put("content", interactText);
            payload.put("text", text);
            textMsg.put("payload", payload);
            dataList.put(toJson(textMsg));
        } catch (Exception e) {
            System.out.println("sendInteractText error: " + e.getMessage());
        }
    }

    private void connectAvatar() {
        try {
            Map<String, Object> startMsg = new HashMap<>();
            Map<String, Object> header = new HashMap<>();
            header.put("app_id", appId);
            header.put("request_id", UUID.randomUUID().toString());
            header.put("ctrl", "start");
            header.put("scene_id", sceneId); // 添加场景ID
            startMsg.put("header", header);
            Map<String, Object> parameter = new HashMap<>();
            Map<String, Object> tts = new HashMap<>();
            tts.put("vcn", vcn);
            parameter.put("tts", tts);
            Map<String, Object> avatar = new HashMap<>();
            Map<String, Object> stream = new HashMap<>();
            stream.put("protocol", "webrtc");
            stream.put("fps", 25); // 视频刷新率
            stream.put("bitrate", 5000); // 视频码率
            stream.put("alpha", 0); // 透明背景，0关闭
            avatar.put("stream", stream);
            avatar.put("avatar_id", avatarId);
            avatar.put("width", 720); // 视频分辨率：宽
            avatar.put("height", 1280); // 视频分辨率：高
            parameter.put("avatar", avatar);
            startMsg.put("parameter", parameter);
            System.out.println("send start request: " + toJson(startMsg));
            this.send(toJson(startMsg));
        } catch (Exception e) {
            System.out.println("connectAvatar error: " + e.getMessage());
        }
    }

    private String getPingMsg() {
        Map<String, Object> pingMsg = new HashMap<>();
        Map<String, Object> header = new HashMap<>();
        header.put("app_id", appId);
        header.put("request_id", UUID.randomUUID().toString());
        header.put("ctrl", "ping");
        pingMsg.put("header", header);
        return toJson(pingMsg);
    }

    private void sendMessage() {
        while (status.get()) {
            if (linkConnected) {
                try {
                    if (avatarLinked) {
                        String task = dataList.poll(5, TimeUnit.SECONDS);
                        if (task != null) {
                            System.out.println("send msg: " + task);
                            this.send(task);
                        } else {
                            if (status.get() && avatarLinked) {
                                this.send(getPingMsg());
                            }
                        }
                    } else {
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    System.out.println("sendMessage error: " + e.getMessage());
                }
            } else {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    // 简单JSON序列化/反序列化（可用第三方库优化）
    private String toJson(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
    private Map<String, Object> parseJson(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
} 
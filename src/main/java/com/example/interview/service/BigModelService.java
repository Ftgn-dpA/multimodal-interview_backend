package com.example.interview.service;

import com.example.interview.util.AuthUtil;
import com.google.gson.Gson;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;

@Service
public class BigModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(BigModelService.class);

    // 你的讯飞大模型参数
    private static final String hostUrl = "https://spark-api.xf-yun.com/v3.5/chat";
    private static final String domain = "generalv3.5";
    private static final String appid = "c8a778c5";
    private static final String apiSecret = "Zjk0ZGY1NzlkZDBhZmY2YmEzOThkMDFj";
    private static final String apiKey = "fa559f68bf426ae0737377ad0472ad64";
    private static final Gson gson = new Gson();

    public String askOnce(String question) throws Exception {
        logger.info("=== Starting large model call ===");
        logger.info("Question length: {} characters", question != null ? question.length() : 0);
        if (question != null && question.length() > 200) {
            logger.info("Question preview: {}", question.substring(0, 200) + "...");
        }
        
        String authUrl = AuthUtil.assembleAuthUrl(hostUrl, "GET", apiKey, apiSecret);
        String wsUrl = authUrl.replace("http://", "ws://").replace("https://", "wss://");
        logger.info("WebSocket URL: {}", wsUrl);
        
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(wsUrl).build();
        CountDownLatch finishLatch = new CountDownLatch(1);
        StringBuilder answer = new StringBuilder();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                logger.info("WebSocket connection established");
                
                // 构造请求参数
                Map<String, Object> req = new HashMap<>();
                Map<String, Object> header = new HashMap<>();
                header.put("app_id", appid);
                header.put("uid", UUID.randomUUID().toString().substring(0, 10));
                req.put("header", header);

                Map<String, Object> parameter = new HashMap<>();
                Map<String, Object> chat = new HashMap<>();
                chat.put("domain", domain);
                chat.put("temperature", 0.5);
                chat.put("max_tokens", 4096);
                parameter.put("chat", chat);
                req.put("parameter", parameter);

                Map<String, Object> payload = new HashMap<>();
                Map<String, Object> message = new HashMap<>();
                List<Map<String, String>> text = new ArrayList<>();

                // 先加 system 消息,需要
                Map<String, String> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", "请你根据以下信息给出面试者专业知识水平、技能匹配度、语言表达能力、逻辑思维能力、创新能力、应变抗压能力六方面评分（满分100）、关键问题定位、改进建议以及推荐学习资源" +
                        "以json形式返回，包含以下字段，kg,sl,ep,lo,in,st对应评分，question对应问题，advice对应改进建议，path对应推荐路径");
                text.add(systemMsg);

                // 其他历史消息
                // ...（如有历史消息，依次 add）...

                // 用户消息
                Map<String, String> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", question);
                text.add(userMsg);

                message.put("text", text);
                payload.put("message", message);
                req.put("payload", payload);

                String requestJson = gson.toJson(req);
                logger.info("Sending request to large model, request length: {} characters", requestJson.length());
                webSocket.send(requestJson);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    logger.info("Received large model response, response length: {} characters", text.length());
                    if (text.length() > 200) {
                        logger.info("Response preview: {}", text.substring(0, 200) + "...");
                    }
                    
                    JsonParse resp = gson.fromJson(text, JsonParse.class);
                    if (resp.header.code != 0) {
                        logger.error("Large model returned error, code: {}", resp.header.code);
                        answer.append("[Error] code=").append(resp.header.code);
                        finishLatch.countDown();
                        webSocket.close(1000, "");
                        return;
                    }
                    if (resp.payload != null && resp.payload.choices != null && resp.payload.choices.text != null) {
                        for (Text t : resp.payload.choices.text) {
                            answer.append(t.content);
                        }
                        logger.info("Accumulated answer length: {} characters", answer.length());
                    }
                    if (resp.header.status == 2) {
                        logger.info("Large model answer completed, total length: {} characters", answer.length());
                        finishLatch.countDown();
                        webSocket.close(1000, "");
                    }
                } catch (Exception e) {
                    logger.error("Failed to process large model response: {}", e.getMessage(), e);
                    finishLatch.countDown();
                    webSocket.close(1000, "");
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                logger.error("WebSocket connection failed: {}", t.getMessage(), t);
                answer.append("[Connection failed]");
                finishLatch.countDown();
            }
        };

        client.newWebSocket(request, listener);
        finishLatch.await();
        String result = answer.toString();
        logger.info("=== Large model call completed, final result length: {} characters ===", result.length());
        return result;
    }

    // 内部类用于解析返回
    static class JsonParse {
        Header header;
        Payload payload;
    }
    static class Header {
        int code;
        int status;
        String sid;
    }
    static class Payload {
        Choices choices;
    }
    static class Choices {
        List<Text> text;
    }
    static class Text {
        String role;
        String content;
    }
}
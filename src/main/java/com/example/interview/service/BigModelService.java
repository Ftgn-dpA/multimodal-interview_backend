package com.example.interview.service;

import com.example.interview.util.AuthUtil;
import com.google.gson.Gson;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CountDownLatch;

@Service
public class BigModelService {

    // 你的讯飞大模型参数
    private static final String hostUrl = "https://spark-api.xf-yun.com/v1.1/chat";
    private static final String domain = "lite";
    private static final String appid = "c8a778c5";
    private static final String apiSecret = "Zjk0ZGY1NzlkZDBhZmY2YmEzOThkMDFj";
    private static final String apiKey = "fa559f68bf426ae0737377ad0472ad64";
    private static final Gson gson = new Gson();

    public String askOnce(String question) throws Exception {
        String authUrl = AuthUtil.assembleAuthUrl(hostUrl, "GET", apiKey, apiSecret);
        String wsUrl = authUrl.replace("http://", "ws://").replace("https://", "wss://");
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(wsUrl).build();
        CountDownLatch finishLatch = new CountDownLatch(1);
        StringBuilder answer = new StringBuilder();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
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
                /*Map<String, String> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", "");
                text.add(systemMsg);*/

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

                webSocket.send(gson.toJson(req));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JsonParse resp = gson.fromJson(text, JsonParse.class);
                    if (resp.header.code != 0) {
                        answer.append("[错误] code=").append(resp.header.code);
                        finishLatch.countDown();
                        webSocket.close(1000, "");
                        return;
                    }
                    if (resp.payload != null && resp.payload.choices != null && resp.payload.choices.text != null) {
                        for (Text t : resp.payload.choices.text) {
                            answer.append(t.content);
                        }
                    }
                    if (resp.header.status == 2) {
                        finishLatch.countDown();
                        webSocket.close(1000, "");
                    }
                } catch (Exception e) {
                    finishLatch.countDown();
                    webSocket.close(1000, "");
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                answer.append("[连接失败]");
                finishLatch.countDown();
            }
        };

        client.newWebSocket(request, listener);
        finishLatch.await();
        return answer.toString();
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
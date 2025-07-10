package com.example.interview.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Base64;
import java.util.TimeZone;

public class AuthUtil {
    public static String assembleAuthUrl(String requestUrl, String method, String apiKey, String apiSecret) throws Exception {
        UrlParts u = parseUrl(requestUrl);
        String host = u.host;
        String path = u.path;
        String schema = u.schema;

        String date = getGMTDate();
        String signatureOrigin = "host: " + host + "\n" +
                "date: " + date + "\n" +
                method + " " + path + " HTTP/1.1";
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        hmacSha256.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signatureSha = hmacSha256.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signatureShaBase64 = Base64.getEncoder().encodeToString(signatureSha);

        String authorizationOrigin = String.format("api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"",
                apiKey, signatureShaBase64);
        String authorization = Base64.getEncoder().encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));

        Map<String, String> values = new LinkedHashMap<>();
        values.put("host", host);
        values.put("date", date);
        values.put("authorization", authorization);

        StringBuilder sb = new StringBuilder(requestUrl);
        sb.append("?");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private static String getGMTDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }

    private static UrlParts parseUrl(String requestUrl) {
        int stidx = requestUrl.indexOf("://");
        String schema = requestUrl.substring(0, stidx + 3);
        String host = requestUrl.substring(stidx + 3);
        int edidx = host.indexOf("/");
        if (edidx <= 0) throw new RuntimeException("invalid request url:" + requestUrl);
        String path = host.substring(edidx);
        host = host.substring(0, edidx);
        return new UrlParts(host, path, schema);
    }

    private static class UrlParts {
        String host, path, schema;
        UrlParts(String host, String path, String schema) {
            this.host = host;
            this.path = path;
            this.schema = schema;
        }
    }
} 
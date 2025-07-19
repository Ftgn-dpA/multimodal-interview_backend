package com.example.interview.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeParseService {
    // 直接在此处填写你的 appcode
    private static final String APPCODE = "edb28526ddd341bc8ffeda3c27ff4942";
    private static final String URL = "http://resumesdk.market.alicloudapi.com/ResumeParser";

    public String parseResume(MultipartFile file) throws Exception {
        HttpPost httpPost = new HttpPost(URL);
        httpPost.setHeader("Authorization", "APPCODE " + APPCODE);
        httpPost.addHeader("Content-Type", "application/json");

        byte[] bytes = file.getBytes();
        String data = new String(Base64.encodeBase64(bytes), Consts.UTF_8);

        JSONObject json = new JSONObject();
        json.put("file_name", file.getOriginalFilename());
        json.put("file_cont", data);
        json.put("need_avatar", 0);

        StringEntity params = new StringEntity(json.toString(), Consts.UTF_8);
        httpPost.setEntity(params);

        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(httpPost);
        String resCont = EntityUtils.toString(response.getEntity(), Consts.UTF_8);
        JSONObject res = new JSONObject(resCont);
        JSONObject status = res.getJSONObject("status");
        if (status.getInt("code") != 200) {
            throw new RuntimeException("request failed: code=" + status.getInt("code") + ", message=" + status.getString("message"));
        }
        JSONObject result = res.getJSONObject("result");
        String education = result.optString("cont_education", "");
        String projExp = result.optString("cont_proj_exp", "");
        String jobSkill = result.optString("cont_job_skill", "");
        return "教育经历为：" + education + "\n项目经验为：" + projExp + "\n工作技能为：" + jobSkill;
    }
} 
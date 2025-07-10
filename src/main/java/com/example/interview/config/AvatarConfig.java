package com.example.interview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AvatarConfig {
    @Value("${avatar.app_id}")
    public String appId;

    @Value("${avatar.api_key}")
    public String apiKey;

    @Value("${avatar.api_secret}")
    public String apiSecret;

    @Value("${avatar.avatar_id}")
    public String avatarId;

    @Value("${avatar.vcn}")
    public String vcn;
} 
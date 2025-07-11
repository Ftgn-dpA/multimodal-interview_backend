package com.example.interview.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);

    @Autowired
    private VideoStorageConfig videoStorageConfig;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String videoPath = videoStorageConfig.getStorage().getPath();
        String urlPrefix = videoStorageConfig.getAccessUrlPrefix();
        String resourceLocation = "file:/" + videoPath.replace("\\", "/");
        if (!resourceLocation.endsWith("/")) resourceLocation += "/";
        logger.info("[WebMvcConfig] 静态资源映射: {}** -> {}", urlPrefix, resourceLocation);
        registry.addResourceHandler(urlPrefix + "**")
                .addResourceLocations(resourceLocation);
    }
} 
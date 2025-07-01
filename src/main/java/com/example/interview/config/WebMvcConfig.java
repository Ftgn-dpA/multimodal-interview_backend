package com.example.interview.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private VideoStorageConfig videoStorageConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String videoPath = videoStorageConfig.getStorage().getPath();
        String urlPrefix = videoStorageConfig.getAccessUrlPrefix();
        String resourceLocation = "file:/" + videoPath.replace("\\", "/");
        if (!resourceLocation.endsWith("/")) resourceLocation += "/";
        System.out.println("[WebMvcConfig] 静态资源映射: " + urlPrefix + "** -> " + resourceLocation);
        registry.addResourceHandler(urlPrefix + "**")
                .addResourceLocations(resourceLocation);
    }
} 
package com.example.interview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "video")
public class VideoStorageConfig {
    private Storage storage;
    private String accessUrlPrefix;

    public static class Storage {
        private String path;
        private String maxSize;
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getMaxSize() { return maxSize; }
        public void setMaxSize(String maxSize) { this.maxSize = maxSize; }
    }
    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }
    public String getAccessUrlPrefix() { return accessUrlPrefix; }
    public void setAccessUrlPrefix(String accessUrlPrefix) { this.accessUrlPrefix = accessUrlPrefix; }
} 
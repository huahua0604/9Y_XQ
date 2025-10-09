package com.hospital.req.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="app")
@Getter
public class AppProps {
    private String uploadDir;
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
}

// src/main/java/com/example/backend/config/ResourceConfig.java
package com.example.backend.review.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 실제 업로드된 파일 경로
        String uploadPath = System.getProperty("user.dir") + "/uploads/";

        // 브라우저에서 /uploads/** 로 접근 가능하도록 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}

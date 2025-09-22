package com.example.backend.authlogin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "file:.env", ignoreResourceNotFound = true)
public class EnvConfig {
    // .env 파일을 명시적으로 로드하는 설정 클래스
}
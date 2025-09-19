package com.example.backend.authlogin.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Component
public class DotEnvLoader implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(".env"));
            
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
            
            environment.getPropertySources().addLast(new PropertiesPropertySource("dotenv", props));
            
        } catch (IOException e) {
            System.out.println("Warning: .env file not found or cannot be read: " + e.getMessage());
        }
    }
}

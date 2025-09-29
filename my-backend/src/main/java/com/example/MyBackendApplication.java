// src/main/java/com/example/MyBackendApplication.java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.backend")
public class MyBackendApplication {
  public static void main(String[] args) {
    SpringApplication.run(MyBackendApplication.class, args);
  }
}

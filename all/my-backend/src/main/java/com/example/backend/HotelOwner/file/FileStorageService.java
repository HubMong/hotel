// src/main/java/com/example/backend/HotelOwner/file/FileStorageService.java
package com.example.backend.HotelOwner.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file);
    void deleteFile(String urlOrPath);
}

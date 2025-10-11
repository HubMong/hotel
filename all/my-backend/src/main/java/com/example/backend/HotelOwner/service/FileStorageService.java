package com.example.backend.HotelOwner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    // application.properties 에서 파일 저장 경로를 주입받습니다.
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("파일을 업로드할 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    /**
     * 파일을 저장하고 웹 접근 경로를 반환합니다.
     * @param file 저장할 MultipartFile
     * @return 웹에서 접근 가능한 파일 경로 (예: /uploads/filename.jpg)
     */
    public String storeFile(MultipartFile file) {
        // 파일 이름이 중복되지 않도록 UUID를 사용합니다.
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        try {
            // 파일 이름에 유효하지 않은 문자가 있는지 확인합니다.
            if (fileName.contains("..")) {
                throw new RuntimeException("파일 이름에 유효하지 않은 경로 시퀀스가 포함되어 있습니다: " + fileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 데이터베이스나 프론트엔드에서 사용할 경로를 반환합니다.
            // WebConfig 설정에 따라 /uploads/** 경로로 외부에서 접근 가능해야 합니다.
            return "/uploads/" + fileName;

        } catch (IOException ex) {
            throw new RuntimeException("파일 " + fileName + "을(를) 저장할 수 없습니다. 다시 시도해 주세요.", ex);
        }
    }
}
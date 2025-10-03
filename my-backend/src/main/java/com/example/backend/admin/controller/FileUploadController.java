package com.example.backend.admin.controller;

import com.example.backend.admin.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class FileUploadController {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.upload.url:http://localhost:8080/uploads}")
    private String uploadUrl;

    @PostMapping("/room-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadRoomImage(
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("파일이 비어있습니다."));
        }

        // 파일 크기 체크 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("파일 크기는 5MB 이하여야 합니다."));
        }

        // 이미지 파일 타입 체크
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("이미지 파일만 업로드 가능합니다."));
        }

        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir, "rooms");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 파일명 생성 (UUID + 원본 확장자)
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path targetPath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // URL 생성
            String fileUrl = uploadUrl + "/rooms/" + filename;

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", filename);

            log.info("Room image uploaded successfully: {}", fileUrl);
            return ResponseEntity.ok(ApiResponse.ok(result));

        } catch (IOException e) {
            log.error("Failed to upload room image", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("파일 업로드에 실패했습니다: " + e.getMessage()));
        }
    }
}

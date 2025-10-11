// src/main/java/com/example/backend/HotelOwner/file/LocalFileStorageService.java
package com.example.backend.HotelOwner.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.file.*;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path rootDir = Paths.get("uploads"); // 프로젝트 루트 기준 ./uploads

    @Override
    public String storeFile(MultipartFile file) {
        try {
            if (!Files.exists(rootDir)) Files.createDirectories(rootDir);
            String original = file.getOriginalFilename();
            String clean = (original == null ? "file" : original).replaceAll("[\\\\/]+", "_");
            String filename = System.currentTimeMillis() + "_" + clean;
            Path target = rootDir.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            // 정적 리소스 매핑이 /uploads/** 로 되어있다고 가정
            return "/uploads/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    @Override
    public void deleteFile(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) return;
        try {
            String candidate = urlOrPath.trim();

            // http(s) URL이면 path 부분만 추출
            if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
                try {
                    candidate = new URI(candidate).getPath();
                } catch (Exception ignore) {}
            }

            // "/uploads/xxx" 혹은 "uploads/xxx" → 로컬 경로로 변환
            if (candidate.contains("/uploads/")) {
                String fn = candidate.substring(candidate.lastIndexOf("/uploads/") + "/uploads/".length());
                Path p = rootDir.resolve(fn).normalize();
                Files.deleteIfExists(p);
                return;
            }
            if (candidate.startsWith("/uploads/")) {
                String fn = candidate.substring("/uploads/".length());
                Path p = rootDir.resolve(fn).normalize();
                Files.deleteIfExists(p);
                return;
            }
            if (candidate.startsWith("uploads/")) {
                Path p = rootDir.resolve(candidate.substring("uploads/".length())).normalize();
                Files.deleteIfExists(p);
                return;
            }

            // 그 외: 절대/상대 경로 모두 시도
            Path p = Paths.get(candidate);
            if (!p.isAbsolute()) {
                p = rootDir.resolve(candidate).normalize();
            }
            Files.deleteIfExists(p);
        } catch (Exception ignore) {
            // 파일이 실제 없거나 접근 불가해도 서비스는 계속 진행
        }
    }
}

package com.example.backend.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.admin.dto.ApiResponse;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/health")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class HealthController {

    private final DataSource dataSource;

    @GetMapping("/db")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkDatabase() {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // 기본 연결 테스트
            result.put("connected", true);
            result.put("url", conn.getMetaData().getURL());
            result.put("driver", conn.getMetaData().getDriverName());
            
            // 간단한 쿼리 테스트
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT 1 as test, NOW() as current_time");
                if (rs.next()) {
                    result.put("query_test", rs.getInt("test"));
                    result.put("db_time", rs.getTimestamp("current_time"));
                }
            }
            
            // 테이블 존재 확인
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM Hotel");
                if (rs.next()) {
                    result.put("hotel_count", rs.getInt("count"));
                }
            }
            
            result.put("status", "healthy");
            return ResponseEntity.ok(ApiResponse.ok(result));
            
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
            result.put("status", "unhealthy");
            return ResponseEntity.status(500).body(ApiResponse.fail("Database connection failed"));
        }
    }
}


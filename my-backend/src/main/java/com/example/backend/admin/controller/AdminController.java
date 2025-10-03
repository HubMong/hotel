package com.example.backend.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.admin.dto.ApiResponse;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {
	@GetMapping("/ping")
	public ResponseEntity<ApiResponse<String>> ping() {
		return ResponseEntity.ok(ApiResponse.ok("admin-api-ok"));
	}
	
	@GetMapping("/test")
	public ResponseEntity<ApiResponse<String>> test() {
		return ResponseEntity.ok(ApiResponse.ok("admin-test-success"));
	}
}
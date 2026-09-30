package com.inventory.inventory_service.Controller;

import com.inventory.inventory_service.Dto.ApiResponse;
import com.inventory.inventory_service.Dto.AuthResponse;
import com.inventory.inventory_service.Dto.LoginRequest;
import com.inventory.inventory_service.Dto.RefreshTokenRequest;
import com.inventory.inventory_service.Dto.UserSummaryDto;
import com.inventory.inventory_service.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.inventory.inventory_service.Dto.UserProvisioningRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${application.security.provisioning-key:}")
    private String provisioningKey;

    @PostMapping("/provision")
    public ResponseEntity<ApiResponse<AuthResponse>> provision(
            @RequestHeader(value = "X-Provisioning-Key", required = false) String suppliedKey,
            @Valid @RequestBody UserProvisioningRequest request) {
        if (provisioningKey == null || provisioningKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User provisioning is not configured");
        }
        if (suppliedKey == null || !MessageDigest.isEqual(
                provisioningKey.getBytes(StandardCharsets.UTF_8), suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid provisioning key");
        }
        AuthResponse response = authService.provisionUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User provisioned successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummaryDto>> getCurrentUser() {
        UserSummaryDto user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved", user));
    }
}

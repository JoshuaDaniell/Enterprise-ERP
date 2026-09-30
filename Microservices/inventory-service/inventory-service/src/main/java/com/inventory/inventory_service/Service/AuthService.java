package com.inventory.inventory_service.Service;

import com.inventory.inventory_service.Dto.AuthResponse;
import com.inventory.inventory_service.Dto.LoginRequest;
import com.inventory.inventory_service.Dto.RefreshTokenRequest;
import com.inventory.inventory_service.Dto.UserProvisioningRequest;
import com.inventory.inventory_service.Dto.UserSummaryDto;
import com.inventory.inventory_service.Entity.RefreshToken;
import com.inventory.inventory_service.Entity.Role;
import com.inventory.inventory_service.Entity.User;
import com.inventory.inventory_service.Exception.DuplicateResourceException;
import com.inventory.inventory_service.Repository.UserRepository;
import com.inventory.inventory_service.Security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse provisionUser(UserProvisioningRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username '" + username + "' is already taken");
        }
        String email = request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail().trim().toLowerCase()
                : username.toLowerCase() + "@enterprise-erp.com";

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email '" + email + "' is already registered");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", savedUser.getRole().name());
        extraClaims.put("email", savedUser.getEmail());

        String accessToken = jwtService.generateToken(extraClaims, savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        log.info("Provisioned new user '{}' with role '{}'", savedUser.getUsername(), savedUser.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getUsername()));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("email", user.getEmail());

        String accessToken = jwtService.generateToken(extraClaims, user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User '{}' successfully authenticated", user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken newRefreshToken = refreshTokenService.verifyAndRotate(request.getRefreshToken());
        User user = newRefreshToken.getUser();

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("email", user.getEmail());

        String newAccessToken = jwtService.generateToken(extraClaims, user);

        log.info("Refreshed tokens for user '{}'", user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            refreshTokenService.revokeByToken(request.getRefreshToken());
        }
    }

    public UserSummaryDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("No authenticated user found in current session");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return UserSummaryDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

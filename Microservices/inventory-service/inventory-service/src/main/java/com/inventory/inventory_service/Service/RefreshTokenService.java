package com.inventory.inventory_service.Service;

import com.inventory.inventory_service.Entity.RefreshToken;
import com.inventory.inventory_service.Entity.User;
import com.inventory.inventory_service.Exception.ResourceNotFoundException;
import com.inventory.inventory_service.Repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${application.security.jwt.refresh-token.expiration:604800000}") // 7 days in ms
    private long refreshTokenDurationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke any previous refresh tokens for the user to ensure single-device / rotation hygiene
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for user '{}' expiring at {}", user.getUsername(), saved.getExpiryDate());
        return saved;
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid refresh token"));
    }

    @Transactional
    public RefreshToken verifyAndRotate(String tokenStr) {
        RefreshToken token = findByToken(tokenStr);

        if (token.isRevoked() || token.isExpired()) {
            refreshTokenRepository.delete(token);
            log.warn("Attempted use of expired or revoked refresh token for user '{}'", token.getUser().getUsername());
            throw new RuntimeException("Refresh token is expired or revoked. Please sign in again.");
        }

        // Mark old token revoked and issue new rotating token
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = token.getUser();
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(newRefreshToken);
    }

    @Transactional
    public void revokeUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("Revoked all refresh tokens for user '{}'", user.getUsername());
    }

    @Transactional
    public void revokeByToken(String tokenStr) {
        refreshTokenRepository.findByToken(tokenStr).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Revoked refresh token for user '{}'", token.getUser().getUsername());
        });
    }
}

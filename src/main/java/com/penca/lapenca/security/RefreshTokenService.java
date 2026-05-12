package com.penca.lapenca.security;

import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.entity.RefreshToken;
import com.penca.lapenca.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public RefreshToken createRefreshToken(AppUser user) {
        refreshTokenRepository.deleteByUser(user);

        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(14));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    public AppUser validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken.getUser();
    }

    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }
}
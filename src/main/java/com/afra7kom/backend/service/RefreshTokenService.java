package com.afra7kom.backend.service;

import com.afra7kom.backend.entity.RefreshToken;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import com.afra7kom.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenDurationMs;

    public RefreshToken createRefreshToken(User user) {
        // Révoquer tous les anciens tokens de l'utilisateur
        refreshTokenRepository.revokeAllByUser(user);

        // Créer un nouveau token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000));

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new BadRequestException("Refresh token was expired. Please make a new signin request");
        }

        if (token.getRevoked()) {
            throw new BadRequestException("Refresh token was revoked. Please make a new signin request");
        }

        return token;
    }

    public void revokeToken(String token) {
        refreshTokenRepository.revokeByToken(token);
    }

    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Transactional(readOnly = true)
    public List<RefreshToken> getValidTokensByUser(User user) {
        return refreshTokenRepository.findValidTokensByUser(user, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long countValidTokensByUser(User user) {
        return refreshTokenRepository.countValidTokensByUser(user, LocalDateTime.now());
    }

    // Nettoyage automatique des tokens expirés (toutes les heures)
    @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 ms
    public void cleanupExpiredTokens() {
        int deletedCount = refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
        if (deletedCount > 0) {
            System.out.println("Cleaned up " + deletedCount + " expired/revoked refresh tokens");
        }
    }
}




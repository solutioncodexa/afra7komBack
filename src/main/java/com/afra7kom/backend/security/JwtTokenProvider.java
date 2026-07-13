package com.afra7kom.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private Key signingKey;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private int jwtRefreshExpirationMs;

    @Value("${jwt.key-file:jwt.key}")
    private String keyFile;

    @PostConstruct
    public void init() {
        this.signingKey = loadOrCreateSigningKey();
    }

    public JwtTokenProvider() {
        this.signingKey = null;
    }

    private Key loadOrCreateSigningKey() {
        File file = new File(keyFile);
        if (file.exists()) {
            try {
                byte[] keyBytes = Files.readAllBytes(file.toPath());
                return Keys.hmacShaKeyFor(keyBytes);
            } catch (IOException e) {
                logger.warn("Could not read JWT key file. Creating new key.", e);
            }
        }

        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        try {
            Path keyPath = file.toPath();
            Path parent = keyPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(keyPath, key.getEncoded());
        } catch (IOException e) {
            logger.warn("Could not save JWT key file.", e);
        }
        return key;
    }

    private Key getSigningKey() {
        return signingKey;
    }

    public String generateAccessToken(Authentication authentication) {
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return generateToken(username, jwtExpirationMs);
    }

    public String generateRefreshToken(Authentication authentication) {
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return generateToken(username, jwtRefreshExpirationMs);
    }

    public Long getExpirationTime() {
        return (long) jwtExpirationMs;
    }

    private String generateToken(String email, int expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }
    
    // Méthode de compatibilité - retourne l'email (qui sert de username)
    public String getUsernameFromToken(String token) {
        return getEmailFromToken(token);
    }

    public boolean validateToken(String authToken) {
        try {
            logger.debug("JWT Token Provider - Validating token: {}", authToken.substring(0, Math.min(20, authToken.length())) + "...");
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            logger.debug("JWT Token Provider - Token validation successful");
            return true;
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error validating JWT token: {}", ex.getMessage());
        }
        return false;
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getExpiration();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
}

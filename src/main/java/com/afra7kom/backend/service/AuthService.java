package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.AuthRequest;
import com.afra7kom.backend.dto.AuthResponse;
import com.afra7kom.backend.dto.RefreshTokenRequest;
import com.afra7kom.backend.dto.RegisterRequest;
import com.afra7kom.backend.dto.ConfirmResetPasswordRequest;
import com.afra7kom.backend.dto.ResetPasswordRequest;
import com.afra7kom.backend.entity.RefreshToken;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.afra7kom.backend.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final EmailService emailService;

    public AuthService(AuthenticationManager authenticationManager,
                      JwtTokenProvider tokenProvider,
                      UserService userService,
                      RefreshTokenService refreshTokenService,
                      AuditService auditService,
                      PasswordEncoder passwordEncoder,
                      SecurityUtils securityUtils,
                      EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.securityUtils = securityUtils;
        this.emailService = emailService;
    }

    public AuthResponse login(AuthRequest request) {
        try {
            // Récupérer l'utilisateur depuis la base de données
            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadRequestException("Invalid email or password"));
            
            // Vérifier le mot de passe
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new BadRequestException("Invalid email or password");
            }
            
            // Vérifier que l'utilisateur est activé
            if (!user.isEnabled()) {
                throw new BadRequestException("Account is disabled");
            }

            // Créer l'authentification
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Générer les tokens
            String accessToken = tokenProvider.generateAccessToken(authentication);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            // Créer la réponse
            AuthResponse.UserDto userDto = new AuthResponse.UserDto();
            userDto.setId(user.getId());
            userDto.setEmail(user.getEmail());
            userDto.setPhone(user.getPhone());
            userDto.setEnabled(user.getEnabled());
            userDto.setRoles(user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toSet()));
            userDto.setPermissions(user.getPermissionNames());
            userDto.setCreatedAt(user.getCreatedAt());

            // Audit log
            auditService.createLog(
                user,
                "LOGIN_SUCCESS",
                "Login successful",
                securityUtils.getCurrentIpAddress()
            );

            return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                tokenProvider.getExpirationTime(),
                userDto
            );

        } catch (Exception e) {
            // Audit log pour échec de connexion
            auditService.createLog(
                null, // Pas d'utilisateur pour un échec de connexion
                "LOGIN_FAILURE",
                "Login failed: " + e.getMessage(),
                securityUtils.getCurrentIpAddress()
            );
            throw new BadRequestException("Invalid email or password");
        }
    }

    public AuthResponse register(RegisterRequest request) {
        // Créer l'utilisateur
        User user = userService.createUser(request);

        // Créer l'authentification
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(), null, user.getAuthorities());

        // Générer les tokens
        String accessToken = tokenProvider.generateAccessToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // Créer la réponse
        AuthResponse.UserDto userDto = new AuthResponse.UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setPhone(user.getPhone());
        userDto.setEnabled(user.getEnabled());
        userDto.setRoles(user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet()));
        userDto.setPermissions(user.getPermissionNames());
        userDto.setCreatedAt(user.getCreatedAt());

        return new AuthResponse(
            accessToken,
            refreshToken.getToken(),
            tokenProvider.getExpirationTime(),
            userDto
        );
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken);
        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        // Créer une nouvelle authentification
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(), null, user.getAuthorities());

        // Générer un nouveau access token
        String accessToken = tokenProvider.generateAccessToken(authentication);

        // Créer un nouveau refresh token
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        // Créer la réponse
        AuthResponse.UserDto userDto = new AuthResponse.UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setPhone(user.getPhone());
        userDto.setEnabled(user.getEnabled());
        userDto.setRoles(user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet()));
        userDto.setPermissions(user.getPermissionNames());
        userDto.setCreatedAt(user.getCreatedAt());

        return new AuthResponse(
            accessToken,
            newRefreshToken.getToken(),
            tokenProvider.getExpirationTime(),
            userDto
        );
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revokeToken(refreshToken);
        }

        // Audit log
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = securityUtils.getCurrentUser().orElse(null);
            auditService.createLog(
                currentUser,
                "LOGOUT",
                "User logged out",
                securityUtils.getCurrentIpAddress()
            );
        }

        SecurityContextHolder.clearContext();
    }

    public void requestPasswordReset(ResetPasswordRequest request) {
        userService.findByEmail(request.getEmail()).ifPresentOrElse(
            user -> {
                String token = userService.assignPasswordResetToken(user);
                emailService.sendPasswordResetEmail(user, token);
            },
            () -> log.info("Password reset requested for unknown email (ignored): {}", request.getEmail())
        );
    }

    public void confirmPasswordReset(ConfirmResetPasswordRequest request) {
        if (!request.isPasswordMatching()) {
            throw new BadRequestException("Les mots de passe ne correspondent pas");
        }

        User user = userService.resetPassword(request.getToken(), request.getNewPassword());
        emailService.sendPasswordChangeNotification(user);
    }
}

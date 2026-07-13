package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.*;
import com.afra7kom.backend.service.AuthService;
import com.afra7kom.backend.service.UserService;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.exception.BadRequestException;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get the currently authenticated user's information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<AuthResponse.UserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        // Récupérer l'utilisateur depuis la base de données avec JOIN FETCH
        User user = userService.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        
        AuthResponse.UserDto userDto = new AuthResponse.UserDto(
            user.getId(),
            user.getEmail(),
            user.getPhone(),
            user.isEnabled(),
            user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()),
            user.getPermissionNames(),
            user.getCreatedAt()
        );
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Invalid credentials"),
        @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get new access token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token"),
        @ApiResponse(responseCode = "401", description = "Refresh token not found")
    })
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Revoke refresh token and logout user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful")
    })
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(refreshToken);
        return ResponseEntity.ok(Collections.singletonMap("message", "Logout successful"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Request password reset", description = "Send password reset token to user email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset email sent if account exists")
    })
    public ResponseEntity<Map<String, String>> requestPasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(Collections.singletonMap(
            "message",
            "Si un compte existe avec cet email, un lien de réinitialisation a été envoyé."
        ));
    }

    @PostMapping("/confirm-reset-password")
    @Operation(summary = "Confirm password reset", description = "Set a new password using the reset token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successful"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public ResponseEntity<Map<String, String>> confirmPasswordReset(
            @Valid @RequestBody ConfirmResetPasswordRequest request) {
        if (!request.isPasswordMatching()) {
            throw new BadRequestException("Les mots de passe ne correspondent pas");
        }
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(Collections.singletonMap(
            "message",
            "Mot de passe réinitialisé avec succès"
        ));
    }
}




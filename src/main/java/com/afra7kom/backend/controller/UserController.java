package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.ChangePasswordRequest;
import com.afra7kom.backend.dto.UpdateUserRoleRequest;
import com.afra7kom.backend.dto.UserDto;
import com.afra7kom.backend.entity.Role;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.service.EmailService;
import com.afra7kom.backend.service.UserService;
import com.afra7kom.backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "User management APIs")
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve paginated list of users with optional filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @Parameter(description = "Email filter") @RequestParam(required = false) String email,
            @Parameter(description = "Phone filter") @RequestParam(required = false) String phone,
            @Parameter(description = "Enabled filter") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Role filter") @RequestParam(required = false) Role.RoleName role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<UserDto> users;
        
        if (role != null) {
            users = userService.getUsersByRole(role, pageable);
        } else if (email != null || phone != null || enabled != null) {
            users = userService.searchUsers(email, phone, enabled, pageable);
        } else {
            users = userService.getAllUsers(pageable);
        }

        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve user details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or #id == authentication.principal.id")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(UserDto.fromUser(user));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user roles", description = "Update user roles (Admin/Manager only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User roles updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "400", description = "Invalid role data")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        
        User updatedUser = userService.updateUserRoles(id, request);
        return ResponseEntity.ok(UserDto.fromUser(updatedUser));
    }

    @PatchMapping("/{id}/enable")
    @Operation(summary = "Enable user", description = "Enable user account (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User enabled successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> enableUser(@PathVariable Long id) {
        User user = userService.enableUser(id);
        return ResponseEntity.ok(UserDto.fromUser(user));
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable user", description = "Disable user account (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User disabled successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> disableUser(@PathVariable Long id) {
        User user = userService.disableUser(id);
        return ResponseEntity.ok(UserDto.fromUser(user));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user statistics", description = "Get user count by roles (Admin/Manager only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        Map<String, Object> stats = Map.of(
            "totalUsers", userService.getAllUsers(Pageable.unpaged()).getTotalElements(),
            "adminCount", userService.countUsersByRole(Role.RoleName.ADMIN),
            "managerCount", userService.countUsersByRole(Role.RoleName.MANAGER),
            "agentCount", userService.countUsersByRole(Role.RoleName.AGENT),
            "clientCount", userService.countUsersByRole(Role.RoleName.CLIENT)
        );
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change the current user's password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid password data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        // Vérifier que les mots de passe correspondent
        if (!request.isPasswordMatching()) {
            throw new BadRequestException("Les mots de passe ne correspondent pas");
        }

        // Récupérer l'utilisateur courant
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new BadRequestException("Utilisateur non authentifié"));

        // Changer le mot de passe
        userService.changePassword(currentUser, request.getOldPassword(), request.getNewPassword());

        // Envoyer un email de notification de manière asynchrone (ne bloque pas la réponse)
        // L'email sera envoyé en arrière-plan
        emailService.sendPasswordChangeNotification(currentUser);

        return ResponseEntity.ok(Map.of(
            "message", "Mot de passe modifié avec succès",
            "email", currentUser.getEmail()
        ));
    }
}




package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.RegisterRequest;
import com.afra7kom.backend.dto.UpdateUserRoleRequest;
import com.afra7kom.backend.dto.UserDto;
import com.afra7kom.backend.entity.Role;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.repository.RoleRepository;
import com.afra7kom.backend.repository.UserRepository;
import com.afra7kom.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import com.afra7kom.backend.service.ReservationNotificationService;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final ReservationNotificationService reservationNotificationService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmailWithRolesAndPermissions(String email) {
        return userRepository.findByEmailWithRolesAndPermissions(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User createUser(RegisterRequest request) {
        return createUserWithRole(request, Role.RoleName.CLIENT);
    }

    public User createUserWithRole(RegisterRequest request, Role.RoleName roleName) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already taken!");
        }

        // Vérifier que les mots de passe correspondent si présents
        if (request.getPassword() != null && request.getConfirmPassword() != null) {
            if (!request.isPasswordMatching()) {
                throw new BadRequestException("Passwords do not match!");
            }
        }

        // Créer l'utilisateur
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        // Attribuer le rôle spécifié
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
        user.addRole(role);

        User savedUser = userRepository.save(user);
        
        // Audit log
        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "USER_CREATE",
            String.format("User created: %s with role %s", savedUser.getEmail(), roleName),
            securityUtils.getCurrentIpAddress()
        );

        // Envoyer notifications aux admins
        try {
            notifyAdminsOfNewUser(savedUser);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications aux admins pour le nouvel utilisateur", e);
            // Ne pas faire échouer la création d'utilisateur si les notifications échouent
        }

        return savedUser;
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserDto::fromUser);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getUsersByRole(Role.RoleName roleName, Pageable pageable) {
        return userRepository.findByRoleName(roleName, pageable)
                .map(UserDto::fromUser);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String email, String phone, Boolean enabled, Pageable pageable) {
        return userRepository.findWithFilters(email, phone, enabled, pageable)
                .map(UserDto::fromUser);
    }

    public User updateUserRoles(Long userId, UpdateUserRoleRequest request) {
        User user = findById(userId);
        
        // Vérifier que les rôles existent
        Set<Role> newRoles = request.getRoleNames().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        // Sauvegarder les anciens rôles pour l'audit
        Set<String> oldRoles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        // Mettre à jour les rôles
        user.getRoles().clear();
        newRoles.forEach(user::addRole);

        User savedUser = userRepository.save(user);

        // Audit log
        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "USER_UPDATE",
            String.format("User roles updated from %s to %s", oldRoles, request.getRoleNames()),
            securityUtils.getCurrentIpAddress()
        );

        // Envoyer notifications aux admins pour le changement de rôle
        try {
            Set<String> newRoleNames = request.getRoleNames().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            notifyAdminsOfUserRoleChange(savedUser, oldRoles, newRoleNames);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications aux admins pour le changement de rôle", e);
            // Ne pas faire échouer la mise à jour si les notifications échouent
        }

        return savedUser;
    }

    public User enableUser(Long userId) {
        User user = findById(userId);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "USER_UPDATE",
            "User enabled",
            securityUtils.getCurrentIpAddress()
        );

        return savedUser;
    }

    public User disableUser(Long userId) {
        User user = findById(userId);
        user.setEnabled(false);
        User savedUser = userRepository.save(user);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "USER_UPDATE",
            "User disabled",
            securityUtils.getCurrentIpAddress()
        );

        return savedUser;
    }

    public User toggleUserStatus(Long userId) {
        User user = findById(userId);
        user.setEnabled(!user.isEnabled());
        User savedUser = userRepository.save(user);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "USER_UPDATE",
            String.format("User %s", user.isEnabled() ? "enabled" : "disabled"),
            securityUtils.getCurrentIpAddress()
        );

        return savedUser;
    }

    public void deleteUser(Long userId) {
        User user = findById(userId);
        
        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "USER_DELETE",
            String.format("User deleted: %s", user.getEmail()),
            securityUtils.getCurrentIpAddress()
        );

        userRepository.delete(user);
    }

    public User updateUser(Long userId, Map<String, Object> updates) {
        User user = findById(userId);
        
        if (updates.containsKey("email")) {
            String newEmail = (String) updates.get("email");
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new BadRequestException("Email is already taken!");
            }
            user.setEmail(newEmail);
        }
        
        if (updates.containsKey("phone")) {
            user.setPhone((String) updates.get("phone"));
        }
        
        if (updates.containsKey("enabled")) {
            user.setEnabled((Boolean) updates.get("enabled"));
        }

        User savedUser = userRepository.save(user);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "USER_UPDATE",
            "User information updated",
            securityUtils.getCurrentIpAddress()
        );

        return savedUser;
    }

    public UserDto convertToDto(User user) {
        return UserDto.fromUser(user);
    }

    public Optional<String> createPasswordResetToken(String email) {
        return userRepository.findByEmail(email).map(this::assignPasswordResetToken);
    }

    public String assignPasswordResetToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "USER_UPDATE",
            "Password reset token generated",
            securityUtils.getCurrentIpAddress()
        );

        return token;
    }

    public String generatePasswordResetToken(String email) {
        return createPasswordResetToken(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public User resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (!user.isPasswordResetTokenValid()) {
            throw new BadRequestException("Password reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.clearPasswordResetToken();
        userRepository.save(user);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "USER_UPDATE",
            "Password reset completed",
            securityUtils.getCurrentIpAddress()
        );

        return user;
    }

    @Transactional(readOnly = true)
    public long countUsersByRole(Role.RoleName roleName) {
        return userRepository.countByRoleName(roleName);
    }

    @Transactional(readOnly = true)
    public List<User> getRecentUsers(LocalDateTime since) {
        return userRepository.findRecentUsers(since);
    }
    
    public void changePassword(User user, String oldPassword, String newPassword) {
        // Vérifier que l'ancien mot de passe est correct
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BadRequestException("L'ancien mot de passe est incorrect");
        }

        // Mettre à jour le mot de passe
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditService.createLog(
            user,
            "PASSWORD_CHANGE",
            "Mot de passe modifié avec succès",
            securityUtils.getCurrentIpAddress()
        );

        log.info("Mot de passe modifié pour l'utilisateur: {}", user.getEmail());
    }
    
    /**
     * Notifier tous les admins d'un nouvel utilisateur inscrit
     */
    private void notifyAdminsOfNewUser(User newUser) {
        try {
            // Trouver tous les utilisateurs avec les rôles ADMIN et AGENT
            List<User> adminUsers = userRepository.findByRoleName(Role.RoleName.ADMIN, PageRequest.of(0, 100))
                    .getContent();
            List<User> agentUsers = userRepository.findByRoleName(Role.RoleName.AGENT, PageRequest.of(0, 100))
                    .getContent();
            
            // Combiner les deux listes
            List<User> allNotificationUsers = new ArrayList<>();
            allNotificationUsers.addAll(adminUsers);
            allNotificationUsers.addAll(agentUsers);
            
            if (allNotificationUsers.isEmpty()) {
                log.warn("Aucun utilisateur admin ou agent trouvé pour recevoir les notifications de nouvel utilisateur");
                return;
            }
            
            // Créer le nom de l'utilisateur
            String userName = newUser.getEmail();
            if (newUser.getFirstName() != null && newUser.getLastName() != null) {
                userName = newUser.getFirstName() + " " + newUser.getLastName();
            }
            
            // Envoyer une notification à chaque utilisateur (admin et agent)
            for (User user : allNotificationUsers) {
                try {
                    reservationNotificationService.createSystemNotificationForTest(
                        user.getId(),
                        "Nouvel utilisateur inscrit",
                        String.format("Un nouvel utilisateur s'est inscrit: %s (%s)", userName, newUser.getEmail())
                    );
                    log.info("Notification de nouvel utilisateur envoyée à l'utilisateur {} pour l'utilisateur {}", 
                            user.getEmail(), newUser.getEmail());
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi de notification à l'utilisateur {}: {}", 
                            user.getEmail(), e.getMessage());
                }
            }
            
            log.info("Notifications de nouvel utilisateur envoyées à {} utilisateur(s) (admin + agent)", allNotificationUsers.size());
            
        } catch (Exception e) {
            log.error("Erreur lors de la notification des admins pour le nouvel utilisateur: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Notifier tous les admins d'un changement de rôle d'utilisateur
     */
    private void notifyAdminsOfUserRoleChange(User user, Set<String> oldRoles, Set<String> newRoles) {
        try {
            // Trouver tous les utilisateurs avec les rôles ADMIN et AGENT
            List<User> adminUsers = userRepository.findByRoleName(Role.RoleName.ADMIN, PageRequest.of(0, 100))
                    .getContent();
            List<User> agentUsers = userRepository.findByRoleName(Role.RoleName.AGENT, PageRequest.of(0, 100))
                    .getContent();
            
            // Combiner les deux listes
            List<User> allNotificationUsers = new ArrayList<>();
            allNotificationUsers.addAll(adminUsers);
            allNotificationUsers.addAll(agentUsers);
            
            if (allNotificationUsers.isEmpty()) {
                log.warn("Aucun utilisateur admin ou agent trouvé pour recevoir les notifications de changement de rôle");
                return;
            }
            
            // Créer le nom de l'utilisateur
            String userName = user.getEmail();
            if (user.getFirstName() != null && user.getLastName() != null) {
                userName = user.getFirstName() + " " + user.getLastName();
            }
            
            // Envoyer une notification à chaque utilisateur (admin et agent)
            for (User notificationUser : allNotificationUsers) {
                try {
                    reservationNotificationService.createSystemNotificationForTest(
                        notificationUser.getId(),
                        "Changement de rôle utilisateur",
                        String.format("Les rôles de %s (%s) ont été modifiés de %s vers %s", 
                                userName, user.getEmail(), oldRoles, newRoles)
                    );
                    log.info("Notification de changement de rôle envoyée à l'utilisateur {} pour l'utilisateur {}", 
                            notificationUser.getEmail(), user.getEmail());
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi de notification à l'utilisateur {}: {}", 
                            notificationUser.getEmail(), e.getMessage());
                }
            }
            
            log.info("Notifications de changement de rôle envoyées à {} utilisateur(s) (admin + agent)", allNotificationUsers.size());
            
        } catch (Exception e) {
            log.error("Erreur lors de la notification des admins pour le changement de rôle: {}", e.getMessage(), e);
        }
    }
}




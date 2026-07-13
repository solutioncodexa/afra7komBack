package com.afra7kom.backend.util;

import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.repository.UserRepository;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Récupère l'utilisateur actuellement connecté
     * @return L'utilisateur connecté ou null si non authentifié
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        String userEmail = authentication.getName();
        
        return userRepository.findByEmail(userEmail);
    }

    /**
     * Récupère l'utilisateur actuellement connecté ou lance une exception
     * @return L'utilisateur connecté
     * @throws BadRequestException si l'utilisateur n'est pas authentifié
     * @throws ResourceNotFoundException si l'utilisateur n'est pas trouvé
     */
    public User getCurrentUserOrThrow() {
        return getCurrentUser()
                .orElseThrow(() -> new BadRequestException("Utilisateur non authentifié"));
    }

    /**
     * Récupère l'adresse IP de la requête courante
     * @return L'adresse IP ou null si non disponible
     */
    public String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Essayer différents headers pour l'IP
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_X_FORWARDED_FOR");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_X_FORWARDED");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_CLIENT_IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_FORWARDED_FOR");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_FORWARDED");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_VIA");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("REMOTE_ADDR");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                
                // Si l'IP contient plusieurs adresses (X-Forwarded-For), prendre la première
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                
                return ip;
            }
        } catch (Exception e) {
            // Log l'erreur mais ne pas faire échouer l'application
            System.err.println("Erreur lors de la récupération de l'IP: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Récupère l'User-Agent de la requête courante
     * @return L'User-Agent ou null si non disponible
     */
    public String getCurrentUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de l'User-Agent: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Vérifie si l'utilisateur connecté a un rôle spécifique
     * @param roleName Le nom du rôle à vérifier
     * @return true si l'utilisateur a le rôle, false sinon
     */
    public boolean hasRole(String roleName) {
        return getCurrentUser()
                .map(user -> user.hasAnyRole(roleName))
                .orElse(false);
    }

    /**
     * Vérifie si l'utilisateur connecté a l'un des rôles spécifiés
     * @param roleNames Les noms des rôles à vérifier
     * @return true si l'utilisateur a au moins un des rôles, false sinon
     */
    public boolean hasAnyRole(String... roleNames) {
        return getCurrentUser()
                .map(user -> user.hasAnyRole(roleNames))
                .orElse(false);
    }
}

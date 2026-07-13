package com.afra7kom.backend.service;

import com.afra7kom.backend.entity.AuditLog;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.repository.AuditLogRepository;
import com.afra7kom.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    
    /**
     * Logger une action avec les informations de l'utilisateur et de la requête
     */
    public void logAction(String actionType, String entityType, Long entityId, String description, String userEmail) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setActionType(actionType);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDescription(description);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            // Récupérer l'utilisateur si l'email est fourni
            if (userEmail != null && !userEmail.isEmpty()) {
                userRepository.findByEmail(userEmail).ifPresent(auditLog::setUser);
            }
            
            // Récupérer l'IP et l'User-Agent de la requête
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    auditLog.setIpAddress(getClientIpAddress(request));
                    auditLog.setUserAgent(request.getHeader("User-Agent"));
                }
            } catch (Exception e) {
                log.warn("Impossible de récupérer les informations de la requête pour l'audit", e);
            }
            
            auditLogRepository.save(auditLog);
            log.debug("Action audité: {} - {} - {}", actionType, entityType, description);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création du log d'audit", e);
        }
    }
    
    /**
     * Logger une action avec l'ID de l'utilisateur
     */
    public void logAction(String actionType, String entityType, Long entityId, String description, Long userId) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setActionType(actionType);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDescription(description);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            // Récupérer l'utilisateur par ID
            if (userId != null) {
                userRepository.findById(userId).ifPresent(auditLog::setUser);
            }
            
            // Récupérer l'IP et l'User-Agent de la requête
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    auditLog.setIpAddress(getClientIpAddress(request));
                    auditLog.setUserAgent(request.getHeader("User-Agent"));
                }
            } catch (Exception e) {
                log.warn("Impossible de récupérer les informations de la requête pour l'audit", e);
            }
            
            auditLogRepository.save(auditLog);
            log.debug("Action audité: {} - {} - {}", actionType, entityType, description);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création du log d'audit", e);
        }
    }
    
    /**
     * Logger une action sans utilisateur (pour les actions publiques)
     */
    public void logPublicAction(String actionType, String entityType, Long entityId, String description) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setActionType(actionType);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDescription(description);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            // Récupérer l'IP et l'User-Agent de la requête
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    auditLog.setIpAddress(getClientIpAddress(request));
                    auditLog.setUserAgent(request.getHeader("User-Agent"));
                }
            } catch (Exception e) {
                log.warn("Impossible de récupérer les informations de la requête pour l'audit", e);
            }
            
            auditLogRepository.save(auditLog);
            log.debug("Action publique audité: {} - {} - {}", actionType, entityType, description);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création du log d'audit public", e);
        }
    }
    
    /**
     * Obtenir l'adresse IP réelle du client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}



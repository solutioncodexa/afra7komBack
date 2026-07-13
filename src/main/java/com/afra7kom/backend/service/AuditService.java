package com.afra7kom.backend.service;

import com.afra7kom.backend.entity.AuditLog;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.repository.AuditLogRepository;
import com.afra7kom.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    // Créer un log d'audit
    public AuditLog createLog(User user, String action, String details, String ipAddress) {
        AuditLog log = new AuditLog(user, action, details, ipAddress);
        return auditLogRepository.save(log);
    }

    // Créer un log d'audit avec plus de détails
    public AuditLog createDetailedLog(User user, String action, String details, String ipAddress, 
                                    String resourceType, Long resourceId, String oldValues, String newValues) {
        AuditLog log = new AuditLog(user, action, details, ipAddress);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setOldValues(oldValues);
        log.setNewValues(newValues);
        return auditLogRepository.save(log);
    }

    // Créer un log d'échec
    public AuditLog createFailureLog(User user, String action, String details, String ipAddress) {
        AuditLog log = new AuditLog(user, action, details, ipAddress);
        log.setStatus("FAILURE");
        return auditLogRepository.save(log);
    }

    // Récupérer les logs avec filtres
    public Page<AuditLog> getAuditLogs(Long userId, String action, String status, 
                                      LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        // Utiliser des valeurs par défaut pour éviter les problèmes de type
        Long filterUserId = (userId != null) ? userId : 0L;
        String filterAction = (action != null && !action.trim().isEmpty()) ? action : "";
        
        return auditLogRepository.findWithFilters(filterUserId, filterAction, startDate, endDate, pageable);
    }

    // Récupérer les logs par utilisateur
    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    // Récupérer les logs par action
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    // Récupérer les logs de sécurité
    public Page<AuditLog> getSecurityLogs(Pageable pageable) {
        return auditLogRepository.findSecurityLogs(pageable);
    }

    // Récupérer les logs d'échec
    public Page<AuditLog> getFailureLogs(Pageable pageable) {
        // Retourner une page vide pour l'instant car la colonne status n'existe pas encore
        return Page.empty(pageable);
    }

    // Récupérer les logs de modifications
    public Page<AuditLog> getModificationLogs(Pageable pageable) {
        return auditLogRepository.findModificationLogs(pageable);
    }

    // Récupérer les logs récents
    public List<AuditLog> getRecentLogs(LocalDateTime since) {
        return auditLogRepository.findRecentLogs(since);
    }

    // Statistiques
    public Map<String, Object> getAuditStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Statistiques par action
        List<Object[]> actionStats = auditLogRepository.findActionStats();
        stats.put("actionStats", actionStats);
        
        // Statistiques par statut (temporairement vide)
        stats.put("statusStats", new ArrayList<>());
        
        // Top utilisateurs par activité
        List<Object[]> topUsers = auditLogRepository.findTopUsersByActivity();
        stats.put("topUsers", topUsers);
        
        // Statistiques quotidiennes (7 derniers jours)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> dailyStats = auditLogRepository.findDailyStats(weekAgo);
        stats.put("dailyStats", dailyStats);
        
        return stats;
    }

    // Statistiques par utilisateur
    public Map<String, Object> getUserAuditStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Top actions de l'utilisateur
        List<Object[]> topActions = auditLogRepository.findTopActionsByUserId(userId);
        stats.put("topActions", topActions);
        
        // Nombre total d'actions
        long totalActions = auditLogRepository.countByUserId(userId);
        stats.put("totalActions", totalActions);
        
        // Actions récentes (24h)
        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
        List<AuditLog> recentActions = auditLogRepository.findRecentLogs(dayAgo);
        stats.put("recentActions", recentActions);
        
        return stats;
    }

    // Logs de connexion
    public void logLogin(User user, String ipAddress, boolean success) {
        String action = success ? "LOGIN" : "LOGIN_FAILED";
        String details = success ? "Connexion réussie" : "Échec de connexion";
        String status = success ? "SUCCESS" : "FAILURE";
        
        AuditLog log = new AuditLog(user, action, details, ipAddress);
        log.setStatus(status);
        auditLogRepository.save(log);
    }

    // Logs de déconnexion
    public void logLogout(User user, String ipAddress) {
        AuditLog log = new AuditLog(user, "LOGOUT", "Déconnexion", ipAddress);
        auditLogRepository.save(log);
    }

    // Logs de création d'utilisateur
    public void logUserCreation(User createdUser, User creator, String ipAddress) {
        String details = String.format("Création de l'utilisateur %s", createdUser.getEmail());
        AuditLog log = new AuditLog(creator, "USER_CREATE", details, ipAddress);
        log.setResourceType("USER");
        log.setResourceId(createdUser.getId());
        auditLogRepository.save(log);
    }

    // Logs de modification d'utilisateur
    public void logUserUpdate(User updatedUser, User modifier, String ipAddress, String oldValues, String newValues) {
        String details = String.format("Modification de l'utilisateur %s", updatedUser.getEmail());
        AuditLog log = new AuditLog(modifier, "USER_UPDATE", details, ipAddress);
        log.setResourceType("USER");
        log.setResourceId(updatedUser.getId());
        log.setOldValues(oldValues);
        log.setNewValues(newValues);
        auditLogRepository.save(log);
    }

    // Logs de création de réservation
    public void logReservationCreation(Long reservationId, User creator, String ipAddress) {
        String details = String.format("Création de la réservation #%d", reservationId);
        AuditLog log = new AuditLog(creator, "RESERVATION_CREATE", details, ipAddress);
        log.setResourceType("RESERVATION");
        log.setResourceId(reservationId);
        auditLogRepository.save(log);
    }

    // Logs de modification de réservation
    public void logReservationUpdate(Long reservationId, User modifier, String ipAddress, String oldValues, String newValues) {
        String details = String.format("Modification de la réservation #%d", reservationId);
        AuditLog log = new AuditLog(modifier, "RESERVATION_UPDATE", details, ipAddress);
                log.setResourceType("RESERVATION");
        log.setResourceId(reservationId);
        log.setOldValues(oldValues);
        log.setNewValues(newValues);
        auditLogRepository.save(log);
    }

    // Logs de création d'équipement
    public void logEquipmentCreation(Long equipmentId, User creator, String ipAddress) {
        String details = String.format("Création de l'équipement #%d", equipmentId);
        AuditLog log = new AuditLog(creator, "EQUIPMENT_CREATE", details, ipAddress);
        log.setResourceType("EQUIPMENT");
        log.setResourceId(equipmentId);
        auditLogRepository.save(log);
    }

    // Logs de modification d'équipement
    public void logEquipmentUpdate(Long equipmentId, User modifier, String ipAddress, String oldValues, String newValues) {
        String details = String.format("Modification de l'équipement #%d", equipmentId);
        AuditLog log = new AuditLog(modifier, "EQUIPMENT_UPDATE", details, ipAddress);
                log.setResourceType("EQUIPMENT");
        log.setResourceId(equipmentId);
        log.setOldValues(oldValues);
        log.setNewValues(newValues);
        auditLogRepository.save(log);
    }

    // Nettoyer les anciens logs (plus de 90 jours)
    public void cleanOldLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        // Cette méthode nécessiterait une implémentation personnalisée dans le repository
        // pour supprimer les logs plus anciens que 90 jours
    }
}

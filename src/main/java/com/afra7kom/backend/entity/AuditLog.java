package com.afra7kom.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "action", nullable = false)
    private String action;
    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "resource_type")
    private String resourceType;
    
    @Column(name = "resource_id")
    private Long resourceId;
    
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;
    
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;
    
    @Column(name = "status")
    private String status; // SUCCESS, FAILURE, WARNING
    
    @Column(name = "session_id")
    private String sessionId;
    
    // Constructeurs
    public AuditLog() {}
    
    public AuditLog(User user, String action, String details, String ipAddress) {
        this.user = user;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
        this.status = "SUCCESS";
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    // Méthodes utilitaires pour la compatibilité
    public void setActionType(String actionType) {
        this.action = actionType;
    }
    
    public void setEntityType(String entityType) {
        this.resourceType = entityType;
    }
    
    public void setEntityId(Long entityId) {
        this.resourceId = entityId;
    }
    
    public void setDescription(String description) {
        this.details = description;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.timestamp = createdAt;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    
    public Long getResourceId() {
        return resourceId;
    }
    
    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }
    
    public String getOldValues() {
        return oldValues;
    }
    
    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }
    
    public String getNewValues() {
        return newValues;
    }
    
    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    // Méthodes utilitaires
    public String getUserEmail() {
        return user != null ? user.getEmail() : "Système";
    }
    
    public String getActionDisplayName() {
        switch (action) {
            case "LOGIN": return "Connexion";
            case "LOGOUT": return "Déconnexion";
            case "USER_CREATE": return "Création utilisateur";
            case "USER_UPDATE": return "Modification utilisateur";
            case "USER_DELETE": return "Suppression utilisateur";
            case "RESERVATION_CREATE": return "Création réservation";
            case "RESERVATION_UPDATE": return "Modification réservation";
            case "RESERVATION_DELETE": return "Suppression réservation";
            case "EQUIPMENT_CREATE": return "Création équipement";
            case "EQUIPMENT_UPDATE": return "Modification équipement";
            case "EQUIPMENT_DELETE": return "Suppression équipement";
            case "PACK_CREATE": return "Création pack";
            case "PACK_UPDATE": return "Modification pack";
            case "PACK_DELETE": return "Suppression pack";
            default: return action;
        }
    }
    
    public String getStatusDisplayName() {
        switch (status) {
            case "SUCCESS": return "Succès";
            case "FAILURE": return "Échec";
            case "WARNING": return "Avertissement";
            default: return status;
        }
    }
}

package com.afra7kom.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    
    @Column(name = "reservation_id")
    private Long reservationId;
    
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;
    
    @Column(name = "related_entity_id")
    private Long relatedEntityId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = NotificationStatus.UNREAD;
        }
    }
    
    public enum NotificationType {
        RESERVATION_CREATED("Nouvelle réservation"),
        RESERVATION_CONFIRMED("Réservation confirmée"),
        RESERVATION_CANCELLED("Réservation annulée"),
        RESERVATION_MODIFIED("Réservation modifiée"),
        PAYMENT_RECEIVED("Paiement reçu"),
        PAYMENT_OVERDUE("Paiement en retard"),
        SYSTEM_ALERT("Alerte système"),
        MAINTENANCE_SCHEDULED("Maintenance programmée"),
        INVENTORY_LOW("Stock faible"),
        USER_ACTION_REQUIRED("Action requise");
        
        private final String displayName;
        
        NotificationType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum NotificationStatus {
        UNREAD("Non lue"),
        READ("Lue"),
        ARCHIVED("Archivée");
        
        private final String displayName;
        
        NotificationStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}


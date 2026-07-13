package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Récupérer toutes les notifications d'un utilisateur triées par date de création (plus récentes en premier)
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Récupérer les notifications d'un utilisateur avec pagination
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Récupérer les notifications non lues d'un utilisateur
     */
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Notification.NotificationStatus status);
    
    /**
     * Récupérer les notifications par statut d'un utilisateur
     */
    List<Notification> findByUserIdAndStatus(Long userId, Notification.NotificationStatus status);
    
    /**
     * Compter les notifications non lues d'un utilisateur
     */
    Long countByUserIdAndStatus(Long userId, Notification.NotificationStatus status);
    
    /**
     * Récupérer toutes les notifications d'un utilisateur (pour suppression)
     */
    List<Notification> findByUserId(Long userId);
    
    /**
     * Récupérer les notifications par type
     */
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, Notification.NotificationType type);
    
    /**
     * Supprimer les notifications anciennes (plus de X jours)
     */
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    void deleteOldNotifications(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
    
    /**
     * Compter les notifications par statut pour un utilisateur
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.status = :status")
    Long countByUserIdAndStatusQuery(@Param("userId") Long userId, @Param("status") Notification.NotificationStatus status);
}

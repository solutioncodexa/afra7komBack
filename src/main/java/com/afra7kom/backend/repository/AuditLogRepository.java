package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.AuditLog;
import com.afra7kom.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Recherche par utilisateur
    Page<AuditLog> findByUser(User user, Pageable pageable);
    
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    
    // Recherche par action
    Page<AuditLog> findByAction(String action, Pageable pageable);
    

    
    // Recherche par période
    Page<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Recherche par utilisateur et période
    Page<AuditLog> findByUserAndTimestampBetween(User user, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Recherche par action et période
    Page<AuditLog> findByActionAndTimestampBetween(String action, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Recherche avec filtres multiples
    @Query("SELECT al FROM AuditLog al WHERE " +
           "(:userId = 0 OR al.user.id = :userId) AND " +
           "(:action = '' OR al.action = :action) AND " +
           "(:startDate IS NULL OR al.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR al.timestamp <= :endDate)")
    Page<AuditLog> findWithFilters(@Param("userId") Long userId,
                                  @Param("action") String action,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  Pageable pageable);
    
    // Recherche par ressource
    Page<AuditLog> findByResourceTypeAndResourceId(String resourceType, Long resourceId, Pageable pageable);
    

    
    // Statistiques
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.action = :action")
    long countByAction(@Param("action") String action);
    
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
    

    
    // Top actions par utilisateur
    @Query("SELECT al.action, COUNT(al) as count FROM AuditLog al " +
           "WHERE al.user.id = :userId " +
           "GROUP BY al.action " +
           "ORDER BY count DESC")
    List<Object[]> findTopActionsByUserId(@Param("userId") Long userId);
    
    // Top utilisateurs par activité
    @Query("SELECT al.user.email, COUNT(al) as count FROM AuditLog al " +
           "GROUP BY al.user.email " +
           "ORDER BY count DESC")
    List<Object[]> findTopUsersByActivity();
    
    // Actions récentes
    @Query("SELECT al FROM AuditLog al WHERE al.timestamp >= :since ORDER BY al.timestamp DESC")
    List<AuditLog> findRecentLogs(@Param("since") LocalDateTime since);
    

    
    // Logs de sécurité (connexions, déconnexions, échecs)
    @Query("SELECT al FROM AuditLog al WHERE al.action IN ('LOGIN', 'LOGOUT', 'LOGIN_FAILED') ORDER BY al.timestamp DESC")
    Page<AuditLog> findSecurityLogs(Pageable pageable);
    
    // Logs de modifications
    @Query("SELECT al FROM AuditLog al WHERE al.action LIKE '%UPDATE%' OR al.action LIKE '%CREATE%' OR al.action LIKE '%DELETE%' ORDER BY al.timestamp DESC")
    Page<AuditLog> findModificationLogs(Pageable pageable);
    
    // Statistiques par jour
    @Query("SELECT DATE(al.timestamp) as date, COUNT(al) as count FROM AuditLog al " +
           "WHERE al.timestamp >= :startDate " +
           "GROUP BY DATE(al.timestamp) " +
           "ORDER BY date DESC")
    List<Object[]> findDailyStats(@Param("startDate") LocalDateTime startDate);
    
    // Statistiques par action
    @Query("SELECT al.action, COUNT(al) as count FROM AuditLog al " +
           "GROUP BY al.action " +
           "ORDER BY count DESC")
    List<Object[]> findActionStats();
    

}

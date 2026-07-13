package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.NotificationDto;
import com.afra7kom.backend.entity.Notification;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.repository.NotificationRepository;
import com.afra7kom.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service("notificationRestService")
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Récupérer toutes les notifications d'un utilisateur
     */
    public List<NotificationDto> getUserNotifications(Long userId) {
        log.info("Récupération des notifications pour l'utilisateur {}", userId);
        
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        log.info("Trouvé {} notifications pour l'utilisateur {}", notifications.size(), userId);
        
        return notifications.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les notifications avec pagination
     */
    public Page<NotificationDto> getUserNotificationsPaginated(Long userId, Pageable pageable) {
        log.info("Récupération des notifications paginées pour l'utilisateur {}", userId);
        
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        log.info("Trouvé {} notifications (page {}) pour l'utilisateur {}", 
                notifications.getTotalElements(), notifications.getNumber(), userId);
        
        return notifications.map(this::mapToDto);
    }

    /**
     * Récupérer les notifications non lues
     */
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        log.info("Récupération des notifications non lues pour l'utilisateur {}", userId);
        
        List<Notification> notifications = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, Notification.NotificationStatus.UNREAD);
        log.info("Trouvé {} notifications non lues pour l'utilisateur {}", notifications.size(), userId);
        
        return notifications.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Compter les notifications non lues
     */
    public Long getUnreadNotificationCount(Long userId) {
        log.info("Comptage des notifications non lues pour l'utilisateur {}", userId);
        
        Long count = notificationRepository.countByUserIdAndStatus(userId, Notification.NotificationStatus.UNREAD);
        log.info("Nombre de notifications non lues pour l'utilisateur {}: {}", userId, count);
        
        return count;
    }

    /**
     * Marquer une notification comme lue
     */
    @Transactional
    public NotificationDto markAsRead(Long notificationId) {
        log.info("Marquage de la notification {} comme lue", notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));
        
        notification.setStatus(Notification.NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification {} marquée comme lue", notificationId);
        
        return mapToDto(savedNotification);
    }

    /**
     * Marquer toutes les notifications comme lues
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        log.info("Marquage de toutes les notifications comme lues pour l'utilisateur {}", userId);
        
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, Notification.NotificationStatus.UNREAD);
        
        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> {
            notification.setStatus(Notification.NotificationStatus.READ);
            notification.setReadAt(now);
        });
        
        notificationRepository.saveAll(unreadNotifications);
        log.info("{} notifications marquées comme lues pour l'utilisateur {}", unreadNotifications.size(), userId);
    }

    /**
     * Supprimer une notification
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        log.info("Suppression de la notification {}", notificationId);
        
        if (!notificationRepository.existsById(notificationId)) {
            throw new RuntimeException("Notification non trouvée");
        }
        
        notificationRepository.deleteById(notificationId);
        log.info("Notification {} supprimée", notificationId);
    }

    /**
     * Supprimer toutes les notifications d'un utilisateur
     */
    @Transactional
    public void deleteAllUserNotifications(Long userId) {
        log.info("Suppression de toutes les notifications pour l'utilisateur {}", userId);
        
        List<Notification> notifications = notificationRepository.findByUserId(userId);
        notificationRepository.deleteAll(notifications);
        
        log.info("{} notifications supprimées pour l'utilisateur {}", notifications.size(), userId);
    }

    /**
     * Créer une notification de test
     */
    @Transactional
    public NotificationDto createTestNotification(Long userId) {
        log.info("Création d'une notification de test pour l'utilisateur {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle("Notification de test");
        notification.setMessage("Ceci est une notification de test créée le " + LocalDateTime.now());
        notification.setType(Notification.NotificationType.SYSTEM_ALERT);
        notification.setStatus(Notification.NotificationStatus.UNREAD);
        notification.setCreatedAt(LocalDateTime.now());
        
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification de test créée avec l'ID {}", savedNotification.getId());
        
        return mapToDto(savedNotification);
    }

    /**
     * Vérifier si l'utilisateur est propriétaire
     */
    public boolean isUserOwner(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // Pour l'instant, permettre l'accès aux admins
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Vérifier si l'utilisateur est propriétaire d'une notification
     */
    public boolean isUserOwnerOfNotification(Long notificationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // Pour l'instant, permettre l'accès aux admins
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Mapper une entité Notification vers un DTO
     */
    private NotificationDto mapToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUser().getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType().toString());
        dto.setStatus(notification.getStatus().toString());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReadAt(notification.getReadAt());
        dto.setReservationId(notification.getReservationId());
        return dto;
    }
}

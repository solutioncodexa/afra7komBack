package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.NotificationDto;
import com.afra7kom.backend.entity.Notification;
import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.repository.NotificationRepository;
import com.afra7kom.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Créer une notification pour une nouvelle réservation
     */
    @Transactional
    public void createReservationNotification(Reservation reservation) {
        try {
            log.info("Création d'une notification pour la réservation {}", reservation.getId());
            
            // Créer une notification pour l'utilisateur qui a fait la réservation
            Notification userNotification = new Notification();
            userNotification.setUser(reservation.getUser());
            userNotification.setTitle("Réservation créée");
            userNotification.setMessage("Votre réservation #" + reservation.getId() + " a été créée avec succès");
            userNotification.setType(Notification.NotificationType.RESERVATION_CREATED);
            userNotification.setStatus(Notification.NotificationStatus.UNREAD);
            userNotification.setReservationId(reservation.getId());
            userNotification.setCreatedAt(LocalDateTime.now());
            
            Notification savedUserNotification = notificationRepository.save(userNotification);
            log.info("Notification utilisateur créée avec l'ID {}", savedUserNotification.getId());
            
            // Envoyer la notification via WebSocket
            sendNotificationViaWebSocket(reservation.getUser().getId(), savedUserNotification);
            
            // Créer des notifications pour les admins et agents
            createAdminNotifications(reservation);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création de la notification pour la réservation {}", 
                     reservation.getId(), e);
        }
    }

    /**
     * Créer des notifications pour les admins et agents
     */
    @Transactional
    public void createAdminNotifications(Reservation reservation) {
        try {
            // Récupérer tous les utilisateurs avec le rôle ADMIN ou AGENT
            // Pour l'instant, utiliser une approche simple
            List<User> adminUsers = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                    .anyMatch(role -> "ADMIN".equals(role.getName()) || "AGENT".equals(role.getName())))
                .filter(user -> !user.getId().equals(reservation.getUser().getId())) // Exclure l'utilisateur qui a créé la réservation
                .collect(java.util.stream.Collectors.toList());
            
            for (User admin : adminUsers) {
                Notification adminNotification = new Notification();
                adminNotification.setUser(admin);
                adminNotification.setTitle("Nouvelle réservation");
                adminNotification.setMessage("Une nouvelle réservation #" + reservation.getId() + 
                                           " a été créée par " + reservation.getFirstName() + " " + reservation.getLastName());
                adminNotification.setType(Notification.NotificationType.RESERVATION_CREATED);
                adminNotification.setStatus(Notification.NotificationStatus.UNREAD);
                adminNotification.setReservationId(reservation.getId());
                adminNotification.setCreatedAt(LocalDateTime.now());
                
                Notification savedAdminNotification = notificationRepository.save(adminNotification);
                log.info("Notification admin créée avec l'ID {} pour l'utilisateur {}", 
                        savedAdminNotification.getId(), admin.getId());
                
                // Envoyer la notification via WebSocket
                sendNotificationViaWebSocket(admin.getId(), savedAdminNotification);
            }
            
        } catch (Exception e) {
            log.error("Erreur lors de la création des notifications admin pour la réservation {}", 
                     reservation.getId(), e);
        }
    }

    /**
     * Créer une notification pour la confirmation d'une réservation
     */
    @Transactional
    public void createReservationConfirmedNotification(Reservation reservation) {
        try {
            log.info("Création d'une notification de confirmation pour la réservation {}", reservation.getId());
            
            Notification notification = new Notification();
            notification.setUser(reservation.getUser());
            notification.setTitle("Réservation confirmée");
            notification.setMessage("Votre réservation #" + reservation.getId() + " a été confirmée");
            notification.setType(Notification.NotificationType.RESERVATION_CONFIRMED);
            notification.setStatus(Notification.NotificationStatus.UNREAD);
            notification.setReservationId(reservation.getId());
            notification.setCreatedAt(LocalDateTime.now());
            
            Notification savedNotification = notificationRepository.save(notification);
            log.info("Notification de confirmation créée avec l'ID {}", savedNotification.getId());
            
            // Envoyer la notification via WebSocket
            sendNotificationViaWebSocket(reservation.getUser().getId(), savedNotification);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création de la notification de confirmation pour la réservation {}", 
                     reservation.getId(), e);
        }
    }

    /**
     * Créer une notification pour l'annulation d'une réservation
     */
    @Transactional
    public void createReservationCancelledNotification(Reservation reservation) {
        try {
            log.info("Création d'une notification d'annulation pour la réservation {}", reservation.getId());
            
            Notification notification = new Notification();
            notification.setUser(reservation.getUser());
            notification.setTitle("Réservation annulée");
            notification.setMessage("Votre réservation #" + reservation.getId() + " a été annulée");
            notification.setType(Notification.NotificationType.RESERVATION_CANCELLED);
            notification.setStatus(Notification.NotificationStatus.UNREAD);
            notification.setReservationId(reservation.getId());
            notification.setCreatedAt(LocalDateTime.now());
            
            Notification savedNotification = notificationRepository.save(notification);
            log.info("Notification d'annulation créée avec l'ID {}", savedNotification.getId());
            
            // Envoyer la notification via WebSocket
            sendNotificationViaWebSocket(reservation.getUser().getId(), savedNotification);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création de la notification d'annulation pour la réservation {}", 
                     reservation.getId(), e);
        }
    }

    /**
     * Envoyer une notification via WebSocket
     */
    private void sendNotificationViaWebSocket(Long userId, Notification notification) {
        try {
            // Convertir en DTO
            NotificationDto notificationDto = mapToDto(notification);
            
            // Envoyer la notification
            messagingTemplate.convertAndSendToUser(
                userId.toString(), 
                "/queue/notifications", 
                notificationDto
            );
            
            // Mettre à jour le compteur
            Long unreadCount = notificationRepository.countByUserIdAndStatus(
                userId, Notification.NotificationStatus.UNREAD);
            messagingTemplate.convertAndSendToUser(
                userId.toString(), 
                "/queue/notification-count", 
                unreadCount
            );
            
            log.info("Notification envoyée via WebSocket à l'utilisateur {}", userId);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification via WebSocket à l'utilisateur {}", 
                     userId, e);
        }
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

    /**
     * Créer une notification de test pour une réservation
     */
    @Transactional
    public void createReservationNotificationForTest(Long userId, Long reservationId, String customerName) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitle("Notification de test - Réservation");
            notification.setMessage("Notification de test pour la réservation #" + reservationId + 
                                 " créée par " + customerName);
            notification.setType(Notification.NotificationType.RESERVATION_CREATED);
            notification.setStatus(Notification.NotificationStatus.UNREAD);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setReservationId(reservationId);
            notification.setRelatedEntityType("Reservation");
            notification.setRelatedEntityId(reservationId);

            Notification savedNotification = notificationRepository.save(notification);
            log.info("Notification de test créée pour l'utilisateur {}: {}", userId, savedNotification.getId());

            // Envoyer via WebSocket
            sendNotificationViaWebSocket(userId, savedNotification);

        } catch (Exception e) {
            log.error("Erreur lors de la création de la notification de test pour l'utilisateur {}: {}", 
                     userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Créer une notification système de test
     */
    @Transactional
    public void createSystemNotificationForTest(Long userId, String title, String message) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(Notification.NotificationType.SYSTEM_ALERT);
            notification.setStatus(Notification.NotificationStatus.UNREAD);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRelatedEntityType("System");
            notification.setRelatedEntityId(0L);

            Notification savedNotification = notificationRepository.save(notification);
            log.info("Notification système de test créée pour l'utilisateur {}: {}", userId, savedNotification.getId());

            // Envoyer via WebSocket
            sendNotificationViaWebSocket(userId, savedNotification);

        } catch (Exception e) {
            log.error("Erreur lors de la création de la notification système de test pour l'utilisateur {}: {}", 
                     userId, e.getMessage());
            throw e;
        }
    }
}

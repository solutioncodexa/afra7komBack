package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.NotificationDto;
import com.afra7kom.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationController {

    @Qualifier("notificationRestService")
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gérer la connexion d'un utilisateur
     */
    @MessageMapping("/connect")
    public void handleConnect(@Payload String userId, Principal principal) {
        log.info("Utilisateur {} connecté via WebSocket", userId);
        
        try {
            Long userIdLong = Long.parseLong(userId);
            
            // Envoyer les notifications existantes
            List<NotificationDto> notifications = notificationService.getUserNotifications(userIdLong);
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notifications);
            
            // Envoyer le compteur de notifications non lues
            Long unreadCount = notificationService.getUnreadNotificationCount(userIdLong);
            messagingTemplate.convertAndSendToUser(userId, "/queue/notification-count", unreadCount);
            
        } catch (Exception e) {
            log.error("Erreur lors de la connexion WebSocket pour l'utilisateur {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Marquer une notification comme lue
     */
    @MessageMapping("/mark-read")
    public void markAsRead(@Payload String notificationId, Principal principal) {
        log.info("Marquage de la notification {} comme lue", notificationId);
        
        try {
            Long notificationIdLong = Long.parseLong(notificationId);
            NotificationDto notification = notificationService.markAsRead(notificationIdLong);
            
            // Envoyer la notification mise à jour
            messagingTemplate.convertAndSendToUser(
                principal.getName(), 
                "/queue/notification-updates", 
                notification
            );
            
            // Mettre à jour le compteur
            Long unreadCount = notificationService.getUnreadNotificationCount(notification.getUserId());
            messagingTemplate.convertAndSendToUser(
                principal.getName(), 
                "/queue/notification-count", 
                unreadCount
            );
            
        } catch (Exception e) {
            log.error("Erreur lors du marquage de la notification {} comme lue: {}", notificationId, e.getMessage());
        }
    }

    /**
     * Marquer toutes les notifications comme lues
     */
    @MessageMapping("/mark-all-read")
    public void markAllAsRead(@Payload String userId, Principal principal) {
        log.info("Marquage de toutes les notifications comme lues pour l'utilisateur {}", userId);
        
        try {
            Long userIdLong = Long.parseLong(userId);
            notificationService.markAllAsRead(userIdLong);
            
            // Envoyer le compteur mis à jour
            messagingTemplate.convertAndSendToUser(
                userId, 
                "/queue/notification-count", 
                0L
            );
            
        } catch (Exception e) {
            log.error("Erreur lors du marquage de toutes les notifications comme lues: {}", e.getMessage());
        }
    }

    /**
     * Demander les notifications de l'utilisateur
     */
    @MessageMapping("/get-notifications")
    public void getNotifications(@Payload String userId, Principal principal) {
        log.info("Demande des notifications pour l'utilisateur {}", userId);
        
        try {
            Long userIdLong = Long.parseLong(userId);
            List<NotificationDto> notifications = notificationService.getUserNotifications(userIdLong);
            
            messagingTemplate.convertAndSendToUser(
                userId, 
                "/queue/notifications", 
                notifications
            );
            
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des notifications: {}", e.getMessage());
        }
    }

    /**
     * Demander le compteur de notifications non lues
     */
    @MessageMapping("/get-unread-count")
    public void getUnreadCount(@Payload String userId, Principal principal) {
        log.info("Demande du compteur de notifications non lues pour l'utilisateur {}", userId);
        
        try {
            Long userIdLong = Long.parseLong(userId);
            Long unreadCount = notificationService.getUnreadNotificationCount(userIdLong);
            
            messagingTemplate.convertAndSendToUser(
                userId, 
                "/queue/notification-count", 
                unreadCount
            );
            
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du compteur de notifications: {}", e.getMessage());
        }
    }
}

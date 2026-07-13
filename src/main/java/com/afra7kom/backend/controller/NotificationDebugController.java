package com.afra7kom.backend.controller;

import com.afra7kom.backend.entity.Notification;
import com.afra7kom.backend.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications/debug")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Debug", description = "Debug des notifications")
public class NotificationDebugController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/user/{userId}/all")
    @Operation(summary = "Récupérer toutes les notifications d'un utilisateur avec détails")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUserNotifications(@PathVariable Long userId) {
        try {
            List<Notification> allNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
            List<Notification> unreadNotifications = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    userId, Notification.NotificationStatus.UNREAD);
            List<Notification> readNotifications = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    userId, Notification.NotificationStatus.READ);
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalNotifications", allNotifications.size());
            result.put("unreadCount", unreadNotifications.size());
            result.put("readCount", readNotifications.size());
            result.put("allNotifications", allNotifications.stream().map(n -> {
                Map<String, Object> notif = new HashMap<>();
                notif.put("id", n.getId());
                notif.put("title", n.getTitle());
                notif.put("status", n.getStatus());
                notif.put("createdAt", n.getCreatedAt());
                notif.put("readAt", n.getReadAt());
                return notif;
            }).toList());
            result.put("unreadNotifications", unreadNotifications.stream().map(n -> {
                Map<String, Object> notif = new HashMap<>();
                notif.put("id", n.getId());
                notif.put("title", n.getTitle());
                notif.put("status", n.getStatus());
                notif.put("createdAt", n.getCreatedAt());
                return notif;
            }).toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erreur lors du debug des notifications pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/user/{userId}/status-counts")
    @Operation(summary = "Compter les notifications par statut")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getNotificationStatusCounts(@PathVariable Long userId) {
        try {
            Long unreadCount = notificationRepository.countByUserIdAndStatus(userId, Notification.NotificationStatus.UNREAD);
            Long readCount = notificationRepository.countByUserIdAndStatus(userId, Notification.NotificationStatus.READ);
            Long archivedCount = notificationRepository.countByUserIdAndStatus(userId, Notification.NotificationStatus.ARCHIVED);
            
            Map<String, Long> result = new HashMap<>();
            result.put("unread", unreadCount);
            result.put("read", readCount);
            result.put("archived", archivedCount);
            result.put("total", unreadCount + readCount + archivedCount);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erreur lors du comptage des notifications pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/user/{userId}/mark-all-read")
    @Operation(summary = "Marquer toutes les notifications comme lues (debug)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> markAllAsReadDebug(@PathVariable Long userId) {
        try {
            List<Notification> unreadNotifications = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    userId, Notification.NotificationStatus.UNREAD);
            
            unreadNotifications.forEach(notification -> {
                notification.setStatus(Notification.NotificationStatus.READ);
                notification.setReadAt(java.time.LocalDateTime.now());
            });
            
            notificationRepository.saveAll(unreadNotifications);
            
            return ResponseEntity.ok("Marqué " + unreadNotifications.size() + " notifications comme lues");
        } catch (Exception e) {
            log.error("Erreur lors du marquage de toutes les notifications comme lues pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}

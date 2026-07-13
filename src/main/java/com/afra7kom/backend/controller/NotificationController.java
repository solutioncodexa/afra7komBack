package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.NotificationDto;
import com.afra7kom.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Gestion des notifications")
public class NotificationController {

    @Qualifier("notificationRestService")
    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Récupérer les notifications d'un utilisateur", description = "Retourne toutes les notifications d'un utilisateur")
    @PreAuthorize("hasRole('ADMIN') or @notificationService.isUserOwner(#userId)")
    public ResponseEntity<List<NotificationDto>> getUserNotifications(@PathVariable Long userId) {
        try {
            List<NotificationDto> notifications = notificationService.getUserNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des notifications pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/user/{userId}/paginated")
    @Operation(summary = "Récupérer les notifications paginées", description = "Retourne les notifications d'un utilisateur avec pagination")
    @PreAuthorize("hasRole('ADMIN') or @notificationService.isUserOwner(#userId)")
    public ResponseEntity<Page<NotificationDto>> getUserNotificationsPaginated(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<NotificationDto> notifications = notificationService.getUserNotificationsPaginated(userId, pageable);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des notifications paginées pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Récupérer les notifications non lues", description = "Retourne les notifications non lues d'un utilisateur")
    @PreAuthorize("hasRole('ADMIN') or @notificationService.isUserOwner(#userId)")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(@PathVariable Long userId) {
        try {
            List<NotificationDto> notifications = notificationService.getUnreadNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des notifications non lues pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/user/{userId}/unread-count")
    @Operation(summary = "Compter les notifications non lues", description = "Retourne le nombre de notifications non lues d'un utilisateur")
    @PreAuthorize("hasRole('ADMIN') or @notificationService.isUserOwner(#userId)")
    public ResponseEntity<Long> getUnreadNotificationCount(@PathVariable Long userId) {
        try {
            Long count = notificationService.getUnreadNotificationCount(userId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Erreur lors du comptage des notifications non lues pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Marquer une notification comme lue", description = "Marque une notification comme lue")
    @PreAuthorize("hasRole('ADMIN') or @notificationService.isUserOwnerOfNotification(#notificationId)")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable Long notificationId) {
        try {
            NotificationDto notification = notificationService.markAsRead(notificationId);
            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            log.error("Erreur lors du marquage de la notification {} comme lue: {}", notificationId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/user/{userId}/mark-all-read")
    @Operation(summary = "Marquer toutes les notifications comme lues", description = "Marque toutes les notifications d'un utilisateur comme lues")
    @PreAuthorize("hasRole('ADMIN') or @notificationService.isUserOwner(#userId)")
    public ResponseEntity<String> markAllAsRead(@PathVariable Long userId) {
        try {
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok("Toutes les notifications ont été marquées comme lues");
        } catch (Exception e) {
            log.error("Erreur lors du marquage de toutes les notifications comme lues pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Supprimer une notification", description = "Supprime une notification")
    @PreAuthorize("hasRole('ADMIN') or @notificationService.isUserOwnerOfNotification(#notificationId)")
    public ResponseEntity<String> deleteNotification(@PathVariable Long notificationId) {
        try {
            notificationService.deleteNotification(notificationId);
            return ResponseEntity.ok("Notification supprimée");
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la notification {}: {}", notificationId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Supprimer toutes les notifications", description = "Supprime toutes les notifications d'un utilisateur")
    @PreAuthorize("hasRole('ADMIN') or @notificationService.isUserOwner(#userId)")
    public ResponseEntity<String> deleteAllUserNotifications(@PathVariable Long userId) {
        try {
            notificationService.deleteAllUserNotifications(userId);
            return ResponseEntity.ok("Toutes les notifications ont été supprimées");
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de toutes les notifications pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/test/{userId}")
    @Operation(summary = "Créer une notification de test", description = "Crée une notification de test pour un utilisateur")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationDto> createTestNotification(@PathVariable Long userId) {
        try {
            NotificationDto notification = notificationService.createTestNotification(userId);
            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            log.error("Erreur lors de la création de la notification de test pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/user/{userId}/debug-unread")
    @Operation(summary = "Debug des notifications non lues", description = "Retourne les détails des notifications non lues pour debug")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> debugUnreadNotifications(@PathVariable Long userId) {
        try {
            List<NotificationDto> unreadNotifications = notificationService.getUnreadNotifications(userId);
            Long unreadCount = notificationService.getUnreadNotificationCount(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("unreadNotifications", unreadNotifications);
            result.put("unreadCount", unreadCount);
            result.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erreur lors du debug des notifications non lues pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}

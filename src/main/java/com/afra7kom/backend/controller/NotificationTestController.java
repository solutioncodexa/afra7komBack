package com.afra7kom.backend.controller;

import com.afra7kom.backend.service.ReservationNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Test Notifications", description = "Endpoints de test pour les notifications")
public class NotificationTestController {

    private final ReservationNotificationService reservationNotificationService;

    @PostMapping("/create-reservation-notification/{userId}")
    @Operation(summary = "Créer une notification de réservation de test", description = "Crée une notification de test pour un utilisateur")
    public ResponseEntity<Map<String, Object>> createReservationNotification(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "Test Client") String customerName,
            @RequestParam(defaultValue = "999") Long reservationId) {
        
        try {
            log.info("Création d'une notification de test pour l'utilisateur {} avec le client {} et la réservation {}", 
                    userId, customerName, reservationId);
            
            // Créer une notification de test pour la réservation
            reservationNotificationService.createReservationNotificationForTest(userId, reservationId, customerName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification de test créée avec succès");
            response.put("userId", userId);
            response.put("customerName", customerName);
            response.put("reservationId", reservationId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création de la notification de test: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de la création de la notification: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/create-system-notification/{userId}")
    @Operation(summary = "Créer une notification système de test", description = "Crée une notification système de test pour un utilisateur")
    public ResponseEntity<Map<String, Object>> createSystemNotification(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "Test System Alert") String title,
            @RequestParam(defaultValue = "Ceci est une notification système de test") String message) {
        
        try {
            log.info("Création d'une notification système de test pour l'utilisateur {} avec le titre '{}'", 
                    userId, title);
            
            // Créer une notification système de test
            reservationNotificationService.createSystemNotificationForTest(userId, title, message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification système de test créée avec succès");
            response.put("userId", userId);
            response.put("title", title);
            response.put("message", message);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création de la notification système de test: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de la création de la notification: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/test-websocket/{userId}")
    @Operation(summary = "Tester la connexion WebSocket", description = "Teste si le WebSocket fonctionne pour un utilisateur")
    public ResponseEntity<Map<String, Object>> testWebSocket(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("websocketUrl", "ws://localhost:8080/ws");
        response.put("message", "Pour tester le WebSocket, connectez-vous avec l'ID utilisateur: " + userId);
        response.put("instructions", "Utilisez un client WebSocket pour vous connecter à ws://localhost:8080/ws et envoyer des messages");
        
        return ResponseEntity.ok(response);
    }
}
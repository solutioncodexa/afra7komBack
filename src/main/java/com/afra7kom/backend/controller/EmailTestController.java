package com.afra7kom.backend.controller;

import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/test/email")
@RequiredArgsConstructor
@Slf4j
public class EmailTestController {
    
    private final EmailService emailService;
    
    /**
     * Tester l'envoi d'email de nouvelle réservation
     */
    @PostMapping("/new-reservation")
    public ResponseEntity<String> testNewReservationEmail(@RequestParam String toEmail) {
        try {
            // Créer une réservation de test
            Reservation testReservation = createTestReservation();
            
            // Envoyer l'email
            emailService.sendNewReservationNotification(testReservation);
            
            return ResponseEntity.ok("Email de nouvelle réservation envoyé avec succès à " + toEmail);
        } catch (Exception e) {
            log.error("Erreur lors du test d'envoi d'email de nouvelle réservation", e);
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }
    
    /**
     * Tester l'envoi d'email d'approbation
     */
    @PostMapping("/approval")
    public ResponseEntity<String> testApprovalEmail(@RequestParam String toEmail) {
        try {
            // Créer une réservation de test
            Reservation testReservation = createTestReservation();
            testReservation.setEmail(toEmail);
            
            // Envoyer l'email
            emailService.sendApprovalNotification(testReservation);
            
            return ResponseEntity.ok("Email d'approbation envoyé avec succès à " + toEmail);
        } catch (Exception e) {
            log.error("Erreur lors du test d'envoi d'email d'approbation", e);
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }
    
    /**
     * Tester l'envoi d'email de rejet
     */
    @PostMapping("/rejection")
    public ResponseEntity<String> testRejectionEmail(@RequestParam String toEmail, 
                                                   @RequestParam(defaultValue = "Matériel non disponible") String reason) {
        try {
            // Créer une réservation de test
            Reservation testReservation = createTestReservation();
            testReservation.setEmail(toEmail);
            
            // Envoyer l'email
            emailService.sendRejectionNotification(testReservation, reason);
            
            return ResponseEntity.ok("Email de rejet envoyé avec succès à " + toEmail);
        } catch (Exception e) {
            log.error("Erreur lors du test d'envoi d'email de rejet", e);
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }
    
    /**
     * Créer une réservation de test
     */
    private Reservation createTestReservation() {
        Reservation reservation = new Reservation();
        reservation.setFirstName("Ahmed");
        reservation.setLastName("Benali");
        reservation.setPhone("+212 6 12 34 56 78");
        reservation.setEmail("test@example.com");
        reservation.setStartDate(LocalDate.now().plusDays(7));
        reservation.setEndDate(LocalDate.now().plusDays(10));
        reservation.setQuantity(2);
        reservation.setTotalAmount(new BigDecimal("1500.00"));
        reservation.setDepositAmount(new BigDecimal("450.00"));
        reservation.setNotes("Test de réservation pour vérifier le système d'email");
        reservation.setDeliveryAddress("123 Rue de la Paix, Casablanca");
        
        // Créer un pack de test
        Pack testPack = new Pack();
        testPack.setId(1L);
        testPack.setName("Pack Mariage Premium");
        testPack.setDescription("Pack complet pour mariage avec éclairage, sonorisation et décoration");
        testPack.setPrice(new BigDecimal("500.00"));
        reservation.setPack(testPack);
        
        return reservation;
    }
}

package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.*;
import com.afra7kom.backend.dto.AvailabilityCheckRequestDto;
import com.afra7kom.backend.service.ReservationService;
import com.afra7kom.backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Réservations", description = "API de gestion des réservations")
public class ReservationController {
    
    private final ReservationService reservationService;
    private final SecurityUtils securityUtils;
    
    /**
     * Créer une nouvelle réservation (public - invité ou client connecté)
     */
    @PostMapping
    @Operation(summary = "Créer une réservation", description = "Créer une nouvelle réservation pour un invité ou un client connecté")
    public ResponseEntity<ReservationResponseDto> createReservation(
            @Valid @RequestBody ReservationRequestDto request) {
        
        String userEmail = null;
        try {
            userEmail = securityUtils.getCurrentUser().map(user -> user.getEmail()).orElse(null);
        } catch (Exception e) {
            // Si l'utilisateur n'est pas authentifié, on continue comme invité
            log.debug("Utilisateur non authentifié, création en tant qu'invité");
        }
        
        log.info("Création de réservation par: {}", userEmail != null ? userEmail : "invité");
        
        ReservationResponseDto reservation = reservationService.createReservation(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }
    
    /**
     * Vérifier la disponibilité d'un produit (avec validation complète)
     */
    @PostMapping("/check-availability")
    @Operation(summary = "Vérifier la disponibilité", description = "Vérifier la disponibilité d'un produit sur une période donnée")
    public ResponseEntity<AvailabilityCheckDto> checkAvailability(
            @Valid @RequestBody ReservationRequestDto request) {
        
        log.info("Vérification de disponibilité pour: {} - {}", 
                request.getPackId() != null ? "pack " + request.getPackId() : "matériel " + request.getMaterielId(),
                request.getStartDate() + " à " + request.getEndDate());
        
        AvailabilityCheckDto availability = reservationService.checkAvailability(request);
        return ResponseEntity.ok(availability);
    }
    
    /**
     * Vérifier la disponibilité d'un produit (sans validation des champs personnels)
     */
    @PostMapping("/check-availability-simple")
    @Operation(summary = "Vérifier la disponibilité simple", description = "Vérifier la disponibilité d'un produit sans validation des champs personnels")
    public ResponseEntity<AvailabilityCheckDto> checkAvailabilitySimple(
            @Valid @RequestBody AvailabilityCheckRequestDto request) {
        
        log.info("Vérification simple de disponibilité pour: {} - {}", 
                request.getPackId() != null ? "pack " + request.getPackId() : "matériel " + request.getMaterielId(),
                request.getStartDate() + " à " + request.getEndDate());
        
        AvailabilityCheckDto availability = reservationService.checkAvailabilitySimple(request);
        return ResponseEntity.ok(availability);
    }
    
    /**
     * Obtenir la disponibilité d'un mois entier pour un produit
     */
    @GetMapping("/availability-month")
    @Operation(summary = "Disponibilité mensuelle", description = "Obtenir la disponibilité d'un mois entier pour un produit")
    public ResponseEntity<Map<String, AvailabilityCheckDto>> getMonthlyAvailability(
            @RequestParam(required = false) Long packId,
            @RequestParam(required = false) Long materielId,
            @RequestParam int year,
            @RequestParam int month) {
        
        // Validation : au moins un des deux paramètres doit être fourni
        if (packId == null && materielId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Récupération de la disponibilité mensuelle pour: {} - {}/{}", 
                packId != null ? "pack " + packId : "matériel " + materielId, month, year);
        
        Map<String, AvailabilityCheckDto> monthlyAvailability = reservationService.getMonthlyAvailability(packId, materielId, year, month);
        return ResponseEntity.ok(monthlyAvailability);
    }
    
    /**
     * Obtenir une réservation par ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une réservation", description = "Obtenir les détails d'une réservation par son ID")
    public ResponseEntity<ReservationResponseDto> getReservation(@PathVariable Long id) {
        
        log.info("Récupération de la réservation: {}", id);
        
        ReservationResponseDto reservation = reservationService.getReservationById(id);
        return ResponseEntity.ok(reservation);
    }
    
    /**
     * Obtenir la liste des réservations
     * - Admins/Agents voient toutes les réservations
     * - Utilisateurs connectés voient seulement leurs réservations
     */
    @GetMapping
    @Operation(summary = "Lister les réservations", description = "Obtenir la liste paginée des réservations avec filtres")
    public ResponseEntity<Page<ReservationResponseDto>> getReservations(
            @Parameter(description = "Filtres de recherche") ReservationFilterDto filter) {
        
        String userEmail = securityUtils.getCurrentUser().map(user -> user.getEmail()).orElse(null);
        boolean isAdmin = securityUtils.hasAnyRole("ADMIN", "AGENT");
        
        log.info("Récupération des réservations avec filtres par: {} (Admin: {})", userEmail, isAdmin);
        
        Page<ReservationResponseDto> reservations = reservationService.getReservations(filter, userEmail, isAdmin);
        return ResponseEntity.ok(reservations);
    }
    
    /**
     * Obtenir les réservations de l'utilisateur connecté
     */
    @GetMapping("/my-reservations")
    @Operation(summary = "Mes réservations", description = "Obtenir les réservations de l'utilisateur connecté")
    public ResponseEntity<Page<ReservationResponseDto>> getMyReservations(
            @Parameter(description = "Filtres de recherche") ReservationFilterDto filter) {
        
        String userEmail = securityUtils.getCurrentUser().map(user -> user.getEmail()).orElse(null);
        
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Récupération des réservations de l'utilisateur: {}", userEmail);
        
        // Forcer l'utilisateur à voir seulement ses réservations
        Page<ReservationResponseDto> reservations = reservationService.getReservations(filter, userEmail, false);
        return ResponseEntity.ok(reservations);
    }
    
    /**
     * Obtenir une réservation publique par ID (pour les invités)
     */
    @GetMapping("/public/{id}")
    @Operation(summary = "Obtenir une réservation publique", description = "Obtenir les détails d'une réservation par son ID (accès public)")
    public ResponseEntity<ReservationResponseDto> getPublicReservation(@PathVariable Long id) {
        
        log.info("Récupération publique de la réservation: {}", id);
        
        ReservationResponseDto reservation = reservationService.getReservationById(id);
        return ResponseEntity.ok(reservation);
    }
    
    /**
     * Valider ou rejeter une réservation (Admin/Agent)
     */
    @PutMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Valider/Rejeter une réservation", description = "Approuver ou rejeter une réservation (Admin/Agent uniquement)")
    public ResponseEntity<ReservationResponseDto> validateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationValidationDto validation) {
        
        String adminEmail = securityUtils.getCurrentUser().map(user -> user.getEmail()).orElse("inconnu");
        log.info("Validation de réservation {} par: {} - Statut: {}", id, adminEmail, validation.getStatus());
        
        ReservationResponseDto reservation = reservationService.validateReservation(id, validation, adminEmail);
        return ResponseEntity.ok(reservation);
    }
    
    /**
     * Annuler une réservation
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Annuler une réservation", description = "Annuler une réservation (client ou admin)")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id,
            @RequestParam String reason) {
        
        String userEmail = securityUtils.getCurrentUser().map(user -> user.getEmail()).orElse("inconnu");
        log.info("Annulation de réservation {} par: {} - Motif: {}", id, userEmail, reason);
        
        reservationService.cancelReservation(id, reason, userEmail);
        return ResponseEntity.noContent().build();
    }
    
    
    /**
     * Mettre à jour le statut d'une réservation (Admin/Agent)
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Mettre à jour le statut", description = "Mettre à jour le statut d'une réservation (Admin/Agent uniquement)")
    public ResponseEntity<ReservationResponseDto> updateReservationStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReservationStatusUpdateDto statusUpdate) {
        
        String adminEmail = securityUtils.getCurrentUser().map(user -> user.getEmail()).orElse("inconnu");
        log.info("Mise à jour du statut de la réservation {} par: {} - Nouveau statut: {}", id, adminEmail, statusUpdate.getStatus());
        
        ReservationResponseDto reservation = reservationService.updateReservationStatus(id, statusUpdate, adminEmail);
        return ResponseEntity.ok(reservation);
    }
    
    /**
     * Obtenir les statistiques des réservations (Admin/Agent)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Statistiques des réservations", description = "Obtenir les statistiques des réservations (Admin/Agent uniquement)")
    public ResponseEntity<Object> getReservationStats() {
        
        log.info("Récupération des statistiques par: {}", securityUtils.getCurrentUser().map(user -> user.getEmail()).orElse("inconnu"));
        
        // TODO: Implémenter les statistiques
        return ResponseEntity.ok(Map.of(
            "totalReservations", 0,
            "pendingReservations", 0,
            "approvedReservations", 0,
            "rejectedReservations", 0,
            "totalRevenue", 0.0
        ));
    }
}




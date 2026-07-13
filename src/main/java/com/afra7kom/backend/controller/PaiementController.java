package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.PaiementDto;
import com.afra7kom.backend.entity.Paiement;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.service.PaiementService;
import com.afra7kom.backend.service.FactureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Paiements", description = "Gestion des paiements et facturation")
public class PaiementController {

    private final PaiementService paiementService;
    private final FactureService factureService;

    @PostMapping
    @Operation(summary = "Créer un paiement", description = "Enregistrer un nouveau paiement pour une réservation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Paiement créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Réservation non trouvée"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<PaiementDto> createPaiement(@RequestBody Map<String, Object> request) {
        
        Long reservationId = Long.valueOf(request.get("reservationId").toString());
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        Paiement.TypePaiement type = Paiement.TypePaiement.valueOf(request.get("type").toString());
        Paiement.StatutPaiement statut = Paiement.StatutPaiement.valueOf(request.get("statut").toString());
        
        String referenceExterne = request.containsKey("referenceExterne") ? 
            request.get("referenceExterne").toString() : null;
        String notes = request.containsKey("notes") ? 
            request.get("notes").toString() : null;
        
        LocalDateTime dateEcheance = null;
        if (request.containsKey("dateEcheance") && request.get("dateEcheance") != null) {
            dateEcheance = LocalDateTime.parse(request.get("dateEcheance").toString());
        }
        
        String modeReglement = request.containsKey("modeReglement") ? 
            request.get("modeReglement").toString() : null;
        String banque = request.containsKey("banque") ? 
            request.get("banque").toString() : null;
        String numeroCheque = request.containsKey("numeroCheque") ? 
            request.get("numeroCheque").toString() : null;
        String numeroVirement = request.containsKey("numeroVirement") ? 
            request.get("numeroVirement").toString() : null;

        PaiementDto paiement = paiementService.createPaiement(
            reservationId, amount, type, statut, referenceExterne, notes,
            dateEcheance, modeReglement, banque, numeroCheque, numeroVirement
        );

        return ResponseEntity.status(201).body(paiement);
    }

    @GetMapping
    @Operation(summary = "Lister les paiements", description = "Récupérer la liste paginée des paiements avec filtres")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des paiements récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<Page<PaiementDto>> getAllPaiements(
            @Parameter(description = "Filtre par réservation") @RequestParam(required = false) Long reservationId,
            @Parameter(description = "Filtre par type") @RequestParam(required = false) Paiement.TypePaiement type,
            @Parameter(description = "Filtre par statut") @RequestParam(required = false) Paiement.StatutPaiement statut,
            @Parameter(description = "Date de début") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        boolean isAdminOrAgent = currentUser.hasAnyRole(String.valueOf(List.of("ADMIN", "MANAGER", "AGENT")));

        Page<PaiementDto> paiements;
        
        if (reservationId != null || type != null || statut != null || startDate != null || endDate != null) {
            paiements = paiementService.searchPaiements(reservationId, type, statut, startDate, endDate, pageable);
        } else {
            if (isAdminOrAgent) {
                paiements = paiementService.getAllPaiements(pageable);
            } else {
                // Les clients ne voient que les paiements de leurs réservations
                paiements = paiementService.searchPaiements(null, null, null, null, null, pageable);
            }
        }

        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un paiement", description = "Récupérer les détails d'un paiement")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Paiement trouvé"),
        @ApiResponse(responseCode = "404", description = "Paiement non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<PaiementDto> getPaiementById(@PathVariable Long id) {
        PaiementDto paiement = paiementService.getPaiementById(id);
        return ResponseEntity.ok(paiement);
    }

    @GetMapping("/reservation/{reservationId}")
    @Operation(summary = "Paiements d'une réservation", description = "Récupérer tous les paiements d'une réservation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT') or @reservationService.getReservationById(#reservationId).userId == authentication.principal.id")
    public ResponseEntity<List<PaiementDto>> getPaiementsByReservation(@PathVariable Long reservationId) {
        List<PaiementDto> paiements = paiementService.getPaiementsByReservation(reservationId);
        return ResponseEntity.ok(paiements);
    }

    @PatchMapping("/{id}/marquer-paye")
    @Operation(summary = "Marquer un paiement comme payé", description = "Marquer un paiement comme payé avec date et référence")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<PaiementDto> marquerCommePaye(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        LocalDateTime datePaiement = null;
        if (request.containsKey("datePaiement") && request.get("datePaiement") != null) {
            datePaiement = LocalDateTime.parse(request.get("datePaiement").toString());
        }
        
        String referenceExterne = request.containsKey("referenceExterne") ? 
            request.get("referenceExterne").toString() : null;
        String notes = request.containsKey("notes") ? 
            request.get("notes").toString() : null;

        PaiementDto paiement = paiementService.marquerCommePaye(id, datePaiement, referenceExterne, notes);
        return ResponseEntity.ok(paiement);
    }

    @GetMapping("/statistiques")
    @Operation(summary = "Statistiques des revenus", description = "Obtenir les statistiques des revenus par période")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getStatistiquesRevenus(
            @Parameter(description = "Date de début") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        Map<String, Object> statistiques = paiementService.getStatistiquesRevenus(startDate, endDate);
        return ResponseEntity.ok(statistiques);
    }

    @GetMapping("/situation/{reservationId}")
    @Operation(summary = "Situation financière d'une réservation", description = "Obtenir la situation financière complète d'une réservation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT') or @reservationService.getReservationById(#reservationId).userId == authentication.principal.id")
    public ResponseEntity<Map<String, Object>> getSituationPaiement(@PathVariable Long reservationId) {
        Map<String, Object> situation = paiementService.getSituationPaiementReservation(reservationId);
        return ResponseEntity.ok(situation);
    }

    // Génération de factures PDF
    @GetMapping("/{id}/facture")
    @Operation(summary = "Télécharger la facture PDF", description = "Générer et télécharger la facture PDF d'un paiement")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Facture générée avec succès"),
        @ApiResponse(responseCode = "404", description = "Paiement non trouvé"),
        @ApiResponse(responseCode = "500", description = "Erreur lors de la génération de la facture")
    })
    public ResponseEntity<byte[]> telechargerFacture(@PathVariable Long id) {
        try {
            byte[] pdfContent = factureService.genererFacturePDF(id);
            PaiementDto paiement = paiementService.getPaiementById(id);
            
            String filename = "facture_" + paiement.getFactureNumero() + ".pdf";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfContent.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
                    
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération de la facture: " + e.getMessage());
        }
    }

    @GetMapping("/recapitulatif/{reservationId}")
    @Operation(summary = "Télécharger le récapitulatif PDF", description = "Générer et télécharger le récapitulatif PDF d'une réservation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT') or @reservationService.getReservationById(#reservationId).userId == authentication.principal.id")
    public ResponseEntity<byte[]> telechargerRecapitulatif(@PathVariable Long reservationId) {
        try {
            byte[] pdfContent = factureService.genererRecapitulatifReservation(reservationId);
            
            String filename = "recapitulatif_reservation_" + reservationId + ".pdf";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfContent.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
                    
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du récapitulatif: " + e.getMessage());
        }
    }

    @GetMapping("/en-retard")
    @Operation(summary = "Paiements en retard", description = "Récupérer la liste des paiements en retard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<List<PaiementDto>> getPaiementsEnRetard() {
        List<PaiementDto> paiementsEnRetard = paiementService.getPaiementsEnRetard()
                .stream()
                .map(PaiementDto::fromEntitySimple)
                .toList();
        
        return ResponseEntity.ok(paiementsEnRetard);
    }

    @GetMapping("/types")
    @Operation(summary = "Types de paiement", description = "Récupérer la liste des types de paiement disponibles")
    public ResponseEntity<Map<String, String>> getTypesPaiement() {
        Map<String, String> types = Map.of(
            "ESPECE", "Espèces",
            "VIREMENT", "Virement bancaire",
            "CHEQUE", "Chèque",
            "CARTE_BANCAIRE", "Carte bancaire",
            "AUTRE", "Autre"
        );
        return ResponseEntity.ok(types);
    }

    @GetMapping("/statuts")
    @Operation(summary = "Statuts de paiement", description = "Récupérer la liste des statuts de paiement disponibles")
    public ResponseEntity<Map<String, String>> getStatutsPaiement() {
        Map<String, String> statuts = Map.of(
            "ACOMPTE", "Acompte",
            "SOLDE", "Soldé",
            "IMPAYE", "Impayé",
            "REMBOURSE", "Remboursé",
            "PARTIEL", "Partiel"
        );
        return ResponseEntity.ok(statuts);
    }
}




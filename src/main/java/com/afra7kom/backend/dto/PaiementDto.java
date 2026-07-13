package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Paiement;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaiementDto {

    private Long id;
    private Long reservationId;
    private String reservationReference;
    private BigDecimal amount;
    private Paiement.TypePaiement type;
    private String typeDisplayName;
    private Paiement.StatutPaiement statut;
    private String statutDisplayName;
    private String referenceExterne;
    private String notes;
    private String factureNumero;
    private Boolean factureGeneree;
    private LocalDateTime dateEcheance;
    private LocalDateTime datePaiement;
    private String modeReglement;
    private String banque;
    private String numeroCheque;
    private String numeroVirement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isPaye;
    private Boolean isEnRetard;
    private String formattedAmount;

    public static PaiementDto fromEntity(Paiement paiement) {
        PaiementDto dto = new PaiementDto();
        dto.setId(paiement.getId());
        dto.setReservationId(paiement.getReservation().getId());
        dto.setReservationReference("RES-" + paiement.getReservation().getId());
        dto.setAmount(paiement.getAmount());
        dto.setType(paiement.getType());
        dto.setTypeDisplayName(paiement.getTypeDisplayName());
        dto.setStatut(paiement.getStatut());
        dto.setStatutDisplayName(paiement.getStatutDisplayName());
        dto.setReferenceExterne(paiement.getReferenceExterne());
        dto.setNotes(paiement.getNotes());
        dto.setFactureNumero(paiement.getFactureNumero());
        dto.setFactureGeneree(paiement.getFactureGeneree());
        dto.setDateEcheance(paiement.getDateEcheance());
        dto.setDatePaiement(paiement.getDatePaiement());
        dto.setModeReglement(paiement.getModeReglement());
        dto.setBanque(paiement.getBanque());
        dto.setNumeroCheque(paiement.getNumeroCheque());
        dto.setNumeroVirement(paiement.getNumeroVirement());
        dto.setCreatedAt(paiement.getCreatedAt());
        dto.setUpdatedAt(paiement.getUpdatedAt());
        dto.setIsPaye(paiement.isPaye());
        dto.setIsEnRetard(paiement.isEnRetard());
        dto.setFormattedAmount(paiement.getFormattedAmount());
        
        return dto;
    }

    // Version simplifiée pour les listes
    public static PaiementDto fromEntitySimple(Paiement paiement) {
        PaiementDto dto = new PaiementDto();
        dto.setId(paiement.getId());
        dto.setReservationId(paiement.getReservation().getId());
        dto.setAmount(paiement.getAmount());
        dto.setType(paiement.getType());
        dto.setTypeDisplayName(paiement.getTypeDisplayName());
        dto.setStatut(paiement.getStatut());
        dto.setStatutDisplayName(paiement.getStatutDisplayName());
        dto.setFactureNumero(paiement.getFactureNumero());
        dto.setDatePaiement(paiement.getDatePaiement());
        dto.setCreatedAt(paiement.getCreatedAt());
        dto.setIsPaye(paiement.isPaye());
        dto.setIsEnRetard(paiement.isEnRetard());
        dto.setFormattedAmount(paiement.getFormattedAmount());
        
        return dto;
    }
}

// Request DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
class PaiementRequest {
    private Long reservationId;
    private BigDecimal amount;
    private Paiement.TypePaiement type;
    private Paiement.StatutPaiement statut;
    private String referenceExterne;
    private String notes;
    private LocalDateTime dateEcheance;
    private String modeReglement;
    private String banque;
    private String numeroCheque;
    private String numeroVirement;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class MarquerPayeRequest {
    private LocalDateTime datePaiement;
    private String referenceExterne;
    private String notes;
}

// Response DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
class StatistiquesRevenusDto {
    private BigDecimal totalRevenus;
    private BigDecimal totalAcomptes;
    private BigDecimal totalSoldes;
    private Integer nombrePaiements;
    private Integer nombreFactures;
    private BigDecimal moyennePaiement;
    private RevenusParMoisDto[] revenusParMois;
    private RevenusParTypeDto[] revenusParType;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class RevenusParMoisDto {
    private Integer annee;
    private Integer mois;
    private String moisNom;
    private BigDecimal montant;
    private Integer nombrePaiements;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class RevenusParTypeDto {
    private Paiement.TypePaiement type;
    private String typeDisplayName;
    private BigDecimal montant;
    private Integer nombrePaiements;
    private Double pourcentage;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class SituationPaiementDto {
    private Long reservationId;
    private BigDecimal totalReservation;
    private BigDecimal totalPaye;
    private BigDecimal resteAPayerresteAPayer;
    private BigDecimal acompteAttendu;
    private BigDecimal acomptePaye;
    private Boolean acompteComplet;
    private Boolean reservationSoldee;
    private Integer nombrePaiements;
    private PaiementDto[] paiements;
}




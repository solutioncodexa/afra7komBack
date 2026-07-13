package com.afra7kom.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@EntityListeners(AuditingEntityListener.class)
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    @NotNull(message = "Reservation is required")
    private Reservation reservation;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @NotNull(message = "Payment type is required")
    private TypePaiement type;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @NotNull(message = "Payment status is required")
    private StatutPaiement statut;

    @Column(name = "reference_externe")
    private String referenceExterne;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "facture_numero")
    private String factureNumero;

    @Column(name = "facture_generee")
    private Boolean factureGeneree = false;

    @Column(name = "date_echeance")
    private LocalDateTime dateEcheance;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "mode_reglement")
    private String modeReglement;

    @Column(name = "banque")
    private String banque;

    @Column(name = "numero_cheque")
    private String numeroCheque;

    @Column(name = "numero_virement")
    private String numeroVirement;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum TypePaiement {
        ESPECE("Espèces"),
        VIREMENT("Virement bancaire"),
        CHEQUE("Chèque"),
        CARTE_BANCAIRE("Carte bancaire"),
        AUTRE("Autre");

        private final String displayName;

        TypePaiement(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum StatutPaiement {
        ACOMPTE("Acompte"),
        SOLDE("Soldé"),
        IMPAYE("Impayé"),
        REMBOURSE("Remboursé"),
        PARTIEL("Partiel");

        private final String displayName;

        StatutPaiement(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Constructeurs utilitaires
    public Paiement(Reservation reservation, BigDecimal amount, TypePaiement type, StatutPaiement statut) {
        this.reservation = reservation;
        this.amount = amount;
        this.type = type;
        this.statut = statut;
    }

    // Méthodes utilitaires
    public boolean isAcompte() {
        return statut == StatutPaiement.ACOMPTE;
    }

    public boolean isSolde() {
        return statut == StatutPaiement.SOLDE;
    }

    public boolean isImpaye() {
        return statut == StatutPaiement.IMPAYE;
    }

    public boolean isPaye() {
        return datePaiement != null && 
               (statut == StatutPaiement.ACOMPTE || statut == StatutPaiement.SOLDE || statut == StatutPaiement.PARTIEL);
    }

    public boolean isEnRetard() {
        return dateEcheance != null && 
               LocalDateTime.now().isAfter(dateEcheance) && 
               !isPaye();
    }

    public void marquerCommePaye() {
        this.datePaiement = LocalDateTime.now();
        if (this.statut == StatutPaiement.IMPAYE) {
            // Déterminer si c'est un acompte ou un solde basé sur le montant
            BigDecimal totalReservation = reservation.getTotalAmount();
            BigDecimal acompteAttendu = totalReservation.multiply(new BigDecimal("0.30"));
            
            if (this.amount.compareTo(acompteAttendu) <= 0) {
                this.statut = StatutPaiement.ACOMPTE;
            } else if (this.amount.compareTo(totalReservation) >= 0) {
                this.statut = StatutPaiement.SOLDE;
            } else {
                this.statut = StatutPaiement.PARTIEL;
            }
        }
    }

    public void genererNumeroFacture() {
        if (factureNumero == null) {
            // Format: FAC-YYYY-MM-RRRRR-PP
            // YYYY-MM: année-mois
            // RRRRR: ID réservation sur 5 chiffres
            // PP: ID paiement sur 2 chiffres
            LocalDateTime now = LocalDateTime.now();
            this.factureNumero = String.format("FAC-%04d-%02d-%05d-%02d",
                now.getYear(),
                now.getMonthValue(),
                reservation.getId(),
                this.id != null ? this.id : 0);
        }
    }

    public String getTypeDisplayName() {
        return type != null ? type.getDisplayName() : "";
    }

    public String getStatutDisplayName() {
        return statut != null ? statut.getDisplayName() : "";
    }

    public String getFormattedAmount() {
        return String.format("%.2f MAD", amount);
    }

    // Validation métier
    @PrePersist
    @PreUpdate
    private void validate() {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        
        if (reservation != null && amount != null) {
            BigDecimal totalReservation = reservation.getTotalAmount();
            if (amount.compareTo(totalReservation) > 0) {
                throw new IllegalArgumentException("Le montant ne peut pas dépasser le total de la réservation");
            }
        }

        // Générer numéro de facture si nécessaire
        if (isPaye() && factureNumero == null) {
            genererNumeroFacture();
            factureGeneree = true;
        }
    }

    @Override
    public String toString() {
        return "Paiement{" +
                "id=" + id +
                ", reservationId=" + (reservation != null ? reservation.getId() : null) +
                ", amount=" + amount +
                ", type=" + type +
                ", statut=" + statut +
                ", factureNumero='" + factureNumero + '\'' +
                ", datePaiement=" + datePaiement +
                '}';
    }
}




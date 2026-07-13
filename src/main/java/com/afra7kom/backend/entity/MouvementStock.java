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
@Table(name = "mouvements_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@EntityListeners(AuditingEntityListener.class)
public class MouvementStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materiel_id", nullable = false)
    @NotNull(message = "Le matériel est requis")
    private Materiel materiel;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "L'utilisateur est requis")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @NotNull(message = "Le type de mouvement est requis")
    private TypeMouvement type;

    @Column(name = "quantity", nullable = false)
    @NotNull(message = "La quantité est requise")
    @Positive(message = "La quantité doit être positive")
    private Integer quantity;

    @Column(name = "date", nullable = false)
    @NotNull(message = "La date est requise")
    private LocalDateTime date;

    @Column(name = "prix_unitaire", precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(name = "cout_total", precision = 10, scale = 2)
    private BigDecimal coutTotal;

    @Column(name = "reference_externe")
    private String referenceExterne;

    @Column(name = "numero_facture")
    private String numeroFacture;

    @Column(name = "fournisseur")
    private String fournisseur;

    @Column(name = "bon_livraison")
    private String bonLivraison;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "motif")
    private String motif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(name = "stock_avant")
    private Integer stockAvant;

    @Column(name = "stock_apres")
    private Integer stockApres;

    @Column(name = "validé")
    private Boolean valide = true;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validé_par")
    private User validePar;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum TypeMouvement {
        ACHAT("Achat", true, "Entrée de stock par achat"),
        RETOUR("Retour", true, "Retour de matériel après location"),
        TRANSFERT_ENTRANT("Transfert Entrant", true, "Transfert depuis un autre dépôt"),
        CORRECTION_POSITIVE("Correction +", true, "Correction d'inventaire positive"),
        RESERVATION("Réservation", false, "Sortie pour réservation"),
        CASSE("Casse", false, "Matériel cassé ou hors service"),
        PERTE("Perte", false, "Matériel perdu"),
        TRANSFERT_SORTANT("Transfert Sortant", false, "Transfert vers un autre dépôt"),
        CORRECTION_NEGATIVE("Correction -", false, "Correction d'inventaire négative"),
        MAINTENANCE("Maintenance", false, "Sortie pour maintenance"),
        VENTE("Vente", false, "Vente de matériel");

        private final String displayName;
        private final boolean entree;
        private final String description;

        TypeMouvement(String displayName, boolean entree, String description) {
            this.displayName = displayName;
            this.entree = entree;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isEntree() {
            return entree;
        }

        public boolean isSortie() {
            return !entree;
        }

        public String getDescription() {
            return description;
        }

        public String getSymbole() {
            return entree ? "+" : "-";
        }
    }

    // Constructeurs utilitaires
    public MouvementStock(Materiel materiel, User user, TypeMouvement type,
                         Integer quantity, LocalDateTime date) {
        this.materiel = materiel;
        this.user = user;
        this.type = type;
        this.quantity = quantity;
        this.date = date;
    }

    public MouvementStock(Materiel materiel,  User user, TypeMouvement type,
                         Integer quantity, LocalDateTime date, String notes) {
        this(materiel, user, type, quantity, date);
        this.notes = notes;
    }

    // Méthodes utilitaires
    public boolean isEntree() {
        return type.isEntree();
    }

    public boolean isSortie() {
        return type.isSortie();
    }

    public int getQuantiteSigne() {
        return isEntree() ? quantity : -quantity;
    }

    public boolean isValide() {
        return valide != null && valide;
    }

    public void valider(User validateur) {
        this.valide = true;
        this.dateValidation = LocalDateTime.now();
        this.validePar = validateur;
    }

    public void invalider() {
        this.valide = false;
        this.dateValidation = null;
        this.validePar = null;
    }

    public void calculerCoutTotal() {
        if (prixUnitaire != null && quantity != null) {
            this.coutTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public String getTypeDisplayName() {
        return type != null ? type.getDisplayName() : "";
    }

    public String getTypeSymbole() {
        return type != null ? type.getSymbole() : "";
    }

    public String getFormattedAmount() {
        if (coutTotal != null) {
            return String.format("%.2f MAD", coutTotal);
        }
        return "";
    }

    public boolean isLieAReservation() {
        return reservation != null;
    }

    public boolean isTransfert() {
        return type == TypeMouvement.TRANSFERT_ENTRANT || type == TypeMouvement.TRANSFERT_SORTANT;
    }

    public boolean isCorrection() {
        return type == TypeMouvement.CORRECTION_POSITIVE || type == TypeMouvement.CORRECTION_NEGATIVE;
    }

    public boolean isAchat() {
        return type == TypeMouvement.ACHAT;
    }

    public boolean isCasse() {
        return type == TypeMouvement.CASSE || type == TypeMouvement.PERTE;
    }

    public String getStatutValidation() {
        if (valide == null || !valide) {
            return "En attente";
        }
        return "Validé";
    }

    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(type.getDisplayName());
        
        if (materiel != null) {
            desc.append(" - ").append(materiel.getName());
        }
        
        desc.append(" (").append(getTypeSymbole()).append(quantity).append(")");

        
        return desc.toString();
    }

    // Validation métier
    @PrePersist
    @PreUpdate
    private void validate() {
        if (quantity != null && quantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être positive");
        }
        
        if (date == null) {
            date = LocalDateTime.now();
        }
        
        // Calculer le coût total si possible
        calculerCoutTotal();
        
        // Valider automatiquement certains types de mouvements
        if (valide == null) {
            valide = !isCorrection(); // Les corrections nécessitent une validation manuelle
        }
    }

    @Override
    public String toString() {
        return "MouvementStock{" +
                "id=" + id +
                ", materielId=" + (materiel != null ? materiel.getId() : null) +
                ", type=" + type +
                ", quantity=" + quantity +
                ", date=" + date +
                ", valide=" + valide +
                '}';
    }
}




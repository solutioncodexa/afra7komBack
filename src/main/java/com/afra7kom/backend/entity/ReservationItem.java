package com.afra7kom.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "reservation_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@EntityListeners(AuditingEntityListener.class)
public class ReservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    @NotNull(message = "Reservation is required")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materiel_id")
    private Materiel materiel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id")
    private Pack pack;

    @Column(name = "quantity", nullable = false)
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Unit price is required")
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "notes")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ItemType type;

    @Column(name = "delivered")
    private Boolean delivered = false;

    @Column(name = "returned")
    private Boolean returned = false;

    @Column(name = "damage_notes")
    private String damageNotes;

    @Column(name = "damage_cost", precision = 10, scale = 2)
    private BigDecimal damageCost;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ItemType {
        PACK("Pack"),
        MATERIEL("Matériel");

        private final String displayName;

        ItemType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Constructeurs utilitaires
    public ReservationItem(Reservation reservation, Pack pack, Integer quantity, BigDecimal unitPrice) {
        this.reservation = reservation;
        this.pack = pack;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.type = ItemType.PACK;
        calculateTotalPrice();
    }

    public ReservationItem(Reservation reservation, Materiel materiel, Integer quantity, BigDecimal unitPrice) {
        this.reservation = reservation;
        this.materiel = materiel;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.type = ItemType.MATERIEL;
        calculateTotalPrice();
    }

    // Méthodes utilitaires
    public void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public String getItemName() {
        return type == ItemType.PACK ? 
            (pack != null ? pack.getName() : "Pack inconnu") :
            (materiel != null ? materiel.getName() : "Matériel inconnu");
    }

    public Long getItemId() {
        return type == ItemType.PACK ? 
            (pack != null ? pack.getId() : null) :
            (materiel != null ? materiel.getId() : null);
    }

    public String getItemDescription() {
        return type == ItemType.PACK ? 
            (pack != null ? pack.getDescription() : null) :
            (materiel != null ? materiel.getDescription() : null);
    }

    public String getItemImageUrl() {
        if (type == ItemType.PACK && pack != null) {
            return pack.getPrimaryImage();
        } else if (type == ItemType.MATERIEL && materiel != null) {
            return materiel.getPrimaryImage();
        }
        return null;
    }

    public boolean isAvailableForPeriod(LocalDateTime start, LocalDateTime end) {
        if (type == ItemType.PACK && pack != null) {
            return pack.isAvailable();
        } else if (type == ItemType.MATERIEL && materiel != null) {
            return materiel.getAvailableQuantity() >= quantity;
        }
        return false;
    }

    public boolean isDelivered() {
        return delivered != null && delivered;
    }

    public boolean isReturned() {
        return returned != null && returned;
    }

    public boolean hasDamage() {
        return damageNotes != null && !damageNotes.trim().isEmpty();
    }

    @PrePersist
    @PreUpdate
    private void validateItem() {
        if (pack == null && materiel == null) {
            throw new IllegalStateException("ReservationItem must have either a pack or a materiel");
        }
        if (pack != null && materiel != null) {
            throw new IllegalStateException("ReservationItem cannot have both pack and materiel");
        }
        
        // Définir le type automatiquement
        if (pack != null) {
            this.type = ItemType.PACK;
        } else if (materiel != null) {
            this.type = ItemType.MATERIEL;
        }
        
        // Calculer le prix total
        calculateTotalPrice();
    }

    @Override
    public String toString() {
        return "ReservationItem{" +
                "id=" + id +
                ", reservationId=" + (reservation != null ? reservation.getId() : null) +
                ", type=" + type +
                ", itemId=" + getItemId() +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                '}';
    }
}




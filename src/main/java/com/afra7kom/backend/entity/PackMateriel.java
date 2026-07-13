package com.afra7kom.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "pack_materiels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = {"pack", "materiel"})
@EntityListeners(AuditingEntityListener.class)
public class PackMateriel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id", nullable = false)
    private Pack pack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materiel_id", nullable = false)
    private Materiel materiel;

    @Column(name = "quantity", nullable = false)
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Column(name = "is_optional")
    private Boolean isOptional = false;

    @Column(name = "notes")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PackMateriel(Pack pack, Materiel materiel, Integer quantity) {
        this.pack = pack;
        this.materiel = materiel;
        this.quantity = quantity;
    }

    public PackMateriel(Pack pack, Materiel materiel, Integer quantity, Boolean isOptional) {
        this.pack = pack;
        this.materiel = materiel;
        this.quantity = quantity;
        this.isOptional = isOptional;
    }

    // Méthodes utilitaires
    public boolean isAvailable() {
        return materiel.getAvailableQuantity() >= quantity;
    }

    @Override
    public String toString() {
        return "PackMateriel{" +
                "id=" + id +
                ", packId=" + (pack != null ? pack.getId() : null) +
                ", materielId=" + (materiel != null ? materiel.getId() : null) +
                ", quantity=" + quantity +
                ", isOptional=" + isOptional +
                '}';
    }
}




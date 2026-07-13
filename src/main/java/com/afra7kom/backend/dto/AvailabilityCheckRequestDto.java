package com.afra7kom.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * DTO pour la vérification de disponibilité sans validation des champs personnels
 */
public class AvailabilityCheckRequestDto {
    
    @NotNull(message = "L'ID du pack est obligatoire")
    private Long packId;
    
    private Long materielId;
    
    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;
    
    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate endDate;
    
    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    private Integer quantity;

    // Constructeurs
    public AvailabilityCheckRequestDto() {}

    public AvailabilityCheckRequestDto(Long packId, Long materielId, LocalDate startDate, LocalDate endDate, Integer quantity) {
        this.packId = packId;
        this.materielId = materielId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.quantity = quantity;
    }

    // Getters et Setters
    public Long getPackId() {
        return packId;
    }

    public void setPackId(Long packId) {
        this.packId = packId;
    }

    public Long getMaterielId() {
        return materielId;
    }

    public void setMaterielId(Long materielId) {
        this.materielId = materielId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

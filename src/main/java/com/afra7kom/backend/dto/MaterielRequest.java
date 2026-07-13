package com.afra7kom.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterielRequest {
    private String name;
    private String description;
    private String type; // Nouveau: type d'équipement
    private BigDecimal price;
    @Deprecated
    private String imageUrl; // Obsolète - utiliser ProductImage à la place
    private Integer quantity;
    private Integer minimumStock;
    private Boolean active;
    private Long categorieId;
    private BigDecimal packPrice; // Nouveau: prix du pack
    private List<Long> selectedMaterials; // Nouveau: matériels sélectionnés
    private Boolean alwaysAvailable; // Nouveau: toujours disponible
}

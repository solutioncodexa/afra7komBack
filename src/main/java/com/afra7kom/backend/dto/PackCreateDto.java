package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.PackType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackCreateDto {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    @Deprecated
    private String imageUrl; // Obsolète - utiliser ProductImage à la place
    private Boolean active = true;
    private Long categoryId;
    private PackType type = PackType.PACK; // Type du produit : PACK, BUFFET, PACK_BUFFET, MATERIEL, CADEAU
    
    // IDs des images de la galerie à associer au pack
    private List<Long> imageIds;
    
    // Matériels avec quantités
    private List<PackMaterielCreateDto> materials;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackMaterielCreateDto {
        private Long materielId;
        private Integer quantity;
    }
}


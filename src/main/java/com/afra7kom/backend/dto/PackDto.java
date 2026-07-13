package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.PackMateriel;
import com.afra7kom.backend.entity.PackType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackDto {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean active;
    private PackType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private CategorieDto categorie;
    private List<PackMaterielDto> materiels;
    private List<GalleryDto> galleries;
    private List<String> images;
    private String primaryImage;
    private Boolean isFavorite;
    private Boolean isAvailable;
    private BigDecimal totalValue;
    private Integer totalItems;
    private Integer totalMateriels;
    private Integer totalGalleries;

    public static PackDto fromEntity(Pack pack) {
        PackDto dto = new PackDto();
        dto.setId(pack.getId());
        dto.setName(pack.getName());
        dto.setDescription(pack.getDescription());
        dto.setPrice(pack.getPrice());
        dto.setActive(pack.getActive());
        dto.setType(pack.getType());
        dto.setCreatedAt(pack.getCreatedAt());
        dto.setUpdatedAt(pack.getUpdatedAt());
        
        // Catégorie
        if (pack.getCategorie() != null) {
            dto.setCategorie(CategorieDto.fromEntity(pack.getCategorie()));
        }
        
        // Matériels du pack
        if (pack.getPackMateriels() != null) {
            dto.setMateriels(pack.getPackMateriels().stream()
                    .map(PackMaterielDto::fromEntity)
                    .collect(Collectors.toList()));
            dto.setTotalMateriels(pack.getPackMateriels().size());
        }
        
        // Galeries du pack (à supprimer si plus utilisées)
        // Note: Les galeries sont maintenant gérées par ProductImage
        
        // Images du pack
        if (pack.getImages() != null) {
            dto.setImages(pack.getActiveImages());
            
            // Image primaire
            String primaryImageUrl = pack.getPrimaryImage();
            if (primaryImageUrl != null) {
                dto.setPrimaryImage(primaryImageUrl);
            }
        }
        
        // Statuts calculés
        dto.setIsAvailable(pack.isAvailable());
        dto.setTotalValue(pack.calculateTotalValue());
        dto.setTotalItems(pack.getTotalItems());
        
        return dto;
    }

    public static PackDto fromEntityWithFavorite(Pack pack, boolean isFavorite) {
        PackDto dto = fromEntity(pack);
        dto.setIsFavorite(isFavorite);
        return dto;
    }

    // Version simplifiée pour les listes
    public static PackDto fromEntitySimple(Pack pack) {
        PackDto dto = new PackDto();
        dto.setId(pack.getId());
        dto.setName(pack.getName());
        dto.setDescription(pack.getDescription());
        dto.setPrice(pack.getPrice());
        dto.setActive(pack.getActive());
        dto.setType(pack.getType());
        dto.setCreatedAt(pack.getCreatedAt());
        
        if (pack.getCategorie() != null) {
            dto.setCategorie(CategorieDto.fromEntity(pack.getCategorie()));
        }
        
        // Compter les matériels et galeries pour l'affichage en liste
        if (pack.getPackMateriels() != null) {
            dto.setTotalMateriels(pack.getPackMateriels().size());
        }
        
        // Total galleries maintenant basé sur les images
        if (pack.getImages() != null) {
            dto.setTotalGalleries(pack.getActiveImages().size());
        }
        
        // Images du pack (nécessaires pour l'affichage en liste)
        if (pack.getImages() != null) {
            dto.setImages(pack.getActiveImages());
            
            // Image primaire
            String primaryImageUrl = pack.getPrimaryImage();
            if (primaryImageUrl != null) {
                dto.setPrimaryImage(primaryImageUrl);
            }
        }
        
        dto.setIsAvailable(pack.isAvailable());
        dto.setTotalItems(pack.getTotalItems());
        
        return dto;
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class PackRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean active;
    private PackType type;
    private Long categorieId;
    private List<PackMaterielRequest> materiels;
    private List<Long> galleryIds;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class PackMaterielRequest {
    private Long materielId;
    private Integer quantity;
    private Boolean isOptional;
    private String notes;
}




package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Materiel;
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
public class MaterielDto {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private Integer availableQuantity;
    private Integer minimumStock;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private CategorieDto categorie;
    private List<String> images;
    private String primaryImage;
    private Boolean isFavorite;
    private Boolean isLowStock;
    private Boolean isAvailable;

    public static MaterielDto fromEntity(Materiel materiel) {
        MaterielDto dto = new MaterielDto();
        dto.setId(materiel.getId());
        dto.setName(materiel.getName());
        dto.setDescription(materiel.getDescription());
        dto.setPrice(materiel.getPrice());
        dto.setQuantity(materiel.getTotalQuantity());
        dto.setAvailableQuantity(materiel.getAvailableQuantity());
        dto.setMinimumStock(materiel.getMinimumStock());
        dto.setActive(materiel.getActive());
        dto.setCreatedAt(materiel.getCreatedAt());
        dto.setUpdatedAt(materiel.getUpdatedAt());
        
        // Catégorie
        if (materiel.getCategorie() != null) {
            dto.setCategorie(CategorieDto.fromEntity(materiel.getCategorie()));
        }
        
        // Images
        if (materiel.getImages() != null) {
            dto.setImages(materiel.getActiveImages());
            
            // Image primaire
            String primaryImageUrl = materiel.getPrimaryImage();
            if (primaryImageUrl != null) {
                dto.setPrimaryImage(primaryImageUrl);
            }
        }
        
        // Statuts calculés
        dto.setIsLowStock(materiel.isLowStock());
        dto.setIsAvailable(materiel.isAvailable());
        
        return dto;
    }

    public static MaterielDto fromEntityWithFavorite(Materiel materiel, boolean isFavorite) {
        MaterielDto dto = fromEntity(materiel);
        dto.setIsFavorite(isFavorite);
        return dto;
    }
}




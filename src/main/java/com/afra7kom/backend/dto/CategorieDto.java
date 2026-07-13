package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Categorie;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorieDto {

    private Long id;
    private String name;
    private String description;
    private Boolean active;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> images;
    private String primaryImage;
    private Long packsCount;
    private Long materielsCount;

    public static CategorieDto fromEntity(Categorie categorie) {
        CategorieDto dto = new CategorieDto();
        dto.setId(categorie.getId());
        dto.setName(categorie.getName());
        dto.setDescription(categorie.getDescription());
        dto.setActive(categorie.getActive());
        dto.setSortOrder(categorie.getSortOrder());
        dto.setCreatedAt(categorie.getCreatedAt());
        dto.setUpdatedAt(categorie.getUpdatedAt());
        
        // Images
        if (categorie.getImages() != null) {
            dto.setImages(categorie.getActiveImages());
            String primaryImageUrl = categorie.getPrimaryImage();
            if (primaryImageUrl != null) {
                dto.setPrimaryImage(primaryImageUrl);
            }
        }
        
        // Compter les éléments associés si disponibles
        if (categorie.getPacks() != null) {
            dto.setPacksCount((long) categorie.getPacks().size());
        }
        if (categorie.getMateriels() != null) {
            dto.setMaterielsCount((long) categorie.getMateriels().size());
        }
        
        return dto;
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CategorieRequest {
    private String name;
    private String description;
    private Boolean active;
    private Integer sortOrder;
}




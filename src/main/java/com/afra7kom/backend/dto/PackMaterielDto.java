package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.PackMateriel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackMaterielDto {

    private Long id;
    private Long materielId;
    private String materielName;
    private String materielDescription;
    private BigDecimal materielPrice;
    private String materielImageUrl;
    private Integer quantity;
    private String categoryName;

    public static PackMaterielDto fromEntity(PackMateriel packMateriel) {
        PackMaterielDto dto = new PackMaterielDto();
        dto.setId(packMateriel.getId());
        dto.setQuantity(packMateriel.getQuantity());
        
        if (packMateriel.getMateriel() != null) {
            dto.setMaterielId(packMateriel.getMateriel().getId());
            dto.setMaterielName(packMateriel.getMateriel().getName());
            dto.setMaterielDescription(packMateriel.getMateriel().getDescription());
            dto.setMaterielPrice(packMateriel.getMateriel().getPrice());
            // Récupérer l'image primaire du matériel
            String primaryImageUrl = packMateriel.getMateriel().getPrimaryImage();
            dto.setMaterielImageUrl(primaryImageUrl);
            
            if (packMateriel.getMateriel().getCategorie() != null) {
                dto.setCategoryName(packMateriel.getMateriel().getCategorie().getName());
            }
        }

        return dto;
    }
}




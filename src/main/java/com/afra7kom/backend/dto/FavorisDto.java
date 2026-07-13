package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Favoris;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavorisDto {

    private Long id;
    private Long userId;
    private Favoris.FavorisType type;
    private PackDto pack;
    private MaterielDto materiel;
    private LocalDateTime createdAt;

    public static FavorisDto fromEntity(Favoris favoris) {
        FavorisDto dto = new FavorisDto();
        dto.setId(favoris.getId());
        dto.setUserId(favoris.getUser().getId());
        dto.setType(favoris.getType());
        dto.setCreatedAt(favoris.getCreatedAt());
        
        // Pack ou Matériel selon le type
        if (favoris.getType() == Favoris.FavorisType.PACK && favoris.getPack() != null) {
            dto.setPack(PackDto.fromEntitySimple(favoris.getPack()));
        } else if (favoris.getType() == Favoris.FavorisType.MATERIEL && favoris.getMateriel() != null) {
            dto.setMateriel(MaterielDto.fromEntity(favoris.getMateriel()));
        }
        
        return dto;
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class FavorisRequest {
    private Favoris.FavorisType type;
    private Long packId;
    private Long materielId;
}




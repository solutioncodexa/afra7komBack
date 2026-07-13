package com.afra7kom.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GalleryImageDto {
    private Long id;
    private String name;
    private String type; // PACK, BUFFET, PACK_BUFFET, MATERIEL, CADEAU
    private String imageUrl; // Pour la compatibilité (image principale)
    private String primaryImageUrl; // Image principale
    private List<String> images; // Liste de toutes les images
    private String description;
    private Double price;
    private String categoryName;
    private Boolean isAvailable;
}

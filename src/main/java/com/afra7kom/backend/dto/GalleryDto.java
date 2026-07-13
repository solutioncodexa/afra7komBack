package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Gallery;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GalleryDto {

    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String altText;
    private String category;
    private Integer sortOrder;
    private Boolean active;
    private Boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    public static GalleryDto fromEntity(Gallery gallery) {
        GalleryDto dto = new GalleryDto();
        dto.setId(gallery.getId());
        dto.setTitle(gallery.getTitle());
        dto.setDescription(gallery.getDescription());
        dto.setImageUrl(gallery.getImageUrl());
        dto.setAltText(gallery.getAltText());
        dto.setCategory(gallery.getCategory());
        dto.setSortOrder(gallery.getSortOrder());
        dto.setActive(gallery.getActive());
        dto.setFeatured(gallery.getFeatured());
        dto.setCreatedAt(gallery.getCreatedAt());
        dto.setUpdatedAt(gallery.getUpdatedAt());
        
        if (gallery.getCreatedBy() != null) {
            dto.setCreatedBy(gallery.getCreatedBy().getEmail());
        }
        
        return dto;
    }

    public static Gallery toEntity(GalleryDto dto) {
        Gallery gallery = new Gallery();
        gallery.setId(dto.getId());
        gallery.setTitle(dto.getTitle());
        gallery.setDescription(dto.getDescription());
        gallery.setImageUrl(dto.getImageUrl());
        gallery.setAltText(dto.getAltText());
        gallery.setCategory(dto.getCategory());
        gallery.setSortOrder(dto.getSortOrder());
        gallery.setActive(dto.getActive());
        gallery.setFeatured(dto.getFeatured());
        
        return gallery;
    }
}


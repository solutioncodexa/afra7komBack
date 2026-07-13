package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.PackMateriel;
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
public class PackDetailDto {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Informations de catégorie
    private CategorieDto categorie;
    
    // Matériels détaillés
    private List<PackMaterielDetailDto> materiels;
    private List<PackMaterielDetailDto> requiredMateriels;
    private List<PackMaterielDetailDto> optionalMateriels;
    
    // Galeries
    private List<GalleryDto> galleries;
    private List<GalleryDto> featuredGalleries;
    
    // Statistiques
    private Boolean isFavorite;
    private Boolean isAvailable;
    private BigDecimal totalValue;
    private Integer totalItems;
    private Integer totalMateriels;
    private Integer totalGalleries;
    private Integer requiredMaterielsCount;
    private Integer optionalMaterielsCount;
    
    // Informations de disponibilité
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private BigDecimal dailyRate;
    private BigDecimal weeklyRate;
    private BigDecimal monthlyRate;
    
    // Métadonnées
    private String tags;
    private String specifications;
    private String conditions;
    private String notes;

    public static PackDetailDto fromEntity(Pack pack) {
        PackDetailDto dto = new PackDetailDto();
        dto.setId(pack.getId());
        dto.setName(pack.getName());
        dto.setDescription(pack.getDescription());
        dto.setPrice(pack.getPrice());
        dto.setActive(pack.getActive());
        dto.setCreatedAt(pack.getCreatedAt());
        dto.setUpdatedAt(pack.getUpdatedAt());
        
        // Catégorie
        if (pack.getCategorie() != null) {
            dto.setCategorie(CategorieDto.fromEntity(pack.getCategorie()));
        }
        
        // Matériels détaillés
        if (pack.getPackMateriels() != null) {
            List<PackMaterielDetailDto> allMateriels = pack.getPackMateriels().stream()
                    .map(PackMaterielDetailDto::fromEntity)
                    .collect(Collectors.toList());
            
            dto.setMateriels(allMateriels);
            dto.setRequiredMateriels(allMateriels.stream()
                    .filter(m -> !m.getIsOptional())
                    .collect(Collectors.toList()));
            dto.setOptionalMateriels(allMateriels.stream()
                    .filter(PackMaterielDetailDto::getIsOptional)
                    .collect(Collectors.toList()));
            
            dto.setTotalMateriels(allMateriels.size());
            dto.setRequiredMaterielsCount((int) allMateriels.stream().filter(m -> !m.getIsOptional()).count());
            dto.setOptionalMaterielsCount((int) allMateriels.stream().filter(PackMaterielDetailDto::getIsOptional).count());
        }
        
        // Galeries (maintenant gérées par ProductImage)
        // Note: Les galeries sont maintenant gérées par ProductImage
        // TODO: Migrer vers ProductImage si nécessaire
        
        // Statistiques calculées
        dto.setIsAvailable(pack.isAvailable());
        dto.setTotalValue(pack.calculateTotalValue());
        dto.setTotalItems(pack.getTotalItems());
        
        // Calcul des taux de location
        dto.setDailyRate(pack.getPrice());
        dto.setWeeklyRate(pack.getPrice().multiply(BigDecimal.valueOf(7)).multiply(BigDecimal.valueOf(0.9))); // 10% de réduction
        dto.setMonthlyRate(pack.getPrice().multiply(BigDecimal.valueOf(30)).multiply(BigDecimal.valueOf(0.8))); // 20% de réduction
        
        return dto;
    }

    public static PackDetailDto fromEntityWithFavorite(Pack pack, boolean isFavorite) {
        PackDetailDto dto = fromEntity(pack);
        dto.setIsFavorite(isFavorite);
        return dto;
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class PackMaterielDetailDto {

    private Long id;
    private Long materielId;
    private String materielName;
    private String materielDescription;
    private BigDecimal materielPrice;
    private String materielImageUrl;
    private Integer quantity;
    private Boolean isOptional;
    private String notes;
    private String categoryName;
    
    // Informations de disponibilité
    private Integer availableQuantity;
    private Integer totalQuantity;
    private Boolean isAvailable;
    private BigDecimal totalValue;

    public static PackMaterielDetailDto fromEntity(PackMateriel packMateriel) {
        PackMaterielDetailDto dto = new PackMaterielDetailDto();
        dto.setId(packMateriel.getId());
        dto.setQuantity(packMateriel.getQuantity());
        dto.setIsOptional(packMateriel.getIsOptional());
        dto.setNotes(packMateriel.getNotes());
        
        if (packMateriel.getMateriel() != null) {
            dto.setMaterielId(packMateriel.getMateriel().getId());
            dto.setMaterielName(packMateriel.getMateriel().getName());
            dto.setMaterielDescription(packMateriel.getMateriel().getDescription());
            dto.setMaterielPrice(packMateriel.getMateriel().getPrice());
            // Récupérer l'image primaire du matériel
            String primaryImageUrl = packMateriel.getMateriel().getPrimaryImage();
            dto.setMaterielImageUrl(primaryImageUrl);
            dto.setAvailableQuantity(packMateriel.getMateriel().getAvailableQuantity());
            dto.setTotalQuantity(packMateriel.getMateriel().getTotalQuantity());
            dto.setIsAvailable(packMateriel.getMateriel().getAvailableQuantity() >= packMateriel.getQuantity());
            dto.setTotalValue(packMateriel.getMateriel().getPrice().multiply(BigDecimal.valueOf(packMateriel.getQuantity())));
            
            if (packMateriel.getMateriel().getCategorie() != null) {
                dto.setCategoryName(packMateriel.getMateriel().getCategorie().getName());
            }
        }

        return dto;
    }
}

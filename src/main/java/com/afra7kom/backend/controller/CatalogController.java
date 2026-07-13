package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.MaterielDto;
import com.afra7kom.backend.dto.PackDto;
import com.afra7kom.backend.repository.MaterielRepository;
import com.afra7kom.backend.repository.PackRepository;
import com.afra7kom.backend.service.MaterielService;
import com.afra7kom.backend.service.PackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalogue", description = "Catalogue unifié des items (packs et matériels)")
public class CatalogController {

    private final PackService packService;
    private final MaterielService materielService;
    private final PackRepository packRepository;
    private final MaterielRepository materielRepository;

    @GetMapping("/items")
    @Operation(summary = "Lister tous les items", description = "Récupérer la liste paginée de tous les items (packs et matériels)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des items récupérée avec succès")
    })
    public ResponseEntity<Page<PackDto>> getAllItems(
            @Parameter(description = "Filtre par catégorie") @RequestParam(required = false) Long categorieId,
            @Parameter(description = "Recherche par nom") @RequestParam(required = false) String search,
            @Parameter(description = "Prix minimum") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Prix maximum") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Type d'item (PACK, BUFFET, PACK_BUFFET, CADEAU, MATERIEL)") @RequestParam(required = false) String type,
            @Parameter(description = "Filtre actif/inactif") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Tri par") @RequestParam(required = false, defaultValue = "name") String sortBy,
            @Parameter(description = "Direction du tri") @RequestParam(required = false, defaultValue = "asc") String sortDirection,
            @PageableDefault(size = 30, sort = "name") Pageable pageable) {
        
        // Si un type spécifique est demandé, on filtre directement
        if (type != null && !type.trim().isEmpty()) {
            // Filtrer uniquement par le type demandé
            if (isPackType(type)) {
                // C'est un type de pack (PACK, BUFFET, PACK_BUFFET, CADEAU)
                String cleanSearch = (search != null && !search.trim().isEmpty()) ? search : null;
                
                // Récupérer tous les packs puis filtrer par type
            Pageable largePageable = org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE, pageable.getSort());
                Page<PackDto> allPacks = packService.searchPacksWithFilters(
                    active != null ? active : true, 
                    categorieId, 
                    cleanSearch, 
                    minPrice, 
                    maxPrice, 
                    largePageable
                );
                
                // Filtrer par type EXACT
                List<PackDto> filteredByType = allPacks.getContent().stream()
                    .filter(pack -> pack.getType() != null && pack.getType().name().equalsIgnoreCase(type))
                .toList();
                
                // Appliquer la pagination
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), filteredByType.size());
                
                if (start >= filteredByType.size()) {
                    return ResponseEntity.ok(new PageImpl<>(List.of(), pageable, filteredByType.size()));
                }
                
                List<PackDto> pageContent = filteredByType.subList(start, end);
                return ResponseEntity.ok(new PageImpl<>(pageContent, pageable, filteredByType.size()));
            } else if ("MATERIEL".equalsIgnoreCase(type)) {
                // ✅ CORRECTION: Charger les matériels depuis le service
                String cleanSearch = (search != null && !search.trim().isEmpty()) ? search : null;
                
                Page<MaterielDto> materiels = materielService.searchMaterielsWithFilters(
                    active, 
                    categorieId, 
                    minPrice, 
                    maxPrice, 
                    null, // isAvailable
                    pageable
                );
                
                // Convertir les matériels en PackDto pour compatibilité
                List<PackDto> convertedMateriels = materiels.getContent().stream()
                    .map(this::convertMaterielToPackDto)
                    .collect(Collectors.toList());
                
                return ResponseEntity.ok(new PageImpl<>(convertedMateriels, pageable, materiels.getTotalElements()));
            }
        }
        
        // Si pas de type spécifié, retourner TOUS les items (packs + matériels)
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search : null;
        
        // Charger tous les items sans pagination pour les combiner
        Pageable largePageable = org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE, pageable.getSort());
        
        // Récupérer les packs
        Page<PackDto> packs = packService.searchPacksWithFilters(
            active != null ? active : true, 
            categorieId, 
            cleanSearch, 
            minPrice, 
            maxPrice, 
            largePageable
        );
        
        // Récupérer les matériels
        Page<MaterielDto> materiels = materielService.searchMaterielsWithFilters(
            active, 
            categorieId, 
            minPrice, 
            maxPrice, 
            null, // isAvailable
            largePageable
        );
        
        // Combiner les deux listes
        List<PackDto> allItems = new ArrayList<>();
        allItems.addAll(packs.getContent());
        
        // Convertir et ajouter les matériels
        List<PackDto> convertedMateriels = materiels.getContent().stream()
            .map(this::convertMaterielToPackDto)
            .collect(Collectors.toList());
        allItems.addAll(convertedMateriels);
        
        // Trier selon sortBy et sortDirection
        if ("name".equalsIgnoreCase(sortBy)) {
            if ("asc".equalsIgnoreCase(sortDirection)) {
                allItems.sort(Comparator.comparing(PackDto::getName, String.CASE_INSENSITIVE_ORDER));
            } else {
                allItems.sort(Comparator.comparing(PackDto::getName, String.CASE_INSENSITIVE_ORDER).reversed());
            }
        } else if ("price".equalsIgnoreCase(sortBy)) {
            Comparator<PackDto> priceComparator = Comparator.comparing(
                item -> item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO
            );
            if ("desc".equalsIgnoreCase(sortDirection)) {
                priceComparator = priceComparator.reversed();
            }
            allItems.sort(priceComparator);
        }
        
        // Appliquer la pagination
        int totalElements = allItems.size();
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), totalElements);
        
        if (start >= totalElements) {
            return ResponseEntity.ok(new PageImpl<>(List.of(), pageable, totalElements));
        }
        
        List<PackDto> pageContent = allItems.subList(start, end);
        return ResponseEntity.ok(new PageImpl<>(pageContent, pageable, totalElements));
    }

    @GetMapping("/counts")
    @Operation(summary = "Compteurs par type", description = "Retourne le nombre d'items actifs par type")
    public ResponseEntity<Map<String, Long>> getTypeCounts() {
        Map<String, Long> counts = new HashMap<>();

        for (Object[] row : packRepository.countActiveByType()) {
            String type = row[0].toString();
            Long count = (Long) row[1];
            counts.put(type, count);
        }

        long materielCount = materielRepository.countByActiveTrue();
        counts.put("MATERIEL", materielCount);

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        counts.put("ALL", total);

        return ResponseEntity.ok(counts);
    }
    
    private boolean isPackType(String type) {
        if (type == null) return false;
        return "PACK".equalsIgnoreCase(type) || 
               "BUFFET".equalsIgnoreCase(type) || 
               "PACK_BUFFET".equalsIgnoreCase(type) || 
               "CADEAU".equalsIgnoreCase(type) ||
               "GATEAU".equalsIgnoreCase(type);
    }
    
    /**
     * Convertit un MaterielDto en PackDto pour l'uniformisation du catalogue
     * Les matériels sont considérés comme des "packs" de type MATERIEL
     */
    private PackDto convertMaterielToPackDto(MaterielDto materiel) {
        PackDto packDto = new PackDto();
        packDto.setId(materiel.getId());
        packDto.setName(materiel.getName());
        packDto.setDescription(materiel.getDescription());
        packDto.setPrice(materiel.getPrice());
        packDto.setActive(materiel.getActive());
        
        // ✅ IMPORTANT: Définir le type comme MATERIEL
        packDto.setType(com.afra7kom.backend.entity.PackType.MATERIEL);
        
        // Catégorie - Les deux DTO utilisent CategorieDto
        if (materiel.getCategorie() != null) {
            packDto.setCategorie(materiel.getCategorie());
        }
        
        // Images
        packDto.setImages(materiel.getImages());
        packDto.setPrimaryImage(materiel.getPrimaryImage());
        
        // Dates
        packDto.setCreatedAt(materiel.getCreatedAt());
        packDto.setUpdatedAt(materiel.getUpdatedAt());
        
        // Disponibilité
        packDto.setIsAvailable(materiel.getIsAvailable());
        
        return packDto;
    }
}

package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.PackDto;
import com.afra7kom.backend.dto.PackDetailDto;
import com.afra7kom.backend.dto.EquipmentStatsDto;
import com.afra7kom.backend.service.PackService;
import com.afra7kom.backend.service.EquipmentStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.afra7kom.backend.dto.GalleryDto;

@RestController
@RequestMapping("/api/packs")
@RequiredArgsConstructor
@Tag(name = "Packs", description = "Gestion des packs de location")
public class PackController {

    private final PackService packService;
    private final EquipmentStatsService equipmentStatsService;

    @GetMapping
    @Operation(summary = "Lister les packs", description = "Récupérer la liste paginée des packs avec filtres")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des packs récupérée avec succès")
    })
    public ResponseEntity<Page<PackDto>> getAllPacks(
            @Parameter(description = "Filtre par catégorie") @RequestParam(required = false) Long categorieId,
            @Parameter(description = "Recherche par nom") @RequestParam(required = false) String search,
            @Parameter(description = "Prix minimum") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Prix maximum") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Filtre actif/inactif") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Inclure les galeries") @RequestParam(required = false, defaultValue = "false") Boolean withGalleries,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PackDto> packs;

        if (withGalleries) {
            packs = packService.getPacksWithGalleries(pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            packs = packService.searchPacks(search.trim(), pageable);
        } else if (categorieId != null || minPrice != null || maxPrice != null || active != null) {
            packs = packService.searchPacksWithFilters(
                active != null ? active : true, categorieId, null, 
                minPrice, maxPrice, pageable);
        } else {
            packs = packService.getAllPacks(pageable);
        }

        return ResponseEntity.ok(packs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un pack", description = "Récupérer les détails d'un pack avec ses matériels")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pack trouvé"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<PackDto> getPackById(
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            // Récupérer l'ID de l'utilisateur connecté si disponible
            // userId = getCurrentUserId(authentication);
        }
        
        PackDto pack = userId != null ? 
            packService.getPackByIdForUser(id, userId) : 
            packService.getPackById(id);
            
        return ResponseEntity.ok(pack);
    }

    @GetMapping("/{id}/detail")
    @Operation(summary = "Détail complet d'un pack", description = "Récupérer les détails complets d'un pack avec matériels, galeries et statistiques")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pack détaillé trouvé"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<PackDetailDto> getPackDetailById(
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            // Récupérer l'ID de l'utilisateur connecté si disponible
            // userId = getCurrentUserId(authentication);
        }
        
        PackDetailDto pack = userId != null ? 
            packService.getPackDetailByIdForUser(id, userId) : 
            packService.getPackDetailById(id);
            
        return ResponseEntity.ok(pack);
    }

    @GetMapping("/category/{categorieId}")
    @Operation(summary = "Packs par catégorie", description = "Récupérer les packs d'une catégorie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Packs de la catégorie récupérés")
    })
    public ResponseEntity<Page<PackDto>> getPacksByCategorie(
            @PathVariable Long categorieId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        Page<PackDto> packs = packService.getPacksByCategorie(categorieId, pageable);
        return ResponseEntity.ok(packs);
    }

    @GetMapping("/most-favorited")
    @Operation(summary = "Packs les plus favoris", description = "Récupérer les packs les plus ajoutés en favoris")
    public ResponseEntity<Page<PackDto>> getMostFavoritedPacks(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        
        Page<PackDto> packs = packService.getMostFavoritedPacks(pageable);
        return ResponseEntity.ok(packs);
    }

    @GetMapping("/most-rented")
    @Operation(summary = "Packs les plus loués", description = "Récupérer les packs les plus loués")
    public ResponseEntity<Page<PackDto>> getMostRentedPacks(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        
        Page<PackDto> packs = packService.getMostRentedPacks(pageable);
        return ResponseEntity.ok(packs);
    }

    @GetMapping("/with-galleries")
    @Operation(summary = "Packs avec galeries", description = "Récupérer les packs avec leurs galeries")
    public ResponseEntity<Page<PackDto>> getPacksWithGalleries(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<PackDto> packs = packService.getPacksWithGalleries(pageable);
        return ResponseEntity.ok(packs);
    }

    @PostMapping
    @Operation(summary = "Créer un pack", description = "Créer un nouveau pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pack créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<PackDto> createPack(@RequestBody Map<String, Object> request) {
        com.afra7kom.backend.entity.PackType type = null;
        if (request.containsKey("type") && request.get("type") != null) {
            try {
                type = com.afra7kom.backend.entity.PackType.valueOf(request.get("type").toString());
            } catch (IllegalArgumentException e) {
                type = com.afra7kom.backend.entity.PackType.PACK;
            }
        }
        
        PackDto pack = packService.createPack(
            (String) request.get("name"),
            (String) request.get("description"),
            new BigDecimal(request.get("price").toString()),
            (String) request.get("imageUrl"),
            Long.valueOf(request.get("categorieId").toString()),
            (List<Map<String, Object>>) request.get("materiels"),
            (List<Long>) request.get("galleryIds"),
            type
        );
        
        return ResponseEntity.status(201).body(pack);
    }

    @PostMapping(value = "/with-images", consumes = "multipart/form-data")
    @Operation(summary = "Créer un pack avec images", description = "Créer un nouveau pack avec upload d'images en une seule requête")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pack créé avec succès et images uploadées"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<PackDto> createPackWithImages(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("categorieId") Long categorieId,
            @RequestParam(value = "materiels", required = false) String materielsJson,
            @RequestParam(value = "galleryIds", required = false) String galleryIdsJson,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "imageDescriptions", required = false) List<String> imageDescriptions,
            @RequestParam(value = "primaryImageIndex", required = false, defaultValue = "0") Integer primaryImageIndex,
            @RequestParam(value = "type", required = false) String typeString) {
        
        com.afra7kom.backend.entity.PackType type = null;
        if (typeString != null && !typeString.trim().isEmpty()) {
            try {
                type = com.afra7kom.backend.entity.PackType.valueOf(typeString);
            } catch (IllegalArgumentException e) {
                type = com.afra7kom.backend.entity.PackType.PACK;
            }
        }
        
        PackDto pack = packService.createPackWithImages(
            name, description, price, categorieId, 
            materielsJson, galleryIdsJson, 
            images, imageDescriptions, primaryImageIndex, type
        );
        
        return ResponseEntity.status(201).body(pack);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un pack", description = "Modifier un pack existant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pack modifié avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<PackDto> updatePack(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        
        com.afra7kom.backend.entity.PackType type = null;
        if (request.containsKey("type") && request.get("type") != null) {
            try {
                type = com.afra7kom.backend.entity.PackType.valueOf(request.get("type").toString());
            } catch (IllegalArgumentException e) {
                // Garder le type actuel si la conversion échoue
                type = null;
            }
        }
        
        PackDto pack = packService.updatePack(
            id,
            (String) request.get("name"),
            (String) request.get("description"),
            request.containsKey("price") ? new BigDecimal(request.get("price").toString()) : null,
            (String) request.get("imageUrl"),
            request.containsKey("categorieId") ? Long.valueOf(request.get("categorieId").toString()) : null,
            request.containsKey("active") ? Boolean.valueOf(request.get("active").toString()) : null,
            (List<Map<String, Object>>) request.get("materiels"),
            (List<Long>) request.get("galleryIds"),
            type,
            (List<String>) request.get("images"),
            (String) request.get("primaryImage")
        );
        
        return ResponseEntity.ok(pack);
    }

    @PutMapping(value = "/{id}/with-images", consumes = "multipart/form-data")
    @Operation(summary = "Modifier un pack avec images", description = "Modifier un pack existant avec upload d'images")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pack modifié avec succès et images uploadées"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<PackDto> updatePackWithImages(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam(value = "categorieId", required = false) Long categorieId,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "materiels", required = false) String materielsJson,
            @RequestParam(value = "galleryIds", required = false) String galleryIdsJson,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "existingImages", required = false) String existingImagesJson,
            @RequestParam(value = "primaryImageIndex", required = false) Integer primaryImageIndex,
            @RequestParam(value = "type", required = false) String typeString) {
        
        com.afra7kom.backend.entity.PackType type = null;
        if (typeString != null && !typeString.trim().isEmpty()) {
            try {
                type = com.afra7kom.backend.entity.PackType.valueOf(typeString);
            } catch (IllegalArgumentException e) {
                type = null; // Garder le type actuel
            }
        }
        
        PackDto pack = packService.updatePackWithImages(
            id, name, description, price, categorieId, active,
            materielsJson, galleryIdsJson, 
            images, existingImagesJson, primaryImageIndex, type
        );
        
        return ResponseEntity.ok(pack);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un pack", description = "Supprimer un pack (avec option force pour supprimer même avec réservations)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Pack supprimé avec succès"),
        @ApiResponse(responseCode = "400", description = "Pack a des réservations (utilisez force=true pour forcer)"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deletePack(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") Boolean force) {
        packService.deletePack(id, force);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Vérifier la disponibilité", description = "Vérifier la disponibilité d'un pack pour une période")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Disponibilité vérifiée"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @PathVariable Long id,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        boolean isAvailable = packService.checkAvailability(
            id, 
            java.time.LocalDate.parse(startDate), 
            java.time.LocalDate.parse(endDate)
        );
        
        return ResponseEntity.ok(Map.of(
            "packId", id,
            "startDate", startDate,
            "endDate", endDate,
            "isAvailable", isAvailable
        ));
    }

    @PostMapping("/{id}/images/upload")
    @Operation(summary = "Upload d'images pour un pack", description = "Uploader des images pour un pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Images uploadées avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<Map<String, Object>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files) {
        
        List<String> uploadedUrls = packService.uploadImages(id, files);
        
        return ResponseEntity.ok(Map.of(
            "packId", id,
            "uploadedImages", uploadedUrls,
            "message", uploadedUrls.size() + " image(s) uploadée(s) avec succès"
        ));
    }

    @PutMapping("/{id}/images/{imageId}/set-main")
    @Operation(summary = "Définir l'image principale", description = "Définir une image comme principale pour le pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image principale définie"),
        @ApiResponse(responseCode = "404", description = "Pack ou image non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<Map<String, Object>> setMainImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        
        String mainImageUrl = packService.setMainImage(id, imageId);
        
        return ResponseEntity.ok(Map.of(
            "packId", id,
            "imageId", imageId,
            "mainImageUrl", mainImageUrl,
            "message", "Image principale définie avec succès"
        ));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Supprimer une image", description = "Supprimer une image d'un pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image supprimée"),
        @ApiResponse(responseCode = "404", description = "Pack ou image non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<Map<String, Object>> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        
        packService.deleteImage(id, imageId);
        
        return ResponseEntity.ok(Map.of(
            "packId", id,
            "imageId", imageId,
            "message", "Image supprimée avec succès"
        ));
    }

    @GetMapping("/{id}/images")
    @Operation(summary = "Lister les images d'un pack", description = "Récupérer toutes les images d'un pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Images récupérées"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<List<GalleryDto>> getPackImages(@PathVariable Long id) {
        List<GalleryDto> images = packService.getPackImages(id);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Statistiques d'un pack", description = "Récupérer les statistiques de réservations et revenus d'un pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistiques récupérées"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<EquipmentStatsDto> getPackStats(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "12") Integer derniersMois) {
        EquipmentStatsDto stats = equipmentStatsService.getEquipmentStats(id, true, derniersMois);
        return ResponseEntity.ok(stats);
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Activer/Désactiver un pack", description = "Changer rapidement le statut actif d'un pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statut modifié avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<PackDto> togglePackActive(@PathVariable Long id) {
        PackDto pack = packService.toggleActive(id);
        return ResponseEntity.ok(pack);
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Dupliquer un pack", description = "Créer une copie d'un pack existant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pack dupliqué avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<PackDto> duplicatePack(@PathVariable Long id) {
        PackDto duplicatedPack = packService.duplicatePack(id);
        return ResponseEntity.status(201).body(duplicatedPack);
    }

    @PostMapping("/bulk/delete")
    @Operation(summary = "Supprimer plusieurs packs", description = "Supprimer plusieurs packs en une seule opération")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> bulkDeletePacks(@RequestBody List<Object> idsObj) {
        List<Long> ids = idsObj.stream()
                .map(id -> id instanceof Integer ? ((Integer) id).longValue() : (Long) id)
                .toList();
        int deleted = packService.bulkDelete(ids);
        return ResponseEntity.ok(Map.of(
            "deleted", deleted,
            "message", deleted + " pack(s) supprimé(s) avec succès"
        ));
    }

    @PostMapping("/bulk/toggle-active")
    @Operation(summary = "Activer/Désactiver plusieurs packs", description = "Changer le statut actif de plusieurs packs")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> bulkToggleActive(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Object> idsObj = (List<Object>) request.get("ids");
        List<Long> ids = idsObj.stream()
                .map(id -> id instanceof Integer ? ((Integer) id).longValue() : (Long) id)
                .toList();
        Boolean active = (Boolean) request.get("active");
        int updated = packService.bulkToggleActive(ids, active);
        return ResponseEntity.ok(Map.of(
            "updated", updated,
            "message", updated + " pack(s) " + (active ? "activé(s)" : "désactivé(s)") + " avec succès"
        ));
    }
}




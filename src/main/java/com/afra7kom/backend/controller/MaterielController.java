package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.MaterielDto;
import com.afra7kom.backend.dto.MaterielCreateDto;
import com.afra7kom.backend.dto.EquipmentStatsDto;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.service.MaterielService;
import com.afra7kom.backend.service.EquipmentStatsService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/materiels")
@RequiredArgsConstructor
@Tag(name = "Matériels", description = "Gestion des matériels de location")
public class MaterielController {

    private final MaterielService materielService;
    private final EquipmentStatsService equipmentStatsService;

    @GetMapping
    @Operation(summary = "Lister les matériels", description = "Récupérer la liste paginée des matériels avec filtres")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des matériels récupérée avec succès")
    })
    public ResponseEntity<Page<MaterielDto>> getAllMateriels(
            @Parameter(description = "Filtre par catégorie") @RequestParam(required = false) Long categorieId,
            @Parameter(description = "Recherche par nom") @RequestParam(required = false) String search,
            @Parameter(description = "Prix minimum") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Prix maximum") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Filtre actif/inactif") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filtre par disponibilité") @RequestParam(required = false) Boolean isAvailable,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<MaterielDto> materiels;

        if (search != null && !search.trim().isEmpty()) {
            materiels = materielService.searchMateriels(search.trim(), pageable);
        } else if (categorieId != null || minPrice != null || maxPrice != null || active != null || isAvailable != null) {
            materiels = materielService.searchMaterielsWithFilters(
                active != null ? active : true, 
                categorieId, 
                minPrice, 
                maxPrice, 
                isAvailable,
                pageable);
        } else {
            materiels = materielService.getAllMateriels(pageable);
        }

        return ResponseEntity.ok(materiels);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un matériel", description = "Récupérer les détails d'un matériel")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Matériel trouvé"),
        @ApiResponse(responseCode = "404", description = "Matériel non trouvé")
    })
    public ResponseEntity<MaterielDto> getMaterielById(@PathVariable Long id) {
        MaterielDto materiel = materielService.getMaterielById(id);
        return ResponseEntity.ok(materiel);
    }

    @GetMapping("/category/{categorieId}")
    @Operation(summary = "Matériels par catégorie", description = "Récupérer les matériels d'une catégorie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Matériels de la catégorie récupérés")
    })
    public ResponseEntity<Page<MaterielDto>> getMaterielsByCategorie(
            @PathVariable Long categorieId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        Page<MaterielDto> materiels = materielService.getMaterielsByCategorie(categorieId, pageable);
        return ResponseEntity.ok(materiels);
    }

    @GetMapping("/disponibles")
    @Operation(summary = "Matériels disponibles", description = "Récupérer les matériels disponibles")
    public ResponseEntity<Page<MaterielDto>> getMaterielsDisponibles(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        Page<MaterielDto> materiels = materielService.getMaterielsDisponibles(pageable);
        return ResponseEntity.ok(materiels);
    }

    @GetMapping("/most-favorited")
    @Operation(summary = "Matériels les plus favoris", description = "Récupérer les matériels les plus ajoutés en favoris")
    public ResponseEntity<Page<MaterielDto>> getMostFavoritedMateriels(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        
        Page<MaterielDto> materiels = materielService.getMostFavoritedMateriels(pageable);
        return ResponseEntity.ok(materiels);
    }

    @GetMapping("/most-rented")
    @Operation(summary = "Matériels les plus loués", description = "Récupérer les matériels les plus loués")
    public ResponseEntity<Page<MaterielDto>> getMostRentedMateriels(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        
        Page<MaterielDto> materiels = materielService.getMostRentedMateriels(pageable);
        return ResponseEntity.ok(materiels);
    }

    @PostMapping
    @Operation(summary = "Créer un matériel", description = "Créer un nouveau matériel")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Matériel créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<MaterielDto> createMateriel(@Valid @RequestBody MaterielCreateDto materielCreateDto) {
        try {
            // Log pour déboguer
            System.out.println("=== DEBUG: Données reçues ===");
            System.out.println("MaterielCreateDto: " + materielCreateDto);
            System.out.println("name: " + materielCreateDto.getName());
            System.out.println("price: " + materielCreateDto.getPrice());
            System.out.println("categorieId: " + materielCreateDto.getCategorieId());
            System.out.println("=============================");
            
            MaterielDto materiel = materielService.createMateriel(
                materielCreateDto.getName(),
                materielCreateDto.getDescription(),
                materielCreateDto.getPrice(),
                materielCreateDto.getImageUrl(),
                materielCreateDto.getCategorieId(),
                materielCreateDto.getMarque(),
                materielCreateDto.getModele()
            );
            
            return ResponseEntity.status(201).body(materiel);
        } catch (Exception e) {
            throw new IllegalArgumentException("Erreur lors de la création du matériel: " + e.getMessage());
        }
    }

    @PostMapping(value = "/with-images", consumes = "multipart/form-data")
    @Operation(summary = "Créer un matériel avec images", description = "Créer un nouveau matériel avec upload d'images en une seule requête")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Matériel créé avec succès et images uploadées"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<MaterielDto> createMaterielWithImages(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("categorieId") Long categorieId,
            @RequestParam(value = "marque", required = false) String marque,
            @RequestParam(value = "modele", required = false) String modele,
            @RequestParam(value = "quantity", required = false, defaultValue = "0") Integer quantity,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "imageDescriptions", required = false) List<String> imageDescriptions,
            @RequestParam(value = "primaryImageIndex", required = false, defaultValue = "0") Integer primaryImageIndex) {
        
        MaterielDto materiel = materielService.createMaterielWithImages(
            name, description, price, categorieId, marque, modele, quantity,
            images, imageDescriptions, primaryImageIndex
        );
        
        return ResponseEntity.status(201).body(materiel);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un matériel", description = "Modifier un matériel existant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Matériel modifié avec succès"),
        @ApiResponse(responseCode = "404", description = "Matériel non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<MaterielDto> updateMateriel(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        
        // Les champs sont tous optionnels pour permettre des mises à jour partielles
        MaterielDto materiel = materielService.updateMateriel(
            id,
            request.containsKey("name") ? (String) request.get("name") : null,
            request.containsKey("description") ? (String) request.get("description") : null,
            request.containsKey("price") && request.get("price") != null ? new BigDecimal(request.get("price").toString()) : null,
            request.containsKey("imageUrl") ? (String) request.get("imageUrl") : null,
            request.containsKey("categorieId") && request.get("categorieId") != null ? Long.valueOf(request.get("categorieId").toString()) : null,
            request.containsKey("marque") ? (String) request.get("marque") : null,
            request.containsKey("modele") ? (String) request.get("modele") : null,
            request.containsKey("active") && request.get("active") != null ? Boolean.valueOf(request.get("active").toString()) : null,
            request.containsKey("quantity") && request.get("quantity") != null ? Integer.valueOf(request.get("quantity").toString()) : null
        );
        
        return ResponseEntity.ok(materiel);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un matériel", description = "Supprimer un matériel (avec option force pour supprimer même avec réservations)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Matériel supprimé avec succès"),
        @ApiResponse(responseCode = "400", description = "Matériel a des réservations (utilisez force=true pour forcer)"),
        @ApiResponse(responseCode = "404", description = "Matériel non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteMateriel(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") Boolean force) {
        materielService.deleteMateriel(id, force);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Statistiques d'un matériel", description = "Récupérer les statistiques de réservations et revenus d'un matériel")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistiques récupérées"),
        @ApiResponse(responseCode = "404", description = "Matériel non trouvé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<EquipmentStatsDto> getMaterielStats(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "12") Integer derniersMois) {
        EquipmentStatsDto stats = equipmentStatsService.getEquipmentStats(id, false, derniersMois);
        return ResponseEntity.ok(stats);
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Activer/Désactiver un matériel", description = "Changer rapidement le statut actif d'un matériel")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statut modifié avec succès"),
        @ApiResponse(responseCode = "404", description = "Matériel non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<MaterielDto> toggleMaterielActive(@PathVariable Long id) {
        MaterielDto materiel = materielService.toggleActive(id);
        return ResponseEntity.ok(materiel);
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Mettre à jour le stock total d'un matériel", description = "Mise à jour directe du stock total et disponible d'un matériel")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock mis à jour avec succès"),
        @ApiResponse(responseCode = "400", description = "Quantité invalide"),
        @ApiResponse(responseCode = "404", description = "Matériel non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<MaterielDto> updateStock(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        
        if (!request.containsKey("quantity")) {
            throw new BadRequestException("Le champ 'quantity' est requis");
        }
        
        Integer quantity = Integer.valueOf(request.get("quantity").toString());
        if (quantity < 0) {
            throw new BadRequestException("La quantité ne peut pas être négative");
        }
        
        MaterielDto materiel = materielService.updateStock(id, quantity);
        return ResponseEntity.ok(materiel);
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Dupliquer un matériel", description = "Créer une copie d'un matériel existant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Matériel dupliqué avec succès"),
        @ApiResponse(responseCode = "404", description = "Matériel non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<MaterielDto> duplicateMateriel(@PathVariable Long id) {
        MaterielDto duplicatedMateriel = materielService.duplicateMateriel(id);
        return ResponseEntity.status(201).body(duplicatedMateriel);
    }

    @PostMapping("/bulk/delete")
    @Operation(summary = "Supprimer plusieurs matériels", description = "Supprimer plusieurs matériels en une seule opération")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> bulkDeleteMateriels(@RequestBody List<Object> idsObj) {
        List<Long> ids = idsObj.stream()
                .map(id -> id instanceof Integer ? ((Integer) id).longValue() : (Long) id)
                .toList();
        int deleted = materielService.bulkDelete(ids);
        return ResponseEntity.ok(Map.of(
            "deleted", deleted,
            "message", deleted + " matériel(s) supprimé(s) avec succès"
        ));
    }

    @PostMapping("/bulk/toggle-active")
    @Operation(summary = "Activer/Désactiver plusieurs matériels", description = "Changer le statut actif de plusieurs matériels")
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
        int updated = materielService.bulkToggleActive(ids, active);
        return ResponseEntity.ok(Map.of(
            "updated", updated,
            "message", updated + " matériel(s) " + (active ? "activé(s)" : "désactivé(s)") + " avec succès"
        ));
    }
}

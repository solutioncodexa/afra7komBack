package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.CategorieDto;
import com.afra7kom.backend.service.CategorieService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Catégories", description = "Gestion des catégories de matériels")
public class CategorieController {

    private final CategorieService categorieService;

    @GetMapping
    @Operation(summary = "Lister les catégories", description = "Récupérer la liste des catégories")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des catégories récupérée avec succès")
    })
    public ResponseEntity<List<CategorieDto>> getAllCategories() {
        List<CategorieDto> categories = categorieService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/paginated")
    @Operation(summary = "Lister les catégories paginées", description = "Récupérer la liste paginée des catégories")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des catégories récupérée avec succès")
    })
    public ResponseEntity<Page<CategorieDto>> getAllCategoriesPaginated(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<CategorieDto> categories = categorieService.getAllCategoriesPaginated(pageable);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une catégorie", description = "Récupérer les détails d'une catégorie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Catégorie trouvée"),
        @ApiResponse(responseCode = "404", description = "Catégorie non trouvée")
    })
    public ResponseEntity<CategorieDto> getCategorieById(@PathVariable Long id) {
        CategorieDto categorie = categorieService.getCategorieById(id);
        return ResponseEntity.ok(categorie);
    }

    @GetMapping("/active")
    @Operation(summary = "Catégories actives", description = "Récupérer les catégories actives")
    public ResponseEntity<List<CategorieDto>> getActiveCategories() {
        List<CategorieDto> categories = categorieService.getActiveCategories();
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    @Operation(summary = "Créer une catégorie", description = "Créer une nouvelle catégorie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Catégorie créée avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CategorieDto> createCategorie(@RequestBody Map<String, Object> request) {
        CategorieDto categorie = categorieService.createCategorie(
            (String) request.get("name"),
            (String) request.get("description"),
            (String) request.get("icon"),
            request.containsKey("sortOrder") ? Integer.valueOf(request.get("sortOrder").toString()) : null
        );
        
        return ResponseEntity.status(201).body(categorie);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une catégorie", description = "Modifier une catégorie existante")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Catégorie modifiée avec succès"),
        @ApiResponse(responseCode = "404", description = "Catégorie non trouvée"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CategorieDto> updateCategorie(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        
        CategorieDto categorie = categorieService.updateCategorie(
            id,
            (String) request.get("name"),
            (String) request.get("description"),
            (String) request.get("icon"),
            request.containsKey("active") ? Boolean.valueOf(request.get("active").toString()) : null,
            request.containsKey("sortOrder") ? Integer.valueOf(request.get("sortOrder").toString()) : null
        );
        
        return ResponseEntity.ok(categorie);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une catégorie", description = "Supprimer une catégorie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Catégorie supprimée avec succès"),
        @ApiResponse(responseCode = "404", description = "Catégorie non trouvée"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteCategorie(@PathVariable Long id) {
        categorieService.deleteCategorie(id);
        return ResponseEntity.noContent().build();
    }
}

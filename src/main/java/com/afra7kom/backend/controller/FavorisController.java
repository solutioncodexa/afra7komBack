package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.FavorisDto;
import com.afra7kom.backend.entity.Favoris;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.service.FavorisService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favoris")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Favoris", description = "Gestion des favoris utilisateur")
public class FavorisController {

    private final FavorisService favorisService;

    @GetMapping
    @Operation(summary = "Mes favoris", description = "Récupérer tous les favoris de l'utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Favoris récupérés avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<Page<FavorisDto>> getMyFavoris(
            @RequestParam(required = false) Favoris.FavorisType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        
        Long userId = getCurrentUserId(authentication);
        
        Page<FavorisDto> favoris = type != null ?
            favorisService.getUserFavorisByType(userId, type, pageable) :
            favorisService.getUserFavoris(userId, pageable);
            
        return ResponseEntity.ok(favoris);
    }

    @GetMapping("/packs")
    @Operation(summary = "Mes packs favoris", description = "Récupérer les packs favoris de l'utilisateur")
    public ResponseEntity<List<FavorisDto>> getMyPackFavoris(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<FavorisDto> favoris = favorisService.getUserPackFavoris(userId);
        return ResponseEntity.ok(favoris);
    }

    @GetMapping("/materiels")
    @Operation(summary = "Mes matériels favoris", description = "Récupérer les matériels favoris de l'utilisateur")
    public ResponseEntity<List<FavorisDto>> getMyMaterielFavoris(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<FavorisDto> favoris = favorisService.getUserMaterielFavoris(userId);
        return ResponseEntity.ok(favoris);
    }

    @PostMapping("/packs/{packId}")
    @Operation(summary = "Ajouter pack aux favoris", description = "Ajouter un pack aux favoris")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pack ajouté aux favoris"),
        @ApiResponse(responseCode = "400", description = "Pack déjà en favoris"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<FavorisDto> addPackToFavoris(
            @PathVariable Long packId,
            Authentication authentication) {
        
        Long userId = getCurrentUserId(authentication);
        FavorisDto favoris = favorisService.addPackToFavoris(userId, packId);
        return ResponseEntity.status(201).body(favoris);
    }

    @PostMapping("/materiels/{materielId}")
    @Operation(summary = "Ajouter matériel aux favoris", description = "Ajouter un matériel aux favoris")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Matériel ajouté aux favoris"),
        @ApiResponse(responseCode = "400", description = "Matériel déjà en favoris"),
        @ApiResponse(responseCode = "404", description = "Matériel non trouvé")
    })
    public ResponseEntity<FavorisDto> addMaterielToFavoris(
            @PathVariable Long materielId,
            Authentication authentication) {
        
        Long userId = getCurrentUserId(authentication);
        FavorisDto favoris = favorisService.addMaterielToFavoris(userId, materielId);
        return ResponseEntity.status(201).body(favoris);
    }

    @DeleteMapping("/packs/{packId}")
    @Operation(summary = "Retirer pack des favoris", description = "Retirer un pack des favoris")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Pack retiré des favoris"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé dans les favoris")
    })
    public ResponseEntity<Void> removePackFromFavoris(
            @PathVariable Long packId,
            Authentication authentication) {
        
        Long userId = getCurrentUserId(authentication);
        favorisService.removePackFromFavoris(userId, packId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/materiels/{materielId}")
    @Operation(summary = "Retirer matériel des favoris", description = "Retirer un matériel des favoris")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Matériel retiré des favoris"),
        @ApiResponse(responseCode = "404", description = "Matériel non trouvé dans les favoris")
    })
    public ResponseEntity<Void> removeMaterielFromFavoris(
            @PathVariable Long materielId,
            Authentication authentication) {
        
        Long userId = getCurrentUserId(authentication);
        favorisService.removeMaterielFromFavoris(userId, materielId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/packs/{packId}/check")
    @Operation(summary = "Vérifier si pack en favoris", description = "Vérifier si un pack est dans les favoris")
    public ResponseEntity<Map<String, Boolean>> checkPackFavorite(
            @PathVariable Long packId,
            Authentication authentication) {
        
        Long userId = getCurrentUserId(authentication);
        boolean isFavorite = favorisService.isPackFavorite(userId, packId);
        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }

    @GetMapping("/materiels/{materielId}/check")
    @Operation(summary = "Vérifier si matériel en favoris", description = "Vérifier si un matériel est dans les favoris")
    public ResponseEntity<Map<String, Boolean>> checkMaterielFavorite(
            @PathVariable Long materielId,
            Authentication authentication) {
        
        Long userId = getCurrentUserId(authentication);
        boolean isFavorite = favorisService.isMaterielFavorite(userId, materielId);
        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }

    private Long getCurrentUserId(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }
}




package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.AvailabilityDto;
import com.afra7kom.backend.service.MaterielService;
import com.afra7kom.backend.service.PackService;
import com.afra7kom.backend.service.GalleryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Endpoints publics accessibles sans authentification")
public class PublicController {

    private final MaterielService materielService;
    private final PackService packService;
    private final GalleryService galleryService;

    @GetMapping("/materiels/{id}/availability")
    @Operation(summary = "Vérifier disponibilité matériel", description = "Vérifier la disponibilité d'un matériel pour une période donnée")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Disponibilité vérifiée"),
        @ApiResponse(responseCode = "404", description = "Matériel non trouvé")
    })
    public ResponseEntity<Map<String, Object>> checkMaterielAvailability(
            @PathVariable Long id,
            @Parameter(description = "Date de début (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Date de fin (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        boolean available = materielService.checkAvailability(id, startDate, endDate);
        String message = available ? 
            "Matériel disponible pour cette période" : 
            "Matériel non disponible pour cette période";
        
        return ResponseEntity.ok(Map.of(
            "available", available,
            "message", message,
            "materielId", id,
            "startDate", startDate,
            "endDate", endDate
        ));
    }

    @GetMapping("/packs/{id}/availability")
    @Operation(summary = "Vérifier disponibilité pack", description = "Vérifier la disponibilité d'un pack pour une période donnée")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Disponibilité vérifiée"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<Map<String, Object>> checkPackAvailability(
            @PathVariable Long id,
            @Parameter(description = "Date de début (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Date de fin (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        boolean available = packService.checkAvailability(id, startDate, endDate);
        String message = available ? 
            "Pack disponible pour cette période" : 
            "Pack non disponible pour cette période";
        
        return ResponseEntity.ok(Map.of(
            "available", available,
            "message", message,
            "packId", id,
            "startDate", startDate,
            "endDate", endDate
        ));
    }

    @GetMapping("/availability/check")
    @Operation(summary = "Vérifier disponibilité générale", description = "Vérifier la disponibilité générale des équipements")
    public ResponseEntity<Map<String, Object>> checkGeneralAvailability(
            @Parameter(description = "Date de début (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Date de fin (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // Logique pour vérifier la disponibilité générale
        Map<String, Object> availabilityInfo = Map.of(
            "startDate", startDate,
            "endDate", endDate,
            "message", "Service de vérification de disponibilité disponible",
            "available", true
        );
        
        return ResponseEntity.ok(availabilityInfo);
    }

    @GetMapping("/gallery/images")
    @Operation(summary = "Récupérer toutes les URLs d'images des items actifs", description = "Récupère juste les URLs d'images extraites de la colonne images des packs et matériels actifs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "URLs d'images récupérées avec succès")
    })
    public ResponseEntity<Page<String>> getAllActiveItemsImages(
            @Parameter(description = "Filtre par type d'item") @RequestParam(required = false) String type,
            @PageableDefault(size = 3, sort = "id", direction = org.springframework.data.domain.Sort.Direction.ASC) Pageable pageable) {
        
        Page<String> imageUrls;
        
        if (type != null && !type.trim().isEmpty()) {
            imageUrls = galleryService.getImageUrlsByType(type.toUpperCase(), pageable);
        } else {
            imageUrls = galleryService.getAllActiveItemsImages(pageable);
        }
        
        return ResponseEntity.ok(imageUrls);
    }

    @GetMapping("/gallery/images/{type}")
    @Operation(summary = "Récupérer les URLs d'images par type", description = "Récupère juste les URLs d'images des items actifs filtrés par type (PACK, BUFFET, PACK_BUFFET, MATERIEL, CADEAU)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "URLs d'images récupérées avec succès"),
        @ApiResponse(responseCode = "400", description = "Type d'item invalide")
    })
    public ResponseEntity<Page<String>> getImagesByType(
            @PathVariable String type,
            @PageableDefault(size = 3, sort = "id", direction = org.springframework.data.domain.Sort.Direction.ASC) Pageable pageable) {
        
        // Validation du type
        String[] validTypes = {"PACK", "BUFFET", "PACK_BUFFET", "MATERIEL", "CADEAU", "GATEAU"};
        boolean isValidType = java.util.Arrays.asList(validTypes).contains(type.toUpperCase());
        
        if (!isValidType) {
            return ResponseEntity.badRequest().build();
        }
        
        Page<String> imageUrls = galleryService.getImageUrlsByType(type.toUpperCase(), pageable);
        return ResponseEntity.ok(imageUrls);
    }
}


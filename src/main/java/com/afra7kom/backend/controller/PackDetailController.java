package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.PackCreateDto;
import com.afra7kom.backend.dto.PackDetailDto;
import com.afra7kom.backend.service.PackDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/pack-details")
@CrossOrigin(origins = "*")
public class PackDetailController {

    @Autowired
    private PackDetailService packDetailService;

    @GetMapping
    public ResponseEntity<List<PackDetailDto>> getAllPacks() {
        List<PackDetailDto> packs = packDetailService.getAllPacks();
        return ResponseEntity.ok(packs);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<PackDetailDto>> getAllPacksPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PackDetailDto> packs = packDetailService.getAllPacksPaginated(pageable);
        return ResponseEntity.ok(packs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackDetailDto> getPackById(@PathVariable Long id) {
        PackDetailDto pack = packDetailService.getPackById(id);
        return ResponseEntity.ok(pack);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<PackDetailDto> createPack(@RequestBody PackCreateDto packCreateDto) {
        PackDetailDto createdPack = packDetailService.createPack(packCreateDto);
        return ResponseEntity.ok(createdPack);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<PackDetailDto> updatePack(@PathVariable Long id, @RequestBody PackCreateDto packCreateDto) {
        PackDetailDto updatedPack = packDetailService.updatePack(id, packCreateDto);
        return ResponseEntity.ok(updatedPack);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un pack", description = "Supprimer un pack et toutes ses associations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Pack supprimé avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<Void> deletePack(@PathVariable Long id) {
        packDetailService.deletePack(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clean-duplicates")
    @Operation(summary = "Nettoyer les doublons", description = "Supprimer les matériels en double dans un pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Doublons nettoyés avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<PackDetailDto> cleanDuplicateMaterials(@PathVariable Long id) {
        packDetailService.cleanDuplicateMaterials(id);
        PackDetailDto pack = packDetailService.getPackById(id);
        return ResponseEntity.ok(pack);
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Dupliquer un pack", description = "Créer une copie d'un pack existant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pack dupliqué avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<PackDetailDto> duplicatePack(@PathVariable Long id) {
        PackDetailDto duplicatedPack = packDetailService.duplicatePack(id);
        return ResponseEntity.ok(duplicatedPack);
    }

    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Activer/Désactiver un pack", description = "Basculer le statut actif/inactif d'un pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statut modifié avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<PackDetailDto> togglePackStatus(@PathVariable Long id) {
        PackDetailDto updatedPack = packDetailService.togglePackStatus(id);
        return ResponseEntity.ok(updatedPack);
    }
}


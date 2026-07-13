package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.AvailabilityInfoDto;
import com.afra7kom.backend.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Disponibilité", description = "API pour vérifier la disponibilité des produits")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/pack/{packId}")
    @Operation(summary = "Vérifier la disponibilité d'un pack", 
               description = "Vérifie la disponibilité d'un pack pour une période donnée")
    public ResponseEntity<AvailabilityInfoDto> checkPackAvailability(
            @Parameter(description = "ID du pack") @PathVariable Long packId,
            @Parameter(description = "Date de début (optionnel)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Date de fin (optionnelle)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Vérification de disponibilité pour le pack {} du {} au {}", packId, startDate, endDate);
        
        try {
            AvailabilityInfoDto availability = availabilityService.checkPackAvailability(packId, startDate, endDate);
            return ResponseEntity.ok(availability);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de disponibilité du pack {}", packId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/materiel/{materielId}")
    @Operation(summary = "Vérifier la disponibilité d'un matériel", 
               description = "Vérifie la disponibilité d'un matériel pour une période donnée")
    public ResponseEntity<AvailabilityInfoDto> checkMaterielAvailability(
            @Parameter(description = "ID du matériel") @PathVariable Long materielId,
            @Parameter(description = "Date de début (optionnel)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Date de fin (optionnelle)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Vérification de disponibilité pour le matériel {} du {} au {}", materielId, startDate, endDate);
        
        try {
            AvailabilityInfoDto availability = availabilityService.checkMaterielAvailability(materielId, startDate, endDate);
            return ResponseEntity.ok(availability);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de disponibilité du matériel {}", materielId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/general")
    @Operation(summary = "Vérifier la disponibilité générale", 
               description = "Vérifie la disponibilité générale d'un pack ou matériel")
    public ResponseEntity<AvailabilityInfoDto> checkGeneralAvailability(
            @Parameter(description = "ID du pack (optionnel)") @RequestParam(required = false) Long packId,
            @Parameter(description = "ID du matériel (optionnel)") @RequestParam(required = false) Long materielId) {
        
        if (packId == null && materielId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Vérification de disponibilité générale pour pack: {}, matériel: {}", packId, materielId);
        
        try {
            AvailabilityInfoDto availability = availabilityService.checkGeneralAvailability(packId, materielId);
            return ResponseEntity.ok(availability);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de disponibilité générale", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/debug/pack/{packId}")
    @Operation(summary = "Debug - Informations détaillées d'un pack", 
               description = "Retourne les informations détaillées d'un pack pour debug")
    public ResponseEntity<Map<String, Object>> debugPackAvailability(
            @Parameter(description = "ID du pack") @PathVariable Long packId) {
        
        log.info("Debug - Informations détaillées pour le pack {}", packId);
        
        try {
            Map<String, Object> debugInfo = availabilityService.getPackDebugInfo(packId);
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            log.error("Erreur lors du debug du pack {}", packId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/debug/pack/{packId}/availability")
    @Operation(summary = "Debug - Test disponibilité d'un pack", 
               description = "Teste la disponibilité d'un pack pour une période spécifique")
    public ResponseEntity<Map<String, Object>> debugPackAvailabilityWithDates(
            @Parameter(description = "ID du pack") @PathVariable Long packId,
            @Parameter(description = "Date de début") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Date de fin") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Debug - Test disponibilité pour le pack {} du {} au {}", packId, startDate, endDate);
        
        try {
            Map<String, Object> debugInfo = availabilityService.debugPackAvailability(packId, startDate, endDate);
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            log.error("Erreur lors du debug de disponibilité du pack {}", packId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/debug/pack/{packId}/simple")
    @Operation(summary = "Debug - Test simple disponibilité d'un pack", 
               description = "Teste simple la disponibilité d'un pack")
    public ResponseEntity<AvailabilityInfoDto> debugPackSimple(
            @Parameter(description = "ID du pack") @PathVariable Long packId,
            @Parameter(description = "Date de début") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Date de fin") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Debug SIMPLE - Test disponibilité pour le pack {} du {} au {}", packId, startDate, endDate);
        
        try {
            AvailabilityInfoDto result = availabilityService.checkPackAvailability(packId, startDate, endDate);
            log.info("Résultat debug simple: availableQuantity={}, isAvailable={}, message={}", 
                    result.getAvailableQuantity(), result.getIsAvailable(), result.getMessage());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erreur lors du debug simple du pack {}", packId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/debug/materiel/{materielId}")
    @Operation(summary = "Debug - Informations détaillées d'un matériel", 
               description = "Retourne les informations détaillées d'un matériel pour debug")
    public ResponseEntity<Map<String, Object>> debugMateriel(
            @Parameter(description = "ID du matériel") @PathVariable Long materielId) {
        
        log.info("Debug - Informations détaillées pour le matériel {}", materielId);
        
        try {
            Map<String, Object> debugInfo = availabilityService.getMaterielDebugInfo(materielId);
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            log.error("Erreur lors du debug du matériel {}", materielId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/debug/materiel/{materielId}/fix-stock")
    @Operation(summary = "Debug - Corriger le stock disponible d'un matériel", 
               description = "Corrige le stock disponible d'un matériel en le synchronisant avec le stock total")
    public ResponseEntity<Map<String, Object>> fixMaterielStock(
            @Parameter(description = "ID du matériel") @PathVariable Long materielId) {
        
        log.info("Debug - Correction du stock pour le matériel {}", materielId);
        
        try {
            Map<String, Object> result = availabilityService.fixMaterielStock(materielId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erreur lors de la correction du stock du matériel {}", materielId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

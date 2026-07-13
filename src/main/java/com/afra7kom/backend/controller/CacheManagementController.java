package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.AvailabilityInfoDto;
import com.afra7kom.backend.service.CacheService;
import com.afra7kom.backend.service.CachedAvailabilityService;
import com.afra7kom.backend.service.CacheInvalidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cache Management", description = "API pour gérer le cache et tester les performances")
public class CacheManagementController {

    private final CacheService cacheService;
    private final CachedAvailabilityService cachedAvailabilityService;
    private final CacheInvalidationService cacheInvalidationService;
    private final CacheManager cacheManager;

    @GetMapping("/availability-month-cached")
    @Operation(summary = "API availability-month avec cache", 
               description = "Version avec cache de l'API availability-month")
    public ResponseEntity<Map<String, Object>> getCachedMonthlyAvailability(
            @Parameter(description = "ID du pack") @RequestParam Long packId,
            @Parameter(description = "ID du matériel (optionnel)") @RequestParam(required = false) Long materielId,
            @Parameter(description = "Année") @RequestParam int year,
            @Parameter(description = "Mois") @RequestParam int month) {
        
        log.info("🚀 API AVEC CACHE - Pack: {}, Matériel: {}, Mois: {}/{}", packId, materielId, month, year);
        
        long startTime = System.currentTimeMillis();
        Map<String, AvailabilityInfoDto> result;
        
        if (packId != null) {
            result = cachedAvailabilityService.getCachedPackMonthlyAvailability(packId, year, month);
        } else if (materielId != null) {
            result = cachedAvailabilityService.getCachedMaterielMonthlyAvailability(materielId, year, month);
        } else {
            return ResponseEntity.badRequest().build();
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("executionTime", executionTime + "ms");
        response.put("daysProcessed", result.size());
        response.put("cacheHit", executionTime < 100); // Si < 100ms, probablement un cache hit
        
        log.info("✅ API avec cache exécutée en {}ms pour {} jours", executionTime, result.size());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/availability-daily-cached")
    @Operation(summary = "API availability-daily avec cache", 
               description = "Version avec cache de l'API availability-daily")
    public ResponseEntity<Map<String, Object>> getCachedDailyAvailability(
            @Parameter(description = "ID du pack") @RequestParam(required = false) Long packId,
            @Parameter(description = "ID du matériel") @RequestParam(required = false) Long materielId,
            @Parameter(description = "Date") @RequestParam String date) {
        
        LocalDate testDate = LocalDate.parse(date);
        log.info("🚀 API AVEC CACHE - Pack: {}, Matériel: {}, Date: {}", packId, materielId, testDate);
        
        long startTime = System.currentTimeMillis();
        AvailabilityInfoDto result;
        
        if (packId != null) {
            result = cachedAvailabilityService.getCachedPackDailyAvailability(packId, testDate);
        } else if (materielId != null) {
            result = cachedAvailabilityService.getCachedMaterielDailyAvailability(materielId, testDate);
        } else {
            return ResponseEntity.badRequest().build();
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("executionTime", executionTime + "ms");
        response.put("cacheHit", executionTime < 50); // Si < 50ms, probablement un cache hit
        
        log.info("✅ API avec cache exécutée en {}ms", executionTime);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invalidate/pack/{packId}")
    @Operation(summary = "Invalider le cache d'un pack", 
               description = "Invalide tout le cache lié à un pack spécifique")
    public ResponseEntity<Map<String, Object>> invalidatePackCache(
            @Parameter(description = "ID du pack") @PathVariable Long packId) {
        
        log.info("🗑️ INVALIDATION CACHE - Pack {}", packId);
        
        // Invalider le cache des détails du pack
        cacheService.evictPackDetails(packId);
        
        // Invalider le cache de disponibilité pour les 3 prochains mois
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 3; i++) {
            LocalDate month = now.plusMonths(i);
            cacheService.evictPackMonthlyAvailability(packId, month.getYear(), month.getMonthValue());
        }
        
        // Invalider le cache des réservations du pack
        cacheService.evictPackReservations(packId, null);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cache invalidé pour le pack " + packId);
        response.put("packId", packId);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invalidate/materiel/{materielId}")
    @Operation(summary = "Invalider le cache d'un matériel", 
               description = "Invalide tout le cache lié à un matériel spécifique")
    public ResponseEntity<Map<String, Object>> invalidateMaterielCache(
            @Parameter(description = "ID du matériel") @PathVariable Long materielId) {
        
        log.info("🗑️ INVALIDATION CACHE - Matériel {}", materielId);
        
        // Invalider le cache des détails du matériel
        cacheService.evictMaterielDetails(materielId);
        
        // Invalider le cache de disponibilité pour les 3 prochains mois
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 3; i++) {
            LocalDate month = now.plusMonths(i);
            cacheService.evictMaterielMonthlyAvailability(materielId, month.getYear(), month.getMonthValue());
        }
        
        // Invalider le cache des relations pack-matériel
        cacheService.evictPackMateriels(materielId);
        
        // Invalider le cache des réservations du matériel
        cacheService.evictMaterielReservations(materielId, null);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cache invalidé pour le matériel " + materielId);
        response.put("materielId", materielId);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invalidate/all")
    @Operation(summary = "Invalider tout le cache", 
               description = "Invalide tout le cache (à utiliser avec précaution)")
    public ResponseEntity<Map<String, Object>> invalidateAllCache() {
        
        log.info("🗑️ INVALIDATION CACHE - Tout le cache");
        
        cacheInvalidationService.invalidateAllCache();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tout le cache a été invalidé");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invalidate/availability")
    @Operation(summary = "Invalider le cache de disponibilité", 
               description = "Invalide uniquement le cache de disponibilité")
    public ResponseEntity<Map<String, Object>> invalidateAvailabilityCache() {
        
        log.info("🗑️ INVALIDATION CACHE - Cache de disponibilité");
        
        cacheInvalidationService.invalidateAllAvailabilityCache();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cache de disponibilité invalidé");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques du cache", 
               description = "Retourne les statistiques du cache Caffeine")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        
        Map<String, Object> stats = new HashMap<>();
        
        // Statistiques pour chaque cache
        String[] cacheNames = {"availability-month", "availability-daily", "pack-details", "materiel-details", "pack-materiels", "reservations", "categories"};
        
        for (String cacheName : cacheNames) {
            try {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    Map<String, Object> cacheStats = new HashMap<>();
                    cacheStats.put("name", cacheName);
                    cacheStats.put("nativeCache", cache.getNativeCache().getClass().getSimpleName());
                    stats.put(cacheName, cacheStats);
                }
            } catch (Exception e) {
                log.warn("Impossible de récupérer les statistiques pour le cache {}", cacheName, e);
            }
        }
        
        stats.put("timestamp", System.currentTimeMillis());
        stats.put("totalCaches", cacheNames.length);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/test-performance")
    @Operation(summary = "Test de performance du cache", 
               description = "Teste les performances avec et sans cache")
    public ResponseEntity<Map<String, Object>> testCachePerformance(
            @Parameter(description = "ID du pack") @RequestParam Long packId,
            @Parameter(description = "Année") @RequestParam int year,
            @Parameter(description = "Mois") @RequestParam int month) {
        
        log.info("🧪 TEST DE PERFORMANCE - Pack: {}, Mois: {}/{}", packId, month, year);
        
        Map<String, Object> results = new HashMap<>();
        
        // Test 1: Premier appel (cache miss)
        long startTime = System.currentTimeMillis();
        Map<String, AvailabilityInfoDto> firstCall = cachedAvailabilityService.getCachedPackMonthlyAvailability(packId, year, month);
        long firstCallTime = System.currentTimeMillis() - startTime;
        
        // Test 2: Deuxième appel (cache hit)
        startTime = System.currentTimeMillis();
        Map<String, AvailabilityInfoDto> secondCall = cachedAvailabilityService.getCachedPackMonthlyAvailability(packId, year, month);
        long secondCallTime = System.currentTimeMillis() - startTime;
        
        // Test 3: Troisième appel (cache hit)
        startTime = System.currentTimeMillis();
        Map<String, AvailabilityInfoDto> thirdCall = cachedAvailabilityService.getCachedPackMonthlyAvailability(packId, year, month);
        long thirdCallTime = System.currentTimeMillis() - startTime;
        
        results.put("packId", packId);
        results.put("year", year);
        results.put("month", month);
        results.put("firstCall", Map.of(
            "executionTime", firstCallTime + "ms",
            "cacheHit", false,
            "daysProcessed", firstCall.size()
        ));
        results.put("secondCall", Map.of(
            "executionTime", secondCallTime + "ms",
            "cacheHit", true,
            "daysProcessed", secondCall.size()
        ));
        results.put("thirdCall", Map.of(
            "executionTime", thirdCallTime + "ms",
            "cacheHit", true,
            "daysProcessed", thirdCall.size()
        ));
        
        // Calculer l'amélioration
        if (firstCallTime > 0) {
            long improvement = firstCallTime - secondCallTime;
            double improvementPercent = (double) improvement / firstCallTime * 100;
            
            results.put("improvement", Map.of(
                "timeSaved", improvement + "ms",
                "improvementPercent", String.format("%.1f%%", improvementPercent),
                "speedup", String.format("%.1fx", (double) firstCallTime / secondCallTime)
            ));
        }
        
        results.put("timestamp", System.currentTimeMillis());
        
        log.info("🎯 RÉSULTATS: 1er appel: {}ms, 2ème appel: {}ms, 3ème appel: {}ms", 
                firstCallTime, secondCallTime, thirdCallTime);
        
        return ResponseEntity.ok(results);
    }
}


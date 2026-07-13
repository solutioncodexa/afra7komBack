package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.MaterielDto;
import com.afra7kom.backend.dto.PackDto;
import com.afra7kom.backend.dto.CategorieDto;
import com.afra7kom.backend.service.CachedListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance/lists")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "List Performance", description = "API pour tester les performances des listes avec cache")
public class ListPerformanceController {

    private final CachedListService cachedListService;

    // ==============================================
    // TESTS DE PERFORMANCE POUR LES PACKS
    // ==============================================

    @GetMapping("/packs")
    @Operation(summary = "Test de performance - Liste des packs avec cache", 
               description = "Teste les performances de la liste des packs avec cache")
    public ResponseEntity<Map<String, Object>> testPacksPerformance(
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size) {
        
        log.info("🧪 TEST DE PERFORMANCE - Liste des packs - Page: {}, Size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        long startTime = System.currentTimeMillis();
        Page<PackDto> result = cachedListService.getCachedActivePacks(pageable);
        long executionTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getContent());
        response.put("executionTime", executionTime + "ms");
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("currentPage", result.getNumber());
        response.put("pageSize", result.getSize());
        response.put("cacheHit", executionTime < 100); // Si < 100ms, probablement un cache hit
        
        log.info("✅ Test packs terminé en {}ms - {} éléments", executionTime, result.getTotalElements());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/packs/category/{categorieId}")
    @Operation(summary = "Test de performance - Packs par catégorie avec cache", 
               description = "Teste les performances des packs par catégorie avec cache")
    public ResponseEntity<Map<String, Object>> testPacksByCategoryPerformance(
            @Parameter(description = "ID de la catégorie") @PathVariable Long categorieId,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size) {
        
        log.info("🧪 TEST DE PERFORMANCE - Packs par catégorie {} - Page: {}, Size: {}", categorieId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        long startTime = System.currentTimeMillis();
        Page<PackDto> result = cachedListService.getCachedPacksByCategorie(categorieId, pageable);
        long executionTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getContent());
        response.put("executionTime", executionTime + "ms");
        response.put("categorieId", categorieId);
        response.put("totalElements", result.getTotalElements());
        response.put("cacheHit", executionTime < 100);
        
        log.info("✅ Test packs par catégorie terminé en {}ms - {} éléments", executionTime, result.getTotalElements());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/packs/search")
    @Operation(summary = "Test de performance - Recherche de packs avec cache", 
               description = "Teste les performances de la recherche de packs avec cache")
    public ResponseEntity<Map<String, Object>> testPacksSearchPerformance(
            @Parameter(description = "Terme de recherche") @RequestParam String search,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size) {
        
        log.info("🧪 TEST DE PERFORMANCE - Recherche de packs: '{}' - Page: {}, Size: {}", search, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        long startTime = System.currentTimeMillis();
        Page<PackDto> result = cachedListService.getCachedSearchPacks(search, pageable);
        long executionTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getContent());
        response.put("executionTime", executionTime + "ms");
        response.put("searchTerm", search);
        response.put("totalElements", result.getTotalElements());
        response.put("cacheHit", executionTime < 100);
        
        log.info("✅ Test recherche packs terminé en {}ms - {} éléments", executionTime, result.getTotalElements());
        
        return ResponseEntity.ok(response);
    }

    // ==============================================
    // TESTS DE PERFORMANCE POUR LES MATÉRIELS
    // ==============================================

    @GetMapping("/materiels")
    @Operation(summary = "Test de performance - Liste des matériels avec cache", 
               description = "Teste les performances de la liste des matériels avec cache")
    public ResponseEntity<Map<String, Object>> testMaterielsPerformance(
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size) {
        
        log.info("🧪 TEST DE PERFORMANCE - Liste des matériels - Page: {}, Size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        long startTime = System.currentTimeMillis();
        Page<MaterielDto> result = cachedListService.getCachedActiveMateriels(pageable);
        long executionTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getContent());
        response.put("executionTime", executionTime + "ms");
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("currentPage", result.getNumber());
        response.put("pageSize", result.getSize());
        response.put("cacheHit", executionTime < 100);
        
        log.info("✅ Test matériels terminé en {}ms - {} éléments", executionTime, result.getTotalElements());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/materiels/search")
    @Operation(summary = "Test de performance - Recherche de matériels avec cache", 
               description = "Teste les performances de la recherche de matériels avec cache")
    public ResponseEntity<Map<String, Object>> testMaterielsSearchPerformance(
            @Parameter(description = "Terme de recherche") @RequestParam String search,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size) {
        
        log.info("🧪 TEST DE PERFORMANCE - Recherche de matériels: '{}' - Page: {}, Size: {}", search, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        long startTime = System.currentTimeMillis();
        Page<MaterielDto> result = cachedListService.getCachedSearchMateriels(search, pageable);
        long executionTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getContent());
        response.put("executionTime", executionTime + "ms");
        response.put("searchTerm", search);
        response.put("totalElements", result.getTotalElements());
        response.put("cacheHit", executionTime < 100);
        
        log.info("✅ Test recherche matériels terminé en {}ms - {} éléments", executionTime, result.getTotalElements());
        
        return ResponseEntity.ok(response);
    }

    // ==============================================
    // TESTS DE PERFORMANCE POUR LES CATÉGORIES
    // ==============================================

    @GetMapping("/categories")
    @Operation(summary = "Test de performance - Liste des catégories avec cache", 
               description = "Teste les performances de la liste des catégories avec cache")
    public ResponseEntity<Map<String, Object>> testCategoriesPerformance() {
        
        log.info("🧪 TEST DE PERFORMANCE - Liste des catégories");
        
        long startTime = System.currentTimeMillis();
        List<CategorieDto> result = cachedListService.getCachedActiveCategories();
        long executionTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("executionTime", executionTime + "ms");
        response.put("totalElements", result.size());
        response.put("cacheHit", executionTime < 50); // Si < 50ms, probablement un cache hit
        
        log.info("✅ Test catégories terminé en {}ms - {} éléments", executionTime, result.size());
        
        return ResponseEntity.ok(response);
    }

    // ==============================================
    // TESTS DE PERFORMANCE COMPARATIFS
    // ==============================================

    @GetMapping("/compare/packs")
    @Operation(summary = "Test comparatif - Packs avec et sans cache", 
               description = "Compare les performances des packs avec et sans cache")
    public ResponseEntity<Map<String, Object>> comparePacksPerformance(
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size) {
        
        log.info("🧪 TEST COMPARATIF - Packs - Page: {}, Size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Map<String, Object> results = new HashMap<>();
        
        // Test 1: Premier appel (cache miss)
        long startTime = System.currentTimeMillis();
        Page<PackDto> firstCall = cachedListService.getCachedActivePacks(pageable);
        long firstCallTime = System.currentTimeMillis() - startTime;
        
        // Test 2: Deuxième appel (cache hit)
        startTime = System.currentTimeMillis();
        Page<PackDto> secondCall = cachedListService.getCachedActivePacks(pageable);
        long secondCallTime = System.currentTimeMillis() - startTime;
        
        // Test 3: Troisième appel (cache hit)
        startTime = System.currentTimeMillis();
        Page<PackDto> thirdCall = cachedListService.getCachedActivePacks(pageable);
        long thirdCallTime = System.currentTimeMillis() - startTime;
        
        results.put("firstCall", Map.of(
            "executionTime", firstCallTime + "ms",
            "cacheHit", false,
            "totalElements", firstCall.getTotalElements()
        ));
        results.put("secondCall", Map.of(
            "executionTime", secondCallTime + "ms",
            "cacheHit", true,
            "totalElements", secondCall.getTotalElements()
        ));
        results.put("thirdCall", Map.of(
            "executionTime", thirdCallTime + "ms",
            "cacheHit", true,
            "totalElements", thirdCall.getTotalElements()
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
        
        log.info("🎯 RÉSULTATS COMPARATIFS: 1er appel: {}ms, 2ème appel: {}ms, 3ème appel: {}ms", 
                firstCallTime, secondCallTime, thirdCallTime);
        
        return ResponseEntity.ok(results);
    }

    @GetMapping("/compare/materiels")
    @Operation(summary = "Test comparatif - Matériels avec et sans cache", 
               description = "Compare les performances des matériels avec et sans cache")
    public ResponseEntity<Map<String, Object>> compareMaterielsPerformance(
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size) {
        
        log.info("🧪 TEST COMPARATIF - Matériels - Page: {}, Size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Map<String, Object> results = new HashMap<>();
        
        // Test 1: Premier appel (cache miss)
        long startTime = System.currentTimeMillis();
        Page<MaterielDto> firstCall = cachedListService.getCachedActiveMateriels(pageable);
        long firstCallTime = System.currentTimeMillis() - startTime;
        
        // Test 2: Deuxième appel (cache hit)
        startTime = System.currentTimeMillis();
        Page<MaterielDto> secondCall = cachedListService.getCachedActiveMateriels(pageable);
        long secondCallTime = System.currentTimeMillis() - startTime;
        
        results.put("firstCall", Map.of(
            "executionTime", firstCallTime + "ms",
            "cacheHit", false,
            "totalElements", firstCall.getTotalElements()
        ));
        results.put("secondCall", Map.of(
            "executionTime", secondCallTime + "ms",
            "cacheHit", true,
            "totalElements", secondCall.getTotalElements()
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
        
        log.info("🎯 RÉSULTATS COMPARATIFS: 1er appel: {}ms, 2ème appel: {}ms", firstCallTime, secondCallTime);
        
        return ResponseEntity.ok(results);
    }

    // ==============================================
    // TESTS DE CHARGE
    // ==============================================

    @GetMapping("/load-test/packs")
    @Operation(summary = "Test de charge - Packs", 
               description = "Teste la charge sur l'API des packs avec cache")
    public ResponseEntity<Map<String, Object>> loadTestPacks(
            @Parameter(description = "Nombre de requêtes") @RequestParam(defaultValue = "10") int requests,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size) {
        
        log.info("🧪 TEST DE CHARGE - Packs - {} requêtes - Page: {}, Size: {}", requests, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < requests; i++) {
            cachedListService.getCachedActivePacks(pageable);
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        long averageTime = totalTime / requests;
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalRequests", requests);
        response.put("totalTime", totalTime + "ms");
        response.put("averageTime", averageTime + "ms");
        response.put("requestsPerSecond", String.format("%.2f", (double) requests * 1000 / totalTime));
        
        log.info("✅ Test de charge terminé - {} requêtes en {}ms (moyenne: {}ms)", requests, totalTime, averageTime);
        
        return ResponseEntity.ok(response);
    }
}

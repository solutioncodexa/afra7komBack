package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.MaterielDto;
import com.afra7kom.backend.dto.PackDto;
import com.afra7kom.backend.dto.PackDetailDto;
import com.afra7kom.backend.dto.CategorieDto;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.Categorie;
import com.afra7kom.backend.repository.MaterielRepository;
import com.afra7kom.backend.repository.PackRepository;
import com.afra7kom.backend.repository.CategorieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CachedListService {

    private final MaterielRepository materielRepository;
    private final PackRepository packRepository;
    private final CategorieRepository categorieRepository;

    // ==============================================
    // CACHE POUR LES PACKS
    // ==============================================

    /**
     * Cache la liste des packs actifs
     * Clé: "packs:active:page:{page}:size:{size}"
     */
    @Cacheable(value = "packs-list", key = "'packs:active:page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<PackDto> getCachedActivePacks(Pageable pageable) {
        log.info("🔍 CACHE MISS - Récupération des packs actifs - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return packRepository.findByActiveTrue(pageable)
                .map(PackDto::fromEntitySimple);
    }

    /**
     * Cache la liste des packs par catégorie
     * Clé: "packs:category:{categorieId}:page:{page}:size:{size}"
     */
    @Cacheable(value = "packs-list", key = "'packs:category:' + #categorieId + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<PackDto> getCachedPacksByCategorie(Long categorieId, Pageable pageable) {
        log.info("🔍 CACHE MISS - Récupération des packs par catégorie {} - Page: {}, Size: {}", categorieId, pageable.getPageNumber(), pageable.getPageSize());
        return packRepository.findByActiveTrueAndCategorieId(categorieId, pageable)
                .map(PackDto::fromEntitySimple);
    }

    /**
     * Cache la recherche de packs
     * Clé: "packs:search:{search}:page:{page}:size:{size}"
     */
    @Cacheable(value = "packs-list", key = "'packs:search:' + #search + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<PackDto> getCachedSearchPacks(String search, Pageable pageable) {
        log.info("🔍 CACHE MISS - Recherche de packs: '{}' - Page: {}, Size: {}", search, pageable.getPageNumber(), pageable.getPageSize());
        return packRepository.findByActiveTrueAndNameContainingIgnoreCase(search, pageable)
                .map(PackDto::fromEntitySimple);
    }

    /**
     * Cache les packs avec filtres
     * Clé: "packs:filters:active:{active}:category:{categorieId}:minPrice:{minPrice}:maxPrice:{maxPrice}:page:{page}:size:{size}"
     */
    @Cacheable(value = "packs-list", key = "'packs:filters:active:' + #active + ':category:' + #categorieId + ':minPrice:' + #minPrice + ':maxPrice:' + #maxPrice + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<PackDto> getCachedPacksWithFilters(Boolean active, Long categorieId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        log.info("🔍 CACHE MISS - Packs avec filtres - Active: {}, Category: {}, MinPrice: {}, MaxPrice: {}, Page: {}, Size: {}", 
                active, categorieId, minPrice, maxPrice, pageable.getPageNumber(), pageable.getPageSize());
        return packRepository.findWithFilters(active, categorieId, null, minPrice, maxPrice, pageable)
                .map(PackDto::fromEntitySimple);
    }

    // ==============================================
    // CACHE POUR LES MATÉRIELS
    // ==============================================

    /**
     * Cache la liste des matériels actifs
     * Clé: "materiels:active:page:{page}:size:{size}"
     */
    @Cacheable(value = "materiels-list", key = "'materiels:active:page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<MaterielDto> getCachedActiveMateriels(Pageable pageable) {
        log.info("🔍 CACHE MISS - Récupération des matériels actifs - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return materielRepository.findByActiveTrue(pageable)
                .map(MaterielDto::fromEntity);
    }

    /**
     * Cache la liste des matériels par catégorie
     * Clé: "materiels:category:{categorieId}:page:{page}:size:{size}"
     */
    @Cacheable(value = "materiels-list", key = "'materiels:category:' + #categorieId + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<MaterielDto> getCachedMaterielsByCategorie(Long categorieId, Pageable pageable) {
        log.info("🔍 CACHE MISS - Récupération des matériels par catégorie {} - Page: {}, Size: {}", categorieId, pageable.getPageNumber(), pageable.getPageSize());
        return materielRepository.findByActiveTrueAndCategorieId(categorieId, pageable)
                .map(MaterielDto::fromEntity);
    }

    /**
     * Cache la recherche de matériels
     * Clé: "materiels:search:{search}:page:{page}:size:{size}"
     */
    @Cacheable(value = "materiels-list", key = "'materiels:search:' + #search + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<MaterielDto> getCachedSearchMateriels(String search, Pageable pageable) {
        log.info("🔍 CACHE MISS - Recherche de matériels: '{}' - Page: {}, Size: {}", search, pageable.getPageNumber(), pageable.getPageSize());
        return materielRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable)
                .map(MaterielDto::fromEntity);
    }

    /**
     * Cache les matériels avec filtres
     * Clé: "materiels:filters:active:{active}:category:{categorieId}:minPrice:{minPrice}:maxPrice:{maxPrice}:available:{isAvailable}:page:{page}:size:{size}"
     */
    @Cacheable(value = "materiels-list", key = "'materiels:filters:active:' + #active + ':category:' + #categorieId + ':minPrice:' + #minPrice + ':maxPrice:' + #maxPrice + ':available:' + #isAvailable + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<MaterielDto> getCachedMaterielsWithFilters(Boolean active, Long categorieId, BigDecimal minPrice, BigDecimal maxPrice, Boolean isAvailable, Pageable pageable) {
        log.info("🔍 CACHE MISS - Matériels avec filtres - Active: {}, Category: {}, MinPrice: {}, MaxPrice: {}, Available: {}, Page: {}, Size: {}", 
                active, categorieId, minPrice, maxPrice, isAvailable, pageable.getPageNumber(), pageable.getPageSize());
        return materielRepository.findWithFilters(active, categorieId, minPrice, maxPrice, isAvailable, pageable)
                .map(MaterielDto::fromEntity);
    }

    // ==============================================
    // CACHE POUR LES CATÉGORIES
    // ==============================================

    /**
     * Cache toutes les catégories actives
     * Clé: "categories:active"
     */
    @Cacheable(value = "categories", key = "'categories:active'")
    public List<CategorieDto> getCachedActiveCategories() {
        log.info("🔍 CACHE MISS - Récupération des catégories actives");
        return categorieRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(CategorieDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Cache les catégories avec items
     * Clé: "categories:with-items"
     */
    @Cacheable(value = "categories", key = "'categories:with-items'")
    public List<CategorieDto> getCachedCategoriesWithItems() {
        log.info("🔍 CACHE MISS - Récupération des catégories avec items");
        return categorieRepository.findCategoriesWithItems().stream()
                .map(CategorieDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Cache la recherche de catégories
     * Clé: "categories:search:active:{active}:name:{name}:page:{page}:size:{size}"
     */
    @Cacheable(value = "categories", key = "'categories:search:active:' + #active + ':name:' + #name + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<CategorieDto> getCachedSearchCategories(Boolean active, String name, Pageable pageable) {
        log.info("🔍 CACHE MISS - Recherche de catégories - Active: {}, Name: '{}', Page: {}, Size: {}", 
                active, name, pageable.getPageNumber(), pageable.getPageSize());
        return categorieRepository.findWithFilters(active, name, pageable)
                .map(CategorieDto::fromEntity);
    }

    // ==============================================
    // CACHE POUR LES DÉTAILS INDIVIDUELS
    // ==============================================

    /**
     * Cache les détails d'un pack (version simple)
     * Clé: "pack:details:{packId}"
     */
    @Cacheable(value = "pack-details", key = "'pack:details:' + #packId")
    public PackDto getCachedPackDetails(Long packId) {
        log.info("🔍 CACHE MISS - Récupération des détails du pack {}", packId);
        Pack pack = packRepository.findByIdWithMaterielsAndImages(packId)
                .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));
        return PackDto.fromEntity(pack);
    }

    /**
     * Cache les détails complets d'un pack
     * Clé: "pack:detail-dto:{packId}"
     */
    @Cacheable(value = "pack-details", key = "'pack:detail-dto:' + #packId")
    public PackDetailDto getCachedPackDetailDto(Long packId) {
        log.info("🔍 CACHE MISS - Récupération des détails complets du pack {}", packId);
        Pack pack = packRepository.findByIdWithMaterielsAndImages(packId)
                .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));
        return PackDetailDto.fromEntity(pack);
    }

    /**
     * Cache les détails d'un matériel
     * Clé: "materiel:details:{materielId}"
     */
    @Cacheable(value = "materiel-details", key = "'materiel:details:' + #materielId")
    public MaterielDto getCachedMaterielDetails(Long materielId) {
        log.info("🔍 CACHE MISS - Récupération des détails du matériel {}", materielId);
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new RuntimeException("Matériel non trouvé avec l'ID: " + materielId));
        return MaterielDto.fromEntity(materiel);
    }

    /**
     * Cache les détails d'une catégorie
     * Clé: "category:details:{categorieId}"
     */
    @Cacheable(value = "categories", key = "'category:details:' + #categorieId")
    public CategorieDto getCachedCategorieDetails(Long categorieId) {
        log.info("🔍 CACHE MISS - Récupération des détails de la catégorie {}", categorieId);
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'ID: " + categorieId));
        return CategorieDto.fromEntity(categorie);
    }

    // ==============================================
    // MÉTHODES D'INVALIDATION DU CACHE
    // ==============================================

    /**
     * Vider tous les caches liés aux packs
     * À appeler après création, mise à jour ou suppression d'un pack
     */
    @CacheEvict(value = {"packs-list", "pack-details"}, allEntries = true)
    public void evictAllPackCaches() {
        log.info("🗑️ CACHE EVICTION - Tous les caches de packs ont été vidés");
    }

    /**
     * Vider le cache d'un pack spécifique
     */
    @CacheEvict(value = "pack-details", key = "'pack:details:' + #packId")
    public void evictPackDetails(Long packId) {
        log.info("🗑️ CACHE EVICTION - Cache du pack {} vidé", packId);
    }

    /**
     * Vider tous les caches liés aux matériels
     * À appeler après création, mise à jour ou suppression d'un matériel
     */
    @CacheEvict(value = {"materiels-list", "materiel-details"}, allEntries = true)
    public void evictAllMaterielCaches() {
        log.info("🗑️ CACHE EVICTION - Tous les caches de matériels ont été vidés");
    }

    /**
     * Vider le cache d'un matériel spécifique
     */
    @CacheEvict(value = "materiel-details", key = "'materiel:details:' + #materielId")
    public void evictMaterielDetails(Long materielId) {
        log.info("🗑️ CACHE EVICTION - Cache du matériel {} vidé", materielId);
    }

    /**
     * Vider tous les caches liés aux catégories
     * À appeler après création, mise à jour ou suppression d'une catégorie
     */
    @CacheEvict(value = "categories", allEntries = true)
    public void evictAllCategorieCaches() {
        log.info("🗑️ CACHE EVICTION - Tous les caches de catégories ont été vidés");
    }
}

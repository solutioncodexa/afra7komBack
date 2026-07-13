package com.afra7kom.backend.service;

import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.Categorie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListCacheInvalidationService {

    // ==============================================
    // INVALIDATION LORS DES OPÉRATIONS SUR LES PACKS
    // ==============================================

    /**
     * Invalide le cache lors de la création d'un pack
     */
    @CacheEvict(value = {"packs-list", "pack-details"}, allEntries = true)
    public void invalidateCacheOnPackCreate(Pack pack) {
        log.info("🗑️ INVALIDATION CACHE - Création du pack {}", pack.getId());
    }

    /**
     * Invalide le cache lors de la mise à jour d'un pack
     */
    @CacheEvict(value = {"packs-list", "pack-details"}, allEntries = true)
    public void invalidateCacheOnPackUpdate(Pack pack) {
        log.info("🗑️ INVALIDATION CACHE - Mise à jour du pack {}", pack.getId());
    }

    /**
     * Invalide le cache lors de la suppression d'un pack
     */
    @CacheEvict(value = {"packs-list", "pack-details"}, allEntries = true)
    public void invalidateCacheOnPackDelete(Long packId) {
        log.info("🗑️ INVALIDATION CACHE - Suppression du pack {}", packId);
    }

    // ==============================================
    // INVALIDATION LORS DES OPÉRATIONS SUR LES MATÉRIELS
    // ==============================================

    /**
     * Invalide le cache lors de la création d'un matériel
     */
    @CacheEvict(value = {"materiels-list", "materiel-details"}, allEntries = true)
    public void invalidateCacheOnMaterielCreate(Materiel materiel) {
        log.info("🗑️ INVALIDATION CACHE - Création du matériel {}", materiel.getId());
    }

    /**
     * Invalide le cache lors de la mise à jour d'un matériel
     */
    @CacheEvict(value = {"materiels-list", "materiel-details"}, allEntries = true)
    public void invalidateCacheOnMaterielUpdate(Materiel materiel) {
        log.info("🗑️ INVALIDATION CACHE - Mise à jour du matériel {}", materiel.getId());
    }

    /**
     * Invalide le cache lors de la suppression d'un matériel
     */
    @CacheEvict(value = {"materiels-list", "materiel-details"}, allEntries = true)
    public void invalidateCacheOnMaterielDelete(Long materielId) {
        log.info("🗑️ INVALIDATION CACHE - Suppression du matériel {}", materielId);
    }

    // ==============================================
    // INVALIDATION LORS DES OPÉRATIONS SUR LES CATÉGORIES
    // ==============================================

    /**
     * Invalide le cache lors de la création d'une catégorie
     */
    @CacheEvict(value = {"categories", "packs-list", "materiels-list"}, allEntries = true)
    public void invalidateCacheOnCategorieCreate(Categorie categorie) {
        log.info("🗑️ INVALIDATION CACHE - Création de la catégorie {}", categorie.getId());
    }

    /**
     * Invalide le cache lors de la mise à jour d'une catégorie
     */
    @CacheEvict(value = {"categories", "packs-list", "materiels-list"}, allEntries = true)
    public void invalidateCacheOnCategorieUpdate(Categorie categorie) {
        log.info("🗑️ INVALIDATION CACHE - Mise à jour de la catégorie {}", categorie.getId());
    }

    /**
     * Invalide le cache lors de la suppression d'une catégorie
     */
    @CacheEvict(value = {"categories", "packs-list", "materiels-list"}, allEntries = true)
    public void invalidateCacheOnCategorieDelete(Long categorieId) {
        log.info("🗑️ INVALIDATION CACHE - Suppression de la catégorie {}", categorieId);
    }

    // ==============================================
    // INVALIDATION GLOBALE
    // ==============================================

    /**
     * Invalide tout le cache des listes
     */
    @CacheEvict(value = {"packs-list", "materiels-list", "categories", "pack-details", "materiel-details"}, allEntries = true)
    public void invalidateAllListCache() {
        log.info("🗑️ INVALIDATION CACHE - Invalidation de tout le cache des listes");
    }

    /**
     * Invalide le cache des packs
     */
    @CacheEvict(value = {"packs-list", "pack-details"}, allEntries = true)
    public void invalidatePacksCache() {
        log.info("🗑️ INVALIDATION CACHE - Invalidation du cache des packs");
    }

    /**
     * Invalide le cache des matériels
     */
    @CacheEvict(value = {"materiels-list", "materiel-details"}, allEntries = true)
    public void invalidateMaterielsCache() {
        log.info("🗑️ INVALIDATION CACHE - Invalidation du cache des matériels");
    }

    /**
     * Invalide le cache des catégories
     */
    @CacheEvict(value = "categories", allEntries = true)
    public void invalidateCategoriesCache() {
        log.info("🗑️ INVALIDATION CACHE - Invalidation du cache des catégories");
    }
}


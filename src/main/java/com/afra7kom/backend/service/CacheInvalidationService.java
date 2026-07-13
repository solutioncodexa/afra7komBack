package com.afra7kom.backend.service;

import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.Categorie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationService {

    private final CacheService cacheService;

    // ==============================================
    // INVALIDATION LORS DES OPÉRATIONS SUR LES RÉSERVATIONS
    // ==============================================

    /**
     * Invalide le cache lors de la création d'une réservation
     */
    public void invalidateCacheOnReservationCreate(Reservation reservation) {
        log.info("🗑️ INVALIDATION CACHE - Création d'une réservation");
        
        if (reservation.getPack() != null) {
            // Invalider le cache de disponibilité mensuelle pour le pack
            invalidatePackAvailabilityCache(reservation.getPack().getId(), reservation.getStartDate(), reservation.getEndDate());
        }
        
        if (reservation.getMateriel() != null) {
            // Invalider le cache de disponibilité mensuelle pour le matériel
            invalidateMaterielAvailabilityCache(reservation.getMateriel().getId(), reservation.getStartDate(), reservation.getEndDate());
        }
        
        // Invalider le cache des réservations
        if (reservation.getPack() != null) {
            cacheService.evictPackReservations(reservation.getPack().getId(), reservation.getStatus());
        }
        if (reservation.getMateriel() != null) {
            cacheService.evictMaterielReservations(reservation.getMateriel().getId(), reservation.getStatus());
        }
    }

    /**
     * Invalide le cache lors de la mise à jour d'une réservation
     */
    public void invalidateCacheOnReservationUpdate(Reservation oldReservation, Reservation newReservation) {
        log.info("🗑️ INVALIDATION CACHE - Mise à jour d'une réservation");
        
        // Invalider le cache pour l'ancienne réservation
        if (oldReservation.getPack() != null) {
            invalidatePackAvailabilityCache(oldReservation.getPack().getId(), oldReservation.getStartDate(), oldReservation.getEndDate());
        }
        if (oldReservation.getMateriel() != null) {
            invalidateMaterielAvailabilityCache(oldReservation.getMateriel().getId(), oldReservation.getStartDate(), oldReservation.getEndDate());
        }
        
        // Invalider le cache pour la nouvelle réservation
        if (newReservation.getPack() != null) {
            invalidatePackAvailabilityCache(newReservation.getPack().getId(), newReservation.getStartDate(), newReservation.getEndDate());
        }
        if (newReservation.getMateriel() != null) {
            invalidateMaterielAvailabilityCache(newReservation.getMateriel().getId(), newReservation.getStartDate(), newReservation.getEndDate());
        }
        
        // Invalider le cache des réservations
        if (oldReservation.getPack() != null) {
            cacheService.evictPackReservations(oldReservation.getPack().getId(), oldReservation.getStatus());
        }
        if (oldReservation.getMateriel() != null) {
            cacheService.evictMaterielReservations(oldReservation.getMateriel().getId(), oldReservation.getStatus());
        }
        if (newReservation.getPack() != null) {
            cacheService.evictPackReservations(newReservation.getPack().getId(), newReservation.getStatus());
        }
        if (newReservation.getMateriel() != null) {
            cacheService.evictMaterielReservations(newReservation.getMateriel().getId(), newReservation.getStatus());
        }
    }

    /**
     * Invalide le cache lors de la suppression d'une réservation
     */
    public void invalidateCacheOnReservationDelete(Reservation reservation) {
        log.info("🗑️ INVALIDATION CACHE - Suppression d'une réservation");
        
        if (reservation.getPack() != null) {
            invalidatePackAvailabilityCache(reservation.getPack().getId(), reservation.getStartDate(), reservation.getEndDate());
            cacheService.evictPackReservations(reservation.getPack().getId(), reservation.getStatus());
        }
        
        if (reservation.getMateriel() != null) {
            invalidateMaterielAvailabilityCache(reservation.getMateriel().getId(), reservation.getStartDate(), reservation.getEndDate());
            cacheService.evictMaterielReservations(reservation.getMateriel().getId(), reservation.getStatus());
        }
    }

    // ==============================================
    // INVALIDATION LORS DES OPÉRATIONS SUR LES PACKS
    // ==============================================

    /**
     * Invalide le cache lors de la mise à jour d'un pack
     */
    public void invalidateCacheOnPackUpdate(Pack pack) {
        log.info("🗑️ INVALIDATION CACHE - Mise à jour du pack {}", pack.getId());
        
        // Invalider le cache des détails du pack
        cacheService.evictPackDetails(pack.getId());
        
        // Invalider le cache de disponibilité pour les 3 prochains mois
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 3; i++) {
            LocalDate month = now.plusMonths(i);
            cacheService.evictPackMonthlyAvailability(pack.getId(), month.getYear(), month.getMonthValue());
        }
        
        // Invalider le cache des relations pack-matériel pour tous les matériels du pack
        if (pack.getPackMateriels() != null) {
            pack.getPackMateriels().forEach(pm -> {
                cacheService.evictPackMateriels(pm.getMateriel().getId());
            });
        }
    }

    /**
     * Invalide le cache lors de la suppression d'un pack
     */
    public void invalidateCacheOnPackDelete(Long packId) {
        log.info("🗑️ INVALIDATION CACHE - Suppression du pack {}", packId);
        
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
    }

    // ==============================================
    // INVALIDATION LORS DES OPÉRATIONS SUR LES MATÉRIELS
    // ==============================================

    /**
     * Invalide le cache lors de la mise à jour d'un matériel
     */
    public void invalidateCacheOnMaterielUpdate(Materiel materiel) {
        log.info("🗑️ INVALIDATION CACHE - Mise à jour du matériel {}", materiel.getId());
        
        // Invalider le cache des détails du matériel
        cacheService.evictMaterielDetails(materiel.getId());
        
        // Invalider le cache de disponibilité pour les 3 prochains mois
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 3; i++) {
            LocalDate month = now.plusMonths(i);
            cacheService.evictMaterielMonthlyAvailability(materiel.getId(), month.getYear(), month.getMonthValue());
        }
        
        // Invalider le cache des relations pack-matériel
        cacheService.evictPackMateriels(materiel.getId());
        
        // Invalider le cache des réservations du matériel
        cacheService.evictMaterielReservations(materiel.getId(), null);
    }

    /**
     * Invalide le cache lors de la suppression d'un matériel
     */
    public void invalidateCacheOnMaterielDelete(Long materielId) {
        log.info("🗑️ INVALIDATION CACHE - Suppression du matériel {}", materielId);
        
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
    }

    // ==============================================
    // INVALIDATION LORS DES OPÉRATIONS SUR LES CATÉGORIES
    // ==============================================

    /**
     * Invalide le cache lors de la mise à jour d'une catégorie
     */
    public void invalidateCacheOnCategorieUpdate(Categorie categorie) {
        log.info("🗑️ INVALIDATION CACHE - Mise à jour de la catégorie {}", categorie.getId());
        
        // Invalider le cache des catégories
        cacheService.evictActiveCategories();
    }

    /**
     * Invalide le cache lors de la suppression d'une catégorie
     */
    public void invalidateCacheOnCategorieDelete(Long categorieId) {
        log.info("🗑️ INVALIDATION CACHE - Suppression de la catégorie {}", categorieId);
        
        // Invalider le cache des catégories
        cacheService.evictActiveCategories();
    }

    // ==============================================
    // MÉTHODES UTILITAIRES
    // ==============================================

    /**
     * Invalide le cache de disponibilité d'un pack pour une période donnée
     */
    private void invalidatePackAvailabilityCache(Long packId, LocalDate startDate, LocalDate endDate) {
        // Invalider le cache pour tous les mois qui chevauchent avec la période
        LocalDate current = startDate.withDayOfMonth(1);
        LocalDate end = endDate.withDayOfMonth(1);
        
        while (!current.isAfter(end)) {
            cacheService.evictPackMonthlyAvailability(packId, current.getYear(), current.getMonthValue());
            current = current.plusMonths(1);
        }
    }

    /**
     * Invalide le cache de disponibilité d'un matériel pour une période donnée
     */
    private void invalidateMaterielAvailabilityCache(Long materielId, LocalDate startDate, LocalDate endDate) {
        // Invalider le cache pour tous les mois qui chevauchent avec la période
        LocalDate current = startDate.withDayOfMonth(1);
        LocalDate end = endDate.withDayOfMonth(1);
        
        while (!current.isAfter(end)) {
            cacheService.evictMaterielMonthlyAvailability(materielId, current.getYear(), current.getMonthValue());
            current = current.plusMonths(1);
        }
    }

    /**
     * Invalide tout le cache (à utiliser avec précaution)
     */
    public void invalidateAllCache() {
        log.info("🗑️ INVALIDATION CACHE - Invalidation de tout le cache");
        cacheService.evictAllCache();
    }

    /**
     * Invalide le cache de disponibilité (à utiliser avec précaution)
     */
    public void invalidateAllAvailabilityCache() {
        log.info("🗑️ INVALIDATION CACHE - Invalidation de tout le cache de disponibilité");
        cacheService.evictAllAvailabilityCache();
    }
}


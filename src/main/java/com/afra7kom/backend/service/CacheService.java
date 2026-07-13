package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.AvailabilityInfoDto;
import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.PackMateriel;
import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.entity.Categorie;
import com.afra7kom.backend.repository.PackRepository;
import com.afra7kom.backend.repository.MaterielRepository;
import com.afra7kom.backend.repository.PackMaterielRepository;
import com.afra7kom.backend.repository.ReservationRepository;
import com.afra7kom.backend.repository.CategorieRepository;
import com.afra7kom.backend.entity.Reservation.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final PackRepository packRepository;
    private final MaterielRepository materielRepository;
    private final PackMaterielRepository packMaterielRepository;
    private final ReservationRepository reservationRepository;
    private final CategorieRepository categorieRepository;

    // ==============================================
    // CACHE POUR LA DISPONIBILITÉ MENSUELLE
    // ==============================================

    /**
     * Cache la disponibilité mensuelle d'un pack
     * Clé: "pack:{packId}:month:{year}:{month}"
     */
    @Cacheable(value = "availability-month", key = "'pack:' + #packId + ':month:' + #year + ':' + #month")
    public Map<String, AvailabilityInfoDto> getCachedPackMonthlyAvailability(Long packId, int year, int month) {
        log.info("🔍 CACHE MISS - Récupération de la disponibilité mensuelle du pack {} pour {}/{}", packId, month, year);
        // Cette méthode sera appelée par le service qui implémente la logique
        return null; // Sera remplacée par la vraie implémentation
    }

    /**
     * Cache la disponibilité mensuelle d'un matériel
     * Clé: "materiel:{materielId}:month:{year}:{month}"
     */
    @Cacheable(value = "availability-month", key = "'materiel:' + #materielId + ':month:' + #year + ':' + #month")
    public Map<String, AvailabilityInfoDto> getCachedMaterielMonthlyAvailability(Long materielId, int year, int month) {
        log.info("🔍 CACHE MISS - Récupération de la disponibilité mensuelle du matériel {} pour {}/{}", materielId, month, year);
        return null; // Sera remplacée par la vraie implémentation
    }

    /**
     * Invalide le cache de disponibilité mensuelle pour un pack
     */
    @CacheEvict(value = "availability-month", key = "'pack:' + #packId + ':month:' + #year + ':' + #month")
    public void evictPackMonthlyAvailability(Long packId, int year, int month) {
        log.info("🗑️ CACHE EVICT - Invalidation du cache de disponibilité mensuelle du pack {} pour {}/{}", packId, month, year);
    }

    /**
     * Invalide le cache de disponibilité mensuelle pour un matériel
     */
    @CacheEvict(value = "availability-month", key = "'materiel:' + #materielId + ':month:' + #year + ':' + #month")
    public void evictMaterielMonthlyAvailability(Long materielId, int year, int month) {
        log.info("🗑️ CACHE EVICT - Invalidation du cache de disponibilité mensuelle du matériel {} pour {}/{}", materielId, month, year);
    }

    // ==============================================
    // CACHE POUR LA DISPONIBILITÉ QUOTIDIENNE
    // ==============================================

    /**
     * Cache la disponibilité quotidienne d'un pack
     * Clé: "pack:{packId}:date:{date}"
     */
    @Cacheable(value = "availability-daily", key = "'pack:' + #packId + ':date:' + #date")
    public AvailabilityInfoDto getCachedPackDailyAvailability(Long packId, LocalDate date) {
        log.info("🔍 CACHE MISS - Récupération de la disponibilité quotidienne du pack {} pour {}", packId, date);
        return null; // Sera remplacée par la vraie implémentation
    }

    /**
     * Cache la disponibilité quotidienne d'un matériel
     * Clé: "materiel:{materielId}:date:{date}"
     */
    @Cacheable(value = "availability-daily", key = "'materiel:' + #materielId + ':date:' + #date")
    public AvailabilityInfoDto getCachedMaterielDailyAvailability(Long materielId, LocalDate date) {
        log.info("🔍 CACHE MISS - Récupération de la disponibilité quotidienne du matériel {} pour {}", materielId, date);
        return null; // Sera remplacée par la vraie implémentation
    }

    // ==============================================
    // CACHE POUR LES DÉTAILS DES PACKS
    // ==============================================

    /**
     * Cache les détails d'un pack avec ses matériels
     * Clé: "pack:{packId}"
     */
    @Cacheable(value = "pack-details", key = "'pack:' + #packId")
    public Optional<Pack> getCachedPackWithMateriels(Long packId) {
        log.info("🔍 CACHE MISS - Récupération du pack {} avec ses matériels", packId);
        return packRepository.findByIdWithMateriels(packId);
    }

    /**
     * Invalide le cache d'un pack
     */
    @CacheEvict(value = "pack-details", key = "'pack:' + #packId")
    public void evictPackDetails(Long packId) {
        log.info("🗑️ CACHE EVICT - Invalidation du cache du pack {}", packId);
    }

    // ==============================================
    // CACHE POUR LES DÉTAILS DES MATÉRIELS
    // ==============================================

    /**
     * Cache les détails d'un matériel
     * Clé: "materiel:{materielId}"
     */
    @Cacheable(value = "materiel-details", key = "'materiel:' + #materielId")
    public Optional<Materiel> getCachedMateriel(Long materielId) {
        log.info("🔍 CACHE MISS - Récupération du matériel {}", materielId);
        return materielRepository.findById(materielId);
    }

    /**
     * Invalide le cache d'un matériel
     */
    @CacheEvict(value = "materiel-details", key = "'materiel:' + #materielId")
    public void evictMaterielDetails(Long materielId) {
        log.info("🗑️ CACHE EVICT - Invalidation du cache du matériel {}", materielId);
    }

    // ==============================================
    // CACHE POUR LES RELATIONS PACK-MATÉRIEL
    // ==============================================

    /**
     * Cache les relations pack-matériel pour un matériel
     * Clé: "pack-materiels:{materielId}"
     */
    @Cacheable(value = "pack-materiels", key = "'pack-materiels:' + #materielId")
    public List<PackMateriel> getCachedPackMateriels(Long materielId) {
        log.info("🔍 CACHE MISS - Récupération des relations pack-matériel pour le matériel {}", materielId);
        return packMaterielRepository.findByMaterielId(materielId);
    }

    /**
     * Invalide le cache des relations pack-matériel pour un matériel
     */
    @CacheEvict(value = "pack-materiels", key = "'pack-materiels:' + #materielId")
    public void evictPackMateriels(Long materielId) {
        log.info("🗑️ CACHE EVICT - Invalidation du cache des relations pack-matériel pour le matériel {}", materielId);
    }

    // ==============================================
    // CACHE POUR LES RÉSERVATIONS
    // ==============================================

    /**
     * Cache les réservations d'un pack
     * Clé: "reservations:pack:{packId}:status:{status}"
     */
    @Cacheable(value = "reservations", key = "'reservations:pack:' + #packId + ':status:' + #status")
    public List<Reservation> getCachedPackReservations(Long packId, ReservationStatus status) {
        log.info("🔍 CACHE MISS - Récupération des réservations du pack {} avec statut {}", packId, status);
        return reservationRepository.findByPackIdAndStatusOrderByStartDateAsc(packId, status);
    }

    /**
     * Cache les réservations d'un matériel
     * Clé: "reservations:materiel:{materielId}:status:{status}"
     */
    @Cacheable(value = "reservations", key = "'reservations:materiel:' + #materielId + ':status:' + #status")
    public List<Reservation> getCachedMaterielReservations(Long materielId, ReservationStatus status) {
        log.info("🔍 CACHE MISS - Récupération des réservations du matériel {} avec statut {}", materielId, status);
        return reservationRepository.findByMaterielIdAndStatusOrderByStartDateAsc(materielId, status);
    }

    /**
     * Invalide le cache des réservations d'un pack
     */
    @CacheEvict(value = "reservations", key = "'reservations:pack:' + #packId + ':status:' + #status")
    public void evictPackReservations(Long packId, ReservationStatus status) {
        log.info("🗑️ CACHE EVICT - Invalidation du cache des réservations du pack {} avec statut {}", packId, status);
    }

    /**
     * Invalide le cache des réservations d'un matériel
     */
    @CacheEvict(value = "reservations", key = "'reservations:materiel:' + #materielId + ':status:' + #status")
    public void evictMaterielReservations(Long materielId, ReservationStatus status) {
        log.info("🗑️ CACHE EVICT - Invalidation du cache des réservations du matériel {} avec statut {}", materielId, status);
    }

    // ==============================================
    // CACHE POUR LES CATÉGORIES
    // ==============================================

    /**
     * Cache toutes les catégories actives
     * Clé: "categories:active"
     */
    @Cacheable(value = "categories", key = "'categories:active'")
    public List<Categorie> getCachedActiveCategories() {
        log.info("🔍 CACHE MISS - Récupération des catégories actives");
        return categorieRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    /**
     * Invalide le cache des catégories
     */
    @CacheEvict(value = "categories", key = "'categories:active'")
    public void evictActiveCategories() {
        log.info("🗑️ CACHE EVICT - Invalidation du cache des catégories actives");
    }

    // ==============================================
    // MÉTHODES D'INVALIDATION GLOBALES
    // ==============================================

    /**
     * Invalide tout le cache de disponibilité
     */
    @CacheEvict(value = {"availability-month", "availability-daily"}, allEntries = true)
    public void evictAllAvailabilityCache() {
        log.info("🗑️ CACHE EVICT - Invalidation de tout le cache de disponibilité");
    }

    /**
     * Invalide tout le cache des réservations
     */
    @CacheEvict(value = "reservations", allEntries = true)
    public void evictAllReservationsCache() {
        log.info("🗑️ CACHE EVICT - Invalidation de tout le cache des réservations");
    }

    /**
     * Invalide tout le cache
     */
    @CacheEvict(allEntries = true)
    public void evictAllCache() {
        log.info("🗑️ CACHE EVICT - Invalidation de tout le cache");
    }

    // ==============================================
    // MÉTHODES DE MISE À JOUR DU CACHE
    // ==============================================

    /**
     * Met à jour le cache de disponibilité mensuelle d'un pack
     */
    @CachePut(value = "availability-month", key = "'pack:' + #packId + ':month:' + #year + ':' + #month")
    public Map<String, AvailabilityInfoDto> updatePackMonthlyAvailabilityCache(Long packId, int year, int month, Map<String, AvailabilityInfoDto> data) {
        log.info("🔄 CACHE PUT - Mise à jour du cache de disponibilité mensuelle du pack {} pour {}/{}", packId, month, year);
        return data;
    }

    /**
     * Met à jour le cache de disponibilité mensuelle d'un matériel
     */
    @CachePut(value = "availability-month", key = "'materiel:' + #materielId + ':month:' + #year + ':' + #month")
    public Map<String, AvailabilityInfoDto> updateMaterielMonthlyAvailabilityCache(Long materielId, int year, int month, Map<String, AvailabilityInfoDto> data) {
        log.info("🔄 CACHE PUT - Mise à jour du cache de disponibilité mensuelle du matériel {} pour {}/{}", materielId, month, year);
        return data;
    }
}


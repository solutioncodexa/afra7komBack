package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.AvailabilityInfoDto;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.PackType;
import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.entity.Reservation.ReservationStatus;
import com.afra7kom.backend.entity.PackMateriel;
import com.afra7kom.backend.repository.PackRepository;
import com.afra7kom.backend.repository.MaterielRepository;
import com.afra7kom.backend.repository.ReservationRepository;
import com.afra7kom.backend.repository.PackMaterielRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CachedAvailabilityService {

    private final PackRepository packRepository;
    private final MaterielRepository materielRepository;
    private final ReservationRepository reservationRepository;
    private final PackMaterielRepository packMaterielRepository;
    private final CacheService cacheService;

    /**
     * Version avec cache de la vérification de disponibilité mensuelle
     * Utilise le pattern Cache-Aside pour optimiser les performances
     */
    @Cacheable(value = "availability-month", key = "'pack:' + #packId + ':month:' + #year + ':' + #month")
    public Map<String, AvailabilityInfoDto> getCachedPackMonthlyAvailability(Long packId, int year, int month) {
        log.info("🚀 CACHE MISS - Calcul de la disponibilité mensuelle du pack {} pour {}/{}", packId, month, year);
        
        Map<String, AvailabilityInfoDto> monthlyAvailability = new HashMap<>();
        
        // Calculer le premier et dernier jour du mois
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());
        
        // 1. Récupérer le pack avec ses matériels (avec cache)
        Pack pack = cacheService.getCachedPackWithMateriels(packId)
                .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));

        log.info("Pack trouvé: {} (ID: {}), Type: {}", pack.getName(), pack.getId(), pack.getType());

        // RÈGLE: PACK et CADEAU sont toujours disponibles
        if (pack.getType() == PackType.PACK || pack.getType() == PackType.CADEAU || pack.getType() == PackType.GATEAU) {
            log.info("✅ Type {} - TOUJOURS DISPONIBLE (pas de gestion de stock)", pack.getType());
            
            // Remplir tout le mois avec "disponible"
            LocalDate currentDate = firstDayOfMonth;
            while (!currentDate.isAfter(lastDayOfMonth)) {
                monthlyAvailability.put(currentDate.toString(), AvailabilityInfoDto.builder()
                        .packId(packId)
                        .totalQuantity(999)
                        .availableQuantity(999)
                        .reservedQuantity(0)
                        .isAvailable(true)
                        .message("Toujours disponible")
                        .build());
                currentDate = currentDate.plusDays(1);
            }
            return monthlyAvailability;
        }

        // 2. Récupérer toutes les réservations confirmées pour ce pack (avec cache)
        List<Reservation> reservations = cacheService.getCachedPackReservations(packId, ReservationStatus.CONFIRMEE);
        
        // Filtrer les réservations qui chevauchent avec la période demandée
        List<Reservation> overlappingReservations = reservations.stream()
                .filter(r -> isReservationOverlapping(r, firstDayOfMonth, lastDayOfMonth))
                .collect(Collectors.toList());
        
        log.info("📊 Réservations trouvées: {} total, {} qui chevauchent la période", 
                reservations.size(), overlappingReservations.size());

        // 3. Calculer la disponibilité pour chaque jour du mois
        LocalDate currentDate = firstDayOfMonth;
        while (!currentDate.isAfter(lastDayOfMonth)) {
            String dateKey = currentDate.toString();
            
            // Calculer la disponibilité pour ce jour spécifique
            AvailabilityInfoDto availability = calculatePackAvailabilityForDate(pack, currentDate, overlappingReservations);
            monthlyAvailability.put(dateKey, availability);
            
            currentDate = currentDate.plusDays(1);
        }
        
        log.info("✅ Disponibilité mensuelle calculée et mise en cache pour {} jours", monthlyAvailability.size());
        return monthlyAvailability;
    }

    /**
     * Version avec cache pour les matériels
     */
    @Cacheable(value = "availability-month", key = "'materiel:' + #materielId + ':month:' + #year + ':' + #month")
    public Map<String, AvailabilityInfoDto> getCachedMaterielMonthlyAvailability(Long materielId, int year, int month) {
        log.info("🚀 CACHE MISS - Calcul de la disponibilité mensuelle du matériel {} pour {}/{}", materielId, month, year);
        
        Map<String, AvailabilityInfoDto> monthlyAvailability = new HashMap<>();
        
        // Calculer le premier et dernier jour du mois
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());
        
        // 1. Récupérer le matériel (avec cache)
        Materiel materiel = cacheService.getCachedMateriel(materielId)
                .orElseThrow(() -> new RuntimeException("Matériel non trouvé avec l'ID: " + materielId));

        // 2. Récupérer toutes les réservations confirmées pour ce matériel (avec cache)
        List<Reservation> materielReservations = cacheService.getCachedMaterielReservations(materielId, ReservationStatus.CONFIRMEE);
        
        // 3. Récupérer toutes les réservations de packs qui contiennent ce matériel (avec cache)
        List<PackMateriel> packMateriels = cacheService.getCachedPackMateriels(materielId);
        List<Reservation> packReservations = new ArrayList<>();
        
        for (PackMateriel packMateriel : packMateriels) {
            if (!packMateriel.getIsOptional()) {
                Long packId = packMateriel.getPack().getId();
                List<Reservation> packRes = cacheService.getCachedPackReservations(packId, ReservationStatus.CONFIRMEE);
                packReservations.addAll(packRes);
            }
        }
        
        // Filtrer les réservations qui chevauchent avec la période demandée
        List<Reservation> overlappingMaterielReservations = materielReservations.stream()
                .filter(r -> isReservationOverlapping(r, firstDayOfMonth, lastDayOfMonth))
                .collect(Collectors.toList());
        
        List<Reservation> overlappingPackReservations = packReservations.stream()
                .filter(r -> isReservationOverlapping(r, firstDayOfMonth, lastDayOfMonth))
                .collect(Collectors.toList());
        
        log.info("📊 Réservations matériel: {} directes, {} via packs", 
                overlappingMaterielReservations.size(), overlappingPackReservations.size());

        // 4. Calculer la disponibilité pour chaque jour du mois
        LocalDate currentDate = firstDayOfMonth;
        while (!currentDate.isAfter(lastDayOfMonth)) {
            String dateKey = currentDate.toString();
            
            // Calculer la disponibilité pour ce jour spécifique
            AvailabilityInfoDto availability = calculateMaterielAvailabilityForDate(
                    materiel, currentDate, overlappingMaterielReservations, overlappingPackReservations, packMateriels);
            monthlyAvailability.put(dateKey, availability);
            
            currentDate = currentDate.plusDays(1);
        }
        
        log.info("✅ Disponibilité mensuelle du matériel calculée et mise en cache pour {} jours", monthlyAvailability.size());
        return monthlyAvailability;
    }

    /**
     * Version avec cache pour la disponibilité quotidienne d'un pack
     */
    @Cacheable(value = "availability-daily", key = "'pack:' + #packId + ':date:' + #date")
    public AvailabilityInfoDto getCachedPackDailyAvailability(Long packId, LocalDate date) {
        log.info("🚀 CACHE MISS - Calcul de la disponibilité quotidienne du pack {} pour {}", packId, date);
        
        // Récupérer le pack avec ses matériels (avec cache)
        Pack pack = cacheService.getCachedPackWithMateriels(packId)
                .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));

        // RÈGLE: PACK et CADEAU sont toujours disponibles
        if (pack.getType() == PackType.PACK || pack.getType() == PackType.CADEAU || pack.getType() == PackType.GATEAU) {
            return AvailabilityInfoDto.builder()
                    .packId(packId)
                    .totalQuantity(999)
                    .availableQuantity(999)
                    .reservedQuantity(0)
                    .isAvailable(true)
                    .message("Toujours disponible")
                    .build();
        }

        // Récupérer les réservations (avec cache)
        List<Reservation> reservations = cacheService.getCachedPackReservations(packId, ReservationStatus.CONFIRMEE);
        
        // Calculer la disponibilité pour cette date
        return calculatePackAvailabilityForDate(pack, date, reservations);
    }

    /**
     * Version avec cache pour la disponibilité quotidienne d'un matériel
     */
    @Cacheable(value = "availability-daily", key = "'materiel:' + #materielId + ':date:' + #date")
    public AvailabilityInfoDto getCachedMaterielDailyAvailability(Long materielId, LocalDate date) {
        log.info("🚀 CACHE MISS - Calcul de la disponibilité quotidienne du matériel {} pour {}", materielId, date);
        
        // Récupérer le matériel (avec cache)
        Materiel materiel = cacheService.getCachedMateriel(materielId)
                .orElseThrow(() -> new RuntimeException("Matériel non trouvé avec l'ID: " + materielId));

        // Récupérer les réservations (avec cache)
        List<Reservation> materielReservations = cacheService.getCachedMaterielReservations(materielId, ReservationStatus.CONFIRMEE);
        
        // Récupérer les relations pack-matériel (avec cache)
        List<PackMateriel> packMateriels = cacheService.getCachedPackMateriels(materielId);
        List<Reservation> packReservations = new ArrayList<>();
        
        for (PackMateriel packMateriel : packMateriels) {
            if (!packMateriel.getIsOptional()) {
                Long packId = packMateriel.getPack().getId();
                List<Reservation> packRes = cacheService.getCachedPackReservations(packId, ReservationStatus.CONFIRMEE);
                packReservations.addAll(packRes);
            }
        }
        
        // Calculer la disponibilité pour cette date
        return calculateMaterielAvailabilityForDate(materiel, date, materielReservations, packReservations, packMateriels);
    }

    // ==============================================
    // MÉTHODES UTILITAIRES (identiques à OptimizedAvailabilityService)
    // ==============================================

    /**
     * Calcule la disponibilité d'un pack pour une date spécifique
     */
    private AvailabilityInfoDto calculatePackAvailabilityForDate(Pack pack, LocalDate date, List<Reservation> reservations) {
        // Calculer la quantité réservée pour cette date
        int reservedQuantity = reservations.stream()
                .filter(r -> isReservationActiveOnDate(r, date))
                .mapToInt(Reservation::getQuantity)
                .sum();
        
        // Calculer la quantité totale disponible
        int totalQuantity = calculatePackTotalQuantity(pack);
        int availableQuantity = Math.max(0, totalQuantity - reservedQuantity);
        
        return AvailabilityInfoDto.builder()
                .packId(pack.getId())
                .totalQuantity(totalQuantity)
                .availableQuantity(availableQuantity)
                .reservedQuantity(reservedQuantity)
                .isAvailable(availableQuantity > 0)
                .message(generateAvailabilityMessage(availableQuantity, totalQuantity))
                .build();
    }

    /**
     * Calcule la disponibilité d'un matériel pour une date spécifique
     */
    private AvailabilityInfoDto calculateMaterielAvailabilityForDate(Materiel materiel, LocalDate date, 
            List<Reservation> materielReservations, List<Reservation> packReservations, List<PackMateriel> packMateriels) {
        
        // Calculer la quantité réservée directement
        int directReserved = materielReservations.stream()
                .filter(r -> isReservationActiveOnDate(r, date))
                .mapToInt(Reservation::getQuantity)
                .sum();
        
        // Calculer la quantité réservée via les packs
        int packReserved = 0;
        for (Reservation packReservation : packReservations) {
            if (isReservationActiveOnDate(packReservation, date)) {
                // Trouver la quantité de ce matériel dans ce pack
                for (PackMateriel packMateriel : packMateriels) {
                    if (packMateriel.getPack().getId().equals(packReservation.getPack().getId()) && 
                        !packMateriel.getIsOptional()) {
                        packReserved += packMateriel.getQuantity() * packReservation.getQuantity();
                        break;
                    }
                }
            }
        }
        
        int totalReserved = directReserved + packReserved;
        int totalQuantity = materiel.getTotalQuantity() != null ? materiel.getTotalQuantity() : 0;
        int availableQuantity = Math.max(0, totalQuantity - totalReserved);
        
        return AvailabilityInfoDto.builder()
                .materielId(materiel.getId())
                .totalQuantity(totalQuantity)
                .availableQuantity(availableQuantity)
                .reservedQuantity(totalReserved)
                .isAvailable(availableQuantity > 0)
                .message(generateAvailabilityMessage(availableQuantity, totalQuantity))
                .build();
    }

    /**
     * Vérifie si une réservation est active à une date donnée
     */
    private boolean isReservationActiveOnDate(Reservation reservation, LocalDate date) {
        LocalDate reservationStart = reservation.getStartDate();
        LocalDate reservationEnd = reservation.getEndDate();
        
        // Le pack/matériel est indisponible du startDate au endDate-1
        // Mais disponible le jour de fin de réservation (endDate inclus)
        LocalDate availabilityEndDate = reservationEnd.minusDays(1);
        
        return !date.isBefore(reservationStart) && !date.isAfter(availabilityEndDate);
    }

    /**
     * Vérifie si une réservation chevauche la période demandée
     */
    private boolean isReservationOverlapping(Reservation reservation, LocalDate startDate, LocalDate endDate) {
        LocalDate reservationStart = reservation.getStartDate();
        LocalDate reservationEnd = reservation.getEndDate();
        
        // Si la réservation se termine avant la période demandée, pas de conflit
        if (reservationEnd.isBefore(startDate)) {
            return false;
        }
        
        // Si la réservation commence après la période demandée, pas de conflit
        if (reservationStart.isAfter(endDate)) {
            return false;
        }
        
        // Il y a chevauchement
        return true;
    }

    /**
     * Calcule la quantité totale disponible d'un pack basée sur ses matériels
     */
    private int calculatePackTotalQuantity(Pack pack) {
        if (pack.getPackMateriels() == null || pack.getPackMateriels().isEmpty()) {
            return 0;
        }

        // Calculer le nombre maximum de packs disponibles basé sur le matériel le plus limitant
        int maxPacksAvailable = Integer.MAX_VALUE;

        for (var packMateriel : pack.getPackMateriels()) {
            if (!packMateriel.getIsOptional()) { // Seulement les matériels obligatoires
                Materiel materiel = packMateriel.getMateriel();
                int requiredQuantity = packMateriel.getQuantity();
                int totalQuantity = materiel.getTotalQuantity() != null ? materiel.getTotalQuantity() : 0;
                
                // Calculer combien de packs peuvent être formés avec ce matériel
                int packsPossibleWithThisMateriel = totalQuantity / requiredQuantity;
                
                // Le pack est limité par le matériel le plus restrictif
                maxPacksAvailable = Math.min(maxPacksAvailable, packsPossibleWithThisMateriel);
            }
        }

        // Si aucun matériel obligatoire, retourner 0
        if (maxPacksAvailable == Integer.MAX_VALUE) {
            return 0;
        }

        return Math.max(0, maxPacksAvailable);
    }

    /**
     * Génère un message de disponibilité
     */
    private String generateAvailabilityMessage(int availableQuantity, int totalQuantity) {
        if (availableQuantity == 0) {
            return "Rupture de stock - Aucune unité disponible";
        } else if (availableQuantity <= 2) {
            return "Stock faible - Seulement " + availableQuantity + " unité(s) disponible(s)";
        } else {
            return availableQuantity + " unité(s) disponible(s) sur " + totalQuantity;
        }
    }
}


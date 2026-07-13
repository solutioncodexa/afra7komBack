package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.AvailabilityInfoDto;
import com.afra7kom.backend.entity.MouvementStock;
import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.PackType;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.entity.Reservation.ReservationStatus;
import com.afra7kom.backend.repository.PackRepository;
import com.afra7kom.backend.repository.MaterielRepository;
import com.afra7kom.backend.repository.ReservationRepository;
import com.afra7kom.backend.repository.PackMaterielRepository;
import com.afra7kom.backend.entity.PackMateriel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {

    private final PackRepository packRepository;
    private final MaterielRepository materielRepository;
    private final ReservationRepository reservationRepository;
    private final PackMaterielRepository packMaterielRepository;

    /**
     * Calcule la disponibilité quotidienne d'un pack pour une période donnée
     * Workflow de vérification stock de pack journalier :
     * 1. Vérifier chaque matériel du pack pour la date spécifique
     * 2. Calculer nombre max de packs possible pour chaque matériel (stock_disponible / quantité_requise)
     * 3. Prendre le minimum (matériel le plus restrictif)
     */
    private int calculatePackDailyAvailability(Pack pack, LocalDate startDate, LocalDate endDate) {
        log.info("=== WORKFLOW VÉRIFICATION STOCK PACK JOURNALIER ===");
        log.info("Pack ID: {}, Nom: {}", pack.getId(), pack.getName());
        log.info("Pack actif: {}", pack.getActive());
        log.info("Période: {} au {}", startDate, endDate);
        
        if (pack.getPackMateriels() == null || pack.getPackMateriels().isEmpty()) {
            log.warn("❌ Pack {} n'a aucun matériel associé", pack.getName());
            return 0;
        }

        log.info("Nombre de matériels dans le pack: {}", pack.getPackMateriels().size());
        
        // Workflow de vérification : trouver le matériel le plus restrictif
        int maxPacksAvailable = Integer.MAX_VALUE;
        String limitingMaterial = null;
        int materialCount = 0;

        for (var packMateriel : pack.getPackMateriels()) {
            materialCount++;
            log.info("--- Étape {}: Vérification Matériel {} ---", materialCount, packMateriel.getMateriel().getName());
            log.info("Matériel obligatoire: {}", !packMateriel.getIsOptional());
            
            if (!packMateriel.getIsOptional()) { // Seulement les matériels obligatoires
                Materiel materiel = packMateriel.getMateriel();
                int requiredQuantity = packMateriel.getQuantity();
                int totalQuantity = materiel.getTotalQuantity() != null ? materiel.getTotalQuantity() : 0;
                
                // Calculer les réservations actives pour ce matériel pendant cette période spécifique
                int reservedForPeriod = calculateMaterielReservedQuantity(materiel.getId(), startDate, endDate);
                int availableForPeriod = totalQuantity - reservedForPeriod;
                
                log.info("📊 Données matériel {}:", materiel.getName());
                log.info("   - Stock total: {}", totalQuantity);
                log.info("   - Réservé pour la période: {}", reservedForPeriod);
                log.info("   - Disponible pour la période: {}", availableForPeriod);
                log.info("   - Quantité requise par pack: {}", requiredQuantity);
                
                // Calculer nombre max de packs possible avec ce matériel
                int packsPossibleWithThisMateriel = availableForPeriod / requiredQuantity;
                
                log.info("🧮 Calcul: {} ÷ {} = {} packs possibles", 
                    availableForPeriod, requiredQuantity, packsPossibleWithThisMateriel);
                
                // Le pack est limité par le matériel le plus restrictif
                if (packsPossibleWithThisMateriel < maxPacksAvailable) {
                    maxPacksAvailable = packsPossibleWithThisMateriel;
                    limitingMaterial = materiel.getName();
                    log.info("⚠️  NOUVEAU LIMITANT: {} packs (limité par {})", 
                        maxPacksAvailable, limitingMaterial);
                } else {
                    log.info("✅ Non limitant: {} packs (limite actuelle: {})", 
                        packsPossibleWithThisMateriel, maxPacksAvailable);
                }
            } else {
                log.info("⏭️ Matériel optionnel ignoré: {}", packMateriel.getMateriel().getName());
            }
        }

        // Si aucun matériel obligatoire, retourner 0
        if (maxPacksAvailable == Integer.MAX_VALUE) {
            log.warn("❌ Aucun matériel obligatoire trouvé pour le pack {}", pack.getName());
            return 0;
        }

        log.info("🎯 RÉSULTAT FINAL:");
        log.info("   - Nombre max de packs disponibles: {}", maxPacksAvailable);
        log.info("   - Matériel limitant: {}", limitingMaterial);
        log.info("   - Date vérifiée: {}", startDate);
        log.info("=== FIN WORKFLOW VÉRIFICATION STOCK PACK JOURNALIER ===");
        
        return Math.max(0, maxPacksAvailable);
    }
    

    /**
     * Vérifie la disponibilité d'un pack pour une période donnée
     * Calcule la disponibilité jour par jour pour une location journalière
     * 
     * RÈGLES SELON TYPE:
     * - PACK et CADEAU: Toujours disponibles (pas de gestion de stock)
     * - BUFFET, PACK_BUFFET, MATERIEL: Location journalière avec calcul de disponibilité
     */
    public AvailabilityInfoDto checkPackAvailability(Long packId, LocalDate startDate, LocalDate endDate) {
        log.info("🔍 DÉBUT VÉRIFICATION DISPONIBILITÉ PACK");
        log.info("Pack ID: {}, Période: {} au {}", packId, startDate, endDate);
        
        Pack pack = packRepository.findByIdWithMateriels(packId)
                .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));

        log.info("Pack trouvé: {} (ID: {}), Type: {}", pack.getName(), pack.getId(), pack.getType());

        // RÈGLE: PACK et CADEAU sont toujours disponibles
        if (pack.getType() == PackType.PACK || pack.getType() == PackType.CADEAU || pack.getType() == PackType.GATEAU) {
            log.info("✅ Type {} - TOUJOURS DISPONIBLE (pas de gestion de stock)", pack.getType());
            return AvailabilityInfoDto.builder()
                    .packId(packId)
                    .totalQuantity(999) // Illimité
                    .availableQuantity(999) // Toujours disponible
                    .reservedQuantity(0)
                    .isAvailable(true)
                    .message("Toujours disponible")
                    .build();
        }

        // Si pas de dates spécifiées, vérifier la disponibilité générale
        if (startDate == null || endDate == null) {
            log.info("Pas de dates spécifiées, vérification générale");
            return checkPackGeneralAvailability(packId);
        }

        // Pour les locations journalières (BUFFET, PACK_BUFFET, MATERIEL), calculer la disponibilité
        log.info("📅 Type {} - LOCATION JOURNALIÈRE (calcul de disponibilité)", pack.getType());
        log.info("Calcul de la disponibilité quotidienne...");
        int availableQuantity = calculatePackDailyAvailability(pack, startDate, endDate);
        log.info("Disponibilité quotidienne calculée: {}", availableQuantity);
        
        // Calculer la quantité totale basée sur les matériels du pack
        log.info("Calcul de la quantité totale...");
        int totalQuantity = calculatePackTotalQuantity(pack);
        log.info("Quantité totale: {}", totalQuantity);
        
        // Calculer la quantité réservée pour la période (pour information)
        log.info("Calcul de la quantité réservée...");
        int reservedQuantity = calculatePackReservedQuantity(packId, startDate, endDate);
        log.info("Quantité réservée: {}", reservedQuantity);
        
        log.info("🔢 CALCUL DÉTAILLÉ:");
        log.info("   - availableQuantity (calculée par date): {}", availableQuantity);
        log.info("   - totalQuantity: {}", totalQuantity);
        log.info("   - reservedQuantity: {}", reservedQuantity);
        log.info("Quantité disponible finale: {}", availableQuantity);

        String message = generateAvailabilityMessage(availableQuantity, totalQuantity);
        boolean isAvailable = availableQuantity > 0;
        
        log.info("📊 RÉSULTAT FINAL:");
        log.info("   - Disponible: {}", isAvailable);
        log.info("   - Quantité disponible: {}", availableQuantity);
        log.info("   - Quantité totale: {}", totalQuantity);
        log.info("   - Message: {}", message);
        log.info("🔍 FIN VÉRIFICATION DISPONIBILITÉ PACK");

        return AvailabilityInfoDto.builder()
                .packId(packId)
                .totalQuantity(totalQuantity)
                .availableQuantity(availableQuantity)
                .reservedQuantity(reservedQuantity)
                .isAvailable(isAvailable)
                .message(message)
                .nextAvailableDate(findNextAvailableDate(packId, startDate, endDate))
                .build();
    }

    /**
     * Vérifie la disponibilité d'un matériel pour une période donnée
     */
    public AvailabilityInfoDto checkMaterielAvailability(Long materielId, LocalDate startDate, LocalDate endDate) {
        log.info("Vérification de disponibilité du matériel {} du {} au {}", materielId, startDate, endDate);
        
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new RuntimeException("Matériel non trouvé avec l'ID: " + materielId));

        // Si pas de dates spécifiées, vérifier la disponibilité générale
        if (startDate == null || endDate == null) {
            return checkMaterielGeneralAvailability(materielId);
        }

        // Calculer la quantité réservée pour la période
        int reservedQuantity = calculateMaterielReservedQuantity(materielId, startDate, endDate);
        int totalQuantity = materiel.getTotalQuantity() != null ? materiel.getTotalQuantity() : 0;
        int availableQuantity = Math.max(0, totalQuantity - reservedQuantity);

        return AvailabilityInfoDto.builder()
                .materielId(materielId)
                .totalQuantity(totalQuantity)
                .availableQuantity(availableQuantity)
                .reservedQuantity(reservedQuantity)
                .isAvailable(availableQuantity > 0)
                .message(generateAvailabilityMessage(availableQuantity, totalQuantity))
                .nextAvailableDate(findNextAvailableDate(materielId, startDate, endDate))
                .build();
    }

    /**
     * Vérifie la disponibilité générale d'un pack ou matériel
     */
    public AvailabilityInfoDto checkGeneralAvailability(Long packId, Long materielId) {
        if (packId != null) {
            return checkPackGeneralAvailability(packId);
        } else if (materielId != null) {
            return checkMaterielGeneralAvailability(materielId);
        }
        throw new RuntimeException("packId ou materielId doit être fourni");
    }

    /**
     * Vérifie la disponibilité générale d'un pack
     * Pour les locations journalières, retourne la capacité totale du pack
     */
    private AvailabilityInfoDto checkPackGeneralAvailability(Long packId) {
        Pack pack = packRepository.findByIdWithMateriels(packId)
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

        // Pour les locations journalières, la disponibilité générale est basée sur le stock total
        // car les matériels sont rendus après chaque location
        int totalQuantity = calculatePackTotalQuantity(pack);
        int availableQuantity = totalQuantity; // Disponible = total pour les locations journalières
        int reservedQuantity = 0; // Pas de concept de "réservé" en général pour les locations journalières

        return AvailabilityInfoDto.builder()
                .packId(packId)
                .totalQuantity(totalQuantity)
                .availableQuantity(availableQuantity)
                .reservedQuantity(reservedQuantity)
                .isAvailable(availableQuantity > 0)
                .message(generateAvailabilityMessage(availableQuantity, totalQuantity))
                .build();
    }

    /**
     * Vérifie la disponibilité générale d'un matériel
     */
    private AvailabilityInfoDto checkMaterielGeneralAvailability(Long materielId) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new RuntimeException("Matériel non trouvé avec l'ID: " + materielId));

        int totalQuantity = materiel.getTotalQuantity() != null ? materiel.getTotalQuantity() : 0;
        int reservedQuantity = calculateMaterielCurrentReservedQuantity(materielId);
        int availableQuantity = Math.max(0, totalQuantity - reservedQuantity);

        return AvailabilityInfoDto.builder()
                .materielId(materielId)
                .totalQuantity(totalQuantity)
                .availableQuantity(availableQuantity)
                .reservedQuantity(reservedQuantity)
                .isAvailable(availableQuantity > 0)
                .message(generateAvailabilityMessage(availableQuantity, totalQuantity))
                .build();
    }

    /**
     * Calcule la quantité réservée d'un pack pour une période donnée
     */
    private int calculatePackReservedQuantity(Long packId, LocalDate startDate, LocalDate endDate) {
        List<Reservation> reservations = reservationRepository.findByPackIdAndStatusOrderByStartDateAsc(packId, ReservationStatus.CONFIRMEE);
        
        int totalReserved = reservations.stream()
                .filter(r -> isReservationOverlapping(r, startDate, endDate))
                .mapToInt(Reservation::getQuantity)
                .sum();
        
        log.debug("Total packs réservés pour pack {} du {} au {}: {}", 
            packId, startDate, endDate, totalReserved);
        
        return totalReserved;
    }

    /**
     * Calcule la quantité réservée d'un matériel pour une période donnée
     * Cette méthode compte les réservations confirmées qui chevauchent la période demandée
     * INCLUT les réservations de packs qui contiennent ce matériel
     */
    private int calculateMaterielReservedQuantity(Long materielId, LocalDate startDate, LocalDate endDate) {
        int totalReserved = 0;
        
        // 1. Compter les réservations de matériels individuels
        List<Reservation> materielReservations = reservationRepository.findByMaterielIdAndStatusOrderByStartDateAsc(materielId, ReservationStatus.CONFIRMEE);
        
        for (Reservation reservation : materielReservations) {
            if (isReservationOverlapping(reservation, startDate, endDate)) {
                totalReserved += reservation.getQuantity();
                log.debug("Réservation matériel individuel {}: {} unités réservées", 
                    reservation.getMateriel().getName(), reservation.getQuantity());
            }
        }
        
        // 2. Compter les réservations de packs qui contiennent ce matériel
        // Obtenir tous les packs qui contiennent ce matériel
        List<PackMateriel> packMateriels = packMaterielRepository.findByMaterielId(materielId);
        
        for (PackMateriel packMateriel : packMateriels) {
            if (!packMateriel.getIsOptional()) { // Seulement les matériels obligatoires
                Long packId = packMateriel.getPack().getId();
                int quantityInPack = packMateriel.getQuantity();
                
                // Obtenir toutes les réservations confirmées de ce pack
                List<Reservation> packReservations = reservationRepository.findByPackIdAndStatusOrderByStartDateAsc(packId, ReservationStatus.CONFIRMEE);
                
                for (Reservation reservation : packReservations) {
                    if (isReservationOverlapping(reservation, startDate, endDate)) {
                        int totalMaterialNeeded = quantityInPack * reservation.getQuantity();
                        totalReserved += totalMaterialNeeded;
                        
                        log.debug("Réservation pack {}: {} packs × {} matériel = {} total réservé", 
                            reservation.getPack().getName(), reservation.getQuantity(), quantityInPack, totalMaterialNeeded);
                    }
                }
            }
        }
        
        log.info("🔍 CALCUL RÉSERVATIONS MATÉRIEL {} du {} au {}: {} total réservé", 
            materielId, startDate, endDate, totalReserved);
        
        return totalReserved;
    }

    /**
     * Obtient la quantité d'un matériel spécifique dans un pack
     */
    private int getMaterialQuantityInPack(Long packId, Long materielId) {
        Pack pack = packRepository.findByIdWithMateriels(packId)
                .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));
        
        if (pack.getPackMateriels() != null) {
            for (var packMateriel : pack.getPackMateriels()) {
                if (packMateriel.getMateriel().getId().equals(materielId) && !packMateriel.getIsOptional()) {
                    return packMateriel.getQuantity();
                }
            }
        }
        
        return 0; // Matériel non trouvé dans le pack ou optionnel
    }

    /**
     * Calcule les réservations actuelles d'un pack
     */
    private int calculatePackCurrentReservations(Long packId) {
        List<Reservation> reservations = reservationRepository.findByPackIdAndStatusOrderByStartDateAsc(packId, ReservationStatus.CONFIRMEE);
        LocalDate today = LocalDate.now();
        
        return reservations.stream()
                .filter(r -> isReservationActive(r, today))
                .mapToInt(Reservation::getQuantity)
                .sum();
    }

    /**
     * Calcule les réservations actuelles d'un matériel
     */
    private int calculateMaterielCurrentReservedQuantity(Long materielId) {
        List<Reservation> reservations = reservationRepository.findByMaterielIdAndStatusOrderByStartDateAsc(materielId, ReservationStatus.CONFIRMEE);
        LocalDate today = LocalDate.now();
        
        return reservations.stream()
                .filter(r -> isReservationActive(r, today))
                .mapToInt(Reservation::getQuantity)
                .sum();
    }

    /**
     * Vérifie si une réservation chevauche la période demandée
     * Pour les locations journalières, le pack est indisponible pendant toute la période de réservation
     */
    private boolean isReservationOverlapping(Reservation reservation, LocalDate startDate, LocalDate endDate) {
        LocalDate reservationStart = reservation.getStartDate();
        LocalDate reservationEnd = reservation.getEndDate();
        
        log.debug("🔍 Vérification chevauchement: Réservation {} au {}, Période demandée {} au {}", 
            reservationStart, reservationEnd, startDate, endDate);
        
        // Pour les locations journalières, le pack est indisponible pendant toute la période de réservation
        // Vérifier si les périodes se chevauchent
        
        // Si la réservation se termine avant la période demandée, pas de conflit
        if (reservationEnd.isBefore(startDate)) {
            log.debug("❌ Pas de conflit: Réservation se termine avant la période demandée");
            return false;
        }
        
        // Si la réservation commence après la période demandée, pas de conflit
        if (reservationStart.isAfter(endDate)) {
            log.debug("❌ Pas de conflit: Réservation commence après la période demandée");
            return false;
        }
        
        // Il y a chevauchement
        log.debug("✅ Conflit détecté: Les périodes se chevauchent");
        return true;
    }

    /**
     * Vérifie si une réservation est active à une date donnée
     * Règle de disponibilité : un pack/matériel est disponible le jour de fin de réservation inclus
     */
    private boolean isReservationActive(Reservation reservation, LocalDate date) {
        LocalDate reservationStart = reservation.getStartDate();
        LocalDate reservationEnd = reservation.getEndDate();
        
        // Le pack/matériel est indisponible du startDate au endDate-1
        // Mais disponible le jour de fin de réservation (endDate inclus)
        LocalDate availabilityEndDate = reservationEnd.minusDays(1);
        
        return !date.isBefore(reservationStart) && !date.isAfter(availabilityEndDate);
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

    /**
     * Calcule la quantité totale disponible d'un pack basée sur ses matériels
     * Pour les locations journalières, utilise le stock total des matériels
     */
    private int calculatePackTotalQuantity(Pack pack) {
        log.info("Calcul de disponibilité pour le pack: {}", pack.getName());
        
        if (pack.getPackMateriels() == null || pack.getPackMateriels().isEmpty()) {
            log.warn("Pack {} n'a aucun matériel associé", pack.getName());
            return 0;
        }

        // Calculer le nombre maximum de packs disponibles basé sur le matériel le plus limitant
        // Utilise le stock total pour les locations journalières
        int maxPacksAvailable = Integer.MAX_VALUE;

        for (var packMateriel : pack.getPackMateriels()) {
            if (!packMateriel.getIsOptional()) { // Seulement les matériels obligatoires
                Materiel materiel = packMateriel.getMateriel();
                int requiredQuantity = packMateriel.getQuantity();
                int totalQuantity = materiel.getTotalQuantity() != null ? materiel.getTotalQuantity() : 0;
                
                log.info("Matériel: {} - Requis: {} - Stock total: {}", 
                    materiel.getName(), requiredQuantity, totalQuantity);
                
                // Calculer combien de packs peuvent être formés avec ce matériel
                int packsPossibleWithThisMateriel = totalQuantity / requiredQuantity;
                
                log.info("Packs possibles avec {}: {}", materiel.getName(), packsPossibleWithThisMateriel);
                
                // Le pack est limité par le matériel le plus restrictif
                maxPacksAvailable = Math.min(maxPacksAvailable, packsPossibleWithThisMateriel);
            }
        }

        // Si aucun matériel obligatoire, retourner 0
        if (maxPacksAvailable == Integer.MAX_VALUE) {
            log.warn("Aucun matériel obligatoire trouvé pour le pack {}", pack.getName());
            return 0;
        }

        log.info("Nombre total de packs disponibles pour {}: {}", pack.getName(), maxPacksAvailable);
        return Math.max(0, maxPacksAvailable);
    }

    /**
     * Trouve la prochaine date disponible
     */
    private LocalDate findNextAvailableDate(Long packId, LocalDate startDate, LocalDate endDate) {
        // Logique pour trouver la prochaine date disponible
        // Pour l'instant, retourner null
        return null;
    }

    /**
     * Méthode de debug pour tester la disponibilité d'un pack
     */
    public Map<String, Object> debugPackAvailability(Long packId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            log.info("🔧 DEBUG DISPONIBILITÉ PACK - ID: {}, Période: {} au {}", packId, startDate, endDate);
            
            // Appeler la méthode principale
            AvailabilityInfoDto result = checkPackAvailability(packId, startDate, endDate);
            
            debugInfo.put("packId", packId);
            debugInfo.put("startDate", startDate);
            debugInfo.put("endDate", endDate);
            debugInfo.put("isAvailable", result.getIsAvailable());
            debugInfo.put("availableQuantity", result.getAvailableQuantity());
            debugInfo.put("totalQuantity", result.getTotalQuantity());
            debugInfo.put("reservedQuantity", result.getReservedQuantity());
            debugInfo.put("message", result.getMessage());
            
            // Informations détaillées sur le pack
            Pack pack = packRepository.findByIdWithMateriels(packId)
                    .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));
            
            debugInfo.put("packName", pack.getName());
            debugInfo.put("packActive", pack.getActive());
            
            // Informations sur les matériels
            if (pack.getPackMateriels() != null) {
                List<Map<String, Object>> materielsInfo = new java.util.ArrayList<>();
                
                for (var packMateriel : pack.getPackMateriels()) {
                    Map<String, Object> materielInfo = new HashMap<>();
                    Materiel materiel = packMateriel.getMateriel();
                    
                    materielInfo.put("materielId", materiel.getId());
                    materielInfo.put("materielName", materiel.getName());
                    materielInfo.put("requiredQuantity", packMateriel.getQuantity());
                    materielInfo.put("availableQuantity", materiel.getAvailableQuantity());
                    materielInfo.put("totalQuantity", materiel.getTotalQuantity());
                    materielInfo.put("isOptional", packMateriel.getIsOptional());
                    
                    if (!packMateriel.getIsOptional() && materiel.getAvailableQuantity() != null) {
                        int packsPossible = materiel.getAvailableQuantity() / packMateriel.getQuantity();
                        materielInfo.put("packsPossible", packsPossible);
                    }
                    
                    materielsInfo.add(materielInfo);
                }
                
                debugInfo.put("materiels", materielsInfo);
            }
            
        } catch (Exception e) {
            debugInfo.put("error", e.getMessage());
            log.error("Erreur lors du debug de disponibilité du pack {}", packId, e);
        }
        
        return debugInfo;
    }

    /**
     * Méthode de debug pour obtenir les informations détaillées d'un matériel
     */
    public Map<String, Object> getMaterielDebugInfo(Long materielId) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            log.info("🔧 DEBUG MATÉRIEL - ID: {}", materielId);
            
            Materiel materiel = materielRepository.findById(materielId)
                    .orElseThrow(() -> new RuntimeException("Matériel non trouvé avec l'ID: " + materielId));
            
            debugInfo.put("materielId", materiel.getId());
            debugInfo.put("materielName", materiel.getName());
            debugInfo.put("totalQuantity", materiel.getTotalQuantity());
            debugInfo.put("availableQuantity", materiel.getAvailableQuantity());
            debugInfo.put("active", materiel.getActive());
            debugInfo.put("createdAt", materiel.getCreatedAt());
            debugInfo.put("updatedAt", materiel.getUpdatedAt());
            
            // Informations sur les réservations
            List<Reservation> reservations = reservationRepository.findByMaterielIdAndStatusOrderByStartDateAsc(materielId, ReservationStatus.CONFIRMEE);
            debugInfo.put("reservationsCount", reservations.size());
            
            int stockReserve = 0;
            for (Reservation reservation : reservations) {
                stockReserve += reservation.getQuantity();
            }
            debugInfo.put("stockReserve", stockReserve);
            
            log.info("📊 DEBUG MATÉRIEL {}: total={}, disponible={}, réservé={}", 
                    materiel.getName(), materiel.getTotalQuantity(), materiel.getAvailableQuantity(), stockReserve);
            
        } catch (Exception e) {
            debugInfo.put("error", e.getMessage());
            log.error("Erreur lors du debug du matériel {}", materielId, e);
        }
        
        return debugInfo;
    }

    /**
     * Méthode pour corriger le stock disponible d'un matériel
     */
    public Map<String, Object> fixMaterielStock(Long materielId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🔧 CORRECTION STOCK - Matériel ID: {}", materielId);
            
            Materiel materiel = materielRepository.findById(materielId)
                    .orElseThrow(() -> new RuntimeException("Matériel non trouvé avec l'ID: " + materielId));
            
            // Calculer le stock disponible correct
            List<Reservation> reservations = reservationRepository.findByMaterielIdAndStatusOrderByStartDateAsc(materielId, ReservationStatus.CONFIRMEE);
            int stockReserve = reservations.stream().mapToInt(Reservation::getQuantity).sum();
            int stockDisponibleCorrect = materiel.getTotalQuantity() - stockReserve;
            
            log.info("📊 CORRECTION STOCK {}: total={}, réservé={}, disponible_correct={}", 
                    materiel.getName(), materiel.getTotalQuantity(), stockReserve, stockDisponibleCorrect);
            
            // Mettre à jour le stock disponible
            materiel.setAvailableQuantity(stockDisponibleCorrect);
            materielRepository.save(materiel);
            
            result.put("materielId", materiel.getId());
            result.put("materielName", materiel.getName());
            result.put("totalQuantity", materiel.getTotalQuantity());
            result.put("oldAvailableQuantity", materiel.getAvailableQuantity());
            result.put("newAvailableQuantity", stockDisponibleCorrect);
            result.put("stockReserve", stockReserve);
            result.put("reservationsCount", reservations.size());
            result.put("fixed", true);
            
            log.info("✅ STOCK CORRIGÉ pour {}: {} -> {}", materiel.getName(), 
                    result.get("oldAvailableQuantity"), result.get("newAvailableQuantity"));
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("fixed", false);
            log.error("Erreur lors de la correction du stock du matériel {}", materielId, e);
        }
        
        return result;
    }

    /**
     * Vérifie la disponibilité d'un pack pour une réservation spécifique
     * Cette méthode est utilisée lors de l'approbation pour empêcher les réservations multiples
     * sur la même date qui dépassent le stock disponible
     */
    public AvailabilityInfoDto checkPackAvailabilityForReservation(Long packId, LocalDate startDate, LocalDate endDate, Integer requestedQuantity) {
        log.info("🔍 VÉRIFICATION DISPONIBILITÉ PACK POUR RÉSERVATION");
        log.info("Pack ID: {}, Période: {} au {}, Quantité demandée: {}", packId, startDate, endDate, requestedQuantity);
        
        Pack pack = packRepository.findByIdWithMateriels(packId)
                .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));

        // RÈGLE: PACK et CADEAU sont toujours disponibles
        if (pack.getType() == PackType.PACK || pack.getType() == PackType.CADEAU || pack.getType() == PackType.GATEAU) {
            log.info("✅ Type {} - TOUJOURS DISPONIBLE", pack.getType());
            return AvailabilityInfoDto.builder()
                    .packId(packId)
                    .totalQuantity(999)
                    .availableQuantity(999)
                    .reservedQuantity(0)
                    .isAvailable(true)
                    .message("Toujours disponible")
                    .build();
        }

        // Calculer la disponibilité maximale pour cette période
        int maxAvailableQuantity = calculatePackDailyAvailability(pack, startDate, endDate);
        
        log.info("📊 RÉSULTAT VÉRIFICATION:");
        log.info("   - Disponibilité maximale: {}", maxAvailableQuantity);
        log.info("   - Quantité demandée: {}", requestedQuantity);
        log.info("   - Disponible: {}", maxAvailableQuantity >= requestedQuantity);
        
        boolean isAvailable = maxAvailableQuantity >= requestedQuantity;
        String message;
        
        if (isAvailable) {
            message = String.format("Pack disponible - %d unité(s) disponible(s), %d demandée(s)", 
                maxAvailableQuantity, requestedQuantity);
        } else {
            message = String.format("Stock insuffisant - Disponible: %d, Demandé: %d", 
                maxAvailableQuantity, requestedQuantity);
        }
        
        return AvailabilityInfoDto.builder()
                .packId(packId)
                .totalQuantity(maxAvailableQuantity)
                .availableQuantity(maxAvailableQuantity)
                .reservedQuantity(0) // Calculé dans calculatePackDailyAvailability
                .isAvailable(isAvailable)
                .message(message)
                .build();
    }

    /**
     * Méthode pour simuler une réservation confirmée et voir l'impact sur la disponibilité
     * Utile pour tester que les réservations confirmées réduisent bien la disponibilité
     */
    public Map<String, Object> simulateReservationImpact(Long packId, LocalDate testDate, Integer quantity) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🧪 SIMULATION IMPACT RÉSERVATION - Pack: {}, Date: {}, Quantité: {}", packId, testDate, quantity);
            
            Pack pack = packRepository.findByIdWithMateriels(packId)
                    .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));
            
            // Calculer la disponibilité AVANT la réservation
            int availabilityBefore = calculatePackDailyAvailability(pack, testDate, testDate);
            
            result.put("packId", packId);
            result.put("packName", pack.getName());
            result.put("testDate", testDate);
            result.put("requestedQuantity", quantity);
            result.put("availabilityBefore", availabilityBefore);
            
            // Simuler l'impact en créant une réservation temporaire
            List<Map<String, Object>> materialImpact = new ArrayList<>();
            
            if (pack.getPackMateriels() != null) {
                for (var packMateriel : pack.getPackMateriels()) {
                    if (!packMateriel.getIsOptional()) {
                        Map<String, Object> impact = new HashMap<>();
                        Materiel materiel = packMateriel.getMateriel();
                        
                        int requiredQuantity = packMateriel.getQuantity();
                        int totalQuantity = materiel.getTotalQuantity() != null ? materiel.getTotalQuantity() : 0;
                        int reservedBefore = calculateMaterielReservedQuantity(materiel.getId(), testDate, testDate);
                        int availableBefore = totalQuantity - reservedBefore;
                        
                        // Simuler l'impact de la nouvelle réservation
                        int additionalReserved = requiredQuantity * quantity;
                        int availableAfter = availableBefore - additionalReserved;
                        int packsPossibleBefore = availableBefore / requiredQuantity;
                        int packsPossibleAfter = Math.max(0, availableAfter / requiredQuantity);
                        
                        impact.put("materielName", materiel.getName());
                        impact.put("requiredPerPack", requiredQuantity);
                        impact.put("totalStock", totalQuantity);
                        impact.put("reservedBefore", reservedBefore);
                        impact.put("availableBefore", availableBefore);
                        impact.put("additionalReserved", additionalReserved);
                        impact.put("availableAfter", availableAfter);
                        impact.put("packsPossibleBefore", packsPossibleBefore);
                        impact.put("packsPossibleAfter", packsPossibleAfter);
                        
                        materialImpact.add(impact);
                    }
                }
            }
            
            result.put("materialImpact", materialImpact);
            result.put("canReserve", availabilityBefore >= quantity);
            result.put("remainingAfter", Math.max(0, availabilityBefore - quantity));
            
            log.info("🎯 SIMULATION RÉSULTAT: {} disponible(s) avant, {} demandé(s), {} restant(s) après", 
                availabilityBefore, quantity, result.get("remainingAfter"));
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("Erreur lors de la simulation d'impact de réservation pour pack {}", packId, e);
        }
        
        return result;
    }

    /**
     * Méthode de debug pour tester le workflow de vérification avec un exemple concret
     * Exemple: Pack fête normal (3 tables, 10 chaises, 6 nappes) pour le 10/10/2010
     * Stock: 10 tables, 25 nappes, 29 chaises
     * Résultat attendu: 2 packs maximum (limité par les chaises: 29/10 = 2.9 = 2)
     */
    public Map<String, Object> debugPackAvailabilityExample(Long packId, LocalDate testDate) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            log.info("🧪 DEBUG EXEMPLE WORKFLOW PACK - Date: {}", testDate);
            
            Pack pack = packRepository.findByIdWithMateriels(packId)
                    .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));
            
            debugInfo.put("packId", packId);
            debugInfo.put("packName", pack.getName());
            debugInfo.put("testDate", testDate);
            
            // Calculer la disponibilité pour cette date spécifique
            int maxAvailable = calculatePackDailyAvailability(pack, testDate, testDate);
            debugInfo.put("maxPacksAvailable", maxAvailable);
            
            // Détails par matériel
            if (pack.getPackMateriels() != null) {
                List<Map<String, Object>> materielsDetails = new ArrayList<>();
                
                for (var packMateriel : pack.getPackMateriels()) {
                    if (!packMateriel.getIsOptional()) {
                        Map<String, Object> materielInfo = new HashMap<>();
                        Materiel materiel = packMateriel.getMateriel();
                        
                        int requiredQuantity = packMateriel.getQuantity();
                        int totalQuantity = materiel.getTotalQuantity() != null ? materiel.getTotalQuantity() : 0;
                        int reservedForDate = calculateMaterielReservedQuantity(materiel.getId(), testDate, testDate);
                        int availableForDate = totalQuantity - reservedForDate;
                        int packsPossible = availableForDate / requiredQuantity;
                        
                        materielInfo.put("materielName", materiel.getName());
                        materielInfo.put("requiredPerPack", requiredQuantity);
                        materielInfo.put("totalStock", totalQuantity);
                        materielInfo.put("reservedForDate", reservedForDate);
                        materielInfo.put("availableForDate", availableForDate);
                        materielInfo.put("packsPossible", packsPossible);
                        materielInfo.put("calculation", availableForDate + " ÷ " + requiredQuantity + " = " + packsPossible);
                        
                        materielsDetails.add(materielInfo);
                    }
                }
                
                debugInfo.put("materielsDetails", materielsDetails);
            }
            
            log.info("🎯 RÉSULTAT DEBUG: {} packs disponibles le {}", maxAvailable, testDate);
            
        } catch (Exception e) {
            debugInfo.put("error", e.getMessage());
            log.error("Erreur lors du debug de l'exemple de disponibilité du pack {}", packId, e);
        }
        
        return debugInfo;
    }

    /**
     * Méthode de debug pour analyser le problème de calcul de disponibilité
     * Spécialement pour déboguer le pack 6 et la date 24/09/2025
     */
    public Map<String, Object> debugAvailabilityCalculation(Long packId, LocalDate testDate) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            log.info("🔧 DEBUG CALCUL DISPONIBILITÉ - Pack: {}, Date: {}", packId, testDate);
            
            Pack pack = packRepository.findByIdWithMateriels(packId)
                    .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));
            
            debugInfo.put("packId", packId);
            debugInfo.put("packName", pack.getName());
            debugInfo.put("testDate", testDate);
            
            // 1. Vérifier les réservations confirmées pour ce pack
            List<Reservation> packReservations = reservationRepository.findByPackIdAndStatusOrderByStartDateAsc(packId, ReservationStatus.CONFIRMEE);
            List<Map<String, Object>> reservationsInfo = new ArrayList<>();
            
            for (Reservation reservation : packReservations) {
                if (isReservationOverlapping(reservation, testDate, testDate)) {
                    Map<String, Object> resInfo = new HashMap<>();
                    resInfo.put("reservationId", reservation.getId());
                    resInfo.put("startDate", reservation.getStartDate());
                    resInfo.put("endDate", reservation.getEndDate());
                    resInfo.put("quantity", reservation.getQuantity());
                    resInfo.put("status", reservation.getStatus());
                    resInfo.put("overlaps", true);
                    reservationsInfo.add(resInfo);
                    
                    log.info("📅 Réservation {}: {} packs du {} au {}", 
                        reservation.getId(), reservation.getQuantity(), 
                        reservation.getStartDate(), reservation.getEndDate());
                }
            }
            
            debugInfo.put("confirmedReservations", reservationsInfo);
            debugInfo.put("totalConfirmedReservations", reservationsInfo.size());
            
            // 2. Analyser chaque matériel du pack
            if (pack.getPackMateriels() != null) {
                List<Map<String, Object>> materielsAnalysis = new ArrayList<>();
                
                for (var packMateriel : pack.getPackMateriels()) {
                    if (!packMateriel.getIsOptional()) {
                        Map<String, Object> analysis = new HashMap<>();
                        Materiel materiel = packMateriel.getMateriel();
                        
                        int requiredQuantity = packMateriel.getQuantity();
                        int totalQuantity = materiel.getTotalQuantity() != null ? materiel.getTotalQuantity() : 0;
                        int reservedQuantity = calculateMaterielReservedQuantity(materiel.getId(), testDate, testDate);
                        int availableQuantity = totalQuantity - reservedQuantity;
                        int packsPossible = availableQuantity / requiredQuantity;
                        
                        analysis.put("materielId", materiel.getId());
                        analysis.put("materielName", materiel.getName());
                        analysis.put("requiredPerPack", requiredQuantity);
                        analysis.put("totalStock", totalQuantity);
                        analysis.put("reservedForDate", reservedQuantity);
                        analysis.put("availableForDate", availableQuantity);
                        analysis.put("packsPossible", packsPossible);
                        
                        // Debug détaillé des réservations pour ce matériel
                        List<Map<String, Object>> materialReservations = new ArrayList<>();
                        
                        // Réservations directes du matériel
                        List<Reservation> directReservations = reservationRepository.findByMaterielIdAndStatusOrderByStartDateAsc(materiel.getId(), ReservationStatus.CONFIRMEE);
                        for (Reservation res : directReservations) {
                            if (isReservationOverlapping(res, testDate, testDate)) {
                                Map<String, Object> resDetail = new HashMap<>();
                                resDetail.put("type", "direct");
                                resDetail.put("quantity", res.getQuantity());
                                resDetail.put("startDate", res.getStartDate());
                                materialReservations.add(resDetail);
                            }
                        }
                        
                        // Réservations via packs
                        List<PackMateriel> packMateriels = packMaterielRepository.findByMaterielId(materiel.getId());
                        for (PackMateriel pm : packMateriels) {
                            if (!pm.getIsOptional()) {
                                List<Reservation> packRes = reservationRepository.findByPackIdAndStatusOrderByStartDateAsc(pm.getPack().getId(), ReservationStatus.CONFIRMEE);
                                for (Reservation res : packRes) {
                                    if (isReservationOverlapping(res, testDate, testDate)) {
                                        Map<String, Object> resDetail = new HashMap<>();
                                        resDetail.put("type", "pack");
                                        resDetail.put("packName", res.getPack().getName());
                                        resDetail.put("packQuantity", res.getQuantity());
                                        resDetail.put("materialPerPack", pm.getQuantity());
                                        resDetail.put("totalMaterial", res.getQuantity() * pm.getQuantity());
                                        resDetail.put("startDate", res.getStartDate());
                                        materialReservations.add(resDetail);
                                    }
                                }
                            }
                        }
                        
                        analysis.put("reservationDetails", materialReservations);
                        materielsAnalysis.add(analysis);
                    }
                }
                
                debugInfo.put("materielsAnalysis", materielsAnalysis);
                
                // 3. Calcul final
                int finalAvailability = calculatePackDailyAvailability(pack, testDate, testDate);
                debugInfo.put("finalAvailability", finalAvailability);
            }
            
            log.info("🎯 DEBUG RÉSULTAT: {} packs disponibles le {}", debugInfo.get("finalAvailability"), testDate);
            
        } catch (Exception e) {
            debugInfo.put("error", e.getMessage());
            log.error("Erreur lors du debug de calcul de disponibilité pour pack {}", packId, e);
        }
        
        return debugInfo;
    }

    /**
     * Méthode de debug pour obtenir les informations détaillées d'un pack
     */
    public Map<String, Object> getPackDebugInfo(Long packId) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            Pack pack = packRepository.findByIdWithMateriels(packId)
                    .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID: " + packId));

            debugInfo.put("packId", packId);
            debugInfo.put("packName", pack.getName());
            debugInfo.put("packActive", pack.getActive());
            
            if (pack.getPackMateriels() != null) {
                List<Map<String, Object>> materielsInfo = new java.util.ArrayList<>();
                
                for (var packMateriel : pack.getPackMateriels()) {
                    Map<String, Object> materielInfo = new HashMap<>();
                    Materiel materiel = packMateriel.getMateriel();
                    
                    materielInfo.put("materielId", materiel.getId());
                    materielInfo.put("materielName", materiel.getName());
                    materielInfo.put("requiredQuantity", packMateriel.getQuantity());
                    materielInfo.put("availableQuantity", materiel.getAvailableQuantity());
                    materielInfo.put("totalQuantity", materiel.getTotalQuantity());
                    materielInfo.put("isOptional", packMateriel.getIsOptional());
                    materielInfo.put("packsPossible", materiel.getAvailableQuantity() / packMateriel.getQuantity());
                    
                    materielsInfo.add(materielInfo);
                }
                
                debugInfo.put("materiels", materielsInfo);
                
                // Calculer la disponibilité totale
                int totalQuantity = calculatePackTotalQuantity(pack);
                debugInfo.put("calculatedTotalQuantity", totalQuantity);
                
                // Réservations actuelles
                int reservedQuantity = calculatePackCurrentReservations(packId);
                debugInfo.put("reservedQuantity", reservedQuantity);
                
                int availableQuantity = Math.max(0, totalQuantity - reservedQuantity);
                debugInfo.put("finalAvailableQuantity", availableQuantity);
                
            } else {
                debugInfo.put("materiels", "Aucun matériel associé");
                debugInfo.put("calculatedTotalQuantity", 0);
            }
            
        } catch (Exception e) {
            debugInfo.put("error", e.getMessage());
            log.error("Erreur lors du debug du pack {}", packId, e);
        }
        
        return debugInfo;
    }
}

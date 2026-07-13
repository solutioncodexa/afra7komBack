package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.EquipmentStatsDto;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import com.afra7kom.backend.repository.MaterielRepository;
import com.afra7kom.backend.repository.PackRepository;
import com.afra7kom.backend.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentStatsService {

    private final PackRepository packRepository;
    private final MaterielRepository materielRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Récupère les statistiques d'un équipement (Pack ou Materiel)
     */
    public EquipmentStatsDto getEquipmentStats(Long equipmentId, boolean isPack, int derniersMois) {
        List<Reservation> reservations;
        BigDecimal prixUnitaire;

        if (isPack) {
            Pack pack = packRepository.findById(equipmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + equipmentId));
            reservations = reservationRepository.findByPackId(equipmentId);
            prixUnitaire = pack.getPrice();
        } else {
            Materiel materiel = materielRepository.findById(equipmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + equipmentId));
            reservations = reservationRepository.findByMaterielId(equipmentId);
            prixUnitaire = materiel.getPrice();
        }

        // Filtrer uniquement les réservations confirmées/en cours/soldées
        List<Reservation> reservationsApprouvees = reservations.stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CONFIRMEE
                        || r.getStatus() == Reservation.ReservationStatus.EN_COURS
                        || r.getStatus() == Reservation.ReservationStatus.SOLDEE)
                .toList();

        // Calculer les réservations par jour
        List<EquipmentStatsDto.ReservationParJour> reservationsParJour = 
                calculerReservationsParJour(reservationsApprouvees, prixUnitaire);

        // Calculer les revenus par mois
        List<EquipmentStatsDto.RevenuParMois> revenusParMois = 
                calculerRevenusParMois(reservationsApprouvees, prixUnitaire, derniersMois);

        // Calculer les statistiques globales
        Integer totalReservations = reservationsApprouvees.size();
        BigDecimal totalRevenue = reservationsApprouvees.stream()
                .map(r -> prixUnitaire.multiply(BigDecimal.valueOf(r.getQuantity() != null ? r.getQuantity() : 1)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageMonthlyRevenue = BigDecimal.ZERO;
        if (!revenusParMois.isEmpty()) {
            averageMonthlyRevenue = totalRevenue.divide(
                    BigDecimal.valueOf(revenusParMois.size()), 2, RoundingMode.HALF_UP);
        }

        EquipmentStatsDto stats = new EquipmentStatsDto();
        stats.setReservationsParJour(reservationsParJour);
        stats.setRevenusParMois(revenusParMois);
        stats.setTotalReservations(totalReservations);
        stats.setTotalRevenue(totalRevenue);
        stats.setAverageMonthlyRevenue(averageMonthlyRevenue);

        return stats;
    }

    /**
     * Calcule les réservations par jour
     */
    private List<EquipmentStatsDto.ReservationParJour> calculerReservationsParJour(
            List<Reservation> reservations, BigDecimal prixUnitaire) {
        
        Map<LocalDate, List<Reservation>> reservationsParDate = new HashMap<>();

        for (Reservation reservation : reservations) {
            LocalDate startDate = reservation.getStartDate();
            if (startDate != null) {
                reservationsParDate.computeIfAbsent(startDate, k -> new ArrayList<>()).add(reservation);
            }
        }

        return reservationsParDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Reservation> resJour = entry.getValue();
                    int nombreReservations = resJour.size();
                    BigDecimal revenuJour = resJour.stream()
                            .map(r -> prixUnitaire.multiply(BigDecimal.valueOf(r.getQuantity() != null ? r.getQuantity() : 1)))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return new EquipmentStatsDto.ReservationParJour(date, nombreReservations, revenuJour);
                })
                .sorted(Comparator.comparing(EquipmentStatsDto.ReservationParJour::getDate))
                .collect(Collectors.toList());
    }

    /**
     * Calcule les revenus par mois pour les derniers X mois
     */
    private List<EquipmentStatsDto.RevenuParMois> calculerRevenusParMois(
            List<Reservation> reservations, BigDecimal prixUnitaire, int derniersMois) {
        
        Map<YearMonth, List<Reservation>> reservationsParMois = new HashMap<>();

        for (Reservation reservation : reservations) {
            LocalDate startDate = reservation.getStartDate();
            if (startDate != null) {
                YearMonth yearMonth = YearMonth.from(startDate);
                reservationsParMois.computeIfAbsent(yearMonth, k -> new ArrayList<>()).add(reservation);
            }
        }

        // Générer les derniers X mois même s'il n'y a pas de réservations
        YearMonth currentMonth = YearMonth.now();
        List<EquipmentStatsDto.RevenuParMois> result = new ArrayList<>();

        for (int i = derniersMois - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            List<Reservation> resduMois = reservationsParMois.getOrDefault(month, new ArrayList<>());
            
            BigDecimal revenu = resduMois.stream()
                    .map(r -> prixUnitaire.multiply(BigDecimal.valueOf(r.getQuantity() != null ? r.getQuantity() : 1)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            EquipmentStatsDto.RevenuParMois revenuMois = new EquipmentStatsDto.RevenuParMois();
            revenuMois.setMois(month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            revenuMois.setAnnee(month.getYear());
            revenuMois.setMoisNumero(month.getMonthValue());
            revenuMois.setRevenu(revenu);
            revenuMois.setNombreReservations(resduMois.size());

            result.add(revenuMois);
        }

        return result;
    }
}



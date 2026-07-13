package com.afra7kom.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentStatsDto {
    
    // Statistiques des réservations par jour
    private List<ReservationParJour> reservationsParJour;
    
    // Moyenne des revenus par mois
    private List<RevenuParMois> revenusParMois;
    
    // Statistiques globales
    private Integer totalReservations;
    private BigDecimal totalRevenue;
    private BigDecimal averageMonthlyRevenue;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationParJour {
        private LocalDate date;
        private Integer nombreReservations;
        private BigDecimal revenuJour;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenuParMois {
        private String mois; // Format: "2025-01"
        private Integer annee;
        private Integer moisNumero;
        private BigDecimal revenu;
        private Integer nombreReservations;
    }
}



package com.afra7kom.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDto {

    private Long itemId;
    private String itemType; // "PACK" ou "MATERIEL"
    private String itemName;
    private LocalDate date;
    private Boolean available;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private List<ReservationSummary> conflictingReservations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationSummary {
        private Long reservationId;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer quantity;
        private String status;
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class AvailabilityRequest {
    private Long materielId;
    private Long packId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer quantity;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class AvailabilityResponse {
    private Boolean available;
    private String message;
    private List<AvailabilityDto> details;
    private Map<LocalDate, Boolean> calendar;
}




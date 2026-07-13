package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Reservation.ReservationStatus;
import com.afra7kom.backend.entity.Reservation.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReservationFilterDto {
    
    private ReservationStatus status; // PENDING, APPROVED, REJECTED, CANCELLED
    private PaymentStatus paymentStatus; // PENDING, PARTIAL, COMPLETED
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDateFrom;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDateTo;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDateFrom;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDateTo;
    
    private String customerName; // Recherche par nom/prénom
    private String phone;
    private String email;
    
    private Long packId;
    private Long materielId;
    
    private String assignedAgentName;
    
    // Pagination
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}

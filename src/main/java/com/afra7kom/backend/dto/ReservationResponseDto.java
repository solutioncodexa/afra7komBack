package com.afra7kom.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReservationResponseDto {
    
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String city;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private Integer quantity;
    private Integer numberOfDays;
    
    private Long packId;
    private String packName;
    private Long materielId;
    private String materielName;
    
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED
    private String paymentStatus; // PENDING, PARTIAL, COMPLETED
    
    private String notes;
    private String deliveryAddress;
    private String assignedAgentName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // Informations de disponibilité
    private Boolean isAvailable;
    private String availabilityMessage;
}



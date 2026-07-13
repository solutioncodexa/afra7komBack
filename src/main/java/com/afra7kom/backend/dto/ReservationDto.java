package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Reservation;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {

    private Long id;
    private Long userId;
    private String userEmail;
    private String userName;
    private Long packId;
    private Long materielId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer quantity;
    private Reservation.ReservationStatus status;
    private String statusDisplayName;
    private Reservation.PaymentStatus paymentStatus;
    private String paymentStatusDisplayName;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private String notes;
    private String deliveryAddress;
    private Long assignedAgentId;
    private String assignedAgentEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReservationItemDto> items;
    private Long durationDays;
    private Boolean canCancel;
    private Boolean canModify;

    public static ReservationDto fromEntity(Reservation reservation) {
        ReservationDto dto = new ReservationDto();
        dto.setId(reservation.getId());
        dto.setUserId(reservation.getUser().getId());
        dto.setUserEmail(reservation.getUser().getEmail());
        dto.setUserName(reservation.getUser().getEmail()); // Ou nom complet si disponible
        dto.setPackId(reservation.getPack() != null ? reservation.getPack().getId() : null);
        dto.setMaterielId(reservation.getMateriel() != null ? reservation.getMateriel().getId() : null);
        dto.setStartDate(reservation.getStartDate());
        dto.setEndDate(reservation.getEndDate());
        dto.setQuantity(reservation.getQuantity());
        dto.setStatus(reservation.getStatus());
        dto.setStatusDisplayName(reservation.getStatus().getDisplayName());
        dto.setPaymentStatus(reservation.getPaymentStatus());
        dto.setPaymentStatusDisplayName(reservation.getPaymentStatus().getDisplayName());
        dto.setTotalAmount(reservation.getTotalAmount());
        dto.setDepositAmount(reservation.getDepositAmount());
        dto.setNotes(reservation.getNotes());
        dto.setDeliveryAddress(reservation.getDeliveryAddress());
        dto.setAssignedAgentId(reservation.getAssignedAgent() != null ? reservation.getAssignedAgent().getId() : null);
        dto.setAssignedAgentEmail(reservation.getAssignedAgent() != null ? reservation.getAssignedAgent().getEmail() : null);
        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setUpdatedAt(reservation.getUpdatedAt());
        
        // Items
        if (reservation.getItems() != null) {
            dto.setItems(reservation.getItems().stream()
                    .map(ReservationItemDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        
        // Propriétés calculées
        dto.setDurationDays(reservation.getDurationDays());
        dto.setCanCancel(reservation.isCancellable());
        dto.setCanModify(reservation.isModifiable());
        
        return dto;
    }

    // Version simplifiée pour les listes
    public static ReservationDto fromEntitySimple(Reservation reservation) {
        ReservationDto dto = new ReservationDto();
        dto.setId(reservation.getId());
        dto.setUserId(reservation.getUser().getId());
        dto.setUserEmail(reservation.getUser().getEmail());
        dto.setPackId(reservation.getPack() != null ? reservation.getPack().getId() : null);
        dto.setMaterielId(reservation.getMateriel() != null ? reservation.getMateriel().getId() : null);
        dto.setStartDate(reservation.getStartDate());
        dto.setEndDate(reservation.getEndDate());
        dto.setQuantity(reservation.getQuantity());
        dto.setStatus(reservation.getStatus());
        dto.setStatusDisplayName(reservation.getStatus().getDisplayName());
        dto.setPaymentStatus(reservation.getPaymentStatus());
        dto.setPaymentStatusDisplayName(reservation.getPaymentStatus().getDisplayName());
        dto.setTotalAmount(reservation.getTotalAmount());
        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setDurationDays(reservation.getDurationDays());
        dto.setCanCancel(reservation.isCancellable());
        dto.setCanModify(reservation.isModifiable());
        
        return dto;
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ReservationRequest {
    private Long packId;
    private Long materielId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer quantity;
    private String notes;
    private String deliveryAddress;
    private List<ReservationItemRequest> items;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ReservationItemRequest {
    private Long materielId;
    private Long packId;
    private Integer quantity;
    private String notes;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class UpdateReservationStatusRequest {
    private Reservation.ReservationStatus status;
    private Reservation.PaymentStatus paymentStatus;
    private Long assignedAgentId;
}




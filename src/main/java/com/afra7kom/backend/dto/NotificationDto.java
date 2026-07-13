package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Notification;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String type;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private Long reservationId;

    public static NotificationDto fromEntity(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().toString())
                .status(notification.getStatus().toString())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .reservationId(notification.getReservationId())
                .build();
    }
}


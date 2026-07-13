package com.afra7kom.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatusUpdateDto {
    
    @NotBlank(message = "Le statut est obligatoire")
    private String status;
    
    private String notes;
    
    private String reason;
}

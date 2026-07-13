package com.afra7kom.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityCheckDto {
    
    private boolean available;
    private String message;
    private Integer availableQuantity;
    private Integer requestedQuantity;
    
    public AvailabilityCheckDto(boolean available, String message) {
        this.available = available;
        this.message = message;
    }
    
    public AvailabilityCheckDto(boolean available, String message, Integer availableQuantity) {
        this.available = available;
        this.message = message;
        this.availableQuantity = availableQuantity;
    }
}



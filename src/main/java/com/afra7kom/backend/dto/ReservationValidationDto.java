package com.afra7kom.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReservationValidationDto {
    
    @NotNull(message = "Le statut de validation est obligatoire")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "Le statut doit être APPROVED ou REJECTED")
    private String status;
    
    @NotNull(message = "Le montant de l'avance est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "L'avance doit être supérieure à 0")
    @Digits(integer = 8, fraction = 2, message = "Format de montant invalide")
    private BigDecimal depositAmount;
    
    @NotBlank(message = "Le mode de paiement est obligatoire")
    @Pattern(regexp = "^(CASH|VIREMENT|CHEQUE)$", message = "Mode de paiement invalide")
    private String paymentMethod;
    
    private String notes;
    private String rejectionReason; // Obligatoire si status = REJECTED
    
    @AssertTrue(message = "Le motif de rejet est obligatoire en cas de rejet")
    public boolean isRejectionReasonProvided() {
        if ("REJECTED".equals(status)) {
            return rejectionReason != null && !rejectionReason.trim().isEmpty();
        }
        return true;
    }
}



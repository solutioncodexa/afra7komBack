package com.afra7kom.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class ReservationRequestDto {
    
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String firstName;
    
    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    private String lastName;
    
    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^[+]?[0-9\\s\\-()]{8,20}$", message = "Format de téléphone invalide")
    private String phone;
    
    @Email(message = "Format d'email invalide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    private String email; // Optionnel pour les invités
    
    @NotBlank(message = "La ville est obligatoire")
    @Size(max = 50, message = "La ville ne peut pas dépasser 50 caractères")
    private String city;
    
    @NotNull(message = "La date de début est obligatoire")
    @JsonDeserialize(using = IsoDateDeserializer.class)
    private LocalDate startDate;
    
    @NotNull(message = "La date de fin est obligatoire")
    @JsonDeserialize(using = IsoDateDeserializer.class)
    private LocalDate endDate;
    
    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantity;
    
    // Un seul des deux doit être fourni
    private Long packId;
    private Long materielId;
    
    private String notes;
    private String deliveryAddress;
    
    @AssertTrue(message = "Vous devez sélectionner soit un pack soit un matériel")
    public boolean isPackOrMaterielSelected() {
        return (packId != null && materielId == null) || (packId == null && materielId != null);
    }
    
    @AssertTrue(message = "La date de fin doit être après la date de début")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}

/**
 * Désérialiseur personnalisé pour convertir les dates ISO en LocalDate
 * Gère correctement les timezones pour éviter les décalages de date
 */
class IsoDateDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<LocalDate> {
    
    @Override
    public LocalDate deserialize(com.fasterxml.jackson.core.JsonParser p, 
                                com.fasterxml.jackson.databind.DeserializationContext ctxt) 
                                throws java.io.IOException {
        String dateString = p.getText();
        
        try {
            // Essayer de parser comme LocalDate simple d'abord (format: 2025-09-24)
            return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e1) {
            try {
                // Si c'est un format ISO avec timezone, extraire seulement la partie date
                // Format: 2025-09-24T00:00:00.000Z -> 2025-09-24
                if (dateString.contains("T")) {
                    String datePart = dateString.split("T")[0];
                    return LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
                
                // Essayer de parser comme LocalDateTime ISO puis extraire la date
                LocalDateTime dateTime = LocalDateTime.parse(dateString, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
                return dateTime.toLocalDate();
            } catch (Exception e2) {
                try {
                    // Essayer de parser comme LocalDateTime sans millisecondes
                    LocalDateTime dateTime = LocalDateTime.parse(dateString, 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
                    return dateTime.toLocalDate();
                } catch (Exception e3) {
                    throw new com.fasterxml.jackson.databind.exc.InvalidFormatException(
                        p, "Impossible de parser la date: " + dateString, dateString, LocalDate.class);
                }
            }
        }
    }
}

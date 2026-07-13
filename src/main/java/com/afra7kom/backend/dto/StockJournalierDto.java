package com.afra7kom.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockJournalierDto {
    private Long materielId;
    private String materielName;
    private String marque;
    private String modele;
    private Integer stockTotal;
    private Integer stockReserve; // Quantité réservée pour aujourd'hui
    private Integer stockDisponible; // Stock journalier restant
    private BigDecimal price;
    private LocalDate date;
    private Boolean active;
    private String imageUrl;
}



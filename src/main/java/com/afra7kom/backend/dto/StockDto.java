package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.MouvementStock;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDto {

    private Long materielId;
    private String materielName;
    private String materielDescription;
    private String materielImageUrl;
    private Integer stockActuel;
    private Integer stockMinimum;
    private Integer stockReserve;
    private Integer stockDisponible;
    private Boolean alerteStockBas;
    private Boolean alerteStockEpuise;
    private BigDecimal valeurStock;
    private BigDecimal prixUnitaireMoyen;
    private LocalDateTime dernierMouvement;
    private String dernierTypeMouvement;
    private List<MouvementStockDto> derniersMovements;
    private StatutStock statut;

    public enum StatutStock {
        DISPONIBLE("Disponible", "#4caf50"),
        STOCK_BAS("Stock bas", "#ff9800"),
        EPUISE("Épuisé", "#f44336"),
        SURSTOCKE("Surstocké", "#2196f3"),
        INACTIF("Inactif", "#9e9e9e");

        private final String displayName;
        private final String color;

        StatutStock(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }
    }

    // Méthodes utilitaires
    public StatutStock determinerStatut() {
        if (stockActuel == null || stockActuel <= 0) {
            return StatutStock.EPUISE;
        }
        
        if (stockMinimum != null && stockActuel <= stockMinimum) {
            return StatutStock.STOCK_BAS;
        }
        
        if (dernierMouvement != null && 
            dernierMouvement.isBefore(LocalDateTime.now().minusDays(90))) {
            return StatutStock.INACTIF;
        }
        
        return StatutStock.DISPONIBLE;
    }

    public boolean isStockBas() {
        return alerteStockBas != null && alerteStockBas;
    }

    public boolean isStockEpuise() {
        return alerteStockEpuise != null && alerteStockEpuise;
    }

    public double getPourcentageStock() {
        if (stockActuel == null || stockActuel <= 0) {
            return 0.0;
        }
        // Utiliser le stock minimum comme référence si disponible
        if (stockMinimum != null && stockMinimum > 0) {
            return Math.min(100.0, (stockActuel * 100.0) / stockMinimum);
        }
        return 100.0; // Si pas de référence, considérer comme 100%
    }

    public String getFormattedValeurStock() {
        if (valeurStock != null) {
            return String.format("%.2f MAD", valeurStock);
        }
        return "0.00 MAD";
    }
}

// Request DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
class MouvementStockRequest {
    private Long materielId;
    private MouvementStock.TypeMouvement type;
    private Integer quantity;
    private LocalDateTime date;
    private BigDecimal prixUnitaire;
    private String referenceExterne;
    private String fournisseur;
    private String bonLivraison;
    private String notes;
    private String motif;
    private Long reservationId;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CorrectionStockRequest {
    private Long materielId;
    private Integer stockTheorique;
    private Integer stockReel;
    private String motif;
    private String notes;
}

// Response DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
class StockSummaryDto {
    private Long totalMateriels;
    private Long totalMouvements;
    private BigDecimal valeurTotaleStock;
    private Long alertesStockBas;
    private Long alertesStockEpuise;
    private LocalDateTime derniereMiseAJour;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class AlerteStockDto {
    private Long materielId;
    private String materielName;
    private Integer stockActuel;
    private Integer stockMinimum;
    private StockDto.StatutStock statut;
    private String message;
    private Integer joursDepuisDernierMouvement;
    private LocalDateTime dernierMouvement;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class StatistiquesStockDto {
    private LocalDateTime periode;
    private Long totalEntrees;
    private Long totalSorties;
    private BigDecimal valeurEntrees;
    private BigDecimal valeurSorties;
    private List<StatParType> mouvementsParType;
    private List<StatParMateriel> topMateriels;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class StatParType {
    private MouvementStock.TypeMouvement type;
    private String typeDisplayName;
    private Long nombreMouvements;
    private Long quantiteTotale;
    private BigDecimal valeurTotale;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class StatParMateriel {
    private Long materielId;
    private String materielName;
    private Long stockTotal;
    private Long nombreMouvements;
    private BigDecimal valeurStock;
    private LocalDateTime dernierMouvement;
}




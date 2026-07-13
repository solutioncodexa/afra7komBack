package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.MouvementStock;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MouvementStockDto {
    private Long id;
    private Long materielId;
    private String materielName;
    private Long userId;
    private String userName;
    private MouvementStock.TypeMouvement type;
    private String typeDisplayName;
    private String typeSymbole;
    private Integer quantity;
    private LocalDateTime date;
    private BigDecimal prixUnitaire;
    private BigDecimal coutTotal;
    private String referenceExterne;
    private String fournisseur;
    private String notes;
    private Long reservationId;
    private Integer stockAvant;
    private Integer stockApres;
    private Boolean valide;
    private LocalDateTime dateValidation;
    private String valideParName;
    private LocalDateTime createdAt;

    public static MouvementStockDto fromEntity(MouvementStock mouvement) {
        MouvementStockDto dto = new MouvementStockDto();
        dto.setId(mouvement.getId());
        dto.setMaterielId(mouvement.getMateriel().getId());
        dto.setMaterielName(mouvement.getMateriel().getName());
        dto.setUserId(mouvement.getUser().getId());
        dto.setUserName(mouvement.getUser().getEmail());
        dto.setType(mouvement.getType());
        dto.setTypeDisplayName(mouvement.getTypeDisplayName());
        dto.setTypeSymbole(mouvement.getTypeSymbole());
        dto.setQuantity(mouvement.getQuantity());
        dto.setDate(mouvement.getDate());
        dto.setPrixUnitaire(mouvement.getPrixUnitaire());
        dto.setCoutTotal(mouvement.getCoutTotal());
        dto.setReferenceExterne(mouvement.getReferenceExterne());
        dto.setFournisseur(mouvement.getFournisseur());
        dto.setNotes(mouvement.getNotes());
        
        if (mouvement.getReservation() != null) {
            dto.setReservationId(mouvement.getReservation().getId());
        }
        
        dto.setStockAvant(mouvement.getStockAvant());
        dto.setStockApres(mouvement.getStockApres());
        dto.setValide(mouvement.getValide());
        dto.setDateValidation(mouvement.getDateValidation());
        
        if (mouvement.getValidePar() != null) {
            dto.setValideParName(mouvement.getValidePar().getEmail());
        }
        
        dto.setCreatedAt(mouvement.getCreatedAt());
        
        return dto;
    }
}

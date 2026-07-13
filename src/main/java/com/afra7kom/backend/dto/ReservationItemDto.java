package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.ReservationItem;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationItemDto {

    private Long id;
    private Long reservationId;
    private Long materielId;
    private Long packId;
    private String itemName;
    private String itemDescription;
    private String itemImageUrl;
    private ReservationItem.ItemType type;
    private String typeDisplayName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String notes;
    private Boolean delivered;
    private Boolean returned;
    private String damageNotes;
    private BigDecimal damageCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private MaterielDto materiel;
    private PackDto pack;

    public static ReservationItemDto fromEntity(ReservationItem item) {
        ReservationItemDto dto = new ReservationItemDto();
        dto.setId(item.getId());
        dto.setReservationId(item.getReservation().getId());
        dto.setType(item.getType());
        dto.setTypeDisplayName(item.getType().getDisplayName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setNotes(item.getNotes());
        dto.setDelivered(item.getDelivered());
        dto.setReturned(item.getReturned());
        dto.setDamageNotes(item.getDamageNotes());
        dto.setDamageCost(item.getDamageCost());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        
        // Informations de l'item
        dto.setItemName(item.getItemName());
        dto.setItemDescription(item.getItemDescription());
        dto.setItemImageUrl(item.getItemImageUrl());
        
        // Références
        if (item.getMateriel() != null) {
            dto.setMaterielId(item.getMateriel().getId());
            dto.setMateriel(MaterielDto.fromEntity(item.getMateriel()));
        }
        
        if (item.getPack() != null) {
            dto.setPackId(item.getPack().getId());
            dto.setPack(PackDto.fromEntitySimple(item.getPack()));
        }
        
        return dto;
    }

    // Version simplifiée
    public static ReservationItemDto fromEntitySimple(ReservationItem item) {
        ReservationItemDto dto = new ReservationItemDto();
        dto.setId(item.getId());
        dto.setType(item.getType());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setItemName(item.getItemName());
        dto.setItemImageUrl(item.getItemImageUrl());
        
        if (item.getMateriel() != null) {
            dto.setMaterielId(item.getMateriel().getId());
        }
        
        if (item.getPack() != null) {
            dto.setPackId(item.getPack().getId());
        }
        
        return dto;
    }
}




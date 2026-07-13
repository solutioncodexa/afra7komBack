package com.afra7kom.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class MaterielCreateDto {
    
    @NotBlank(message = "Le nom est obligatoire")
    private String name;
    
    private String description;
    
    @NotNull(message = "Le prix est obligatoire")
    @Positive(message = "Le prix doit être positif")
    private BigDecimal price;
    
    @Deprecated
    private String imageUrl; // Obsolète - utiliser ProductImage à la place
    
    @NotNull(message = "L'ID de la catégorie est obligatoire")
    @JsonProperty("categoryId")
    private Long categorieId;
    
    private String marque;
    
    private String modele;
    
    private Boolean active = true;
    
    // Constructeurs
    public MaterielCreateDto() {}
    
    public MaterielCreateDto(String name, String description, BigDecimal price, String imageUrl, Long categorieId, String marque, String modele, Boolean active) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.categorieId = categorieId;
        this.marque = marque;
        this.modele = modele;
        this.active = active;
    }
    
    // Getters et Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public Long getCategorieId() {
        return categorieId;
    }
    
    public void setCategorieId(Long categorieId) {
        this.categorieId = categorieId;
    }
    
    public String getMarque() {
        return marque;
    }
    
    public void setMarque(String marque) {
        this.marque = marque;
    }
    
    public String getModele() {
        return modele;
    }
    
    public void setModele(String modele) {
        this.modele = modele;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
}






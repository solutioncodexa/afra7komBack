package com.afra7kom.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "packs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@EntityListeners(AuditingEntityListener.class)
public class Pack {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    @NotBlank(message = "Pack name cannot be blank")
    @Size(max = 100, message = "Pack name cannot exceed 100 characters")
    private String name;
    
    @Column(name = "description")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    @Column(name = "price", nullable = false)
    @DecimalMin(value = "0.0", message = "Price must be positive")
    private BigDecimal price;
    
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "is_favorite")
    private Boolean isFavorite = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PackType type = PackType.PACK;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;
    
    @OneToMany(mappedBy = "pack", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PackMateriel> packMateriels;
    
    // Images stockées directement comme colonnes simples
    @Column(name = "images", columnDefinition = "TEXT")
    private String images; // JSON array des URLs d'images
    
    @Column(name = "primary_image_url")
    private String primaryImageUrl;
    
    // Getters pour les colonnes de base de données
    public String getImages() {
        return images;
    }
    
    public void setImages(String images) {
        this.images = images;
    }
    
    public String getPrimaryImageUrl() {
        return primaryImageUrl;
    }
    
    public void setPrimaryImageUrl(String primaryImageUrl) {
        this.primaryImageUrl = primaryImageUrl;
    }
    
    @OneToMany(mappedBy = "pack", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReservationItem> reservationItems;

    @OneToMany(mappedBy = "pack", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Favoris> favoris;

    // Champs temporaires pour l'upload d'images (non persistés en base)

    // Constructeurs utilitaires
    public Pack(String name, String description, BigDecimal price, Categorie categorie) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categorie = categorie;
        this.active = true;
        this.type = PackType.PACK;
    }
    
    public Pack(String name, String description, BigDecimal price, Categorie categorie, PackType type) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categorie = categorie;
        this.active = true;
        this.type = type;
    }

    // Méthodes utilitaires
    public boolean isAvailable() {
        return active && packMateriels != null && 
               packMateriels.stream().allMatch(pm -> pm.getMateriel().isAvailable());
    }

    public BigDecimal calculateTotalValue() {
        if (packMateriels == null) return BigDecimal.ZERO;
        
        return packMateriels.stream()
                .map(pm -> pm.getMateriel().getPrice().multiply(BigDecimal.valueOf(pm.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Integer getTotalItems() {
        if (packMateriels == null) return 0;
        
        return packMateriels.stream()
                .mapToInt(PackMateriel::getQuantity)
                .sum();
    }
    
    // Méthodes pour gérer les images (JSON)
    public String getPrimaryImage() {
        return primaryImageUrl != null ? primaryImageUrl : 
               (getActiveImages() != null && !getActiveImages().isEmpty() ? getActiveImages().get(0) : null);
    }
    
    public List<String> getActiveImages() {
        if (images == null || images.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // Simple parsing JSON array: ["url1", "url2", "url3"]
            String cleanJson = images.trim();
            if (cleanJson.startsWith("[") && cleanJson.endsWith("]")) {
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1);
                if (cleanJson.trim().isEmpty()) {
                    return new ArrayList<>();
                }
                return Arrays.stream(cleanJson.split(","))
                    .map(url -> url.trim().replaceAll("^\"|\"$", ""))
                    .filter(url -> !url.isEmpty())
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing des images JSON: " + e.getMessage());
        }
        return new ArrayList<>();
    }
    
    public void setActiveImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            this.images = null;
        } else {
            this.images = "[" + imageUrls.stream()
                .map(url -> "\"" + url + "\"")
                .collect(Collectors.joining(",")) + "]";
        }
    }
    
    public void setPrimaryImage(String imageUrl) {
        this.primaryImageUrl = imageUrl;
    }
    
    public void addImage(String imageUrl) {
        List<String> currentImages = getActiveImages();
        currentImages.add(imageUrl);
        setActiveImages(currentImages);
    }
    
    // Méthode pour compatibilité avec le code existant
    public void setImagesList(List<String> imageUrls) {
        setActiveImages(imageUrls);
    }


    @Override
    public String toString() {
        return "Pack{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", active=" + active +
                ", categoryId=" + (categorie != null ? categorie.getId() : null) +
                '}';
    }
}




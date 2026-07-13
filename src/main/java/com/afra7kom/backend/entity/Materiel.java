package com.afra7kom.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "materiels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@EntityListeners(AuditingEntityListener.class)
public class Materiel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    @NotBlank(message = "Material name cannot be blank")
    @Size(max = 100, message = "Material name cannot exceed 100 characters")
    private String name;
    
    @Column(name = "description")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    @Column(name = "price", nullable = false)
    @DecimalMin(value = "0.0", message = "Price must be positive")
    private BigDecimal price;
    
    
    @Column(name = "total_quantity", nullable = false)
    @Min(value = 0, message = "Total quantity must be positive")
    private Integer totalQuantity;
    
    @Column(name = "available_quantity", nullable = false)
    @Min(value = 0, message = "Available quantity must be positive")
    private Integer availableQuantity;
    
    @Column(name = "minimum_stock")
    private Integer minimumStock;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "is_favorite")
    private Boolean isFavorite = false;

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
    
    @OneToMany(mappedBy = "materiel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PackMateriel> packMateriels;
    
    @OneToMany(mappedBy = "materiel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReservationItem> reservationItems;
    
    @OneToMany(mappedBy = "materiel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MouvementStock> mouvementsStock;

    @OneToMany(mappedBy = "materiel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Favoris> favoris;
    
    // Images stockées directement comme colonnes simples
    @Column(name = "images", columnDefinition = "TEXT")
    private String images; // JSON array des URLs d'images
    
    @Column(name = "primary_image_url")
    private String primaryImageUrl;


    // Constructeurs utilitaires
    public Materiel(String name, String description, BigDecimal price, Categorie categorie) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categorie = categorie;
        this.totalQuantity = 0;
        this.availableQuantity = 0;
    }

    // Méthodes utilitaires
    public boolean isAvailable() {
        return active && availableQuantity > 0;
    }

    public boolean isLowStock() {
        return minimumStock != null && availableQuantity <= minimumStock;
    }

    public void updateAvailableQuantity(int quantityChange) {
        this.availableQuantity = Math.max(0, this.availableQuantity + quantityChange);
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
    public void setImages(List<String> imageUrls) {
        setActiveImages(imageUrls);
    }

    @Override
    public String toString() {
        return "Materiel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", totalQuantity=" + totalQuantity +
                ", availableQuantity=" + availableQuantity +
                ", active=" + active +
                '}';
    }
}




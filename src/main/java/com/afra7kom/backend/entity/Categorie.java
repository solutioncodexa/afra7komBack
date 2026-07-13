package com.afra7kom.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@EntityListeners(AuditingEntityListener.class)
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    @NotBlank(message = "Category name cannot be blank")
    @Size(max = 100, message = "Category name cannot exceed 100 characters")
    private String name;

    @Column(name = "description")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;


    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relations
    @OneToMany(mappedBy = "categorie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Materiel> materiels;

    @OneToMany(mappedBy = "categorie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pack> packs;
    
    // Images stockées directement comme colonnes simples
    @Column(name = "images", columnDefinition = "TEXT")
    private String images; // JSON array des URLs d'images
    
    @Column(name = "primary_image_url")
    private String primaryImageUrl;

    public Categorie(String name, String description) {
        this.name = name;
        this.description = description;
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
        return "Categorie{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", active=" + active +
                '}';
    }
}




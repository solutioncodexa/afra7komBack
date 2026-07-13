package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.MaterielDto;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.Categorie;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.repository.MaterielRepository;
import com.afra7kom.backend.repository.CategorieRepository;
import com.afra7kom.backend.repository.ReservationRepository;
import com.afra7kom.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterielService {

    private final MaterielRepository materielRepository;
    private final CategorieRepository categorieRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final CachedListService cachedListService;
    private final FileStorageService fileStorageService;
    
    // Injection avec @Lazy pour éviter la dépendance circulaire
    @Autowired
    @Lazy
    private ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public Page<MaterielDto> getAllMateriels(Pageable pageable) {
        return cachedListService.getCachedActiveMateriels(pageable);
    }

    @Transactional(readOnly = true)
    public Page<MaterielDto> searchMateriels(String search, Pageable pageable) {
        return cachedListService.getCachedSearchMateriels(search, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MaterielDto> searchMaterielsWithFilters(Boolean active, Long categorieId, 
                                                       BigDecimal minPrice, BigDecimal maxPrice, 
                                                       Boolean isAvailable, Pageable pageable) {
        return cachedListService.getCachedMaterielsWithFilters(active, categorieId, minPrice, maxPrice, isAvailable, pageable);
    }

    @Transactional(readOnly = true)
    public MaterielDto getMaterielById(Long id) {
        Materiel materiel = materielRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + id));
        return MaterielDto.fromEntity(materiel);
    }

    @Transactional(readOnly = true)
    public Page<MaterielDto> getMaterielsByCategorie(Long categorieId, Pageable pageable) {
        return materielRepository.findByCategorieIdAndActiveTrue(categorieId, pageable)
                .map(MaterielDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<MaterielDto> getMaterielsDisponibles(Pageable pageable) {
        return materielRepository.findByActiveTrueAndAvailableTrue(pageable)
                .map(MaterielDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<MaterielDto> getMostFavoritedMateriels(Pageable pageable) {
        return materielRepository.findMostFavoritedMateriels(pageable)
                .map(MaterielDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<MaterielDto> getMostRentedMateriels(Pageable pageable) {
        return materielRepository.findMostRentedMateriels(pageable)
                .map(MaterielDto::fromEntity);
    }

    public MaterielDto createMateriel(String name, String description, BigDecimal price, 
                                     String imageUrl, Long categorieId, 
                                     String marque, String modele) {
        // Vérifier que la catégorie existe
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID: " + categorieId));

        Materiel materiel = new Materiel();
        materiel.setName(name);
        materiel.setDescription(description);
        materiel.setPrice(price);
        // imageUrl supprimé - maintenant géré par ProductImage
        materiel.setCategorie(categorie);
        materiel.setActive(true);
        materiel.setTotalQuantity(0);
        materiel.setAvailableQuantity(0);

        Materiel savedMateriel = materielRepository.save(materiel);

        // Créer automatiquement une image pour le nouveau matériel
        // TODO: Gérer les images via la nouvelle structure List<String>
        // Les images sont maintenant gérées directement dans l'entité Materiel

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "MATERIEL_CREATE",
            "Matériel créé: " + savedMateriel.getName(),
            securityUtils.getCurrentIpAddress()
        );

        // Invalider le cache
        cachedListService.evictAllMaterielCaches();

        return MaterielDto.fromEntity(savedMateriel);
    }

    public MaterielDto createMaterielWithImages(String name, String description, BigDecimal price, Long categorieId,
                                               String marque, String modele, Integer quantity,
                                               List<MultipartFile> images, List<String> imageDescriptions, 
                                               Integer primaryImageIndex) {
        
        // Vérifier que la catégorie existe
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID: " + categorieId));

        // Créer le matériel
        Materiel materiel = new Materiel();
        materiel.setName(name);
        materiel.setDescription(description);
        materiel.setPrice(price);
        materiel.setCategorie(categorie);
        materiel.setActive(true);
        materiel.setTotalQuantity(quantity != null ? quantity : 0);
        materiel.setAvailableQuantity(quantity != null ? quantity : 0);
        // Note: marque et modele ne sont pas encore implémentés dans l'entité Materiel
        
        // Gérer les images directement dans l'entité
        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            String primaryImageUrl = null;
            
            for (int i = 0; i < images.size(); i++) {
                MultipartFile image = images.get(i);
                if (image != null && !image.isEmpty()) {
                    try {
                        // Sauvegarder le fichier et obtenir l'URL
                        String imageUrl = fileStorageService.storeImage(image, "images");
                        imageUrls.add(imageUrl);
                        
                        // Définir l'image primaire
                        if (i == primaryImageIndex) {
                            primaryImageUrl = imageUrl;
                        }
                        
                        System.out.println("✅ Image " + i + " uploadée avec succès: " + imageUrl);
                    } catch (Exception e) {
                        System.err.println("❌ Erreur lors de l'upload de l'image " + i + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            // Définir les images dans l'entité
            materiel.setImages(imageUrls);
            if (primaryImageUrl != null) {
                materiel.setPrimaryImageUrl(primaryImageUrl);
            } else if (!imageUrls.isEmpty()) {
                materiel.setPrimaryImageUrl(imageUrls.get(0));
            }
        } else {
            // Image par défaut
            String defaultImageUrl = "https://images.unsplash.com/photo-1519167758481-83f1426c4e7f?w=400&h=300&fit=crop&crop=center";
            materiel.setImages(List.of(defaultImageUrl));
            materiel.setPrimaryImageUrl(defaultImageUrl);
        }

        Materiel savedMateriel = materielRepository.save(materiel);

        // Audit log dans un try-catch séparé pour ne pas affecter la transaction principale
        try {
            auditService.createLog(
                securityUtils.getCurrentUser().orElse(null),
                "MATERIEL_CREATE_WITH_IMAGES",
                "Matériel créé avec images: " + savedMateriel.getName(),
                securityUtils.getCurrentIpAddress()
            );
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création de l'audit log pour le matériel " + savedMateriel.getId() + ": " + e.getMessage());
            e.printStackTrace();
            // Ne pas faire échouer la création du matériel si l'audit échoue
        }

        // Invalider le cache
        cachedListService.evictAllMaterielCaches();

        return MaterielDto.fromEntity(savedMateriel);
    }

    public MaterielDto updateMateriel(Long id, String name, String description, BigDecimal price,
                                     String imageUrl, Long categorieId,
                                     String marque, String modele, Boolean active, Integer quantity) {
        Materiel materiel = materielRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + id));

        // Vérifier que la catégorie existe si fournie
        if (categorieId != null) {
            Categorie categorie = categorieRepository.findById(categorieId)
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID: " + categorieId));
            materiel.setCategorie(categorie);
        }

        String oldName = materiel.getName();

        if (name != null) materiel.setName(name);
        if (description != null) materiel.setDescription(description);
        if (price != null) materiel.setPrice(price);
        // imageUrl supprimé - maintenant géré par ProductImage
        if (active != null) materiel.setActive(active);
        
        // Mettre à jour la quantité si fournie
        if (quantity != null) {
            Integer oldTotalQuantity = materiel.getTotalQuantity();
            Integer oldAvailableQuantity = materiel.getAvailableQuantity();
            Integer difference = quantity - oldTotalQuantity;
            
            materiel.setTotalQuantity(quantity);
            // Ajuster le stock disponible proportionnellement
            Integer newAvailableQuantity = oldAvailableQuantity + difference;
            materiel.setAvailableQuantity(Math.max(0, newAvailableQuantity));
        }

        Materiel savedMateriel = materielRepository.save(materiel);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "MATERIEL_UPDATE",
            "Matériel mis à jour: " + oldName + " -> " + savedMateriel.getName(),
            securityUtils.getCurrentIpAddress()
        );

        // Invalider le cache
        cachedListService.evictAllMaterielCaches();

        return MaterielDto.fromEntity(savedMateriel);
    }

    public void deleteMateriel(Long id, Boolean force) {
        Materiel materiel = materielRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + id));

        // Vérifier s'il y a des réservations
        boolean hasReservations = reservationRepository.existsByMaterielId(id);
        
        if (hasReservations && !Boolean.TRUE.equals(force)) {
            throw new BadRequestException(
                "Impossible de supprimer ce matériel car il a des réservations associées. " +
                "Vous pouvez le désactiver à la place en décochant 'Équipement actif'."
            );
        }

        String materielName = materiel.getName();
        
        // Si force=true et qu'il y a des réservations, les supprimer d'abord
        if (hasReservations && Boolean.TRUE.equals(force)) {
            List<com.afra7kom.backend.entity.Reservation> reservations = reservationRepository.findByMaterielId(id);
            reservationRepository.deleteAll(reservations);
            System.out.println("⚠️ Suppression forcée : " + reservations.size() + " réservation(s) supprimée(s) pour le matériel " + id);
        }
        
        materielRepository.delete(materiel);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            force ? "MATERIEL_DELETE_FORCED" : "MATERIEL_DELETE",
            "Matériel supprimé" + (force ? " (forcé)" : "") + ": " + materielName,
            securityUtils.getCurrentIpAddress()
        );

        // Invalider le cache
        cachedListService.evictAllMaterielCaches();
    }

    /**
     * Vérifier la disponibilité d'un matériel pour une période donnée
     */
    @Transactional(readOnly = true)
    public boolean checkAvailability(Long materielId, LocalDate startDate, LocalDate endDate) {
        // Vérifier que le matériel existe et est actif
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + materielId));
        
        if (!materiel.getActive()) {
            return false;
        }
        
        // Vérifier que le matériel est disponible
        if (!materiel.isAvailable()) {
            return false;
        }
        
        // Vérifier qu'il y a suffisamment de stock
        if (materiel.getAvailableQuantity() <= 0) {
            return false;
        }
        
        // TODO: Ajouter la logique de vérification des réservations existantes
        // Pour l'instant, on retourne true si le matériel est actif et disponible
        return true;
    }

    /**
     * Toggle active status (optimisé pour performance)
     */
    @Transactional
    public MaterielDto toggleActive(Long id) {
        Materiel materiel = materielRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + id));

        // Inverser le statut
        materiel.setActive(!materiel.getActive());
        Materiel savedMateriel = materielRepository.save(materiel);

        // Invalider seulement le cache de ce matériel
        cachedListService.evictMaterielDetails(id);

        return MaterielDto.fromEntity(savedMateriel);
    }

    /**
     * Mettre à jour le stock total d'un matériel
     */
    @Transactional
    public MaterielDto updateStock(Long id, Integer newQuantity) {
        Materiel materiel = materielRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + id));

        Integer oldTotalQuantity = materiel.getTotalQuantity();
        Integer oldAvailableQuantity = materiel.getAvailableQuantity();
        
        // Calculer la différence
        Integer difference = newQuantity - oldTotalQuantity;
        
        // Mettre à jour le stock total
        materiel.setTotalQuantity(newQuantity);
        
        // Ajuster le stock disponible proportionnellement
        Integer newAvailableQuantity = oldAvailableQuantity + difference;
        // S'assurer que le stock disponible ne devient pas négatif
        materiel.setAvailableQuantity(Math.max(0, newAvailableQuantity));
        
        Materiel savedMateriel = materielRepository.save(materiel);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "MATERIEL_STOCK_UPDATE",
            "Stock mis à jour pour: " + materiel.getName() + 
            " (Total: " + oldTotalQuantity + " → " + newQuantity + 
            ", Disponible: " + oldAvailableQuantity + " → " + savedMateriel.getAvailableQuantity() + ")",
            securityUtils.getCurrentIpAddress()
        );

        // Invalider le cache
        cachedListService.evictMaterielDetails(id);
        cachedListService.evictAllMaterielCaches();

        return MaterielDto.fromEntity(savedMateriel);
    }

    /**
     * Dupliquer un matériel
     */
    @Transactional
    public MaterielDto duplicateMateriel(Long id) {
        Materiel originalMateriel = materielRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + id));

        // Créer une copie du matériel
        Materiel duplicatedMateriel = new Materiel();
        duplicatedMateriel.setName(originalMateriel.getName() + " (Copie)");
        duplicatedMateriel.setDescription(originalMateriel.getDescription());
        duplicatedMateriel.setPrice(originalMateriel.getPrice());
        duplicatedMateriel.setTotalQuantity(originalMateriel.getTotalQuantity());
        duplicatedMateriel.setAvailableQuantity(originalMateriel.getAvailableQuantity());
        duplicatedMateriel.setMinimumStock(originalMateriel.getMinimumStock());
        duplicatedMateriel.setActive(false); // Désactivé par défaut
        duplicatedMateriel.setCategorie(originalMateriel.getCategorie());
        duplicatedMateriel.setActiveImages(originalMateriel.getActiveImages());
        duplicatedMateriel.setPrimaryImageUrl(originalMateriel.getPrimaryImageUrl());

        // Sauvegarder le matériel dupliqué
        Materiel savedMateriel = materielRepository.save(duplicatedMateriel);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "MATERIEL_DUPLICATE",
            "Matériel dupliqué: " + originalMateriel.getName() + " → " + savedMateriel.getName(),
            securityUtils.getCurrentIpAddress()
        );

        cachedListService.evictAllMaterielCaches();

        return MaterielDto.fromEntity(savedMateriel);
    }

    /**
     * Supprimer plusieurs matériels
     */
    @Transactional
    public int bulkDelete(List<Long> ids) {
        int deleted = 0;
        for (Long id : ids) {
            try {
                deleteMateriel(id, false);
                deleted++;
            } catch (Exception e) {
                System.err.println("Erreur lors de la suppression du matériel " + id + ": " + e.getMessage());
            }
        }
        return deleted;
    }

    /**
     * Activer/Désactiver plusieurs matériels
     */
    @Transactional
    public int bulkToggleActive(List<Long> ids, Boolean active) {
        int updated = 0;
        for (Long id : ids) {
            try {
                Materiel materiel = materielRepository.findById(id).orElse(null);
                if (materiel != null) {
                    materiel.setActive(active);
                    materielRepository.save(materiel);
                    updated++;
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la mise à jour du matériel " + id + ": " + e.getMessage());
            }
        }

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "MATERIEL_BULK_TOGGLE",
            updated + " matériel(s) " + (active ? "activé(s)" : "désactivé(s)"),
            securityUtils.getCurrentIpAddress()
        );

        cachedListService.evictAllMaterielCaches();
        return updated;
    }
}

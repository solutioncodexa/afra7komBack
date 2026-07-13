package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.PackDto;
import com.afra7kom.backend.dto.PackDetailDto;
import com.afra7kom.backend.entity.Categorie;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.PackMateriel;
import com.afra7kom.backend.entity.Gallery;
import com.afra7kom.backend.entity.PackImage;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.repository.CategorieRepository;
import com.afra7kom.backend.repository.MaterielRepository;
import com.afra7kom.backend.repository.PackRepository;
import com.afra7kom.backend.repository.PackMaterielRepository;
import com.afra7kom.backend.repository.GalleryRepository;
import com.afra7kom.backend.repository.PackImageRepository;
import com.afra7kom.backend.repository.FavorisRepository;
import com.afra7kom.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;
import java.io.IOException;
import java.util.stream.Collectors;
import com.afra7kom.backend.dto.GalleryDto;
import com.afra7kom.backend.repository.ReservationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;


@Service
@RequiredArgsConstructor
@Transactional
public class PackService {

    private final PackRepository packRepository;
    private final CategorieRepository categorieRepository;
    private final MaterielRepository materielRepository;
    private final PackMaterielRepository packMaterielRepository;
    private final GalleryRepository galleryRepository;
    private final PackImageRepository packImageRepository;
    private final FavorisRepository favorisRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final CachedListService cachedListService;
    private final FileStorageService fileStorageService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    // Injection avec @Lazy pour éviter la dépendance circulaire
    @Autowired
    @Lazy
    private ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public Page<PackDto> getAllPacks(Pageable pageable) {
        return cachedListService.getCachedActivePacks(pageable);
    }

    @Transactional(readOnly = true)
    public Page<PackDto> getPacksByCategorie(Long categorieId, Pageable pageable) {
        return cachedListService.getCachedPacksByCategorie(categorieId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PackDto> searchPacks(String search, Pageable pageable) {
        return cachedListService.getCachedSearchPacks(search, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PackDto> searchPacksWithFilters(Boolean active, Long categorieId, String search, 
                                               BigDecimal minPrice, BigDecimal maxPrice, 
                                               Pageable pageable) {
        // Si pas de recherche textuelle, utiliser le cache
        if (search == null || search.trim().isEmpty()) {
            return cachedListService.getCachedPacksWithFilters(active, categorieId, minPrice, maxPrice, pageable);
        }
        // Sinon, requête directe (la recherche textuelle est moins cacheable)
        Page<Pack> packs = packRepository.findWithFilters(active, categorieId, search, minPrice, maxPrice, pageable);
        return packs.map(PackDto::fromEntitySimple);
    }

    @Transactional(readOnly = true)
    public PackDto getPackById(Long id) {
        // Utiliser le cache pour les packs individuels
        return cachedListService.getCachedPackDetails(id);
    }

    @Transactional(readOnly = true)
    public PackDetailDto getPackDetailById(Long id) {
        // Utiliser le cache pour les détails du pack
        return cachedListService.getCachedPackDetailDto(id);
    }

    @Transactional(readOnly = true)
    public PackDto getPackByIdForUser(Long id, Long userId) {
        Pack pack = packRepository.findByIdWithMaterielsAndImages(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + id));
        
        // Les images sont maintenant stockées directement dans l'entité
        
        boolean isFavorite = userId != null && favorisRepository.existsByUserIdAndPackId(userId, id);
        return PackDto.fromEntityWithFavorite(pack, isFavorite);
    }

    @Transactional(readOnly = true)
    public PackDetailDto getPackDetailByIdForUser(Long id, Long userId) {
        Pack pack = packRepository.findByIdWithMaterielsAndImages(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + id));
        
        // Les images sont maintenant stockées directement dans l'entité
        
        boolean isFavorite = userId != null && favorisRepository.existsByUserIdAndPackId(userId, id);
        return PackDetailDto.fromEntityWithFavorite(pack, isFavorite);
    }

    public PackDto createPack(String name, String description, BigDecimal price, String imageUrl,
                             Long categorieId,
                             List<Map<String, Object>> materiels,
                             List<Long> galleryIds,
                             com.afra7kom.backend.entity.PackType type) {
        
        // Vérifier que la catégorie existe
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID: " + categorieId));

        Pack pack = new Pack();
        pack.setName(name);
        pack.setDescription(description);
        pack.setPrice(price);
        // imageUrl supprimé - maintenant géré par ProductImage
        pack.setCategorie(categorie);
        pack.setActive(true);
        pack.setType(type != null ? type : com.afra7kom.backend.entity.PackType.PACK);

        Pack savedPack = packRepository.save(pack);

        // TODO: Gérer les images via la nouvelle structure List<String>
        // Les images sont maintenant gérées directement dans l'entité Pack

        // Ajouter les matériels au pack
        if (materiels != null && !materiels.isEmpty()) {
            addMaterielsToPack(savedPack, materiels);
        }

        // Ajouter les galeries au pack
        if (galleryIds != null && !galleryIds.isEmpty()) {
            addGalleriesToPack(savedPack, galleryIds);
        }

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "PACK_CREATE",
            "Pack créé: " + savedPack.getName(),
            securityUtils.getCurrentIpAddress()
        );

        // Invalider le cache
        cachedListService.evictAllPackCaches();

        return PackDto.fromEntity(savedPack);
    }

    public PackDto createPackWithImages(String name, String description, BigDecimal price, Long categorieId,
                                       String materielsJson, String galleryIdsJson,
                                       List<MultipartFile> images, List<String> imageDescriptions, 
                                       Integer primaryImageIndex, com.afra7kom.backend.entity.PackType type) {
        
        // Vérifier que la catégorie existe
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID: " + categorieId));

        // Créer le pack
        Pack pack = new Pack();
        pack.setName(name);
        pack.setDescription(description);
        pack.setPrice(price);
        pack.setCategorie(categorie);
        pack.setActive(true);
        pack.setType(type != null ? type : com.afra7kom.backend.entity.PackType.PACK);
        
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
            pack.setImagesList(imageUrls);
            if (primaryImageUrl != null) {
                pack.setPrimaryImageUrl(primaryImageUrl);
            } else if (!imageUrls.isEmpty()) {
                pack.setPrimaryImageUrl(imageUrls.get(0));
            }
        } else {
            // Image par défaut
            String defaultImageUrl = "https://images.unsplash.com/photo-1519167758481-83f1426c4e7f?w=400&h=300&fit=crop&crop=center";
            pack.setImagesList(List.of(defaultImageUrl));
            pack.setPrimaryImageUrl(defaultImageUrl);
        }

        Pack savedPack = packRepository.save(pack);

        // Ajouter les matériels au pack si fournis
        if (materielsJson != null && !materielsJson.trim().isEmpty()) {
            try {
                System.out.println("📦 Parsing matériels JSON: " + materielsJson);
                
                // Parser le JSON des matériels
                List<Map<String, Object>> materiels = parseMaterielsJson(materielsJson);
                System.out.println("✅ " + materiels.size() + " matériel(s) parsé(s)");
                
                // Ajouter les matériels au pack
                addMaterielsToPack(savedPack, materiels);
                System.out.println("✅ Matériels ajoutés au pack " + savedPack.getId());
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de l'ajout des matériels au pack " + savedPack.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Ajouter les galeries au pack si fournies
        if (galleryIdsJson != null && !galleryIdsJson.trim().isEmpty()) {
            try {
                System.out.println("🖼️ Parsing galeries JSON: " + galleryIdsJson);
                
                // Parser le JSON des galeries
                List<Long> galleryIds = parseGalleryIdsJson(galleryIdsJson);
                System.out.println("✅ " + galleryIds.size() + " galerie(s) parsée(s)");
                
                // Ajouter les galeries au pack
                addGalleriesToPack(savedPack, galleryIds);
                System.out.println("✅ Galeries ajoutées au pack " + savedPack.getId());
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de l'ajout des galeries au pack " + savedPack.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Audit log dans un try-catch séparé pour ne pas affecter la transaction principale
        try {
            auditService.createLog(
                securityUtils.getCurrentUser().orElse(null),
                "PACK_CREATE_WITH_IMAGES",
                "Pack créé avec images: " + savedPack.getName(),
                securityUtils.getCurrentIpAddress()
            );
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création de l'audit log pour le pack " + savedPack.getId() + ": " + e.getMessage());
            e.printStackTrace();
            // Ne pas faire échouer la création du pack si l'audit échoue
        }

        // Invalider le cache
        cachedListService.evictAllPackCaches();

        return PackDto.fromEntity(savedPack);
    }

    public PackDto updatePack(Long id, String name, String description, BigDecimal price, String imageUrl,
                             Long categorieId, Boolean active,
                             List<Map<String, Object>> materiels,
                             List<Long> galleryIds, com.afra7kom.backend.entity.PackType type,
                             List<String> images, String primaryImage) {
        
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + id));

        String oldName = pack.getName();

        if (name != null) pack.setName(name);
        if (description != null) pack.setDescription(description);
        if (price != null) pack.setPrice(price);
        // imageUrl supprimé - maintenant géré par ProductImage
        if (active != null) pack.setActive(active);
        if (type != null) pack.setType(type);

        if (categorieId != null) {
            Categorie categorie = categorieRepository.findById(categorieId)
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID: " + categorieId));
            pack.setCategorie(categorie);
        }

        // Mettre à jour les images si fournies
        if (images != null) {
            pack.setActiveImages(images);
            System.out.println("✅ Images mises à jour: " + images.size() + " image(s)");
        }
        
        // Mettre à jour l'image primaire si fournie
        if (primaryImage != null) {
            pack.setPrimaryImage(primaryImage);
            System.out.println("✅ Image primaire mise à jour: " + primaryImage);
        }

        // Sauvegarder d'abord les modifications de base
        Pack savedPack = packRepository.save(pack);

        // Mettre à jour les matériels uniquement si fournis
        if (materiels != null) {
            // Toujours supprimer les anciens matériels
            packMaterielRepository.deleteByPackId(id);
            entityManager.flush(); // Forcer l'exécution immédiate de la suppression
            
            // Ajouter les nouveaux matériels (si la liste n'est pas vide)
            if (!materiels.isEmpty()) {
                addMaterielsToPack(savedPack, materiels);
                System.out.println("✅ Matériels mis à jour: " + materiels.size() + " matériel(s)");
            } else {
                System.out.println("✅ Tous les matériels ont été supprimés");
            }
        }

        // Mettre à jour les galeries uniquement si fournies
        if (galleryIds != null) {
            // Supprimer les anciennes galeries
            packImageRepository.deleteByPackId(id);
            
            // Ajouter les nouvelles galeries
            addGalleriesToPack(savedPack, galleryIds);
        }

        // Audit log asynchrone pour ne pas ralentir
        try {
            auditService.createLog(
                securityUtils.getCurrentUser().orElse(null),
                "PACK_UPDATE",
                "Pack mis à jour: " + oldName + " -> " + savedPack.getName(),
                securityUtils.getCurrentIpAddress()
            );
        } catch (Exception e) {
            // Ne pas bloquer si l'audit échoue
            System.err.println("Erreur audit log: " + e.getMessage());
        }

        // Invalider seulement le cache de ce pack spécifique
        cachedListService.evictPackDetails(id);

        return PackDto.fromEntity(savedPack);
    }

    public PackDto updatePackWithImages(Long id, String name, String description, BigDecimal price, 
                                       Long categorieId, Boolean active, String materielsJson, 
                                       String galleryIdsJson, List<MultipartFile> images, 
                                       String existingImagesJson, Integer primaryImageIndex, 
                                       com.afra7kom.backend.entity.PackType type) {
        
        // Récupérer le pack existant
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + id));

        String oldName = pack.getName();

        // Mettre à jour les champs de base si fournis
        if (name != null) pack.setName(name);
        if (description != null) pack.setDescription(description);
        if (price != null) pack.setPrice(price);
        if (active != null) pack.setActive(active);
        if (type != null) pack.setType(type);

        if (categorieId != null) {
            Categorie categorie = categorieRepository.findById(categorieId)
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID: " + categorieId));
            pack.setCategorie(categorie);
        }

        // Gérer les images
        List<String> allImageUrls = new ArrayList<>();
        
        // 1. Conserver les images existantes (celles qui n'ont pas été supprimées)
        if (existingImagesJson != null && !existingImagesJson.trim().isEmpty()) {
            try {
                List<String> existingImages = parseExistingImagesJson(existingImagesJson);
                allImageUrls.addAll(existingImages);
                System.out.println("✅ " + existingImages.size() + " image(s) existante(s) conservée(s)");
            } catch (Exception e) {
                System.err.println("❌ Erreur lors du parsing des images existantes: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 2. Uploader les nouvelles images en parallèle (optimisation)
        if (images != null && !images.isEmpty()) {
            List<String> newImageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    try {
                        String imageUrl = fileStorageService.storeImage(image, "images");
                        newImageUrls.add(imageUrl);
                    } catch (Exception e) {
                        System.err.println("❌ Erreur upload: " + e.getMessage());
                    }
                }
            }
            allImageUrls.addAll(newImageUrls);
            System.out.println("✅ " + newImageUrls.size() + " nouvelle(s) image(s) uploadée(s)");
        }
        
        // 3. Définir toutes les images dans l'entité
        if (!allImageUrls.isEmpty()) {
            pack.setImagesList(allImageUrls);
            
            // 4. Définir l'image primaire
            String primaryImageUrl = null;
            if (primaryImageIndex != null && primaryImageIndex >= 0 && primaryImageIndex < allImageUrls.size()) {
                primaryImageUrl = allImageUrls.get(primaryImageIndex);
            } else if (!allImageUrls.isEmpty()) {
                primaryImageUrl = allImageUrls.get(0); // Par défaut, la première image
            }
            
            if (primaryImageUrl != null) {
                pack.setPrimaryImageUrl(primaryImageUrl);
                System.out.println("✅ Image primaire définie: " + primaryImageUrl);
            }
        }

        // Sauvegarder d'abord le pack (plus rapide)
        Pack savedPack = packRepository.save(pack);

        // Mettre à jour les matériels UNIQUEMENT si fournis
        if (materielsJson != null) {
            try {
                packMaterielRepository.deleteByPackId(id);
                
                String cleanJson = materielsJson.trim();
                if (!cleanJson.isEmpty() && !cleanJson.equals("[]")) {
                    List<Map<String, Object>> materiels = parseMaterielsJson(materielsJson);
                    addMaterielsToPack(savedPack, materiels);
                    System.out.println("✅ " + materiels.size() + " matériel(s) mis à jour");
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur matériels: " + e.getMessage());
            }
        }

        // Mettre à jour les galeries UNIQUEMENT si fournies
        if (galleryIdsJson != null && !galleryIdsJson.trim().isEmpty()) {
            try {
                packImageRepository.deleteByPackId(id);
                List<Long> galleryIds = parseGalleryIdsJson(galleryIdsJson);
                addGalleriesToPack(savedPack, galleryIds);
            } catch (Exception e) {
                System.err.println("❌ Erreur galeries: " + e.getMessage());
            }
        }

        // Audit asynchrone
        try {
            auditService.createLog(
                securityUtils.getCurrentUser().orElse(null),
                "PACK_UPDATE",
                "Pack mis à jour: " + oldName,
                securityUtils.getCurrentIpAddress()
            );
        } catch (Exception e) {
            System.err.println("Erreur audit: " + e.getMessage());
        }

        // Cache spécifique uniquement
        cachedListService.evictPackDetails(id);
        // Évacuer aussi les listes de packs pour éviter des résultats obsolètes
        cachedListService.evictAllPackCaches();

        return PackDto.fromEntity(savedPack);
    }

    public void deletePack(Long id, Boolean force) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + id));

        // Vérifier s'il y a des réservations
        boolean hasReservations = reservationRepository.existsByPackId(id);
        
        if (hasReservations && !Boolean.TRUE.equals(force)) {
            throw new BadRequestException(
                "Impossible de supprimer ce pack car il a des réservations associées. " +
                "Vous pouvez le désactiver à la place en décochant 'Équipement actif'."
            );
        }

        String packName = pack.getName();
        
        // Si force=true et qu'il y a des réservations, les supprimer d'abord
        if (hasReservations && Boolean.TRUE.equals(force)) {
            List<com.afra7kom.backend.entity.Reservation> reservations = reservationRepository.findByPackId(id);
            reservationRepository.deleteAll(reservations);
            entityManager.flush(); // Forcer la suppression immédiate
            System.out.println("⚠️ Suppression forcée : " + reservations.size() + " réservation(s) supprimée(s) pour le pack " + id);
        }
        
        packRepository.delete(pack);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            force ? "PACK_DELETE_FORCED" : "PACK_DELETE",
            "Pack supprimé" + (force ? " (forcé)" : "") + ": " + packName,
            securityUtils.getCurrentIpAddress()
        );

        // Invalider le cache
        cachedListService.evictAllPackCaches();
    }

    @Transactional(readOnly = true)
    public Page<PackDto> getMostFavoritedPacks(Pageable pageable) {
        return packRepository.findMostFavorited(pageable)
                .map(PackDto::fromEntitySimple);
    }

    @Transactional(readOnly = true)
    public Page<PackDto> getMostRentedPacks(Pageable pageable) {
        return packRepository.findMostRented(pageable)
                .map(PackDto::fromEntitySimple);
    }

    @Transactional(readOnly = true)
    public Page<PackDto> getPacksWithGalleries(Pageable pageable) {
        return packRepository.findByActiveTrue(pageable)
                .map(pack -> {
                    PackDto dto = PackDto.fromEntitySimple(pack);
                    // Charger les galeries pour chaque pack
                    List<PackImage> packImages = packImageRepository.findByPackIdOrderBySortOrder(pack.getId());
                    dto.setTotalGalleries(packImages.size());
                    return dto;
                });
    }

    /**
     * Upload d'images pour un pack
     */
    public List<String> uploadImages(Long packId, MultipartFile[] files) {
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + packId));

        List<String> uploadedUrls = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                String filePath = fileStorageService.storeImage(file, "images");
                
                // Créer une entrée dans la galerie
                Gallery gallery = new Gallery();
                gallery.setTitle("Image pour " + pack.getName());
                gallery.setDescription("Image uploadée pour le pack " + pack.getName());
                gallery.setImageUrl(filePath);
                gallery.setActive(true);
                gallery.setCreatedBy(securityUtils.getCurrentUser().orElse(null));
                
                Gallery savedGallery = galleryRepository.save(gallery);
                
                // Créer la relation PackImage
                PackImage packImage = new PackImage(pack, savedGallery);
                packImageRepository.save(packImage);
                
                uploadedUrls.add(filePath);
                
            } catch (Exception e) {
                throw new BadRequestException("Erreur lors de l'upload de l'image: " + e.getMessage());
            }
        }
        
        return uploadedUrls;
    }

    /**
     * Définir une image comme principale
     */
    public String setMainImage(Long packId, Long imageId) {
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + packId));
        
        Gallery gallery = galleryRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image non trouvée avec l'ID: " + imageId));
        
        // Mettre à jour l'image principale du pack via la nouvelle structure
        try {
            // Mettre à jour l'image principale du pack
            pack.setPrimaryImageUrl(gallery.getImageUrl());
            
            // Ajouter l'image à la liste des images si elle n'y est pas déjà
            List<String> currentImages = pack.getActiveImages();
            if (!currentImages.contains(gallery.getImageUrl())) {
                currentImages.add(gallery.getImageUrl());
                pack.setImagesList(currentImages);
            }
            
            packRepository.save(pack);
            return gallery.getImageUrl();
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour de l'image du pack: " + e.getMessage());
            return gallery.getImageUrl();
        }
    }

    /**
     * Supprimer une image d'un pack
     */
    public void deleteImage(Long packId, Long imageId) {
        // Vérifier que le pack existe
        if (!packRepository.existsById(packId)) {
            throw new ResourceNotFoundException("Pack non trouvé avec l'ID: " + packId);
        }
        
        Gallery gallery = galleryRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image non trouvée avec l'ID: " + imageId));
        
        // Supprimer la relation PackImage
        packImageRepository.deleteByPackIdAndGalleryId(packId, imageId);
        
        // Supprimer l'image de la galerie
        galleryRepository.delete(gallery);
        
        // Si c'était l'image principale, la gérer via ProductImage
        // Note: La gestion des images principales se fait maintenant via ProductImageService
        // Plus besoin de gérer setImageUrl/getImageUrl sur l'entité Pack
    }

    /**
     * Récupérer toutes les images d'un pack
     */
    @Transactional(readOnly = true)
    public List<GalleryDto> getPackImages(Long packId) {
        List<PackImage> packImages = packImageRepository.findByPackIdWithGallery(packId);
        return packImages.stream()
                .map(pi -> GalleryDto.fromEntity(pi.getGallery()))
                .collect(Collectors.toList());
    }

    private void addMaterielsToPack(Pack pack, List<Map<String, Object>> materiels) {
        for (Map<String, Object> materielData : materiels) {
            Long materielId = Long.valueOf(materielData.get("materielId").toString());
            Integer quantity = Integer.valueOf(materielData.get("quantity").toString());
            Boolean isOptional = materielData.containsKey("isOptional") ? 
                Boolean.valueOf(materielData.get("isOptional").toString()) : false;
            String notes = materielData.containsKey("notes") ? 
                materielData.get("notes").toString() : null;

            Materiel materiel = materielRepository.findById(materielId)
                    .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + materielId));

            PackMateriel packMateriel = new PackMateriel(pack, materiel, quantity, isOptional);
            packMateriel.setNotes(notes);
            
            packMaterielRepository.save(packMateriel);
        }
    }

    private void addGalleriesToPack(Pack pack, List<Long> galleryIds) {
        for (int i = 0; i < galleryIds.size(); i++) {
            Long galleryId = galleryIds.get(i);
            Gallery gallery = galleryRepository.findById(galleryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Galerie non trouvée avec l'ID: " + galleryId));

            PackImage packImage = new PackImage(pack, gallery, i);
            packImageRepository.save(packImage);
        }
    }

    /**
     * Vérifier la disponibilité d'un pack pour une période donnée
     */
    @Transactional(readOnly = true)
    public boolean checkAvailability(Long packId, LocalDate startDate, LocalDate endDate) {
        // Vérifier que le pack existe et est actif
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + packId));
        
        if (!pack.getActive()) {
            return false;
        }
        
        // Vérifier la disponibilité de tous les matériels du pack
        for (PackMateriel packMateriel : pack.getPackMateriels()) {
            Materiel materiel = packMateriel.getMateriel();
            
            // Vérifier que le matériel est actif et disponible
            if (!materiel.getActive() || !materiel.isAvailable()) {
                return false;
            }
            
            // Vérifier qu'il y a suffisamment de stock pour la quantité requise
            if (materiel.getAvailableQuantity() < packMateriel.getQuantity()) {
                return false;
            }
        }
        
        // TODO: Ajouter la logique de vérification des réservations existantes
        // Pour l'instant, on retourne true si tous les matériels du pack sont disponibles
        return true;
    }
    
    /**
     * Parser le JSON des matériels
     * Format attendu: [{"materielId": 1, "quantity": 10}, {"materielId": 2, "quantity": 5}]
     */
    private List<Map<String, Object>> parseMaterielsJson(String materielsJson) {
        List<Map<String, Object>> materiels = new ArrayList<>();
        
        try {
            // Parse simple du JSON manuellement
            String cleanJson = materielsJson.trim();
            
            // Retirer les crochets externes
            if (cleanJson.startsWith("[") && cleanJson.endsWith("]")) {
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1).trim();
            }
            
            if (cleanJson.isEmpty()) {
                return materiels;
            }
            
            // Séparer les objets JSON
            String[] objects = cleanJson.split("\\},\\s*\\{");
            
            for (String obj : objects) {
                // Nettoyer l'objet
                obj = obj.replace("{", "").replace("}", "").trim();
                
                Map<String, Object> materiel = new HashMap<>();
                
                // Parser les propriétés
                String[] properties = obj.split(",");
                for (String property : properties) {
                    String[] keyValue = property.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replace("\"", "");
                        String value = keyValue[1].trim();
                        
                        if (key.equals("materielId")) {
                            materiel.put(key, Long.parseLong(value));
                        } else if (key.equals("quantity")) {
                            materiel.put(key, Integer.parseInt(value));
                        } else if (key.equals("isOptional")) {
                            materiel.put(key, Boolean.parseBoolean(value));
                        } else {
                            materiel.put(key, value.replace("\"", ""));
                        }
                    }
                }
                
                if (!materiel.isEmpty()) {
                    materiels.add(materiel);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing du JSON des matériels: " + e.getMessage());
            e.printStackTrace();
        }
        
        return materiels;
    }
    
    /**
     * Parser le JSON des IDs de galeries
     * Format attendu: [1, 2, 3]
     */
    private List<Long> parseGalleryIdsJson(String galleryIdsJson) {
        List<Long> galleryIds = new ArrayList<>();
        
        try {
            String cleanJson = galleryIdsJson.trim();
            
            // Retirer les crochets
            if (cleanJson.startsWith("[") && cleanJson.endsWith("]")) {
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1).trim();
            }
            
            if (cleanJson.isEmpty()) {
                return galleryIds;
            }
            
            // Séparer les IDs
            String[] ids = cleanJson.split(",");
            for (String id : ids) {
                try {
                    galleryIds.add(Long.parseLong(id.trim()));
                } catch (NumberFormatException e) {
                    System.err.println("ID de galerie invalide: " + id);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing du JSON des galeries: " + e.getMessage());
            e.printStackTrace();
        }
        
        return galleryIds;
    }
    
    /**
     * Parser le JSON des images existantes
     * Format attendu: ["url1", "url2", "url3"] ou url1,url2,url3
     */
    private List<String> parseExistingImagesJson(String existingImagesJson) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            String cleanJson = existingImagesJson.trim();
            
            // Retirer les crochets si présents
            if (cleanJson.startsWith("[") && cleanJson.endsWith("]")) {
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1).trim();
            }
            
            if (cleanJson.isEmpty()) {
                return imageUrls;
            }
            
            // Séparer les URLs
            String[] urls = cleanJson.split(",");
            for (String url : urls) {
                String cleanUrl = url.trim();
                // Retirer les guillemets si présents
                if (cleanUrl.startsWith("\"") && cleanUrl.endsWith("\"")) {
                    cleanUrl = cleanUrl.substring(1, cleanUrl.length() - 1);
                }
                if (!cleanUrl.isEmpty()) {
                    imageUrls.add(cleanUrl);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing du JSON des images existantes: " + e.getMessage());
            e.printStackTrace();
        }
        
        return imageUrls;
    }

    /**
     * Toggle active status (optimisé pour performance)
     */
    @Transactional
    public PackDto toggleActive(Long id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + id));

        // Inverser le statut
        pack.setActive(!pack.getActive());
        Pack savedPack = packRepository.save(pack);

        // Invalider seulement le cache de ce pack
        cachedListService.evictPackDetails(id);

        return PackDto.fromEntitySimple(savedPack);
    }

    /**
     * Dupliquer un pack (OPTIMISÉ)
     */
    @Transactional
    public PackDto duplicatePack(Long id) {
        // Charger SEULEMENT les champs nécessaires (pas de EAGER fetch)
        Pack originalPack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + id));

        // Créer une copie du pack
        Pack duplicatedPack = new Pack();
        duplicatedPack.setName(originalPack.getName() + " (Copie)");
        duplicatedPack.setDescription(originalPack.getDescription());
        duplicatedPack.setPrice(originalPack.getPrice());
        duplicatedPack.setActive(false); // Désactivé par défaut
        duplicatedPack.setType(originalPack.getType());
        duplicatedPack.setCategorie(originalPack.getCategorie());
        duplicatedPack.setImages(originalPack.getImages());
        duplicatedPack.setPrimaryImageUrl(originalPack.getPrimaryImageUrl());

        // Sauvegarder le pack dupliqué
        Pack savedPack = packRepository.save(duplicatedPack);

        // Dupliquer les matériels en BATCH (plus rapide)
        List<PackMateriel> originalMateriels = packMaterielRepository.findByPackId(id);
        if (!originalMateriels.isEmpty()) {
            List<PackMateriel> duplicatedMateriels = new ArrayList<>();
            for (PackMateriel pm : originalMateriels) {
                PackMateriel duplicatedPM = new PackMateriel();
                duplicatedPM.setPack(savedPack);
                duplicatedPM.setMateriel(pm.getMateriel());
                duplicatedPM.setQuantity(pm.getQuantity());
                duplicatedMateriels.add(duplicatedPM);
            }
            // Save all en une seule opération (batch)
            packMaterielRepository.saveAll(duplicatedMateriels);
            System.out.println("✅ " + duplicatedMateriels.size() + " matériel(s) dupliqué(s) en batch");
        }

        // Audit log asynchrone
        try {
            auditService.createLog(
                securityUtils.getCurrentUser().orElse(null),
                "PACK_DUPLICATE",
                "Pack dupliqué: " + originalPack.getName(),
                securityUtils.getCurrentIpAddress()
            );
        } catch (Exception e) {
            System.err.println("Erreur audit: " + e.getMessage());
        }

        // Invalider le cache du pack et les listes liées
        cachedListService.evictPackDetails(id);
        cachedListService.evictAllPackCaches();

        return PackDto.fromEntitySimple(savedPack);
    }

    /**
     * Supprimer plusieurs packs
     */
    @Transactional
    public int bulkDelete(List<Long> ids) {
        int deleted = 0;
        for (Long id : ids) {
            try {
                deletePack(id, false);
                deleted++;
            } catch (Exception e) {
                System.err.println("Erreur lors de la suppression du pack " + id + ": " + e.getMessage());
            }
        }
        return deleted;
    }

    /**
     * Activer/Désactiver plusieurs packs
     */
    @Transactional
    public int bulkToggleActive(List<Long> ids, Boolean active) {
        int updated = 0;
        for (Long id : ids) {
            try {
                Pack pack = packRepository.findById(id).orElse(null);
                if (pack != null) {
                    pack.setActive(active);
                    packRepository.save(pack);
                    updated++;
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la mise à jour du pack " + id + ": " + e.getMessage());
            }
        }

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "PACK_BULK_TOGGLE",
            updated + " pack(s) " + (active ? "activé(s)" : "désactivé(s)"),
            securityUtils.getCurrentIpAddress()
        );

        cachedListService.evictAllPackCaches();
        return updated;
    }
}




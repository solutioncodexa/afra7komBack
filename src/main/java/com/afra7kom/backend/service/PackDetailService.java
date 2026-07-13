package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.PackCreateDto;
import com.afra7kom.backend.dto.PackDetailDto;
import com.afra7kom.backend.entity.*;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import com.afra7kom.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
@Transactional
public class PackDetailService {

    @Autowired
    private PackRepository packRepository;

    @Autowired
    private PackImageRepository packImageRepository;

    @Autowired
    private PackMaterielRepository packMaterielRepository;

    @Autowired
    private GalleryRepository galleryRepository;

    @Autowired
    private MaterielRepository materielRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    public PackDetailDto getPackById(Long id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found with id: " + id));
        
        // Charger explicitement les relations
        if (pack.getPackMateriels() != null) {
            pack.getPackMateriels().size(); // Force le chargement
        }
        if (pack.getImages() != null) {
            pack.getActiveImages().size(); // Force le chargement
        }
        
        return PackDetailDto.fromEntity(pack);
    }

    public List<PackDetailDto> getAllPacks() {
        return packRepository.findAll().stream()
                .map(PackDetailDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<PackDetailDto> getAllPacksPaginated(Pageable pageable) {
        Page<Pack> packsPage = packRepository.findAll(pageable);
        
        // Charger explicitement les relations pour chaque pack
        packsPage.getContent().forEach(pack -> {
            if (pack.getPackMateriels() != null) {
                pack.getPackMateriels().size(); // Force le chargement
            }
            if (pack.getImages() != null) {
                pack.getActiveImages().size(); // Force le chargement
            }
        });
        
        return packsPage.map(PackDetailDto::fromEntity);
    }

    public PackDetailDto createPack(PackCreateDto packCreateDto) {
        // Créer le pack
        Pack pack = new Pack();
        pack.setName(packCreateDto.getName());
        pack.setDescription(packCreateDto.getDescription());
        pack.setPrice(packCreateDto.getPrice());
        // imageUrl supprimé - maintenant géré par ProductImage
        pack.setActive(packCreateDto.getActive());
        pack.setType(packCreateDto.getType() != null ? packCreateDto.getType() : PackType.PACK);

        // Associer la catégorie
        if (packCreateDto.getCategoryId() != null) {
            Categorie categorie = categorieRepository.findById(packCreateDto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            pack.setCategorie(categorie);
        }

        pack = packRepository.save(pack);

        // Créer automatiquement une image pour le nouveau pack
        // TODO: Gérer les images via la nouvelle structure List<String>
        // Les images sont maintenant gérées directement dans l'entité Pack

        // Associer les images
        if (packCreateDto.getImageIds() != null && !packCreateDto.getImageIds().isEmpty()) {
            for (int i = 0; i < packCreateDto.getImageIds().size(); i++) {
                Long imageId = packCreateDto.getImageIds().get(i);
                Gallery gallery = galleryRepository.findById(imageId)
                        .orElseThrow(() -> new ResourceNotFoundException("Gallery image not found"));
                
                PackImage packImage = new PackImage(pack, gallery, i);
                packImageRepository.save(packImage);
            }
        }

        // Associer les matériels avec validation des doublons
        if (packCreateDto.getMaterials() != null && !packCreateDto.getMaterials().isEmpty()) {
            // Vérifier les doublons dans la liste des matériels
            List<Long> materielIds = packCreateDto.getMaterials().stream()
                    .map(PackCreateDto.PackMaterielCreateDto::getMaterielId)
                    .collect(Collectors.toList());
            
            // Trouver les IDs qui apparaissent plus d'une fois
            Set<Long> uniqueIds = new HashSet<>();
            List<Long> duplicateIds = materielIds.stream()
                    .filter(materielId -> !uniqueIds.add(materielId)) // Si add() retourne false, c'est un doublon
                    .distinct()
                    .collect(Collectors.toList());
            
            if (!duplicateIds.isEmpty()) {
                throw new IllegalArgumentException("Les matériels suivants sont dupliqués: " + duplicateIds);
            }
            
            // Ajouter les matériels
            for (PackCreateDto.PackMaterielCreateDto materialDto : packCreateDto.getMaterials()) {
                Materiel materiel = materielRepository.findById(materialDto.getMaterielId())
                        .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialDto.getMaterielId()));
                
                PackMateriel packMateriel = new PackMateriel();
                packMateriel.setPack(pack);
                packMateriel.setMateriel(materiel);
                packMateriel.setQuantity(materialDto.getQuantity());
                packMaterielRepository.save(packMateriel);
            }
        }

        return getPackById(pack.getId());
    }

    public PackDetailDto updatePack(Long id, PackCreateDto packCreateDto) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found with id: " + id));

        pack.setName(packCreateDto.getName());
        pack.setDescription(packCreateDto.getDescription());
        pack.setPrice(packCreateDto.getPrice());
        // imageUrl supprimé - maintenant géré par ProductImage
        pack.setActive(packCreateDto.getActive());
        if (packCreateDto.getType() != null) {
            pack.setType(packCreateDto.getType());
        }

        // Mettre à jour la catégorie
        if (packCreateDto.getCategoryId() != null) {
            Categorie categorie = categorieRepository.findById(packCreateDto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            pack.setCategorie(categorie);
        }

        pack = packRepository.save(pack);

        // Gérer l'image du pack via ProductImage
        // TODO: Gérer les images via la nouvelle structure List<String>
        // Les images sont maintenant gérées directement dans l'entité Pack

        // Supprimer TOUTES les anciennes associations de manière forcée
        List<PackMateriel> existingPackMateriels = packMaterielRepository.findByPackId(id);
        System.out.println("Suppression de " + existingPackMateriels.size() + " associations PackMateriel existantes");
        for (PackMateriel existing : existingPackMateriels) {
            packMaterielRepository.delete(existing);
        }
        packMaterielRepository.flush(); // Forcer la synchronisation avec la base de données
        
        List<PackImage> existingPackImages = packImageRepository.findByPackId(id);
        System.out.println("Suppression de " + existingPackImages.size() + " associations PackImage existantes");
        for (PackImage existing : existingPackImages) {
            packImageRepository.delete(existing);
        }
        packImageRepository.flush(); // Forcer la synchronisation avec la base de données

        // Associer les nouvelles images
        if (packCreateDto.getImageIds() != null && !packCreateDto.getImageIds().isEmpty()) {
            for (int i = 0; i < packCreateDto.getImageIds().size(); i++) {
                Long imageId = packCreateDto.getImageIds().get(i);
                Gallery gallery = galleryRepository.findById(imageId)
                        .orElseThrow(() -> new ResourceNotFoundException("Gallery image not found"));
                
                PackImage packImage = new PackImage(pack, gallery, i);
                packImageRepository.save(packImage);
            }
        }

        // Associer les nouveaux matériels avec validation des doublons
        if (packCreateDto.getMaterials() != null && !packCreateDto.getMaterials().isEmpty()) {
            System.out.println("Ajout de " + packCreateDto.getMaterials().size() + " nouveaux matériels");
            
            // Vérifier les doublons dans la liste des matériels
            List<Long> materielIds = packCreateDto.getMaterials().stream()
                    .map(PackCreateDto.PackMaterielCreateDto::getMaterielId)
                    .collect(Collectors.toList());
            
            // Trouver les IDs qui apparaissent plus d'une fois
            Set<Long> uniqueIds = new HashSet<>();
            List<Long> duplicateIds = materielIds.stream()
                    .filter(materielId -> !uniqueIds.add(materielId)) // Si add() retourne false, c'est un doublon
                    .distinct()
                    .collect(Collectors.toList());
            
            if (!duplicateIds.isEmpty()) {
                throw new IllegalArgumentException("Les matériels suivants sont dupliqués: " + duplicateIds);
            }
            
            // Ajouter les matériels
            for (PackCreateDto.PackMaterielCreateDto materialDto : packCreateDto.getMaterials()) {
                Materiel materiel = materielRepository.findById(materialDto.getMaterielId())
                        .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialDto.getMaterielId()));
                
                PackMateriel packMateriel = new PackMateriel();
                packMateriel.setPack(pack);
                packMateriel.setMateriel(materiel);
                packMateriel.setQuantity(materialDto.getQuantity());
                packMaterielRepository.save(packMateriel);
                System.out.println("Ajouté: " + materiel.getName() + " (ID: " + materiel.getId() + ") - Quantité: " + materialDto.getQuantity());
            }
        } else {
            System.out.println("Aucun matériel à ajouter");
        }

        return getPackById(pack.getId());
    }

    public void deletePack(Long id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found with id: " + id));
        
        // Supprimer les associations
        packImageRepository.deleteByPackId(id);
        packMaterielRepository.deleteByPackId(id);
        
        packRepository.delete(pack);
    }

    // Méthode utilitaire pour nettoyer les doublons dans un pack
    public void cleanDuplicateMaterials(Long packId) {
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found with id: " + packId));
        
        // Charger les relations
        pack.getPackMateriels().size();
        
        // Grouper par materielId et garder seulement le premier
        Map<Long, PackMateriel> uniqueMaterials = new HashMap<>();
        List<PackMateriel> toDelete = new ArrayList<>();
        
        for (PackMateriel packMateriel : pack.getPackMateriels()) {
            Long materielId = packMateriel.getMateriel().getId();
            if (uniqueMaterials.containsKey(materielId)) {
                // Doublon trouvé, marquer pour suppression
                toDelete.add(packMateriel);
            } else {
                // Premier occurrence, garder
                uniqueMaterials.put(materielId, packMateriel);
            }
        }
        
        // Supprimer les doublons
        for (PackMateriel duplicate : toDelete) {
            packMaterielRepository.delete(duplicate);
        }
    }

    // Méthode pour dupliquer un pack
    public PackDetailDto duplicatePack(Long packId) {
        Pack originalPack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found with id: " + packId));
        
        // Charger les relations
        if (originalPack.getPackMateriels() != null) {
            originalPack.getPackMateriels().size();
        }
        if (originalPack.getImages() != null) {
            originalPack.getActiveImages().size();
        }
        
        // Créer un nouveau pack avec les mêmes propriétés
        Pack duplicatedPack = new Pack();
        duplicatedPack.setName(originalPack.getName() + " (Copie)");
        duplicatedPack.setDescription(originalPack.getDescription());
        duplicatedPack.setPrice(originalPack.getPrice());
        // imageUrl supprimé - maintenant géré par ProductImage
        duplicatedPack.setActive(false); // Le pack dupliqué est inactif par défaut
        duplicatedPack.setCategorie(originalPack.getCategorie());
        duplicatedPack.setType(originalPack.getType());
        
        // Sauvegarder le nouveau pack
        duplicatedPack = packRepository.save(duplicatedPack);
        
        // Dupliquer les matériels
        if (originalPack.getPackMateriels() != null) {
            for (PackMateriel originalMateriel : originalPack.getPackMateriels()) {
                PackMateriel duplicatedMateriel = new PackMateriel();
                duplicatedMateriel.setPack(duplicatedPack);
                duplicatedMateriel.setMateriel(originalMateriel.getMateriel());
                duplicatedMateriel.setQuantity(originalMateriel.getQuantity());
                packMaterielRepository.save(duplicatedMateriel);
            }
        }
        
        // TODO: Dupliquer les images via la nouvelle structure List<String>
        // Les images sont maintenant gérées directement dans l'entité Pack
        try {
            // Copier les images de l'original vers le dupliqué
            List<String> originalImages = originalPack.getActiveImages();
            if (originalImages != null && !originalImages.isEmpty()) {
                duplicatedPack.setImagesList(new ArrayList<>(originalImages));
                duplicatedPack.setPrimaryImageUrl(originalPack.getPrimaryImageUrl());
                packRepository.save(duplicatedPack);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la duplication des images: " + e.getMessage());
        }
        
        return getPackById(duplicatedPack.getId());
    }

    // Méthode pour activer/désactiver un pack
    public PackDetailDto togglePackStatus(Long packId) {
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found with id: " + packId));
        
        // Basculer le statut
        pack.setActive(!pack.getActive());
        
        // Sauvegarder les modifications
        pack = packRepository.save(pack);
        
        return getPackById(pack.getId());
    }
}


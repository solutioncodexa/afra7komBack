package com.afra7kom.backend.service;

import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.repository.MaterielRepository;
import com.afra7kom.backend.repository.PackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GalleryService {

    private final PackRepository packRepository;
    private final MaterielRepository materielRepository;

    /**
     * Récupère toutes les URLs d'images des items actifs pour la galerie publique
     * Retourne juste les URLs d'images extraites de la colonne images
     */
    public Page<String> getAllActiveItemsImages(Pageable pageable) {
        List<String> allImageUrls = new ArrayList<>();
        
        // Récupérer tous les packs actifs avec leurs images
        // Équivalent SQL: SELECT p.images FROM packs as p WHERE p.images IS NOT NULL AND p.images != '' and p.active = 't'
        Page<Pack> activePacks = packRepository.findByActiveTrue(
            org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 
                Integer.MAX_VALUE, 
                pageable.getSort()
            )
        );
        
        for (Pack pack : activePacks.getContent()) {
            // Extraire toutes les images de ce pack
            List<String> images = pack.getActiveImages();
            if (images != null && !images.isEmpty()) {
                allImageUrls.addAll(images);
            }
        }
        
        // Récupérer tous les matériels actifs avec leurs images
        Page<Materiel> activeMateriels = materielRepository.findByActiveTrue(
            org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 
                Integer.MAX_VALUE, 
                pageable.getSort()
            )
        );
        
        for (Materiel materiel : activeMateriels.getContent()) {
            // Extraire toutes les images de ce matériel
            List<String> images = materiel.getActiveImages();
            if (images != null && !images.isEmpty()) {
                allImageUrls.addAll(images);
            }
        }
        
        // Appliquer la pagination sur la liste complète d'URLs
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allImageUrls.size());
        
        List<String> paginatedUrls = allImageUrls.isEmpty() || start >= allImageUrls.size() 
            ? new ArrayList<>() 
            : allImageUrls.subList(start, end);
        
        // Créer une page avec les URLs récupérées
        return new org.springframework.data.domain.PageImpl<>(
            paginatedUrls, 
            pageable, 
            allImageUrls.size()
        );
    }

    /**
     * Récupère les URLs d'images par type d'item
     * Retourne juste les URLs d'images filtrées par type
     */
    public Page<String> getImageUrlsByType(String type, Pageable pageable) {
        List<String> imageUrls = new ArrayList<>();
        
        if ("MATERIEL".equals(type)) {
            // Récupérer seulement les matériels actifs
            Page<Materiel> materiels = materielRepository.findByActiveTrue(
                org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), 
                    Integer.MAX_VALUE, 
                    pageable.getSort()
                )
            );
            
            for (Materiel materiel : materiels.getContent()) {
                // Extraire toutes les images de ce matériel
                List<String> images = materiel.getActiveImages();
                if (images != null && !images.isEmpty()) {
                    imageUrls.addAll(images);
                }
            }
        } else {
            // Récupérer les packs actifs du type spécifié
            Page<Pack> packs = packRepository.findByActiveTrue(
                org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), 
                    Integer.MAX_VALUE, 
                    pageable.getSort()
                )
            );
            
            for (Pack pack : packs.getContent()) {
                if (pack.getType().toString().equals(type)) {
                    // Extraire toutes les images de ce pack
                    List<String> images = pack.getActiveImages();
                    if (images != null && !images.isEmpty()) {
                        imageUrls.addAll(images);
                    }
                }
            }
        }
        
        // Appliquer la pagination sur la liste complète d'URLs
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), imageUrls.size());
        
        List<String> paginatedUrls = imageUrls.isEmpty() || start >= imageUrls.size() 
            ? new ArrayList<>() 
            : imageUrls.subList(start, end);
        
        return new org.springframework.data.domain.PageImpl<>(
            paginatedUrls, 
            pageable, 
            imageUrls.size()
        );
    }
}

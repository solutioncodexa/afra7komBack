package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.GalleryDto;
import com.afra7kom.backend.entity.Gallery;
import com.afra7kom.backend.repository.GalleryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/gallery")
@RequiredArgsConstructor
@Tag(name = "Galerie publique", description = "Images de galerie pour le site web")
public class PublicGalleryController {

    private final GalleryRepository galleryRepository;

    @GetMapping
    @Operation(summary = "Lister toutes les images actives")
    public ResponseEntity<List<GalleryDto>> getAllActive() {
        List<GalleryDto> images = galleryRepository.findByActiveTrueOrderBySortOrderAscCreatedAtDesc().stream()
                .map(GalleryDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(images);
    }

    @GetMapping("/paginated")
    @Operation(summary = "Images actives paginées")
    public ResponseEntity<Page<GalleryDto>> getPaginated(@PageableDefault(size = 20) Pageable pageable) {
        Page<GalleryDto> page = galleryRepository.findByActiveTrue(pageable)
                .map(GalleryDto::fromEntity);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/featured")
    @Operation(summary = "Images mises en avant (accueil hero / à propos)")
    public ResponseEntity<List<GalleryDto>> getFeatured() {
        List<GalleryDto> images = galleryRepository.findByFeaturedTrueAndActiveTrueOrderBySortOrderAscCreatedAtDesc().stream()
                .map(GalleryDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(images);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GalleryDto> getById(@PathVariable Long id) {
        Gallery gallery = galleryRepository.findById(id)
                .filter(g -> Boolean.TRUE.equals(g.getActive()))
                .orElseThrow(() -> new RuntimeException("Image not found"));
        return ResponseEntity.ok(GalleryDto.fromEntity(gallery));
    }
}

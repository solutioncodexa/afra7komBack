package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.GalleryDto;
import com.afra7kom.backend.entity.Gallery;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.repository.GalleryRepository;
import com.afra7kom.backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
@Tag(name = "Gallery Admin", description = "Gestion des images de la galerie")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
public class GalleryAdminController {

    private final GalleryRepository galleryRepository;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Lister toutes les images (admin)")
    public ResponseEntity<Page<GalleryDto>> getAllImages(@PageableDefault(size = 20) Pageable pageable) {
        Page<GalleryDto> page = galleryRepository.findAll(pageable)
                .map(GalleryDto::fromEntity);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GalleryDto> getImageById(@PathVariable Long id) {
        Gallery gallery = galleryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        return ResponseEntity.ok(GalleryDto.fromEntity(gallery));
    }

    @PostMapping
    public ResponseEntity<GalleryDto> createImage(@RequestBody GalleryDto dto) {
        Gallery gallery = GalleryDto.toEntity(dto);
        gallery.setActive(dto.getActive() != null ? dto.getActive() : true);
        gallery.setFeatured(dto.getFeatured() != null ? dto.getFeatured() : false);
        securityUtils.getCurrentUser().ifPresent(gallery::setCreatedBy);
        return ResponseEntity.ok(GalleryDto.fromEntity(galleryRepository.save(gallery)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GalleryDto> updateImage(@PathVariable Long id, @RequestBody GalleryDto dto) {
        Gallery gallery = galleryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        if (dto.getTitle() != null) gallery.setTitle(dto.getTitle());
        if (dto.getDescription() != null) gallery.setDescription(dto.getDescription());
        if (dto.getImageUrl() != null) gallery.setImageUrl(dto.getImageUrl());
        if (dto.getCategory() != null) gallery.setCategory(dto.getCategory());
        if (dto.getActive() != null) gallery.setActive(dto.getActive());
        if (dto.getFeatured() != null) gallery.setFeatured(dto.getFeatured());
        return ResponseEntity.ok(GalleryDto.fromEntity(galleryRepository.save(gallery)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        galleryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<GalleryDto> toggleStatus(@PathVariable Long id) {
        Gallery gallery = galleryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        gallery.setActive(!Boolean.TRUE.equals(gallery.getActive()));
        return ResponseEntity.ok(GalleryDto.fromEntity(galleryRepository.save(gallery)));
    }

    @PatchMapping("/{id}/toggle-featured")
    public ResponseEntity<GalleryDto> toggleFeatured(@PathVariable Long id) {
        Gallery gallery = galleryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        gallery.setFeatured(!Boolean.TRUE.equals(gallery.getFeatured()));
        return ResponseEntity.ok(GalleryDto.fromEntity(galleryRepository.save(gallery)));
    }

    @GetMapping("/stats/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategoryStats() {
        List<Map<String, Object>> stats = galleryRepository.countByCategory().stream()
                .map(row -> Map.<String, Object>of("category", row[0], "count", row[1]))
                .collect(Collectors.toList());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/my-images")
    public ResponseEntity<List<GalleryDto>> getMyImages() {
        User user = securityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        List<GalleryDto> images = galleryRepository.findByCreatedByOrderByCreatedAtDesc(user).stream()
                .map(GalleryDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(images);
    }
}

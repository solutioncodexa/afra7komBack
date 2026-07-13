package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.SiteSettingsDto;
import com.afra7kom.backend.service.SiteSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/site-settings")
@RequiredArgsConstructor
@Tag(name = "Admin Site Settings", description = "Gestion des paramètres du site")
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
public class AdminSiteSettingsController {

    private final SiteSettingsService siteSettingsService;

    @GetMapping
    @Operation(summary = "Récupérer les paramètres du site (admin)")
    public ResponseEntity<SiteSettingsDto> getSettings() {
        return ResponseEntity.ok(siteSettingsService.getSettings());
    }

    @PutMapping
    @Operation(summary = "Mettre à jour les paramètres du site")
    public ResponseEntity<SiteSettingsDto> updateSettings(@RequestBody SiteSettingsDto settings) {
        return ResponseEntity.ok(siteSettingsService.updateSettings(settings));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader le logo du site")
    public ResponseEntity<SiteSettingsDto> uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(siteSettingsService.uploadLogo(file));
    }

    @PostMapping(value = "/homepage/hero-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader l'image Réalisation 1 (hero accueil)")
    public ResponseEntity<SiteSettingsDto> uploadHeroImage(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(siteSettingsService.uploadHomepageHeroImage(file));
    }

    @PostMapping(value = "/homepage/about-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader l'image Réalisation 2 (section à propos)")
    public ResponseEntity<SiteSettingsDto> uploadAboutImage(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(siteSettingsService.uploadHomepageAboutImage(file));
    }
}

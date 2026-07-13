package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.SiteSettingsDto;
import com.afra7kom.backend.service.SiteSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/site-settings")
@RequiredArgsConstructor
@Tag(name = "Site Settings", description = "Configuration publique du site")
public class PublicSiteSettingsController {

    private final SiteSettingsService siteSettingsService;

    @GetMapping
    @Operation(summary = "Récupérer les paramètres publics du site")
    public ResponseEntity<SiteSettingsDto> getPublicSettings() {
        return ResponseEntity.ok(siteSettingsService.getSettings());
    }
}

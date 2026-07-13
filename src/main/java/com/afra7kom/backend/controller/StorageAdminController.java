package com.afra7kom.backend.controller;

import com.afra7kom.backend.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/storage")
@RequiredArgsConstructor
@Tag(name = "Stockage images", description = "Upload d'images (MinIO ou local)")
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
public class StorageAdminController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader une image (galerie, packs, etc.)")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "gallery") String folder
    ) throws IOException {
        String url = fileStorageService.storeImage(file, sanitizeFolder(folder));
        return ResponseEntity.ok(Map.of("url", url));
    }

    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "gallery";
        }
        return folder.replaceAll("[^a-zA-Z0-9/_-]", "").replaceAll("/+", "/");
    }
}

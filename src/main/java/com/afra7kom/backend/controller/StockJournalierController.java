package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.StockJournalierDto;
import com.afra7kom.backend.service.StockJournalierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stock-journalier")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StockJournalierController {

    private final StockJournalierService stockJournalierService;

    @GetMapping
    @Operation(summary = "Obtenir le stock journalier", description = "Récupérer le stock disponible pour une date donnée")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock journalier récupéré avec succès"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<List<StockJournalierDto>> getStockJournalier(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<StockJournalierDto> stock = stockJournalierService.getStockJournalier(targetDate);
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/materiel/{materielId}")
    @Operation(summary = "Obtenir le stock journalier d'un matériel", description = "Récupérer le stock disponible d'un matériel spécifique pour une date donnée")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock journalier du matériel récupéré avec succès"),
        @ApiResponse(responseCode = "404", description = "Matériel non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<StockJournalierDto> getStockJournalierMateriel(
            @PathVariable Long materielId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        StockJournalierDto stock = stockJournalierService.getStockJournalierMateriel(materielId, targetDate);
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard du stock journalier", description = "Statistiques et vue d'ensemble du stock journalier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard récupéré avec succès"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<List<StockJournalierDto>> getDashboardStockJournalier(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Boolean stockFaible) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<StockJournalierDto> stock = stockJournalierService.getStockJournalier(targetDate);
        
        // Filtrer par stock faible si demandé
        if (Boolean.TRUE.equals(stockFaible)) {
            stock = stock.stream()
                    .filter(s -> s.getStockDisponible() != null && s.getStockDisponible() <= 5)
                    .toList();
        }
        
        return ResponseEntity.ok(stock);
    }
}



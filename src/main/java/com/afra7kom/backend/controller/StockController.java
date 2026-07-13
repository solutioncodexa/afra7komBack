package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.StockDto;
import com.afra7kom.backend.dto.MouvementStockDto;
import com.afra7kom.backend.entity.MouvementStock;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Stock", description = "Gestion du stock et des mouvements")
public class StockController {

    private final StockService stockService;

    // Consultation du stock
    @GetMapping
    @Operation(summary = "Consulter le stock", description = "Récupérer le stock d'un matériel dans un dépôt ou tous les stocks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock récupéré avec succès"),
        @ApiResponse(responseCode = "404", description = "Matériel ou dépôt non trouvé"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<Object> getStock(
            @Parameter(description = "ID du matériel") @RequestParam(required = false) Long materielId) {

        if (materielId != null) {
            // Stock spécifique du matériel
            StockDto stock = stockService.getStock(materielId);
            return ResponseEntity.ok(stock);
        } else {
            // Tous les stocks
            List<StockDto> stocks = stockService.getAllStocks();
            return ResponseEntity.ok(stocks);
        }
    }

    @GetMapping("/materiel/{materielId}")
    @Operation(summary = "Stock par matériel", description = "Récupérer le stock d'un matériel dans tous les dépôts")
    public ResponseEntity<List<StockDto>> getStocksByMateriel(@PathVariable Long materielId) {
        List<StockDto> stocks = stockService.getStocksByMateriel(materielId);
        return ResponseEntity.ok(stocks);
    }

    // Gestion des mouvements
    @PostMapping("/mouvement")
    @Operation(summary = "Ajouter un mouvement de stock", description = "Enregistrer un nouveau mouvement de stock")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Mouvement créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides ou stock insuffisant"),
        @ApiResponse(responseCode = "404", description = "Matériel ou dépôt non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<MouvementStockDto> ajouterMouvement(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();

        Long materielId = Long.valueOf(request.get("materielId").toString());
        MouvementStock.TypeMouvement type = MouvementStock.TypeMouvement.valueOf(request.get("type").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());
        
        LocalDateTime date = null;
        if (request.containsKey("date") && request.get("date") != null) {
            date = LocalDateTime.parse(request.get("date").toString());
        }
        
        BigDecimal prixUnitaire = null;
        if (request.containsKey("prixUnitaire") && request.get("prixUnitaire") != null) {
            prixUnitaire = new BigDecimal(request.get("prixUnitaire").toString());
        }
        
        String referenceExterne = request.containsKey("referenceExterne") ? 
            request.get("referenceExterne").toString() : null;
        String fournisseur = request.containsKey("fournisseur") ? 
            request.get("fournisseur").toString() : null;
        String notes = request.containsKey("notes") ? 
            request.get("notes").toString() : null;
        String motif = request.containsKey("motif") ? 
            request.get("motif").toString() : null;
        
        Long reservationId = null;
        if (request.containsKey("reservationId") && request.get("reservationId") != null) {
            reservationId = Long.valueOf(request.get("reservationId").toString());
        }

        MouvementStockDto mouvement = stockService.ajouterMouvement(
            materielId, currentUser.getId(), type, quantity, date,
            prixUnitaire, referenceExterne, fournisseur, notes, motif, reservationId
        );

        return ResponseEntity.status(201).body(mouvement);
    }

    @PostMapping("/correction")
    @Operation(summary = "Correction de stock", description = "Corriger le stock suite à un inventaire")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MouvementStockDto> correctionStock(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();

        Long materielId = Long.valueOf(request.get("materielId").toString());
        Integer stockTheorique = Integer.valueOf(request.get("stockTheorique").toString());
        Integer stockReel = Integer.valueOf(request.get("stockReel").toString());
        String motif = request.get("motif").toString();
        String notes = request.containsKey("notes") ? request.get("notes").toString() : null;

        MouvementStockDto correction = stockService.correctionStock(
            materielId, currentUser.getId(), stockTheorique, stockReel, motif, notes
        );

        return ResponseEntity.status(201).body(correction);
    }



    // Alertes et surveillance
    @GetMapping("/alertes")
    @Operation(summary = "Alertes de stock", description = "Récupérer les alertes de stock bas ou épuisé")
    public ResponseEntity<List<StockDto>> getAlertesStock(
            @Parameter(description = "Seuil minimum") @RequestParam(defaultValue = "5") Integer seuilMinimum) {
        
        List<StockDto> alertes = stockService.getAlerteStockBas(seuilMinimum);
        return ResponseEntity.ok(alertes);
    }

    @GetMapping("/alertes/stock-bas")
    @Operation(summary = "Stock bas", description = "Matériels avec stock bas")
    public ResponseEntity<List<StockDto>> getStockBas(
            @RequestParam(defaultValue = "5") Integer seuil) {
        List<StockDto> stocksBas = stockService.getAlerteStockBas(seuil);
        return ResponseEntity.ok(stocksBas.stream()
                .filter(s -> !s.isStockEpuise())
                .toList());
    }

    @GetMapping("/alertes/stock-epuise")
    @Operation(summary = "Stock épuisé", description = "Matériels avec stock épuisé")
    public ResponseEntity<List<StockDto>> getStockEpuise() {
        List<StockDto> stocksEpuises = stockService.getAlerteStockBas(0);
        return ResponseEntity.ok(stocksEpuises.stream()
                .filter(StockDto::isStockEpuise)
                .toList());
    }

    // Historique et recherche
    @GetMapping("/mouvements")
    @Operation(summary = "Rechercher les mouvements", description = "Rechercher les mouvements de stock avec filtres")
    public ResponseEntity<Page<MouvementStockDto>> searchMouvements(
            @Parameter(description = "ID du matériel") @RequestParam(required = false) Long materielId,
            @Parameter(description = "Type de mouvement") @RequestParam(required = false) MouvementStock.TypeMouvement type,
            @Parameter(description = "ID de l'utilisateur") @RequestParam(required = false) Long userId,
            @Parameter(description = "Date de début") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Validé") @RequestParam(required = false) Boolean valide,
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<MouvementStockDto> mouvements = stockService.searchMouvements(
            materielId, type, userId, startDate, endDate, valide, pageable
        );

        return ResponseEntity.ok(mouvements);
    }

    @GetMapping("/historique")
    @Operation(summary = "Historique d'un matériel", description = "Récupérer l'historique complet d'un matériel")
    public ResponseEntity<List<MouvementStockDto>> getHistorique(
            @RequestParam Long materielId) {

        List<MouvementStockDto> historique = stockService.getHistoriqueMouvement(materielId);
        return ResponseEntity.ok(historique);
    }

    // Validation des mouvements
    @PatchMapping("/mouvement/{id}/valider")
    @Operation(summary = "Valider un mouvement", description = "Valider un mouvement de stock en attente")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MouvementStockDto> validerMouvement(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        MouvementStockDto mouvement = stockService.validerMouvement(id, currentUser.getId());
        return ResponseEntity.ok(mouvement);
    }

    // Utilitaires
//    @GetMapping("/types-mouvement")
//    @Operation(summary = "Types de mouvement", description = "Récupérer la liste des types de mouvement disponibles")
//    public ResponseEntity<Map<String, Object>> getTypesMouvement() {
//        Map<String, Object> types = Map.of(
//            "ACHAT", Map.of("name", "Achat", "entree", true, "description", "Entrée de stock par achat"),
//            "RETOUR", Map.of("name", "Retour", "entree", true, "description", "Retour de matériel après location"),
//            "TRANSFERT_ENTRANT", Map.of("name", "Transfert Entrant", "entree", true, "description", "Transfert depuis un autre dépôt"),
//            "CORRECTION_POSITIVE", Map.of("name", "Correction +", "entree", true, "description", "Correction d'inventaire positive"),
//            "RESERVATION", Map.of("name", "Réservation", "entree", false, "description", "Sortie pour réservation"),
//            "CASSE", Map.of("name", "Casse", "entree", false, "description", "Matériel cassé ou hors service"),
//            "PERTE", Map.of("name", "Perte", "entree", false, "description", "Matériel perdu"),
//            "TRANSFERT_SORTANT", Map.of("name", "Transfert Sortant", "entree", false, "description", "Transfert vers un autre dépôt"),
//            "CORRECTION_NEGATIVE", Map.of("name", "Correction -", "entree", false, "description", "Correction d'inventaire négative"),
//            "MAINTENANCE", Map.of("name", "Maintenance", "entree", false, "description", "Sortie pour maintenance"),
//            "VENTE", Map.of("name", "Vente", "entree", false, "description", "Vente de matériel")
//        );
//        return ResponseEntity.ok(types);
//    }

    @GetMapping("/resume")
    @Operation(summary = "Résumé du stock", description = "Obtenir un résumé général du stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<Map<String, Object>> getResumeStock() {
        List<StockDto> allStocks = stockService.getAllStocks();
        List<StockDto> alertes = stockService.getAlerteStockBas(5);

        Map<String, Object> resume = Map.of(
            "totalMateriels", allStocks.stream().map(StockDto::getMaterielId).distinct().count(),
            "totalStock", allStocks.stream().mapToInt(s -> s.getStockActuel() != null ? s.getStockActuel() : 0).sum(),
            "valeurTotale", allStocks.stream()
                .filter(s -> s.getValeurStock() != null)
                .map(StockDto::getValeurStock)
                .reduce(BigDecimal.ZERO, BigDecimal::add),
            "alertesStockBas", alertes.stream().filter(a -> !a.isStockEpuise()).count(),
            "alertesStockEpuise", alertes.stream().filter(StockDto::isStockEpuise).count(),
            "derniereMiseAJour", LocalDateTime.now()
        );

        return ResponseEntity.ok(resume);
    }
}




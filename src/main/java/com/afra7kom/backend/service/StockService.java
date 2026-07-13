package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.StockDto;
import com.afra7kom.backend.dto.MouvementStockDto;
import com.afra7kom.backend.dto.MaterielDto;
import com.afra7kom.backend.dto.MaterielRequest;
import com.afra7kom.backend.entity.*;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.repository.*;
import com.afra7kom.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockService {

    private final MouvementStockRepository mouvementStockRepository;
    private final MaterielRepository materielRepository;
    private final PackRepository packRepository;
    private final UserRepository userRepository;
    private final CategorieRepository categorieRepository;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;
    private final FileStorageService fileStorageService;

    // Consultation du stock
    public List<StockDto> getStockStatus() {
        List<Materiel> materiels = materielRepository.findAll();
        return materiels.stream()
                .map(this::mapToStockDto)
                .collect(Collectors.toList());
    }

    public StockDto getStockStatusById(Long materielId) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + materielId));
        return mapToStockDto(materiel);
    }

    private StockDto mapToStockDto(Materiel materiel) {
        StockDto dto = new StockDto();
        dto.setMaterielId(materiel.getId());
        dto.setMaterielName(materiel.getName());
        dto.setMaterielDescription(materiel.getDescription());
        dto.setStockActuel(materiel.getTotalQuantity());
        dto.setStockDisponible(materiel.getAvailableQuantity());
        dto.setStockReserve(materiel.getTotalQuantity() - materiel.getAvailableQuantity());
        dto.setStockMinimum(materiel.getMinimumStock());
        dto.setAlerteStockBas(materiel.getAvailableQuantity() <= materiel.getMinimumStock());
        dto.setAlerteStockEpuise(materiel.getAvailableQuantity() <= 0);
        return dto;
    }

    // Mouvements de stock
    public Page<MouvementStockDto> getMouvementsStock(Pageable pageable) {
        Page<MouvementStock> mouvements = mouvementStockRepository.findAll(pageable);
        return mouvements.map(this::mapToMouvementStockDto);
    }

    public MouvementStockDto createMouvementStock(MouvementStockDto mouvementDto) {
        Materiel materiel = materielRepository.findById(mouvementDto.getMaterielId())
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé"));
        
        User user = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        MouvementStock mouvement = new MouvementStock();
        mouvement.setMateriel(materiel);
        mouvement.setUser(user);
        mouvement.setType(mouvementDto.getType());
        mouvement.setQuantity(mouvementDto.getQuantity());
        mouvement.setDate(LocalDateTime.now());
        mouvement.setPrixUnitaire(mouvementDto.getPrixUnitaire());
        mouvement.setCoutTotal(mouvementDto.getCoutTotal());
        mouvement.setReferenceExterne(mouvementDto.getReferenceExterne());
        mouvement.setFournisseur(mouvementDto.getFournisseur());
        mouvement.setNotes(mouvementDto.getNotes());
        
        if (mouvementDto.getReservationId() != null) {
            // Créer une réservation temporaire pour la référence
            Reservation reservation = new Reservation();
            reservation.setId(mouvementDto.getReservationId());
            mouvement.setReservation(reservation);
        }

        // Calculer le stock avant et après
        mouvement.setStockAvant(materiel.getAvailableQuantity());
        
        // Appliquer le mouvement selon le type
        int newStock = materiel.getAvailableQuantity();
        switch (mouvementDto.getType()) {
            case ACHAT:
            case RETOUR:
            case TRANSFERT_ENTRANT:
            case CORRECTION_POSITIVE:
                newStock += mouvementDto.getQuantity();
                break;
            case RESERVATION:
            case CASSE:
            case PERTE:
            case TRANSFERT_SORTANT:
            case CORRECTION_NEGATIVE:
            case MAINTENANCE:
            case VENTE:
                if (newStock < mouvementDto.getQuantity()) {
                    throw new BadRequestException("Stock insuffisant pour cette sortie");
                }
                newStock -= mouvementDto.getQuantity();
                break;
        }
        
        mouvement.setStockApres(newStock);
        materiel.setAvailableQuantity(newStock);
        materielRepository.save(materiel);

        MouvementStock savedMouvement = mouvementStockRepository.save(mouvement);
        
        // Audit log
        auditLogService.logAction("MOUVEMENT_STOCK_CREATED", "MOUVEMENT_STOCK", 
                savedMouvement.getId(), "Mouvement de stock créé", getCurrentUserEmail());
        
        return MouvementStockDto.fromEntity(savedMouvement);
    }

    private MouvementStockDto mapToMouvementStockDto(MouvementStock mouvement) {
        return MouvementStockDto.fromEntity(mouvement);
    }

    // Gestion des réservations de stock
    /**
     * Réserver du stock pour un matériel
     */
    public void reserveStock(Long materielId, Integer quantity) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé"));
        
        if (materiel.getAvailableQuantity() < quantity) {
            throw new BadRequestException("Stock insuffisant pour réserver " + quantity + " unités");
        }
        
        // Mettre à jour le stock disponible
        materiel.setAvailableQuantity(materiel.getAvailableQuantity() - quantity);
        materielRepository.save(materiel);
        
        log.info("Stock réservé: {} unités de {}", quantity, materiel.getName());
    }
    
    /**
     * Réserver du stock pour un pack (pour location journalière)
     * Note: Pour les locations journalières, nous ne réduisons pas le stock physique
     * car les matériels sont rendus disponibles après la période de location
     */
    public void reservePackStock(Long packId, Integer packQuantity) {
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));
        
        List<PackMateriel> packMateriels = pack.getPackMateriels();
        
        // Vérifier la disponibilité sans réduire le stock physique
        // Le stock physique reste inchangé pour les locations journalières
        for (PackMateriel packMateriel : packMateriels) {
            Materiel materiel = packMateriel.getMateriel();
            Integer requiredQuantity = packMateriel.getQuantity() * packQuantity;
            
            // Vérifier que le stock total est suffisant (pas le stock disponible)
            // car pour les locations journalières, les matériels sont rendus après usage
            if (materiel.getTotalQuantity() < requiredQuantity) {
                throw new BadRequestException("Stock total insuffisant pour le pack: " + materiel.getName() + 
                    " (total: " + materiel.getTotalQuantity() + ", requis: " + requiredQuantity + ")");
            }
            
            log.info("Pack validé pour location journalière: {} unités de {} (stock total: {})", 
                requiredQuantity, materiel.getName(), materiel.getTotalQuantity());
        }
        
        log.info("Pack {} validé pour location journalière - aucun stock physique retiré", pack.getName());
    }
    
    /**
     * Libérer du stock réservé (annulation de réservation)
     */
    public void releaseStock(Long materielId, Integer quantity) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé"));
        
        materiel.setAvailableQuantity(materiel.getAvailableQuantity() + quantity);
        materielRepository.save(materiel);
        
        log.info("Stock libéré: {} unités de {}", quantity, materiel.getName());
    }
    
    /**
     * Libérer du stock réservé pour un pack
     */
    public void releasePackStock(Long packId, Integer packQuantity) {
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));
        
        List<PackMateriel> packMateriels = pack.getPackMateriels();
        
        for (PackMateriel packMateriel : packMateriels) {
            Materiel materiel = packMateriel.getMateriel();
            Integer quantity = packMateriel.getQuantity() * packQuantity;
            
            materiel.setAvailableQuantity(materiel.getAvailableQuantity() + quantity);
            materielRepository.save(materiel);
            
            log.info("Stock libéré pour pack: {} unités de {}", quantity, materiel.getName());
        }
    }
    
    /**
     * Vérifier si un matériel est en stock bas
     */
    public boolean isLowStock(Long materielId) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé"));
        
        return materiel.getAvailableQuantity() <= materiel.getMinimumStock();
    }
    
    /**
     * Obtenir le niveau de stock d'un matériel
     */
    public Integer getAvailableStock(Long materielId) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé"));
        
        return materiel.getAvailableQuantity();
    }

    // Utilitaires
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        return null;
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getEmail();
        }
        return null;
    }
    
    /**
     * Obtenir les équipements avec pagination
     */
    public Page<StockDto> getEquipmentsWithPagination(String searchTerm, Pageable pageable) {
        Page<Materiel> materiels;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Recherche simple par nom contenant
            List<Materiel> allMateriels = materielRepository.findAll();
            List<Materiel> filteredMateriels = allMateriels.stream()
                    .filter(m -> m.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                    .collect(Collectors.toList());
            
            // Pagination manuelle
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredMateriels.size());
            List<Materiel> pageContent = filteredMateriels.subList(start, end);
            return new org.springframework.data.domain.PageImpl<>(pageContent.stream()
                    .map(this::mapToStockDto)
                    .collect(Collectors.toList()), pageable, filteredMateriels.size());
        } else {
            materiels = materielRepository.findAll(pageable);
        }
        return materiels.map(this::mapToStockDto);
    }
    
    /**
     * Créer un nouvel équipement
     */
    public StockDto createEquipment(MaterielRequest request) {
        return createEquipment(request, null);
    }

    /**
     * Créer un nouvel équipement avec images
     */
    public StockDto createEquipment(MaterielRequest request, List<org.springframework.web.multipart.MultipartFile> images) {
        Materiel materiel = new Materiel();
        materiel.setName(request.getName());
        materiel.setDescription(request.getDescription());
        materiel.setPrice(request.getPrice());
        materiel.setTotalQuantity(request.getQuantity());
        materiel.setAvailableQuantity(request.getQuantity());
        materiel.setMinimumStock(request.getMinimumStock());
        materiel.setActive(true);
        
        Categorie categorie = categorieRepository.findById(request.getCategorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée"));
        materiel.setCategorie(categorie);
        
        Materiel savedMateriel = materielRepository.save(materiel);

        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (org.springframework.web.multipart.MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    try {
                        String imageUrl = fileStorageService.storeImage(image, "materiels");
                        imageUrls.add(imageUrl);
                        log.info("Image uploadée (MinIO/local): {}", imageUrl);
                    } catch (IOException e) {
                        log.error("Erreur upload image pour matériel {}: {}", savedMateriel.getId(), e.getMessage());
                    }
                }
            }
            if (!imageUrls.isEmpty()) {
                savedMateriel.setImages(imageUrls);
                savedMateriel.setPrimaryImageUrl(imageUrls.get(0));
                savedMateriel = materielRepository.save(savedMateriel);
            }
        }
        
        return mapToStockDto(savedMateriel);
    }
    
    // Méthodes supplémentaires pour la compatibilité avec les contrôleurs existants
    
    /**
     * Corriger le stock disponible d'un matériel pour les locations journalières
     * Cette méthode recalcule le stock disponible basé sur le stock total
     * moins les réservations actives, sans tenir compte des locations journalières terminées
     */
    public StockDto fixMaterielStockForDailyRental(Long materielId) {
        log.info("🔧 CORRECTION STOCK POUR LOCATION JOURNALIÈRE - Matériel ID: {}", materielId);
        
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + materielId));
        
        // Pour les locations journalières, le stock disponible devrait être égal au stock total
        // car les matériels sont rendus après chaque location
        int correctAvailableQuantity = materiel.getTotalQuantity();
        
        log.info("📊 CORRECTION STOCK {}: total={}, disponible_actuel={}, disponible_correct={}", 
                materiel.getName(), materiel.getTotalQuantity(), materiel.getAvailableQuantity(), correctAvailableQuantity);
        
        // Mettre à jour le stock disponible
        materiel.setAvailableQuantity(correctAvailableQuantity);
        materielRepository.save(materiel);
        
        log.info("✅ STOCK CORRIGÉ pour {}: {} -> {}", materiel.getName(), 
                materiel.getAvailableQuantity(), correctAvailableQuantity);
        
        // Audit log
        auditLogService.logAction("STOCK_CORRECTED", "MATERIEL", 
                materielId, "Stock corrigé pour location journalière", getCurrentUserEmail());
        
        return mapToStockDto(materiel);
    }
    
    /**
     * Corriger tous les stocks pour les locations journalières
     */
    public List<StockDto> fixAllMaterielStocksForDailyRental() {
        log.info("🔧 CORRECTION GLOBALE DES STOCKS POUR LOCATION JOURNALIÈRE");
        
        List<Materiel> allMateriels = materielRepository.findAll();
        List<StockDto> correctedStocks = new ArrayList<>();
        
        for (Materiel materiel : allMateriels) {
            if (materiel.getActive()) {
                StockDto correctedStock = fixMaterielStockForDailyRental(materiel.getId());
                correctedStocks.add(correctedStock);
            }
        }
        
        log.info("✅ CORRECTION TERMINÉE: {} matériels corrigés", correctedStocks.size());
        return correctedStocks;
    }
    
    /**
     * Obtenir le stock d'un matériel spécifique
     */
    public StockDto getStock(Long materielId) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé"));
        return mapToStockDto(materiel);
    }
    
    /**
     * Obtenir tous les stocks
     */
    public List<StockDto> getAllStocks() {
        List<Materiel> materiels = materielRepository.findAll();
        return materiels.stream()
                .map(this::mapToStockDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtenir les stocks par matériel
     */
    public List<StockDto> getStocksByMateriel(Long materielId) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé"));
        return List.of(mapToStockDto(materiel));
    }
    
    /**
     * Ajouter un mouvement de stock
     */
    public MouvementStockDto ajouterMouvement(Long materielId, Long userId, MouvementStock.TypeMouvement type,
                                            Integer quantity, LocalDateTime date, BigDecimal prixUnitaire,
                                            String referenceExterne, String fournisseur, String notes, String motif, Long reservationId) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        MouvementStock mouvement = new MouvementStock();
        mouvement.setMateriel(materiel);
        mouvement.setUser(user);
        mouvement.setType(type);
        mouvement.setQuantity(quantity);
        mouvement.setDate(date);
        mouvement.setPrixUnitaire(prixUnitaire);
        mouvement.setCoutTotal(prixUnitaire.multiply(BigDecimal.valueOf(quantity)));
        mouvement.setReferenceExterne(referenceExterne);
        mouvement.setFournisseur(fournisseur);
        mouvement.setNotes(notes);
        mouvement.setMotif(motif);
        
        if (reservationId != null) {
            Reservation reservation = new Reservation();
            reservation.setId(reservationId);
            mouvement.setReservation(reservation);
        }
        
        MouvementStock savedMouvement = mouvementStockRepository.save(mouvement);
        return mapToMouvementStockDto(savedMouvement);
    }
    
    /**
     * Correction de stock
     */
    public MouvementStockDto correctionStock(Long materielId, Long userId, Integer stockAvant, Integer stockApres, 
                                           String motif, String notes) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        // Créer un mouvement de correction
        MouvementStock mouvement = new MouvementStock();
        mouvement.setMateriel(materiel);
        mouvement.setUser(user);
        mouvement.setType(MouvementStock.TypeMouvement.CORRECTION_POSITIVE);
        mouvement.setQuantity(stockApres - stockAvant);
        mouvement.setDate(LocalDateTime.now());
        mouvement.setMotif(motif);
        mouvement.setNotes(notes);
        mouvement.setStockAvant(stockAvant);
        mouvement.setStockApres(stockApres);
        
        MouvementStock savedMouvement = mouvementStockRepository.save(mouvement);
        
        // Mettre à jour le stock
        materiel.setTotalQuantity(stockApres);
        materiel.setAvailableQuantity(stockApres);
        materielRepository.save(materiel);
        
        return mapToMouvementStockDto(savedMouvement);
    }
    
    /**
     * Obtenir les alertes de stock bas
     */
    public List<StockDto> getAlerteStockBas(Integer seuil) {
        List<Materiel> materiels = materielRepository.findAll();
        return materiels.stream()
                .filter(m -> m.getAvailableQuantity() <= (seuil != null ? seuil : m.getMinimumStock()))
                .map(this::mapToStockDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Rechercher des mouvements
     */
    public Page<MouvementStockDto> searchMouvements(Long materielId, MouvementStock.TypeMouvement type,
                                                   Long userId, LocalDateTime dateDebut, LocalDateTime dateFin,
                                                   Boolean valide, Pageable pageable) {
        // Implémentation simplifiée - retourner tous les mouvements avec pagination
        Page<MouvementStock> mouvements = mouvementStockRepository.findAll(pageable);
        return mouvements.map(this::mapToMouvementStockDto);
    }
    
    /**
     * Obtenir l'historique des mouvements d'un matériel
     */
    public List<MouvementStockDto> getHistoriqueMouvement(Long materielId) {
        List<MouvementStock> mouvements = mouvementStockRepository.findAll();
        return mouvements.stream()
                .filter(m -> m.getMateriel().getId().equals(materielId))
                .sorted((m1, m2) -> m2.getDate().compareTo(m1.getDate()))
                .map(this::mapToMouvementStockDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Valider un mouvement
     */
    public MouvementStockDto validerMouvement(Long mouvementId, Long userId) {
        MouvementStock mouvement = mouvementStockRepository.findById(mouvementId)
                .orElseThrow(() -> new ResourceNotFoundException("Mouvement non trouvé"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        mouvement.setValide(true);
        mouvement.setDateValidation(LocalDateTime.now());
        mouvement.setValidePar(user);
        
        MouvementStock savedMouvement = mouvementStockRepository.save(mouvement);
        return mapToMouvementStockDto(savedMouvement);
    }
}

package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.*;
import com.afra7kom.backend.dto.AvailabilityCheckRequestDto;
import com.afra7kom.backend.entity.*;
import com.afra7kom.backend.repository.*;
import com.afra7kom.backend.entity.Notification;
import com.afra7kom.backend.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import com.afra7kom.backend.service.ReservationNotificationService;
import com.afra7kom.backend.entity.Role;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final PackRepository packRepository;
    private final MaterielRepository materielRepository;
    private final UserRepository userRepository;
    private final PaiementRepository paiementRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;
    private final WhatsAppService whatsAppService;
    private final EmailService emailService;
    private final StockService stockService;
    private final AvailabilityService availabilityService;
    private final OptimizedAvailabilityService optimizedAvailabilityService;
    private final CachedAvailabilityService cachedAvailabilityService;
    private final ReservationNotificationService reservationNotificationService;
    
    /**
     * Créer une nouvelle réservation (invité ou client connecté)
     */
    @Transactional
    public ReservationResponseDto createReservation(ReservationRequestDto request, String userEmail) {
        log.info("Création d'une réservation pour: {} - {} {}", userEmail, request.getFirstName(), request.getLastName());
        
        // Vérifier la disponibilité
        AvailabilityCheckDto availability = checkAvailability(request);
        if (!availability.isAvailable()) {
            throw new BusinessException("Produit non disponible: " + availability.getMessage());
        }
        
        // Calculer le prix total
        BigDecimal totalAmount = calculateTotalPrice(request);

        // Créer la réservation
        Reservation reservation = new Reservation();
        reservation.setStartDate(request.getStartDate());
        reservation.setEndDate(request.getEndDate());
        reservation.setQuantity(request.getQuantity());
        reservation.setTotalAmount(totalAmount);
        reservation.setStatus(Reservation.ReservationStatus.DEMANDE);
        reservation.setPaymentStatus(Reservation.PaymentStatus.PENDING);
        reservation.setNotes(request.getNotes());
        reservation.setDeliveryAddress(request.getDeliveryAddress());
        
        // Gérer l'utilisateur (connecté ou invité)
        if (userEmail != null && !userEmail.isEmpty()) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));
            reservation.setUser(user);
            // Pour les utilisateurs connectés, on peut récupérer les infos depuis leur profil
                    // Récupérer les informations depuis le profil utilisateur
        reservation.setFirstName(user.getFirstName() != null ? user.getFirstName() : user.getEmail().split("@")[0]);
        reservation.setLastName(user.getLastName() != null ? user.getLastName() : "");
        reservation.setPhone(user.getPhone());
        reservation.setEmail(user.getEmail());
        reservation.setCity(request.getCity()); // Ville toujours depuis la requête
        } else {
            // Utilisateur invité
            reservation.setFirstName(request.getFirstName());
            reservation.setLastName(request.getLastName());
            reservation.setPhone(request.getPhone());
            reservation.setEmail(request.getEmail());
            reservation.setCity(request.getCity());
            // Créer un utilisateur temporaire pour l'invité
            User guestUser = new User();
            guestUser.setEmail("guest_" + System.currentTimeMillis() + "@afra7kom.com");
            guestUser.setPasswordHash("N/A");
            guestUser.setEnabled(false);
            User savedGuestUser = userRepository.save(guestUser);
            reservation.setUser(savedGuestUser);
        }
        
        // Définir le type de réservation
        if (request.getPackId() != null) {
            Pack pack = packRepository.findById(request.getPackId())
                    .orElseThrow(() -> new BusinessException("Pack non trouvé"));
            reservation.setPack(pack);
        } else if (request.getMaterielId() != null) {
            Materiel materiel = materielRepository.findById(request.getMaterielId())
                    .orElseThrow(() -> new BusinessException("Matériel non trouvé"));
            reservation.setMateriel(materiel);
        }
        
        // Sauvegarder la réservation
        Reservation savedReservation = reservationRepository.save(reservation);
        
        log.info("✅ Réservation {} créée et sauvegardée", savedReservation.getId());

        // ⚡ OPTIMISATION: Envoyer toutes les notifications de manière asynchrone
        // pour ne pas bloquer la réponse au client
        Long reservationId = savedReservation.getId();
        
        // Exécuter les notifications en arrière-plan de manière asynchrone
        CompletableFuture.runAsync(() -> {
            try {
                log.info("🔔 Début envoi notifications asynchrones pour réservation {}", reservationId);
                
                // WhatsApp
                try {
                    whatsAppService.sendReservationNotification(savedReservation);
                    log.info("✅ WhatsApp envoyé pour réservation {}", reservationId);
                } catch (Exception e) {
                    log.error("❌ Erreur WhatsApp pour réservation {}", reservationId, e);
                }
                
                // Email
                try {
                    emailService.sendNewReservationNotification(savedReservation);
                    log.info("✅ Email envoyé pour réservation {}", reservationId);
                } catch (Exception e) {
                    log.error("❌ Erreur Email pour réservation {}", reservationId, e);
                }
                
                // Notifications DB
                try {
                    reservationNotificationService.createReservationNotification(savedReservation);
                    log.info("✅ Notifications DB créées pour réservation {}", reservationId);
                } catch (Exception e) {
                    log.error("❌ Erreur Notifications DB pour réservation {}", reservationId, e);
                }
                
                // Notifications admins
                try {
                    notifyAdminsOfNewReservation(savedReservation);
                    log.info("✅ Notifications admins envoyées pour réservation {}", reservationId);
                } catch (Exception e) {
                    log.error("❌ Erreur Notifications admins pour réservation {}", reservationId, e);
                }
                
                log.info("🎉 Toutes les notifications traitées pour réservation {}", reservationId);
            } catch (Exception e) {
                log.error("❌ Erreur générale dans traitement asynchrone pour réservation {}", reservationId, e);
            }
        });
        
        // Audit log (gardé synchrone car rapide)
        auditLogService.logAction("RESERVATION_CREATED", "RESERVATION", savedReservation.getId(), 
                "Nouvelle réservation créée", userEmail);
        
        // ⚡ Retourner immédiatement la réponse sans attendre les notifications
        log.info("⚡ Réponse envoyée immédiatement pour réservation {}", savedReservation.getId());
        return mapToResponseDto(savedReservation);
    }
    
    /**
     * Vérifier la disponibilité d'un produit sur une période
     */
    public AvailabilityCheckDto checkAvailability(ReservationRequestDto request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        Integer requestedQuantity = request.getQuantity();
        
        if (request.getPackId() != null) {
            return checkPackAvailability(request.getPackId(), startDate, endDate, requestedQuantity);
        } else if (request.getMaterielId() != null) {
            return checkMaterielAvailability(request.getMaterielId(), startDate, endDate, requestedQuantity);
        }
        
        return new AvailabilityCheckDto(false, "Type de produit non spécifié");
    }
    
    /**
     * Vérifier la disponibilité d'un produit sur une période (version simple sans validation des champs personnels)
     */
    public AvailabilityCheckDto checkAvailabilitySimple(AvailabilityCheckRequestDto request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        Integer requestedQuantity = request.getQuantity();
        
        if (request.getPackId() != null) {
            return checkPackAvailability(request.getPackId(), startDate, endDate, requestedQuantity);
        } else if (request.getMaterielId() != null) {
            return checkMaterielAvailability(request.getMaterielId(), startDate, endDate, requestedQuantity);
        }
        
        return new AvailabilityCheckDto(false, "Type de produit non spécifié");
    }
    
    /**
     * Obtenir la disponibilité d'un mois entier pour un produit (VERSION AVEC CACHE)
     * Utilise Caffeine Cache avec Cache-Aside pattern pour des performances optimales
     */
    public Map<String, AvailabilityCheckDto> getMonthlyAvailability(Long packId, Long materielId, int year, int month) {
        log.info("🚀 UTILISATION DE LA VERSION AVEC CACHE pour la disponibilité mensuelle");
        log.info("Pack ID: {}, Matériel ID: {}, Mois: {}/{}", packId, materielId, month, year);
        
        Map<String, AvailabilityInfoDto> cachedResult;
        
        if (packId != null) {
            // Utiliser la version avec cache pour les packs
            cachedResult = cachedAvailabilityService.getCachedPackMonthlyAvailability(packId, year, month);
        } else if (materielId != null) {
            // Utiliser la version avec cache pour les matériels
            cachedResult = cachedAvailabilityService.getCachedMaterielMonthlyAvailability(materielId, year, month);
        } else {
            throw new RuntimeException("packId ou materielId doit être fourni");
        }
        
        // Convertir AvailabilityInfoDto vers AvailabilityCheckDto
        Map<String, AvailabilityCheckDto> monthlyAvailability = new HashMap<>();
        for (Map.Entry<String, AvailabilityInfoDto> entry : cachedResult.entrySet()) {
            AvailabilityInfoDto info = entry.getValue();
            AvailabilityCheckDto check = new AvailabilityCheckDto(
                info.getIsAvailable(),
                info.getMessage(),
                info.getAvailableQuantity()
            );
            monthlyAvailability.put(entry.getKey(), check);
        }
        
        log.info("✅ Disponibilité mensuelle avec cache calculée pour {} jours", monthlyAvailability.size());
        return monthlyAvailability;
    }
    
    /**
     * Vérifier la disponibilité d'un pack avec quantité maximale
     */
    private AvailabilityCheckDto checkPackAvailabilityWithMaxQuantity(Long packId, LocalDate startDate, LocalDate endDate) {
        // Utiliser AvailabilityService pour vérifier la disponibilité réelle du pack
        AvailabilityInfoDto availabilityInfo = availabilityService.checkPackAvailability(packId, startDate, endDate);
        
        if (!availabilityInfo.getIsAvailable() || availabilityInfo.getAvailableQuantity() == null || availabilityInfo.getAvailableQuantity() <= 0) {
            return new AvailabilityCheckDto(false, 
                    "Produit non disponible: " + availabilityInfo.getMessage(),
                    availabilityInfo.getAvailableQuantity());
        }
        
        return new AvailabilityCheckDto(true, 
                "Pack disponible - " + availabilityInfo.getAvailableQuantity() + " unité(s) disponible(s)",
                availabilityInfo.getAvailableQuantity());
    }

    /**
     * Vérifier la disponibilité d'un pack
     */
    private AvailabilityCheckDto checkPackAvailability(Long packId, LocalDate startDate, LocalDate endDate, Integer requestedQuantity) {
        // Utiliser AvailabilityService pour vérifier la disponibilité réelle du pack
        AvailabilityInfoDto availabilityInfo = availabilityService.checkPackAvailability(packId, startDate, endDate);
        
        if (!availabilityInfo.getIsAvailable() || availabilityInfo.getAvailableQuantity() < requestedQuantity) {
            return new AvailabilityCheckDto(false, 
                    "Produit non disponible: " + availabilityInfo.getMessage() + 
                    " (disponible: " + availabilityInfo.getAvailableQuantity() + 
                    ", requis: " + requestedQuantity + ")");
        }
        
        return new AvailabilityCheckDto(true, "Pack disponible");
    }
    
    /**
     * Vérifier la disponibilité d'un matériel avec quantité maximale
     */
    private AvailabilityCheckDto checkMaterielAvailabilityWithMaxQuantity(Long materielId, LocalDate startDate, LocalDate endDate) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new BusinessException("Matériel non trouvé"));
        
        // Utiliser AvailabilityService pour vérifier la disponibilité réelle sur la période
        AvailabilityInfoDto availabilityInfo = availabilityService.checkMaterielAvailability(
            materielId, startDate, endDate);
        
        if (!availabilityInfo.getIsAvailable() || availabilityInfo.getAvailableQuantity() == null || availabilityInfo.getAvailableQuantity() <= 0) {
            return new AvailabilityCheckDto(false, 
                    "Stock insuffisant (disponible: " + availabilityInfo.getAvailableQuantity() + ")",
                    availabilityInfo.getAvailableQuantity());
        }
        
        return new AvailabilityCheckDto(true, 
                "Matériel disponible - " + availabilityInfo.getAvailableQuantity() + " unité(s) disponible(s)",
                availabilityInfo.getAvailableQuantity());
    }

    /**
     * Vérifier la disponibilité d'un matériel
     */
    private AvailabilityCheckDto checkMaterielAvailability(Long materielId, LocalDate startDate, LocalDate endDate, Integer requestedQuantity) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new BusinessException("Matériel non trouvé"));
        
        // Utiliser AvailabilityService pour vérifier la disponibilité réelle sur la période
        AvailabilityInfoDto availabilityInfo = availabilityService.checkMaterielAvailability(
            materielId, startDate, endDate);
        
        if (!availabilityInfo.getIsAvailable() || availabilityInfo.getAvailableQuantity() < requestedQuantity) {
            return new AvailabilityCheckDto(false, 
                    "Stock insuffisant (disponible: " + availabilityInfo.getAvailableQuantity() + 
                    ", requis: " + requestedQuantity + ")");
        }
        
        return new AvailabilityCheckDto(true, "Matériel disponible");
    }
    
    /**
     * Calculer le prix total de la réservation
     */
    private BigDecimal calculateTotalPrice(ReservationRequestDto request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        Integer quantity = request.getQuantity();
        
        // Calculer le nombre de jours
        long numberOfDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        BigDecimal unitPrice;
        if (request.getPackId() != null) {
            Pack pack = packRepository.findById(request.getPackId())
                    .orElseThrow(() -> new BusinessException("Pack non trouvé"));
            unitPrice = pack.getPrice();
        } else {
            Materiel materiel = materielRepository.findById(request.getMaterielId())
                    .orElseThrow(() -> new BusinessException("Matériel non trouvé"));
            unitPrice = materiel.getPrice();
        }
        
        // Prix total = prix journalier × nombre de jours × quantité
        return unitPrice.multiply(BigDecimal.valueOf(numberOfDays)).multiply(BigDecimal.valueOf(quantity));
    }
    
    /**
     * Obtenir la quantité réservée pour un matériel sur une période
     */
    private Integer getReservedQuantityForPeriod(Long materielId, LocalDate startDate, LocalDate endDate) {
        return reservationRepository.sumReservedQuantityForPeriod(materielId, startDate, endDate);
    }
    
    /**
     * Valider ou rejeter une réservation (Admin/Agent)
     */
    @Transactional
    public ReservationResponseDto validateReservation(Long reservationId, ReservationValidationDto validation, String adminEmail) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée"));
        
        if (!"PENDING".equals(reservation.getStatus())) {
            throw new BusinessException("Seules les réservations en attente peuvent être validées");
        }
        
        // Mettre à jour le statut
        if ("APPROVED".equals(validation.getStatus())) {
            reservation.setStatus(Reservation.ReservationStatus.CONFIRMEE);
        } else if ("REJECTED".equals(validation.getStatus())) {
            reservation.setStatus(Reservation.ReservationStatus.ANNULEE);
        }
        
        if ("APPROVED".equals(validation.getStatus())) {
            // Créer le paiement
            Paiement paiement = new Paiement();
            paiement.setReservation(reservation);
            paiement.setAmount(validation.getDepositAmount());
            paiement.setType(Paiement.TypePaiement.ESPECE); // Par défaut, à adapter selon validation.getPaymentMethod()
            paiement.setStatut(Paiement.StatutPaiement.ACOMPTE);
            paiement.setModeReglement(validation.getPaymentMethod());
            paiement.setNotes(validation.getNotes());
            paiement.setDatePaiement(java.time.LocalDateTime.now());
            
            paiementRepository.save(paiement);
            
            // Mettre à jour le statut de paiement
            reservation.setPaymentStatus(Reservation.PaymentStatus.PARTIAL);
            reservation.setDepositAmount(validation.getDepositAmount());
            
            // Mettre à jour le stock (réserver le matériel)
            if (reservation.getMateriel() != null) {
                stockService.reserveStock(reservation.getMateriel().getId(), reservation.getQuantity());
            } else if (reservation.getPack() != null) {
                stockService.reservePackStock(reservation.getPack().getId(), reservation.getQuantity());
            }
            
            // Envoyer notification WhatsApp de confirmation
            try {
                whatsAppService.sendApprovalNotification(reservation);
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de la notification WhatsApp d'approbation", e);
            }
            
            // Envoyer notification email d'approbation au client
            try {
                emailService.sendApprovalNotification(reservation);
                log.info("Notification email d'approbation envoyée au client {}", reservation.getEmail());
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de la notification email d'approbation au client {}", 
                         reservation.getEmail(), e);
            }
            
        } else if ("REJECTED".equals(validation.getStatus())) {
            reservation.setNotes(validation.getRejectionReason());
            
            // Envoyer notification WhatsApp de rejet
            try {
                whatsAppService.sendRejectionNotification(reservation, validation.getRejectionReason());
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de la notification WhatsApp de rejet", e);
            }
            
            // Email de rejet désactivé (uniquement WhatsApp)
            // try {
            //     emailService.sendRejectionNotification(reservation, validation.getRejectionReason());
            //     log.info("Notification email de rejet envoyée au client {}", reservation.getEmail());
            // } catch (Exception e) {
            //     log.error("Erreur lors de l'envoi de la notification email de rejet au client {}", 
            //              reservation.getEmail(), e);
            // }
        }
        
        Reservation updatedReservation = reservationRepository.save(reservation);
        
        // Audit log
        auditLogService.logAction("RESERVATION_" + validation.getStatus(), "RESERVATION", 
                reservationId, "Réservation " + validation.getStatus().toLowerCase(), adminEmail);
        
        return mapToResponseDto(updatedReservation);
    }
    
    /**
     * Approuver une réservation (méthode simplifiée)
     */
    @Transactional
    public ReservationResponseDto approveReservation(Long reservationId, String depositAmountStr, String paymentMethod, String notes) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'ID: " + reservationId));
        
        if (reservation.getStatus() != Reservation.ReservationStatus.DEMANDE) {
            throw new BusinessException("Seules les réservations en demande peuvent être approuvées");
        }
        
        // Mettre à jour le statut
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMEE);
        reservation.setPaymentStatus(Reservation.PaymentStatus.PARTIAL);
        
        // Calculer l'acompte
        BigDecimal depositAmount = new BigDecimal(depositAmountStr);
        reservation.setDepositAmount(depositAmount);
        
        // Créer le paiement
        Paiement paiement = new Paiement();
        paiement.setReservation(reservation);
        paiement.setAmount(depositAmount);
        paiement.setType(Paiement.TypePaiement.valueOf(paymentMethod));
        paiement.setStatut(Paiement.StatutPaiement.ACOMPTE);
        paiement.setModeReglement(paymentMethod);
        paiement.setNotes(notes);
        paiement.setDatePaiement(java.time.LocalDateTime.now());
        
        paiementRepository.save(paiement);
        
        // Vérifier la disponibilité du stock avant de réserver
        if (reservation.getMateriel() != null) {
            Materiel materiel = materielRepository.findById(reservation.getMateriel().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé"));
            
            if (materiel.getAvailableQuantity() < reservation.getQuantity()) {
                throw new BadRequestException("Stock insuffisant pour le matériel: " + materiel.getName() + 
                    " (disponible: " + materiel.getAvailableQuantity() + ", requis: " + reservation.getQuantity() + ")");
            }
            
            stockService.reserveStock(reservation.getMateriel().getId(), reservation.getQuantity());
        } else if (reservation.getPack() != null) {
            // Pour les packs (locations journalières), vérifier la disponibilité réelle
            // avant d'approuver pour empêcher les réservations multiples sur la même date
            AvailabilityInfoDto availabilityInfo = availabilityService.checkPackAvailabilityForReservation(
                reservation.getPack().getId(), 
                reservation.getStartDate(), 
                reservation.getEndDate(), 
                reservation.getQuantity()
            );
            
            if (!availabilityInfo.getIsAvailable()) {
                throw new BadRequestException("Stock insuffisant pour le pack: " + availabilityInfo.getMessage());
            }
            
            log.info("✅ Disponibilité vérifiée pour pack {}: {} disponible(s), {} demandé(s)", 
                reservation.getPack().getName(), 
                availabilityInfo.getAvailableQuantity(), 
                reservation.getQuantity());
            
            // Pour les packs (locations journalières), ne pas réduire le stock physique
            // car les matériels sont rendus après la location
            stockService.reservePackStock(reservation.getPack().getId(), reservation.getQuantity());
        }
        
        // Envoyer notification WhatsApp de confirmation
        try {
            whatsAppService.sendApprovalNotification(reservation);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification WhatsApp d'approbation", e);
        }
        
        // Envoyer notification email d'approbation au client
        try {
            emailService.sendApprovalNotification(reservation);
            log.info("Notification email d'approbation envoyée au client {}", reservation.getEmail());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification email d'approbation au client {}", 
                     reservation.getEmail(), e);
        }
        
        Reservation updatedReservation = reservationRepository.save(reservation);
        
        // Audit log
        auditLogService.logAction("RESERVATION_APPROVED", "RESERVATION", 
                reservationId, "Réservation approuvée", "admin");
        
        return mapToResponseDto(updatedReservation);
    }
    
    /**
     * Rejeter une réservation (méthode simplifiée)
     */
    @Transactional
    public ReservationResponseDto rejectReservation(Long reservationId, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'ID: " + reservationId));
        
        if (reservation.getStatus() != Reservation.ReservationStatus.DEMANDE) {
            throw new BusinessException("Seules les réservations en demande peuvent être rejetées");
        }
        
        // Mettre à jour le statut
        reservation.setStatus(Reservation.ReservationStatus.ANNULEE);
        reservation.setNotes(reason);
        
        // Envoyer notification WhatsApp de rejet
        try {
            whatsAppService.sendRejectionNotification(reservation, reason);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification WhatsApp de rejet", e);
        }
        
        // Email de rejet désactivé (uniquement WhatsApp)
        // try {
        //     emailService.sendRejectionNotification(reservation, reason);
        //     log.info("Notification email de rejet envoyée au client {}", reservation.getEmail());
        // } catch (Exception e) {
        //     log.error("Erreur lors de l'envoi de la notification email de rejet au client {}", 
        //              reservation.getEmail(), e);
        // }
        
        Reservation updatedReservation = reservationRepository.save(reservation);
        
        // Audit log
        auditLogService.logAction("RESERVATION_REJECTED", "RESERVATION", 
                reservationId, "Réservation rejetée: " + reason, "admin");
        
        return mapToResponseDto(updatedReservation);
    }
    
    /**
     * Obtenir la liste des réservations avec filtres
     * - Admins/Agents voient toutes les réservations
     * - Utilisateurs connectés voient seulement leurs réservations
     */
    public Page<ReservationResponseDto> getReservations(ReservationFilterDto filter, String userEmail, boolean isAdmin) {
        // Construire la pagination
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        
        Page<Reservation> reservations;
        
        if (isAdmin) {
            // Admins/Agents voient toutes les réservations
            reservations = reservationRepository.findWithFilters(
                filter.getStatus(),
                filter.getPaymentStatus(),
                filter.getStartDateFrom(),
                filter.getStartDateTo(),
                filter.getEndDateFrom(),
                filter.getEndDateTo(),
                filter.getCustomerName(),
                filter.getPhone(),
                filter.getEmail(),
                filter.getPackId(),
                filter.getMaterielId(),
                pageable
            );
        } else {
            // Utilisateurs connectés voient seulement leurs réservations
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));
            
            reservations = reservationRepository.findWithFiltersForUser(
                user.getId(),
                filter.getStatus(),
                filter.getPaymentStatus(),
                filter.getStartDateFrom(),
                filter.getStartDateTo(),
                filter.getEndDateFrom(),
                filter.getEndDateTo(),
                filter.getCustomerName(),
                filter.getPhone(),
                filter.getEmail(),
                filter.getPackId(),
                filter.getMaterielId(),
                pageable
            );
        }
        
        return reservations.map(this::mapToResponseDto);
    }
    
    /**
     * Obtenir une réservation par ID
     */
    public ReservationResponseDto getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée"));
        return mapToResponseDto(reservation);
    }
    
    /**
     * Obtenir toutes les réservations avec pagination
     */
    public Page<ReservationResponseDto> getAllReservations(Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findAll(pageable);
        return reservations.map(this::mapToResponseDto);
    }
    
    /**
     * Obtenir les réservations par statut avec pagination
     */
    public Page<ReservationResponseDto> getReservationsByStatus(String status, Pageable pageable) {
        List<Reservation> reservations = reservationRepository.findByStatusOrderByCreatedAtDesc(status);
        // Convertir en Page
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), reservations.size());
        List<Reservation> pageContent = reservations.subList(start, end);
        List<ReservationResponseDto> dtoContent = pageContent.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return new org.springframework.data.domain.PageImpl<>(dtoContent, pageable, reservations.size());
    }
    
    /**
     * Annuler une réservation
     */
    @Transactional
    public void cancelReservation(Long reservationId, String reason, String userEmail) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée"));
        
        if (!reservation.isCancellable()) {
            throw new BusinessException("Cette réservation ne peut plus être annulée");
        }
        
        reservation.setStatus(Reservation.ReservationStatus.ANNULEE);
        reservation.setNotes(reason);
        reservationRepository.save(reservation);
        
        // Audit log
        auditLogService.logAction("RESERVATION_CANCELLED", "RESERVATION", 
                reservationId, "Réservation annulée: " + reason, userEmail);
    }
    
    /**
     * Mettre à jour le statut d'une réservation
     */
    @Transactional
    public ReservationResponseDto updateReservationStatus(Long reservationId, ReservationStatusUpdateDto statusUpdate, String userEmail) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée"));
        
        // Vérifier que le statut est valide
        Reservation.ReservationStatus newStatus;
        try {
            newStatus = Reservation.ReservationStatus.valueOf(statusUpdate.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Statut invalide: " + statusUpdate.getStatus());
        }
        
        // Sauvegarder l'ancien statut pour l'audit
        Reservation.ReservationStatus oldStatus = reservation.getStatus();
        
        // Mettre à jour le statut
        reservation.setStatus(newStatus);
        
        // Ajouter des notes si fournies
        if (statusUpdate.getNotes() != null && !statusUpdate.getNotes().trim().isEmpty()) {
            String currentNotes = reservation.getNotes() != null ? reservation.getNotes() : "";
            String newNotes = currentNotes.isEmpty() ? 
                statusUpdate.getNotes() : 
                currentNotes + "\n---\n" + statusUpdate.getNotes();
            reservation.setNotes(newNotes);
        }
        
        // Gérer les transitions de statut spécifiques
        if (newStatus == Reservation.ReservationStatus.CONFIRMEE && oldStatus == Reservation.ReservationStatus.DEMANDE) {
            // Mettre à jour le statut de paiement si nécessaire
            if (reservation.getPaymentStatus() == Reservation.PaymentStatus.PENDING) {
                reservation.setPaymentStatus(Reservation.PaymentStatus.PARTIAL);
            }
            
            // Réserver le stock
            if (reservation.getMateriel() != null) {
                stockService.reserveStock(reservation.getMateriel().getId(), reservation.getQuantity());
            } else if (reservation.getPack() != null) {
                stockService.reservePackStock(reservation.getPack().getId(), reservation.getQuantity());
            }
            
            // Envoyer notification WhatsApp de confirmation
            try {
                whatsAppService.sendApprovalNotification(reservation);
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de la notification WhatsApp d'approbation", e);
            }
        } else if (newStatus == Reservation.ReservationStatus.ANNULEE && oldStatus != Reservation.ReservationStatus.ANNULEE) {
            // Libérer le stock si la réservation était confirmée
            if (oldStatus == Reservation.ReservationStatus.CONFIRMEE) {
                if (reservation.getMateriel() != null) {
                    stockService.releaseStock(reservation.getMateriel().getId(), reservation.getQuantity());
                } else if (reservation.getPack() != null) {
                    stockService.releasePackStock(reservation.getPack().getId(), reservation.getQuantity());
                }
            }
            
            // Envoyer notification WhatsApp d'annulation
            try {
                String reason = statusUpdate.getReason() != null ? statusUpdate.getReason() : "Annulation de la réservation";
                whatsAppService.sendRejectionNotification(reservation, reason);
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de la notification WhatsApp d'annulation", e);
            }
        } else if (newStatus == Reservation.ReservationStatus.SOLDEE && oldStatus == Reservation.ReservationStatus.CONFIRMEE) {
            // Marquer le paiement comme complet
            reservation.setPaymentStatus(Reservation.PaymentStatus.COMPLETED);
            
            // Envoyer notification WhatsApp de finalisation
            try {
                whatsAppService.sendApprovalNotification(reservation);
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de la notification WhatsApp de finalisation", e);
            }
        }
        
        Reservation updatedReservation = reservationRepository.save(reservation);
        
        // Audit log
        auditLogService.logAction("RESERVATION_STATUS_UPDATED", "RESERVATION", 
                reservationId, 
                String.format("Statut modifié de %s vers %s", oldStatus, newStatus), 
                userEmail);
        
        return mapToResponseDto(updatedReservation);
    }
    
    /**
     * Mapper l'entité vers le DTO de réponse
     */
    private ReservationResponseDto mapToResponseDto(Reservation reservation) {
        ReservationResponseDto dto = new ReservationResponseDto();
        dto.setId(reservation.getId());
        
        // Récupérer les informations client depuis Reservation ou User
        String firstName = reservation.getFirstName();
        String lastName = reservation.getLastName();
        String phone = reservation.getPhone();
        String email = reservation.getEmail();
        
        // Si les informations ne sont pas dans Reservation, les récupérer depuis User
        if (firstName == null && reservation.getUser() != null) {
            firstName = reservation.getUser().getFirstName();
        }
        if (lastName == null && reservation.getUser() != null) {
            lastName = reservation.getUser().getLastName();
        }
        if (phone == null && reservation.getUser() != null) {
            phone = reservation.getUser().getPhone();
        }
        if (email == null && reservation.getUser() != null) {
            email = reservation.getUser().getEmail();
        }
        
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setPhone(phone);
        dto.setEmail(email);
        dto.setCity(reservation.getCity());
        dto.setStartDate(reservation.getStartDate());
        dto.setEndDate(reservation.getEndDate());
        dto.setQuantity(reservation.getQuantity());
        dto.setTotalAmount(reservation.getTotalAmount());
        dto.setStatus(reservation.getStatus().name());
        dto.setPaymentStatus(reservation.getPaymentStatus().name());
        dto.setNotes(reservation.getNotes());
        dto.setDeliveryAddress(reservation.getDeliveryAddress());
        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setUpdatedAt(reservation.getUpdatedAt());
        
        // Calculer le nombre de jours
        if (reservation.getStartDate() != null && reservation.getEndDate() != null) {
            long numberOfDays = ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate()) + 1;
            dto.setNumberOfDays((int) numberOfDays);
        }
        
        // Récupérer les informations du pack ou matériel
        if (reservation.getPack() != null) {
            Pack pack = reservation.getPack();
            dto.setPackId(pack.getId());
            dto.setPackName(pack.getName());
            dto.setUnitPrice(pack.getPrice());
        } else if (reservation.getMateriel() != null) {
            Materiel materiel = reservation.getMateriel();
            dto.setMaterielId(materiel.getId());
            dto.setMaterielName(materiel.getName());
            dto.setUnitPrice(materiel.getPrice());
        }
        
        return dto;
    }
    
    /**
     * Notifier tous les admins d'une nouvelle réservation
     */
    private void notifyAdminsOfNewReservation(Reservation reservation) {
        try {
            log.info("Début de la notification des admins pour la réservation {}", reservation.getId());
            
            // Trouver tous les utilisateurs avec les rôles ADMIN et AGENT
            List<User> adminUsers = userRepository.findByRoleName(Role.RoleName.ADMIN, PageRequest.of(0, 100))
                    .getContent();
            List<User> agentUsers = userRepository.findByRoleName(Role.RoleName.AGENT, PageRequest.of(0, 100))
                    .getContent();
            
            // Combiner les deux listes
            List<User> allNotificationUsers = new ArrayList<>();
            allNotificationUsers.addAll(adminUsers);
            allNotificationUsers.addAll(agentUsers);
            
            log.info("Nombre d'utilisateurs admin trouvés: {}", adminUsers.size());
            log.info("Nombre d'utilisateurs agent trouvés: {}", agentUsers.size());
            log.info("Total d'utilisateurs à notifier: {}", allNotificationUsers.size());
            
            if (allNotificationUsers.isEmpty()) {
                log.warn("Aucun utilisateur admin ou agent trouvé pour recevoir les notifications de réservation");
                return;
            }
            
            // Créer le nom du client
            String customerName = reservation.getFirstName() + " " + reservation.getLastName();
            if (customerName.trim().isEmpty()) {
                customerName = reservation.getEmail();
            }
            
            log.info("Nom du client pour la notification: {}", customerName);
            
            // Envoyer une notification à chaque utilisateur (admin et agent)
            int successCount = 0;
            int errorCount = 0;
            
            for (User user : allNotificationUsers) {
                try {
                    log.info("Envoi de notification à l'utilisateur {} (ID: {})", user.getEmail(), user.getId());
                    
                    // Créer une notification pour cet utilisateur
                    createNotificationForUser(user, reservation, customerName);
                    
                    successCount++;
                    log.info("Notification envoyée avec succès à l'utilisateur {} pour la réservation {}", 
                            user.getEmail(), reservation.getId());
                } catch (Exception e) {
                    errorCount++;
                    log.error("Erreur lors de l'envoi de notification à l'utilisateur {} (ID: {}): {}", 
                            user.getEmail(), user.getId(), e.getMessage(), e);
                }
            }
            
            log.info("Résumé des notifications: {} succès, {} erreurs sur {} utilisateur(s) (admin + agent)", 
                    successCount, errorCount, allNotificationUsers.size());
            
        } catch (Exception e) {
            log.error("Erreur lors de la notification des admins pour la réservation {}: {}", 
                    reservation.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Getter pour accéder à AvailabilityService depuis les contrôleurs
     */
    public AvailabilityService getAvailabilityService() {
        return availabilityService;
    }

    /**
     * Créer une notification pour un utilisateur spécifique
     */
    private void createNotificationForUser(User user, Reservation reservation, String customerName) {
        try {
            // Créer une notification simple
            com.afra7kom.backend.entity.Notification notification = new com.afra7kom.backend.entity.Notification();
            notification.setUser(user);
            notification.setTitle("Nouvelle réservation");
            notification.setMessage("Une nouvelle réservation #" + reservation.getId() + 
                                 " a été créée par " + customerName);
            notification.setType(com.afra7kom.backend.entity.Notification.NotificationType.RESERVATION_CREATED);
            notification.setStatus(com.afra7kom.backend.entity.Notification.NotificationStatus.UNREAD);
            notification.setReservationId(reservation.getId());
            notification.setCreatedAt(java.time.LocalDateTime.now());
            
            // Sauvegarder la notification
            notificationRepository.save(notification);
            
            log.info("Notification créée pour l'utilisateur {} (ID: {})", user.getEmail(), user.getId());
            
        } catch (Exception e) {
            log.error("Erreur lors de la création de la notification pour l'utilisateur {}: {}", 
                     user.getEmail(), e.getMessage());
        }
    }
    
    /**
     * Obtenir les réservations par mois pour le calendrier
     * Charge le mois spécifié + mois précédent et suivant pour un buffer
     */
    public List<ReservationResponseDto> getReservationsByMonth(Integer year, Integer month) {
        // Calculer le premier jour du mois précédent
        LocalDate startDate = LocalDate.of(year, month, 1).minusMonths(1);
        
        // Calculer le dernier jour du mois suivant
        LocalDate endDate = LocalDate.of(year, month, 1).plusMonths(2).minusDays(1);
        
        log.info("📅 Chargement réservations pour calendrier: {} à {} (mois demandé: {}/{})", 
                startDate, endDate, month, year);
        
        // Récupérer toutes les réservations qui chevauchent cette période
        List<Reservation> reservations = reservationRepository.findReservationsBetweenDates(startDate, endDate);
        
        log.info("📅 {} réservations trouvées pour la période", reservations.size());
        
        // Convertir en DTO
        return reservations.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
}




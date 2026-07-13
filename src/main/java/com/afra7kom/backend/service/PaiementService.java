package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.PaiementDto;
import com.afra7kom.backend.entity.Paiement;
import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.entity.AuditLog;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.repository.PaiementRepository;
import com.afra7kom.backend.repository.ReservationRepository;
import com.afra7kom.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final ReservationRepository reservationRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    // CRUD Operations
    public PaiementDto createPaiement(Long reservationId, BigDecimal amount, Paiement.TypePaiement type,
                                     Paiement.StatutPaiement statut, String referenceExterne, String notes,
                                     LocalDateTime dateEcheance, String modeReglement, String banque,
                                     String numeroCheque, String numeroVirement) {
        
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'ID: " + reservationId));

        // Validations métier
        validatePaiementCreation(reservation, amount, statut);

        Paiement paiement = new Paiement();
        paiement.setReservation(reservation);
        paiement.setAmount(amount);
        paiement.setType(type);
        paiement.setStatut(statut);
        paiement.setReferenceExterne(referenceExterne);
        paiement.setNotes(notes);
        paiement.setDateEcheance(dateEcheance);
        paiement.setModeReglement(modeReglement);
        paiement.setBanque(banque);
        paiement.setNumeroCheque(numeroCheque);
        paiement.setNumeroVirement(numeroVirement);

        Paiement savedPaiement = paiementRepository.save(paiement);

        // Mettre à jour le statut de la réservation si nécessaire
        updateReservationStatusAfterPaiement(reservation);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "PAIEMENT_CREATE",
            "Paiement créé pour réservation " + reservationId + " - Montant: " + amount + " MAD",
            securityUtils.getCurrentIpAddress()
        );

        return PaiementDto.fromEntity(savedPaiement);
    }

    public PaiementDto marquerCommePaye(Long paiementId, LocalDateTime datePaiement, 
                                       String referenceExterne, String notes) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement non trouvé avec l'ID: " + paiementId));

        if (paiement.isPaye()) {
            throw new BadRequestException("Ce paiement est déjà marqué comme payé");
        }

        paiement.setDatePaiement(datePaiement != null ? datePaiement : LocalDateTime.now());
        if (referenceExterne != null) {
            paiement.setReferenceExterne(referenceExterne);
        }
        if (notes != null) {
            paiement.setNotes(notes);
        }

        paiement.marquerCommePaye();
        Paiement savedPaiement = paiementRepository.save(paiement);

        // Mettre à jour le statut de la réservation
        updateReservationStatusAfterPaiement(paiement.getReservation());

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "PAIEMENT_UPDATE",
            "Paiement marqué comme payé - Référence: " + referenceExterne,
            securityUtils.getCurrentIpAddress()
        );

        return PaiementDto.fromEntity(savedPaiement);
    }

    @Transactional(readOnly = true)
    public List<PaiementDto> getPaiementsByReservation(Long reservationId) {
        return paiementRepository.findByReservationId(reservationId)
                .stream()
                .map(PaiementDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PaiementDto> getAllPaiements(Pageable pageable) {
        return paiementRepository.findAll(pageable)
                .map(PaiementDto::fromEntitySimple);
    }

    @Transactional(readOnly = true)
    public Page<PaiementDto> searchPaiements(Long reservationId, Paiement.TypePaiement type,
                                            Paiement.StatutPaiement statut, LocalDateTime startDate,
                                            LocalDateTime endDate, Pageable pageable) {
        return paiementRepository.findWithFilters(reservationId, type, statut, startDate, endDate, pageable)
                .map(PaiementDto::fromEntitySimple);
    }

    @Transactional(readOnly = true)
    public PaiementDto getPaiementById(Long id) {
        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement non trouvé avec l'ID: " + id));
        return PaiementDto.fromEntity(paiement);
    }

    // Statistiques
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistiquesRevenus(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new HashMap<>();

        // Revenus totaux
        BigDecimal totalRevenus = paiementRepository.getTotalRevenusPeriode(startDate, endDate);
        stats.put("totalRevenus", totalRevenus);

        // Nombre de paiements
        List<Paiement> paiements = paiementRepository.findPaiementsPayesDansPeriode(startDate, endDate);
        stats.put("nombrePaiements", paiements.size());

        // Moyenne des paiements
        BigDecimal moyennePaiement = paiements.isEmpty() ? BigDecimal.ZERO :
                totalRevenus.divide(BigDecimal.valueOf(paiements.size()), 2, RoundingMode.HALF_UP);
        stats.put("moyennePaiement", moyennePaiement);

        // Revenus par type
        List<Object[]> revenusParType = paiementRepository.getRevenusParType(startDate, endDate);
        Map<String, Object> typeStats = new HashMap<>();
        for (Object[] row : revenusParType) {
            Paiement.TypePaiement type = (Paiement.TypePaiement) row[0];
            BigDecimal montant = (BigDecimal) row[1];
            typeStats.put(type.name(), Map.of(
                "type", type.getDisplayName(),
                "montant", montant,
                "pourcentage", totalRevenus.compareTo(BigDecimal.ZERO) > 0 ?
                    montant.divide(totalRevenus, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                    BigDecimal.ZERO
            ));
        }
        stats.put("revenusParType", typeStats);

        // Statistiques acomptes vs soldes
        List<Object[]> acomptesSoldes = paiementRepository.getStatistiquesAcomptesSoldes(startDate, endDate);
        Map<String, Object> statutStats = new HashMap<>();
        for (Object[] row : acomptesSoldes) {
            Paiement.StatutPaiement statut = (Paiement.StatutPaiement) row[0];
            Long count = (Long) row[1];
            BigDecimal montant = (BigDecimal) row[2];
            statutStats.put(statut.name(), Map.of(
                "statut", statut.getDisplayName(),
                "nombre", count,
                "montant", montant
            ));
        }
        stats.put("statistiquesStatuts", statutStats);

        // Revenus mensuels (derniers 12 mois)
        List<Object[]> revenusMensuels = paiementRepository.getRevenusMensuels();
        List<Map<String, Object>> monthlyStats = new ArrayList<>();
        for (Object[] row : revenusMensuels.subList(0, Math.min(12, revenusMensuels.size()))) {
            Integer annee = (Integer) row[0];
            Integer mois = (Integer) row[1];
            BigDecimal montant = (BigDecimal) row[2];
            
            monthlyStats.add(Map.of(
                "annee", annee,
                "mois", mois,
                "moisNom", Month.of(mois).name(),
                "montant", montant
            ));
        }
        stats.put("revenusMensuels", monthlyStats);

        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSituationPaiementReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'ID: " + reservationId));

        Map<String, Object> situation = new HashMap<>();

        BigDecimal totalReservation = reservation.getTotalAmount();
        BigDecimal totalPaye = paiementRepository.getTotalPayePourReservation(reservationId);
        BigDecimal acomptePaye = paiementRepository.getAcomptePayePourReservation(reservationId);
        BigDecimal acompteAttendu = totalReservation.multiply(new BigDecimal("0.30"));
        BigDecimal resteAPayer = totalReservation.subtract(totalPaye);

        situation.put("reservationId", reservationId);
        situation.put("totalReservation", totalReservation);
        situation.put("totalPaye", totalPaye);
        situation.put("resteAPayer", resteAPayer);
        situation.put("acompteAttendu", acompteAttendu);
        situation.put("acomptePaye", acomptePaye);
        situation.put("acompteComplet", acomptePaye.compareTo(acompteAttendu) >= 0);
        situation.put("reservationSoldee", resteAPayer.compareTo(BigDecimal.ZERO) <= 0);

        List<PaiementDto> paiements = getPaiementsByReservation(reservationId);
        situation.put("nombrePaiements", paiements.size());
        situation.put("paiements", paiements);

        return situation;
    }

    // Méthodes utilitaires privées
    private void validatePaiementCreation(Reservation reservation, BigDecimal amount, 
                                         Paiement.StatutPaiement statut) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Le montant doit être positif");
        }

        if (amount.compareTo(reservation.getTotalAmount()) > 0) {
            throw new BadRequestException("Le montant ne peut pas dépasser le total de la réservation");
        }

        // Vérifier que le total des paiements ne dépasse pas le total de la réservation
        BigDecimal totalExistant = paiementRepository.getTotalPayePourReservation(reservation.getId());
        if (totalExistant.add(amount).compareTo(reservation.getTotalAmount()) > 0) {
            throw new BadRequestException("Le total des paiements dépasserait le montant de la réservation");
        }

        // Validation spécifique aux acomptes
        if (statut == Paiement.StatutPaiement.ACOMPTE) {
            BigDecimal acompteAttendu = reservation.getTotalAmount().multiply(new BigDecimal("0.30"));
            BigDecimal acompteExistant = paiementRepository.getAcomptePayePourReservation(reservation.getId());
            
            if (acompteExistant.add(amount).compareTo(acompteAttendu.multiply(new BigDecimal("1.1"))) > 0) {
                throw new BadRequestException("Le montant de l'acompte est trop élevé (max 110% de l'acompte attendu)");
            }
        }
    }

    private void updateReservationStatusAfterPaiement(Reservation reservation) {
        BigDecimal totalPaye = paiementRepository.getTotalPayePourReservation(reservation.getId());
        BigDecimal acomptePaye = paiementRepository.getAcomptePayePourReservation(reservation.getId());
        BigDecimal acompteAttendu = reservation.getTotalAmount().multiply(new BigDecimal("0.30"));

        // Mettre à jour le statut de la réservation selon les paiements
        if (totalPaye.compareTo(reservation.getTotalAmount()) >= 0) {
            // Réservation soldée
            if (reservation.getStatus() != Reservation.ReservationStatus.SOLDEE) {
                reservation.setStatus(Reservation.ReservationStatus.SOLDEE);
                reservationRepository.save(reservation);
            }
        } else if (acomptePaye.compareTo(acompteAttendu) >= 0) {
            // Acompte payé, réservation confirmée
            if (reservation.getStatus() == Reservation.ReservationStatus.EN_ATTENTE_ACOMPTE) {
                reservation.setStatus(Reservation.ReservationStatus.CONFIRMEE);
                //reservation.setConfirmedAt(LocalDateTime.now());
                reservationRepository.save(reservation);
            }
        } else if (acomptePaye.compareTo(BigDecimal.ZERO) > 0) {
            // Acompte partiel reçu
            if (reservation.getStatus() == Reservation.ReservationStatus.DEMANDE) {
                reservation.setStatus(Reservation.ReservationStatus.EN_ATTENTE_ACOMPTE);
                reservationRepository.save(reservation);
            }
        }
    }

    // Méthodes pour factures
    @Transactional(readOnly = true)
    public Optional<Paiement> findByFactureNumero(String factureNumero) {
        return paiementRepository.findByFactureNumero(factureNumero);
    }

    @Transactional(readOnly = true)
    public List<Paiement> getPaiementsEnRetard() {
        return paiementRepository.findPaiementsEnRetard(LocalDateTime.now());
    }

    public void genererNumeroFacture(Long paiementId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement non trouvé avec l'ID: " + paiementId));
        
        paiement.genererNumeroFacture();
        paiement.setFactureGeneree(true);
        paiementRepository.save(paiement);
    }
}




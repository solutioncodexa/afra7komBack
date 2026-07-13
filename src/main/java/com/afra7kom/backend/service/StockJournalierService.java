package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.StockJournalierDto;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.PackMateriel;
import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import com.afra7kom.backend.repository.MaterielRepository;
import com.afra7kom.backend.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockJournalierService {

    private final MaterielRepository materielRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Calcule le stock journalier pour tous les matériels
     */
    public List<StockJournalierDto> getStockJournalier(LocalDate date) {
        List<Materiel> materiels = materielRepository.findAll();
        
        // Récupérer toutes les réservations confirmées ou en cours pour la date
        List<Reservation> reservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CONFIRMEE 
                          || r.getStatus() == Reservation.ReservationStatus.EN_COURS
                          || r.getStatus() == Reservation.ReservationStatus.SOLDEE)
                .filter(r -> isDateInRange(date, r.getStartDate(), r.getEndDate()))
                .toList();

        // Calculer les quantités réservées par matériel
        Map<Long, Integer> quantitesReservees = calculerQuantitesReservees(reservations);

        // Créer les DTO
        return materiels.stream()
                .map(materiel -> creerStockJournalierDto(materiel, date, quantitesReservees))
                .collect(Collectors.toList());
    }

    /**
     * Calcule le stock journalier pour un matériel spécifique
     */
    public StockJournalierDto getStockJournalierMateriel(Long materielId, LocalDate date) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + materielId));

        // Récupérer toutes les réservations confirmées ou en cours pour la date
        List<Reservation> reservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CONFIRMEE 
                          || r.getStatus() == Reservation.ReservationStatus.EN_COURS
                          || r.getStatus() == Reservation.ReservationStatus.SOLDEE)
                .filter(r -> isDateInRange(date, r.getStartDate(), r.getEndDate()))
                .toList();

        // Calculer les quantités réservées
        Map<Long, Integer> quantitesReservees = calculerQuantitesReservees(reservations);

        return creerStockJournalierDto(materiel, date, quantitesReservees);
    }

    /**
     * Vérifie si une date est dans une plage de dates
     */
    private boolean isDateInRange(LocalDate date, LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            return false;
        }
        return !date.isBefore(dateDebut) && !date.isAfter(dateFin);
    }

    /**
     * Calcule les quantités réservées pour chaque matériel
     */
    private Map<Long, Integer> calculerQuantitesReservees(List<Reservation> reservations) {
        Map<Long, Integer> quantitesReservees = new HashMap<>();

        for (Reservation reservation : reservations) {
            // Réservation directe d'un matériel
            if (reservation.getMateriel() != null) {
                Long materielId = reservation.getMateriel().getId();
                int quantite = reservation.getQuantity() != null ? reservation.getQuantity() : 1;
                quantitesReservees.merge(materielId, quantite, Integer::sum);
            }

            // Réservation d'un pack contenant des matériels
            if (reservation.getPack() != null && reservation.getPack().getPackMateriels() != null) {
                for (PackMateriel packMateriel : reservation.getPack().getPackMateriels()) {
                    Long materielId = packMateriel.getMateriel().getId();
                    int quantite = packMateriel.getQuantity();
                    // Multiplier par la quantité de packs réservés si applicable
                    int quantiteReservation = reservation.getQuantity() != null ? reservation.getQuantity() : 1;
                    quantitesReservees.merge(materielId, quantite * quantiteReservation, Integer::sum);
                }
            }
        }

        return quantitesReservees;
    }

    /**
     * Crée un DTO de stock journalier pour un matériel
     */
    private StockJournalierDto creerStockJournalierDto(
            Materiel materiel, 
            LocalDate date, 
            Map<Long, Integer> quantitesReservees) {
        
        StockJournalierDto dto = new StockJournalierDto();
        dto.setMaterielId(materiel.getId());
        dto.setMaterielName(materiel.getName());
        dto.setMarque(null); // Pas de marque dans Materiel
        dto.setModele(null); // Pas de modèle dans Materiel
        dto.setStockTotal(materiel.getTotalQuantity() != null ? materiel.getTotalQuantity() : 0);
        dto.setPrice(materiel.getPrice());
        dto.setDate(date);
        dto.setActive(materiel.getActive() != null ? materiel.getActive() : true);
        dto.setImageUrl(materiel.getPrimaryImageUrl());

        // Calculer le stock réservé et disponible
        int stockReserve = quantitesReservees.getOrDefault(materiel.getId(), 0);
        dto.setStockReserve(stockReserve);
        
        int stockDisponible = Math.max(0, dto.getStockTotal() - stockReserve);
        dto.setStockDisponible(stockDisponible);

        return dto;
    }
}


package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Paiement;
import com.afra7kom.backend.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    // Recherche par réservation
    List<Paiement> findByReservationId(Long reservationId);
    
    List<Paiement> findByReservation(Reservation reservation);
    
    Page<Paiement> findByReservationId(Long reservationId, Pageable pageable);

    // Recherche par statut
    List<Paiement> findByStatut(Paiement.StatutPaiement statut);
    
    Page<Paiement> findByStatut(Paiement.StatutPaiement statut, Pageable pageable);

    // Recherche par type
    List<Paiement> findByType(Paiement.TypePaiement type);

    // Recherche par numéro de facture
    Optional<Paiement> findByFactureNumero(String factureNumero);

    // Paiements en retard
    @Query("SELECT p FROM Paiement p WHERE p.dateEcheance < :currentDate AND p.datePaiement IS NULL")
    List<Paiement> findPaiementsEnRetard(@Param("currentDate") LocalDateTime currentDate);

    // Paiements payés dans une période
    @Query("SELECT p FROM Paiement p WHERE p.datePaiement BETWEEN :startDate AND :endDate")
    List<Paiement> findPaiementsPayesDansPeriode(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    // Statistiques - Revenus par période
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Paiement p WHERE " +
           "p.datePaiement BETWEEN :startDate AND :endDate AND p.datePaiement IS NOT NULL")
    BigDecimal getTotalRevenusPeriode(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // Statistiques - Revenus par mois
    @Query("SELECT YEAR(p.datePaiement), MONTH(p.datePaiement), SUM(p.amount) " +
           "FROM Paiement p WHERE p.datePaiement IS NOT NULL " +
           "GROUP BY YEAR(p.datePaiement), MONTH(p.datePaiement) " +
           "ORDER BY YEAR(p.datePaiement) DESC, MONTH(p.datePaiement) DESC")
    List<Object[]> getRevenusMensuels();

    // Statistiques - Revenus par type de paiement
    @Query("SELECT p.type, SUM(p.amount) FROM Paiement p WHERE " +
           "p.datePaiement BETWEEN :startDate AND :endDate AND p.datePaiement IS NOT NULL " +
           "GROUP BY p.type")
    List<Object[]> getRevenusParType(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    // Statistiques - Acomptes vs Soldes
    @Query("SELECT p.statut, COUNT(p), SUM(p.amount) FROM Paiement p WHERE " +
           "p.datePaiement BETWEEN :startDate AND :endDate AND p.datePaiement IS NOT NULL " +
           "GROUP BY p.statut")
    List<Object[]> getStatistiquesAcomptesSoldes(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    // Montant total payé pour une réservation
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Paiement p WHERE " +
           "p.reservation.id = :reservationId AND p.datePaiement IS NOT NULL")
    BigDecimal getTotalPayePourReservation(@Param("reservationId") Long reservationId);

    // Montant des acomptes payés pour une réservation
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Paiement p WHERE " +
           "p.reservation.id = :reservationId AND p.statut = 'ACOMPTE' AND p.datePaiement IS NOT NULL")
    BigDecimal getAcomptePayePourReservation(@Param("reservationId") Long reservationId);

    // Vérifier si une réservation est soldée
    @Query("SELECT CASE WHEN r.totalAmount <= " +
           "(SELECT COALESCE(SUM(p.amount), 0) FROM Paiement p WHERE p.reservation.id = :reservationId AND p.datePaiement IS NOT NULL) " +
           "THEN true ELSE false END " +
           "FROM Reservation r WHERE r.id = :reservationId")
    boolean isReservationSoldee(@Param("reservationId") Long reservationId);

    // Paiements récents
    @Query("SELECT p FROM Paiement p WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Paiement> findPaiementsRecents(@Param("since") LocalDateTime since);

    // Paiements par utilisateur (via réservation)
    @Query("SELECT p FROM Paiement p WHERE p.reservation.user.id = :userId")
    List<Paiement> findPaiementsByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Paiement p WHERE p.reservation.user.id = :userId")
    Page<Paiement> findPaiementsByUserId(@Param("userId") Long userId, Pageable pageable);

    // Recherche avec filtres multiples
    @Query("SELECT p FROM Paiement p WHERE " +
           "(:reservationId IS NULL OR p.reservation.id = :reservationId) AND " +
           "(:type IS NULL OR p.type = :type) AND " +
           "(:statut IS NULL OR p.statut = :statut) AND " +
           "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR p.createdAt <= :endDate)")
    Page<Paiement> findWithFilters(@Param("reservationId") Long reservationId,
                                  @Param("type") Paiement.TypePaiement type,
                                  @Param("statut") Paiement.StatutPaiement statut,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  Pageable pageable);

    // Compter les paiements par statut
    @Query("SELECT COUNT(p) FROM Paiement p WHERE p.statut = :statut")
    long countByStatut(@Param("statut") Paiement.StatutPaiement statut);

    // Factures générées dans une période
    @Query("SELECT p FROM Paiement p WHERE p.factureGeneree = true AND " +
           "p.updatedAt BETWEEN :startDate AND :endDate")
    List<Paiement> findFacturesGenereesDansPeriode(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
}




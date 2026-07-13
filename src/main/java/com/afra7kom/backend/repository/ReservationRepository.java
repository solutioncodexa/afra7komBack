package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.entity.Reservation.ReservationStatus;
import com.afra7kom.backend.entity.Reservation.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    /**
     * Trouver les réservations par utilisateur
     */
    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Trouver les réservations par statut
     */
    List<Reservation> findByStatusOrderByCreatedAtDesc(String status);
    
    /**
     * Trouver les réservations par pack
     */
    @Query("SELECT r FROM Reservation r WHERE r.pack.id = :packId AND r.status = :status ORDER BY r.startDate ASC")
    List<Reservation> findByPackIdAndStatusOrderByStartDateAsc(@Param("packId") Long packId, @Param("status") ReservationStatus status);
    
    /**
     * Vérifier si un pack a des réservations (pour empêcher la suppression)
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.pack.id = :packId")
    boolean existsByPackId(@Param("packId") Long packId);
    
    /**
     * Trouver toutes les réservations d'un pack (pour suppression forcée)
     */
    @Query("SELECT r FROM Reservation r WHERE r.pack.id = :packId")
    List<Reservation> findByPackId(@Param("packId") Long packId);
    
    /**
     * Vérifier si un matériel a des réservations (pour empêcher la suppression)
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.materiel.id = :materielId")
    boolean existsByMaterielId(@Param("materielId") Long materielId);
    
    /**
     * Trouver toutes les réservations d'un matériel (pour suppression forcée)
     */
    @Query("SELECT r FROM Reservation r WHERE r.materiel.id = :materielId")
    List<Reservation> findByMaterielId(@Param("materielId") Long materielId);
    
    /**
     * Trouver les réservations par matériel
     */
    @Query("SELECT r FROM Reservation r WHERE r.materiel.id = :materielId AND r.status = :status ORDER BY r.startDate ASC")
    List<Reservation> findByMaterielIdAndStatusOrderByStartDateAsc(@Param("materielId") Long materielId, @Param("status") ReservationStatus status);
    
    /**
     * Trouver les réservations sur une période donnée
     */
    @Query("SELECT r FROM Reservation r WHERE r.status = 'CONFIRMEE' AND " +
           "((r.startDate BETWEEN :startDate AND :endDate) OR " +
           "(r.endDate BETWEEN :startDate AND :endDate) OR " +
           "(r.startDate <= :startDate AND r.endDate >= :endDate))")
    List<Reservation> findOverlappingReservations(@Param("startDate") LocalDate startDate, 
                                                  @Param("endDate") LocalDate endDate);
    
    /**
     * Trouver les réservations d'un matériel sur une période
     */
    @Query("SELECT r FROM Reservation r WHERE r.materiel.id = :materielId AND r.status = 'CONFIRMEE' AND " +
           "((r.startDate BETWEEN :startDate AND :endDate) OR " +
           "(r.endDate BETWEEN :startDate AND :endDate) OR " +
           "(r.startDate <= :startDate AND r.endDate >= :endDate))")
    List<Reservation> findOverlappingReservationsForMateriel(@Param("materielId") Long materielId,
                                                             @Param("startDate") LocalDate startDate, 
                                                             @Param("endDate") LocalDate endDate);
    
    /**
     * Calculer la quantité réservée pour un matériel sur une période
     */
    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM Reservation r WHERE r.materiel.id = :materielId " +
           "AND r.status = 'CONFIRMEE' AND " +
           "((r.startDate BETWEEN :startDate AND :endDate) OR " +
           "(r.endDate BETWEEN :startDate AND :endDate) OR " +
           "(r.startDate <= :startDate AND r.endDate >= :endDate))")
    Integer sumReservedQuantityForPeriod(@Param("materielId") Long materielId,
                                        @Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);
    
    /**
     * Trouver les réservations avec filtres avancés
     */
    @Query("SELECT r FROM Reservation r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:paymentStatus IS NULL OR r.paymentStatus = :paymentStatus) AND " +
           "(:startDateFrom IS NULL OR r.startDate >= :startDateFrom) AND " +
           "(:startDateTo IS NULL OR r.startDate <= :startDateTo) AND " +
           "(:endDateFrom IS NULL OR r.endDate >= :endDateFrom) AND " +
           "(:endDateTo IS NULL OR r.endDate <= :endDateTo) AND " +
           "(:customerName IS NULL OR CONCAT(COALESCE(r.firstName, r.user.firstName), ' ', COALESCE(r.lastName, r.user.lastName)) LIKE CONCAT('%', :customerName, '%')) AND " +
           "(:phone IS NULL OR COALESCE(r.phone, r.user.phone) LIKE CONCAT('%', :phone, '%')) AND " +
           "(:email IS NULL OR COALESCE(r.email, r.user.email) LIKE CONCAT('%', :email, '%')) AND " +
           "(:packId IS NULL OR r.pack.id = :packId) AND " +
           "(:materielId IS NULL OR r.materiel.id = :materielId)")
    Page<Reservation> findWithFilters(@Param("status") ReservationStatus status,
                                      @Param("paymentStatus") PaymentStatus paymentStatus,
                                      @Param("startDateFrom") LocalDate startDateFrom,
                                      @Param("startDateTo") LocalDate startDateTo,
                                      @Param("endDateFrom") LocalDate endDateFrom,
                                      @Param("endDateTo") LocalDate endDateTo,
                                      @Param("customerName") String customerName,
                                      @Param("phone") String phone,
                                      @Param("email") String email,
                                      @Param("packId") Long packId,
                                      @Param("materielId") Long materielId,
                                      Pageable pageable);
    
    /**
     * Trouver les réservations avec filtres avancés pour un utilisateur spécifique
     */
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:paymentStatus IS NULL OR r.paymentStatus = :paymentStatus) AND " +
           "(:startDateFrom IS NULL OR r.startDate >= :startDateFrom) AND " +
           "(:startDateTo IS NULL OR r.startDate <= :startDateTo) AND " +
           "(:endDateFrom IS NULL OR r.endDate >= :endDateFrom) AND " +
           "(:endDateTo IS NULL OR r.endDate <= :endDateTo) AND " +
           "(:customerName IS NULL OR CONCAT(COALESCE(r.firstName, r.user.firstName), ' ', COALESCE(r.lastName, r.user.lastName)) LIKE CONCAT('%', :customerName, '%')) AND " +
           "(:phone IS NULL OR COALESCE(r.phone, r.user.phone) LIKE CONCAT('%', :phone, '%')) AND " +
           "(:email IS NULL OR COALESCE(r.email, r.user.email) LIKE CONCAT('%', :email, '%')) AND " +
           "(:packId IS NULL OR r.pack.id = :packId) AND " +
           "(:materielId IS NULL OR r.materiel.id = :materielId)")
    Page<Reservation> findWithFiltersForUser(@Param("userId") Long userId,
                                           @Param("status") ReservationStatus status,
                                           @Param("paymentStatus") PaymentStatus paymentStatus,
                                           @Param("startDateFrom") LocalDate startDateFrom,
                                           @Param("startDateTo") LocalDate startDateTo,
                                           @Param("endDateFrom") LocalDate endDateFrom,
                                           @Param("endDateTo") LocalDate endDateTo,
                                           @Param("customerName") String customerName,
                                           @Param("phone") String phone,
                                           @Param("email") String email,
                                           @Param("packId") Long packId,
                                           @Param("materielId") Long materielId,
                                           Pageable pageable);
    
    /**
     * Compter les réservations en attente
     */
    long countByStatus(ReservationStatus status);
    
    /**
     * Compter les réservations par période
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Somme des prix totaux par période et statut
     */
    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM Reservation r WHERE r.createdAt BETWEEN :startDate AND :endDate AND r.status = :status")
    BigDecimal sumTotalPriceByCreatedAtBetweenAndStatus(@Param("startDate") LocalDateTime startDate, 
                                                       @Param("endDate") LocalDateTime endDate, 
                                                       @Param("status") ReservationStatus status);
    
    /**
     * Top clients par revenus
     */
    @Query("SELECT CONCAT(COALESCE(r.user.firstName, ''), ' ', COALESCE(r.user.lastName, r.user.email)) as clientName, " +
           "r.user.email, SUM(r.totalAmount) as revenue FROM Reservation r " +
           "WHERE r.status = 'CONFIRMEE' GROUP BY r.user.id, r.user.firstName, r.user.lastName, r.user.email ORDER BY revenue DESC")
    List<Object[]> findTopClientsByRevenue();
    
    /**
     * Top packs par nombre de réservations
     */
    @Query("SELECT r.pack.id, r.pack.name, COUNT(r) as count, " +
           "SUM(r.totalAmount) as revenue, AVG(r.totalAmount) as averagePrice FROM Reservation r " +
           "WHERE r.pack IS NOT NULL AND r.status = 'CONFIRMEE' " +
           "GROUP BY r.pack.id, r.pack.name ORDER BY count DESC")
    List<Object[]> findTopPacksByReservations();
    
    /**
     * Équipements populaires par nombre de réservations
     */
    @Query("SELECT r.materiel.id, r.materiel.name, r.materiel.categorie.name, COUNT(r) as count, " +
           "SUM(r.totalAmount) as revenue, 4.5 as rating FROM Reservation r " +
           "WHERE r.materiel IS NOT NULL AND r.status = 'CONFIRMEE' " +
           "GROUP BY r.materiel.id, r.materiel.name, r.materiel.categorie.name " +
           "ORDER BY count DESC")
    List<Object[]> findPopularEquipments();
    
    /**
     * Statistiques des réservations par statut
     */
    @Query("SELECT r.status, COUNT(r) as count, " +
           "(COUNT(r) * 100.0 / (SELECT COUNT(r2) FROM Reservation r2)) as percentage, " +
           "SUM(r.totalAmount) as revenue FROM Reservation r " +
           "GROUP BY r.status ORDER BY count DESC")
    List<Object[]> findReservationStatusStats();
    
    /**
     * Trouver les réservations qui nécessitent une attention (stock bas, etc.)
     */
    @Query("SELECT r FROM Reservation r WHERE r.status = 'DEMANDE' AND " +
           "((r.materiel IS NOT NULL AND EXISTS (SELECT m FROM Materiel m WHERE m.id = r.materiel.id AND m.availableQuantity <= m.minimumStock)) OR " +
           "(r.pack IS NOT NULL AND EXISTS (SELECT p FROM Pack p JOIN p.packMateriels pm JOIN pm.materiel m WHERE p.id = r.pack.id AND m.availableQuantity <= m.minimumStock)))")
    List<Reservation> findReservationsRequiringAttention();
    
    /**
     * Statistiques des revenus par pack (réservations confirmées uniquement)
     */
    @Query("SELECT r.pack.name, SUM(r.totalAmount) as revenue, COUNT(r) as reservations, " +
           "AVG(r.totalAmount) as averagePrice FROM Reservation r " +
           "WHERE r.pack IS NOT NULL AND r.status = 'CONFIRMEE' " +
           "GROUP BY r.pack.id, r.pack.name ORDER BY revenue DESC")
    List<Object[]> findPackRevenueStats();
    
    /**
     * Produit le plus rentable ce mois
     */
    @Query(value = "SELECT " +
           "CASE " +
           "  WHEN r.pack_id IS NOT NULL THEN p.name " +
           "  ELSE m.name " +
           "END as productName, " +
           "CASE " +
           "  WHEN r.pack_id IS NOT NULL THEN 'PACK' " +
           "  ELSE 'MATERIEL' " +
           "END as productType, " +
           "SUM(r.total_amount) as revenue, " +
           "COUNT(r.id) as reservations, " +
           "AVG(r.total_amount) as averagePrice " +
           "FROM reservations r " +
           "LEFT JOIN packs p ON r.pack_id = p.id " +
           "LEFT JOIN materiels m ON r.materiel_id = m.id " +
           "WHERE r.status = 'CONFIRMEE' " +
           "AND EXTRACT(YEAR FROM r.created_at) = EXTRACT(YEAR FROM CURRENT_DATE) " +
           "AND EXTRACT(MONTH FROM r.created_at) = EXTRACT(MONTH FROM CURRENT_DATE) " +
           "GROUP BY r.pack_id, r.materiel_id, p.name, m.name " +
           "ORDER BY revenue DESC", nativeQuery = true)
    List<Object[]> findMostProfitableProductThisMonth();
    
    /**
     * Statistiques des réservations par jour de la semaine (réservations confirmées uniquement)
     */
    @Query(value = "SELECT " +
           "CASE " +
           "  WHEN EXTRACT(DOW FROM r.created_at) = 0 THEN 'SUNDAY' " +
           "  WHEN EXTRACT(DOW FROM r.created_at) = 1 THEN 'MONDAY' " +
           "  WHEN EXTRACT(DOW FROM r.created_at) = 2 THEN 'TUESDAY' " +
           "  WHEN EXTRACT(DOW FROM r.created_at) = 3 THEN 'WEDNESDAY' " +
           "  WHEN EXTRACT(DOW FROM r.created_at) = 4 THEN 'THURSDAY' " +
           "  WHEN EXTRACT(DOW FROM r.created_at) = 5 THEN 'FRIDAY' " +
           "  WHEN EXTRACT(DOW FROM r.created_at) = 6 THEN 'SATURDAY' " +
           "END as dayOfWeek, " +
           "COUNT(r.id) as reservations " +
           "FROM reservations r " +
           "WHERE r.status = 'CONFIRMEE' " +
           "GROUP BY EXTRACT(DOW FROM r.created_at) " +
           "ORDER BY reservations DESC", nativeQuery = true)
    List<Object[]> findReservationsByDayOfWeek();
    
    /**
     * Statistiques des réservations approuvées vs non approuvées par mois (12 derniers mois)
     */
    @Query(value = "SELECT " +
           "EXTRACT(YEAR FROM r.created_at) as year, " +
           "EXTRACT(MONTH FROM r.created_at) as month, " +
           "SUM(CASE WHEN r.status = 'CONFIRMEE' THEN 1 ELSE 0 END) as approved, " +
           "SUM(CASE WHEN r.status = 'ANNULEE' OR r.status = 'REJETEE' THEN 1 ELSE 0 END) as rejected, " +
           "COUNT(r.id) as total " +
           "FROM reservations r " +
           "WHERE r.created_at >= :startDate " +
           "GROUP BY EXTRACT(YEAR FROM r.created_at), EXTRACT(MONTH FROM r.created_at) " +
           "ORDER BY year DESC, month DESC", nativeQuery = true)
    List<Object[]> findApprovalStatsByMonth(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Trouver toutes les réservations (tous statuts) qui chevauchent une période donnée
     * Utilisé pour le calendrier des réservations
     */
    @Query("SELECT r FROM Reservation r WHERE " +
           "((r.startDate BETWEEN :startDate AND :endDate) OR " +
           "(r.endDate BETWEEN :startDate AND :endDate) OR " +
           "(r.startDate <= :startDate AND r.endDate >= :endDate)) " +
           "ORDER BY r.startDate ASC, r.createdAt DESC")
    List<Reservation> findReservationsBetweenDates(@Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
}




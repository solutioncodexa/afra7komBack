package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationItemRepository extends JpaRepository<ReservationItem, Long> {

    List<ReservationItem> findByReservationId(Long reservationId);
    
    List<ReservationItem> findByMaterielId(Long materielId);
    
    List<ReservationItem> findByPackId(Long packId);

    // Recherche par type d'item
    List<ReservationItem> findByType(ReservationItem.ItemType type);
    
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.reservation.id = :reservationId AND ri.type = :type")
    List<ReservationItem> findByReservationIdAndType(@Param("reservationId") Long reservationId,
                                                    @Param("type") ReservationItem.ItemType type);

    // Items livrés/retournés
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.delivered = :delivered")
    List<ReservationItem> findByDelivered(@Param("delivered") Boolean delivered);
    
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.returned = :returned")
    List<ReservationItem> findByReturned(@Param("returned") Boolean returned);
    
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.delivered = true AND ri.returned = false")
    List<ReservationItem> findDeliveredButNotReturned();

    // Items avec dégâts
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.damageNotes IS NOT NULL AND ri.damageNotes != ''")
    List<ReservationItem> findItemsWithDamage();

    // Quantité totale réservée pour un matériel sur une période
    @Query("SELECT COALESCE(SUM(ri.quantity), 0) FROM ReservationItem ri " +
           "WHERE ri.materiel.id = :materielId AND " +
           "ri.reservation.status IN ('CONFIRMEE', 'EN_COURS') AND " +
           "(ri.reservation.startDate <= :endDate AND ri.reservation.endDate >= :startDate)")
    Integer getTotalReservedQuantityForMateriel(@Param("materielId") Long materielId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    // Vérifier si un pack est réservé sur une période
    @Query("SELECT COUNT(ri) > 0 FROM ReservationItem ri " +
           "WHERE ri.pack.id = :packId AND " +
           "ri.reservation.status IN ('CONFIRMEE', 'EN_COURS') AND " +
           "(ri.reservation.startDate <= :endDate AND ri.reservation.endDate >= :startDate)")
    boolean isPackReservedInPeriod(@Param("packId") Long packId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    // Statistiques
    @Query("SELECT COUNT(ri) FROM ReservationItem ri WHERE ri.type = :type")
    long countByType(@Param("type") ReservationItem.ItemType type);

    @Query("SELECT ri.materiel.id, COUNT(ri) FROM ReservationItem ri " +
           "WHERE ri.type = 'MATERIEL' GROUP BY ri.materiel.id ORDER BY COUNT(ri) DESC")
    List<Object[]> findMostReservedMateriels();

    @Query("SELECT ri.pack.id, COUNT(ri) FROM ReservationItem ri " +
           "WHERE ri.type = 'PACK' GROUP BY ri.pack.id ORDER BY COUNT(ri) DESC")
    List<Object[]> findMostReservedPacks();

    // Items par réservation avec détails
    @Query("SELECT ri FROM ReservationItem ri " +
           "LEFT JOIN FETCH ri.materiel " +
           "LEFT JOIN FETCH ri.pack " +
           "WHERE ri.reservation.id = :reservationId")
    List<ReservationItem> findByReservationIdWithDetails(@Param("reservationId") Long reservationId);
}




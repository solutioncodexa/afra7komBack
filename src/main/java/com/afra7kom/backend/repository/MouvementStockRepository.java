package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.MouvementStock;
import com.afra7kom.backend.entity.Materiel;

import com.afra7kom.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    // Recherche par matériel
    List<MouvementStock> findByMaterielId(Long materielId);
    
    Page<MouvementStock> findByMaterielId(Long materielId, Pageable pageable);
    
    List<MouvementStock> findByMateriel(Materiel materiel);



    // Recherche par utilisateur
    List<MouvementStock> findByUserId(Long userId);
    
    List<MouvementStock> findByUser(User user);

    // Recherche par type de mouvement
    List<MouvementStock> findByType(MouvementStock.TypeMouvement type);
    
    Page<MouvementStock> findByType(MouvementStock.TypeMouvement type, Pageable pageable);



    // Calcul du stock actuel
    @Query("SELECT COALESCE(SUM(CASE WHEN m.type IN ('ACHAT', 'RETOUR', 'TRANSFERT_ENTRANT', 'CORRECTION_POSITIVE') " +
           "THEN m.quantity ELSE -m.quantity END), 0) " +
           "FROM MouvementStock m WHERE m.materiel.id = :materielId AND m.valide = true")
    Integer calculateStockActuel(@Param("materielId") Long materielId);



    // Stock global par matériel
    @Query("SELECT COALESCE(SUM(CASE WHEN m.type IN ('ACHAT', 'RETOUR', 'TRANSFERT_ENTRANT', 'CORRECTION_POSITIVE') " +
           "THEN m.quantity ELSE -m.quantity END), 0) " +
           "FROM MouvementStock m WHERE m.materiel.id = :materielId AND m.valide = true")
    Integer getStockGlobal(@Param("materielId") Long materielId);

    // Mouvements par période
    @Query("SELECT m FROM MouvementStock m WHERE m.date BETWEEN :startDate AND :endDate")
    List<MouvementStock> findByDateBetween(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);



    // Mouvements récents
    @Query("SELECT m FROM MouvementStock m WHERE m.createdAt >= :since ORDER BY m.createdAt DESC")
    List<MouvementStock> findRecentMovements(@Param("since") LocalDateTime since);

    // Mouvements en attente de validation
    List<MouvementStock> findByValideFalse();
    
    @Query("SELECT m FROM MouvementStock m WHERE m.valide = false ORDER BY m.createdAt ASC")
    List<MouvementStock> findPendingValidation();

    // Mouvements par réservation
    List<MouvementStock> findByReservationId(Long reservationId);

    // Statistiques des entrées/sorties
    @Query("SELECT m.type, COUNT(m), SUM(m.quantity) FROM MouvementStock m " +
           "WHERE m.date BETWEEN :startDate AND :endDate AND m.valide = true " +
           "GROUP BY m.type")
    List<Object[]> getStatistiquesParType(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    // Mouvements avec coût
    @Query("SELECT m FROM MouvementStock m WHERE m.coutTotal IS NOT NULL AND m.coutTotal > 0")
    List<MouvementStock> findMovementsWithCost();



    // Recherche avec filtres multiples
    @Query("SELECT m FROM MouvementStock m WHERE " +
           "(:materielId IS NULL OR m.materiel.id = :materielId) AND " +
           "(:type IS NULL OR m.type = :type) AND " +
           "(:userId IS NULL OR m.user.id = :userId) AND " +
           "(:startDate IS NULL OR m.date >= :startDate) AND " +
           "(:endDate IS NULL OR m.date <= :endDate) AND " +
           "(:valide IS NULL OR m.valide = :valide)")
    Page<MouvementStock> findWithFilters(@Param("materielId") Long materielId,
                                        @Param("type") MouvementStock.TypeMouvement type,
                                        @Param("userId") Long userId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        @Param("valide") Boolean valide,
                                        Pageable pageable);

    // Matériels avec stock faible
    @Query("SELECT m.materiel.id, m.materiel.name, " +
           "SUM(CASE WHEN m.type IN ('ACHAT', 'RETOUR', 'TRANSFERT_ENTRANT', 'CORRECTION_POSITIVE') " +
           "THEN m.quantity ELSE -m.quantity END) " +
           "FROM MouvementStock m WHERE m.valide = true " +
           "GROUP BY m.materiel.id, m.materiel.name " +
           "HAVING SUM(CASE WHEN m.type IN ('ACHAT', 'RETOUR', 'TRANSFERT_ENTRANT', 'CORRECTION_POSITIVE') " +
           "THEN m.quantity ELSE -m.quantity END) <= :seuilMin")
    List<Object[]> findMaterielsStockFaible(@Param("seuilMin") Integer seuilMin);

    // Matériels sans mouvement depuis X jours
    @Query("SELECT DISTINCT m.materiel FROM MouvementStock m " +
           "WHERE m.materiel.id NOT IN (" +
           "    SELECT m2.materiel.id FROM MouvementStock m2 " +
           "    WHERE m2.date >= :dateLimite" +
           ")")
    List<Materiel> findMaterielsInactifs(@Param("dateLimite") LocalDateTime dateLimite);

    // Historique complet d'un matériel
    @Query("SELECT m FROM MouvementStock m " +
           "WHERE m.materiel.id = :materielId " +
           "ORDER BY m.date DESC, m.createdAt DESC")
    List<MouvementStock> getHistoriqueMateriel(@Param("materielId") Long materielId);

    // Dernier mouvement d'un matériel
    @Query("SELECT m FROM MouvementStock m WHERE m.id = (" +
           "    SELECT MAX(m2.id) FROM MouvementStock m2 " +
           "    WHERE m2.materiel.id = :materielId" +
           ")")
    MouvementStock getDernierMouvement(@Param("materielId") Long materielId);

    // Mouvements de transfert
    @Query("SELECT m FROM MouvementStock m WHERE m.type IN ('TRANSFERT_ENTRANT', 'TRANSFERT_SORTANT')")
    List<MouvementStock> findTransferts();

    // Mouvements de correction d'inventaire
    @Query("SELECT m FROM MouvementStock m WHERE m.type IN ('CORRECTION_POSITIVE', 'CORRECTION_NEGATIVE')")
    List<MouvementStock> findCorrections();

    // Achats par fournisseur
    @Query("SELECT m.fournisseur, COUNT(m), SUM(m.quantity), SUM(m.coutTotal) " +
           "FROM MouvementStock m WHERE m.type = 'ACHAT' AND m.fournisseur IS NOT NULL " +
           "GROUP BY m.fournisseur")
    List<Object[]> getStatistiquesAchatsParFournisseur();
}




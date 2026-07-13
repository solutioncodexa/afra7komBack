package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Categorie;
import com.afra7kom.backend.entity.Materiel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MaterielRepository extends JpaRepository<Materiel, Long>, JpaSpecificationExecutor<Materiel> {

    Page<Materiel> findByActiveTrue(Pageable pageable);

    long countByActiveTrue();
    
    Page<Materiel> findByActiveTrueAndCategorie(Categorie categorie, Pageable pageable);
    
    Page<Materiel> findByActiveTrueAndCategorieId(Long categorieId, Pageable pageable);
    
    @Query("SELECT m FROM Materiel m WHERE m.active = true AND " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Materiel> findByActiveTrueAndNameContainingIgnoreCase(@Param("search") String search, Pageable pageable);
    

    
    @Query("SELECT m FROM Materiel m WHERE m.active = true AND m.availableQuantity > 0")
    List<Materiel> findAvailableMateriels();
    
    @Query("SELECT m FROM Materiel m WHERE m.active = true AND m.minimumStock IS NOT NULL AND m.availableQuantity <= m.minimumStock")
    List<Materiel> findLowStockMateriels();
    
    @Query("SELECT m FROM Materiel m WHERE m.active = true AND m.availableQuantity = 0")
    List<Materiel> findOutOfStockMateriels();
    
    @Query("SELECT COUNT(m) FROM Materiel m WHERE m.active = true AND m.categorie.id = :categorieId")
    long countByCategorieId(@Param("categorieId") Long categorieId);
    
    @Query("SELECT m FROM Materiel m JOIN m.favoris f WHERE f.user.id = :userId AND m.active = true")
    Page<Materiel> findFavoritesByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT m FROM Materiel m WHERE m.active = true ORDER BY " +
           "(SELECT COUNT(f) FROM Favoris f WHERE f.materiel = m) DESC")
    Page<Materiel> findMostFavorited(Pageable pageable);
    
    @Query("SELECT m FROM Materiel m WHERE m.categorie.name LIKE %:category%")
    Page<Materiel> findByCategorieNomContainingIgnoreCase(@Param("category") String category, Pageable pageable);
    
    Page<Materiel> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description, Pageable pageable);
    
    @Query("SELECT m FROM Materiel m WHERE " +
           "(:active IS NULL OR m.active = :active) AND " +
           "(:categorieId IS NULL OR m.categorie.id = :categorieId) AND " +
           "(:minPrice IS NULL OR m.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR m.price <= :maxPrice) AND " +
           "(:isAvailable IS NULL OR (CASE WHEN :isAvailable = true THEN (m.active = true AND m.availableQuantity > 0) ELSE (m.active = false OR m.availableQuantity = 0) END))")
    Page<Materiel> findWithFilters(@Param("active") Boolean active,
                                  @Param("categorieId") Long categorieId,
                                  @Param("minPrice") BigDecimal minPrice,
                                  @Param("maxPrice") BigDecimal maxPrice,
                                  @Param("isAvailable") Boolean isAvailable,
                                  Pageable pageable);
    
    Page<Materiel> findByCategorieIdAndActiveTrue(Long categorieId, Pageable pageable);
    
    @Query("SELECT m FROM Materiel m WHERE m.active = true AND m.availableQuantity > 0")
    Page<Materiel> findByActiveTrueAndAvailableTrue(Pageable pageable);
    
    @Query("SELECT m FROM Materiel m WHERE m.active = true ORDER BY " +
           "(SELECT COUNT(f) FROM Favoris f WHERE f.materiel = m) DESC")
    Page<Materiel> findMostFavoritedMateriels(Pageable pageable);
    
    @Query("SELECT m FROM Materiel m WHERE m.active = true ORDER BY " +
           "(SELECT COUNT(ri) FROM ReservationItem ri WHERE ri.materiel = m) DESC")
    Page<Materiel> findMostRentedMateriels(Pageable pageable);
    
    @Query("SELECT COUNT(ri) FROM ReservationItem ri WHERE ri.materiel.id = :materielId AND ri.reservation.status = 'ACTIVE'")
    long countActiveReservationsByMaterielId(@Param("materielId") Long materielId);
}




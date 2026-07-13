package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Categorie;
import com.afra7kom.backend.entity.Pack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PackRepository extends JpaRepository<Pack, Long>, JpaSpecificationExecutor<Pack> {

    Page<Pack> findByActiveTrue(Pageable pageable);
    
    Page<Pack> findByActiveTrueAndCategorie(Categorie categorie, Pageable pageable);
    
    Page<Pack> findByActiveTrueAndCategorieId(Long categorieId, Pageable pageable);
    
    @Query("SELECT p FROM Pack p WHERE p.active = true AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Pack> findByActiveTrueAndNameContainingIgnoreCase(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT p FROM Pack p WHERE " +
           "(:active IS NULL OR p.active = :active) AND " +
           "(:categorieId IS NULL OR p.categorie.id = :categorieId) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(CAST(p.name AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Pack> findWithFilters(@Param("active") Boolean active,
                              @Param("categorieId") Long categorieId,
                              @Param("search") String search,
                              @Param("minPrice") BigDecimal minPrice,
                              @Param("maxPrice") BigDecimal maxPrice,
                              Pageable pageable);
    
    @Query("SELECT p FROM Pack p LEFT JOIN FETCH p.packMateriels pm LEFT JOIN FETCH pm.materiel WHERE p.id = :id")
    Optional<Pack> findByIdWithMateriels(@Param("id") Long id);
    
    @Query("SELECT p FROM Pack p LEFT JOIN FETCH p.packMateriels pm LEFT JOIN FETCH pm.materiel WHERE p.id = :id")
    Optional<Pack> findByIdWithMaterielsAndImages(@Param("id") Long id);
    
    @Query("SELECT p FROM Pack p WHERE p.active = true AND " +
           "NOT EXISTS (SELECT pm FROM PackMateriel pm WHERE pm.pack = p AND pm.materiel.availableQuantity < pm.quantity)")
    List<Pack> findAvailablePacks();
    
    @Query("SELECT COUNT(p) FROM Pack p WHERE p.active = true AND p.categorie.id = :categorieId")
    long countByCategorieId(@Param("categorieId") Long categorieId);

    @Query("SELECT p.type, COUNT(p) FROM Pack p WHERE p.active = true GROUP BY p.type")
    List<Object[]> countActiveByType();
    
    @Query("SELECT p FROM Pack p JOIN p.favoris f WHERE f.user.id = :userId AND p.active = true")
    Page<Pack> findFavoritesByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT p FROM Pack p WHERE p.active = true ORDER BY " +
           "(SELECT COUNT(f) FROM Favoris f WHERE f.pack = p) DESC")
    Page<Pack> findMostFavorited(Pageable pageable);
    
    @Query("SELECT p FROM Pack p WHERE p.active = true ORDER BY " +
           "(SELECT COUNT(r) FROM Reservation r WHERE r.pack = p) DESC")
    Page<Pack> findMostRented(Pageable pageable);
    

}




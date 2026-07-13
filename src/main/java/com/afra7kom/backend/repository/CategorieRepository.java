package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Categorie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    Optional<Categorie> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Categorie> findByActiveTrue();
    
    List<Categorie> findByActiveTrueOrderBySortOrderAscNameAsc();
    
    Page<Categorie> findByActiveTrue(Pageable pageable);
    
    @Query("SELECT c FROM Categorie c WHERE " +
           "(:active IS NULL OR c.active = :active) AND " +
           "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Categorie> findWithFilters(@Param("active") Boolean active,
                                   @Param("name") String name,
                                   Pageable pageable);
    
    @Query("SELECT c FROM Categorie c WHERE c.active = true AND " +
           "(SIZE(c.packs) > 0 OR SIZE(c.materiels) > 0)")
    List<Categorie> findCategoriesWithItems();
    
    @Query("SELECT c FROM Categorie c WHERE c.active = true AND SIZE(c.packs) > 0")
    List<Categorie> findCategoriesWithPacks();
    
    @Query("SELECT c FROM Categorie c WHERE c.active = true AND SIZE(c.materiels) > 0")
    List<Categorie> findCategoriesWithMateriels();
    
    @Query("SELECT COUNT(p) FROM Pack p WHERE p.categorie.id = :categorieId")
    long countPacksByCategorieId(@Param("categorieId") Long categorieId);
    
    @Query("SELECT COUNT(m) FROM Materiel m WHERE m.categorie.id = :categorieId")
    long countMaterielsByCategorieId(@Param("categorieId") Long categorieId);
}




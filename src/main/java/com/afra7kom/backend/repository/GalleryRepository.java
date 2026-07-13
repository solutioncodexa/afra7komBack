package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Gallery;
import com.afra7kom.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalleryRepository extends JpaRepository<Gallery, Long> {

    // Trouver toutes les images actives
    List<Gallery> findByActiveTrueOrderBySortOrderAscCreatedAtDesc();

    // Trouver les images par catégorie
    List<Gallery> findByCategoryAndActiveTrueOrderBySortOrderAscCreatedAtDesc(String category);

    // Trouver les images mises en avant
    List<Gallery> findByFeaturedTrueAndActiveTrueOrderBySortOrderAscCreatedAtDesc();

    // Pagination pour les images actives
    Page<Gallery> findByActiveTrue(Pageable pageable);

    // Pagination par catégorie
    Page<Gallery> findByCategoryAndActiveTrue(String category, Pageable pageable);

    // Recherche par titre ou description
    @Query("SELECT g FROM Gallery g WHERE g.active = true AND (LOWER(g.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY g.sortOrder ASC, g.createdAt DESC")
    Page<Gallery> searchActiveImages(@Param("search") String search, Pageable pageable);

    // Compter les images par catégorie
    @Query("SELECT g.category, COUNT(g) FROM Gallery g WHERE g.active = true GROUP BY g.category")
    List<Object[]> countByCategory();

    // Trouver les images créées par un utilisateur
    List<Gallery> findByCreatedByOrderByCreatedAtDesc(User createdBy);
}


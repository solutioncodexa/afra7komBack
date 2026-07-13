package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Favoris;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavorisRepository extends JpaRepository<Favoris, Long> {

    Page<Favoris> findByUserId(Long userId, Pageable pageable);
    
    List<Favoris> findByUserId(Long userId);
    
    Page<Favoris> findByUserIdAndType(Long userId, Favoris.FavorisType type, Pageable pageable);
    
    List<Favoris> findByUserIdAndType(Long userId, Favoris.FavorisType type);
    
    Optional<Favoris> findByUserIdAndPackId(Long userId, Long packId);
    
    Optional<Favoris> findByUserIdAndMaterielId(Long userId, Long materielId);
    
    boolean existsByUserIdAndPackId(Long userId, Long packId);
    
    boolean existsByUserIdAndMaterielId(Long userId, Long materielId);
    
    void deleteByUserIdAndPackId(Long userId, Long packId);
    
    void deleteByUserIdAndMaterielId(Long userId, Long materielId);
    
    @Query("SELECT COUNT(f) FROM Favoris f WHERE f.pack.id = :packId")
    long countByPackId(@Param("packId") Long packId);
    
    @Query("SELECT COUNT(f) FROM Favoris f WHERE f.materiel.id = :materielId")
    long countByMaterielId(@Param("materielId") Long materielId);
    
    @Query("SELECT f FROM Favoris f JOIN FETCH f.pack WHERE f.user.id = :userId AND f.type = 'PACK'")
    List<Favoris> findPackFavoritesByUserIdWithPack(@Param("userId") Long userId);
    
    @Query("SELECT f FROM Favoris f JOIN FETCH f.materiel WHERE f.user.id = :userId AND f.type = 'MATERIEL'")
    List<Favoris> findMaterielFavoritesByUserIdWithMateriel(@Param("userId") Long userId);
}




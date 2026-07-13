package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.PackMateriel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackMaterielRepository extends JpaRepository<PackMateriel, Long> {

    List<PackMateriel> findByPackId(Long packId);
    
    List<PackMateriel> findByMaterielId(Long materielId);
    
    @Query("SELECT pm FROM PackMateriel pm JOIN FETCH pm.materiel WHERE pm.pack.id = :packId")
    List<PackMateriel> findByPackIdWithMateriel(@Param("packId") Long packId);
    
    @Query("SELECT pm FROM PackMateriel pm WHERE pm.pack.id = :packId AND pm.isOptional = false")
    List<PackMateriel> findRequiredByPackId(@Param("packId") Long packId);
    
    @Query("SELECT pm FROM PackMateriel pm WHERE pm.pack.id = :packId AND pm.isOptional = true")
    List<PackMateriel> findOptionalByPackId(@Param("packId") Long packId);
    
    @Query("SELECT pm FROM PackMateriel pm WHERE pm.materiel.availableQuantity < pm.quantity")
    List<PackMateriel> findUnavailableItems();
    
    @Query("SELECT COUNT(pm) FROM PackMateriel pm WHERE pm.pack.id = :packId")
    long countByPackId(@Param("packId") Long packId);
    
    @Query("SELECT SUM(pm.quantity) FROM PackMateriel pm WHERE pm.pack.id = :packId")
    Integer getTotalQuantityByPackId(@Param("packId") Long packId);
    
    @Modifying
    @Query("DELETE FROM PackMateriel pm WHERE pm.pack.id = :packId AND pm.materiel.id = :materielId")
    void deleteByPackIdAndMaterielId(@Param("packId") Long packId, @Param("materielId") Long materielId);
    
    @Modifying
    @Query("DELETE FROM PackMateriel pm WHERE pm.pack.id = :packId")
    void deleteByPackId(@Param("packId") Long packId);
}




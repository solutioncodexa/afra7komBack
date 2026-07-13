package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.PackImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackImageRepository extends JpaRepository<PackImage, Long> {

    List<PackImage> findByPackId(Long packId);
    
    List<PackImage> findByPackIdOrderBySortOrder(Long packId);
    
    List<PackImage> findByGalleryId(Long galleryId);
    
    @Query("SELECT pi FROM PackImage pi JOIN FETCH pi.gallery WHERE pi.pack.id = :packId ORDER BY pi.sortOrder")
    List<PackImage> findByPackIdWithGallery(@Param("packId") Long packId);
    
    @Query("SELECT pi FROM PackImage pi JOIN FETCH pi.pack WHERE pi.gallery.id = :galleryId")
    List<PackImage> findByGalleryIdWithPack(@Param("galleryId") Long galleryId);
    
    void deleteByPackId(Long packId);
    
    void deleteByGalleryId(Long galleryId);
    
    @Query("SELECT COUNT(pi) FROM PackImage pi WHERE pi.pack.id = :packId")
    long countByPackId(@Param("packId") Long packId);
    
    @Query("SELECT COUNT(pi) FROM PackImage pi WHERE pi.gallery.id = :galleryId")
    long countByGalleryId(@Param("galleryId") Long galleryId);
    
    void deleteByPackIdAndGalleryId(Long packId, Long galleryId);
}

package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);
    
    boolean existsByName(String name);
    
    @Query("SELECT p FROM Permission p WHERE p.name IN :names")
    List<Permission> findByNames(@Param("names") Set<String> names);
    
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource")
    List<Permission> findByResource(@Param("resource") String resource);
    
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.action = :action")
    Optional<Permission> findByResourceAndAction(@Param("resource") String resource, 
                                               @Param("action") String action);
    
    @Query("SELECT DISTINCT p.resource FROM Permission p WHERE p.resource IS NOT NULL")
    List<String> findAllResources();
    
    @Query("SELECT DISTINCT p.action FROM Permission p WHERE p.action IS NOT NULL")
    List<String> findAllActions();
}




package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(Role.RoleName name);
    
    boolean existsByName(Role.RoleName name);
    
    @Query("SELECT r FROM Role r WHERE r.name IN :names")
    List<Role> findByNames(@Param("names") Set<Role.RoleName> names);
    
    @Query("SELECT r FROM Role r JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(@Param("name") Role.RoleName name);
    
    @Query("SELECT r FROM Role r JOIN FETCH r.permissions")
    List<Role> findAllWithPermissions();
    
    @Query("SELECT r FROM Role r WHERE SIZE(r.users) > 0")
    List<Role> findRolesWithUsers();
}




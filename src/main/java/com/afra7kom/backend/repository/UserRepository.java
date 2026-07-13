package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Role;
import com.afra7kom.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Méthode avec JOIN FETCH pour charger les rôles et permissions
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.email = :email")
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);

    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    Optional<User> findByPasswordResetToken(String token);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    Page<User> findByRoleName(@Param("roleName") Role.RoleName roleName, Pageable pageable);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    Page<User> findByRoleNames(@Param("roleNames") List<Role.RoleName> roleNames, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.enabled = :enabled")
    Page<User> findByEnabled(@Param("enabled") Boolean enabled, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:phone IS NULL OR u.phone LIKE CONCAT('%', :phone, '%')) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> findWithFilters(@Param("email") String email,
                              @Param("phone") String phone,
                              @Param("enabled") Boolean enabled,
                              Pageable pageable);
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") Role.RoleName roleName);
    
    @Query("SELECT u FROM User u WHERE u.createdAt >= :since")
    List<User> findRecentUsers(@Param("since") java.time.LocalDateTime since);
}




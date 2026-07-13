package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Materiel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentRepository extends JpaRepository<Materiel, Long> {

    // Compter les matériels actifs
    @Query("SELECT COUNT(m) FROM Materiel m WHERE m.active = true")
    long countActiveEquipments();

    // Compter les matériels disponibles (actifs et avec quantité disponible > 0)
    @Query("SELECT COUNT(m) FROM Materiel m WHERE m.active = true AND m.availableQuantity > 0")
    long countAvailableEquipments();

    // Compter les matériels en rupture de stock (actifs mais sans stock disponible)
    @Query("SELECT COUNT(m) FROM Materiel m WHERE m.active = true AND m.availableQuantity = 0")
    long countOutOfStockEquipments();

    // Compter les matériels inactifs
    @Query("SELECT COUNT(m) FROM Materiel m WHERE m.active = false")
    long countInactiveEquipments();

    // Compter les matériels en stock faible (disponible <= minimum)
    @Query("SELECT COUNT(m) FROM Materiel m WHERE m.active = true AND m.minimumStock IS NOT NULL AND m.availableQuantity <= m.minimumStock")
    long countLowStockEquipments();
}

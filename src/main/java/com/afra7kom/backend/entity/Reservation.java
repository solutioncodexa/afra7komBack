package com.afra7kom.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@EntityListeners(AuditingEntityListener.class)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;
    
    // Champs pour les informations du client (invité ou connecté)
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "city")
    private String city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id")
    private Pack pack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materiel_id")
    private Materiel materiel;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status = ReservationStatus.DEMANDE;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relations
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReservationItem> items;

    public enum ReservationStatus {
        DEMANDE("Demande"),
        EN_ATTENTE_ACOMPTE("En attente d'acompte"),
        CONFIRMEE("Confirmée"),
        EN_COURS("En cours"),
        SOLDEE("Soldée"),
        ANNULEE("Annulée");

        private final String displayName;

        ReservationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum PaymentStatus {
        PENDING("En attente"),
        PARTIAL("Partiel"),
        COMPLETED("Complété"),
        CANCELLED("Annulé");

        private final String displayName;

        PaymentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Constructeurs utilitaires
    public Reservation(User user, LocalDate startDate, LocalDate endDate) {
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Méthodes utilitaires
    public boolean isActive() {
        return status == ReservationStatus.CONFIRMEE || status == ReservationStatus.EN_COURS;
    }

    public boolean isCancellable() {
        return status == ReservationStatus.DEMANDE || 
               status == ReservationStatus.EN_ATTENTE_ACOMPTE ||
               status == ReservationStatus.CONFIRMEE;
    }

    public boolean isModifiable() {
        return status == ReservationStatus.DEMANDE;
    }

    public long getDurationDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public boolean isOverlapping(LocalDate otherStart, LocalDate otherEnd) {
        return !(endDate.isBefore(otherStart) || startDate.isAfter(otherEnd));
    }

    public void updateStatus(ReservationStatus newStatus) {
        this.status = newStatus;
    }

    public void calculateAmounts() {
        if (items != null && !items.isEmpty()) {
            this.totalAmount = items.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculer l'acompte (30% du total par défaut)
            this.depositAmount = totalAmount.multiply(new BigDecimal("0.30"));
        }
    }
    
    // Getters et setters pour les nouveaux champs
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    // Méthodes utilitaires pour obtenir les IDs
    public Long getPackId() {
        return pack != null ? pack.getId() : null;
    }
    
    public Long getMaterielId() {
        return materiel != null ? materiel.getId() : null;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                '}';
    }
}




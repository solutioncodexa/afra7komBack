package com.afra7kom.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "favoris", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "pack_id"}),
           @UniqueConstraint(columnNames = {"user_id", "materiel_id"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@EntityListeners(AuditingEntityListener.class)
public class Favoris {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id")
    private Pack pack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materiel_id")
    private Materiel materiel;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private FavorisType type;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum FavorisType {
        PACK, MATERIEL
    }

    // Constructeurs utilitaires
    public Favoris(User user, Pack pack) {
        this.user = user;
        this.pack = pack;
        this.type = FavorisType.PACK;
    }

    public Favoris(User user, Materiel materiel) {
        this.user = user;
        this.materiel = materiel;
        this.type = FavorisType.MATERIEL;
    }

    // Méthodes utilitaires
    public String getItemName() {
        return type == FavorisType.PACK ? 
            (pack != null ? pack.getName() : "Pack inconnu") :
            (materiel != null ? materiel.getName() : "Matériel inconnu");
    }

    public Long getItemId() {
        return type == FavorisType.PACK ? 
            (pack != null ? pack.getId() : null) :
            (materiel != null ? materiel.getId() : null);
    }

    @Override
    public String toString() {
        return "Favoris{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", type=" + type +
                ", itemId=" + getItemId() +
                ", createdAt=" + createdAt +
                '}';
    }
}




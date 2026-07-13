package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.FavorisDto;
import com.afra7kom.backend.entity.Favoris;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.repository.FavorisRepository;
import com.afra7kom.backend.repository.MaterielRepository;
import com.afra7kom.backend.repository.PackRepository;
import com.afra7kom.backend.repository.UserRepository;
import com.afra7kom.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FavorisService {

    private final FavorisRepository favorisRepository;
    private final UserRepository userRepository;
    private final PackRepository packRepository;
    private final MaterielRepository materielRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<FavorisDto> getUserFavoris(Long userId, Pageable pageable) {
        return favorisRepository.findByUserId(userId, pageable)
                .map(FavorisDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<FavorisDto> getUserFavorisByType(Long userId, Favoris.FavorisType type, Pageable pageable) {
        return favorisRepository.findByUserIdAndType(userId, type, pageable)
                .map(FavorisDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<FavorisDto> getUserPackFavoris(Long userId) {
        return favorisRepository.findPackFavoritesByUserIdWithPack(userId).stream()
                .map(FavorisDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FavorisDto> getUserMaterielFavoris(Long userId) {
        return favorisRepository.findMaterielFavoritesByUserIdWithMateriel(userId).stream()
                .map(FavorisDto::fromEntity)
                .collect(Collectors.toList());
    }

    public FavorisDto addPackToFavoris(Long userId, Long packId) {
        // Vérifier que l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + userId));

        // Vérifier que le pack existe
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + packId));

        // Vérifier si déjà en favoris
        if (favorisRepository.existsByUserIdAndPackId(userId, packId)) {
            throw new BadRequestException("Ce pack est déjà dans vos favoris");
        }

        Favoris favoris = new Favoris(user, pack);
        Favoris savedFavoris = favorisRepository.save(favoris);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "FAVORIS_CREATE",
            "Pack ajouté aux favoris: " + pack.getName(),
            securityUtils.getCurrentIpAddress()
        );

        return FavorisDto.fromEntity(savedFavoris);
    }

    public FavorisDto addMaterielToFavoris(Long userId, Long materielId) {
        // Vérifier que l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + userId));

        // Vérifier que le matériel existe
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + materielId));

        // Vérifier si déjà en favoris
        if (favorisRepository.existsByUserIdAndMaterielId(userId, materielId)) {
            throw new BadRequestException("Ce matériel est déjà dans vos favoris");
        }

        Favoris favoris = new Favoris(user, materiel);
        Favoris savedFavoris = favorisRepository.save(favoris);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "FAVORIS_CREATE",
            "Matériel ajouté aux favoris: " + materiel.getName(),
            securityUtils.getCurrentIpAddress()
        );

        return FavorisDto.fromEntity(savedFavoris);
    }

    public void removePackFromFavoris(Long userId, Long packId) {
        // Vérifier que le favori existe
        if (!favorisRepository.existsByUserIdAndPackId(userId, packId)) {
            throw new ResourceNotFoundException("Ce pack n'est pas dans vos favoris");
        }

        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé avec l'ID: " + packId));

        favorisRepository.deleteByUserIdAndPackId(userId, packId);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "FAVORIS_DELETE",
            "Pack retiré des favoris: " + pack.getName(),
            securityUtils.getCurrentIpAddress()
        );
    }

    public void removeMaterielFromFavoris(Long userId, Long materielId) {
        // Vérifier que le favori existe
        if (!favorisRepository.existsByUserIdAndMaterielId(userId, materielId)) {
            throw new ResourceNotFoundException("Ce matériel n'est pas dans vos favoris");
        }

        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel non trouvé avec l'ID: " + materielId));

        favorisRepository.deleteByUserIdAndMaterielId(userId, materielId);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "FAVORIS_DELETE",
            "Matériel retiré des favoris: " + materiel.getName(),
            securityUtils.getCurrentIpAddress()
        );
    }

    @Transactional(readOnly = true)
    public boolean isPackFavorite(Long userId, Long packId) {
        return favorisRepository.existsByUserIdAndPackId(userId, packId);
    }

    @Transactional(readOnly = true)
    public boolean isMaterielFavorite(Long userId, Long materielId) {
        return favorisRepository.existsByUserIdAndMaterielId(userId, materielId);
    }

    @Transactional(readOnly = true)
    public long countPackFavorites(Long packId) {
        return favorisRepository.countByPackId(packId);
    }

    @Transactional(readOnly = true)
    public long countMaterielFavorites(Long materielId) {
        return favorisRepository.countByMaterielId(materielId);
    }
}




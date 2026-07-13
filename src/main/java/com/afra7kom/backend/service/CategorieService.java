package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.CategorieDto;
import com.afra7kom.backend.entity.Categorie;
import com.afra7kom.backend.exception
        .ResourceNotFoundException;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.repository.CategorieRepository;
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
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final CachedListService cachedListService;

    @Transactional(readOnly = true)
    public List<CategorieDto> getAllCategories() {
        return cachedListService.getCachedActiveCategories();
    }

    @Transactional(readOnly = true)
    public Page<CategorieDto> getAllCategoriesPaginated(Pageable pageable) {
        return categorieRepository.findAll(pageable)
                .map(CategorieDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CategorieDto> getActiveCategories() {
        return cachedListService.getCachedActiveCategories();
    }

    @Transactional(readOnly = true)
    public List<CategorieDto> getCategoriesWithItems() {
        return cachedListService.getCachedCategoriesWithItems();
    }

    @Transactional(readOnly = true)
    public Page<CategorieDto> searchCategories(Boolean active, String name, Pageable pageable) {
        return categorieRepository.findWithFilters(active, name, pageable)
                .map(CategorieDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public CategorieDto getCategorieById(Long id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID: " + id));
        return CategorieDto.fromEntity(categorie);
    }

    public CategorieDto createCategorie(String name, String description, String imageUrl, Integer sortOrder) {
        // Vérifier l'unicité du nom
        if (categorieRepository.existsByName(name)) {
            throw new BadRequestException("Une catégorie avec ce nom existe déjà");
        }

        Categorie categorie = new Categorie();
        categorie.setName(name);
        categorie.setDescription(description);
        // imageUrl supprimé - maintenant géré par ProductImage
        categorie.setSortOrder(sortOrder != null ? sortOrder : 0);
        categorie.setActive(true);

        Categorie savedCategorie = categorieRepository.save(categorie);

        // Créer automatiquement une image pour la nouvelle catégorie
        // TODO: Gérer les images via la nouvelle structure List<String>
        // Les images sont maintenant gérées directement dans l'entité Categorie

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "CATEGORIE_CREATE",
            "Catégorie créée: " + savedCategorie.getName(),
            securityUtils.getCurrentIpAddress()
        );

        return CategorieDto.fromEntity(savedCategorie);
    }

    public CategorieDto updateCategorie(Long id, String name, String description, String imageUrl, 
                                       Boolean active, Integer sortOrder) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID: " + id));

        // Vérifier l'unicité du nom si changé
        if (name != null && !name.equals(categorie.getName()) && categorieRepository.existsByName(name)) {
            throw new BadRequestException("Une catégorie avec ce nom existe déjà");
        }

        String oldName = categorie.getName();

        if (name != null) categorie.setName(name);
        if (description != null) categorie.setDescription(description);
        // imageUrl supprimé - maintenant géré par ProductImage
        if (active != null) categorie.setActive(active);
        if (sortOrder != null) categorie.setSortOrder(sortOrder);

        Categorie savedCategorie = categorieRepository.save(categorie);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "CATEGORIE_UPDATE",
            "Catégorie mise à jour: " + oldName + " -> " + savedCategorie.getName(),
            securityUtils.getCurrentIpAddress()
        );

        return CategorieDto.fromEntity(savedCategorie);
    }

    public void deleteCategorie(Long id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID: " + id));

        // Vérifier s'il y a des packs ou matériels associés
        long packsCount = categorieRepository.countPacksByCategorieId(id);
        long materielsCount = categorieRepository.countMaterielsByCategorieId(id);

        if (packsCount > 0 || materielsCount > 0) {
            throw new BadRequestException("Impossible de supprimer cette catégorie car elle contient des packs ou matériels");
        }

        String categorieName = categorie.getName();
        categorieRepository.delete(categorie);

        auditService.createLog(
            securityUtils.getCurrentUser().orElse(null),
            "CATEGORIE_DELETE",
            "Catégorie supprimée: " + categorieName,
            securityUtils.getCurrentIpAddress()
        );
    }

    @Transactional(readOnly = true)
    public long countPacksByCategorie(Long categorieId) {
        return categorieRepository.countPacksByCategorieId(categorieId) + 
               categorieRepository.countMaterielsByCategorieId(categorieId);
    }
}




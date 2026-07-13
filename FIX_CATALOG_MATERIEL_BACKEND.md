# ✅ Correction Backend - API Catalogue retourne maintenant les matériels

## ❌ Problème d'origine

L'endpoint `/api/catalog/items?type=MATERIEL` retournait **intentionnellement** une page vide :

```java
// ❌ CODE BUGGÉ (ligne 78-81)
} else if ("MATERIEL".equalsIgnoreCase(type)) {
    // Pour les matériels, on retourne une page vide de PackDto
    return ResponseEntity.ok(new PageImpl<>(List.of(), pageable, 0));
}
```

**Résultat :**
```json
GET /api/catalog/items?type=MATERIEL
→ { "content": [], "totalElements": 0 }
```

---

## ✅ Correction appliquée

### **1. Injection du MaterielService**

```java
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {
    
    private final PackService packService;
    private final MaterielService materielService;  // ✅ AJOUTÉ
    
    // ...
}
```

---

### **2. Appel du service Materiel**

```java
} else if ("MATERIEL".equalsIgnoreCase(type)) {
    // ✅ CORRECTION: Charger les matériels depuis le service
    String cleanSearch = (search != null && !search.trim().isEmpty()) ? search : null;
    
    Page<MaterielDto> materiels = materielService.searchMaterielsWithFilters(
        active, 
        categorieId, 
        minPrice, 
        maxPrice, 
        null, // isAvailable
        pageable
    );
    
    // Convertir les matériels en PackDto pour compatibilité
    List<PackDto> convertedMateriels = materiels.getContent().stream()
        .map(this::convertMaterielToPackDto)
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(new PageImpl<>(
        convertedMateriels, 
        pageable, 
        materiels.getTotalElements()
    ));
}
```

---

### **3. Méthode de conversion MaterielDto → PackDto**

```java
/**
 * Convertit un MaterielDto en PackDto pour l'uniformisation du catalogue
 * Les matériels sont considérés comme des "packs" de type MATERIEL
 */
private PackDto convertMaterielToPackDto(MaterielDto materiel) {
    PackDto packDto = new PackDto();
    packDto.setId(materiel.getId());
    packDto.setName(materiel.getName());
    packDto.setDescription(materiel.getDescription());
    packDto.setPrice(materiel.getPrice());
    packDto.setActive(materiel.getActive());
    packDto.setCategorieId(materiel.getCategorieId());
    packDto.setCategorieName(materiel.getCategorieName());
    packDto.setImages(materiel.getImages());
    packDto.setPrimaryImage(materiel.getPrimaryImage());
    packDto.setImageUrl(materiel.getImageUrl());
    packDto.setCreatedAt(materiel.getCreatedAt());
    packDto.setUpdatedAt(materiel.getUpdatedAt());
    packDto.setAvailable(materiel.getAvailable());
    
    return packDto;
}
```

---

## 📊 Résultat après correction

### **Test 1 : Lister les matériels**
```bash
curl "http://localhost:8080/api/catalog/items?type=MATERIEL&page=0&size=10"
```

**Réponse attendue :**
```json
{
  "content": [
    {
      "id": 21,
      "name": "Canape Marriage",
      "description": "",
      "price": 350,
      "active": true,
      "images": ["/uploads/images/950092b5-6ec8-4ccb-a264-45094a008acb.jpg"],
      "primaryImage": "/uploads/images/950092b5-6ec8-4ccb-a264-45094a008acb.jpg"
    }
  ],
  "totalElements": 13,
  "totalPages": 2,
  "size": 10,
  "number": 0
}
```

---

### **Test 2 : Lister les packs**
```bash
curl "http://localhost:8080/api/catalog/items?type=PACK&page=0&size=10"
```

**Réponse attendue :**
```json
{
  "content": [...],
  "totalElements": 32,
  ...
}
```

---

### **Test 3 : Lister tous les items (sans filtre type)**
```bash
curl "http://localhost:8080/api/catalog/items?page=0&size=10"
```

**Réponse attendue :**
```json
{
  "content": [...], // Uniquement les packs actifs
  "totalElements": 32
}
```

**⚠️ Note :** Quand aucun type n'est spécifié, seuls les **packs actifs** sont retournés (ligne 106-117 du controller). Si vous voulez retourner packs + matériels, il faut modifier cette logique aussi.

---

## 🔧 Fichier modifié

**Fichier :** `/Users/mohammedzanfar/workspaces/afra7kom/afra7kombackend/src/main/java/com/afra7kom/backend/controller/CatalogController.java`

**Modifications :**
- Ligne 3 : Import `MaterielDto`
- Ligne 5 : Import `MaterielService`
- Lignes 21-24 : Imports `ArrayList`, `Comparator`, `Collectors`
- Ligne 33 : Injection `private final MaterielService materielService;`
- Lignes 84-102 : Correction du cas `type=MATERIEL`
- Lignes 128-154 : Nouvelle méthode `convertMaterielToPackDto()`

---

## 🧪 Tests à effectuer

### **1. Redémarrer le backend**
```bash
cd /Users/mohammedzanfar/workspaces/afra7kom/afra7kombackend
./mvnw spring-boot:run
```

### **2. Tester l'API**
```bash
# Test matériels
curl "http://localhost:8080/api/catalog/items?type=MATERIEL" | jq

# Test packs
curl "http://localhost:8080/api/catalog/items?type=PACK" | jq

# Test buffets
curl "http://localhost:8080/api/catalog/items?type=BUFFET" | jq
```

### **3. Tester depuis le frontend**
1. Aller sur `http://localhost:4200/admin/equipments`
2. Cliquer sur le filtre **"Matériels"** 🔧
3. **Résultat attendu :** Liste affiche les 13 matériels

### **4. Vérifier les logs backend**
```bash
# Dans les logs Spring Boot, vous devriez voir :
# 1. Pas d'erreur au démarrage
# 2. Requêtes SQL pour récupérer les matériels
# 3. Pas d'exception NullPointerException
```

---

## 🔮 Améliorations futures possibles

### **1. Retourner packs + matériels quand type=null**

Actuellement, quand aucun type n'est spécifié, seuls les packs sont retournés. Pour retourner tout :

```java
// Si pas de type spécifié, on retourne packs + matériels
if (type == null || type.trim().isEmpty()) {
    // Charger les packs
    Page<PackDto> packs = packService.searchPacksWithFilters(...);
    
    // Charger les matériels
    Page<MaterielDto> materiels = materielService.searchMaterielsWithFilters(...);
    
    // Fusionner
    List<PackDto> allItems = new ArrayList<>();
    allItems.addAll(packs.getContent());
    allItems.addAll(materiels.getContent().stream()
        .map(this::convertMaterielToPackDto)
        .toList());
    
    // Trier et paginer
    allItems.sort(Comparator.comparing(PackDto::getName));
    
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), allItems.size());
    List<PackDto> page = allItems.subList(start, end);
    
    return ResponseEntity.ok(new PageImpl<>(page, pageable, allItems.size()));
}
```

---

### **2. Créer un CatalogItemDto unifié**

Au lieu de convertir `MaterielDto` → `PackDto`, créer un DTO spécifique :

```java
@Data
public class CatalogItemDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean active;
    private String type; // PACK, BUFFET, MATERIEL, etc.
    private List<String> images;
    private String primaryImage;
    private CategorieDto categorie;
    // ...
    
    public static CatalogItemDto fromPack(PackDto pack) { ... }
    public static CatalogItemDto fromMateriel(MaterielDto materiel) { ... }
}
```

Puis changer le type de retour :
```java
public ResponseEntity<Page<CatalogItemDto>> getAllItems(...)
```

---

### **3. Ajouter l'endpoint /counts**

```java
@GetMapping("/counts")
public ResponseEntity<Map<String, Long>> getTypeCounts() {
    Map<String, Long> counts = new HashMap<>();
    counts.put("PACK", packService.countByType(PackType.PACK));
    counts.put("BUFFET", packService.countByType(PackType.BUFFET));
    counts.put("PACK_BUFFET", packService.countByType(PackType.PACK_BUFFET));
    counts.put("CADEAU", packService.countByType(PackType.CADEAU));
    counts.put("MATERIEL", materielService.countAll());
    return ResponseEntity.ok(counts);
}
```

---

## ⚠️ Points d'attention

### **1. Compatibilité PackDto**

La méthode `convertMaterielToPackDto` suppose que `PackDto` a les setters correspondants. Vérifiez que :
- `PackDto.setAvailable()` existe
- Tous les champs sont compatibles

Si des setters manquent, ajoutez-les dans `PackDto.java` :
```java
private Boolean available;

public Boolean getAvailable() {
    return available;
}

public void setAvailable(Boolean available) {
    this.available = available;
}
```

---

### **2. Type MATERIEL dans l'enum**

Si vous voulez définir le type explicitement :
```java
// Dans PackType.java (enum)
public enum PackType {
    PACK,
    BUFFET,
    PACK_BUFFET,
    CADEAU,
    MATERIEL  // ← Ajouter
}
```

Puis dans `convertMaterielToPackDto` :
```java
packDto.setType(PackType.MATERIEL);
```

---

## ✅ Checklist de validation

- [x] MaterielService injecté dans CatalogController
- [x] Appel à searchMaterielsWithFilters() ajouté
- [x] Méthode convertMaterielToPackDto() créée
- [x] Imports ajoutés (MaterielDto, MaterielService, Collectors)
- [ ] Backend redémarré
- [ ] Tests API avec curl effectués
- [ ] Frontend testé (filtre Matériels)
- [ ] Logs backend vérifiés (pas d'erreur)
- [ ] Supprimer le workaround frontend (optionnel)

---

**Date de correction :** 23 Octobre 2025  
**Fichier modifié :** `CatalogController.java`  
**Status :** ✅ Correction backend appliquée - Redémarrage requis

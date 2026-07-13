# Endpoints Publics - Afra7kom Backend

## Vue d'ensemble

Ces endpoints sont accessibles sans authentification (sans token JWT) et sont destinés au site web public d'Afra7kom.

## Configuration de sécurité

Les endpoints publics sont configurés dans `SecurityConfig.java` :

```java
// Endpoints publics pour le site web
.requestMatchers("/api/packs/**").permitAll()
.requestMatchers("/api/materiels/**").permitAll()
.requestMatchers("/api/categories/**").permitAll()
.requestMatchers("/api/public/**").permitAll()
.requestMatchers("/api/reservations/public/**").permitAll()
.requestMatchers("/api/availability/**").permitAll()
```

## Endpoints disponibles

### 📦 Packs

#### GET `/api/packs`
Récupérer la liste paginée des packs
- **Paramètres de requête :**
  - `page` (int, défaut: 0) : Numéro de page
  - `size` (int, défaut: 20) : Taille de la page
  - `categorieId` (Long, optionnel) : Filtrer par catégorie
  - `search` (String, optionnel) : Recherche par nom
  - `minPrice` (BigDecimal, optionnel) : Prix minimum
  - `maxPrice` (BigDecimal, optionnel) : Prix maximum
  - `active` (Boolean, optionnel) : Filtrer actif/inactif

#### GET `/api/packs/{id}`
Récupérer les détails d'un pack
- **Paramètres de chemin :**
  - `id` (Long) : ID du pack

#### GET `/api/packs/category/{categorieId}`
Récupérer les packs d'une catégorie
- **Paramètres de chemin :**
  - `categorieId` (Long) : ID de la catégorie

#### GET `/api/packs/most-favorited`
Récupérer les packs les plus favoris

#### GET `/api/packs/most-rented`
Récupérer les packs les plus loués

### 🔧 Matériels

#### GET `/api/materiels`
Récupérer la liste paginée des matériels
- **Paramètres de requête :**
  - `page` (int, défaut: 0) : Numéro de page
  - `size` (int, défaut: 20) : Taille de la page
  - `categorieId` (Long, optionnel) : Filtrer par catégorie
  - `search` (String, optionnel) : Recherche par nom
  - `minPrice` (BigDecimal, optionnel) : Prix minimum
  - `maxPrice` (BigDecimal, optionnel) : Prix maximum
  - `active` (Boolean, optionnel) : Filtrer actif/inactif
  - `isAvailable` (Boolean, optionnel) : Filtrer par disponibilité

#### GET `/api/materiels/{id}`
Récupérer les détails d'un matériel
- **Paramètres de chemin :**
  - `id` (Long) : ID du matériel

#### GET `/api/materiels/category/{categorieId}`
Récupérer les matériels d'une catégorie
- **Paramètres de chemin :**
  - `categorieId` (Long) : ID de la catégorie

#### GET `/api/materiels/disponibles`
Récupérer les matériels disponibles

#### GET `/api/materiels/most-favorited`
Récupérer les matériels les plus favoris

#### GET `/api/materiels/most-rented`
Récupérer les matériels les plus loués

### 📂 Catégories

#### GET `/api/categories`
Récupérer la liste des catégories

#### GET `/api/categories/paginated`
Récupérer la liste paginée des catégories
- **Paramètres de requête :**
  - `page` (int, défaut: 0) : Numéro de page
  - `size` (int, défaut: 20) : Taille de la page

#### GET `/api/categories/{id}`
Récupérer les détails d'une catégorie
- **Paramètres de chemin :**
  - `id` (Long) : ID de la catégorie

#### GET `/api/categories/active`
Récupérer les catégories actives

### 📅 Vérification de disponibilité

#### GET `/api/public/materiels/{id}/availability`
Vérifier la disponibilité d'un matériel
- **Paramètres de chemin :**
  - `id` (Long) : ID du matériel
- **Paramètres de requête :**
  - `startDate` (LocalDate, format: YYYY-MM-DD) : Date de début
  - `endDate` (LocalDate, format: YYYY-MM-DD) : Date de fin

#### GET `/api/public/packs/{id}/availability`
Vérifier la disponibilité d'un pack
- **Paramètres de chemin :**
  - `id` (Long) : ID du pack
- **Paramètres de requête :**
  - `startDate` (LocalDate, format: YYYY-MM-DD) : Date de début
  - `endDate` (LocalDate, format: YYYY-MM-DD) : Date de fin

#### GET `/api/public/availability/check`
Vérifier la disponibilité générale
- **Paramètres de requête :**
  - `startDate` (LocalDate, format: YYYY-MM-DD) : Date de début
  - `endDate` (LocalDate, format: YYYY-MM-DD) : Date de fin

## Exemples d'utilisation

### Récupérer tous les packs
```bash
curl -X GET "http://localhost:8080/api/packs?page=0&size=20"
```

### Récupérer les packs d'une catégorie
```bash
curl -X GET "http://localhost:8080/api/packs/category/1?page=0&size=10"
```

### Vérifier la disponibilité d'un matériel
```bash
curl -X GET "http://localhost:8080/api/public/materiels/1/availability?startDate=2024-01-15&endDate=2024-01-20"
```

### Récupérer les matériels disponibles
```bash
curl -X GET "http://localhost:8080/api/materiels/disponibles?page=0&size=20"
```

### Récupérer les catégories actives
```bash
curl -X GET "http://localhost:8080/api/categories/active"
```

## Réponses

### Format de réponse paginée
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```

### Format de réponse pour disponibilité
```json
{
  "available": true,
  "message": "Matériel disponible pour cette période",
  "materielId": 1,
  "startDate": "2024-01-15",
  "endDate": "2024-01-20"
}
```

## Intégration Frontend

Le frontend utilise le `PublicService` pour accéder à ces endpoints :

```typescript
// Récupérer les packs
this.publicService.getPacks().subscribe(packs => {
  console.log('Packs:', packs.content);
});

// Vérifier la disponibilité
this.publicService.checkMaterielAvailability(1, '2024-01-15', '2024-01-20')
  .subscribe(result => {
    console.log('Disponibilité:', result.available);
  });
```

## Sécurité

- ✅ **Aucune authentification requise** pour ces endpoints
- ✅ **Lecture seule** - Aucune modification possible
- ✅ **CORS configuré** pour le frontend
- ✅ **Validation des paramètres** côté serveur
- ✅ **Gestion d'erreurs** appropriée

## Notes importantes

1. **Performance** : Les endpoints sont optimisés pour les requêtes publiques
2. **Cache** : Considérer l'ajout de cache pour améliorer les performances
3. **Rate Limiting** : Implémenter si nécessaire pour éviter l'abus
4. **Monitoring** : Surveiller l'utilisation de ces endpoints


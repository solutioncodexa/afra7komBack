package com.afra7kom.backend.controller;

import com.afra7kom.backend.dto.ContactDto;
import com.afra7kom.backend.dto.ContactReplyDto;
import com.afra7kom.backend.entity.Contact;
import com.afra7kom.backend.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ContactController {

    private final ContactService contactService;

    /**
     * Créer un nouveau contact (PUBLIC - sans authentification)
     * POST /api/contacts
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createContact(@Valid @RequestBody ContactDto contactDto) {
        try {
            ContactDto createdContact = contactService.createContact(contactDto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Votre message a été envoyé avec succès. Nous vous contacterons bientôt!");
            response.put("data", createdContact);
            
            log.info("Nouveau contact créé via API publique: {}", createdContact.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Erreur lors de la création du contact", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors de l'envoi du message. Veuillez réessayer.");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Récupérer tous les contacts avec pagination (ADMIN/AGENT uniquement)
     * GET /api/contacts
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> getAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        try {
            Page<ContactDto> contacts = contactService.getAllContacts(page, size, sortBy, sortDirection);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", contacts.getContent());
            response.put("currentPage", contacts.getNumber());
            response.put("totalItems", contacts.getTotalElements());
            response.put("totalPages", contacts.getTotalPages());
            response.put("size", contacts.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contacts", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors de la récupération des contacts");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Récupérer un contact par ID (ADMIN/AGENT uniquement)
     * GET /api/contacts/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> getContactById(@PathVariable Long id) {
        try {
            ContactDto contact = contactService.getContactById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", contact);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du contact {}", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Contact non trouvé");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Rechercher des contacts (ADMIN/AGENT uniquement)
     * GET /api/contacts/search?keyword=...
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> searchContacts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<ContactDto> contacts = contactService.searchContacts(keyword, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", contacts.getContent());
            response.put("currentPage", contacts.getNumber());
            response.put("totalItems", contacts.getTotalElements());
            response.put("totalPages", contacts.getTotalPages());
            response.put("size", contacts.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de contacts", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors de la recherche");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Récupérer les contacts par statut (ADMIN/AGENT uniquement)
     * GET /api/contacts/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> getContactsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Contact.ContactStatus contactStatus = Contact.ContactStatus.valueOf(status.toUpperCase());
            Page<ContactDto> contacts = contactService.getContactsByStatus(contactStatus, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", contacts.getContent());
            response.put("currentPage", contacts.getNumber());
            response.put("totalItems", contacts.getTotalElements());
            response.put("totalPages", contacts.getTotalPages());
            response.put("size", contacts.getSize());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Statut invalide");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contacts par statut", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors de la récupération");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Récupérer les contacts non lus (ADMIN/AGENT uniquement)
     * GET /api/contacts/unread
     */
    @GetMapping("/unread/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> getUnreadContacts() {
        try {
            List<ContactDto> contacts = contactService.getUnreadContacts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", contacts);
            response.put("count", contacts.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contacts non lus", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors de la récupération");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Compter les contacts non lus (ADMIN/AGENT uniquement)
     * GET /api/contacts/unread/count
     */
    @GetMapping("/unread/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> countUnreadContacts() {
        try {
            long count = contactService.countUnreadContacts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors du comptage des contacts non lus", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors du comptage");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Marquer un contact comme lu (ADMIN/AGENT uniquement)
     * PATCH /api/contacts/{id}/read
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        try {
            ContactDto contact = contactService.markAsRead(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Contact marqué comme lu");
            response.put("data", contact);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors du marquage du contact {} comme lu", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors du marquage");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Changer le statut d'un contact (ADMIN/AGENT uniquement)
     * PATCH /api/contacts/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> changeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        try {
            String statusStr = request.get("status");
            if (statusStr == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Le statut est requis");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            Contact.ContactStatus status = Contact.ContactStatus.valueOf(statusStr.toUpperCase());
            ContactDto contact = contactService.changeStatus(id, status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Statut mis à jour");
            response.put("data", contact);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Statut invalide");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Erreur lors du changement de statut du contact {}", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors de la mise à jour");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Répondre à un contact (ADMIN/AGENT uniquement)
     * POST /api/contacts/{id}/reply
     */
    @PostMapping("/{id}/reply")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> replyToContact(
            @PathVariable Long id,
            @Valid @RequestBody ContactReplyDto replyDto
    ) {
        try {
            ContactDto contact = contactService.replyToContact(id, replyDto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Réponse envoyée avec succès");
            response.put("data", contact);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la réponse au contact {}", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors de l'envoi de la réponse");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Obtenir l'URL WhatsApp pour un contact (ADMIN/AGENT uniquement)
     * GET /api/contacts/{id}/whatsapp
     */
    @GetMapping("/{id}/whatsapp")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> getWhatsAppUrl(@PathVariable Long id) {
        try {
            String whatsappUrl = contactService.getWhatsAppUrl(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("whatsappUrl", whatsappUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'URL WhatsApp pour le contact {}", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors de la récupération de l'URL");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Supprimer un contact (ADMIN uniquement)
     * DELETE /api/contacts/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteContact(@PathVariable Long id) {
        try {
            contactService.deleteContact(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Contact supprimé avec succès");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du contact {}", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors de la suppression");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}


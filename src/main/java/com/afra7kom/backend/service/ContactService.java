package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.ContactDto;
import com.afra7kom.backend.dto.ContactReplyDto;
import com.afra7kom.backend.entity.Contact;
import com.afra7kom.backend.entity.Role;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.exception.ResourceNotFoundException;
import com.afra7kom.backend.repository.ContactRepository;
import com.afra7kom.backend.repository.UserRepository;
import com.afra7kom.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final SecurityUtils securityUtils;

    @Value("${spring.mail.username:noreply.codexa@gmail.com}")
    private String fromEmail;

    @Value("${app.email.admin:admin@afra7kom.com}")
    private String adminEmail;

    /**
     * Créer un nouveau contact (endpoint public)
     */
    public ContactDto createContact(ContactDto contactDto) {
        try {
            Contact contact = contactDto.toEntity();
            contact.setStatus(Contact.ContactStatus.NEW);
            contact.setIsRead(false);
            
            // Générer l'URL WhatsApp
            contact.setWhatsappUrl(contact.generateWhatsAppUrl());
            
            Contact savedContact = contactRepository.save(contact);
            log.info("Nouveau contact créé: ID={}, Email={}", savedContact.getId(), savedContact.getEmail());

            // Notifier les admins et agents
            try {
                notifyAdminsOfNewContact(savedContact);
            } catch (Exception e) {
                log.error("Erreur lors de la notification des admins pour le contact {}", savedContact.getId(), e);
                // Ne pas faire échouer la création du contact si l'email échoue
            }

            return new ContactDto(savedContact);
        } catch (Exception e) {
            log.error("Erreur lors de la création du contact", e);
            throw new RuntimeException("Impossible de créer le contact", e);
        }
    }

    /**
     * Récupérer tous les contacts avec pagination (admin/agent uniquement)
     */
    @Transactional(readOnly = true)
    public Page<ContactDto> getAllContacts(int page, int size, String sortBy, String sortDirection) {
        try {
            Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<Contact> contacts = contactRepository.findAll(pageable);
            return contacts.map(ContactDto::new);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contacts", e);
            throw new RuntimeException("Impossible de récupérer les contacts", e);
        }
    }

    /**
     * Récupérer un contact par ID (admin/agent uniquement)
     */
    @Transactional(readOnly = true)
    public ContactDto getContactById(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));
        
        // Marquer comme lu automatiquement
        if (!contact.getIsRead()) {
            contact.setIsRead(true);
            contactRepository.save(contact);
        }
        
        return new ContactDto(contact);
    }

    /**
     * Rechercher des contacts par mot-clé
     */
    @Transactional(readOnly = true)
    public Page<ContactDto> searchContacts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Contact> contacts = contactRepository.searchContacts(keyword, pageable);
        return contacts.map(ContactDto::new);
    }

    /**
     * Récupérer les contacts par statut
     */
    @Transactional(readOnly = true)
    public Page<ContactDto> getContactsByStatus(Contact.ContactStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Contact> contacts = contactRepository.findByStatus(status, pageable);
        return contacts.map(ContactDto::new);
    }

    /**
     * Récupérer les contacts non lus
     */
    @Transactional(readOnly = true)
    public List<ContactDto> getUnreadContacts() {
        List<Contact> contacts = contactRepository.findUnreadContacts();
        return contacts.stream().map(ContactDto::new).collect(Collectors.toList());
    }

    /**
     * Compter les contacts non lus
     */
    @Transactional(readOnly = true)
    public long countUnreadContacts() {
        return contactRepository.countUnreadContacts();
    }

    /**
     * Marquer un contact comme lu
     */
    public ContactDto markAsRead(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));
        
        contact.setIsRead(true);
        Contact updatedContact = contactRepository.save(contact);
        
        log.info("Contact {} marqué comme lu", id);
        return new ContactDto(updatedContact);
    }

    /**
     * Changer le statut d'un contact
     */
    public ContactDto changeStatus(Long id, Contact.ContactStatus newStatus) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));
        
        contact.setStatus(newStatus);
        Contact updatedContact = contactRepository.save(contact);
        
        log.info("Statut du contact {} changé en {}", id, newStatus);
        return new ContactDto(updatedContact);
    }

    /**
     * Répondre à un contact par email
     */
    public ContactDto replyToContact(Long id, ContactReplyDto replyDto) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));

        // Enregistrer la réponse
        contact.setReplyMessage(replyDto.getReplyMessage());
        contact.setRepliedAt(LocalDateTime.now());
        
        // Récupérer l'utilisateur connecté
        User currentUser = securityUtils.getCurrentUser().orElse(null);
        if (currentUser != null) {
            contact.setRepliedBy(currentUser.getEmail());
        }
        
        contact.setStatus(Contact.ContactStatus.REPLIED);
        contact.setIsRead(true);
        
        Contact updatedContact = contactRepository.save(contact);

        // Envoyer l'email de réponse
        if (replyDto.getSendEmail()) {
            try {
                sendReplyEmail(contact, replyDto.getReplyMessage());
                log.info("Email de réponse envoyé au contact {} ({})", id, contact.getEmail());
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de l'email de réponse au contact {}", id, e);
                // Ne pas faire échouer la mise à jour si l'email échoue
            }
        }

        log.info("Réponse enregistrée pour le contact {} par {}", id, contact.getRepliedBy());
        return new ContactDto(updatedContact);
    }

    /**
     * Supprimer un contact
     */
    public void deleteContact(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));
        
        contactRepository.delete(contact);
        log.info("Contact {} supprimé", id);
    }

    /**
     * Obtenir l'URL WhatsApp pour un contact
     */
    @Transactional(readOnly = true)
    public String getWhatsAppUrl(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));
        
        if (contact.getWhatsappUrl() != null) {
            return contact.getWhatsappUrl();
        }
        
        return contact.generateWhatsAppUrl();
    }

    /**
     * Notifier les admins et agents d'un nouveau contact
     */
    private void notifyAdminsOfNewContact(Contact contact) {
        try {
            String subject = "📧 Nouveau message de contact - " + contact.getFullName();
            String htmlContent = buildNewContactEmailContent(contact);
            
            // Récupérer tous les utilisateurs admin et agent
            List<String> adminAndAgentEmails = getAdminAndAgentEmails();
            
            // Envoyer à tous les admins et agents
            for (String email : adminAndAgentEmails) {
                sendHtmlEmail(email, subject, htmlContent);
            }
            
            // Envoyer aussi à l'email admin de configuration (fallback)
            if (!adminAndAgentEmails.contains(adminEmail)) {
                sendHtmlEmail(adminEmail, subject, htmlContent);
            }
            
            log.info("Notifications email envoyées pour le contact {} à {} destinataires", 
                    contact.getId(), adminAndAgentEmails.size() + 1);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications pour le contact {}", contact.getId(), e);
        }
    }

    /**
     * Envoyer un email de réponse au contact
     */
    private void sendReplyEmail(Contact contact, String replyMessage) throws MessagingException {
        String subject = "✉️ Réponse à votre message - Afra7kom";
        String htmlContent = buildReplyEmailContent(contact, replyMessage);
        sendHtmlEmail(contact.getEmail(), subject, htmlContent);
    }

    /**
     * Envoyer un email HTML
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
    }

    /**
     * Construire le contenu HTML pour la notification de nouveau contact
     */
    private String buildNewContactEmailContent(Contact contact) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Nouveau Message de Contact</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background: #f4f4f4; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 25px; border-radius: 8px; text-align: center; margin-bottom: 30px; }");
        html.append(".content { background: white; padding: 20px; }");
        html.append(".info-row { margin: 15px 0; padding: 15px; background: #f8f9fa; border-left: 4px solid #667eea; border-radius: 5px; }");
        html.append(".label { font-weight: bold; color: #667eea; display: inline-block; min-width: 120px; }");
        html.append(".value { color: #333; }");
        html.append(".message-box { background: #e8f4fd; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #3498db; }");
        html.append(".whatsapp-btn { background: #25D366; color: white; padding: 15px 30px; text-decoration: none; border-radius: 50px; font-weight: bold; display: inline-block; margin: 20px 0; }");
        html.append(".whatsapp-btn:hover { background: #128C7E; }");
        html.append(".footer { text-align: center; color: #999; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        
        html.append("<div class='header'>");
        html.append("<h1 style='margin: 0;'>📧 Nouveau Message de Contact</h1>");
        html.append("<p style='margin: 10px 0 0 0;'>Un client a envoyé un message sur le site web</p>");
        html.append("</div>");
        
        html.append("<div class='content'>");
        html.append("<h2 style='color: #667eea; margin-bottom: 20px;'>👤 Informations du Client</h2>");
        
        html.append("<div class='info-row'>");
        html.append("<span class='label'>Nom complet:</span>");
        html.append("<span class='value'>").append(contact.getFullName()).append("</span>");
        html.append("</div>");
        
        html.append("<div class='info-row'>");
        html.append("<span class='label'>Email:</span>");
        html.append("<span class='value'>").append(contact.getEmail()).append("</span>");
        html.append("</div>");
        
        html.append("<div class='info-row'>");
        html.append("<span class='label'>Téléphone:</span>");
        html.append("<span class='value'>").append(contact.getPhone()).append("</span>");
        html.append("</div>");
        
        if (contact.getSubject() != null && !contact.getSubject().isEmpty()) {
            html.append("<div class='info-row'>");
            html.append("<span class='label'>Sujet:</span>");
            html.append("<span class='value'>").append(contact.getSubject()).append("</span>");
            html.append("</div>");
        }
        
        html.append("<h2 style='color: #667eea; margin: 30px 0 15px 0;'>💬 Message</h2>");
        html.append("<div class='message-box'>");
        html.append("<p style='margin: 0; white-space: pre-wrap;'>").append(contact.getMessage()).append("</p>");
        html.append("</div>");
        
        // Bouton WhatsApp
        if (contact.getWhatsappUrl() != null && !contact.getWhatsappUrl().isEmpty()) {
            html.append("<div style='text-align: center; margin: 30px 0;'>");
            html.append("<a href='").append(contact.getWhatsappUrl()).append("' class='whatsapp-btn'>");
            html.append("💬 Contacter sur WhatsApp</a>");
            html.append("</div>");
        }
        
        html.append("<div style='background: #fff3cd; padding: 15px; border-radius: 5px; border-left: 4px solid #ffc107; margin-top: 20px;'>");
        html.append("<strong>⚠️ Action requise:</strong> Veuillez répondre à ce message dans les plus brefs délais.");
        html.append("</div>");
        
        html.append("</div>");
        
        html.append("<div class='footer'>");
        html.append("<p>Email automatique généré par le système Afra7kom</p>");
        html.append("<p>Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * Construire le contenu HTML pour la réponse au contact
     */
    private String buildReplyEmailContent(Contact contact, String replyMessage) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Réponse à votre message</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background: #f4f4f4; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 25px; border-radius: 8px; text-align: center; margin-bottom: 30px; }");
        html.append(".content { padding: 20px; }");
        html.append(".message-box { background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #667eea; }");
        html.append(".original-message { background: #e8f4fd; padding: 15px; border-radius: 5px; margin: 20px 0; font-size: 0.9em; }");
        html.append(".footer { text-align: center; color: #999; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        
        html.append("<div class='header'>");
        html.append("<h1 style='margin: 0;'>✉️ Afra7kom</h1>");
        html.append("<p style='margin: 10px 0 0 0;'>Réponse à votre message</p>");
        html.append("</div>");
        
        html.append("<div class='content'>");
        html.append("<p>Bonjour <strong>").append(contact.getFullName()).append("</strong>,</p>");
        html.append("<p>Merci de nous avoir contactés. Voici notre réponse à votre message :</p>");
        
        html.append("<div class='message-box'>");
        html.append("<p style='margin: 0; white-space: pre-wrap;'>").append(replyMessage).append("</p>");
        html.append("</div>");
        
        html.append("<h3 style='color: #667eea; margin-top: 30px;'>Votre message initial :</h3>");
        html.append("<div class='original-message'>");
        if (contact.getSubject() != null && !contact.getSubject().isEmpty()) {
            html.append("<p><strong>Sujet:</strong> ").append(contact.getSubject()).append("</p>");
        }
        html.append("<p style='white-space: pre-wrap;'>").append(contact.getMessage()).append("</p>");
        html.append("</div>");
        
        html.append("<p style='margin-top: 30px;'>Si vous avez d'autres questions, n'hésitez pas à nous contacter.</p>");
        html.append("<p>Cordialement,<br><strong>L'équipe Afra7kom</strong></p>");
        
        html.append("</div>");
        
        html.append("<div class='footer'>");
        html.append("<p>© ").append(LocalDateTime.now().getYear()).append(" Afra7kom - Tous droits réservés</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * Récupérer tous les emails des utilisateurs admin et agent
     */
    private List<String> getAdminAndAgentEmails() {
        List<String> emails = new ArrayList<>();
        
        try {
            // Récupérer tous les admins
            List<User> adminUsers = userRepository.findByRoleName(Role.RoleName.ADMIN, PageRequest.of(0, 100))
                    .getContent();
            for (User admin : adminUsers) {
                if (admin.getEmail() != null && !admin.getEmail().isEmpty()) {
                    emails.add(admin.getEmail());
                }
            }
            
            // Récupérer tous les agents
            List<User> agentUsers = userRepository.findByRoleName(Role.RoleName.AGENT, PageRequest.of(0, 100))
                    .getContent();
            for (User agent : agentUsers) {
                if (agent.getEmail() != null && !agent.getEmail().isEmpty()) {
                    emails.add(agent.getEmail());
                }
            }
            
            log.info("Récupération de {} emails d'admins et agents pour notifications de contact", emails.size());
            
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des emails d'admins et agents", e);
        }
        
        return emails;
    }
}






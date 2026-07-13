package com.afra7kom.backend.service;

import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.entity.Pack;
import com.afra7kom.backend.entity.Materiel;
import com.afra7kom.backend.entity.Role;
import com.afra7kom.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    
    @Value("${spring.mail.username:noreply.codexa@gmail.com}")
    private String fromEmail;
    
    @Value("${app.email.admin:admin@afra7kom.com}")
    private String adminEmail;
    
    @Value("${app.email.agent:agent@afra7kom.com}")
    private String agentEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    /**
     * Envoyer une notification de nouvelle réservation aux admins et agents
     * Méthode asynchrone pour ne pas bloquer la requête HTTP
     */
    @Async("taskExecutor")
    public void sendNewReservationNotification(Reservation reservation) {
        try {
            String subject = "🆕 Nouvelle réservation - " + reservation.getFirstName() + " " + reservation.getLastName();
            String htmlContent = buildNewReservationEmailContent(reservation);
            
            // Récupérer tous les utilisateurs admin et agent
            List<String> adminAndAgentEmails = getAdminAndAgentEmails();
            
            // Envoyer à tous les admins et agents
            for (String email : adminAndAgentEmails) {
                sendHtmlEmail(email, subject, htmlContent);
            }
            
            // Envoyer aussi aux emails de configuration par défaut (fallback)
            if (!adminAndAgentEmails.contains(adminEmail)) {
                sendHtmlEmail(adminEmail, subject, htmlContent);
            }
            if (!adminAndAgentEmails.contains(agentEmail)) {
                sendHtmlEmail(agentEmail, subject, htmlContent);
            }
            
            log.info("Notifications email envoyées pour la réservation {} à {} destinataires", 
                    reservation.getId(), adminAndAgentEmails.size() + 2);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications email pour la réservation {}", 
                     reservation.getId(), e);
        }
    }
    
    /**
     * Envoyer une notification d'approbation au client
     * Méthode asynchrone pour ne pas bloquer la requête HTTP
     */
    @Async("taskExecutor")
    public void sendApprovalNotification(Reservation reservation) {
        try {
            String subject = "✅ Réservation approuvée - Afra7kom";
            String htmlContent = buildApprovalEmailContent(reservation);
            
            sendHtmlEmail(reservation.getEmail(), subject, htmlContent);
            
            log.info("Notification d'approbation envoyée au client {}", reservation.getEmail());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification d'approbation au client {}", 
                     reservation.getEmail(), e);
        }
    }
    
    /**
     * Envoyer une notification de rejet au client
     * Méthode asynchrone pour ne pas bloquer la requête HTTP
     */
    @Async("taskExecutor")
    public void sendRejectionNotification(Reservation reservation, String reason) {
        try {
            String subject = "❌ Réservation rejetée - Afra7kom";
            String htmlContent = buildRejectionEmailContent(reservation, reason);
            
            sendHtmlEmail(reservation.getEmail(), subject, htmlContent);
            
            log.info("Notification de rejet envoyée au client {}", reservation.getEmail());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification de rejet au client {}", 
                     reservation.getEmail(), e);
        }
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
     * Construire le contenu HTML pour la notification de nouvelle réservation
     */
    private String buildNewReservationEmailContent(Reservation reservation) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Nouvelle Réservation</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 20px; border-radius: 10px; }");
        html.append(".header { background: #2c3e50; color: white; padding: 20px; border-radius: 5px; text-align: center; }");
        html.append(".content { background: white; padding: 20px; margin: 20px 0; border-radius: 5px; }");
        html.append(".info-row { display: flex; justify-content: space-between; margin: 10px 0; padding: 10px; background: #f8f9fa; border-radius: 3px; }");
        html.append(".label { font-weight: bold; color: #2c3e50; }");
        html.append(".value { color: #666; }");
        html.append(".highlight { background: #e8f4fd; border-left: 4px solid #3498db; padding: 15px; margin: 15px 0; }");
        html.append(".footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>🆕 Nouvelle Réservation</h1>");
        html.append("<p>Une nouvelle demande de réservation a été soumise</p>");
        html.append("</div>");
        
        html.append("<div class='content'>");
        html.append("<h2>📋 Informations du Client</h2>");
        html.append("<div class='info-row'>");
        html.append("<span class='label'>Nom complet:</span>");
        html.append("<span class='value'>").append(reservation.getFirstName()).append(" ").append(reservation.getLastName()).append("</span>");
        html.append("</div>");
        html.append("<div class='info-row'>");
        html.append("<span class='label'>Téléphone:</span>");
        html.append("<span class='value'>").append(reservation.getPhone()).append("</span>");
        html.append("</div>");
        html.append("<div class='info-row'>");
        html.append("<span class='label'>Email:</span>");
        html.append("<span class='value'>").append(reservation.getEmail()).append("</span>");
        html.append("</div>");
        
        html.append("<h2>📅 Détails de la Réservation</h2>");
        html.append("<div class='info-row'>");
        html.append("<span class='label'>Date de début:</span>");
        html.append("<span class='value'>").append(reservation.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</span>");
        html.append("</div>");
        html.append("<div class='info-row'>");
        html.append("<span class='label'>Date de fin:</span>");
        html.append("<span class='value'>").append(reservation.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</span>");
        html.append("</div>");
        html.append("<div class='info-row'>");
        html.append("<span class='label'>Quantité:</span>");
        html.append("<span class='value'>").append(reservation.getQuantity()).append("</span>");
        html.append("</div>");
        
        // Informations sur le pack ou matériel
        if (reservation.getPack() != null) {
            Pack pack = reservation.getPack();
            html.append("<h2>📦 Pack Demandé</h2>");
            html.append("<div class='info-row'>");
            html.append("<span class='label'>Nom du pack:</span>");
            html.append("<span class='value'>").append(pack.getName()).append("</span>");
            html.append("</div>");
            html.append("<div class='info-row'>");
            html.append("<span class='label'>Description:</span>");
            html.append("<span class='value'>").append(pack.getDescription()).append("</span>");
            html.append("</div>");
            html.append("<div class='info-row'>");
            html.append("<span class='label'>Prix par jour:</span>");
            html.append("<span class='value'>").append(pack.getPrice()).append(" MAD</span>");
            html.append("</div>");
        } else if (reservation.getMateriel() != null) {
            Materiel materiel = reservation.getMateriel();
            html.append("<h2>🛠️ Matériel Demandé</h2>");
            html.append("<div class='info-row'>");
            html.append("<span class='label'>Nom du matériel:</span>");
            html.append("<span class='value'>").append(materiel.getName()).append("</span>");
            html.append("</div>");
            html.append("<div class='info-row'>");
            html.append("<span class='label'>Description:</span>");
            html.append("<span class='value'>").append(materiel.getDescription()).append("</span>");
            html.append("</div>");
            html.append("<div class='info-row'>");
            html.append("<span class='label'>Prix par jour:</span>");
            html.append("<span class='value'>").append(materiel.getPrice()).append(" MAD</span>");
            html.append("</div>");
        }
        
        
        if (reservation.getNotes() != null && !reservation.getNotes().isEmpty()) {
            html.append("<h2>📝 Notes</h2>");
            html.append("<div class='highlight'>").append(reservation.getNotes()).append("</div>");
        }
        
        if (reservation.getDeliveryAddress() != null && !reservation.getDeliveryAddress().isEmpty()) {
            html.append("<h2>📍 Adresse de Livraison</h2>");
            html.append("<div class='highlight'>").append(reservation.getDeliveryAddress()).append("</div>");
        }
        
        // Lien WhatsApp pour contacter le client
        if (reservation.getPhone() != null && !reservation.getPhone().isEmpty()) {
            String whatsappPhone = formatPhoneForWhatsApp(reservation.getPhone());
            String whatsappMessage = "Bonjour " + reservation.getFirstName() + ", nous avons reçu votre demande de réservation #" + reservation.getId() + ". Nous vous contacterons bientôt pour confirmer les détails.";
            String whatsappUrl = "https://wa.me/" + whatsappPhone.replace("+", "") + "?text=" + 
                    java.net.URLEncoder.encode(whatsappMessage, java.nio.charset.StandardCharsets.UTF_8);
            
            html.append("<h2>📱 Contact WhatsApp</h2>");
            html.append("<div class='highlight' style='background: #25D366; color: white; border-left: 4px solid #128C7E;'>");
            html.append("<p><strong>💬 Contacter le client directement sur WhatsApp:</strong></p>");
            html.append("<p><strong>Numéro:</strong> ").append(reservation.getPhone()).append("</p>");
            html.append("<p><strong>Format WhatsApp:</strong> ").append(whatsappPhone).append("</p>");
            html.append("<p style='margin-top: 15px;'>");
            html.append("<a href='").append(whatsappUrl).append("' ");
            html.append("style='background: #25D366; color: white; padding: 12px 24px; text-decoration: none; border-radius: 25px; font-weight: bold; display: inline-block;'>");
            html.append("💬 Ouvrir WhatsApp</a>");
            html.append("</p>");
            html.append("</div>");
        }
        
        html.append("<div class='highlight'>");
        html.append("<strong>⚠️ Action requise:</strong> Veuillez valider ou rejeter cette réservation dans le système.");
        html.append("</div>");
        
        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>Email automatique généré par le système Afra7kom</p>");
        html.append("<p>Date d'envoi: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * Construire le contenu HTML pour la notification d'approbation
     */
    private String buildApprovalEmailContent(Reservation reservation) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Réservation Approuvée</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 20px; border-radius: 10px; }");
        html.append(".header { background: #27ae60; color: white; padding: 20px; border-radius: 5px; text-align: center; }");
        html.append(".content { background: white; padding: 20px; margin: 20px 0; border-radius: 5px; }");
        html.append(".info-row { display: flex; justify-content: space-between; margin: 10px 0; padding: 10px; background: #f8f9fa; border-radius: 3px; }");
        html.append(".label { font-weight: bold; color: #2c3e50; }");
        html.append(".value { color: #666; }");
        html.append(".success { background: #d4edda; border-left: 4px solid #27ae60; padding: 15px; margin: 15px 0; }");
        html.append(".footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>✅ Réservation Approuvée</h1>");
        html.append("<p>Votre demande de réservation a été acceptée</p>");
        html.append("</div>");
        
        html.append("<div class='content'>");
        html.append("<p>Bonjour <strong>").append(reservation.getFirstName()).append(" ").append(reservation.getLastName()).append("</strong>,</p>");
        html.append("<p>Nous avons le plaisir de vous informer que votre réservation a été <strong>approuvée</strong> !</p>");
        
        html.append("<div class='success'>");
        html.append("<h3>📋 Détails de votre réservation</h3>");
        
        if (reservation.getPack() != null) {
            html.append("<p><strong>Pack réservé:</strong> ").append(reservation.getPack().getName()).append("</p>");
        } else if (reservation.getMateriel() != null) {
            html.append("<p><strong>Matériel réservé:</strong> ").append(reservation.getMateriel().getName()).append("</p>");
        }
        
        html.append("<p><strong>Période:</strong> ").append(reservation.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
           .append(" - ").append(reservation.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>");
        html.append("<p><strong>Quantité:</strong> ").append(reservation.getQuantity()).append("</p>");
        html.append("</div>");
        
        html.append("<h3>📞 Prochaines étapes</h3>");
        html.append("<p>Pour finaliser votre réservation, veuillez :</p>");
        html.append("<ul>");
        html.append("<li>Confirmer la livraison et le paiement du solde</li>");
        html.append("<li>Nous contacter pour organiser la remise du matériel</li>");
        html.append("<li>Préparer les documents nécessaires</li>");
        html.append("</ul>");
        
        html.append("<p>Merci de votre confiance et nous vous souhaitons une excellente expérience avec Afra7kom !</p>");
        
        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>Email automatique généré par le système Afra7kom</p>");
        html.append("<p>Date d'envoi: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * Construire le contenu HTML pour la notification de rejet
     */
    private String buildRejectionEmailContent(Reservation reservation, String reason) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Réservation Rejetée</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 20px; border-radius: 10px; }");
        html.append(".header { background: #e74c3c; color: white; padding: 20px; border-radius: 5px; text-align: center; }");
        html.append(".content { background: white; padding: 20px; margin: 20px 0; border-radius: 5px; }");
        html.append(".info-row { display: flex; justify-content: space-between; margin: 10px 0; padding: 10px; background: #f8f9fa; border-radius: 3px; }");
        html.append(".label { font-weight: bold; color: #2c3e50; }");
        html.append(".value { color: #666; }");
        html.append(".warning { background: #f8d7da; border-left: 4px solid #e74c3c; padding: 15px; margin: 15px 0; }");
        html.append(".footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>❌ Réservation Rejetée</h1>");
        html.append("<p>Votre demande de réservation n'a pas pu être acceptée</p>");
        html.append("</div>");
        
        html.append("<div class='content'>");
        html.append("<p>Bonjour <strong>").append(reservation.getFirstName()).append(" ").append(reservation.getLastName()).append("</strong>,</p>");
        html.append("<p>Nous regrettons de vous informer que votre réservation a été <strong>rejetée</strong>.</p>");
        
        html.append("<div class='warning'>");
        html.append("<h3>📝 Motif du rejet</h3>");
        html.append("<p>").append(reason).append("</p>");
        html.append("</div>");
        
        html.append("<h3>🔄 Que faire maintenant ?</h3>");
        html.append("<p>N'hésitez pas à :</p>");
        html.append("<ul>");
        html.append("<li>Nous contacter pour plus d'informations</li>");
        html.append("<li>Faire une nouvelle réservation avec des dates différentes</li>");
        html.append("<li>Consulter notre catalogue pour d'autres options</li>");
        html.append("</ul>");
        
        html.append("<p>Nous nous excusons pour ce désagrément et espérons pouvoir vous servir prochainement.</p>");
        
        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>Email automatique généré par le système Afra7kom</p>");
        html.append("<p>Date d'envoi: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * Envoyer un email de réinitialisation de mot de passe
     */
    @Async("taskExecutor")
    public void sendPasswordResetEmail(User user, String token) {
        try {
            String resetUrl = frontendUrl.replaceAll("/$", "") + "/auth/reset-password?token=" + token;
            String subject = "Réinitialisation de votre mot de passe - Afra7kom";
            String htmlContent = buildPasswordResetEmailContent(user, resetUrl);
            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            log.info("Email de réinitialisation envoyé à {}", user.getEmail());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de réinitialisation à {}", user.getEmail(), e);
        }
    }

    private String buildPasswordResetEmailContent(User user, String resetUrl) {
        String displayName = user.getEmail();
        return String.format(
            "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
            "body{font-family:Arial,sans-serif;line-height:1.6;color:#333;margin:0;padding:20px}" +
            ".container{max-width:600px;margin:0 auto;background:#fafafa;padding:24px;border-radius:12px}" +
            ".header{background:#ff4081;color:white;padding:20px;border-radius:8px;text-align:center}" +
            ".content{background:white;padding:24px;margin:20px 0;border-radius:8px}" +
            ".btn{display:inline-block;background:#ff4081;color:white!important;padding:12px 24px;" +
            "border-radius:6px;text-decoration:none;font-weight:600;margin:16px 0}" +
            ".footer{text-align:center;color:#666;font-size:12px;margin-top:20px}" +
            "</style></head><body><div class='container'>" +
            "<div class='header'><h2>Réinitialisation du mot de passe</h2></div>" +
            "<div class='content'>" +
            "<p>Bonjour,</p>" +
            "<p>Vous avez demandé la réinitialisation du mot de passe pour le compte <strong>%s</strong>.</p>" +
            "<p>Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe. Ce lien expire dans 1 heure.</p>" +
            "<p style='text-align:center'><a class='btn' href='%s'>Réinitialiser mon mot de passe</a></p>" +
            "<p style='font-size:13px;color:#666'>Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>%s</p>" +
            "<p style='font-size:13px;color:#666'>Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>" +
            "</div><div class='footer'><p>© Afra7kom</p></div></div></body></html>",
            displayName, resetUrl, resetUrl
        );
    }

    /**
     * Envoyer un email de notification de changement de mot de passe
     * Méthode asynchrone pour ne pas bloquer la requête HTTP
     */
    @Async("taskExecutor")
    public void sendPasswordChangeNotification(User user) {
        try {
            String subject = "🔐 Confirmation de changement de mot de passe - Afra7kom";
            String htmlContent = buildPasswordChangeEmailContent(user);
            
            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            
            log.info("Notification de changement de mot de passe envoyée à {}", user.getEmail());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification de changement de mot de passe à {}", 
                     user.getEmail(), e);
        }
    }
    
    /**
     * Construire le contenu HTML pour la notification de changement de mot de passe
     * Version optimisée et légère
     */
    private String buildPasswordChangeEmailContent(User user) {
        String dateTime = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
        
        return String.format(
            "<!DOCTYPE html>" +
            "<html><head><meta charset='UTF-8'><style>" +
            "body{font-family:Arial,sans-serif;line-height:1.6;color:#333;margin:0;padding:20px}" +
            ".container{max-width:600px;margin:0 auto;background:#f9f9f9;padding:20px;border-radius:10px}" +
            ".header{background:#3498db;color:white;padding:20px;border-radius:5px;text-align:center}" +
            ".content{background:white;padding:20px;margin:20px 0;border-radius:5px}" +
            ".box{padding:15px;margin:15px 0;border-left:4px solid #3498db}" +
            ".info{background:#e8f4fd}" +
            ".warning{background:#fff3cd;border-color:#ffc107}" +
            ".footer{text-align:center;color:#666;font-size:12px;margin-top:20px}" +
            "</style></head><body>" +
            "<div class='container'>" +
            "<div class='header'><h2>🔐 Changement de Mot de Passe</h2></div>" +
            "<div class='content'>" +
            "<p>Bonjour <strong>%s</strong>,</p>" +
            "<p>Votre mot de passe a été modifié avec succès.</p>" +
            "<div class='box info'>" +
            "<p><strong>Date:</strong> %s<br><strong>Compte:</strong> %s</p>" +
            "</div>" +
            "<div class='box warning'>" +
            "<p><strong>⚠️ Vous n'êtes pas à l'origine de cette modification ?</strong><br>" +
            "Contactez-nous immédiatement: support@afra7kom.ma</p>" +
            "</div>" +
            "<p>Merci d'utiliser Afra7kom !</p>" +
            "</div>" +
            "<div class='footer'>Email automatique - Afra7kom</div>" +
            "</div></body></html>",
            user.getEmail(), dateTime, user.getEmail()
        );
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
            
            log.info("Récupération de {} emails d'admins et agents", emails.size());
            
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des emails d'admins et agents", e);
        }
        
        return emails;
    }
    
    /**
     * Formater le numéro de téléphone pour WhatsApp (format marocain)
     */
    private String formatPhoneForWhatsApp(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "";
        }
        
        // Nettoyer le numéro (supprimer espaces, tirets, etc.)
        String cleanPhone = phone.replaceAll("[^0-9+]", "");
        
        // Si le numéro commence par +212, le garder tel quel
        if (cleanPhone.startsWith("+212")) {
            return cleanPhone;
        }
        
        // Si le numéro commence par 212, ajouter le +
        if (cleanPhone.startsWith("212")) {
            return "+" + cleanPhone;
        }
        
        // Si le numéro commence par 0, le remplacer par +212
        if (cleanPhone.startsWith("0")) {
            return "+212" + cleanPhone.substring(1);
        }
        
        // Si le numéro ne commence pas par 0, 212 ou +212, ajouter +212
        if (!cleanPhone.startsWith("212") && !cleanPhone.startsWith("+212")) {
            return "+212" + cleanPhone;
        }
        
        return cleanPhone;
    }
}

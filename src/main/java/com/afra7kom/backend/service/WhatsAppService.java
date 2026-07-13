package com.afra7kom.backend.service;

import com.afra7kom.backend.entity.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {
    
    @Value("${whatsapp.api.url:}")
    private String whatsappApiUrl;
    
    @Value("${whatsapp.api.token:}")
    private String whatsappApiToken;
    
    @Value("${whatsapp.admin.number:}")
    private String adminPhoneNumber;
    
    /**
     * Envoyer une notification de nouvelle réservation
     * Méthode asynchrone pour ne pas bloquer la requête HTTP
     */
    @Async("taskExecutor")
    public void sendReservationNotification(Reservation reservation) {
        try {
            String message = buildReservationNotificationMessage(reservation);
            sendWhatsAppMessage(adminPhoneNumber, message);
            log.info("Notification WhatsApp envoyée pour la réservation {}", reservation.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification WhatsApp", e);
        }
    }
    
    /**
     * Envoyer une notification d'approbation
     * Méthode asynchrone pour ne pas blocker la requête HTTP
     */
    @Async("taskExecutor")
    public void sendApprovalNotification(Reservation reservation) {
        try {
            String message = buildApprovalMessage(reservation);
            String customerPhone = reservation.getPhone();
            if (customerPhone != null && !customerPhone.isEmpty()) {
                sendWhatsAppMessage(customerPhone, message);
                log.info("Notification d'approbation WhatsApp envoyée au client {}", customerPhone);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification d'approbation WhatsApp", e);
        }
    }
    
    /**
     * Envoyer une notification de rejet
     * Méthode asynchrone pour ne pas bloquer la requête HTTP
     */
    @Async("taskExecutor")
    public void sendRejectionNotification(Reservation reservation, String reason) {
        try {
            String message = buildRejectionMessage(reservation, reason);
            String customerPhone = reservation.getPhone();
            if (customerPhone != null && !customerPhone.isEmpty()) {
                sendWhatsAppMessage(customerPhone, message);
                log.info("Notification de rejet WhatsApp envoyée au client {}", customerPhone);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification de rejet WhatsApp", e);
        }
    }
    
    /**
     * Construire le message de notification de réservation
     */
    private String buildReservationNotificationMessage(Reservation reservation) {
        StringBuilder message = new StringBuilder();
        message.append("🆕 *NOUVELLE RÉSERVATION*\n\n");
        message.append("👤 *Client:* ").append(reservation.getFirstName()).append(" ").append(reservation.getLastName()).append("\n");
        message.append("📱 *Téléphone:* ").append(reservation.getPhone()).append("\n");
        
        if (reservation.getEmail() != null && !reservation.getEmail().isEmpty()) {
            message.append("📧 *Email:* ").append(reservation.getEmail()).append("\n");
        }
        
        message.append("📅 *Période:* ").append(reservation.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
               .append(" - ").append(reservation.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        message.append("🔢 *Quantité:* ").append(reservation.getQuantity()).append("\n");
        
        if (reservation.getPackId() != null) {
            message.append("📦 *Pack:* ID ").append(reservation.getPackId()).append("\n");
        } else if (reservation.getMaterielId() != null) {
            message.append("🛠️ *Matériel:* ID ").append(reservation.getMaterielId()).append("\n");
        }
        
        message.append("📝 *Notes:* ").append(reservation.getNotes() != null ? reservation.getNotes() : "Aucune").append("\n");
        
        if (reservation.getDeliveryAddress() != null && !reservation.getDeliveryAddress().isEmpty()) {
            message.append("📍 *Adresse de livraison:* ").append(reservation.getDeliveryAddress()).append("\n");
        }
        
        message.append("\n⚠️ *Action requise:* Valider ou rejeter cette réservation");
        
        return message.toString();
    }
    
    /**
     * Construire le message d'approbation
     */
    private String buildApprovalMessage(Reservation reservation) {
        StringBuilder message = new StringBuilder();
        message.append("✅ *RÉSERVATION APPROUVÉE*\n\n");
        message.append("Bonjour ").append(reservation.getFirstName()).append(",\n\n");
        message.append("Votre réservation a été approuvée !\n\n");
        
        if (reservation.getPackId() != null) {
            message.append("📦 *Pack réservé:* ID ").append(reservation.getPackId()).append("\n");
        } else if (reservation.getMaterielId() != null) {
            message.append("🛠️ *Matériel réservé:* ID ").append(reservation.getMaterielId()).append("\n");
        }
        
        message.append("📅 *Période:* ").append(reservation.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
               .append(" - ").append(reservation.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        
        message.append("📞 Contactez-nous pour confirmer la livraison et le paiement du solde.\n\n");
        message.append("Merci de votre confiance !");
        
        return message.toString();
    }
    
    /**
     * Construire le message de rejet
     */
    private String buildRejectionMessage(Reservation reservation, String reason) {
        StringBuilder message = new StringBuilder();
        message.append("❌ *RÉSERVATION REJETÉE*\n\n");
        message.append("Bonjour ").append(reservation.getFirstName()).append(",\n\n");
        message.append("Nous regrettons de vous informer que votre réservation a été rejetée.\n\n");
        message.append("📝 *Motif:* ").append(reason).append("\n\n");
        
        message.append("📞 N'hésitez pas à nous contacter pour plus d'informations ou pour une nouvelle réservation.\n\n");
        message.append("Nous nous excusons pour ce désagrément.");
        
        return message.toString();
    }
    
    /**
     * Envoyer un message WhatsApp via l'API
     */
    private void sendWhatsAppMessage(String phoneNumber, String message) {
        // TODO: Implémenter l'intégration avec l'API WhatsApp Business
        // Pour l'instant, on simule l'envoi
        log.info("Simulation envoi WhatsApp à {}: {}", phoneNumber, message);
        
        // Exemple d'implémentation avec une API externe
        /*
        if (whatsappApiUrl != null && !whatsappApiUrl.isEmpty()) {
            // Créer la requête HTTP vers l'API WhatsApp
            // Utiliser RestTemplate ou WebClient
        }
        */
    }
}



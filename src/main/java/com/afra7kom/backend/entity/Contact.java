package com.afra7kom.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@EntityListeners(AuditingEntityListener.class)
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    private String fullName;

    @Column(name = "email", nullable = false)
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Column(name = "phone", nullable = false)
    @NotBlank(message = "Phone cannot be blank")
    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Column(name = "subject")
    @Size(max = 200, message = "Subject cannot exceed 200 characters")
    private String subject;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Message cannot be blank")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContactStatus status = ContactStatus.NEW;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "replied_by")
    private String repliedBy;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "reply_message", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Reply message cannot exceed 2000 characters")
    private String replyMessage;

    @Column(name = "whatsapp_url")
    @Size(max = 500, message = "WhatsApp URL cannot exceed 500 characters")
    private String whatsappUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ContactStatus {
        NEW,
        IN_PROGRESS,
        REPLIED,
        CLOSED,
        ARCHIVED
    }

    // Helper method to generate WhatsApp URL
    public String generateWhatsAppUrl() {
        if (this.phone != null && !this.phone.isEmpty()) {
            String cleanPhone = this.phone.replaceAll("[^0-9+]", "");
            String message = "Bonjour " + this.fullName + ", merci de nous avoir contactés. Comment puis-je vous aider?";
            return "https://wa.me/" + cleanPhone + "?text=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
        }
        return null;
    }
}






package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Contact;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactDto {

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Size(max = 200, message = "Subject cannot exceed 200 characters")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String message;

    private Contact.ContactStatus status;

    private Boolean isRead;

    private String repliedBy;

    private LocalDateTime repliedAt;

    @Size(max = 2000, message = "Reply message cannot exceed 2000 characters")
    private String replyMessage;

    private String whatsappUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Constructor for creating from entity
    public ContactDto(Contact contact) {
        this.id = contact.getId();
        this.fullName = contact.getFullName();
        this.email = contact.getEmail();
        this.phone = contact.getPhone();
        this.subject = contact.getSubject();
        this.message = contact.getMessage();
        this.status = contact.getStatus();
        this.isRead = contact.getIsRead();
        this.repliedBy = contact.getRepliedBy();
        this.repliedAt = contact.getRepliedAt();
        this.replyMessage = contact.getReplyMessage();
        this.whatsappUrl = contact.getWhatsappUrl();
        this.createdAt = contact.getCreatedAt();
        this.updatedAt = contact.getUpdatedAt();
    }

    // Convert DTO to Entity
    public Contact toEntity() {
        Contact contact = new Contact();
        contact.setId(this.id);
        contact.setFullName(this.fullName);
        contact.setEmail(this.email);
        contact.setPhone(this.phone);
        contact.setSubject(this.subject);
        contact.setMessage(this.message);
        contact.setStatus(this.status != null ? this.status : Contact.ContactStatus.NEW);
        contact.setIsRead(this.isRead != null ? this.isRead : false);
        contact.setRepliedBy(this.repliedBy);
        contact.setRepliedAt(this.repliedAt);
        contact.setReplyMessage(this.replyMessage);
        contact.setWhatsappUrl(this.whatsappUrl);
        return contact;
    }
}






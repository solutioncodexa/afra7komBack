package com.afra7kom.backend.repository;

import com.afra7kom.backend.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    // Find by status
    Page<Contact> findByStatus(Contact.ContactStatus status, Pageable pageable);

    // Find by read status
    Page<Contact> findByIsRead(Boolean isRead, Pageable pageable);

    // Find by email
    List<Contact> findByEmail(String email);

    // Find by phone
    List<Contact> findByPhone(String phone);

    // Find unread contacts
    @Query("SELECT c FROM Contact c WHERE c.isRead = false ORDER BY c.createdAt DESC")
    List<Contact> findUnreadContacts();

    // Count unread contacts
    @Query("SELECT COUNT(c) FROM Contact c WHERE c.isRead = false")
    long countUnreadContacts();

    // Find by status and date range
    @Query("SELECT c FROM Contact c WHERE c.status = :status AND c.createdAt BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC")
    Page<Contact> findByStatusAndDateRange(
            @Param("status") Contact.ContactStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Search contacts by name, email, phone, or message
    @Query("SELECT c FROM Contact c WHERE " +
           "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.message) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.subject) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Contact> searchContacts(@Param("keyword") String keyword, Pageable pageable);

    // Find recent contacts (last 7 days)
    @Query("SELECT c FROM Contact c WHERE c.createdAt >= :date ORDER BY c.createdAt DESC")
    List<Contact> findRecentContacts(@Param("date") LocalDateTime date);

    // Find contacts that need reply (status = NEW or IN_PROGRESS)
    @Query("SELECT c FROM Contact c WHERE c.status IN ('NEW', 'IN_PROGRESS') ORDER BY c.createdAt ASC")
    Page<Contact> findContactsNeedingReply(Pageable pageable);
}






-- Création de la table des notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    user_id BIGINT,
    reservation_id BIGINT,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    expires_at TIMESTAMP
);

-- Création des index pour optimiser les performances
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_reservation_id ON notifications(reservation_id);
CREATE INDEX IF NOT EXISTS idx_notifications_expires_at ON notifications(expires_at);

-- Création des contraintes
ALTER TABLE notifications ADD CONSTRAINT chk_notification_type 
    CHECK (type IN ('RESERVATION_CREATED', 'RESERVATION_CONFIRMED', 'RESERVATION_CANCELLED', 
                   'RESERVATION_MODIFIED', 'PAYMENT_RECEIVED', 'PAYMENT_OVERDUE', 
                   'SYSTEM_ALERT', 'MAINTENANCE_SCHEDULED', 'INVENTORY_LOW', 'USER_ACTION_REQUIRED'));

ALTER TABLE notifications ADD CONSTRAINT chk_notification_status 
    CHECK (status IN ('UNREAD', 'READ', 'ARCHIVED'));

-- Commentaires sur la table et les colonnes
COMMENT ON TABLE notifications IS 'Table des notifications utilisateur';
COMMENT ON COLUMN notifications.id IS 'Identifiant unique de la notification';
COMMENT ON COLUMN notifications.title IS 'Titre de la notification';
COMMENT ON COLUMN notifications.message IS 'Message de la notification';
COMMENT ON COLUMN notifications.type IS 'Type de notification (RESERVATION_CREATED, etc.)';
COMMENT ON COLUMN notifications.status IS 'Statut de la notification (UNREAD, READ, ARCHIVED)';
COMMENT ON COLUMN notifications.user_id IS 'ID de l''utilisateur destinataire';
COMMENT ON COLUMN notifications.reservation_id IS 'ID de la réservation liée (optionnel)';
COMMENT ON COLUMN notifications.related_entity_type IS 'Type d''entité liée (optionnel)';
COMMENT ON COLUMN notifications.related_entity_id IS 'ID de l''entité liée (optionnel)';
COMMENT ON COLUMN notifications.created_at IS 'Date de création de la notification';
COMMENT ON COLUMN notifications.read_at IS 'Date de lecture de la notification';
COMMENT ON COLUMN notifications.expires_at IS 'Date d''expiration de la notification (optionnel)';












CREATE TABLE IF NOT EXISTS operational_notification_recipients (
    id                     VARCHAR(36)  NOT NULL PRIMARY KEY,
    notification_id        VARCHAR(36)  NOT NULL,
    contact_id             VARCHAR(36),
    recipient_name         VARCHAR(255),
    recipient_email        VARCHAR(255),
    status                 VARCHAR(32)  NOT NULL DEFAULT 'SENT',
    delivery_status_detail VARCHAR(255),
    delivered_at           TIMESTAMP,
    CONSTRAINT fk_onr_notification FOREIGN KEY (notification_id) REFERENCES operational_notifications(id) ON DELETE CASCADE,
    CONSTRAINT fk_onr_contact FOREIGN KEY (contact_id) REFERENCES hospital_operational_contacts(id)
);

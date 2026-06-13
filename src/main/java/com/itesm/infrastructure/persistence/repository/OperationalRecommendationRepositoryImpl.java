package com.itesm.infrastructure.persistence.repository;

import com.itesm.domain.models.OperationalNotification;
import com.itesm.domain.models.OperationalNotificationRecipient;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.models.OperationalTask;
import com.itesm.domain.models.SupplyRequest;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import com.itesm.infrastructure.persistence.entity.AlertEntity;
import com.itesm.infrastructure.persistence.entity.HospitalDepartmentResourceEntity;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.HospitalInventoryItemEntity;
import com.itesm.infrastructure.persistence.entity.HospitalInventoryMovementEntity;
import com.itesm.infrastructure.persistence.entity.HospitalOperationalContactEntity;
import com.itesm.infrastructure.persistence.entity.HospitalOperationalGroupEntity;
import com.itesm.infrastructure.persistence.entity.HospitalStaffingProfileEntity;
import com.itesm.infrastructure.persistence.entity.OperationalNotificationEntity;
import com.itesm.infrastructure.persistence.entity.OperationalNotificationRecipientEntity;
import com.itesm.infrastructure.persistence.entity.OperationalRecommendationAuditEntity;
import com.itesm.infrastructure.persistence.entity.OperationalRecommendationEntity;
import com.itesm.infrastructure.persistence.entity.OperationalTaskEntity;
import com.itesm.infrastructure.persistence.entity.OutbreakEntity;
import com.itesm.infrastructure.persistence.entity.SupplyRequestEntity;
import com.itesm.infrastructure.persistence.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class OperationalRecommendationRepositoryImpl
        implements OperationalRecommendationRepository,
                   PanacheRepositoryBase<OperationalRecommendationEntity, UUID> {

    @Inject
    EntityManager em;

    @ConfigProperty(name = "quarkus.datasource.db-kind", defaultValue = "mysql")
    String dbKind;

    // -----------------------------------------------------------------------
    // Recommendations
    // -----------------------------------------------------------------------

    @Override
    public List<OperationalRecommendation> findByHospitalId(UUID hospitalId) {
        return find("hospital.id = ?1 ORDER BY createdAt DESC", hospitalId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<OperationalRecommendation> findByHospitalIdAndStatus(UUID hospitalId, String status) {
        return find("hospital.id = ?1 AND status = ?2 ORDER BY createdAt DESC", hospitalId, status)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<OperationalRecommendation> findByHospitalIdAndSeverity(UUID hospitalId, String severity) {
        return find("hospital.id = ?1 AND severity = ?2 ORDER BY createdAt DESC", hospitalId, severity)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<OperationalRecommendation> findRecommendationById(UUID id) {
        return findByIdOptional(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public OperationalRecommendation save(OperationalRecommendation rec) {
        OperationalRecommendationEntity e = rec.getId() != null
                ? em.find(OperationalRecommendationEntity.class, rec.getId())
                : null;
        if (e == null) {
            e = new OperationalRecommendationEntity();
            e.setId(rec.getId() != null ? rec.getId() : UUID.randomUUID());
            e.setCreatedAt(rec.getCreatedAt() != null ? rec.getCreatedAt() : LocalDateTime.now());
        }

        applyToEntity(rec, e);
        LocalDateTime now = LocalDateTime.now();
        e.setUpdatedAt(now);
        if (!em.contains(e)) {
            em.persist(e);
        }
        return toDomain(e);
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, String status) {
        OperationalRecommendationEntity e = em.find(OperationalRecommendationEntity.class, id);
        if (e == null) throw new jakarta.ws.rs.NotFoundException("Recommendation not found: " + id);
        e.setStatus(status);
        e.setUpdatedAt(LocalDateTime.now());
        if ("COMPLETED".equals(status) || "REJECTED".equals(status)) {
            e.setResolvedAt(LocalDateTime.now());
        }
    }

    // -----------------------------------------------------------------------
    // Audit
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public void appendAudit(OperationalRecommendationAudit audit) {
        OperationalRecommendationAuditEntity e = new OperationalRecommendationAuditEntity();
        e.setId(audit.getId() != null ? audit.getId() : UUID.randomUUID());
        e.setRecommendation(em.getReference(OperationalRecommendationEntity.class, audit.getRecommendationId()));
        e.setActorUserId(audit.getActorUserId());
        e.setEventType(audit.getEventType());
        e.setEventLabel(audit.getEventLabel());
        e.setEventPayloadJson(audit.getEventPayloadJson());
        e.setCreatedAt(audit.getCreatedAt() != null ? audit.getCreatedAt() : LocalDateTime.now());
        em.persist(e);
    }

    @Override
    public List<OperationalRecommendationAudit> findAuditByRecommendationId(UUID recommendationId) {
        return em.createQuery(
                "SELECT a FROM OperationalRecommendationAuditEntity a WHERE a.recommendation.id = :rid ORDER BY a.createdAt ASC",
                OperationalRecommendationAuditEntity.class)
                .setParameter("rid", recommendationId)
                .getResultList()
                .stream().map(this::auditToDomain).collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Tasks
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public OperationalTask createTask(OperationalTask task) {
        OperationalTaskEntity e = new OperationalTaskEntity();
        e.setId(task.getId() != null ? task.getId() : UUID.randomUUID());
        e.setRecommendation(em.getReference(OperationalRecommendationEntity.class, task.getRecommendationId()));
        e.setHospital(em.getReference(HospitalEntity.class, task.getHospitalId()));
        e.setOwnerUserId(task.getOwnerUserId());
        if (task.getOwnerContactId() != null) {
            e.setOwnerContact(em.getReference(HospitalOperationalContactEntity.class, task.getOwnerContactId()));
        }
        if (task.getOwnerGroupId() != null) {
            e.setOwnerGroup(em.getReference(HospitalOperationalGroupEntity.class, task.getOwnerGroupId()));
        }
        e.setOwnerLabel(task.getOwnerLabel());
        e.setDepartmentLabel(task.getDepartmentLabel());
        e.setDeadlineAt(task.getDeadlineAt());
        e.setPriority(task.getPriority() != null ? task.getPriority() : "MEDIUM");
        e.setNotes(task.getNotes());
        e.setStatus(task.getStatus() != null ? task.getStatus() : "PENDING");
        e.setSourceActionCode(task.getSourceActionCode());
        if (task.getRecommendedByRecommendationId() != null) {
            e.setRecommendedByRecommendation(
                    em.getReference(OperationalRecommendationEntity.class, task.getRecommendedByRecommendationId()));
        }
        e.setCreatedByUserId(task.getCreatedByUserId());
        LocalDateTime now = LocalDateTime.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        em.persist(e);
        task.setId(e.getId());
        task.setCreatedAt(e.getCreatedAt());
        task.setUpdatedAt(e.getUpdatedAt());
        return task;
    }

    @Override
    @Transactional
    public OperationalTask updateTask(OperationalTask task) {
        OperationalTaskEntity e = em.find(OperationalTaskEntity.class, task.getId());
        if (e == null) throw new jakarta.ws.rs.NotFoundException("Task not found: " + task.getId());
        e.setOwnerUserId(task.getOwnerUserId());
        e.setOwnerContact(task.getOwnerContactId() != null
                ? em.getReference(HospitalOperationalContactEntity.class, task.getOwnerContactId())
                : null);
        e.setOwnerGroup(task.getOwnerGroupId() != null
                ? em.getReference(HospitalOperationalGroupEntity.class, task.getOwnerGroupId())
                : null);
        e.setOwnerLabel(task.getOwnerLabel());
        e.setDepartmentLabel(task.getDepartmentLabel());
        e.setDeadlineAt(task.getDeadlineAt());
        e.setPriority(task.getPriority() != null ? task.getPriority() : e.getPriority());
        e.setNotes(task.getNotes());
        e.setStatus(task.getStatus() != null ? task.getStatus() : e.getStatus());
        e.setSourceActionCode(task.getSourceActionCode());
        e.setUpdatedAt(LocalDateTime.now());
        return taskToDomain(e);
    }

    @Override
    public List<OperationalTask> findTasksByRecommendationId(UUID recommendationId) {
        return em.createQuery(
                "SELECT t FROM OperationalTaskEntity t WHERE t.recommendation.id = :rid ORDER BY t.createdAt DESC",
                OperationalTaskEntity.class)
                .setParameter("rid", recommendationId)
                .getResultList()
                .stream().map(this::taskToDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<OperationalTask> findActiveTaskByRecommendationId(UUID recommendationId) {
        return em.createQuery(
                "SELECT t FROM OperationalTaskEntity t WHERE t.recommendation.id = :rid AND t.status <> 'COMPLETED' AND t.status <> 'CANCELLED' ORDER BY t.createdAt DESC",
                OperationalTaskEntity.class)
                .setParameter("rid", recommendationId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .map(this::taskToDomain);
    }

    // -----------------------------------------------------------------------
    // Notifications
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public OperationalNotification createNotification(OperationalNotification notification) {
        OperationalNotificationEntity e = new OperationalNotificationEntity();
        e.setId(notification.getId() != null ? notification.getId() : UUID.randomUUID());
        e.setRecommendation(em.getReference(OperationalRecommendationEntity.class, notification.getRecommendationId()));
        e.setHospital(em.getReference(HospitalEntity.class, notification.getHospitalId()));
        if (notification.getAudienceGroupId() != null) {
            e.setAudienceGroup(em.getReference(HospitalOperationalGroupEntity.class, notification.getAudienceGroupId()));
        }
        if (notification.getAudienceContactId() != null) {
            e.setAudienceContact(em.getReference(HospitalOperationalContactEntity.class, notification.getAudienceContactId()));
        }
        e.setAudienceType(notification.getAudienceType());
        e.setAudienceDepartmentCode(notification.getAudienceDepartmentCode());
        e.setAudienceLabel(notification.getAudienceLabel());
        e.setMessage(notification.getMessage());
        e.setStatus(notification.getStatus() != null ? notification.getStatus() : "SENT");
        e.setDeliveryChannel(notification.getDeliveryChannel());
        e.setDeliveryStatusDetail(notification.getDeliveryStatusDetail());
        e.setSourceActionCode(notification.getSourceActionCode());
        e.setSentByUserId(notification.getSentByUserId());
        e.setSentAt(notification.getSentAt() != null ? notification.getSentAt() : LocalDateTime.now());
        em.persist(e);
        notification.setId(e.getId());
        notification.setSentAt(e.getSentAt());
        return notification;
    }

    @Override
    public List<OperationalNotification> findNotificationsByRecommendationId(UUID recommendationId) {
        return em.createQuery(
                "SELECT n FROM OperationalNotificationEntity n WHERE n.recommendation.id = :rid ORDER BY n.sentAt DESC",
                OperationalNotificationEntity.class)
                .setParameter("rid", recommendationId)
                .getResultList()
                .stream().map(this::notificationToDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OperationalNotificationRecipient createNotificationRecipient(OperationalNotificationRecipient recipient) {
        OperationalNotificationRecipientEntity e = new OperationalNotificationRecipientEntity();
        e.setId(recipient.getId() != null ? recipient.getId() : UUID.randomUUID());
        e.setNotification(em.getReference(OperationalNotificationEntity.class, recipient.getNotificationId()));
        if (recipient.getContactId() != null) {
            e.setContact(em.getReference(HospitalOperationalContactEntity.class, recipient.getContactId()));
        }
        e.setRecipientName(recipient.getRecipientName());
        e.setRecipientEmail(recipient.getRecipientEmail());
        e.setStatus(recipient.getStatus() != null ? recipient.getStatus() : "SENT");
        e.setDeliveryStatusDetail(recipient.getDeliveryStatusDetail());
        e.setDeliveredAt(recipient.getDeliveredAt() != null ? recipient.getDeliveredAt() : LocalDateTime.now());
        em.persist(e);
        return recipientToDomain(e);
    }

    @Override
    public List<OperationalNotificationRecipient> findNotificationRecipientsByNotificationId(UUID notificationId) {
        return em.createQuery("""
                SELECT r
                FROM OperationalNotificationRecipientEntity r
                WHERE r.notification.id = :notificationId
                ORDER BY r.deliveredAt ASC
                """, OperationalNotificationRecipientEntity.class)
                .setParameter("notificationId", notificationId)
                .getResultList()
                .stream().map(this::recipientToDomain).collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Supply requests
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public SupplyRequest createSupplyRequest(SupplyRequest sr) {
        SupplyRequestEntity e = new SupplyRequestEntity();
        e.setId(sr.getId() != null ? sr.getId() : UUID.randomUUID());
        if (sr.getRecommendationId() != null) {
            e.setRecommendation(em.getReference(OperationalRecommendationEntity.class, sr.getRecommendationId()));
        }
        e.setHospital(em.getReference(HospitalEntity.class, sr.getHospitalId()));
        if (sr.getInventoryItemId() != null) {
            e.setInventoryItem(em.getReference(HospitalInventoryItemEntity.class, sr.getInventoryItemId()));
        }
        e.setSupplyTypeLabel(sr.getSupplyTypeLabel());
        e.setQuantity(sr.getQuantity());
        e.setUnit(sr.getUnit());
        e.setDestination(sr.getDestination());
        e.setSuggestedSupplier(sr.getSuggestedSupplier());
        e.setStatus(sr.getStatus() != null ? sr.getStatus() : "REQUESTED");
        e.setSourceActionCode(sr.getSourceActionCode());
        e.setPriority(sr.getPriority());
        e.setRequestedNeededBy(sr.getRequestedNeededBy());
        if (sr.getLinkedRecommendationInventoryItemId() != null) {
            e.setLinkedRecommendationInventoryItem(
                    em.getReference(HospitalInventoryItemEntity.class, sr.getLinkedRecommendationInventoryItemId()));
        }
        e.setRequestedByUserId(sr.getRequestedByUserId());
        LocalDateTime now = LocalDateTime.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        em.persist(e);
        sr.setId(e.getId());
        sr.setCreatedAt(e.getCreatedAt());
        sr.setUpdatedAt(e.getUpdatedAt());
        return sr;
    }

    @Override
    @Transactional
    public SupplyRequest createSupplyRequestWithMovement(SupplyRequest sr, String movementNotes) {
        if (sr.getInventoryItemId() == null) {
            return createSupplyRequest(sr);
        }

        if ("mysql".equalsIgnoreCase(dbKind)) {
            UUID requestId = sr.getId() != null ? sr.getId() : UUID.randomUUID();
            UUID movementId = UUID.randomUUID();
            em.createNativeQuery("""
                    CALL sp_create_supply_request_with_movement(
                        ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10,
                        ?11, ?12, ?13, ?14, ?15, ?16
                    )
                    """)
                    .setParameter(1, requestId.toString())
                    .setParameter(2, movementId.toString())
                    .setParameter(3, sr.getRecommendationId().toString())
                    .setParameter(4, sr.getHospitalId().toString())
                    .setParameter(5, sr.getInventoryItemId().toString())
                    .setParameter(6, sr.getSupplyTypeLabel())
                    .setParameter(7, sr.getQuantity())
                    .setParameter(8, sr.getUnit())
                    .setParameter(9, sr.getDestination())
                    .setParameter(10, sr.getSuggestedSupplier())
                    .setParameter(11, sr.getSourceActionCode())
                    .setParameter(12, sr.getPriority())
                    .setParameter(13, sr.getRequestedNeededBy())
                    .setParameter(14, sr.getLinkedRecommendationInventoryItemId() != null
                            ? sr.getLinkedRecommendationInventoryItemId().toString()
                            : null)
                    .setParameter(15, sr.getRequestedByUserId() != null ? sr.getRequestedByUserId().toString() : null)
                    .setParameter(16, movementNotes)
                    .executeUpdate();

            return supplyToDomain(em.find(SupplyRequestEntity.class, requestId));
        }

        SupplyRequest created = createSupplyRequest(sr);
        HospitalInventoryMovementEntity movement = new HospitalInventoryMovementEntity();
        movement.setId(UUID.randomUUID());
        movement.setHospital(em.getReference(HospitalEntity.class, created.getHospitalId()));
        movement.setInventoryItem(em.getReference(HospitalInventoryItemEntity.class, created.getInventoryItemId()));
        movement.setMovementType("REPLENISHMENT");
        movement.setQuantityDelta(created.getQuantity());
        movement.setUnit(created.getUnit());
        movement.setNotes(movementNotes);
        movement.setRelatedSupplyRequestId(created.getId());
        movement.setCreatedAt(LocalDateTime.now());
        em.persist(movement);
        return created;
    }

    @Override
    public List<SupplyRequest> findSupplyRequestsByRecommendationId(UUID recommendationId) {
        return em.createQuery(
                "SELECT s FROM SupplyRequestEntity s WHERE s.recommendation.id = :rid ORDER BY s.createdAt DESC",
                SupplyRequestEntity.class)
                .setParameter("rid", recommendationId)
                .getResultList()
                .stream().map(this::supplyToDomain).collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Mapping helpers
    // -----------------------------------------------------------------------

    private OperationalRecommendation toDomain(OperationalRecommendationEntity e) {
        OperationalRecommendation r = new OperationalRecommendation();
        r.setId(e.getId());
        r.setHospitalId(e.getHospital().getId());
        if (e.getSourceAlert() != null) r.setSourceAlertId(e.getSourceAlert().getId());
        if (e.getSourceOutbreak() != null) r.setSourceOutbreakId(e.getSourceOutbreak().getId());
        r.setType(e.getType());
        r.setSeverity(e.getSeverity());
        r.setStatus(e.getStatus());
        r.setCategory(e.getCategory());
        r.setTitle(e.getTitle());
        r.setDescription(e.getDescription());
        r.setExpectedImpact(e.getExpectedImpact());
        r.setUrgencyWindow(e.getUrgencyWindow());
        r.setConfidenceScore(e.getConfidenceScore());
        r.setContentTranslationsJson(e.getContentTranslationsJson());
        r.setImageMode(e.getImageMode());
        r.setRationaleJson(e.getRationaleJson());
        r.setRecommendedActionsJson(e.getRecommendedActionsJson());
        r.setAffectedDepartmentsJson(e.getAffectedDepartmentsJson());
        r.setAffectedResourcesJson(e.getAffectedResourcesJson());
        if (e.getPrimaryDepartmentResource() != null) r.setPrimaryDepartmentResourceId(e.getPrimaryDepartmentResource().getId());
        if (e.getPrimaryStaffingProfile() != null) r.setPrimaryStaffingProfileId(e.getPrimaryStaffingProfile().getId());
        if (e.getPrimaryInventoryItem() != null) r.setPrimaryInventoryItemId(e.getPrimaryInventoryItem().getId());
        r.setPresentationVariant(e.getPresentationVariant());
        r.setPrimaryActionCode(e.getPrimaryActionCode());
        r.setAvailableActionsJson(e.getAvailableActionsJson());
        r.setAllowedStatusTransitionsJson(e.getAllowedStatusTransitionsJson());
        r.setDisplayCategoryLabel(e.getDisplayCategoryLabel());
        r.setDisplaySeverityLabel(e.getDisplaySeverityLabel());
        r.setDisplayStatusLabel(e.getDisplayStatusLabel());
        r.setExpiresAt(e.getExpiresAt());
        if (e.getAssignedOwnerUser() != null) r.setAssignedOwnerUserId(e.getAssignedOwnerUser().getId());
        r.setModelProvider(e.getModelProvider());
        r.setModelVersion(e.getModelVersion());
        r.setInputContextJson(e.getInputContextJson());
        r.setCreatedByMode(e.getCreatedByMode());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        r.setResolvedAt(e.getResolvedAt());
        return r;
    }

    private void applyToEntity(OperationalRecommendation r, OperationalRecommendationEntity e) {
        e.setHospital(em.getReference(HospitalEntity.class, r.getHospitalId()));
        e.setSourceAlert(r.getSourceAlertId() != null
                ? em.getReference(AlertEntity.class, r.getSourceAlertId())
                : null);
        e.setSourceOutbreak(r.getSourceOutbreakId() != null
                ? em.getReference(OutbreakEntity.class, r.getSourceOutbreakId())
                : null);
        e.setType(r.getType());
        e.setSeverity(r.getSeverity() != null ? r.getSeverity() : "MEDIUM");
        e.setStatus(r.getStatus() != null ? r.getStatus() : "NEW");
        e.setCategory(r.getCategory());
        e.setTitle(r.getTitle());
        e.setDescription(r.getDescription());
        e.setExpectedImpact(r.getExpectedImpact());
        e.setUrgencyWindow(r.getUrgencyWindow());
        e.setConfidenceScore(r.getConfidenceScore());
        e.setContentTranslationsJson(r.getContentTranslationsJson());
        e.setImageMode(r.getImageMode());
        e.setRationaleJson(r.getRationaleJson());
        e.setRecommendedActionsJson(r.getRecommendedActionsJson());
        e.setAffectedDepartmentsJson(r.getAffectedDepartmentsJson());
        e.setAffectedResourcesJson(r.getAffectedResourcesJson());
        e.setPrimaryDepartmentResource(r.getPrimaryDepartmentResourceId() != null
                ? em.getReference(HospitalDepartmentResourceEntity.class, r.getPrimaryDepartmentResourceId())
                : null);
        e.setPrimaryStaffingProfile(r.getPrimaryStaffingProfileId() != null
                ? em.getReference(HospitalStaffingProfileEntity.class, r.getPrimaryStaffingProfileId())
                : null);
        e.setPrimaryInventoryItem(r.getPrimaryInventoryItemId() != null
                ? em.getReference(HospitalInventoryItemEntity.class, r.getPrimaryInventoryItemId())
                : null);
        e.setPresentationVariant(r.getPresentationVariant());
        e.setPrimaryActionCode(r.getPrimaryActionCode());
        e.setAvailableActionsJson(r.getAvailableActionsJson());
        e.setAllowedStatusTransitionsJson(r.getAllowedStatusTransitionsJson());
        e.setDisplayCategoryLabel(r.getDisplayCategoryLabel());
        e.setDisplaySeverityLabel(r.getDisplaySeverityLabel());
        e.setDisplayStatusLabel(r.getDisplayStatusLabel());
        e.setExpiresAt(r.getExpiresAt());
        e.setAssignedOwnerUser(r.getAssignedOwnerUserId() != null
                ? em.getReference(UserEntity.class, r.getAssignedOwnerUserId())
                : null);
        e.setModelProvider(r.getModelProvider());
        e.setModelVersion(r.getModelVersion());
        e.setInputContextJson(r.getInputContextJson());
        e.setCreatedByMode(r.getCreatedByMode() != null ? r.getCreatedByMode() : "RULE_ENGINE");
        if (r.getCreatedAt() != null) {
            e.setCreatedAt(r.getCreatedAt());
        }
        e.setUpdatedAt(r.getUpdatedAt());
        e.setResolvedAt(r.getResolvedAt());
    }

    private OperationalRecommendationAudit auditToDomain(OperationalRecommendationAuditEntity e) {
        OperationalRecommendationAudit a = new OperationalRecommendationAudit();
        a.setId(e.getId());
        a.setRecommendationId(e.getRecommendation().getId());
        a.setActorUserId(e.getActorUserId());
        a.setEventType(e.getEventType());
        a.setEventLabel(e.getEventLabel());
        a.setEventPayloadJson(e.getEventPayloadJson());
        a.setCreatedAt(e.getCreatedAt());
        return a;
    }

    private OperationalTask taskToDomain(OperationalTaskEntity e) {
        OperationalTask t = new OperationalTask();
        t.setId(e.getId());
        t.setRecommendationId(e.getRecommendation().getId());
        t.setHospitalId(e.getHospital().getId());
        t.setOwnerUserId(e.getOwnerUserId());
        if (e.getOwnerContact() != null) t.setOwnerContactId(e.getOwnerContact().getId());
        if (e.getOwnerGroup() != null) t.setOwnerGroupId(e.getOwnerGroup().getId());
        t.setOwnerLabel(e.getOwnerLabel());
        t.setDepartmentLabel(e.getDepartmentLabel());
        t.setDeadlineAt(e.getDeadlineAt());
        t.setPriority(e.getPriority());
        t.setNotes(e.getNotes());
        t.setStatus(e.getStatus());
        t.setSourceActionCode(e.getSourceActionCode());
        if (e.getRecommendedByRecommendation() != null) {
            t.setRecommendedByRecommendationId(e.getRecommendedByRecommendation().getId());
        }
        t.setCreatedByUserId(e.getCreatedByUserId());
        t.setCreatedAt(e.getCreatedAt());
        t.setUpdatedAt(e.getUpdatedAt());
        return t;
    }

    private OperationalNotification notificationToDomain(OperationalNotificationEntity e) {
        OperationalNotification n = new OperationalNotification();
        n.setId(e.getId());
        n.setRecommendationId(e.getRecommendation().getId());
        n.setHospitalId(e.getHospital().getId());
        if (e.getAudienceGroup() != null) n.setAudienceGroupId(e.getAudienceGroup().getId());
        if (e.getAudienceContact() != null) n.setAudienceContactId(e.getAudienceContact().getId());
        n.setAudienceType(e.getAudienceType());
        n.setAudienceDepartmentCode(e.getAudienceDepartmentCode());
        n.setAudienceLabel(e.getAudienceLabel());
        n.setMessage(e.getMessage());
        n.setStatus(e.getStatus());
        n.setDeliveryChannel(e.getDeliveryChannel());
        n.setDeliveryStatusDetail(e.getDeliveryStatusDetail());
        n.setSourceActionCode(e.getSourceActionCode());
        n.setSentByUserId(e.getSentByUserId());
        n.setSentAt(e.getSentAt());
        n.setRecipients(findNotificationRecipientsByNotificationId(e.getId()));
        return n;
    }

    private OperationalNotificationRecipient recipientToDomain(OperationalNotificationRecipientEntity e) {
        OperationalNotificationRecipient r = new OperationalNotificationRecipient();
        r.setId(e.getId());
        r.setNotificationId(e.getNotification().getId());
        if (e.getContact() != null) r.setContactId(e.getContact().getId());
        r.setRecipientName(e.getRecipientName());
        r.setRecipientEmail(e.getRecipientEmail());
        r.setStatus(e.getStatus());
        r.setDeliveryStatusDetail(e.getDeliveryStatusDetail());
        r.setDeliveredAt(e.getDeliveredAt());
        return r;
    }

    private SupplyRequest supplyToDomain(SupplyRequestEntity e) {
        SupplyRequest sr = new SupplyRequest();
        sr.setId(e.getId());
        if (e.getRecommendation() != null) sr.setRecommendationId(e.getRecommendation().getId());
        sr.setHospitalId(e.getHospital().getId());
        if (e.getInventoryItem() != null) sr.setInventoryItemId(e.getInventoryItem().getId());
        sr.setSupplyTypeLabel(e.getSupplyTypeLabel());
        sr.setQuantity(e.getQuantity());
        sr.setUnit(e.getUnit());
        sr.setDestination(e.getDestination());
        sr.setSuggestedSupplier(e.getSuggestedSupplier());
        sr.setStatus(e.getStatus());
        sr.setSourceActionCode(e.getSourceActionCode());
        sr.setPriority(e.getPriority());
        sr.setRequestedNeededBy(e.getRequestedNeededBy());
        if (e.getLinkedRecommendationInventoryItem() != null) {
            sr.setLinkedRecommendationInventoryItemId(e.getLinkedRecommendationInventoryItem().getId());
        }
        sr.setRequestedByUserId(e.getRequestedByUserId());
        sr.setCreatedAt(e.getCreatedAt());
        sr.setUpdatedAt(e.getUpdatedAt());
        return sr;
    }
}

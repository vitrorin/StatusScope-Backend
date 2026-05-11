package com.itesm.infrastructure.persistence.repository;

import com.itesm.domain.models.OperationalNotification;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.models.OperationalTask;
import com.itesm.domain.models.SupplyRequest;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.HospitalInventoryItemEntity;
import com.itesm.infrastructure.persistence.entity.OperationalNotificationEntity;
import com.itesm.infrastructure.persistence.entity.OperationalRecommendationAuditEntity;
import com.itesm.infrastructure.persistence.entity.OperationalRecommendationEntity;
import com.itesm.infrastructure.persistence.entity.OperationalTaskEntity;
import com.itesm.infrastructure.persistence.entity.SupplyRequestEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

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
        OperationalRecommendationEntity e = toEntity(rec);
        if (e.getId() == null) {
            e.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        if (e.getCreatedAt() == null) e.setCreatedAt(now);
        e.setUpdatedAt(now);
        persist(e);
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
        e.setOwnerLabel(task.getOwnerLabel());
        e.setDepartmentLabel(task.getDepartmentLabel());
        e.setDeadlineAt(task.getDeadlineAt());
        e.setPriority(task.getPriority() != null ? task.getPriority() : "MEDIUM");
        e.setNotes(task.getNotes());
        e.setStatus(task.getStatus() != null ? task.getStatus() : "PENDING");
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
    public List<OperationalTask> findTasksByRecommendationId(UUID recommendationId) {
        return em.createQuery(
                "SELECT t FROM OperationalTaskEntity t WHERE t.recommendation.id = :rid ORDER BY t.createdAt DESC",
                OperationalTaskEntity.class)
                .setParameter("rid", recommendationId)
                .getResultList()
                .stream().map(this::taskToDomain).collect(Collectors.toList());
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
        e.setAudienceLabel(notification.getAudienceLabel());
        e.setMessage(notification.getMessage());
        e.setStatus(notification.getStatus() != null ? notification.getStatus() : "SENT");
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

    // -----------------------------------------------------------------------
    // Supply requests
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public SupplyRequest createSupplyRequest(SupplyRequest sr) {
        SupplyRequestEntity e = new SupplyRequestEntity();
        e.setId(sr.getId() != null ? sr.getId() : UUID.randomUUID());
        e.setRecommendation(em.getReference(OperationalRecommendationEntity.class, sr.getRecommendationId()));
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
        r.setImageMode(e.getImageMode());
        r.setRationaleJson(e.getRationaleJson());
        r.setRecommendedActionsJson(e.getRecommendedActionsJson());
        r.setAffectedDepartmentsJson(e.getAffectedDepartmentsJson());
        r.setAffectedResourcesJson(e.getAffectedResourcesJson());
        r.setModelProvider(e.getModelProvider());
        r.setModelVersion(e.getModelVersion());
        r.setInputContextJson(e.getInputContextJson());
        r.setCreatedByMode(e.getCreatedByMode());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        r.setResolvedAt(e.getResolvedAt());
        return r;
    }

    private OperationalRecommendationEntity toEntity(OperationalRecommendation r) {
        OperationalRecommendationEntity e = new OperationalRecommendationEntity();
        e.setId(r.getId());
        e.setHospital(em.getReference(HospitalEntity.class, r.getHospitalId()));
        e.setType(r.getType());
        e.setSeverity(r.getSeverity() != null ? r.getSeverity() : "MEDIUM");
        e.setStatus(r.getStatus() != null ? r.getStatus() : "NEW");
        e.setCategory(r.getCategory());
        e.setTitle(r.getTitle());
        e.setDescription(r.getDescription());
        e.setExpectedImpact(r.getExpectedImpact());
        e.setUrgencyWindow(r.getUrgencyWindow());
        e.setConfidenceScore(r.getConfidenceScore());
        e.setImageMode(r.getImageMode());
        e.setRationaleJson(r.getRationaleJson());
        e.setRecommendedActionsJson(r.getRecommendedActionsJson());
        e.setAffectedDepartmentsJson(r.getAffectedDepartmentsJson());
        e.setAffectedResourcesJson(r.getAffectedResourcesJson());
        e.setModelProvider(r.getModelProvider());
        e.setModelVersion(r.getModelVersion());
        e.setInputContextJson(r.getInputContextJson());
        e.setCreatedByMode(r.getCreatedByMode() != null ? r.getCreatedByMode() : "RULE_ENGINE");
        e.setCreatedAt(r.getCreatedAt());
        e.setUpdatedAt(r.getUpdatedAt());
        e.setResolvedAt(r.getResolvedAt());
        return e;
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
        t.setOwnerLabel(e.getOwnerLabel());
        t.setDepartmentLabel(e.getDepartmentLabel());
        t.setDeadlineAt(e.getDeadlineAt());
        t.setPriority(e.getPriority());
        t.setNotes(e.getNotes());
        t.setStatus(e.getStatus());
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
        n.setAudienceLabel(e.getAudienceLabel());
        n.setMessage(e.getMessage());
        n.setStatus(e.getStatus());
        n.setSentByUserId(e.getSentByUserId());
        n.setSentAt(e.getSentAt());
        return n;
    }

    private SupplyRequest supplyToDomain(SupplyRequestEntity e) {
        SupplyRequest sr = new SupplyRequest();
        sr.setId(e.getId());
        sr.setRecommendationId(e.getRecommendation().getId());
        sr.setHospitalId(e.getHospital().getId());
        if (e.getInventoryItem() != null) sr.setInventoryItemId(e.getInventoryItem().getId());
        sr.setSupplyTypeLabel(e.getSupplyTypeLabel());
        sr.setQuantity(e.getQuantity());
        sr.setUnit(e.getUnit());
        sr.setDestination(e.getDestination());
        sr.setSuggestedSupplier(e.getSuggestedSupplier());
        sr.setStatus(e.getStatus());
        sr.setRequestedByUserId(e.getRequestedByUserId());
        sr.setCreatedAt(e.getCreatedAt());
        sr.setUpdatedAt(e.getUpdatedAt());
        return sr;
    }
}

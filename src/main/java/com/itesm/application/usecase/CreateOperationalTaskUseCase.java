package com.itesm.application.usecase;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.port.out.OperationalEmailGateway;
import com.itesm.application.port.out.OperationalEmailMessage;
import com.itesm.application.port.out.OperationalEmailResult;
import com.itesm.application.usecase.exception.ConflictException;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.models.OperationalNotification;
import com.itesm.domain.models.OperationalNotificationRecipient;
import com.itesm.domain.models.OperationalTask;
import com.itesm.domain.models.HospitalOperationalContact;
import com.itesm.domain.models.HospitalOperationalGroup;
import com.itesm.domain.repository.OperationalDirectoryRepository;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class CreateOperationalTaskUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalRecommendationRepository repository;
    @Inject OperationalDirectoryRepository operationalDirectoryRepository;
    @Inject OperationalEmailGateway operationalEmailGateway;
    @Inject OperationalEmailTemplateBuilder emailTemplateBuilder;

    @Transactional
    public OperationalTask execute(UUID recommendationId, OperationalTask input) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();

        OperationalRecommendation rec = repository.findRecommendationById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found: " + recommendationId));

        if (hospitalId != null && !rec.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Recommendation not found: " + recommendationId);
        }

        input.setRecommendationId(recommendationId);
        input.setHospitalId(rec.getHospitalId());
        input.setCreatedByUserId(currentUser.getUserId());
        input.setStatus(input.getStatus() != null ? input.getStatus() : "PENDING");
        input.setPriority(rec.getSeverity() != null ? rec.getSeverity() : "MEDIUM");
        input.setSourceActionCode(input.getSourceActionCode() != null ? input.getSourceActionCode() : "ASSIGN_TASK");
        input.setRecommendedByRecommendationId(recommendationId);

        HospitalOperationalContact ownerContact = null;
        if (input.getOwnerContactId() != null) {
            ownerContact = operationalDirectoryRepository.findContactById(input.getOwnerContactId())
                    .orElseThrow(() -> new NotFoundException("Operational contact not found: " + input.getOwnerContactId()));
            validateContact(ownerContact, rec.getHospitalId(), true);
            input.setOwnerLabel(input.getOwnerLabel() != null ? input.getOwnerLabel() : ownerContact.getDisplayName());
            input.setDepartmentLabel(input.getDepartmentLabel() != null ? input.getDepartmentLabel() : ownerContact.getDepartmentCode());
            input.setOwnerUserId(input.getOwnerUserId() != null ? input.getOwnerUserId() : ownerContact.getUserId());
        }
        if (input.getOwnerGroupId() != null) {
            HospitalOperationalGroup group = operationalDirectoryRepository.findGroupById(input.getOwnerGroupId()).orElse(null);
            if (group != null && input.getOwnerLabel() == null) {
                input.setOwnerLabel(group.getGroupName());
            }
        }

        OperationalTask activeTask = repository.findActiveTaskByRecommendationId(recommendationId).orElse(null);
        boolean reassigned = activeTask != null;
        OperationalTask created;
        if (reassigned) {
            input.setId(activeTask.getId());
            input.setCreatedAt(activeTask.getCreatedAt());
            created = repository.updateTask(input);
        } else {
            created = repository.createTask(input);
        }

        // Update recommendation status to ASSIGNED if it was NEW or ACCEPTED
        if ("NEW".equals(rec.getStatus()) || "ACCEPTED".equals(rec.getStatus())) {
            repository.updateStatus(recommendationId, "ASSIGNED");
        }
        if (input.getOwnerUserId() != null) {
            rec.setAssignedOwnerUserId(input.getOwnerUserId());
            rec.setStatus("ASSIGNED");
            repository.save(rec);
        }

        OperationalRecommendationAudit audit = new OperationalRecommendationAudit();
        audit.setRecommendationId(recommendationId);
        audit.setActorUserId(currentUser.getUserId());
        audit.setEventType(reassigned ? "TASK_REASSIGNED" : "TASK_CREATED");
        audit.setEventLabel((reassigned ? "Task reassigned to: " : "Task assigned to: ")
                + (input.getOwnerLabel() != null ? input.getOwnerLabel() : "unassigned"));
        audit.setCreatedAt(LocalDateTime.now());
        repository.appendAudit(audit);

        if (ownerContact != null) {
            createTaskEmailEvidence(rec, created, ownerContact, currentUser.getUserId());
        }

        return created;
    }

    private void validateContact(HospitalOperationalContact contact, UUID hospitalId, boolean requireAssignable) {
        if (!hospitalId.equals(contact.getHospitalId())) {
            throw new NotFoundException("Operational contact not found: " + contact.getId());
        }
        if ("INACTIVE".equalsIgnoreCase(contact.getAvailabilityStatus())) {
            throw new ConflictException("Operational contact is inactive");
        }
        if (requireAssignable && !contact.isAssignable()) {
            throw new ConflictException("Operational contact is not assignable");
        }
    }

    private void createTaskEmailEvidence(
            OperationalRecommendation rec,
            OperationalTask task,
            HospitalOperationalContact contact,
            UUID actorUserId) {
        OperationalEmailResult result = sendTaskEmail(rec, task, contact);

        OperationalNotification notification = new OperationalNotification();
        notification.setRecommendationId(rec.getId());
        notification.setHospitalId(rec.getHospitalId());
        notification.setAudienceType("CONTACT");
        notification.setAudienceContactId(contact.getId());
        notification.setAudienceDepartmentCode(contact.getDepartmentCode());
        notification.setAudienceLabel(contact.getDisplayName());
        notification.setMessage(emailTemplateBuilder.taskAssignment(contact.getContactValue(), rec, task, contact.getDisplayName()).body());
        notification.setStatus(result.status());
        notification.setDeliveryChannel("EMAIL");
        notification.setDeliveryStatusDetail(result.detail());
        notification.setSourceActionCode("TASK_EMAIL");
        notification.setSentByUserId(actorUserId);
        notification.setSentAt(LocalDateTime.now());
        OperationalNotification created = repository.createNotification(notification);

        OperationalNotificationRecipient recipient = new OperationalNotificationRecipient();
        recipient.setNotificationId(created.getId());
        recipient.setContactId(contact.getId());
        recipient.setRecipientName(contact.getDisplayName());
        recipient.setRecipientEmail(contact.getContactValue());
        recipient.setStatus(result.status());
        recipient.setDeliveryStatusDetail(result.detail());
        recipient.setDeliveredAt(LocalDateTime.now());
        repository.createNotificationRecipient(recipient);

        OperationalRecommendationAudit audit = new OperationalRecommendationAudit();
        audit.setRecommendationId(rec.getId());
        audit.setActorUserId(actorUserId);
        audit.setEventType(result.sent() ? "TASK_EMAIL_SENT" : "TASK_EMAIL_FAILED");
        audit.setEventLabel((result.sent() ? "Task email sent to: " : "Task email failed for: ") + contact.getDisplayName());
        audit.setCreatedAt(LocalDateTime.now());
        repository.appendAudit(audit);
    }

    private OperationalEmailResult sendTaskEmail(
            OperationalRecommendation rec,
            OperationalTask task,
            HospitalOperationalContact contact) {
        if (!"EMAIL".equalsIgnoreCase(contact.getContactChannel()) || contact.getContactValue() == null) {
            return new OperationalEmailResult(false, "Contact has no email delivery channel.");
        }
        OperationalEmailMessage message = emailTemplateBuilder.taskAssignment(
                contact.getContactValue(),
                rec,
                task,
                contact.getDisplayName());
        return operationalEmailGateway.send(message);
    }
}

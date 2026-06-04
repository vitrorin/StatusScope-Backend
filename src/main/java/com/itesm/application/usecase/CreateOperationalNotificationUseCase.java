package com.itesm.application.usecase;

import com.itesm.application.port.out.OperationalEmailGateway;
import com.itesm.application.port.out.OperationalEmailMessage;
import com.itesm.application.port.out.OperationalEmailResult;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.ConflictException;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.HospitalDepartmentResource;
import com.itesm.domain.models.HospitalOperationalContact;
import com.itesm.domain.models.HospitalOperationalGroup;
import com.itesm.domain.models.OperationalNotification;
import com.itesm.domain.models.OperationalNotificationRecipient;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.repository.HospitalResourceRepository;
import com.itesm.domain.repository.OperationalDirectoryRepository;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class CreateOperationalNotificationUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalRecommendationRepository repository;
    @Inject OperationalDirectoryRepository operationalDirectoryRepository;
    @Inject HospitalResourceRepository hospitalResourceRepository;
    @Inject OperationalEmailGateway operationalEmailGateway;
    @Inject OperationalEmailTemplateBuilder emailTemplateBuilder;

    @Transactional
    public OperationalNotification execute(UUID recommendationId, OperationalNotification input) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();

        OperationalRecommendation rec = repository.findRecommendationById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found: " + recommendationId));

        if (hospitalId != null && !rec.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Recommendation not found: " + recommendationId);
        }

        Audience audience = resolveAudience(rec.getHospitalId(), input);
        List<OperationalNotificationRecipient> deliveries = sendEmails(rec, input, audience.contacts());

        input.setRecommendationId(recommendationId);
        input.setHospitalId(rec.getHospitalId());
        input.setSentByUserId(currentUser.getUserId());
        input.setSentAt(LocalDateTime.now());
        input.setSourceActionCode(input.getSourceActionCode() != null ? input.getSourceActionCode() : "NOTIFY_STAFF");
        input.setAudienceType(audience.type());
        input.setAudienceLabel(audience.label());
        input.setAudienceDepartmentCode(audience.departmentCode());
        input.setAudienceContactId(audience.contactId());
        input.setDeliveryChannel("EMAIL");
        input.setStatus(aggregateStatus(deliveries));
        input.setDeliveryStatusDetail(summaryDetail(deliveries, audience.contacts().size()));

        OperationalNotification created = repository.createNotification(input);
        for (OperationalNotificationRecipient delivery : deliveries) {
            delivery.setNotificationId(created.getId());
            repository.createNotificationRecipient(delivery);
        }
        created.setRecipients(repository.findNotificationRecipientsByNotificationId(created.getId()));

        OperationalRecommendationAudit audit = new OperationalRecommendationAudit();
        audit.setRecommendationId(recommendationId);
        audit.setActorUserId(currentUser.getUserId());
        audit.setEventType("FAILED".equals(input.getStatus()) ? "NOTIFICATION_EMAIL_FAILED" : "NOTIFICATION_SENT");
        audit.setEventLabel("Notified: " + input.getAudienceLabel() + " (" + input.getStatus() + ")");
        audit.setCreatedAt(LocalDateTime.now());
        repository.appendAudit(audit);

        return created;
    }

    private Audience resolveAudience(UUID hospitalId, OperationalNotification input) {
        String requestedType = input.getAudienceType() != null
                ? input.getAudienceType().trim().toUpperCase(Locale.ROOT)
                : (input.getAudienceDepartmentCode() != null ? "DEPARTMENT" : input.getAudienceGroupId() != null ? "GROUP" : "CONTACT");

        if ("DEPARTMENT".equals(requestedType)) {
            String code = cleanRequired(input.getAudienceDepartmentCode(), "audienceDepartmentCode").toUpperCase(Locale.ROOT);
            HospitalDepartmentResource department = hospitalResourceRepository.findDepartmentsByHospitalId(hospitalId).stream()
                    .filter(item -> code.equalsIgnoreCase(item.getDepartmentCode()))
                    .findFirst()
                    .orElseThrow(() -> new ConflictException("audienceDepartmentCode does not belong to this hospital"));
            List<HospitalOperationalContact> contacts = operationalDirectoryRepository.findActiveEmailContactsByDepartment(hospitalId, code, true);
            return new Audience("DEPARTMENT", department.getDepartmentName(), code, null, contacts);
        }

        if ("GROUP".equals(requestedType)) {
            UUID groupId = input.getAudienceGroupId();
            if (groupId == null) {
                throw new ConflictException("audienceGroupId is required");
            }
            HospitalOperationalGroup group = operationalDirectoryRepository.findGroupById(groupId)
                    .orElseThrow(() -> new NotFoundException("Operational group not found: " + groupId));
            if (!hospitalId.equals(group.getHospitalId())) {
                throw new NotFoundException("Operational group not found: " + groupId);
            }
            if (!group.isNotifiable()) {
                throw new ConflictException("Operational group is not notifiable");
            }
            return new Audience("GROUP", group.getGroupName(), group.getDepartmentCode(), null, List.of());
        }

        UUID contactId = input.getAudienceContactId();
        if (contactId == null) {
            throw new ConflictException("audienceContactId is required");
        }
        HospitalOperationalContact contact = operationalDirectoryRepository.findContactById(contactId)
                .orElseThrow(() -> new NotFoundException("Operational contact not found: " + contactId));
        validateContact(contact, hospitalId);
        return new Audience("CONTACT", contact.getDisplayName(), contact.getDepartmentCode(), contact.getId(), List.of(contact));
    }

    private void validateContact(HospitalOperationalContact contact, UUID hospitalId) {
        if (!hospitalId.equals(contact.getHospitalId())) {
            throw new NotFoundException("Operational contact not found: " + contact.getId());
        }
        if ("INACTIVE".equalsIgnoreCase(contact.getAvailabilityStatus())) {
            throw new ConflictException("Operational contact is inactive");
        }
        if (!contact.isNotifiable()) {
            throw new ConflictException("Operational contact is not notifiable");
        }
    }

    private List<OperationalNotificationRecipient> sendEmails(
            OperationalRecommendation rec,
            OperationalNotification input,
            List<HospitalOperationalContact> contacts) {
        List<OperationalNotificationRecipient> deliveries = new ArrayList<>();
        for (HospitalOperationalContact contact : contacts) {
            OperationalNotificationRecipient recipient = new OperationalNotificationRecipient();
            recipient.setContactId(contact.getId());
            recipient.setRecipientName(contact.getDisplayName());
            recipient.setRecipientEmail(contact.getContactValue());
            recipient.setDeliveredAt(LocalDateTime.now());
            OperationalEmailResult result = sendNotificationEmail(rec, input, contact);
            recipient.setStatus(result.status());
            recipient.setDeliveryStatusDetail(result.detail());
            deliveries.add(recipient);
        }
        return deliveries;
    }

    private OperationalEmailResult sendNotificationEmail(
            OperationalRecommendation rec,
            OperationalNotification input,
            HospitalOperationalContact contact) {
        if (!"EMAIL".equalsIgnoreCase(contact.getContactChannel()) || contact.getContactValue() == null) {
            return new OperationalEmailResult(false, "Contact has no email delivery channel.");
        }
        OperationalEmailMessage message = emailTemplateBuilder.staffNotification(
                contact.getContactValue(),
                rec,
                input.getMessage(),
                contact.getDisplayName(),
                input.getLanguage());
        return operationalEmailGateway.send(message);
    }

    private String aggregateStatus(List<OperationalNotificationRecipient> deliveries) {
        if (deliveries.isEmpty()) return "FAILED";
        long failed = deliveries.stream().filter(item -> "FAILED".equalsIgnoreCase(item.getStatus())).count();
        if (failed == 0) return "SENT";
        if (failed == deliveries.size()) return "FAILED";
        return "PARTIAL";
    }

    private String summaryDetail(List<OperationalNotificationRecipient> deliveries, int intendedRecipients) {
        long sent = deliveries.stream().filter(item -> "SENT".equalsIgnoreCase(item.getStatus())).count();
        long failed = deliveries.stream().filter(item -> "FAILED".equalsIgnoreCase(item.getStatus())).count();
        if (intendedRecipients == 0) return "No active notifiable email contacts were found for the selected audience.";
        return "Email delivery summary: " + sent + " sent, " + failed + " failed.";
    }

    private String cleanRequired(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ConflictException(field + " is required");
        }
        return value.trim();
    }

    private record Audience(
            String type,
            String label,
            String departmentCode,
            UUID contactId,
            List<HospitalOperationalContact> contacts) {}
}

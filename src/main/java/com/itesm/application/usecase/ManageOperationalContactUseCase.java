package com.itesm.application.usecase;

import com.itesm.application.dto.OperationalContactDto;
import com.itesm.application.dto.OperationalContactUpsertDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.ConflictException;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.HospitalOperationalContact;
import com.itesm.domain.repository.HospitalResourceRepository;
import com.itesm.domain.repository.OperationalDirectoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class ManageOperationalContactUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalDirectoryRepository repository;
    @Inject HospitalResourceRepository hospitalResourceRepository;

    @Transactional
    public OperationalContactDto create(OperationalContactUpsertDto input) {
        UUID hospitalId = requireHospitalId();
        HospitalOperationalContact contact = new HospitalOperationalContact();
        contact.setHospitalId(hospitalId);
        applyInput(contact, input, hospitalId);
        return toDto(repository.createContact(contact));
    }

    @Transactional
    public OperationalContactDto update(UUID contactId, OperationalContactUpsertDto input) {
        UUID hospitalId = requireHospitalId();
        HospitalOperationalContact contact = repository.findContactById(contactId)
                .orElseThrow(() -> new NotFoundException("Operational contact not found: " + contactId));
        ensureSameHospital(contact, hospitalId, contactId);
        applyInput(contact, input, hospitalId);
        return toDto(repository.updateContact(contact));
    }

    @Transactional
    public OperationalContactDto updateStatus(UUID contactId, String status) {
        UUID hospitalId = requireHospitalId();
        HospitalOperationalContact contact = repository.findContactById(contactId)
                .orElseThrow(() -> new NotFoundException("Operational contact not found: " + contactId));
        ensureSameHospital(contact, hospitalId, contactId);
        contact.setAvailabilityStatus(normalizeStatus(status));
        return toDto(repository.updateContact(contact));
    }

    private UUID requireHospitalId() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new NotFoundException("User has no assigned hospital");
        }
        return hospitalId;
    }

    private void ensureSameHospital(HospitalOperationalContact contact, UUID hospitalId, UUID contactId) {
        if (!hospitalId.equals(contact.getHospitalId())) {
            throw new NotFoundException("Operational contact not found: " + contactId);
        }
    }

    private void applyInput(HospitalOperationalContact contact, OperationalContactUpsertDto input, UUID hospitalId) {
        contact.setDisplayName(cleanRequired(input.getDisplayName(), "displayName"));
        contact.setRoleLabel(cleanRequired(input.getRoleLabel(), "roleLabel"));
        String departmentCode = cleanRequired(input.getDepartmentCode(), "departmentCode").toUpperCase();
        boolean departmentExists = hospitalResourceRepository.findDepartmentsByHospitalId(hospitalId).stream()
                .anyMatch(department -> departmentCode.equalsIgnoreCase(department.getDepartmentCode()));
        if (!departmentExists) {
            throw new ConflictException("departmentCode does not belong to this hospital");
        }
        contact.setDepartmentCode(departmentCode);
        contact.setContactChannel("EMAIL");
        contact.setContactValue(cleanRequired(input.getEmail(), "email").toLowerCase());
        contact.setAssignable(input.isAssignable());
        contact.setNotifiable(input.isNotifiable());
        contact.setAvailabilityStatus(normalizeStatus(input.getAvailabilityStatus()));
    }

    private String cleanRequired(String value, String field) {
        String cleaned = cleanOptional(value);
        if (cleaned == null) {
            throw new ConflictException(field + " is required");
        }
        return cleaned;
    }

    private String cleanOptional(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String normalizeStatus(String value) {
        String normalized = value == null ? "ACTIVE" : value.trim().toUpperCase();
        if ("INACTIVE".equals(normalized) || "ACTIVE".equals(normalized)) {
            return normalized;
        }
        return "ACTIVE";
    }

    private OperationalContactDto toDto(HospitalOperationalContact contact) {
        OperationalContactDto dto = new OperationalContactDto();
        dto.setId(contact.getId().toString());
        if (contact.getUserId() != null) dto.setUserId(contact.getUserId().toString());
        dto.setDisplayName(contact.getDisplayName());
        dto.setRoleLabel(contact.getRoleLabel());
        dto.setDepartmentCode(contact.getDepartmentCode());
        dto.setContactChannel(contact.getContactChannel());
        dto.setContactValue(contact.getContactValue());
        dto.setAvailabilityStatus(contact.getAvailabilityStatus());
        dto.setAssignable(contact.isAssignable());
        dto.setNotifiable(contact.isNotifiable());
        dto.setUpdatedAt(contact.getUpdatedAt());
        return dto;
    }
}

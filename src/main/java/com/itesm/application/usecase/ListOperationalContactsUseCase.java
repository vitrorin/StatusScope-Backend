package com.itesm.application.usecase;

import com.itesm.application.dto.OperationalContactDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.repository.OperationalDirectoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ListOperationalContactsUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalDirectoryRepository repository;

    public List<OperationalContactDto> execute(Boolean assignable, Boolean notifiable, String departmentCode) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new NotFoundException("User has no assigned hospital");
        }

        return repository.findContactsByHospitalId(hospitalId, assignable, notifiable, departmentCode)
                .stream()
                .map(contact -> {
                    OperationalContactDto dto = new OperationalContactDto();
                    dto.setId(contact.getId().toString());
                    if (contact.getUserId() != null) {
                        dto.setUserId(contact.getUserId().toString());
                    }
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
                })
                .collect(Collectors.toList());
    }
}

package com.itesm.application.usecase;

import com.itesm.application.dto.OperationalGroupDto;
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
public class ListOperationalGroupsUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalDirectoryRepository repository;

    public List<OperationalGroupDto> execute(Boolean assignable, Boolean notifiable, String departmentCode) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new NotFoundException("User has no assigned hospital");
        }

        return repository.findGroupsByHospitalId(hospitalId, assignable, notifiable, departmentCode)
                .stream()
                .map(group -> {
                    OperationalGroupDto dto = new OperationalGroupDto();
                    dto.setId(group.getId().toString());
                    dto.setGroupCode(group.getGroupCode());
                    dto.setGroupName(group.getGroupName());
                    dto.setGroupType(group.getGroupType());
                    dto.setAssignable(group.isAssignable());
                    dto.setNotifiable(group.isNotifiable());
                    dto.setMemberCount(group.getMemberCount());
                    dto.setUpdatedAt(group.getUpdatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}

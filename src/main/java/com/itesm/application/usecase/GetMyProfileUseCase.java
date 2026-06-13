package com.itesm.application.usecase;

import com.itesm.application.dto.UserSummaryDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.domain.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;

import java.time.LocalDateTime;

@ApplicationScoped
public class GetMyProfileUseCase {

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    UserRepository userRepository;

    public UserSummaryDto execute() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        if (currentUser == null) {
            throw new NotAuthorizedException("Unauthorized");
        }

        if (shouldTrackLogin(currentUser)) {
            userRepository.updateLastLoginAt(currentUser.getUserId(), LocalDateTime.now());
        }

        UserSummaryDto profile = new UserSummaryDto();
        profile.setId(currentUser.getUserId());
        profile.setEmail(currentUser.getEmail());
        profile.setFullName(currentUser.getFullName());
        profile.setHospitalId(currentUser.getHospitalId());
        profile.setRoles(currentUser.getRoles());
        profile.setPrivileges(currentUser.getPrivileges());
        return profile;
    }

    private boolean shouldTrackLogin(CurrentUser currentUser) {
        if (currentUser.isSystemAdmin()) {
            return false;
        }
        return currentUser.hasRole("HOSPITAL_ADMIN") || currentUser.hasRole("DOCTOR");
    }
}


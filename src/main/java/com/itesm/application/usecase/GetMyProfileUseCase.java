package com.itesm.application.usecase;

import com.itesm.application.dto.UserSummaryDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;

@ApplicationScoped
public class GetMyProfileUseCase {

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    public UserSummaryDto execute() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        if (currentUser == null) {
            throw new NotAuthorizedException("Unauthorized");
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
}


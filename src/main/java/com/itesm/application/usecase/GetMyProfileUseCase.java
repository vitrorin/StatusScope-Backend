package com.itesm.application.usecase;

import com.itesm.application.dto.MyProfileDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;

@ApplicationScoped
public class GetMyProfileUseCase {

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    public MyProfileDto execute() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        if (currentUser == null) {
            throw new NotAuthorizedException("Unauthorized");
        }

        MyProfileDto profile = new MyProfileDto();
        profile.setId(currentUser.getUserId());
        profile.setEmail(currentUser.getEmail());
        profile.setFullName(currentUser.getFullName());
        profile.setRoles(currentUser.getRoles());
        profile.setPrivileges(currentUser.getPrivileges());
        return profile;
    }
}

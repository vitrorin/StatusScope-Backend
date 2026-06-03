package com.itesm.application.usecase;

import com.google.firebase.auth.FirebaseAuthException;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.AuthorizationService;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.UserRepository;
import com.itesm.infrastructure.firebase.FirebaseUserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class UpdateUserStatusByAdminUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateUserStatusByAdminUseCase.class);

    @Inject UserRepository userRepository;
    @Inject FirebaseUserService firebaseUserService;
    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject AuthorizationService authorizationService;

    @RequiresPrivilege("users.manage")
    public User execute(UUID userId, String status) {
        CurrentUser caller = authenticatedUserContext.getCurrentUser();
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!caller.isSystemAdmin()) {
            authorizationService.assertSameHospital(user.getHospitalId());
        }

        UserStatus nextStatus = status == null || status.isBlank()
                ? UserStatus.DISABLED
                : UserStatus.valueOf(status);
        user.setStatus(nextStatus);
        user.setActive(nextStatus == UserStatus.ACTIVE);
        User updated = userRepository.update(user);

        try {
            if (nextStatus == UserStatus.ACTIVE) {
                firebaseUserService.enableUser(user.getExternalAuthId());
            } else {
                firebaseUserService.disableUser(user.getExternalAuthId());
            }
        } catch (FirebaseAuthException e) {
            LOG.warnf("Could not update Firebase user %s status to %s: %s", user.getExternalAuthId(), nextStatus, e.getMessage());
        }
        return updated;
    }
}

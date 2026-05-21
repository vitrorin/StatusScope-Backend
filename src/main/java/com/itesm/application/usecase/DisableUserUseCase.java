package com.itesm.application.usecase;

import com.google.firebase.auth.FirebaseAuthException;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.AuthorizationService;
import com.itesm.application.security.CurrentUser;
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
public class DisableUserUseCase {

    private static final Logger LOG = Logger.getLogger(DisableUserUseCase.class);

    @Inject
    UserRepository userRepository;

    @Inject
    FirebaseUserService firebaseUserService;

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    AuthorizationService authorizationService;

    @RequiresPrivilege("users.manage")
    public void execute(UUID userId) {
        CurrentUser caller = authenticatedUserContext.getCurrentUser();
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!caller.isSystemAdmin()) {
            authorizationService.assertSameHospital(user.getHospitalId());
        }

        user.setStatus(UserStatus.DISABLED);
        user.setActive(false);
        userRepository.update(user);

        try {
            firebaseUserService.disableUser(user.getExternalAuthId());
        } catch (FirebaseAuthException e) {
            LOG.warnf("Could not disable Firebase user %s: %s", user.getExternalAuthId(), e.getMessage());
        }
    }
}

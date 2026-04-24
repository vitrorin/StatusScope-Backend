package com.itesm.infrastructure.bootstrap;

import com.google.firebase.auth.FirebaseAuthException;
import com.itesm.domain.models.Role;
import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import com.itesm.infrastructure.firebase.FirebaseUserService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class DevSeeder {

    private static final Logger LOG = Logger.getLogger(DevSeeder.class);

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    HospitalRepository hospitalRepository;

    @Inject
    FirebaseUserService firebaseUserService;

    @ConfigProperty(name = "quarkus.profile")
    String profile;

    void onStart(@Observes StartupEvent ev) {
        if (!"dev".equals(profile)) return;
        if (userRepository.findByEmail("admin@statusscope.local").isPresent()) return; // idempotent

        seedUser("admin@statusscope.local",       "System Admin",    null,
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
        seedUser("admin.hgz21@statusscope.local", "Admin HGZ-21",
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"));
        seedUser("admin.hre05@statusscope.local", "Admin HRE-05",
                UUID.fromString("30000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"));
        seedUser("doctor1@statusscope.local",     "Dra. Ana López",
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"));
        seedUser("doctor2@statusscope.local",     "Dr. Luis Pérez",
                UUID.fromString("30000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"));
    }

    private void seedUser(String email, String fullName, UUID hospitalId, UUID roleId) {
        String uid;
        try {
            uid = firebaseUserService.createUser(email, "Password123!", fullName);
        } catch (FirebaseAuthException e) {
            // Idempotent restart: Firebase user might already exist
            try {
                var record = firebaseUserService.getUserByEmail(email);
                if (record != null) {
                    uid = record.getUid();
                } else {
                    LOG.errorf("DevSeeder: could not create or find Firebase user for %s: %s", email, e.getMessage());
                    return;
                }
            } catch (FirebaseAuthException ex) {
                LOG.errorf("DevSeeder: error looking up Firebase user %s: %s", email, ex.getMessage());
                return;
            }
        }

        Role role = new Role();
        role.setId(roleId);

        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setFullName(fullName);
        u.setHospitalId(hospitalId);
        u.setExternalAuthId(uid);
        u.setStatus(UserStatus.ACTIVE);
        u.setActive(true);
        u.setRoles(Set.of(role));
        userRepository.create(u);
        LOG.infof("DevSeeder: seeded user %s", email);
    }
}

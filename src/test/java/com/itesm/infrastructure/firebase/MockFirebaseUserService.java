package com.itesm.infrastructure.firebase;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.UUID;

@ApplicationScoped
@Alternative
@Priority(1)
@IfBuildProfile("test")
public class MockFirebaseUserService extends FirebaseUserService {

    @Override
    public String createUser(String email, String password, String fullName) throws FirebaseAuthException {
        return "mock-uid-" + UUID.randomUUID();
    }

    @Override
    public void deleteUser(String uid) throws FirebaseAuthException {
        // no-op in tests
    }

    @Override
    public void disableUser(String uid) throws FirebaseAuthException {
        // no-op in tests
    }

    @Override
    public void enableUser(String uid) throws FirebaseAuthException {
        // no-op in tests
    }

    @Override
    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        return null;
    }
}

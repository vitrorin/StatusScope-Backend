package com.itesm.infrastructure.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@UnlessBuildProfile("test")
public class FirebaseUserService {

    public String createUser(String email, String password, String fullName) throws FirebaseAuthException {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(fullName)
                .setEmailVerified(false);
        UserRecord record = FirebaseAuth.getInstance().createUser(request);
        return record.getUid();
    }

    public void deleteUser(String uid) throws FirebaseAuthException {
        FirebaseAuth.getInstance().deleteUser(uid);
    }

    public void disableUser(String uid) throws FirebaseAuthException {
        FirebaseAuth.getInstance().updateUser(
                new UserRecord.UpdateRequest(uid).setDisabled(true));
    }

    public void enableUser(String uid) throws FirebaseAuthException {
        FirebaseAuth.getInstance().updateUser(
                new UserRecord.UpdateRequest(uid).setDisabled(false));
    }

    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().getUserByEmail(email);
    }
}

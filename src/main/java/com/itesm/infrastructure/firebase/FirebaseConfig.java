package com.itesm.infrastructure.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Startup
@ApplicationScoped
@UnlessBuildProfile("test")
public class FirebaseConfig {

    @ConfigProperty(name = "firebase.service-account-location")
    String path;

    @PostConstruct
    void init() {
        ensureInitialized();
    }

    public synchronized void ensureInitialized() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount;
                if (new File(path).exists()) {
                    serviceAccount = new FileInputStream(path);
                } else {
                    String resourceName = path.contains("/")
                            ? path.substring(path.lastIndexOf('/') + 1)
                            : path;
                    serviceAccount = getClass().getClassLoader().getResourceAsStream(resourceName);
                    if (serviceAccount == null) {
                        throw new IllegalStateException("Failed to initialize Firebase: " + path + " (No such file or directory)");
                    }
                }
                try (serviceAccount) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                    FirebaseApp.initializeApp(options);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Firebase: " + e.getMessage(), e);
        }
    }
}

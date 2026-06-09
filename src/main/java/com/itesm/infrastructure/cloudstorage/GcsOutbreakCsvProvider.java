package com.itesm.infrastructure.cloudstorage;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@ApplicationScoped
public class GcsOutbreakCsvProvider {

    private static final String MUNICIPAL_CSV_NAME = "municipal_outbreaks.csv";
    private static final String STATE_CSV_NAME = "state_outbreaks.csv";

    @ConfigProperty(name = "outbreak.import.gcs.bucket")
    Optional<String> gcsBucket;

    @ConfigProperty(name = "outbreak.import.gcs.enabled", defaultValue = "false")
    boolean gcsEnabled;

    public boolean isGcsEnabled() {
        return gcsEnabled && gcsBucket.isPresent() && !gcsBucket.get().isBlank();
    }

    public InputStream openMunicipalOutbreaksCsv() throws IOException {
        return openGcsObject(MUNICIPAL_CSV_NAME);
    }

    public InputStream openStateOutbreaksCsv() throws IOException {
        return openGcsObject(STATE_CSV_NAME);
    }

    InputStream openGcsObject(String objectName) throws IOException {
        String url = buildGcsUrl(objectName);
        Log.infof("Downloading outbreak CSV from GCS: %s", url);
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new IOException("GCS object not found: " + url);
        }
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readErrorBody(connection);
            throw new IOException("GCS download failed (" + responseCode + ") for " + url + ": " + errorBody);
        }

        return connection.getInputStream();
    }

    String buildGcsUrl(String objectName) {
        String bucket = gcsBucket.orElse("").replaceAll("^gs://", "").replaceAll("/+$", "");
        return "https://storage.googleapis.com/" + bucket + "/outbreaks/" + objectName;
    }

    private String readErrorBody(HttpURLConnection connection) {
        try (InputStream errorStream = connection.getErrorStream()) {
            if (errorStream == null) {
                return "";
            }
            return new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
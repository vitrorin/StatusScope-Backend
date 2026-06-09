package com.itesm.interfaces.rest;

import com.itesm.infrastructure.bootstrap.OutbreakCsvImporter;
import com.itesm.infrastructure.cloudstorage.GcsOutbreakCsvProvider;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

@Path("/internal/outbreaks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InternalOutbreakImportResource {

    @Inject
    OutbreakCsvImporter importer;

    @Inject
    GcsOutbreakCsvProvider gcsProvider;

    @ConfigProperty(name = "outbreak.import.job-token")
    Optional<String> jobToken;

    @POST
    @Path("/import")
    @Transactional
    public Response importOutbreaks(@HeaderParam("X-Internal-Job-Token") String token) {
        if (!isAuthenticated(token)) {
            Log.warn("Outbreak import rejected: invalid or missing job token");
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing job token"))
                    .build();
        }

        Log.info("Starting outbreak import from " + (gcsProvider.isGcsEnabled() ? "GCS" : "classpath"));

        try {
            OutbreakCsvImporter.ImportSummary municipalSummary;
            OutbreakCsvImporter.ImportSummary stateSummary;

            if (gcsProvider.isGcsEnabled()) {
                municipalSummary = importMunicipalFromGcs();
                stateSummary = importStateFromGcs();
            } else {
                municipalSummary = importer.importMunicipalOutbreaks();
                stateSummary = importer.importStateOutbreaks();
            }

            Log.infof("Outbreak import completed: municipal created=%d updated=%d unchanged=%d ended=%d activeRows=%d",
                    municipalSummary.created(), municipalSummary.updated(), municipalSummary.unchanged(),
                    municipalSummary.ended(), municipalSummary.activeRows());
            Log.infof("Outbreak import completed: state created=%d updated=%d unchanged=%d ended=%d activeRows=%d",
                    stateSummary.created(), stateSummary.updated(), stateSummary.unchanged(),
                    stateSummary.ended(), stateSummary.activeRows());

            OutbreakImportResultDto.OutbreakImportSummaryDto municipalDto =
                    new OutbreakImportResultDto.OutbreakImportSummaryDto(
                            municipalSummary.created(), municipalSummary.updated(),
                            municipalSummary.unchanged(), municipalSummary.ended(),
                            municipalSummary.activeRows());
            OutbreakImportResultDto.OutbreakImportSummaryDto stateDto =
                    new OutbreakImportResultDto.OutbreakImportSummaryDto(
                            stateSummary.created(), stateSummary.updated(),
                            stateSummary.unchanged(), stateSummary.ended(),
                            stateSummary.activeRows());

            OutbreakImportResultDto result = OutbreakImportResultDto.success("Outbreak import completed", municipalDto, stateDto);
            return Response.ok(result).build();
        } catch (Exception e) {
            Log.errorf(e, "Outbreak import failed: %s", e.getMessage());
            return Response.serverError()
                    .entity(OutbreakImportResultDto.error("Import failed: " + e.getMessage()))
                    .build();
        }
    }

    private OutbreakCsvImporter.ImportSummary importMunicipalFromGcs() throws Exception {
        try (InputStream csv = gcsProvider.openMunicipalOutbreaksCsv()) {
            return importer.importMunicipalOutbreaks(csv);
        }
    }

    private OutbreakCsvImporter.ImportSummary importStateFromGcs() throws Exception {
        try (InputStream csv = gcsProvider.openStateOutbreaksCsv()) {
            return importer.importStateOutbreaks(csv);
        }
    }

    private boolean isAuthenticated(String token) {
        if (jobToken.isEmpty() || jobToken.get().isBlank()) {
            Log.warn("Outbreak import job token not configured; rejecting all import requests");
            return false;
        }
        return jobToken.get().equals(token);
    }
}
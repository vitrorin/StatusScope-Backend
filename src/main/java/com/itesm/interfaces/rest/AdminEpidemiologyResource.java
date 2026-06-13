package com.itesm.interfaces.rest;

import com.itesm.application.dto.DoctorDashboardSummaryDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardAlertDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardDiseaseDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardMetricDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardZoneDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.GetDoctorDashboardSummaryUseCase;
import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Path("/admin/epidemiology")
@Produces(MediaType.APPLICATION_JSON)
public class AdminEpidemiologyResource {

    @Inject
    GetDoctorDashboardSummaryUseCase getDoctorDashboardSummaryUseCase;

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    EntityManager entityManager;

    @GET
    @Path("/summary")
    @RequiresPrivilege("admin.operations")
    public Response summary(@QueryParam("radiusKm") Double radiusKm) {
        assertHospitalAdmin();
        return Response.ok(getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm))).build();
    }

    @GET
    @Path("/metrics")
    @RequiresPrivilege("admin.operations")
    public Response metrics(@QueryParam("radiusKm") Double radiusKm) {
        assertHospitalAdmin();
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new MetricsResponse(summary.getMetrics(), summary.getHospitalName())).build();
    }

    @GET
    @Path("/map")
    @RequiresPrivilege("admin.operations")
    public Response map(@QueryParam("radiusKm") Double radiusKm) {
        assertHospitalAdmin();
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new MapResponse(
                summary.getZones(),
                summary.getDiseaseBreakdown(),
                summary.getGeneratedAt(),
                summary.getRadiusKm())).build();
    }

    @GET
    @Path("/map/states")
    @RequiresPrivilege("admin.operations")
    public Response stateMap() {
        assertHospitalAdmin();
        return Response.ok(new StateMapResponse(getDoctorDashboardSummaryUseCase.listStateMap())).build();
    }

    @GET
    @Path("/diseases")
    @RequiresPrivilege("admin.operations")
    public Response diseases() {
        assertHospitalAdmin();
        List<DiseaseCatalogItem> diseases = entityManager
                .createQuery("select d from DiseaseEntity d order by d.name asc", DiseaseEntity.class)
                .getResultList()
                .stream()
                .map(disease -> new DiseaseCatalogItem(
                        disease.getId(),
                        disease.getCode(),
                        disease.getName()))
                .toList();
        return Response.ok(new DiseaseCatalogResponse(diseases)).build();
    }

    @GET
    @Path("/map/states/{stateId}/outbreaks")
    @RequiresPrivilege("admin.operations")
    public Response stateOutbreakMap(@PathParam("stateId") UUID stateId) {
        assertHospitalAdmin();
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.stateMap(stateId);
        return Response.ok(new MapResponse(
                summary.getZones(),
                summary.getDiseaseBreakdown(),
                summary.getGeneratedAt(),
                0)).build();
    }

    @GET
    @Path("/alerts")
    @RequiresPrivilege("admin.operations")
    public Response alerts(@QueryParam("radiusKm") Double radiusKm) {
        assertHospitalAdmin();
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new AlertsResponse(summary.getAlerts())).build();
    }

    @GET
    @Path("/disease-breakdown/local")
    @RequiresPrivilege("admin.operations")
    public Response localDiseaseBreakdown(@QueryParam("radiusKm") Double radiusKm) {
        assertHospitalAdmin();
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new DiseaseBreakdownResponse(
                summary.getDiseaseBreakdown(),
                summary.getStateName(),
                summary.getMunicipalityName())).build();
    }

    @GET
    @Path("/disease-breakdown/state")
    @RequiresPrivilege("admin.operations")
    public Response stateDiseaseBreakdown(@QueryParam("radiusKm") Double radiusKm) {
        assertHospitalAdmin();
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new DiseaseBreakdownResponse(
                summary.getStateDiseaseBreakdown(),
                summary.getStateName(),
                summary.getMunicipalityName())).build();
    }

    @GET
    @Path("/reports/{scope}")
    @RequiresPrivilege("admin.operations")
    public Response report(@PathParam("scope") String scope, @QueryParam("radiusKm") Double radiusKm) {
        assertHospitalAdmin();
        return Response.ok(getDoctorDashboardSummaryUseCase.report(scope, normalizeRadius(radiusKm))).build();
    }

    @GET
    @Path("/reports/states/{stateId}")
    @RequiresPrivilege("admin.operations")
    public Response stateReport(@PathParam("stateId") UUID stateId) {
        assertHospitalAdmin();
        return Response.ok(getDoctorDashboardSummaryUseCase.stateReport(stateId)).build();
    }

    private void assertHospitalAdmin() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        if (!currentUser.hasRole("HOSPITAL_ADMIN") || currentUser.getHospitalId() == null) {
            throw new ForbiddenException("Hospital administrator access is required");
        }
    }

    private Double normalizeRadius(Double radiusKm) {
        if (radiusKm == null) return null;
        if (radiusKm <= 0) return null;
        return radiusKm;
    }

    public record MetricsResponse(List<DoctorDashboardMetricDto> metrics, String hospitalName) {}

    public record MapResponse(
            List<DoctorDashboardZoneDto> zones,
            List<DoctorDashboardDiseaseDto> diseaseBreakdown,
            LocalDateTime generatedAt,
            double radiusKm
    ) {}

    public record AlertsResponse(List<DoctorDashboardAlertDto> alerts) {}

    public record DiseaseBreakdownResponse(List<DoctorDashboardDiseaseDto> diseaseBreakdown, String stateName, String municipalityName) {}

    public record StateMapResponse(List<GetDoctorDashboardSummaryUseCase.DoctorDashboardStateMapDto> states) {}

    public record DiseaseCatalogItem(UUID id, String code, String name) {}

    public record DiseaseCatalogResponse(List<DiseaseCatalogItem> diseases) {}
}

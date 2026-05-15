package com.itesm.interfaces.rest;

import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.dto.DoctorDashboardSummaryDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardAlertDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardDiseaseDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardMetricDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardZoneDto;
import com.itesm.application.usecase.GetDoctorDashboardSummaryUseCase;
import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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

@Path("/doctor/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DoctorDashboardResource {

    @Inject
    GetDoctorDashboardSummaryUseCase getDoctorDashboardSummaryUseCase;

    @Inject
    EntityManager entityManager;

    @GET
    @Path("/summary")
    @RequiresPrivilege("outbreaks.read")
    public Response summary(@QueryParam("radiusKm") Double radiusKm) {
        return Response.ok(getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm))).build();
    }

    @GET
    @Path("/metrics")
    @RequiresPrivilege("outbreaks.read")
    public Response metrics(@QueryParam("radiusKm") Double radiusKm) {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new MetricsResponse(summary.getMetrics(), summary.getHospitalName())).build();
    }

    @GET
    @Path("/map")
    @RequiresPrivilege("outbreaks.read")
    public Response map(@QueryParam("radiusKm") Double radiusKm) {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new MapResponse(
                summary.getZones(),
                summary.getDiseaseBreakdown(),
                summary.getGeneratedAt(),
                summary.getRadiusKm())).build();
    }

    @GET
    @Path("/map/states")
    @RequiresPrivilege("outbreaks.read")
    public Response stateMap() {
        return Response.ok(new StateMapResponse(getDoctorDashboardSummaryUseCase.listStateMap())).build();
    }

    @GET
    @Path("/diseases")
    @RequiresPrivilege("outbreaks.read")
    public Response diseases() {
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
    @RequiresPrivilege("outbreaks.read")
    public Response stateOutbreakMap(@PathParam("stateId") UUID stateId) {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.stateMap(stateId);
        return Response.ok(new MapResponse(
                summary.getZones(),
                summary.getDiseaseBreakdown(),
                summary.getGeneratedAt(),
                0)).build();
    }

    @GET
    @Path("/alerts")
    @RequiresPrivilege("outbreaks.read")
    public Response alerts(@QueryParam("radiusKm") Double radiusKm) {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new AlertsResponse(summary.getAlerts())).build();
    }

    @GET
    @Path("/disease-breakdown/local")
    @RequiresPrivilege("outbreaks.read")
    public Response localDiseaseBreakdown(@QueryParam("radiusKm") Double radiusKm) {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new DiseaseBreakdownResponse(
                summary.getDiseaseBreakdown(),
                summary.getStateName(),
                summary.getMunicipalityName())).build();
    }

    @GET
    @Path("/disease-breakdown/state")
    @RequiresPrivilege("outbreaks.read")
    public Response stateDiseaseBreakdown(@QueryParam("radiusKm") Double radiusKm) {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new DiseaseBreakdownResponse(
                summary.getStateDiseaseBreakdown(),
                summary.getStateName(),
                summary.getMunicipalityName())).build();
    }

    @GET
    @Path("/reports/{scope}")
    @RequiresPrivilege("outbreaks.read")
    public Response report(@PathParam("scope") String scope, @QueryParam("radiusKm") Double radiusKm) {
        return Response.ok(getDoctorDashboardSummaryUseCase.report(scope, normalizeRadius(radiusKm))).build();
    }

    @GET
    @Path("/reports/states/{stateId}")
    @RequiresPrivilege("outbreaks.read")
    public Response stateReport(@PathParam("stateId") UUID stateId) {
        return Response.ok(getDoctorDashboardSummaryUseCase.stateReport(stateId)).build();
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

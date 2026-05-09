package com.itesm.interfaces.rest;

import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.dto.DoctorDashboardSummaryDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardAlertDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardDiseaseDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardMetricDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardZoneDto;
import com.itesm.application.usecase.GetDoctorDashboardSummaryUseCase;
import jakarta.inject.Inject;
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

    @GET
    @Path("/summary")
    @RequiresPrivilege("diagnosis.assist")
    public Response summary(@QueryParam("radiusKm") Double radiusKm) {
        return Response.ok(getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm))).build();
    }

    @GET
    @Path("/metrics")
    @RequiresPrivilege("diagnosis.assist")
    public Response metrics(@QueryParam("radiusKm") Double radiusKm) {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new MetricsResponse(summary.getMetrics(), summary.getHospitalName())).build();
    }

    @GET
    @Path("/map")
    @RequiresPrivilege("diagnosis.assist")
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
    @RequiresPrivilege("diagnosis.assist")
    public Response stateMap() {
        return Response.ok(new StateMapResponse(getDoctorDashboardSummaryUseCase.listStateMap())).build();
    }

    @GET
    @Path("/map/states/{stateId}/outbreaks")
    @RequiresPrivilege("diagnosis.assist")
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
    @RequiresPrivilege("diagnosis.assist")
    public Response alerts(@QueryParam("radiusKm") Double radiusKm) {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new AlertsResponse(summary.getAlerts())).build();
    }

    @GET
    @Path("/disease-breakdown/local")
    @RequiresPrivilege("diagnosis.assist")
    public Response localDiseaseBreakdown(@QueryParam("radiusKm") Double radiusKm) {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new DiseaseBreakdownResponse(
                summary.getDiseaseBreakdown(),
                summary.getStateName(),
                summary.getMunicipalityName())).build();
    }

    @GET
    @Path("/disease-breakdown/state")
    @RequiresPrivilege("diagnosis.assist")
    public Response stateDiseaseBreakdown(@QueryParam("radiusKm") Double radiusKm) {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute(normalizeRadius(radiusKm));
        return Response.ok(new DiseaseBreakdownResponse(
                summary.getStateDiseaseBreakdown(),
                summary.getStateName(),
                summary.getMunicipalityName())).build();
    }

    @GET
    @Path("/reports/{scope}")
    @RequiresPrivilege("diagnosis.assist")
    public Response report(@PathParam("scope") String scope, @QueryParam("radiusKm") Double radiusKm) {
        return Response.ok(getDoctorDashboardSummaryUseCase.report(scope, normalizeRadius(radiusKm))).build();
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
}

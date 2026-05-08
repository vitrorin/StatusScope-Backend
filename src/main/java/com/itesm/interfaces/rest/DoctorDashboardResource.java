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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;

@Path("/doctor/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DoctorDashboardResource {

    @Inject
    GetDoctorDashboardSummaryUseCase getDoctorDashboardSummaryUseCase;

    @GET
    @Path("/summary")
    @RequiresPrivilege("diagnosis.assist")
    public Response summary() {
        return Response.ok(getDoctorDashboardSummaryUseCase.execute()).build();
    }

    @GET
    @Path("/metrics")
    @RequiresPrivilege("diagnosis.assist")
    public Response metrics() {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute();
        return Response.ok(new MetricsResponse(summary.getMetrics(), summary.getHospitalName())).build();
    }

    @GET
    @Path("/map")
    @RequiresPrivilege("diagnosis.assist")
    public Response map() {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute();
        return Response.ok(new MapResponse(
                summary.getZones(),
                summary.getDiseaseBreakdown(),
                summary.getGeneratedAt())).build();
    }

    @GET
    @Path("/alerts")
    @RequiresPrivilege("diagnosis.assist")
    public Response alerts() {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute();
        return Response.ok(new AlertsResponse(summary.getAlerts())).build();
    }

    @GET
    @Path("/disease-breakdown/local")
    @RequiresPrivilege("diagnosis.assist")
    public Response localDiseaseBreakdown() {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute();
        return Response.ok(new DiseaseBreakdownResponse(summary.getDiseaseBreakdown(), summary.getStateName())).build();
    }

    @GET
    @Path("/disease-breakdown/state")
    @RequiresPrivilege("diagnosis.assist")
    public Response stateDiseaseBreakdown() {
        DoctorDashboardSummaryDto summary = getDoctorDashboardSummaryUseCase.execute();
        return Response.ok(new DiseaseBreakdownResponse(summary.getStateDiseaseBreakdown(), summary.getStateName())).build();
    }

    public record MetricsResponse(List<DoctorDashboardMetricDto> metrics, String hospitalName) {}

    public record MapResponse(
            List<DoctorDashboardZoneDto> zones,
            List<DoctorDashboardDiseaseDto> diseaseBreakdown,
            LocalDateTime generatedAt
    ) {}

    public record AlertsResponse(List<DoctorDashboardAlertDto> alerts) {}

    public record DiseaseBreakdownResponse(List<DoctorDashboardDiseaseDto> diseaseBreakdown, String stateName) {}
}

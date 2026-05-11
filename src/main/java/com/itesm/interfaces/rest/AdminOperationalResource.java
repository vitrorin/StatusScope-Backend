package com.itesm.interfaces.rest;

import com.itesm.application.dto.HospitalResourcesDto;
import com.itesm.application.dto.OperationalRecommendationDto;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.CreateOperationalNotificationUseCase;
import com.itesm.application.usecase.CreateOperationalTaskUseCase;
import com.itesm.application.usecase.CreateSupplyRequestUseCase;
import com.itesm.application.usecase.GetAdminDashboardSummaryUseCase;
import com.itesm.application.usecase.GetHospitalResourcesUseCase;
import com.itesm.application.usecase.GetOperationalRecommendationDetailUseCase;
import com.itesm.application.usecase.ListOperationalRecommendationsUseCase;
import com.itesm.application.usecase.RefreshOperationalRecommendationsUseCase;
import com.itesm.application.usecase.UpdateHospitalResourcesUseCase;
import com.itesm.application.usecase.UpdateOperationalRecommendationStatusUseCase;
import com.itesm.domain.models.HospitalDepartmentResource;
import com.itesm.domain.models.HospitalInventoryItem;
import com.itesm.domain.models.HospitalResourceSnapshot;
import com.itesm.domain.models.HospitalStaffingProfile;
import com.itesm.domain.models.OperationalNotification;
import com.itesm.domain.models.OperationalTask;
import com.itesm.domain.models.SupplyRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminOperationalResource {

    @Inject GetAdminDashboardSummaryUseCase getAdminDashboardSummaryUseCase;
    @Inject ListOperationalRecommendationsUseCase listOperationalRecommendationsUseCase;
    @Inject GetOperationalRecommendationDetailUseCase getOperationalRecommendationDetailUseCase;
    @Inject RefreshOperationalRecommendationsUseCase refreshOperationalRecommendationsUseCase;
    @Inject UpdateOperationalRecommendationStatusUseCase updateOperationalRecommendationStatusUseCase;
    @Inject CreateOperationalTaskUseCase createOperationalTaskUseCase;
    @Inject CreateOperationalNotificationUseCase createOperationalNotificationUseCase;
    @Inject CreateSupplyRequestUseCase createSupplyRequestUseCase;
    @Inject GetHospitalResourcesUseCase getHospitalResourcesUseCase;
    @Inject UpdateHospitalResourcesUseCase updateHospitalResourcesUseCase;

    // ----------------------------------------------------------------
    // Dashboard
    // ----------------------------------------------------------------

    @GET
    @Path("/dashboard/summary")
    @RequiresPrivilege("admin.operations")
    public Response dashboardSummary() {
        return Response.ok(getAdminDashboardSummaryUseCase.execute()).build();
    }

    // ----------------------------------------------------------------
    // Recommendations – read
    // ----------------------------------------------------------------

    @GET
    @Path("/recommendations")
    @RequiresPrivilege("admin.operations")
    public Response listRecommendations(
            @QueryParam("status") String status,
            @QueryParam("severity") String severity,
            @QueryParam("type") String type) {
        List<OperationalRecommendationDto> items = listOperationalRecommendationsUseCase.execute(status, severity, type);
        return Response.ok(items).build();
    }

    @GET
    @Path("/recommendations/{id}")
    @RequiresPrivilege("admin.operations")
    public Response getRecommendation(@PathParam("id") UUID id) {
        return Response.ok(getOperationalRecommendationDetailUseCase.execute(id)).build();
    }

    // ----------------------------------------------------------------
    // Recommendations – commands
    // ----------------------------------------------------------------

    @POST
    @Path("/recommendations/refresh")
    @RequiresPrivilege("admin.operations")
    public Response refreshRecommendations() {
        int generated = refreshOperationalRecommendationsUseCase.execute();
        return Response.ok(Map.of("generated", generated)).build();
    }

    @PATCH
    @Path("/recommendations/{id}/status")
    @RequiresPrivilege("admin.operations")
    public Response updateStatus(@PathParam("id") UUID id, Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "'status' field is required")).build();
        }
        updateOperationalRecommendationStatusUseCase.execute(id, newStatus);
        return Response.noContent().build();
    }

    @POST
    @Path("/recommendations/{id}/tasks")
    @RequiresPrivilege("admin.operations")
    public Response createTask(@PathParam("id") UUID id, OperationalTask task) {
        OperationalTask created = createOperationalTaskUseCase.execute(id, task);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @Path("/recommendations/{id}/notifications")
    @RequiresPrivilege("admin.operations")
    public Response createNotification(@PathParam("id") UUID id, OperationalNotification notification) {
        OperationalNotification created = createOperationalNotificationUseCase.execute(id, notification);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @Path("/recommendations/{id}/supply-requests")
    @RequiresPrivilege("admin.operations")
    public Response createSupplyRequest(@PathParam("id") UUID id, SupplyRequest supplyRequest) {
        SupplyRequest created = createSupplyRequestUseCase.execute(id, supplyRequest);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    // ----------------------------------------------------------------
    // Resources – read
    // ----------------------------------------------------------------

    @GET
    @Path("/resources/summary")
    @RequiresPrivilege("admin.operations")
    public Response resourceSummary() {
        HospitalResourcesDto dto = getHospitalResourcesUseCase.execute();
        return Response.ok(new ResourceResponse("summary", dto.getSummary())).build();
    }

    @GET
    @Path("/resources/departments")
    @RequiresPrivilege("admin.operations")
    public Response resourceDepartments() {
        HospitalResourcesDto dto = getHospitalResourcesUseCase.execute();
        return Response.ok(new ResourceResponse("departments", dto.getDepartments())).build();
    }

    @GET
    @Path("/resources/staffing")
    @RequiresPrivilege("admin.operations")
    public Response resourceStaffing() {
        HospitalResourcesDto dto = getHospitalResourcesUseCase.execute();
        return Response.ok(new ResourceResponse("staffing", dto.getStaffing())).build();
    }

    @GET
    @Path("/resources/inventory")
    @RequiresPrivilege("admin.operations")
    public Response resourceInventory() {
        HospitalResourcesDto dto = getHospitalResourcesUseCase.execute();
        return Response.ok(new ResourceResponse("inventory", dto.getInventory())).build();
    }

    // ----------------------------------------------------------------
    // Resources – update
    // ----------------------------------------------------------------

    @PUT
    @Path("/resources/summary")
    @RequiresPrivilege("admin.operations")
    public Response updateSummary(HospitalResourceSnapshot input) {
        HospitalResourceSnapshot updated = updateHospitalResourcesUseCase.updateSnapshot(input);
        return Response.ok(updated).build();
    }

    @PUT
    @Path("/resources/departments/{departmentId}")
    @RequiresPrivilege("admin.operations")
    public Response updateDepartment(@PathParam("departmentId") UUID departmentId, HospitalDepartmentResource input) {
        HospitalDepartmentResource updated = updateHospitalResourcesUseCase.updateDepartment(departmentId, input);
        return Response.ok(updated).build();
    }

    @PUT
    @Path("/resources/staffing/{profileId}")
    @RequiresPrivilege("admin.operations")
    public Response updateStaffing(@PathParam("profileId") UUID profileId, HospitalStaffingProfile input) {
        HospitalStaffingProfile updated = updateHospitalResourcesUseCase.updateStaffingProfile(profileId, input);
        return Response.ok(updated).build();
    }

    @PUT
    @Path("/resources/inventory/{itemId}")
    @RequiresPrivilege("admin.operations")
    public Response updateInventoryItem(@PathParam("itemId") UUID itemId, HospitalInventoryItem input) {
        HospitalInventoryItem updated = updateHospitalResourcesUseCase.updateInventoryItem(itemId, input);
        return Response.ok(updated).build();
    }

    // ----------------------------------------------------------------
    // Internal response wrappers
    // ----------------------------------------------------------------

    public static class ResourceResponse {
        private final String section;
        private final Object data;

        public ResourceResponse(String section, Object data) {
            this.section = section;
            this.data = data;
        }

        public String getSection() { return section; }
        public Object getData() { return data; }
    }
}

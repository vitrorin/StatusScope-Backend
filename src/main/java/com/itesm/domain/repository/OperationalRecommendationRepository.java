package com.itesm.domain.repository;

import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.models.OperationalTask;
import com.itesm.domain.models.OperationalNotification;
import com.itesm.domain.models.SupplyRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationalRecommendationRepository {
    List<OperationalRecommendation> findByHospitalId(UUID hospitalId);
    List<OperationalRecommendation> findByHospitalIdAndStatus(UUID hospitalId, String status);
    List<OperationalRecommendation> findByHospitalIdAndSeverity(UUID hospitalId, String severity);
    Optional<OperationalRecommendation> findRecommendationById(UUID id);
    OperationalRecommendation save(OperationalRecommendation recommendation);
    void updateStatus(UUID id, String status);

    void appendAudit(OperationalRecommendationAudit audit);
    List<OperationalRecommendationAudit> findAuditByRecommendationId(UUID recommendationId);

    OperationalTask createTask(OperationalTask task);
    List<OperationalTask> findTasksByRecommendationId(UUID recommendationId);

    OperationalNotification createNotification(OperationalNotification notification);
    List<OperationalNotification> findNotificationsByRecommendationId(UUID recommendationId);

    SupplyRequest createSupplyRequest(SupplyRequest supplyRequest);
    List<SupplyRequest> findSupplyRequestsByRecommendationId(UUID recommendationId);
}

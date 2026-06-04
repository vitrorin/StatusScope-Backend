package com.itesm.domain.repository;

import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.models.OperationalTask;
import com.itesm.domain.models.OperationalNotification;
import com.itesm.domain.models.OperationalNotificationRecipient;
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
    OperationalTask updateTask(OperationalTask task);
    List<OperationalTask> findTasksByRecommendationId(UUID recommendationId);
    Optional<OperationalTask> findActiveTaskByRecommendationId(UUID recommendationId);

    OperationalNotification createNotification(OperationalNotification notification);
    List<OperationalNotification> findNotificationsByRecommendationId(UUID recommendationId);
    OperationalNotificationRecipient createNotificationRecipient(OperationalNotificationRecipient recipient);
    List<OperationalNotificationRecipient> findNotificationRecipientsByNotificationId(UUID notificationId);

    SupplyRequest createSupplyRequest(SupplyRequest supplyRequest);
    SupplyRequest createSupplyRequestWithMovement(SupplyRequest supplyRequest, String movementNotes);
    List<SupplyRequest> findSupplyRequestsByRecommendationId(UUID recommendationId);
}

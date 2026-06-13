package com.itesm.application.usecase;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OperationalRecommendationScheduler {

    private static final Logger LOG = Logger.getLogger(OperationalRecommendationScheduler.class);

    @Inject RefreshOperationalRecommendationsUseCase refreshOperationalRecommendationsUseCase;

    @ConfigProperty(name = "statusscope.admin.recommendations.scheduler.enabled", defaultValue = "false")
    boolean schedulerEnabled;

    @ConfigProperty(name = "statusscope.admin.recommendations.refresh-at-start", defaultValue = "false")
    boolean refreshAtStart;

    void refreshAtStartup(@Observes @Priority(30) StartupEvent event) {
        if (!schedulerEnabled || !refreshAtStart) {
            return;
        }

        refreshAllHospitals("startup");
    }

    @Scheduled(
            every = "{statusscope.admin.recommendations.refresh-interval:6h}",
            identity = "admin-operational-recommendations-refresh",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP
    )
    void refreshAllHospitals() {
        if (!schedulerEnabled) {
            return;
        }

        refreshAllHospitals("scheduled");
    }

    private void refreshAllHospitals(String trigger) {
        int generated = refreshOperationalRecommendationsUseCase.executeForAllHospitals();
        LOG.infof("Admin recommendation refresh completed. trigger=%s newlyGenerated=%d", trigger, generated);
    }
}

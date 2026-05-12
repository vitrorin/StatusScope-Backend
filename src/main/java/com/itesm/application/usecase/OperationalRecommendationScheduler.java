package com.itesm.application.usecase;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OperationalRecommendationScheduler {

    private static final Logger LOG = Logger.getLogger(OperationalRecommendationScheduler.class);

    @Inject RefreshOperationalRecommendationsUseCase refreshOperationalRecommendationsUseCase;

    @ConfigProperty(name = "statusscope.admin.recommendations.scheduler.enabled", defaultValue = "true")
    boolean schedulerEnabled;

    @Scheduled(
            every = "{statusscope.admin.recommendations.refresh-interval:6h}",
            identity = "admin-operational-recommendations-refresh",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP
    )
    void refreshAllHospitals() {
        if (!schedulerEnabled) {
            return;
        }

        int generated = refreshOperationalRecommendationsUseCase.executeForAllHospitals();
        LOG.infof("Admin recommendation refresh completed. Newly generated recommendations: %d", generated);
    }
}

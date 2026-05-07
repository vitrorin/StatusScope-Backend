package com.itesm.infrastructure.bootstrap;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OutbreakIngestionRunner {

    private static final Logger LOG = Logger.getLogger(OutbreakIngestionRunner.class);

    @Inject
    OutbreakCsvImporter importer;

    @ConfigProperty(name = "outbreak.ingestion.import-at-start", defaultValue = "true")
    boolean importAtStart;

    void onStart(@Observes @Priority(15) StartupEvent event) {
        if (!importAtStart) {
            LOG.info("OutbreakIngestionRunner: startup outbreak CSV import disabled");
            return;
        }

        OutbreakCsvImporter.ImportSummary municipalSummary = importer.importMunicipalOutbreaks();
        OutbreakCsvImporter.ImportSummary stateSummary = importer.importStateOutbreaks();
        LOG.infof(
                "OutbreakIngestionRunner: municipal outbreaks imported; created=%d updated=%d unchanged=%d ended=%d activeRows=%d",
                municipalSummary.created(),
                municipalSummary.updated(),
                municipalSummary.unchanged(),
                municipalSummary.ended(),
                municipalSummary.activeRows()
        );
        LOG.infof(
                "OutbreakIngestionRunner: state outbreaks imported; created=%d updated=%d unchanged=%d ended=%d activeRows=%d",
                stateSummary.created(),
                stateSummary.updated(),
                stateSummary.unchanged(),
                stateSummary.ended(),
                stateSummary.activeRows()
        );
    }
}

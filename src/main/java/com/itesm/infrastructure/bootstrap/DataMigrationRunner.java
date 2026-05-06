package com.itesm.infrastructure.bootstrap;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@ApplicationScoped
public class DataMigrationRunner {

    private static final Logger LOG = Logger.getLogger(DataMigrationRunner.class);

    @Inject
    Flyway flyway;

    @Inject
    AgroalDataSource dataSource;

    void onStart(@Observes @Priority(10) StartupEvent ev) {
        resetCatalogHistoryAfterSchemaRecreation();
        flyway.migrate();
        LOG.info("DataMigrationRunner: Flyway data migrations applied");
    }

    private void resetCatalogHistoryAfterSchemaRecreation() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            dropLegacyDiseaseCategoryTables(statement);

            if (!tableExists(statement, "flyway_schema_history")) {
                return;
            }

            clearFailedMigrationHistory(statement);

            if (!tableExists(statement, "municipalities")) {
                return;
            }

            if (!isTableEmpty(statement, "municipalities")) {
                return;
            }

            statement.executeUpdate("delete from flyway_schema_history where version is not null and version <> '0'");
            LOG.info("DataMigrationRunner: reset catalog migration history after schema recreation");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not prepare data migration history", e);
        }
    }

    private void dropLegacyDiseaseCategoryTables(Statement statement) throws SQLException {
        statement.executeUpdate("drop table if exists disease_category_links");
        statement.executeUpdate("drop table if exists disease_categories");
    }

    private void clearFailedMigrationHistory(Statement statement) throws SQLException {
        statement.executeUpdate("delete from flyway_schema_history where success = false");
    }

    private boolean tableExists(Statement statement, String tableName) throws SQLException {
        try (var ignored = statement.executeQuery("select 1 from " + tableName + " where 1 = 0")) {
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean isTableEmpty(Statement statement, String tableName) throws SQLException {
        try (var resultSet = statement.executeQuery("select count(*) from " + tableName)) {
            resultSet.next();
            return resultSet.getLong(1) == 0;
        }
    }
}

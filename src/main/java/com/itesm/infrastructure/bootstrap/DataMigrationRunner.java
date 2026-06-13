package com.itesm.infrastructure.bootstrap;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
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

    @ConfigProperty(name = "quarkus.hibernate-orm.schema-management.strategy", defaultValue = "none")
    String schemaManagementStrategy;

    @ConfigProperty(name = "quarkus.datasource.db-kind", defaultValue = "mysql")
    String dbKind;

    void onStart(@Observes @Priority(10) StartupEvent ev) {
        clearFailedMigrationHistoryIfPresent();
        if (isSchemaRecreatedOnStart()) {
            resetCatalogHistoryAfterSchemaRecreation();
        }
        flywayForCurrentDatabase().migrate();
        LOG.info("DataMigrationRunner: Flyway data migrations applied");
    }

    private Flyway flywayForCurrentDatabase() {
        if (!"h2".equalsIgnoreCase(dbKind)) {
            return flyway;
        }
        return Flyway.configure()
                .configuration(flyway.getConfiguration())
                .target(MigrationVersion.fromVersion("7"))
                .load();
    }

    private boolean isSchemaRecreatedOnStart() {
        return "drop-and-create".equalsIgnoreCase(schemaManagementStrategy)
                || "create".equalsIgnoreCase(schemaManagementStrategy);
    }

    private void resetCatalogHistoryAfterSchemaRecreation() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            dropLegacyDiseaseCategoryTables(statement);

            if (!tableExists(connection, "flyway_schema_history")) {
                return;
            }

            clearFailedMigrationHistory(statement);

            if (!tableExists(connection, "municipalities")) {
                return;
            }

            if (!isMunicipalitiesTableEmpty(statement)) {
                return;
            }

            statement.executeUpdate("delete from flyway_schema_history where version is not null and version <> '0'");
            LOG.info("DataMigrationRunner: reset catalog migration history after schema recreation");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not prepare data migration history", e);
        }
    }

    private void clearFailedMigrationHistoryIfPresent() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            if (!tableExists(connection, "flyway_schema_history")) {
                return;
            }
            clearFailedMigrationHistory(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not repair failed data migration history", e);
        }
    }

    private void dropLegacyDiseaseCategoryTables(Statement statement) throws SQLException {
        statement.executeUpdate("drop table if exists disease_category_links");
        statement.executeUpdate("drop table if exists disease_categories");
    }

    private void clearFailedMigrationHistory(Statement statement) throws SQLException {
        statement.executeUpdate("delete from flyway_schema_history where success = false");
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (var rs = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private boolean isMunicipalitiesTableEmpty(Statement statement) throws SQLException {
        try (var resultSet = statement.executeQuery("select count(*) from municipalities")) {
            resultSet.next();
            return resultSet.getLong(1) == 0;
        }
    }
}

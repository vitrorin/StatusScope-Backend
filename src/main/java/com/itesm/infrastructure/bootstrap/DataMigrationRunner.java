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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DataMigrationRunner {

    private static final Logger LOG = Logger.getLogger(DataMigrationRunner.class);
    private static final List<String> REQUIRED_MYSQL_ROUTINES = List.of(
            "sp_create_supply_request_with_movement",
            "sp_generate_hospital_operational_summary",
            "fn_bed_occupancy_pct",
            "fn_inventory_status"
    );
    private static final List<String> REQUIRED_MYSQL_TRIGGERS = List.of(
            "trg_validate_inventory_before_insert",
            "trg_validate_inventory_before_update",
            "trg_audit_recommendation_change"
    );

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
        validateAdvancedMysqlObjects();
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

    private void clearFailedMigrationHistoryIfPresent() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            if (!tableExists(statement, "flyway_schema_history")) {
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

    private void validateAdvancedMysqlObjects() {
        if (!"mysql".equalsIgnoreCase(dbKind)) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            List<String> missing = new ArrayList<>();
            for (String routineName : REQUIRED_MYSQL_ROUTINES) {
                if (!routineExists(connection, routineName)) {
                    missing.add("routine " + routineName);
                }
            }
            for (String triggerName : REQUIRED_MYSQL_TRIGGERS) {
                if (!triggerExists(connection, triggerName)) {
                    missing.add("trigger " + triggerName);
                }
            }

            if (!missing.isEmpty()) {
                throw new IllegalStateException("Missing MySQL advanced database objects after Flyway migration: "
                        + String.join(", ", missing)
                        + ". Check migration V12__advanced_database_module.sql and flyway_schema_history.");
            }

            LOG.info("DataMigrationRunner: verified MySQL advanced database objects");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not verify MySQL advanced database objects", e);
        }
    }

    private boolean routineExists(Connection connection, String routineName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select 1
                from information_schema.routines
                where routine_schema = database()
                  and routine_name = ?
                limit 1
                """)) {
            statement.setString(1, routineName);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean triggerExists(Connection connection, String triggerName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select 1
                from information_schema.triggers
                where trigger_schema = database()
                  and trigger_name = ?
                limit 1
                """)) {
            statement.setString(1, triggerName);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
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

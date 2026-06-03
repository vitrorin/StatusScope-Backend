package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

public class V11__add_operational_workflow_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        // operational_notifications: new audience and delivery columns
        addColumn(connection, "operational_notifications", "audience_group_id",   "VARCHAR(36)");
        addColumn(connection, "operational_notifications", "audience_contact_id",  "VARCHAR(36)");
        addColumn(connection, "operational_notifications", "audience_type",         "VARCHAR(32)");
        addColumn(connection, "operational_notifications", "audience_department_code", "VARCHAR(32)");
        addColumn(connection, "operational_notifications", "delivery_channel",      "VARCHAR(32)");
        addColumn(connection, "operational_notifications", "delivery_status_detail","VARCHAR(255)");
        addColumn(connection, "operational_notifications", "source_action_code",    "VARCHAR(32)");

        // operational_tasks: new owner and traceability columns
        addColumn(connection, "operational_tasks", "owner_contact_id",               "VARCHAR(36)");
        addColumn(connection, "operational_tasks", "owner_group_id",                 "VARCHAR(36)");
        addColumn(connection, "operational_tasks", "source_action_code",             "VARCHAR(32)");
        addColumn(connection, "operational_tasks", "recommended_by_recommendation_id","VARCHAR(36)");

        // operational_recommendations: assigned owner
        addColumn(connection, "operational_recommendations", "assigned_owner_user_id", "VARCHAR(36)");
    }

    private void addColumn(Connection connection, String table, String column, String definition) throws SQLException {
        if (!columnExists(connection, table, column)) {
            execute(connection, "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String tableVariant : variants(tableName)) {
            for (String columnVariant : variants(columnName)) {
                try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableVariant, columnVariant)) {
                    if (resultSet.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<String> variants(String identifier) {
        String upper = identifier.toUpperCase(Locale.ROOT);
        String lower = identifier.toLowerCase(Locale.ROOT);
        if (upper.equals(lower)) {
            return List.of(identifier);
        }
        return List.of(identifier, upper, lower);
    }
}

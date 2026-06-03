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

public class V9__add_operational_recommendation_translations extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!columnExists(connection, "operational_recommendations", "content_translations_json")) {
            execute(connection, "alter table operational_recommendations add column content_translations_json text");
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

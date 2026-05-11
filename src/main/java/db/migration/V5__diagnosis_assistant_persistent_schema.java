package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class V5__diagnosis_assistant_persistent_schema extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        addPatientEvaluationFeedbackColumns(connection);
        createAssistantTables(connection);
        createIndexes(connection);
        createForeignKeys(connection);
    }

    private void addPatientEvaluationFeedbackColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "patient_evaluations", "finalized_at", "timestamp null");
        addColumnIfMissing(connection, "patient_evaluations", "final_disease_id", "varchar(36) null");
        addColumnIfMissing(connection, "patient_evaluations", "final_diagnosis_label", "varchar(256) null");
        addColumnIfMissing(connection, "patient_evaluations", "final_decision_source", "varchar(48) null");
        addColumnIfMissing(connection, "patient_evaluations", "doctor_feedback_notes", "text null");
    }

    private void createAssistantTables(Connection connection) throws SQLException {
        createTableIfMissing(connection, "diagnosis_assistant_threads", """
                create table diagnosis_assistant_threads (
                    id varchar(36) not null,
                    evaluation_id varchar(36) not null,
                    doctor_user_id varchar(36) not null,
                    hospital_id varchar(36),
                    status varchar(16) not null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    primary key (id),
                    constraint uk_dat_evaluation unique (evaluation_id)
                )
                """);

        createTableIfMissing(connection, "diagnosis_assistant_messages", """
                create table diagnosis_assistant_messages (
                    id varchar(36) not null,
                    thread_id varchar(36) not null,
                    role varchar(16) not null,
                    message_text text not null,
                    sequence_no integer not null,
                    created_at timestamp not null,
                    primary key (id)
                )
                """);

        createTableIfMissing(connection, "diagnosis_assistant_suggestions", """
                create table diagnosis_assistant_suggestions (
                    id varchar(36) not null,
                    message_id varchar(36) not null,
                    evaluation_id varchar(36) not null,
                    disease_id varchar(36),
                    display_name varchar(256) not null,
                    rank_order integer not null,
                    confidence double precision,
                    rationale text,
                    locality_risk_level varchar(16),
                    was_primary_suggestion boolean not null,
                    created_at timestamp not null,
                    primary key (id)
                )
                """);

        createTableIfMissing(connection, "diagnosis_assistant_retrieved_cases", """
                create table diagnosis_assistant_retrieved_cases (
                    id varchar(36) not null,
                    message_id varchar(36) not null,
                    retrieved_evaluation_id varchar(36) not null,
                    rank_order integer not null,
                    similarity_score double precision,
                    created_at timestamp not null,
                    primary key (id)
                )
                """);

        createTableIfMissing(connection, "diagnosis_feedback_events", """
                create table diagnosis_feedback_events (
                    id varchar(36) not null,
                    evaluation_id varchar(36) not null,
                    thread_id varchar(36),
                    doctor_user_id varchar(36) not null,
                    hospital_id varchar(36),
                    feedback_type varchar(48) not null,
                    accepted_assistant_message_id varchar(36),
                    final_disease_id varchar(36),
                    final_diagnosis_label varchar(256),
                    feedback_notes text,
                    created_at timestamp not null,
                    primary key (id)
                )
                """);
    }

    private void createIndexes(Connection connection) throws SQLException {
        createIndexIfMissing(connection, "idx_pe_final_disease", "patient_evaluations", "final_disease_id");

        createIndexIfMissing(connection, "idx_dat_evaluation", "diagnosis_assistant_threads", "evaluation_id");
        createIndexIfMissing(connection, "idx_dat_doctor", "diagnosis_assistant_threads", "doctor_user_id");
        createIndexIfMissing(connection, "idx_dat_hospital", "diagnosis_assistant_threads", "hospital_id");

        createIndexIfMissing(connection, "idx_dam_thread", "diagnosis_assistant_messages", "thread_id");
        createIndexIfMissing(connection, "idx_dam_thread_seq", "diagnosis_assistant_messages", "thread_id", "sequence_no");

        createIndexIfMissing(connection, "idx_das_message", "diagnosis_assistant_suggestions", "message_id");
        createIndexIfMissing(connection, "idx_das_evaluation", "diagnosis_assistant_suggestions", "evaluation_id");
        createIndexIfMissing(connection, "idx_das_disease", "diagnosis_assistant_suggestions", "disease_id");

        createIndexIfMissing(connection, "idx_darc_message", "diagnosis_assistant_retrieved_cases", "message_id");
        createIndexIfMissing(connection, "idx_darc_retrieved", "diagnosis_assistant_retrieved_cases", "retrieved_evaluation_id");

        createIndexIfMissing(connection, "idx_dfe_evaluation", "diagnosis_feedback_events", "evaluation_id");
        createIndexIfMissing(connection, "idx_dfe_thread", "diagnosis_feedback_events", "thread_id");
        createIndexIfMissing(connection, "idx_dfe_doctor", "diagnosis_feedback_events", "doctor_user_id");
        createIndexIfMissing(connection, "idx_dfe_hospital", "diagnosis_feedback_events", "hospital_id");
        createIndexIfMissing(connection, "idx_dfe_type", "diagnosis_feedback_events", "feedback_type");
    }

    private void createForeignKeys(Connection connection) throws SQLException {
        addForeignKeyIfMissing(connection, "fk_pe_final_disease", "patient_evaluations", "final_disease_id", "diseases", "id");

        addForeignKeyIfMissing(connection, "fk_dat_evaluation", "diagnosis_assistant_threads", "evaluation_id", "patient_evaluations", "id");
        addForeignKeyIfMissing(connection, "fk_dat_doctor", "diagnosis_assistant_threads", "doctor_user_id", "users", "id");
        addForeignKeyIfMissing(connection, "fk_dat_hospital", "diagnosis_assistant_threads", "hospital_id", "hospitals", "id");

        addForeignKeyIfMissing(connection, "fk_dam_thread", "diagnosis_assistant_messages", "thread_id", "diagnosis_assistant_threads", "id");

        addForeignKeyIfMissing(connection, "fk_das_message", "diagnosis_assistant_suggestions", "message_id", "diagnosis_assistant_messages", "id");
        addForeignKeyIfMissing(connection, "fk_das_evaluation", "diagnosis_assistant_suggestions", "evaluation_id", "patient_evaluations", "id");
        addForeignKeyIfMissing(connection, "fk_das_disease", "diagnosis_assistant_suggestions", "disease_id", "diseases", "id");

        addForeignKeyIfMissing(connection, "fk_darc_message", "diagnosis_assistant_retrieved_cases", "message_id", "diagnosis_assistant_messages", "id");
        addForeignKeyIfMissing(connection, "fk_darc_retrieved", "diagnosis_assistant_retrieved_cases", "retrieved_evaluation_id", "patient_evaluations", "id");

        addForeignKeyIfMissing(connection, "fk_dfe_evaluation", "diagnosis_feedback_events", "evaluation_id", "patient_evaluations", "id");
        addForeignKeyIfMissing(connection, "fk_dfe_thread", "diagnosis_feedback_events", "thread_id", "diagnosis_assistant_threads", "id");
        addForeignKeyIfMissing(connection, "fk_dfe_doctor", "diagnosis_feedback_events", "doctor_user_id", "users", "id");
        addForeignKeyIfMissing(connection, "fk_dfe_hospital", "diagnosis_feedback_events", "hospital_id", "hospitals", "id");
        addForeignKeyIfMissing(connection, "fk_dfe_message", "diagnosis_feedback_events", "accepted_assistant_message_id", "diagnosis_assistant_messages", "id");
        addForeignKeyIfMissing(connection, "fk_dfe_final_disease", "diagnosis_feedback_events", "final_disease_id", "diseases", "id");
    }

    private void addColumnIfMissing(Connection connection, String tableName, String columnName, String definition)
            throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        execute(connection, "alter table " + tableName + " add column " + columnName + " " + definition);
    }

    private void createTableIfMissing(Connection connection, String tableName, String createSql) throws SQLException {
        if (tableExists(connection, tableName)) {
            return;
        }
        execute(connection, createSql);
    }

    private void createIndexIfMissing(Connection connection, String indexName, String tableName, String... columnNames)
            throws SQLException {
        if (indexExists(connection, tableName, columnNames)) {
            return;
        }
        execute(connection, "create index " + indexName + " on " + tableName + " (" + String.join(", ", columnNames) + ")");
    }

    private void addForeignKeyIfMissing(Connection connection,
                                        String constraintName,
                                        String tableName,
                                        String columnName,
                                        String referencedTableName,
                                        String referencedColumnName) throws SQLException {
        if (foreignKeyExists(connection, tableName, columnName, referencedTableName)) {
            return;
        }
        execute(connection, "alter table " + tableName
                + " add constraint " + constraintName
                + " foreign key (" + columnName + ") references "
                + referencedTableName + " (" + referencedColumnName + ")");
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String variant : variants(tableName)) {
            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, variant, new String[]{"TABLE"})) {
                if (resultSet.next()) {
                    return true;
                }
            }
        }
        return false;
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

    private boolean indexExists(Connection connection, String tableName, String... columnNames) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<String> expected = normalizeColumns(List.of(columnNames));

        for (String tableVariant : variants(tableName)) {
            Map<String, TreeMap<Short, String>> indexes = new LinkedHashMap<>();
            try (ResultSet resultSet = metaData.getIndexInfo(connection.getCatalog(), null, tableVariant, false, false)) {
                while (resultSet.next()) {
                    String indexName = resultSet.getString("INDEX_NAME");
                    String columnName = resultSet.getString("COLUMN_NAME");
                    short ordinal = resultSet.getShort("ORDINAL_POSITION");
                    if (indexName == null || columnName == null || ordinal <= 0) {
                        continue;
                    }
                    indexes.computeIfAbsent(indexName, ignored -> new TreeMap<>())
                            .put(ordinal, columnName);
                }
            }

            for (TreeMap<Short, String> columns : indexes.values()) {
                if (normalizeColumns(new ArrayList<>(columns.values())).equals(expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean foreignKeyExists(Connection connection,
                                     String tableName,
                                     String columnName,
                                     String referencedTableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String tableVariant : variants(tableName)) {
            try (ResultSet resultSet = metaData.getImportedKeys(connection.getCatalog(), null, tableVariant)) {
                while (resultSet.next()) {
                    String existingColumn = resultSet.getString("FKCOLUMN_NAME");
                    String existingReferencedTable = resultSet.getString("PKTABLE_NAME");
                    if (equalsIdentifier(existingColumn, columnName)
                            && equalsIdentifier(existingReferencedTable, referencedTableName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<String> normalizeColumns(List<String> columns) {
        return columns.stream()
                .map(column -> column.toLowerCase(Locale.ROOT))
                .toList();
    }

    private boolean equalsIdentifier(String actual, String expected) {
        return actual != null && actual.equalsIgnoreCase(expected);
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

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

public class V7__admin_operational_workflow_contract extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        createNewTables(connection);
        addOperationalRecommendationColumns(connection);
        addOperationalTaskColumns(connection);
        addOperationalNotificationColumns(connection);
        addSupplyRequestColumns(connection);
        addForeignKeys(connection);
        seedRecommendationWorkflowData(connection);
    }

    private void createNewTables(Connection connection) throws SQLException {
        createTableIfMissing(connection, "hospital_operational_contacts", """
                create table hospital_operational_contacts (
                    id                  varchar(36)  not null primary key,
                    hospital_id         varchar(36)  not null,
                    user_id             varchar(36),
                    display_name        varchar(255) not null,
                    role_label          varchar(128) not null,
                    department_code     varchar(32),
                    contact_channel     varchar(32),
                    contact_value       varchar(255),
                    availability_status varchar(32),
                    is_assignable       boolean      not null default false,
                    is_notifiable       boolean      not null default false,
                    updated_at          timestamp    not null,
                    constraint fk_hoc_hospital foreign key (hospital_id) references hospitals(id),
                    constraint fk_hoc_user     foreign key (user_id)     references users(id)
                )""");

        createTableIfMissing(connection, "hospital_operational_groups", """
                create table hospital_operational_groups (
                    id              varchar(36)  not null primary key,
                    hospital_id     varchar(36)  not null,
                    group_code      varchar(32)  not null,
                    group_name      varchar(128) not null,
                    group_type      varchar(32)  not null,
                    department_code varchar(32),
                    is_assignable   boolean      not null default false,
                    is_notifiable   boolean      not null default false,
                    updated_at      timestamp    not null,
                    constraint fk_hog_hospital foreign key (hospital_id) references hospitals(id)
                )""");

        createTableIfMissing(connection, "hospital_operational_group_members", """
                create table hospital_operational_group_members (
                    id          varchar(36) not null primary key,
                    group_id    varchar(36) not null,
                    contact_id  varchar(36) not null,
                    created_at  timestamp   not null,
                    constraint fk_hogm_group   foreign key (group_id)   references hospital_operational_groups(id),
                    constraint fk_hogm_contact foreign key (contact_id) references hospital_operational_contacts(id)
                )""");

        createTableIfMissing(connection, "hospital_inventory_movements", """
                create table hospital_inventory_movements (
                    id                        varchar(36) not null primary key,
                    hospital_id               varchar(36) not null,
                    inventory_item_id         varchar(36) not null,
                    movement_type             varchar(32) not null,
                    quantity_delta            int         not null default 0,
                    unit                      varchar(32),
                    notes                     text,
                    related_supply_request_id varchar(36),
                    created_at                timestamp   not null,
                    constraint fk_him_hospital      foreign key (hospital_id)               references hospitals(id),
                    constraint fk_him_inventory     foreign key (inventory_item_id)         references hospital_inventory_items(id),
                    constraint fk_him_supply_request foreign key (related_supply_request_id) references supply_requests(id)
                )""");
    }

    private void addOperationalRecommendationColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "operational_recommendations", "primary_department_resource_id", "varchar(36)");
        addColumnIfMissing(connection, "operational_recommendations", "primary_staffing_profile_id",    "varchar(36)");
        addColumnIfMissing(connection, "operational_recommendations", "primary_inventory_item_id",      "varchar(36)");
        addColumnIfMissing(connection, "operational_recommendations", "presentation_variant",            "varchar(32)");
        addColumnIfMissing(connection, "operational_recommendations", "primary_action_code",             "varchar(32)");
        addColumnIfMissing(connection, "operational_recommendations", "available_actions_json",          "text");
        addColumnIfMissing(connection, "operational_recommendations", "allowed_status_transitions_json", "text");
        addColumnIfMissing(connection, "operational_recommendations", "display_category_label",          "varchar(64)");
        addColumnIfMissing(connection, "operational_recommendations", "display_severity_label",          "varchar(32)");
        addColumnIfMissing(connection, "operational_recommendations", "display_status_label",            "varchar(32)");
        addColumnIfMissing(connection, "operational_recommendations", "expires_at",                      "timestamp null");
        addColumnIfMissing(connection, "operational_recommendations", "assigned_owner_user_id",          "varchar(36)");
    }

    private void addOperationalTaskColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "operational_tasks", "owner_contact_id",                 "varchar(36)");
        addColumnIfMissing(connection, "operational_tasks", "owner_group_id",                   "varchar(36)");
        addColumnIfMissing(connection, "operational_tasks", "source_action_code",               "varchar(32)");
        addColumnIfMissing(connection, "operational_tasks", "recommended_by_recommendation_id", "varchar(36)");
    }

    private void addOperationalNotificationColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "operational_notifications", "audience_group_id",      "varchar(36)");
        addColumnIfMissing(connection, "operational_notifications", "audience_contact_id",    "varchar(36)");
        addColumnIfMissing(connection, "operational_notifications", "delivery_channel",       "varchar(32)");
        addColumnIfMissing(connection, "operational_notifications", "delivery_status_detail", "varchar(255)");
        addColumnIfMissing(connection, "operational_notifications", "source_action_code",     "varchar(32)");
    }

    private void addSupplyRequestColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "supply_requests", "source_action_code",                          "varchar(32)");
        addColumnIfMissing(connection, "supply_requests", "priority",                                    "varchar(16)");
        addColumnIfMissing(connection, "supply_requests", "requested_needed_by",                         "timestamp null");
        addColumnIfMissing(connection, "supply_requests", "linked_recommendation_inventory_item_id",     "varchar(36)");
    }

    private void addForeignKeys(Connection connection) throws SQLException {
        addForeignKeyIfMissing(connection, "fk_or_department_resource", "operational_recommendations", "primary_department_resource_id", "hospital_department_resources", "id");
        addForeignKeyIfMissing(connection, "fk_or_staffing_profile",    "operational_recommendations", "primary_staffing_profile_id",    "hospital_staffing_profiles",    "id");
        addForeignKeyIfMissing(connection, "fk_or_inventory_item",      "operational_recommendations", "primary_inventory_item_id",      "hospital_inventory_items",      "id");
        addForeignKeyIfMissing(connection, "fk_or_assigned_owner",      "operational_recommendations", "assigned_owner_user_id",         "users",                         "id");
        addForeignKeyIfMissing(connection, "fk_ot_owner_contact",       "operational_tasks",           "owner_contact_id",               "hospital_operational_contacts", "id");
        addForeignKeyIfMissing(connection, "fk_ot_owner_group",         "operational_tasks",           "owner_group_id",                 "hospital_operational_groups",   "id");
        addForeignKeyIfMissing(connection, "fk_ot_recommended_by",      "operational_tasks",           "recommended_by_recommendation_id","operational_recommendations",  "id");
        addForeignKeyIfMissing(connection, "fk_on_audience_group",      "operational_notifications",   "audience_group_id",              "hospital_operational_groups",   "id");
        addForeignKeyIfMissing(connection, "fk_on_audience_contact",    "operational_notifications",   "audience_contact_id",            "hospital_operational_contacts", "id");
        addForeignKeyIfMissing(connection, "fk_sr_linked_recommendation_inventory_item", "supply_requests", "linked_recommendation_inventory_item_id", "hospital_inventory_items", "id");
    }

    private void seedRecommendationWorkflowData(Connection connection) throws SQLException {
        execute(connection, """
                update operational_recommendations
                set primary_department_resource_id = '21000000-0000-0000-0000-000000000003',
                    primary_staffing_profile_id    = '22000000-0000-0000-0000-000000000002',
                    primary_inventory_item_id      = '23000000-0000-0000-0000-000000000002',
                    presentation_variant           = 'alert',
                    primary_action_code            = 'ASSIGN_TASK',
                    available_actions_json         = '[{"code":"ASSIGN_TASK","label":"Assign task","style":"primary","enabled":true},{"code":"NOTIFY_STAFF","label":"Notify staff","style":"secondary","enabled":true},{"code":"ORDER_SUPPLIES","label":"Order supplies","style":"secondary","enabled":true}]',
                    allowed_status_transitions_json= '["ACCEPTED","ASSIGNED","REJECTED"]',
                    display_category_label         = 'Bed Capacity',
                    display_severity_label         = 'Critical',
                    display_status_label           = 'New',
                    expires_at                     = current_timestamp
                where id = '24000000-0000-0000-0000-000000000001'
                """);

        execute(connection, """
                update operational_recommendations
                set primary_department_resource_id = '21000000-0000-0000-0000-000000000001',
                    primary_staffing_profile_id    = '22000000-0000-0000-0000-000000000001',
                    presentation_variant           = 'urgent',
                    primary_action_code            = 'ASSIGN_TASK',
                    available_actions_json         = '[{"code":"ASSIGN_TASK","label":"Assign task","style":"primary","enabled":true},{"code":"NOTIFY_STAFF","label":"Notify staff","style":"secondary","enabled":true},{"code":"ORDER_SUPPLIES","label":"Order supplies","style":"secondary","enabled":false,"disabledReason":"No inventory item linked"}]',
                    allowed_status_transitions_json= '["ASSIGNED","COMPLETED","REJECTED"]',
                    display_category_label         = 'Staffing',
                    display_severity_label         = 'High',
                    display_status_label           = 'Accepted',
                    expires_at                     = current_timestamp
                where id = '24000000-0000-0000-0000-000000000002'
                """);

        execute(connection, """
                update operational_recommendations
                set primary_department_resource_id = '21000000-0000-0000-0000-000000000004',
                    primary_staffing_profile_id    = '22000000-0000-0000-0000-000000000004',
                    primary_inventory_item_id      = '23000000-0000-0000-0000-000000000005',
                    presentation_variant           = 'standard',
                    primary_action_code            = 'ORDER_SUPPLIES',
                    available_actions_json         = '[{"code":"ASSIGN_TASK","label":"Assign task","style":"secondary","enabled":true},{"code":"NOTIFY_STAFF","label":"Notify staff","style":"secondary","enabled":true},{"code":"ORDER_SUPPLIES","label":"Order supplies","style":"primary","enabled":true}]',
                    allowed_status_transitions_json= '["ACCEPTED","ASSIGNED","REJECTED"]',
                    display_category_label         = 'Isolation',
                    display_severity_label         = 'Medium',
                    display_status_label           = 'New',
                    expires_at                     = current_timestamp
                where id = '24000000-0000-0000-0000-000000000003'
                """);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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
        DatabaseMetaData meta = connection.getMetaData();
        for (String variant : variants(tableName)) {
            try (ResultSet rs = meta.getTables(connection.getCatalog(), null, variant, new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        for (String tableVariant : variants(tableName)) {
            for (String columnVariant : variants(columnName)) {
                try (ResultSet rs = meta.getColumns(connection.getCatalog(), null, tableVariant, columnVariant)) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean foreignKeyExists(Connection connection, String tableName, String columnName,
                                     String referencedTableName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        for (String tableVariant : variants(tableName)) {
            try (ResultSet rs = meta.getImportedKeys(connection.getCatalog(), null, tableVariant)) {
                while (rs.next()) {
                    String existingColumn = rs.getString("FKCOLUMN_NAME");
                    String existingRefTable = rs.getString("PKTABLE_NAME");
                    if (equalsIdentifier(existingColumn, columnName)
                            && equalsIdentifier(existingRefTable, referencedTableName)) {
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

    private boolean equalsIdentifier(String actual, String expected) {
        return actual != null && actual.equalsIgnoreCase(expected);
    }
}

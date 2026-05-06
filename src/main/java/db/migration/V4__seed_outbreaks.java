package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class V4__seed_outbreaks extends BaseJavaMigration {

    private static final String MUNICIPAL_OUTBREAKS_RESOURCE = "data/outbreaks/municipal_outbreaks.csv";
    private static final String STATE_OUTBREAKS_RESOURCE = "data/outbreaks/state_outbreaks.csv";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDateTime STATE_OUTBREAK_STARTED_AT = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        Map<String, String> diseaseIdsByName = loadIdsByNormalizedName(connection, "diseases");
        Map<String, String> stateIdsByName = loadStateIdsByNormalizedName(connection);
        Set<String> municipalityIds = loadIds(connection, "municipalities");

        List<OutbreakSeedRow> rows = new ArrayList<>();
        rows.addAll(loadMunicipalOutbreaks(diseaseIdsByName, municipalityIds));
        rows.addAll(loadStateOutbreaks(diseaseIdsByName, stateIdsByName));

        clearSeededOutbreaks(connection);
        insertOutbreaks(connection, rows);
    }

    private List<OutbreakSeedRow> loadMunicipalOutbreaks(
            Map<String, String> diseaseIdsByName,
            Set<String> municipalityIds
    ) throws Exception {
        CsvData csvData = readCsvResource(MUNICIPAL_OUTBREAKS_RESOURCE);
        csvData.requireHeaders(
                "scope",
                "municipality_id",
                "municipio_nombre",
                "disease_key",
                "disease_name",
                "confirmation_status",
                "case_count",
                "started_at",
                "ended_at",
                "status",
                "source_datasets",
                "latest_update"
        );

        List<OutbreakSeedRow> rows = new ArrayList<>();
        for (int index = 0; index < csvData.rows().size(); index++) {
            Map<String, String> record = csvData.rows().get(index);
            String municipalityId = required(record, "municipality_id", MUNICIPAL_OUTBREAKS_RESOURCE, index);
            if (!municipalityIds.contains(municipalityId)) {
                throw new IllegalStateException("Unknown municipality_id in municipal outbreak CSV row "
                        + (index + 2) + ": " + municipalityId);
            }

            rows.add(new OutbreakSeedRow(
                    "MUNICIPALITY",
                    diseaseId(diseaseIdsByName, record, MUNICIPAL_OUTBREAKS_RESOURCE, index),
                    municipalityId,
                    null,
                    parseCaseCount(record, MUNICIPAL_OUTBREAKS_RESOURCE, index),
                    required(record, "confirmation_status", MUNICIPAL_OUTBREAKS_RESOURCE, index),
                    required(record, "status", MUNICIPAL_OUTBREAKS_RESOURCE, index),
                    parseDateTime(required(record, "started_at", MUNICIPAL_OUTBREAKS_RESOURCE, index)),
                    parseNullableDateTime(record.get("ended_at"))
            ));
        }
        return rows;
    }

    private List<OutbreakSeedRow> loadStateOutbreaks(
            Map<String, String> diseaseIdsByName,
            Map<String, String> stateIdsByName
    ) throws Exception {
        CsvData csvData = readCsvResource(STATE_OUTBREAKS_RESOURCE);
        csvData.requireHeaders("scope", "estado", "disease_name", "case_count", "confirmation_status", "status");

        List<OutbreakSeedRow> rows = new ArrayList<>();
        for (int index = 0; index < csvData.rows().size(); index++) {
            Map<String, String> record = csvData.rows().get(index);
            String stateName = required(record, "estado", STATE_OUTBREAKS_RESOURCE, index);
            String stateId = stateIdsByName.get(normalize(stateName));
            if (stateId == null) {
                throw new IllegalStateException("Unknown state in state outbreak CSV row "
                        + (index + 2) + ": " + stateName);
            }

            rows.add(new OutbreakSeedRow(
                    "STATE",
                    diseaseId(diseaseIdsByName, record, STATE_OUTBREAKS_RESOURCE, index),
                    null,
                    stateId,
                    parseCaseCount(record, STATE_OUTBREAKS_RESOURCE, index),
                    required(record, "confirmation_status", STATE_OUTBREAKS_RESOURCE, index),
                    required(record, "status", STATE_OUTBREAKS_RESOURCE, index),
                    STATE_OUTBREAK_STARTED_AT,
                    null
            ));
        }
        return rows;
    }

    private void clearSeededOutbreaks(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("delete from alerts where outbreak_id like '72000000-%'");
            statement.executeUpdate("delete from outbreaks where id like '72000000-%'");
        }
    }

    private void insertOutbreaks(Connection connection, List<OutbreakSeedRow> rows) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into outbreaks (
                    id,
                    disease_id,
                    scope,
                    municipality_id,
                    state_id,
                    case_count,
                    confirmation_status,
                    status,
                    started_at,
                    ended_at,
                    created_at,
                    updated_at
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """)) {
            for (int index = 0; index < rows.size(); index++) {
                OutbreakSeedRow row = rows.get(index);
                statement.setString(1, sequentialId(index + 1));
                statement.setString(2, row.diseaseId());
                statement.setString(3, row.scope());
                statement.setString(4, row.municipalityId());
                statement.setString(5, row.stateId());
                statement.setInt(6, row.caseCount());
                statement.setString(7, row.confirmationStatus());
                statement.setString(8, row.status());
                statement.setTimestamp(9, Timestamp.valueOf(row.startedAt()));
                statement.setTimestamp(10, row.endedAt() == null ? null : Timestamp.valueOf(row.endedAt()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private String diseaseId(
            Map<String, String> diseaseIdsByName,
            Map<String, String> record,
            String resource,
            int rowIndex
    ) {
        String diseaseName = required(record, "disease_name", resource, rowIndex);
        String diseaseId = diseaseIdsByName.get(normalize(diseaseName));
        if (diseaseId == null) {
            throw new IllegalStateException("Unknown disease in " + resource + " row "
                    + (rowIndex + 2) + ": " + diseaseName);
        }
        return diseaseId;
    }

    private int parseCaseCount(Map<String, String> record, String resource, int rowIndex) {
        String value = required(record, "case_count", resource, rowIndex);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid case_count in " + resource + " row "
                    + (rowIndex + 2) + ": " + value, e);
        }
    }

    private String required(Map<String, String> record, String key, String resource, int rowIndex) {
        String value = record.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + key + " in " + resource + " row " + (rowIndex + 2));
        }
        return value.trim();
    }

    private LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
    }

    private LocalDateTime parseNullableDateTime(String value) {
        return value == null || value.isBlank() ? null : parseDateTime(value);
    }

    private Map<String, String> loadIdsByNormalizedName(Connection connection, String tableName) throws Exception {
        Map<String, String> idsByName = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("select id, name from " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                idsByName.put(normalize(resultSet.getString("name")), resultSet.getString("id"));
            }
        }
        return idsByName;
    }

    private Map<String, String> loadStateIdsByNormalizedName(Connection connection) throws Exception {
        Map<String, String> idsByName = loadIdsByNormalizedName(connection, "states");
        putStateAlias(idsByName, "coahuila", "coahuila de zaragoza");
        putStateAlias(idsByName, "michoacan", "michoacan de ocampo");
        putStateAlias(idsByName, "veracruz", "veracruz de ignacio de la llave");
        putStateAlias(idsByName, "estado de mexico", "mexico");
        return idsByName;
    }

    private void putStateAlias(Map<String, String> idsByName, String alias, String canonicalName) {
        String canonicalId = idsByName.get(normalize(canonicalName));
        if (canonicalId != null) {
            idsByName.put(normalize(alias), canonicalId);
        }
    }

    private Set<String> loadIds(Connection connection, String tableName) throws Exception {
        Set<String> ids = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("select id from " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getString("id"));
            }
        }
        return ids;
    }

    private CsvData readCsvResource(String resource) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resource)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing CSV resource: " + resource);
            }

            List<List<String>> records = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        records.add(parseCsvLine(line));
                    }
                }
            }
            if (records.size() < 2) {
                throw new IllegalStateException("CSV resource must contain a header and at least one row: " + resource);
            }

            List<String> header = records.get(0);
            header.set(0, stripBom(header.get(0)).trim());

            List<Map<String, String>> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex < records.size(); rowIndex++) {
                List<String> record = records.get(rowIndex);
                if (record.size() != header.size()) {
                    throw new IllegalStateException("Invalid CSV row " + (rowIndex + 1) + " in " + resource
                            + ": expected " + header.size() + " columns but got " + record.size());
                }

                Map<String, String> row = new LinkedHashMap<>();
                for (int columnIndex = 0; columnIndex < header.size(); columnIndex++) {
                    row.put(header.get(columnIndex).trim(), record.get(columnIndex).trim());
                }
                rows.add(row);
            }

            return new CsvData(header, rows, resource);
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }

        values.add(value.toString());
        return values;
    }

    private String stripBom(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private String sequentialId(int number) {
        return "72000000-0000-0000-0000-%012d".formatted(number);
    }

    private record CsvData(List<String> header, List<Map<String, String>> rows, String resource) {
        void requireHeaders(String... expectedHeaders) {
            if (header.size() != expectedHeaders.length) {
                throw new IllegalStateException("Invalid header count in " + resource
                        + ": expected " + expectedHeaders.length + " but got " + header.size());
            }
            for (int index = 0; index < expectedHeaders.length; index++) {
                if (!expectedHeaders[index].equals(header.get(index).trim())) {
                    throw new IllegalStateException("Invalid header in " + resource + " column " + (index + 1)
                            + ": expected " + expectedHeaders[index] + " but got " + header.get(index));
                }
            }
        }
    }

    private record OutbreakSeedRow(
            String scope,
            String diseaseId,
            String municipalityId,
            String stateId,
            int caseCount,
            String confirmationStatus,
            String status,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
    }
}

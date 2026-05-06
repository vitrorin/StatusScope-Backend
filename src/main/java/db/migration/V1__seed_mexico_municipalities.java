package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class V1__seed_mexico_municipalities extends BaseJavaMigration {

    private static final String CSV_RESOURCE = "data/municipalities/mexico_municipalities.csv";

    @Override
    public void migrate(Context context) throws Exception {
        List<MunicipalityCsvRow> rows = loadCsvData();
        insertMunicipalities(context.getConnection(), rows);
    }

    private List<MunicipalityCsvRow> loadCsvData() throws Exception {
        List<List<String>> records = readCsvResource();
        if (records.size() < 2) {
            throw new IllegalStateException("Municipalities CSV must contain a header and at least one row");
        }

        List<String> header = records.get(0);
        if (header.size() != 6
                || !"latitud".equalsIgnoreCase(stripBom(header.get(0)).trim())
                || !"longitud".equalsIgnoreCase(header.get(1).trim())
                || !"code".equalsIgnoreCase(header.get(2).trim())
                || !"id".equalsIgnoreCase(header.get(3).trim())
                || !"state_id".equalsIgnoreCase(header.get(4).trim())
                || !"name".equalsIgnoreCase(header.get(5).trim())) {
            throw new IllegalStateException("Municipalities CSV must have headers: latitud,longitud,code,id,state_id,name");
        }

        List<MunicipalityCsvRow> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < records.size(); rowIndex++) {
            List<String> record = records.get(rowIndex);
            if (record.size() != 6) {
                throw new IllegalStateException("Invalid municipality CSV row " + (rowIndex + 1)
                        + ": expected 6 columns but got " + record.size());
            }

            String code = record.get(2).trim();
            String id = record.get(3).trim();
            String stateId = record.get(4).trim();
            String name = record.get(5).trim();
            if (code.isBlank() || id.isBlank() || stateId.isBlank() || name.isBlank()) {
                throw new IllegalStateException("Municipality CSV row " + (rowIndex + 1)
                        + " has blank code, id, state_id, or name");
            }

            rows.add(new MunicipalityCsvRow(
                    id,
                    code,
                    name,
                    stateId,
                    parseDecimal(record.get(0)),
                    parseDecimal(record.get(1))
            ));
        }

        return rows;
    }

    private List<List<String>> readCsvResource() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(CSV_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing CSV resource: " + CSV_RESOURCE);
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
            return records;
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

    private BigDecimal parseDecimal(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : new BigDecimal(trimmed);
    }

    private String stripBom(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }

    private void insertMunicipalities(Connection connection, List<MunicipalityCsvRow> rows) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into municipalities (id, code, name, state_id, latitude, longitude, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """)) {
            for (MunicipalityCsvRow row : rows) {
                statement.setString(1, row.id());
                statement.setString(2, row.code());
                statement.setString(3, row.name());
                statement.setString(4, row.stateId());
                setNullableDecimal(statement, 5, row.latitude());
                setNullableDecimal(statement, 6, row.longitude());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void setNullableDecimal(PreparedStatement statement, int parameterIndex, BigDecimal value) throws Exception {
        if (value == null) {
            statement.setNull(parameterIndex, Types.DECIMAL);
        } else {
            statement.setBigDecimal(parameterIndex, value);
        }
    }

    private record MunicipalityCsvRow(
            String id,
            String code,
            String name,
            String stateId,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }
}

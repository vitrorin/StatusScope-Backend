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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class V3__seed_diseases_and_symptoms extends BaseJavaMigration {

    private static final String CSV_RESOURCE = "data/diseases/diseases_and_specialties.csv";

    private static final Map<String, Integer> LEGACY_DISEASE_IDS = Map.of(
            "Influenza", 1,
            "Tuberculosis", 2,
            "Measles", 3,
            "COVID-19", 4
    );

    private static final Map<String, String> LEGACY_DISEASE_CODES = Map.of(
            "Influenza", "FLU",
            "Tuberculosis", "TB",
            "Measles", "MEASLES",
            "COVID-19", "COVID"
    );

    @Override
    public void migrate(Context context) throws Exception {
        CsvData csvData = loadCsvData();
        Connection connection = context.getConnection();
        Map<String, String> specialtyIdsByName = loadSpecialtyIdsByName(connection);

        clearDiseaseCatalog(connection);
        insertSymptoms(connection, csvData.symptoms());
        DiseaseSeedResult diseaseSeedResult = insertDiseases(connection, csvData.rows(), specialtyIdsByName);
        insertDiseaseSpecialties(connection, diseaseSeedResult.diseaseIdsByName(), diseaseSeedResult.specialtyIdsByDiseaseName());
        insertDiseaseSymptoms(connection, csvData.rows(), csvData.symptomIdsByName(), diseaseSeedResult.diseaseIdsByName());
    }

    private void clearDiseaseCatalog(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("drop table if exists disease_category_links");
            statement.executeUpdate("drop table if exists disease_categories");
            statement.executeUpdate("delete from alerts");
            statement.executeUpdate("delete from outbreaks");
            statement.executeUpdate("delete from diagnoses where event_id in (select id from events)");
            statement.executeUpdate("update patient_evaluations set event_id = null");
            statement.executeUpdate("delete from events");
            statement.executeUpdate("delete from evaluation_differential_diagnoses");
            statement.executeUpdate("delete from disease_symptoms");
            statement.executeUpdate("delete from disease_specialties");
            statement.executeUpdate("delete from symptoms");
            statement.executeUpdate("delete from diseases");
        }
    }

    private CsvData loadCsvData() throws Exception {
        List<List<String>> records = readCsvResource();
        if (records.size() < 2) {
            throw new IllegalStateException("Disease CSV must contain a header and at least one disease row");
        }

        List<String> header = records.get(0);
        String firstHeader = stripBom(header.get(0)).trim();
        String lastHeader = header.get(header.size() - 1).trim();
        if (!"prognosis".equalsIgnoreCase(firstHeader) || !"especialidad".equalsIgnoreCase(lastHeader)) {
            throw new IllegalStateException("Disease CSV must start with prognosis and end with especialidad");
        }

        List<String> symptoms = new ArrayList<>();
        Map<String, String> symptomIdsByName = new LinkedHashMap<>();
        for (int index = 1; index < header.size() - 1; index++) {
            String symptom = header.get(index).trim();
            symptoms.add(symptom);
            symptomIdsByName.put(symptom, sequentialId("61000000", index));
        }

        List<DiseaseCsvRow> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < records.size(); rowIndex++) {
            List<String> record = records.get(rowIndex);
            if (record.size() != header.size()) {
                throw new IllegalStateException("Invalid disease CSV row " + (rowIndex + 1) + ": expected "
                        + header.size() + " columns but got " + record.size());
            }

            String diseaseName = record.get(0).trim();
            String specialtyName = record.get(record.size() - 1).trim();
            if (diseaseName.isBlank() || specialtyName.isBlank()) {
                throw new IllegalStateException("Disease CSV row " + (rowIndex + 1) + " has blank disease or specialty");
            }

            List<String> activeSymptoms = new ArrayList<>();
            for (int columnIndex = 1; columnIndex < record.size() - 1; columnIndex++) {
                if ("1".equals(record.get(columnIndex).trim())) {
                    activeSymptoms.add(header.get(columnIndex).trim());
                }
            }

            rows.add(new DiseaseCsvRow(diseaseName, specialtyName, activeSymptoms));
        }

        return new CsvData(symptoms, symptomIdsByName, rows);
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

    private String stripBom(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }

    private Map<String, String> loadSpecialtyIdsByName(Connection connection) throws Exception {
        Map<String, String> specialtyIdsByName = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("select id, name from specialties");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                specialtyIdsByName.put(resultSet.getString("name"), resultSet.getString("id"));
            }
        }
        return specialtyIdsByName;
    }

    private void insertSymptoms(Connection connection, List<String> symptoms) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into symptoms (id, code, name, created_at, updated_at)
                values (?, ?, ?, current_timestamp, current_timestamp)
                """)) {
            for (int index = 0; index < symptoms.size(); index++) {
                statement.setString(1, sequentialId("61000000", index + 1));
                statement.setString(2, "SYM%06d".formatted(index + 1));
                statement.setString(3, symptoms.get(index));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private DiseaseSeedResult insertDiseases(
            Connection connection,
            List<DiseaseCsvRow> rows,
            Map<String, String> specialtyIdsByName
    ) throws Exception {
        Map<String, String> diseaseIdsByName = new LinkedHashMap<>();
        Map<String, String> specialtyIdsByDiseaseName = new LinkedHashMap<>();
        Set<String> usedCodes = new HashSet<>();
        int nextDiseaseNumber = 5;

        try (PreparedStatement statement = connection.prepareStatement("""
                insert into diseases (id, code, name, specialty_id, created_at, updated_at)
                values (?, ?, ?, ?, current_timestamp, current_timestamp)
                """)) {
            for (DiseaseCsvRow row : rows) {
                String diseaseId;
                if (LEGACY_DISEASE_IDS.containsKey(row.diseaseName())) {
                    diseaseId = sequentialId("60000000", LEGACY_DISEASE_IDS.get(row.diseaseName()));
                } else {
                    diseaseId = sequentialId("60000000", nextDiseaseNumber++);
                }

                String specialtyId = specialtyIdsByName.get(row.specialtyName());
                if (specialtyId == null) {
                    throw new IllegalStateException("Missing specialty in seed data: " + row.specialtyName());
                }

                String diseaseCode = LEGACY_DISEASE_CODES.getOrDefault(
                        row.diseaseName(),
                        uniqueCode(row.diseaseName(), usedCodes)
                );
                if (!usedCodes.add(diseaseCode)) {
                    diseaseCode = uniqueCode(row.diseaseName(), usedCodes);
                }

                diseaseIdsByName.put(row.diseaseName(), diseaseId);
                specialtyIdsByDiseaseName.put(row.diseaseName(), specialtyId);

                statement.setString(1, diseaseId);
                statement.setString(2, diseaseCode);
                statement.setString(3, row.diseaseName());
                statement.setString(4, specialtyId);
                statement.addBatch();
            }
            statement.executeBatch();
        }

        return new DiseaseSeedResult(diseaseIdsByName, specialtyIdsByDiseaseName);
    }

    private void insertDiseaseSpecialties(
            Connection connection,
            Map<String, String> diseaseIdsByName,
            Map<String, String> specialtyIdsByDiseaseName
    ) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into disease_specialties (disease_id, specialty_id)
                values (?, ?)
                """)) {
            for (Map.Entry<String, String> entry : diseaseIdsByName.entrySet()) {
                statement.setString(1, entry.getValue());
                statement.setString(2, specialtyIdsByDiseaseName.get(entry.getKey()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertDiseaseSymptoms(
            Connection connection,
            List<DiseaseCsvRow> rows,
            Map<String, String> symptomIdsByName,
            Map<String, String> diseaseIdsByName
    ) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into disease_symptoms (disease_id, symptom_id)
                values (?, ?)
                """)) {
            for (DiseaseCsvRow row : rows) {
                String diseaseId = diseaseIdsByName.get(row.diseaseName());
                for (String symptom : row.activeSymptoms()) {
                    statement.setString(1, diseaseId);
                    statement.setString(2, symptomIdsByName.get(symptom));
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private String uniqueCode(String name, Set<String> usedCodes) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.isBlank()) {
            base = "DISEASE";
        }

        int suffix = 1;
        while (true) {
            String suffixText = suffix == 1 ? "" : "_" + suffix;
            int maxBaseLength = 32 - suffixText.length();
            String candidate = base.length() > maxBaseLength
                    ? base.substring(0, maxBaseLength).replaceAll("_+$", "") + suffixText
                    : base + suffixText;
            if (!usedCodes.contains(candidate)) {
                return candidate;
            }
            suffix++;
        }
    }

    private static String sequentialId(String prefix, int number) {
        return "%s-0000-0000-0000-%012d".formatted(prefix, number);
    }

    private record CsvData(
            List<String> symptoms,
            Map<String, String> symptomIdsByName,
            List<DiseaseCsvRow> rows
    ) {
    }

    private record DiseaseCsvRow(String diseaseName, String specialtyName, List<String> activeSymptoms) {
    }

    private record DiseaseSeedResult(
            Map<String, String> diseaseIdsByName,
            Map<String, String> specialtyIdsByDiseaseName
    ) {
    }
}

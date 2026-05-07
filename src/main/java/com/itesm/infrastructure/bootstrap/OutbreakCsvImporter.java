package com.itesm.infrastructure.bootstrap;

import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import com.itesm.infrastructure.persistence.entity.MunicipalityEntity;
import com.itesm.infrastructure.persistence.entity.OutbreakEntity;
import com.itesm.infrastructure.persistence.entity.StateEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@ApplicationScoped
public class OutbreakCsvImporter {

    private static final String MANAGED_ID_PREFIX = "72000000-";
    private static final String MUNICIPAL_OUTBREAKS_RESOURCE = "data/outbreaks/municipal_outbreaks.csv";
    private static final String STATE_OUTBREAKS_RESOURCE = "data/outbreaks/state_outbreaks.csv";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDateTime STATE_OUTBREAK_STARTED_AT = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Inject
    EntityManager entityManager;

    @Transactional
    public ImportSummary importMunicipalOutbreaks() {
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

        Map<String, DiseaseEntity> diseasesByName = loadDiseasesByNormalizedName();
        Map<UUID, MunicipalityEntity> municipalitiesById = loadMunicipalitiesById();
        Map<OutbreakKey, OutbreakEntity> managedOutbreaksByKey = loadManagedMunicipalOutbreaksByKey();

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        Set<OutbreakKey> seenKeys = new HashSet<>();

        for (int index = 0; index < csvData.rows().size(); index++) {
            Map<String, String> record = csvData.rows().get(index);
            String scope = required(record, "scope", MUNICIPAL_OUTBREAKS_RESOURCE, index);
            if (!OutbreakEntity.SCOPE_MUNICIPALITY.equals(scope)) {
                throw new IllegalStateException("Only MUNICIPALITY rows are supported in "
                        + MUNICIPAL_OUTBREAKS_RESOURCE + " row " + (index + 2));
            }

            UUID municipalityId = parseUuid(required(record, "municipality_id", MUNICIPAL_OUTBREAKS_RESOURCE, index),
                    "municipality_id", index, MUNICIPAL_OUTBREAKS_RESOURCE);
            MunicipalityEntity municipality = municipalitiesById.get(municipalityId);
            if (municipality == null) {
                throw new IllegalStateException("Unknown municipality_id in " + MUNICIPAL_OUTBREAKS_RESOURCE
                        + " row " + (index + 2) + ": " + municipalityId);
            }

            DiseaseEntity disease = diseaseByName(
                    diseasesByName,
                    required(record, "disease_name", MUNICIPAL_OUTBREAKS_RESOURCE, index),
                    MUNICIPAL_OUTBREAKS_RESOURCE,
                    index
            );
            String confirmationStatus = required(record, "confirmation_status", MUNICIPAL_OUTBREAKS_RESOURCE, index);
            OutbreakKey key = new OutbreakKey(
                    OutbreakEntity.SCOPE_MUNICIPALITY,
                    municipalityId,
                    disease.getId(),
                    confirmationStatus
            );
            if (!seenKeys.add(key)) {
                throw new IllegalStateException("Duplicate municipal outbreak key in "
                        + MUNICIPAL_OUTBREAKS_RESOURCE + " row " + (index + 2));
            }

            OutbreakEntity outbreak = managedOutbreaksByKey.get(key);
            if (outbreak == null) {
                outbreak = new OutbreakEntity();
                outbreak.setId(deterministicId(key));
                outbreak.setScope(OutbreakEntity.SCOPE_MUNICIPALITY);
                outbreak.setDisease(disease);
                outbreak.setMunicipality(municipality);
                outbreak.setState(null);
                applyMunicipalRecord(outbreak, record, index);
                entityManager.persist(outbreak);
                managedOutbreaksByKey.put(key, outbreak);
                created++;
                continue;
            }

            if (applyMunicipalRecord(outbreak, record, index)) {
                updated++;
            } else {
                unchanged++;
            }
        }

        int ended = closeMissingManagedOutbreaks(managedOutbreaksByKey, seenKeys);
        return new ImportSummary(created, updated, unchanged, ended, seenKeys.size());
    }

    @Transactional
    public ImportSummary importStateOutbreaks() {
        CsvData csvData = readCsvResource(STATE_OUTBREAKS_RESOURCE);
        csvData.requireHeaders("scope", "estado", "disease_name", "case_count", "confirmation_status", "status");

        Map<String, DiseaseEntity> diseasesByName = loadDiseasesByNormalizedName();
        Map<String, StateEntity> statesByName = loadStatesByNormalizedName();
        Map<OutbreakKey, OutbreakEntity> managedOutbreaksByKey = loadManagedStateOutbreaksByKey();

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        Set<OutbreakKey> seenKeys = new HashSet<>();

        for (int index = 0; index < csvData.rows().size(); index++) {
            Map<String, String> record = csvData.rows().get(index);
            String scope = required(record, "scope", STATE_OUTBREAKS_RESOURCE, index);
            if (!OutbreakEntity.SCOPE_STATE.equals(scope)) {
                throw new IllegalStateException("Only STATE rows are supported in "
                        + STATE_OUTBREAKS_RESOURCE + " row " + (index + 2));
            }

            String stateName = required(record, "estado", STATE_OUTBREAKS_RESOURCE, index);
            StateEntity state = statesByName.get(normalize(stateName));
            if (state == null) {
                throw new IllegalStateException("Unknown state in " + STATE_OUTBREAKS_RESOURCE
                        + " row " + (index + 2) + ": " + stateName);
            }

            DiseaseEntity disease = diseaseByName(
                    diseasesByName,
                    required(record, "disease_name", STATE_OUTBREAKS_RESOURCE, index),
                    STATE_OUTBREAKS_RESOURCE,
                    index
            );
            String confirmationStatus = required(record, "confirmation_status", STATE_OUTBREAKS_RESOURCE, index);
            OutbreakKey key = new OutbreakKey(
                    OutbreakEntity.SCOPE_STATE,
                    state.getId(),
                    disease.getId(),
                    confirmationStatus
            );
            if (!seenKeys.add(key)) {
                throw new IllegalStateException("Duplicate state outbreak key in "
                        + STATE_OUTBREAKS_RESOURCE + " row " + (index + 2));
            }

            OutbreakEntity outbreak = managedOutbreaksByKey.get(key);
            if (outbreak == null) {
                outbreak = new OutbreakEntity();
                outbreak.setId(deterministicId(key));
                outbreak.setScope(OutbreakEntity.SCOPE_STATE);
                outbreak.setDisease(disease);
                outbreak.setMunicipality(null);
                outbreak.setState(state);
                applyStateRecord(outbreak, record, index);
                entityManager.persist(outbreak);
                managedOutbreaksByKey.put(key, outbreak);
                created++;
                continue;
            }

            if (applyStateRecord(outbreak, record, index)) {
                updated++;
            } else {
                unchanged++;
            }
        }

        int ended = closeMissingManagedOutbreaks(managedOutbreaksByKey, seenKeys);
        return new ImportSummary(created, updated, unchanged, ended, seenKeys.size());
    }

    private boolean applyMunicipalRecord(OutbreakEntity outbreak, Map<String, String> record, int rowIndex) {
        int caseCount = parseInt(required(record, "case_count", MUNICIPAL_OUTBREAKS_RESOURCE, rowIndex),
                "case_count", rowIndex, MUNICIPAL_OUTBREAKS_RESOURCE);
        String confirmationStatus = required(record, "confirmation_status", MUNICIPAL_OUTBREAKS_RESOURCE, rowIndex);
        String status = required(record, "status", MUNICIPAL_OUTBREAKS_RESOURCE, rowIndex);
        LocalDateTime startedAt = parseDateTime(required(record, "started_at", MUNICIPAL_OUTBREAKS_RESOURCE, rowIndex),
                "started_at", rowIndex, MUNICIPAL_OUTBREAKS_RESOURCE);
        LocalDateTime endedAt = parseNullableDateTime(record.get("ended_at"), "ended_at", rowIndex,
                MUNICIPAL_OUTBREAKS_RESOURCE);

        boolean changed = false;
        changed |= setIfChangedCaseCount(outbreak, caseCount);
        changed |= setIfChangedString(outbreak.getConfirmationStatus(), confirmationStatus, outbreak::setConfirmationStatus);
        changed |= setIfChangedString(outbreak.getStatus(), status, outbreak::setStatus);
        changed |= setIfChangedDateTime(outbreak.getStartedAt(), startedAt, outbreak::setStartedAt);
        changed |= setIfChangedDateTime(outbreak.getEndedAt(), endedAt, outbreak::setEndedAt);
        return updateTimestamps(outbreak, changed);
    }

    private boolean applyStateRecord(OutbreakEntity outbreak, Map<String, String> record, int rowIndex) {
        int caseCount = parseInt(required(record, "case_count", STATE_OUTBREAKS_RESOURCE, rowIndex),
                "case_count", rowIndex, STATE_OUTBREAKS_RESOURCE);
        String confirmationStatus = required(record, "confirmation_status", STATE_OUTBREAKS_RESOURCE, rowIndex);
        String status = required(record, "status", STATE_OUTBREAKS_RESOURCE, rowIndex);

        boolean changed = false;
        changed |= setIfChangedCaseCount(outbreak, caseCount);
        changed |= setIfChangedString(outbreak.getConfirmationStatus(), confirmationStatus, outbreak::setConfirmationStatus);
        changed |= setIfChangedString(outbreak.getStatus(), status, outbreak::setStatus);
        changed |= setIfChangedDateTime(outbreak.getStartedAt(), STATE_OUTBREAK_STARTED_AT, outbreak::setStartedAt);
        changed |= setIfChangedDateTime(outbreak.getEndedAt(), null, outbreak::setEndedAt);
        return updateTimestamps(outbreak, changed);
    }

    private boolean updateTimestamps(OutbreakEntity outbreak, boolean changed) {
        if (outbreak.getCreatedAt() == null) {
            outbreak.setCreatedAt(LocalDateTime.now());
            changed = true;
        }
        if (outbreak.getUpdatedAt() == null || changed) {
            outbreak.setUpdatedAt(LocalDateTime.now());
        }
        return changed;
    }

    private int closeMissingManagedOutbreaks(
            Map<OutbreakKey, OutbreakEntity> managedOutbreaksByKey,
            Set<OutbreakKey> seenKeys
    ) {
        LocalDateTime now = LocalDateTime.now();
        int ended = 0;
        for (Map.Entry<OutbreakKey, OutbreakEntity> entry : managedOutbreaksByKey.entrySet()) {
            if (seenKeys.contains(entry.getKey())) {
                continue;
            }
            OutbreakEntity outbreak = entry.getValue();
            if (!"ENDED".equals(outbreak.getStatus())) {
                outbreak.setStatus("ENDED");
                if (outbreak.getEndedAt() == null) {
                    outbreak.setEndedAt(now);
                }
                outbreak.setUpdatedAt(now);
                ended++;
            }
        }
        return ended;
    }

    private DiseaseEntity diseaseByName(
            Map<String, DiseaseEntity> diseasesByName,
            String diseaseName,
            String resource,
            int rowIndex
    ) {
        DiseaseEntity disease = diseasesByName.get(normalize(diseaseName));
        if (disease == null) {
            throw new IllegalStateException("Unknown disease in " + resource
                    + " row " + (rowIndex + 2) + ": " + diseaseName);
        }
        return disease;
    }

    private Map<String, DiseaseEntity> loadDiseasesByNormalizedName() {
        Map<String, DiseaseEntity> diseases = new HashMap<>();
        for (DiseaseEntity disease : entityManager
                .createQuery("select d from DiseaseEntity d", DiseaseEntity.class)
                .getResultList()) {
            diseases.put(normalize(disease.getName()), disease);
        }
        return diseases;
    }

    private Map<UUID, MunicipalityEntity> loadMunicipalitiesById() {
        Map<UUID, MunicipalityEntity> municipalities = new HashMap<>();
        for (MunicipalityEntity municipality : entityManager
                .createQuery("select m from MunicipalityEntity m", MunicipalityEntity.class)
                .getResultList()) {
            municipalities.put(municipality.getId(), municipality);
        }
        return municipalities;
    }

    private Map<String, StateEntity> loadStatesByNormalizedName() {
        Map<String, StateEntity> states = new HashMap<>();
        for (StateEntity state : entityManager
                .createQuery("select s from StateEntity s", StateEntity.class)
                .getResultList()) {
            states.put(normalize(state.getName()), state);
        }
        putStateAlias(states, "coahuila", "coahuila de zaragoza");
        putStateAlias(states, "michoacan", "michoacan de ocampo");
        putStateAlias(states, "veracruz", "veracruz de ignacio de la llave");
        putStateAlias(states, "estado de mexico", "mexico");
        return states;
    }

    private void putStateAlias(Map<String, StateEntity> states, String alias, String canonicalName) {
        StateEntity canonicalState = states.get(normalize(canonicalName));
        if (canonicalState != null) {
            states.put(normalize(alias), canonicalState);
        }
    }

    private Map<OutbreakKey, OutbreakEntity> loadManagedMunicipalOutbreaksByKey() {
        Map<OutbreakKey, OutbreakEntity> outbreaks = new HashMap<>();
        for (OutbreakEntity outbreak : entityManager
                .createQuery("""
                        select o
                        from OutbreakEntity o
                        join fetch o.disease d
                        join fetch o.municipality m
                        where o.scope = :scope
                        """, OutbreakEntity.class)
                .setParameter("scope", OutbreakEntity.SCOPE_MUNICIPALITY)
                .getResultList()) {
            if (!outbreak.getId().toString().startsWith(MANAGED_ID_PREFIX)) {
                continue;
            }
            outbreaks.put(new OutbreakKey(
                    OutbreakEntity.SCOPE_MUNICIPALITY,
                    outbreak.getMunicipality().getId(),
                    outbreak.getDisease().getId(),
                    outbreak.getConfirmationStatus()
            ), outbreak);
        }
        return outbreaks;
    }

    private Map<OutbreakKey, OutbreakEntity> loadManagedStateOutbreaksByKey() {
        Map<OutbreakKey, OutbreakEntity> outbreaks = new HashMap<>();
        for (OutbreakEntity outbreak : entityManager
                .createQuery("""
                        select o
                        from OutbreakEntity o
                        join fetch o.disease d
                        join fetch o.state s
                        where o.scope = :scope
                        """, OutbreakEntity.class)
                .setParameter("scope", OutbreakEntity.SCOPE_STATE)
                .getResultList()) {
            if (!outbreak.getId().toString().startsWith(MANAGED_ID_PREFIX)) {
                continue;
            }
            outbreaks.put(new OutbreakKey(
                    OutbreakEntity.SCOPE_STATE,
                    outbreak.getState().getId(),
                    outbreak.getDisease().getId(),
                    outbreak.getConfirmationStatus()
            ), outbreak);
        }
        return outbreaks;
    }

    private CsvData readCsvResource(String resource) {
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
        } catch (Exception e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Could not read CSV resource: " + resource, e);
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

    private String required(Map<String, String> record, String key, String resource, int rowIndex) {
        String value = record.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + key + " in " + resource + " row " + (rowIndex + 2));
        }
        return value.trim();
    }

    private UUID parseUuid(String value, String field, int rowIndex, String resource) {
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid " + field + " in " + resource
                    + " row " + (rowIndex + 2) + ": " + value, e);
        }
    }

    private int parseInt(String value, String field, int rowIndex, String resource) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid " + field + " in " + resource
                    + " row " + (rowIndex + 2) + ": " + value, e);
        }
    }

    private LocalDateTime parseDateTime(String value, String field, int rowIndex, String resource) {
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid " + field + " in " + resource
                    + " row " + (rowIndex + 2) + ": " + value, e);
        }
    }

    private LocalDateTime parseNullableDateTime(String value, String field, int rowIndex, String resource) {
        return value == null || value.isBlank() ? null : parseDateTime(value, field, rowIndex, resource);
    }

    private UUID deterministicId(OutbreakKey key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.toStableString().getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(hash);
            return UUID.fromString("72000000-" + hex.substring(0, 4)
                    + "-" + hex.substring(4, 8)
                    + "-" + hex.substring(8, 12)
                    + "-" + hex.substring(12, 24));
        } catch (Exception e) {
            throw new IllegalStateException("Could not create deterministic outbreak id", e);
        }
    }

    private boolean setIfChangedCaseCount(OutbreakEntity outbreak, int value) {
        if (outbreak.getCaseCount() == value) {
            return false;
        }
        outbreak.setCaseCount(value);
        return true;
    }

    private boolean setIfChangedString(String current, String next, Consumer<String> setter) {
        if (Objects.equals(current, next)) {
            return false;
        }
        setter.accept(next);
        return true;
    }

    private boolean setIfChangedDateTime(LocalDateTime current, LocalDateTime next, Consumer<LocalDateTime> setter) {
        if (Objects.equals(current, next)) {
            return false;
        }
        setter.accept(next);
        return true;
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

    public record ImportSummary(int created, int updated, int unchanged, int ended, int activeRows) {
    }

    private record OutbreakKey(String scope, UUID locationId, UUID diseaseId, String confirmationStatus) {
        String toStableString() {
            return scope + "|" + locationId + "|" + diseaseId + "|" + confirmationStatus;
        }
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
}

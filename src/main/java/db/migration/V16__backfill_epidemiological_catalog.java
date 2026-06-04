package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.Statement;

public class V16__backfill_epidemiological_catalog extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        seedStates(context);
        seedSpecialties(context);

        if (isTableEmpty(context, "municipalities")) {
            new V1__seed_mexico_municipalities().migrate(context);
        }

        if (isTableEmpty(context, "diseases") || isTableEmpty(context, "symptoms")) {
            new V3__seed_diseases_and_symptoms().migrate(context);
        }

        if (isTableEmpty(context, "outbreaks")) {
            new V4__seed_outbreaks().migrate(context);
        }
    }

    private void seedStates(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.executeUpdate("""
                    INSERT IGNORE INTO states (id, code, name, description, created_at, updated_at) VALUES
                    ('40000000-0000-0000-0000-000000000001', 'AGS',  'Aguascalientes',   'Estado de Aguascalientes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000002', 'BC',   'Baja California',  'Estado de Baja California', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000003', 'BCS',  'Baja California Sur', 'Estado de Baja California Sur', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000004', 'CAM',  'Campeche',         'Estado de Campeche', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000005', 'COA',  'Coahuila de Zaragoza', 'Estado de Coahuila de Zaragoza', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000006', 'COL',  'Colima',           'Estado de Colima', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000007', 'CHP',  'Chiapas',          'Estado de Chiapas', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000008', 'CHH',  'Chihuahua',        'Estado de Chihuahua', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000009', 'CDMX', 'Ciudad de Mexico', 'Entidad federativa Ciudad de Mexico', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000010', 'DUR',  'Durango',          'Estado de Durango', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000011', 'GUA',  'Guanajuato',       'Estado de Guanajuato', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000012', 'GRO',  'Guerrero',         'Estado de Guerrero', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000013', 'HID',  'Hidalgo',          'Estado de Hidalgo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000014', 'JAL',  'Jalisco',          'Estado de Jalisco', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000015', 'MEX',  'Mexico',           'Estado de Mexico', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000016', 'MIC',  'Michoacan de Ocampo', 'Estado de Michoacan de Ocampo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000017', 'MOR',  'Morelos',          'Estado de Morelos', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000018', 'NAY',  'Nayarit',          'Estado de Nayarit', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000019', 'NL',   'Nuevo Leon',       'Estado de Nuevo Leon', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000020', 'OAX',  'Oaxaca',           'Estado de Oaxaca', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000021', 'PUE',  'Puebla',           'Estado de Puebla', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000022', 'QRO',  'Queretaro',        'Estado de Queretaro', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000023', 'ROO',  'Quintana Roo',     'Estado de Quintana Roo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000024', 'SLP',  'San Luis Potosi',  'Estado de San Luis Potosi', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000025', 'SIN',  'Sinaloa',          'Estado de Sinaloa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000026', 'SON',  'Sonora',           'Estado de Sonora', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000027', 'TAB',  'Tabasco',          'Estado de Tabasco', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000028', 'TAM',  'Tamaulipas',       'Estado de Tamaulipas', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000029', 'TLA',  'Tlaxcala',         'Estado de Tlaxcala', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000030', 'VER',  'Veracruz de Ignacio de la Llave', 'Estado de Veracruz de Ignacio de la Llave', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000031', 'YUC',  'Yucatan',          'Estado de Yucatan', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('40000000-0000-0000-0000-000000000032', 'ZAC',  'Zacatecas',        'Estado de Zacatecas', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """);
        }
    }

    private void seedSpecialties(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.executeUpdate("""
                    INSERT IGNORE INTO specialties (id, code, name, description, created_at, updated_at) VALUES
                    ('50000000-0000-0000-0000-000000000001', 'GEN',  'General Medicine',  'Primary care and general practice', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000002', 'CARD', 'Cardiology', 'Heart and circulatory conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000003', 'PULM', 'Pulmonology', 'Respiratory system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000004', 'INF',  'Infectious Diseases', 'Infectious and communicable diseases', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000005', 'PED',  'Pediatrics', 'Care of infants, children, and adolescents', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000006', 'GASTRO', 'Gastroenterology', 'Digestive system and gastrointestinal conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000007', 'NEURO', 'Neurology', 'Brain, nervous system, and neurologic conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000008', 'ENT',  'Otolaryngology', 'Ear, nose, throat, head and neck conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000009', 'DERM', 'Dermatology', 'Skin, hair, and nail conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000010', 'GYN',  'Gynecology', 'Female reproductive health conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000011', 'URO',  'Urology', 'Urinary tract and male reproductive conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000012', 'FAM',  'Family Medicine', 'Comprehensive primary care across ages', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000013', 'OPH',  'Ophthalmology', 'Eye and vision conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('50000000-0000-0000-0000-000000000014', 'DENT', 'Dentistry', 'Oral and dental conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """);
        }
    }

    private boolean isTableEmpty(Context context, String tableName) throws Exception {
        try (PreparedStatement statement = context.getConnection().prepareStatement("select count(*) from " + tableName);
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1) == 0;
        }
    }
}

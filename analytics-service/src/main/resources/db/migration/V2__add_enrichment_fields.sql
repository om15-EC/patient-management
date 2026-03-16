ALTER TABLE patient_events
    ADD COLUMN diagnosis TEXT,
    ADD COLUMN country TEXT,
    ADD COLUMN disease_symptoms TEXT,
    ADD COLUMN disease_treatments TEXT,
    ADD COLUMN country_region TEXT,
    ADD COLUMN country_population BIGINT;
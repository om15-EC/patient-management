ALTER TABLE patient_events
    DROP COLUMN IF EXISTS disease_symptoms,
    DROP COLUMN IF EXISTS disease_treatments,
    ADD COLUMN IF NOT EXISTS disease_description TEXT;
CREATE TABLE patient_events
(
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    patient_id VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    event_type VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_patient_events PRIMARY KEY (id)
);

CREATE INDEX idx_patient_events_patient_id ON patient_events (patient_id);
CREATE INDEX idx_patient_events_event_type ON patient_events (event_type);
CREATE INDEX idx_patient_events_created_at ON patient_events (created_at);
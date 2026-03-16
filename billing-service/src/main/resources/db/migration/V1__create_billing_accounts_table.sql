CREATE TABLE billing_accounts (
                                  id             UUID           NOT NULL DEFAULT gen_random_uuid(),
                                  patient_id     VARCHAR(255)   NOT NULL,
                                  status         VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
                                  billing_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                                  created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
                                  updated_at     TIMESTAMP      NOT NULL DEFAULT NOW(),

                                  CONSTRAINT pk_billing_accounts PRIMARY KEY (id),
                                  CONSTRAINT uq_billing_patient_id UNIQUE (patient_id),
                                  CONSTRAINT chk_billing_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_billing_patient_id ON billing_accounts (patient_id);
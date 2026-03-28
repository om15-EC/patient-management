CREATE TABLE outbox_events (
                               id             UUID         NOT NULL DEFAULT gen_random_uuid(),
                               aggregate_type VARCHAR(255) NOT NULL,
                               aggregate_id   UUID         NOT NULL,
                               event_type     VARCHAR(255) NOT NULL,
                               payload        TEXT         NOT NULL,
                               published      BOOLEAN      NOT NULL DEFAULT FALSE,
                               created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
                               published_at   TIMESTAMP,

                               CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_published ON outbox_events (published);
CREATE INDEX idx_outbox_created_at ON outbox_events (created_at);
CREATE TABLE users (
                       id         UUID         NOT NULL DEFAULT gen_random_uuid(),
                       email      VARCHAR(255) NOT NULL,
                       password   VARCHAR(255) NOT NULL,
                       role       VARCHAR(50)  NOT NULL,
                       enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),

                       CONSTRAINT pk_users        PRIMARY KEY (id),
                       CONSTRAINT uq_users_email  UNIQUE (email),
                       CONSTRAINT chk_users_role  CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
);

-- Index on email for fast lookups during login
CREATE INDEX idx_users_email ON users (email);
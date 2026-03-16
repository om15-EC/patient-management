CREATE TABLE IF NOT EXISTS patient
(
    id              UUID         NOT NULL,
    address         VARCHAR(255) NOT NULL,
    date_of_birth   DATE         NOT NULL,
    email           VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    registered_date DATE         NOT NULL,

    CONSTRAINT pk_patient PRIMARY KEY (id),
    CONSTRAINT ukbawli8xm92f30ei6x9p3h8eju UNIQUE (email)
);
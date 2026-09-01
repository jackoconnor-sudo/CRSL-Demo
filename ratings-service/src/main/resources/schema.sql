-- Applied by spring.datasource.initialization on start up.
-- There is no migration tool in this service; the desk DBA applies the same file by hand
-- against the staging and production schemas.

CREATE TABLE IF NOT EXISTS ratings (
    issuer_id     VARCHAR(16) PRIMARY KEY,
    issuer_name   VARCHAR(128) NOT NULL,
    grade         VARCHAR(8) NOT NULL,
    outlook       VARCHAR(16) NOT NULL,
    sector        VARCHAR(64) NOT NULL,
    last_reviewed DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS export_jobs (
    job_id     VARCHAR(36) PRIMARY KEY,
    format     VARCHAR(16) NOT NULL,
    desk       VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    state      VARCHAR(16) NOT NULL
);

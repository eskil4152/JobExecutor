CREATE TABLE JOBS (
    id UUID PRIMARY KEY,
    type VARCHAR(127) NOT NULL,
    payload TEXT NOT NULL,
    result TEXT,
    status VARCHAR(255) NOT NULL
        CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED')),
    created TIMESTAMP NOT NULL DEFAULT NOW(),
    started TIMESTAMP,
    finished TIMESTAMP
);
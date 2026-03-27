CREATE TABLE JOBS (
    id UUID PRIMARY KEY,
    type TEXT NOT NULL,
    payload TEXT NOT NULL,
    result TEXT,
    status TEXT NOT NULL
        CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED')),
    created TIMESTAMP NOT NULL DEFAULT NOW(),
    started TIMESTAMP,
    finished TIMESTAMP
);
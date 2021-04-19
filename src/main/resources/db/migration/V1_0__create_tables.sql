CREATE TABLE audit_event
(
    id       UUID PRIMARY KEY,
    what     VARCHAR(200) NOT NULL,
    occurred TIMESTAMP WITH TIME ZONE NOT NULL,
    who      VARCHAR(80),
    service  VARCHAR(200),
    details  JSON
);

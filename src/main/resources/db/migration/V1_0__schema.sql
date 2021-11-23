CREATE TABLE IF NOT EXISTS foaas (
    id          BIGSERIAL,
    subtitle    TEXT            NOT NULL,
    message     VARCHAR(200)    NOT NULL,
    PRIMARY KEY (id)
);

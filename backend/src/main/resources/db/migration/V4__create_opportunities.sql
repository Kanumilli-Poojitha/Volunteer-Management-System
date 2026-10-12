CREATE TABLE opportunities (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    title        VARCHAR(200)  NOT NULL,
    description  VARCHAR(2000),
    location     VARCHAR(200)  NOT NULL,
    event_date   DATE          NOT NULL,
    organizer_id BIGINT        NOT NULL,
    created_at   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_opportunities_organizer FOREIGN KEY (organizer_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_opportunities_event_date ON opportunities (event_date);

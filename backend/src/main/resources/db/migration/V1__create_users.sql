-- Baseline: the shape of the original Hibernate-generated table.
-- Existing databases skip this (baseline-version=1); fresh databases start here.
CREATE TABLE users (
    id       INT          NOT NULL AUTO_INCREMENT,
    name     VARCHAR(255),
    email    VARCHAR(255),
    password VARCHAR(255),
    role     VARCHAR(255),
    PRIMARY KEY (id)
);

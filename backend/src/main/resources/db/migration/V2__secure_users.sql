-- Tidy legacy data so that constraints can be added without losing any user rows.
UPDATE users SET name = 'Unnamed user' WHERE name IS NULL OR TRIM(name) = '';
UPDATE users SET email = LOWER(TRIM(email));

-- SECURITY: the legacy API let anyone self-register with any role string (including "Admin").
-- Only ORGANIZER is carried over; everything else becomes VOLUNTEER. Create the real admin with
-- the BOOTSTRAP_ADMIN_* variables described in the README.
UPDATE users
   SET role = CASE WHEN UPPER(TRIM(role)) = 'ORGANIZER' THEN 'ORGANIZER' ELSE 'VOLUNTEER' END;

-- Rows without a password get an impossible hash marker so nobody can log in as them.
UPDATE users SET password = '!' WHERE password IS NULL OR password = '';

-- NOTE: this fails (and Flyway stops the app) if legacy rows share an email address.
-- Resolve duplicates manually first; see README "Upgrading an existing database".
ALTER TABLE users
    MODIFY COLUMN id       BIGINT       NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN name     VARCHAR(255) NOT NULL,
    MODIFY COLUMN email    VARCHAR(255) NOT NULL,
    MODIFY COLUMN password VARCHAR(255) NOT NULL,
    MODIFY COLUMN role     VARCHAR(20)  NOT NULL,
    ADD COLUMN status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD CONSTRAINT uk_users_email UNIQUE (email);

CREATE SCHEMA IF NOT EXISTS public;

CREATE TYPE account_type AS ENUM ('THERAPIST', 'GUARDIAN');
CREATE TYPE account_status AS ENUM ('PENDING_VERIFICATION', 'ACTIVE', 'DISABLED');
CREATE TYPE auth_provider AS ENUM ('PASSWORD', 'GOOGLE', 'APPLE');

CREATE TABLE accounts
(
    id         UUID                              DEFAULT gen_random_uuid(),
    type       account_type             NOT NULL,
    status     account_status           NOT NULL DEFAULT 'PENDING_VERIFICATION',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

CREATE TABLE auth_identities
(
    account_id       UUID                     NOT NULL,
    provider         auth_provider            NOT NULL,
    provider_user_id TEXT                     NOT NULL,
    password_hash    TEXT,
    email            TEXT                     NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_auth_identities_provider_user_id UNIQUE (provider, provider_user_id),
    CONSTRAINT uq_provider_email UNIQUE (provider, email),

    CONSTRAINT pk_auth_identities PRIMARY KEY (account_id),
    CONSTRAINT fk_auth_identities_accounts FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,

    CONSTRAINT ck_accounts_email_normalized
        CHECK (email = lower(trim(email)) AND email <> ''),
    CONSTRAINT ck_auth_identities_password_hash
        CHECK (
            (provider = 'PASSWORD'
                AND password_hash IS NOT NULL
                AND btrim(password_hash) <> '')
                OR
            (provider <> 'PASSWORD' AND password_hash IS NULL)
            )
);

CREATE TABLE therapists
(
    id                 UUID,
    account_id         UUID                     NOT NULL,
    first_name         TEXT                     NOT NULL,
    last_name          TEXT                     NOT NULL,
    license_number     TEXT                     NOT NULL,
    professional_title TEXT                     NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_therapists PRIMARY KEY (id),

    CONSTRAINT fk_therapists_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,

    CONSTRAINT ck_therapists_first_name_not_blank CHECK (first_name = trim(first_name) AND first_name <> ''),
    CONSTRAINT ck_therapists_last_name_not_blank CHECK (last_name = trim(last_name) AND last_name <> ''),
    CONSTRAINT ck_therapists_license_number_normalized
        CHECK (
            license_number = trim(license_number)
                AND license_number <> ''
            ),
    CONSTRAINT ck_therapists_professional_title_not_blank
        CHECK (
            professional_title IS NULL
                OR trim(professional_title) <> ''
            ),

    CONSTRAINT uq_therapists_license_number UNIQUE (license_number)
);

CREATE TABLE guardians
(
    account_id UUID                     NOT NULL,
    first_name TEXT                     NOT NULL,
    last_name  TEXT                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_guardians_first_name_normalized
        CHECK (
            first_name = btrim(first_name)
                AND first_name <> ''
            ),
    CONSTRAINT ck_guardians_last_name_normalized
        CHECK (
            last_name = btrim(last_name)
                AND last_name <> ''
            ),


    CONSTRAINT pk_account_id PRIMARY KEY (account_id),
    CONSTRAINT fk_guardians_accounts FOREIGN KEY (account_id) references accounts (id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens
(
    id         UUID                              DEFAULT gen_random_uuid(),
    account_id UUID                     NOT NULL,
    token_hash TEXT                     NOT NULL UNIQUE,
    used       BOOLEAN                  NOT NULL DEFAULT false,
    expires_at TIMESTAMP                NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    constraint pk_password_reset_token PRIMARY KEY (id),
    constraint fk_password_reset_token_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);
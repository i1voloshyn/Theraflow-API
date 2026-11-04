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

    CONSTRAINT pk_auth_identities PRIMARY KEY (account_id),
    CONSTRAINT fk_auth_identities_accounts FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE ,

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
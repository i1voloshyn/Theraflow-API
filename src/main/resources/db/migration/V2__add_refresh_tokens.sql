CREATE TABLE refresh_tokens
(
    id         UUID                     NOT NULL DEFAULT gen_random_uuid(),
    account_id UUID                     NOT NULL,
    token_hash TEXT                     NOT NULL,
    revoked    BOOLEAN                  NOT NULL DEFAULT false,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_accounts FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_account_id ON refresh_tokens (account_id);

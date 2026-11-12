ALTER TABLE auth_identities
    ADD CONSTRAINT uq_provider_email UNIQUE (provider, email)
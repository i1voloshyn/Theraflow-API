-- One therapist can have many practice locations.
-- Each location belongs to exactly one therapist.
CREATE TABLE practice_locations
(
    id              UUID                     NOT NULL DEFAULT gen_random_uuid(),
    therapist_id    UUID                     NOT NULL,
    name            TEXT                     NOT NULL,
    city            TEXT                     NOT NULL,
    postal_code     TEXT                     NOT NULL,
    street          TEXT                     NOT NULL,
    building_number TEXT                     NOT NULL,
    unit_number     TEXT,
    phone_number    VARCHAR(16),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_practice_locations PRIMARY KEY (id),
    CONSTRAINT fk_practice_locations_therapist
        FOREIGN KEY (therapist_id) REFERENCES therapists (id) ON DELETE CASCADE,
    CONSTRAINT ck_practice_locations_name
        CHECK (name = btrim(name) AND name ~ '[^[:space:]]'),
    CONSTRAINT ck_practice_locations_city
        CHECK (city = btrim(city) AND city ~ '[^[:space:]]'),
    CONSTRAINT ck_practice_locations_postal_code
        CHECK (postal_code = btrim(postal_code) AND postal_code ~ '[^[:space:]]'),
    CONSTRAINT ck_practice_locations_street
        CHECK (street = btrim(street) AND street ~ '[^[:space:]]'),
    CONSTRAINT ck_practice_locations_building_number
        CHECK (building_number = btrim(building_number) AND building_number ~ '[^[:space:]]'),
    CONSTRAINT ck_practice_locations_unit_number
        CHECK (unit_number IS NULL OR (unit_number = btrim(unit_number) AND unit_number ~ '[^[:space:]]')),
    -- Store complete international numbers, e.g. +48587651234, never masked display text.
    CONSTRAINT ck_practice_locations_phone_number
        CHECK (phone_number IS NULL OR phone_number ~ '^\+[1-9][0-9]{1,14}$')
);

-- Supports future city queries; this migration does not enable API city sorting.
CREATE INDEX idx_practice_locations_city_id ON practice_locations (city, id);

CREATE INDEX idx_practice_locations_therapist_id ON practice_locations (therapist_id);

CREATE FUNCTION set_practice_location_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Use the statement time so separate updates in one transaction get fresh timestamps.
    NEW.updated_at := statement_timestamp();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_practice_locations_updated_at
    BEFORE UPDATE ON practice_locations
    FOR EACH ROW
    EXECUTE FUNCTION set_practice_location_updated_at();

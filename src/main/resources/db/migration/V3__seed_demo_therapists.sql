-- Fictional demo data: 30 active therapist accounts with password identities and profiles.
-- Emails: therapist01@example.com through therapist30@example.com.
-- Shared demo password: DemoTherapist123! (BCrypt, with Spring's encoder prefix).
-- This migration seeds every database using the default Flyway migration location.
DO $$
DECLARE
    demo RECORD;
    demo_account_id UUID;
    demo_email TEXT;
BEGIN
    FOR demo IN
        SELECT * FROM (VALUES
            (1, 'Warsaw', 'Poland'),
            (2, 'Berlin', 'Germany'),
            (3, 'Paris', 'France'),
            (4, 'Madrid', 'Spain'),
            (5, 'Rome', 'Italy'),
            (6, 'Lisbon', 'Portugal'),
            (7, 'Vienna', 'Austria'),
            (8, 'Prague', 'Czechia'),
            (9, 'Bratislava', 'Slovakia'),
            (10, 'Budapest', 'Hungary'),
            (11, 'Bucharest', 'Romania'),
            (12, 'Sofia', 'Bulgaria'),
            (13, 'Athens', 'Greece'),
            (14, 'Zagreb', 'Croatia'),
            (15, 'Ljubljana', 'Slovenia'),
            (16, 'Tallinn', 'Estonia'),
            (17, 'Riga', 'Latvia'),
            (18, 'Vilnius', 'Lithuania'),
            (19, 'Helsinki', 'Finland'),
            (20, 'Stockholm', 'Sweden'),
            (21, 'Oslo', 'Norway'),
            (22, 'Copenhagen', 'Denmark'),
            (23, 'Dublin', 'Ireland'),
            (24, 'London', 'United Kingdom'),
            (25, 'Amsterdam', 'Netherlands'),
            (26, 'Brussels', 'Belgium'),
            (27, 'Bern', 'Switzerland'),
            (28, 'Reykjavik', 'Iceland'),
            (29, 'Ottawa', 'Canada'),
            (30, 'Tokyo', 'Japan')
        ) AS locations(number, city, country)
    LOOP
        demo_account_id := gen_random_uuid();
        demo_email := 'therapist' || lpad(demo.number::TEXT, 2, '0') || '@example.com';

        INSERT INTO accounts (id, type, status)
        VALUES (demo_account_id, 'THERAPIST', 'ACTIVE');

        INSERT INTO auth_identities (account_id, provider, provider_user_id, password_hash, email)
        VALUES (
            demo_account_id,
            'PASSWORD',
            demo_email,
            '{bcrypt}$2y$10$yuC7zxraadUQ1C.OuH48MuHPcWWWMV5YiKmljRe/tK7HjgRUccgn6',
            demo_email
        );

        INSERT INTO therapists (id, account_id, first_name, last_name, license_number, professional_title)
        VALUES (
            gen_random_uuid(),
            demo_account_id,
            demo.city,
            demo.country,
            'DEMO-LICENSE-' || lpad(demo.number::TEXT, 3, '0'),
            'Therapist'
        );
    END LOOP;
END;
$$;

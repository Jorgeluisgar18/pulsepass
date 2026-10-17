-- ============================================================
-- PulsePass - Fixtures exclusivos para pruebas E2E manuales
-- ============================================================
--
-- IMPORTANTE:
-- Este script SOLO puede ejecutarse contra la base:
--
--     pulsepass_e2e
--
-- No ejecutar contra desarrollo, QA ni producción.
--
-- Las contraseñas de ADMIN, STAFF e INACTIVE_USER no se
-- almacenan en este archivo. Deben suministrarse mediante
-- variables psql definidas localmente.
-- ============================================================


-- ------------------------------------------------------------
-- Protección contra ejecución accidental en otra base
-- ------------------------------------------------------------

DO $$
BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION
            'seed-e2e.sql solo puede ejecutarse en la base pulsepass_e2e. Base actual: %',
            current_database();
END IF;
END
$$;


-- ------------------------------------------------------------
-- pgcrypto se utiliza únicamente en el ambiente E2E para
-- generar hashes BCrypt a partir de passwords locales.
-- ------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS pgcrypto;


BEGIN;


-- ============================================================
-- LIMPIEZA CONTROLADA DE DATOS MUTABLES E2E
-- ============================================================
--
-- Los tickets cambian de estado durante las pruebas.
-- Al volver a ejecutar el seed se eliminan únicamente tickets
-- asociados a los eventos E2E controlados.
-- ============================================================

DELETE FROM tickets
WHERE event_id IN (
    SELECT id
    FROM events
    WHERE event_code IN (
                         'E2E-DRAFT-001',
                         'E2E-PUBLISHED-001',
                         'E2E-PUBLISH-001',
                         'E2E-AGE-001',
                         'E2E-CAPACITY-001'
        )
);


DELETE FROM event_artists
WHERE event_id IN (
    SELECT id
    FROM events
    WHERE event_code IN (
                         'E2E-DRAFT-001',
                         'E2E-PUBLISHED-001',
                         'E2E-PUBLISH-001',
                         'E2E-AGE-001',
                         'E2E-CAPACITY-001'
        )
);


-- ============================================================
-- VENUES
-- ============================================================

INSERT INTO venues (
    code,
    name,
    city,
    address,
    capacity,
    active
)
VALUES (
           'E2E-VENUE-001',
           'PulsePass E2E Arena',
           'Santa Marta',
           'Direccion controlada para pruebas E2E',
           100,
           TRUE
       )
    ON CONFLICT (code)
DO UPDATE SET
    name = EXCLUDED.name,
           city = EXCLUDED.city,
           address = EXCLUDED.address,
           capacity = EXCLUDED.capacity,
           active = EXCLUDED.active;


-- Venue especial para comprobar capacidad agotada.

INSERT INTO venues (
    code,
    name,
    city,
    address,
    capacity,
    active
)
VALUES (
           'E2E-VENUE-SMALL-001',
           'PulsePass E2E Mini Venue',
           'Santa Marta',
           'Direccion controlada para prueba de capacidad',
           1,
           TRUE
       )
    ON CONFLICT (code)
DO UPDATE SET
    name = EXCLUDED.name,
           city = EXCLUDED.city,
           address = EXCLUDED.address,
           capacity = EXCLUDED.capacity,
           active = EXCLUDED.active;


-- ============================================================
-- USUARIOS PRIVILEGIADOS / CONTROLADOS
-- ============================================================
--
-- USER_A y USER_B NO se insertan aquí.
-- Deben registrarse mediante POST /api/users en Postman.
--
-- ADMIN y STAFF se provisionan fuera del registro público.
-- INACTIVE_USER permite comprobar bloqueo por estado.
-- ============================================================

INSERT INTO users (
    username,
    email,
    active
)
VALUES
    (
        'e2e_admin',
        'e2e.admin@example.test',
        TRUE
    ),
    (
        'e2e_staff',
        'e2e.staff@example.test',
        TRUE
    ),
    (
        'e2e_inactive',
        'e2e.inactive@example.test',
        FALSE
    )
    ON CONFLICT (username)
DO UPDATE SET
    email = EXCLUDED.email,
           active = EXCLUDED.active;


-- ============================================================
-- PERFILES
-- ============================================================

INSERT INTO user_profiles (
    first_name,
    last_name,
    phone,
    city,
    birth_date,
    user_id
)
SELECT
    'Admin',
    'E2E',
    NULL,
    'Santa Marta',
    DATE '1990-01-01',
    u.id
FROM users u
WHERE u.username = 'e2e_admin'
    ON CONFLICT (user_id)
DO UPDATE SET
    first_name = EXCLUDED.first_name,
           last_name = EXCLUDED.last_name,
           phone = EXCLUDED.phone,
           city = EXCLUDED.city,
           birth_date = EXCLUDED.birth_date;


INSERT INTO user_profiles (
    first_name,
    last_name,
    phone,
    city,
    birth_date,
    user_id
)
SELECT
    'Staff',
    'E2E',
    NULL,
    'Santa Marta',
    DATE '1992-01-01',
    u.id
FROM users u
WHERE u.username = 'e2e_staff'
    ON CONFLICT (user_id)
DO UPDATE SET
    first_name = EXCLUDED.first_name,
           last_name = EXCLUDED.last_name,
           phone = EXCLUDED.phone,
           city = EXCLUDED.city,
           birth_date = EXCLUDED.birth_date;


INSERT INTO user_profiles (
    first_name,
    last_name,
    phone,
    city,
    birth_date,
    user_id
)
SELECT
    'Inactive',
    'E2E',
    NULL,
    'Santa Marta',
    DATE '1995-01-01',
    u.id
FROM users u
WHERE u.username = 'e2e_inactive'
    ON CONFLICT (user_id)
DO UPDATE SET
    first_name = EXCLUDED.first_name,
           last_name = EXCLUDED.last_name,
           phone = EXCLUDED.phone,
           city = EXCLUDED.city,
           birth_date = EXCLUDED.birth_date;


-- ============================================================
-- CREDENCIALES BCrypt
-- ============================================================
--
-- Las variables:
--
-- admin_password
-- staff_password
-- inactive_password
--
-- deben suministrarse desde el archivo local ignorado:
--
-- e2e-secrets.local.sql
--
-- gen_salt('bf', 10) genera un hash BCrypt compatible con
-- Spring Security BCryptPasswordEncoder.
-- ============================================================

INSERT INTO user_credentials (
    user_id,
    password_hash,
    password_changed_at
)
SELECT
    u.id,
    crypt(
            :'admin_password',
            gen_salt('bf', 10)
    ),
    CURRENT_TIMESTAMP
FROM users u
WHERE u.username = 'e2e_admin'
    ON CONFLICT (user_id)
DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
           password_changed_at = EXCLUDED.password_changed_at;


INSERT INTO user_credentials (
    user_id,
    password_hash,
    password_changed_at
)
SELECT
    u.id,
    crypt(
            :'staff_password',
            gen_salt('bf', 10)
    ),
    CURRENT_TIMESTAMP
FROM users u
WHERE u.username = 'e2e_staff'
    ON CONFLICT (user_id)
DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
           password_changed_at = EXCLUDED.password_changed_at;


INSERT INTO user_credentials (
    user_id,
    password_hash,
    password_changed_at
)
SELECT
    u.id,
    crypt(
            :'inactive_password',
            gen_salt('bf', 10)
    ),
    CURRENT_TIMESTAMP
FROM users u
WHERE u.username = 'e2e_inactive'
    ON CONFLICT (user_id)
DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
           password_changed_at = EXCLUDED.password_changed_at;


-- ============================================================
-- ROLES
-- ============================================================
--
-- Eliminamos únicamente roles de los fixtures privilegiados
-- para garantizar un estado determinista tras cada seed.
-- ============================================================

DELETE FROM user_roles
WHERE user_id IN (
    SELECT id
    FROM users
    WHERE username IN (
                       'e2e_admin',
                       'e2e_staff',
                       'e2e_inactive'
        )
);


INSERT INTO user_roles (
    user_id,
    role
)
SELECT
    id,
    'ADMIN'
FROM users
WHERE username = 'e2e_admin';


INSERT INTO user_roles (
    user_id,
    role
)
SELECT
    id,
    'STAFF'
FROM users
WHERE username = 'e2e_staff';


INSERT INTO user_roles (
    user_id,
    role
)
SELECT
    id,
    'USER'
FROM users
WHERE username = 'e2e_inactive';


-- ============================================================
-- EVENTOS BASE
-- ============================================================


-- ------------------------------------------------------------
-- Draft que nunca debe exponerse públicamente.
-- ------------------------------------------------------------

INSERT INTO events (
    event_code,
    name,
    description,
    category,
    status,
    event_date,
    minimum_age,
    venue_id,
    streaming_url
)
SELECT
    'E2E-DRAFT-001',
    'PulsePass E2E Draft',
    'Evento borrador controlado para pruebas E2E',
    'MUSIC',
    'DRAFT',
    TIMESTAMP '2035-07-10 20:00:00',
    0,
    v.id,
    NULL
FROM venues v
WHERE v.code = 'E2E-VENUE-001'
    ON CONFLICT (event_code)
DO UPDATE SET
    name = EXCLUDED.name,
           description = EXCLUDED.description,
           category = EXCLUDED.category,
           status = EXCLUDED.status,
           event_date = EXCLUDED.event_date,
           minimum_age = EXCLUDED.minimum_age,
           venue_id = EXCLUDED.venue_id,
           streaming_url = EXCLUDED.streaming_url;


-- ------------------------------------------------------------
-- Evento publicado utilizado para compras normales.
-- ------------------------------------------------------------

INSERT INTO events (
    event_code,
    name,
    description,
    category,
    status,
    event_date,
    minimum_age,
    venue_id,
    streaming_url
)
SELECT
    'E2E-PUBLISHED-001',
    'PulsePass E2E Published',
    'Evento publicado controlado para pruebas E2E',
    'MUSIC',
    'PUBLISHED',
    TIMESTAMP '2035-08-15 20:00:00',
    0,
    v.id,
    NULL
FROM venues v
WHERE v.code = 'E2E-VENUE-001'
    ON CONFLICT (event_code)
DO UPDATE SET
    name = EXCLUDED.name,
           description = EXCLUDED.description,
           category = EXCLUDED.category,
           status = EXCLUDED.status,
           event_date = EXCLUDED.event_date,
           minimum_age = EXCLUDED.minimum_age,
           venue_id = EXCLUDED.venue_id,
           streaming_url = EXCLUDED.streaming_url;


-- ------------------------------------------------------------
-- Evento preparado específicamente para PATCH /publish.
-- ------------------------------------------------------------

INSERT INTO events (
    event_code,
    name,
    description,
    category,
    status,
    event_date,
    minimum_age,
    venue_id,
    streaming_url
)
SELECT
    'E2E-PUBLISH-001',
    'PulsePass E2E To Publish',
    'Evento preparado para transición DRAFT a PUBLISHED',
    'TECHNOLOGY',
    'DRAFT',
    TIMESTAMP '2035-09-20 18:00:00',
    0,
    v.id,
    NULL
FROM venues v
WHERE v.code = 'E2E-VENUE-001'
    ON CONFLICT (event_code)
DO UPDATE SET
    name = EXCLUDED.name,
           description = EXCLUDED.description,
           category = EXCLUDED.category,
           status = EXCLUDED.status,
           event_date = EXCLUDED.event_date,
           minimum_age = EXCLUDED.minimum_age,
           venue_id = EXCLUDED.venue_id,
           streaming_url = EXCLUDED.streaming_url;


-- ------------------------------------------------------------
-- Evento publicado para validar regla de edad.
-- ------------------------------------------------------------

INSERT INTO events (
    event_code,
    name,
    description,
    category,
    status,
    event_date,
    minimum_age,
    venue_id,
    streaming_url
)
SELECT
    'E2E-AGE-001',
    'PulsePass E2E Age Restricted',
    'Evento E2E con edad minima elevada',
    'EDUCATION',
    'PUBLISHED',
    TIMESTAMP '2035-10-10 19:00:00',
    40,
    v.id,
    NULL
FROM venues v
WHERE v.code = 'E2E-VENUE-001'
    ON CONFLICT (event_code)
DO UPDATE SET
    name = EXCLUDED.name,
           description = EXCLUDED.description,
           category = EXCLUDED.category,
           status = EXCLUDED.status,
           event_date = EXCLUDED.event_date,
           minimum_age = EXCLUDED.minimum_age,
           venue_id = EXCLUDED.venue_id,
           streaming_url = EXCLUDED.streaming_url;


-- ------------------------------------------------------------
-- Evento con capacidad total de una entrada.
-- Permite comprobar agotamiento de capacidad.
-- ------------------------------------------------------------

INSERT INTO events (
    event_code,
    name,
    description,
    category,
    status,
    event_date,
    minimum_age,
    venue_id,
    streaming_url
)
SELECT
    'E2E-CAPACITY-001',
    'PulsePass E2E Capacity',
    'Evento E2E con capacidad controlada de una entrada',
    'ENTERTAINMENT',
    'PUBLISHED',
    TIMESTAMP '2035-11-15 20:00:00',
    0,
    v.id,
    NULL
FROM venues v
WHERE v.code = 'E2E-VENUE-SMALL-001'
    ON CONFLICT (event_code)
DO UPDATE SET
    name = EXCLUDED.name,
           description = EXCLUDED.description,
           category = EXCLUDED.category,
           status = EXCLUDED.status,
           event_date = EXCLUDED.event_date,
           minimum_age = EXCLUDED.minimum_age,
           venue_id = EXCLUDED.venue_id,
           streaming_url = EXCLUDED.streaming_url;


-- ============================================================
-- RELACIONES EVENTO / ARTISTA
-- ============================================================
--
-- Solar Beat se asocia intencionalmente tanto a un evento
-- PUBLISHED como a un DRAFT.
--
-- Esto permite demostrar que:
--
-- ANONYMOUS / USER -> solo reciben PUBLISHED
-- STAFF / ADMIN    -> pueden visualizar ambos
-- ============================================================

INSERT INTO event_artists (
    event_id,
    artist_id
)
SELECT
    e.id,
    a.id
FROM events e
         JOIN artists a
              ON a.stage_name = 'Solar Beat'
WHERE e.event_code = 'E2E-DRAFT-001'
    ON CONFLICT DO NOTHING;


INSERT INTO event_artists (
    event_id,
    artist_id
)
SELECT
    e.id,
    a.id
FROM events e
         JOIN artists a
              ON a.stage_name = 'Solar Beat'
WHERE e.event_code = 'E2E-PUBLISHED-001'
    ON CONFLICT DO NOTHING;


COMMIT;


-- ============================================================
-- RESUMEN NO SENSIBLE
-- ============================================================

SELECT
    code,
    name,
    capacity,
    active
FROM venues
WHERE code LIKE 'E2E-%'
ORDER BY code;


SELECT
    event_code,
    status,
    category,
    minimum_age,
    event_date
FROM events
WHERE event_code LIKE 'E2E-%'
ORDER BY event_code;


SELECT
    u.username,
    u.email,
    u.active,
    ur.role,
    (
        uc.password_hash LIKE '$2%'
        ) AS bcrypt_configured
FROM users u
         LEFT JOIN user_roles ur
                   ON ur.user_id = u.id
         LEFT JOIN user_credentials uc
                   ON uc.user_id = u.id
WHERE u.username IN (
                     'e2e_admin',
                     'e2e_staff',
                     'e2e_inactive'
    )
ORDER BY u.username;
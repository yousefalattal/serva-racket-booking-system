-- Serva Racket Club booking system: initial schema
-- Club hours: 08:00 to 02:00 (next day), Asia/Amman. Payment is collected at the club.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TYPE user_role      AS ENUM ('member', 'admin');
CREATE TYPE sport_type     AS ENUM ('padel', 'tennis', 'pickleball');
CREATE TYPE booking_status AS ENUM ('confirmed', 'cancelled', 'no_show');
CREATE TYPE payment_method AS ENUM ('cash', 'card', 'waived');

CREATE TABLE users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(255) NOT NULL,
  phone         VARCHAR(20),
  role          user_role NOT NULL DEFAULT 'member',
  is_active     BOOLEAN NOT NULL DEFAULT true,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE courts (
  id           BIGSERIAL PRIMARY KEY,
  name         VARCHAR(100) NOT NULL UNIQUE,
  sport        sport_type NOT NULL,
  is_available BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE pricing (
  sport          sport_type PRIMARY KEY,
  peak_price     NUMERIC(8,3) NOT NULL,   -- JOD, per court per hour
  off_peak_price NUMERIC(8,3) NOT NULL,
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Off-peak windows in local whole hours. start_hour inclusive, end_hour exclusive.
-- Any open hour not covered by a window is peak.
CREATE TABLE off_peak_windows (
  id         SERIAL PRIMARY KEY,
  start_hour SMALLINT NOT NULL CHECK (start_hour BETWEEN 0 AND 23),
  end_hour   SMALLINT NOT NULL CHECK (end_hour BETWEEN 1 AND 24),
  CHECK (end_hour > start_hour)
);

CREATE TABLE recurring_bookings (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id),
  court_id    BIGINT NOT NULL REFERENCES courts(id),
  day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
  start_time  TIME NOT NULL,
  duration_h  SMALLINT NOT NULL CHECK (duration_h >= 1),
  ends_on     DATE,                        -- NULL = until cancelled
  is_active   BOOLEAN NOT NULL DEFAULT true,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE bookings (
  id             BIGSERIAL PRIMARY KEY,
  user_id        BIGINT NOT NULL REFERENCES users(id),
  court_id       BIGINT NOT NULL REFERENCES courts(id),
  recurring_id   BIGINT REFERENCES recurring_bookings(id),
  starts_at      TIMESTAMPTZ NOT NULL,
  ends_at        TIMESTAMPTZ NOT NULL,
  total_price    NUMERIC(8,3) NOT NULL,    -- computed hour by hour at booking time
  status         booking_status NOT NULL DEFAULT 'confirmed',

  -- Pay at club: front desk records payment
  paid_at        TIMESTAMPTZ,
  paid_amount    NUMERIC(8,3),
  payment_method payment_method,
  received_by    BIGINT REFERENCES users(id),

  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (ends_at > starts_at),
  CHECK (extract(minute from starts_at) = 0 AND extract(minute from ends_at) = 0),
  -- No two active bookings may overlap on the same court
  EXCLUDE USING gist (
    court_id WITH =,
    tstzrange(starts_at, ends_at) WITH &&
  ) WHERE (status <> 'cancelled')
);

CREATE INDEX idx_bookings_user  ON bookings(user_id, starts_at);
CREATE INDEX idx_bookings_range ON bookings(starts_at);

CREATE TABLE audit_logs (
  id          BIGSERIAL PRIMARY KEY,
  admin_id    BIGINT NOT NULL REFERENCES users(id),
  action      VARCHAR(255) NOT NULL,
  entity_type VARCHAR(100),
  entity_id   BIGINT,
  details     JSONB,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed data
INSERT INTO courts (name, sport) VALUES
  ('Padel 1','padel'),('Padel 2','padel'),('Padel 3','padel'),('Padel 4','padel'),
  ('Tennis 1','tennis'),('Tennis 2','tennis'),
  ('Pickleball 1','pickleball');

INSERT INTO pricing (sport, peak_price, off_peak_price) VALUES
  ('padel',      35.000, 25.000),
  ('tennis',     22.000, 16.000),
  ('pickleball', 20.000, 16.000);

INSERT INTO off_peak_windows (start_hour, end_hour) VALUES
  (0, 2),    -- 12 AM to 2 AM
  (8, 17);   -- 8 AM to 5 PM

-- name: create-tables
-- Create tables needed by the server.

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS beacons (
       id SERIAL PRIMARY KEY,
       name VARCHAR(64),
       uuid VARCHAR(64) UNIQUE,
       owner VARCHAR(32),
       location geography(POINT,4326),
       label VARCHAR(32),
       active BOOLEAN
);

CREATE TABLE IF NOT EXISTS users (
       fb_id VARCHAR(32) UNIQUE,
       first_name VARCHAR(32),
       last_name VARCHAR(32),
       mood VARCHAR(8),
       message VARCHAR(256),
       active BOOLEAN,
       photo_url VARCHAR(256),
       sex VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS visits (
       id SERIAL PRIMARY KEY,
       fb_id VARCHAR(32) NOT NULL,
       timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
       beacon_id INT4,
       signal FLOAT,
       location geography(POINT,4326)
);

INSERT INTO beacons (name, uuid, label, location, active)
       VALUES ('Tram 7234', 'NJ', 'liz01',
              ST_GeographyFromText('SRID=4326;POINT(14.5 50)'), true),
              ('DjbX', '11f10af9-8ec0-4e88-bc27-3fb17effe8bf|10998|26482', 'x1', NULL, true),
              ('xgDa', '11f10af9-8ec0-4e88-bc27-3fb17effe8bf|8454|52608', 'x2', NULL, true);;




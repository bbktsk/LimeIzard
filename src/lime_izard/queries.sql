-- name: q-user-get

SELECT first_name, last_name, mood, active, message, photo_url, sex FROM users WHERE fb_id = :fb_id;

-- name: q-user-insert!

INSERT
        INTO users(fb_id, first_name, last_name, mood, active, message, photo_url, sex)
        VALUES (:fb_id, :first_name, :last_name, :mood, :active, :message, :photo_url, :sex)

-- name: q-user-update!

UPDATE users
       SET first_name = :first_name,
           last_name = :last_name,
           mood = :mood,
           active = :active,
           message = :message,
           photo_url = :photo_url,
           sex = :sex
       WHERE fb_id = :fb_id

-- name: q-beacon-by-uuid

SELECT id, uuid, name, label, active, owner, st_x(location::geometry) AS longitude,
st_y(location::geometry) AS latitude FROM beacons WHERE uuid = :uuid

-- name: q-insert-visit!

INSERT INTO visits (fb_id, beacon_id, signal, location)
VALUES (:fb_id, :beacon_id, :signal, ST_GeographyFromText('SRID=4326;POINT(' || :longitude || ' ' || :latitude || ')'))
             

-- name: q-get-nearby

SELECT first_name, mood, message, photo_url, sex, v.fb_id, signal, timestamp
FROM users u, visits v
WHERE v.fb_id = u.fb_id AND v.beacon_id = :beacon_id AND v.fb_id != :self;

-- name: q-user-beacons

SELECT name, uuid, label, active, owner,
       st_x(location::geometry) AS longitude,
       st_y(location::geometry) AS latitude
FROM beacons WHERE owner = :owner

-- name: q-insert-poke!

INSERT INTO pokes(source, target, seen) VALUES (:source, :target, false)


-- name: q-get-user-pokes
SELECT first_name, mood, message, fb_id id
FROM pokes p, users u
WHERE u.fb_id = p.source AND p.target = :self AND p.seen = false;

-- name: q-mark-pokes-seen!
UPDATE pokes SET seen=true WHERE target=:self

-- name: q-beacon-update!
UPDATE beacons SET name=:name,
                   label=:label,
                   active=:active
               WHERE
                   uuid=:uuid
                   

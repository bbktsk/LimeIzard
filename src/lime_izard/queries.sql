-- name: q-user-get

SELECT first_name, last_name, mood, active, message, photo_url FROM users WHERE fb_id = :fb_id;

-- name: q-user-insert!

INSERT
        INTO users(fb_id, first_name, last_name, mood, active, message, photo_url)
        VALUES (:fb_id, :first_name, :last_name, :mood, :active, :message, :photo_url)

-- name: q-user-update!

UPDATE users
       SET first_name = :first_name,
           last_name = :last_name,
           mood = :mood,
           active = :active,
           message = :message,
           photo_url = :photo_url
       WHERE fb_id = :fb_id
             

-- name: insert-fix!
-- Insert location fix into database

INSERT
        INTO fixes (user_id, location, altitude, accuracy)
        VALUES (:user_id, ST_GeographyFromText('SRID=4326;POINT(' || :longitude || ' ' || :latitude || ')'), :altitude, :accuracy)

-- name: all-fixes

SELECT timestamp, user_id, st_x(location::geometry) AS longitude, st_y(location::geometry) AS latitude FROM fixes


-- name: latest-fix
SELECT timestamp,
       st_x(location::geometry) AS longitude,
       st_y(location::geometry) AS latitude,
       accuracy
FROM fixes
WHERE id=(SELECT max(id) FROM fixes WHERE user_id = :user_id)
        


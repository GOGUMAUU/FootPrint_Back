CREATE TABLE photo_uploads (
 path VARCHAR(255) NOT NULL PRIMARY KEY,
 user_id BIGINT NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT fk_upload_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE INDEX ix_upload_user ON photo_uploads(user_id);
-- Existing attached files inherit their recorded uploader. Ambiguous ownership is
-- not guessed; such files remain readable through their authorized guestbook only.
INSERT INTO photo_uploads(path, user_id, created_at)
 SELECT file_path, MIN(uploaded_by), CURRENT_TIMESTAMP
 FROM guestbook_photos
 WHERE file_path IS NOT NULL AND uploaded_by IS NOT NULL AND LENGTH(file_path) <= 255
 GROUP BY file_path HAVING COUNT(DISTINCT uploaded_by) = 1;

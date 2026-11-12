-- Baseline of the pre-authentication JPA model. Review existing databases before
-- explicitly baselining them at version 1. Automatic baselining is disabled.
CREATE TABLE users (
 user_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 email VARCHAR(100) NOT NULL, nickname VARCHAR(50) NOT NULL,
 profile_image_url VARCHAR(255), created_at DATETIME(6) NOT NULL,
 follower_count INT, following_count INT, guestbook_count INT
);
CREATE TABLE follows (
 follow_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 follower_id BIGINT NOT NULL, following_id BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL, status VARCHAR(20),
 CONSTRAINT fk_follow_from FOREIGN KEY (follower_id) REFERENCES users(user_id),
 CONSTRAINT fk_follow_to FOREIGN KEY (following_id) REFERENCES users(user_id)
);
CREATE TABLE guestbooks (
 guestbook_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, content VARCHAR(500),
 latitude DECIMAL(10,8) NOT NULL, longitude DECIMAL(11,8) NOT NULL,
 visibility VARCHAR(30), location_naem VARCHAR(100),
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6),
 CONSTRAINT fk_guestbook_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE TABLE guestbook_photos (
 photo_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 guestbook_id BIGINT NOT NULL, uploaded_by BIGINT, file_path VARCHAR(500),
 sort_order INT, created_at DATETIME(6),
 CONSTRAINT fk_photo_guestbook FOREIGN KEY (guestbook_id) REFERENCES guestbooks(guestbook_id),
 CONSTRAINT fk_photo_user FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
);
CREATE TABLE guestbook_likes (
 like_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 guestbook_id BIGINT NOT NULL, user_id BIGINT,
 CONSTRAINT fk_like_guestbook FOREIGN KEY (guestbook_id) REFERENCES guestbooks(guestbook_id),
 CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE TABLE guestbook_views (
 view_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 guestbook_id BIGINT NOT NULL, user_id BIGINT, viewed_at DATETIME(6),
 CONSTRAINT fk_view_guestbook FOREIGN KEY (guestbook_id) REFERENCES guestbooks(guestbook_id),
 CONSTRAINT fk_view_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE TABLE quests (
 quest_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 title VARCHAR(100) NOT NULL, description TEXT, region_name VARCHAR(50) NOT NULL,
 target_latitude DECIMAL(10,8) NOT NULL, target_longitude DECIMAL(11,8) NOT NULL,
 target_radius INT NOT NULL, badge_image_url VARCHAR(255), created_at DATETIME(6) NOT NULL
);
CREATE TABLE user_quests (
 user_quest_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, quest_id BIGINT NOT NULL, is_completed BOOLEAN NOT NULL,
 completed_at DATETIME(6),
 CONSTRAINT fk_userquest_user FOREIGN KEY (user_id) REFERENCES users(user_id),
 CONSTRAINT fk_userquest_quest FOREIGN KEY (quest_id) REFERENCES quests(quest_id)
);
CREATE TABLE reports (
 report_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 guestbook_id BIGINT NOT NULL, reporter_id BIGINT, reason VARCHAR(30),
 description VARCHAR(500), status VARCHAR(30), created_at DATETIME(6), resolved_at DATETIME(6),
 CONSTRAINT fk_report_guestbook FOREIGN KEY (guestbook_id) REFERENCES guestbooks(guestbook_id),
 CONSTRAINT fk_report_user FOREIGN KEY (reporter_id) REFERENCES users(user_id)
);

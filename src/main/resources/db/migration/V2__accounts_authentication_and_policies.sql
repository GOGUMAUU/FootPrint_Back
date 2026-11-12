-- Duplicate emails/follow pairs fail migration instead of discarding user data.
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN friend_code VARCHAR(20);
ALTER TABLE users ADD COLUMN is_private BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN updated_at DATETIME(6);
ALTER TABLE users ADD COLUMN deleted_at DATETIME(6);
UPDATE users SET email = LOWER(TRIM(email));
CREATE UNIQUE INDEX uk_user_email ON users(email);
CREATE UNIQUE INDEX uk_user_friend_code ON users(friend_code);
ALTER TABLE follows ADD COLUMN responded_at DATETIME(6);
CREATE UNIQUE INDEX uk_follow_pair ON follows(follower_id, following_id);
CREATE INDEX ix_follow_incoming ON follows(following_id, status, created_at);
CREATE TABLE sessions (
 session_id VARCHAR(36) NOT NULL PRIMARY KEY, user_id BIGINT NOT NULL,
 created_at TIMESTAMP(6) NOT NULL, expires_at TIMESTAMP(6) NOT NULL,
 last_used_at TIMESTAMP(6), revoked_at TIMESTAMP(6),
 CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE INDEX ix_session_user ON sessions(user_id);
CREATE TABLE refresh_tokens (
 token_hash VARCHAR(64) NOT NULL PRIMARY KEY, session_id VARCHAR(36) NOT NULL,
 used_at TIMESTAMP(6),
 CONSTRAINT fk_refresh_session FOREIGN KEY (session_id) REFERENCES sessions(session_id)
);
CREATE INDEX ix_refresh_session ON refresh_tokens(session_id);
CREATE TABLE blocks (
 block_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 blocker_id BIGINT NOT NULL, blocked_id BIGINT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT uk_block_pair UNIQUE (blocker_id, blocked_id),
 CONSTRAINT fk_block_from FOREIGN KEY (blocker_id) REFERENCES users(user_id),
 CONSTRAINT fk_block_to FOREIGN KEY (blocked_id) REFERENCES users(user_id)
);
CREATE INDEX ix_block_target ON blocks(blocked_id);
CREATE TABLE policy_documents (
 policy_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 type VARCHAR(30) NOT NULL, version VARCHAR(40) NOT NULL, content TEXT NOT NULL,
 consent_required BOOLEAN NOT NULL, required_at VARCHAR(20) NOT NULL,
 withdrawable BOOLEAN NOT NULL, published BOOLEAN NOT NULL, published_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT uk_policy_version UNIQUE (type, version)
);
CREATE TABLE user_contents (
 consent_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
 type VARCHAR(30) NOT NULL, version VARCHAR(40) NOT NULL,
 consented_at TIMESTAMP(6) NOT NULL, withdrawn_at TIMESTAMP(6),
 CONSTRAINT fk_consent_user FOREIGN KEY (user_id) REFERENCES users(user_id),
 CONSTRAINT fk_consent_policy FOREIGN KEY (type, version) REFERENCES policy_documents(type, version)
);
CREATE INDEX ix_consent_active ON user_contents(user_id, type, version, withdrawn_at);
-- No sample policy documents or consents are installed in application databases.

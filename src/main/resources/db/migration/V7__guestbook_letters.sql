-- 그 자리에서 열리는 편지: 받는 사람과 처음 열어 본 시각
ALTER TABLE guestbooks
    ADD COLUMN recipient_id BIGINT NULL,
    ADD COLUMN letter_opened_at DATETIME(6) NULL,
    ADD CONSTRAINT fk_guestbook_recipient FOREIGN KEY (recipient_id) REFERENCES users(user_id);
CREATE INDEX ix_guestbook_recipient ON guestbooks(recipient_id, letter_opened_at);

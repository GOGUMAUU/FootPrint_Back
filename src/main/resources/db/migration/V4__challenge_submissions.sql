CREATE TABLE challenge_submissions (
    submission_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quest_id BIGINT NOT NULL,
    photo_url VARCHAR(500),
    memo VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_at DATETIME(6) NOT NULL,
    reviewed_at DATETIME(6),

    CONSTRAINT fk_challenge_submission_user
        FOREIGN KEY (user_id) REFERENCES users(user_id),

    CONSTRAINT fk_challenge_submission_quest
        FOREIGN KEY (quest_id) REFERENCES quests(quest_id)
);

-- 도전과제 생성자 및 유형 추가
ALTER TABLE quests
    ADD COLUMN creator_id BIGINT NULL,
    ADD COLUMN quest_type VARCHAR(20) NOT NULL DEFAULT 'SINGLE',
    ADD CONSTRAINT fk_quest_creator
        FOREIGN KEY (creator_id) REFERENCES users(user_id);
-- =====================================================================
-- 지도 테스트용 시드 데이터 (로컬 개발 DB 전용! 운영 DB에 절대 실행 금지)
-- 실행: mysql -u root -p footprint_db < dev/seed_map_test.sql
-- 여러 번 실행해도 됨: 맨 위에서 이전 시드를 지우고 다시 넣음
--
-- 나(@me) = 가입한 계정 중 가장 먼저 만든 계정. 다른 계정으로 하려면
--   아래 @me_email 에 이메일을 넣으세요 (비워두면 자동).
-- =====================================================================
SET @me_email = '';
SET @me = COALESCE(
  (SELECT user_id FROM users WHERE email = @me_email),
  (SELECT MIN(user_id) FROM users WHERE email NOT LIKE 'seed\_%@footprint.dev')
);
SELECT @me AS '나(@me) user_id', (SELECT email FROM users WHERE user_id = @me) AS email;

-- ---------- 이전 시드 삭제 ----------
DELETE l FROM guestbook_likes l JOIN guestbooks g ON g.guestbook_id = l.guestbook_id WHERE g.content LIKE '[seed]%';
DELETE v FROM guestbook_views v JOIN guestbooks g ON g.guestbook_id = v.guestbook_id WHERE g.content LIKE '[seed]%';
DELETE FROM guestbooks WHERE content LIKE '[seed]%';
DELETE f FROM follows f JOIN users u ON (u.user_id = f.follower_id OR u.user_id = f.following_id) WHERE u.email LIKE 'seed\_%@footprint.dev';
DELETE FROM users WHERE email LIKE 'seed\_%@footprint.dev';

-- ---------- 테스트 유저 (로그인 불가, 데이터용) ----------
INSERT INTO users (email, nickname, created_at, follower_count, following_count, guestbook_count, friend_code, is_private) VALUES
 ('seed_a@footprint.dev', '시드A_팔로잉',      UTC_TIMESTAMP(6), 0,0,0, 'SEEDA001', FALSE),
 ('seed_b@footprint.dev', '시드B_비공개계정',  UTC_TIMESTAMP(6), 0,0,0, 'SEEDB001', TRUE),
 ('seed_c@footprint.dev', '시드C_팔로우대기',  UTC_TIMESTAMP(6), 0,0,0, 'SEEDC001', FALSE),
 ('seed_d@footprint.dev', '시드D_모르는사람',  UTC_TIMESTAMP(6), 0,0,0, 'SEEDD001', FALSE);
SET @a = (SELECT user_id FROM users WHERE email='seed_a@footprint.dev');
SET @b = (SELECT user_id FROM users WHERE email='seed_b@footprint.dev');
SET @c = (SELECT user_id FROM users WHERE email='seed_c@footprint.dev');
SET @d = (SELECT user_id FROM users WHERE email='seed_d@footprint.dev');

-- ---------- 팔로우 관계 ----------
-- 나 → A 승인, 나 → B(비공개계정) 승인, 나 → C 대기중, D와는 관계 없음
INSERT INTO follows (follower_id, following_id, created_at, status, responded_at) VALUES
 (@me, @a, UTC_TIMESTAMP(6), 'ACCEPTED', UTC_TIMESTAMP(6)),
 (@me, @b, UTC_TIMESTAMP(6), 'ACCEPTED', UTC_TIMESTAMP(6)),
 (@me, @c, UTC_TIMESTAMP(6), 'PENDING',  NULL);

-- ---------- 방명록 ----------
INSERT INTO guestbooks (user_id, content, latitude, longitude, visibility, location_naem, created_at, updated_at) VALUES
 (@me, '[seed] 학교 정문 도착!', 37.59150000, 127.02220000, 'PUBLIC', '성신여대 정문', '2026-10-30 07:00:00', '2026-10-30 07:00:00'),
 (@me, '[seed] 수정관 앞 (나만 보기)', 37.59100000, 127.02190000, 'PRIVATE', '성신여대', '2026-11-06 16:00:00', '2026-11-06 16:00:00'),
 (@me, '[seed] 붕어빵 맛집 발견', 37.59668674, 127.02422821, 'FOLLOWERS_ONLY', '성북구', '2026-10-28 12:00:00', '2026-10-28 12:00:00'),
 (@me, '[seed] 벤치에서 쉬다 감', 37.59708335, 127.01744905, 'PUBLIC', '성북구', '2026-11-09 10:00:00', '2026-11-09 10:00:00'),
 (@me, '[seed] 노을 진짜 예쁨', 37.59665483, 127.02527252, 'PUBLIC', '성북구', '2026-11-10 14:00:00', '2026-11-10 14:00:00'),
 (@me, '[seed] 고양이 있음', 37.59711930, 127.02933431, 'PUBLIC', '성북구', '2026-10-19 12:00:00', '2026-10-19 12:00:00'),
 (@me, '[seed] 과제하다 탈출', 37.59784296, 127.02475943, 'PUBLIC', '성북구', '2026-10-20 08:00:00', '2026-10-20 08:00:00'),
 (@me, '[seed] 비 오는 날 산책', 37.58924991, 127.03228037, 'PUBLIC', '성북구', '2026-10-19 18:00:00', '2026-10-19 18:00:00'),
 (@me, '[seed] 강남역 다녀옴', 37.49790000, 127.02760000, 'PUBLIC', '강남역', '2026-10-20 05:00:00', '2026-10-20 05:00:00'),
 (@me, '[seed] 홍대입구 다녀옴', 37.55630000, 126.92200000, 'PUBLIC', '홍대입구', '2026-10-15 00:00:00', '2026-10-15 00:00:00'),
 (@me, '[seed] 잠실 다녀옴', 37.51260000, 127.10250000, 'PUBLIC', '잠실', '2026-11-08 17:00:00', '2026-11-08 17:00:00'),
 (@me, '[seed] 해운대 다녀옴', 35.15870000, 129.16040000, 'PUBLIC', '해운대', '2026-10-19 18:00:00', '2026-10-19 18:00:00'),
 (@me, '[seed] 대구 동성로 다녀옴', 35.86900000, 128.59600000, 'PUBLIC', '대구 동성로', '2026-11-05 02:00:00', '2026-11-05 02:00:00'),
 (@me, '[seed] 광주 양림동 다녀옴', 35.13950000, 126.91500000, 'PUBLIC', '광주 양림동', '2026-11-08 23:00:00', '2026-11-08 23:00:00'),
 (@a, '[seed][A] 정문 옆 벤치 (전체공개)', 37.59170000, 127.02180000, 'PUBLIC', '성신여대', '2026-10-20 18:00:00', '2026-10-20 18:00:00'),
 (@a, '[seed][A] 팔로워만 볼 수 있어요', 37.59080000, 127.02250000, 'FOLLOWERS_ONLY', '성신여대', '2026-11-10 10:00:00', '2026-11-10 10:00:00'),
 (@a, '[seed][A] 나만 보기 (안 보여야 정상)', 37.59190000, 127.02270000, 'PRIVATE', '성신여대', '2026-10-20 01:00:00', '2026-10-20 01:00:00'),
 (@a, '[seed][A] 고양이 있음', 37.59189915, 127.02476241, 'FOLLOWERS_ONLY', '성북구', '2026-10-25 21:00:00', '2026-10-25 21:00:00'),
 (@a, '[seed][A] 과제하다 탈출', 37.58356469, 127.02424336, 'PUBLIC', '성북구', '2026-10-31 08:00:00', '2026-10-31 08:00:00'),
 (@a, '[seed][A] 비 오는 날 산책', 37.59321232, 127.02720924, 'PUBLIC', '성북구', '2026-10-19 14:00:00', '2026-10-19 14:00:00'),
 (@a, '[seed][A] 카페 조용해서 좋음', 37.58638002, 127.02229058, 'PUBLIC', '성북구', '2026-10-24 23:00:00', '2026-10-24 23:00:00'),
 (@a, '[seed][A] 버스킹 보고 감', 37.59608297, 127.02134419, 'PUBLIC', '성북구', '2026-10-26 06:00:00', '2026-10-26 06:00:00'),
 (@a, '[seed][A] 시험 끝!', 37.58930539, 127.02595553, 'PUBLIC', '성북구', '2026-11-11 10:00:00', '2026-11-11 10:00:00'),
 (@a, '[seed][A] 떡볶이 최고', 37.59908362, 127.02731057, 'FOLLOWERS_ONLY', '성북구', '2026-10-30 17:00:00', '2026-10-30 17:00:00'),
 (@a, '[seed][A] 벚꽃 명소', 37.58821587, 127.02744458, 'PUBLIC', '성북구', '2026-10-24 15:00:00', '2026-10-24 15:00:00'),
 (@a, '[seed][A] 해운대 여행', 35.15870000, 129.16040000, 'PUBLIC', '해운대', '2026-11-09 03:00:00', '2026-11-09 03:00:00'),
 (@a, '[seed][A] 대구 동성로 여행', 35.86900000, 128.59600000, 'PUBLIC', '대구 동성로', '2026-10-23 21:00:00', '2026-10-23 21:00:00'),
 (@a, '[seed][A] 광주 양림동 여행', 35.13950000, 126.91500000, 'PUBLIC', '광주 양림동', '2026-11-10 12:00:00', '2026-11-10 12:00:00'),
 (@a, '[seed][A] 제주시 여행', 33.49960000, 126.53120000, 'PUBLIC', '제주시', '2026-10-16 12:00:00', '2026-10-16 12:00:00'),
 (@a, '[seed][A] 전주 한옥마을 여행', 35.81500000, 127.15300000, 'PUBLIC', '전주 한옥마을', '2026-10-15 01:00:00', '2026-10-15 01:00:00'),
 (@a, '[seed][A] 강릉 안목해변 여행', 37.77200000, 128.94700000, 'PUBLIC', '강릉 안목해변', '2026-10-31 23:00:00', '2026-10-31 23:00:00'),
 (@a, '[seed][A] 인천 차이나타운 여행', 37.47550000, 126.61800000, 'PUBLIC', '인천 차이나타운', '2026-10-15 14:00:00', '2026-10-15 14:00:00'),
 (@a, '[seed][A] 수원 행궁동 여행', 37.28200000, 127.01400000, 'PUBLIC', '수원 행궁동', '2026-11-12 03:00:00', '2026-11-12 03:00:00'),
 (@a, '[seed][A] 경주 황리단길 여행', 35.83800000, 129.21000000, 'PUBLIC', '경주 황리단길', '2026-10-28 23:00:00', '2026-10-28 23:00:00'),
 (@b, '[seed][B 비공개계정] 카페 조용해서 좋음', 37.59375699, 127.02490711, 'FOLLOWERS_ONLY', '성북구', '2026-11-03 19:00:00', '2026-11-03 19:00:00'),
 (@b, '[seed][B 비공개계정] 버스킹 보고 감', 37.59617224, 127.02859370, 'FOLLOWERS_ONLY', '성북구', '2026-10-27 03:00:00', '2026-10-27 03:00:00'),
 (@b, '[seed][B 비공개계정] 시험 끝!', 37.59479324, 127.01748866, 'FOLLOWERS_ONLY', '성북구', '2026-11-06 00:00:00', '2026-11-06 00:00:00'),
 (@b, '[seed][B 비공개계정] 떡볶이 최고', 37.58614079, 127.02000975, 'FOLLOWERS_ONLY', '성북구', '2026-10-25 18:00:00', '2026-10-25 18:00:00'),
 (@b, '[seed][B 비공개계정] 강남역', 37.49790000, 127.02760000, 'PUBLIC', '강남역', '2026-11-01 05:00:00', '2026-11-01 05:00:00'),
 (@b, '[seed][B 비공개계정] 홍대입구', 37.55630000, 126.92200000, 'PUBLIC', '홍대입구', '2026-10-28 19:00:00', '2026-10-28 19:00:00'),
 (@b, '[seed][B 비공개계정] 잠실', 37.51260000, 127.10250000, 'PUBLIC', '잠실', '2026-11-03 06:00:00', '2026-11-03 06:00:00'),
 (@b, '[seed][B 비공개계정] 해운대', 35.15870000, 129.16040000, 'PUBLIC', '해운대', '2026-11-09 14:00:00', '2026-11-09 14:00:00'),
 (@c, '[seed][C 팔로우대기] 떡볶이 최고', 37.59172659, 127.02682846, 'PUBLIC', '성북구', '2026-11-12 14:00:00', '2026-11-12 14:00:00'),
 (@c, '[seed][C 팔로우대기] 벚꽃 명소', 37.58599970, 127.01790648, 'PUBLIC', '성북구', '2026-11-01 02:00:00', '2026-11-01 02:00:00'),
 (@c, '[seed][C 팔로우대기] 친구랑 사진 찍음', 37.59079819, 127.02245364, 'PUBLIC', '성북구', '2026-10-18 02:00:00', '2026-10-18 02:00:00'),
 (@c, '[seed][C 팔로우대기] 야경 좋다', 37.59776887, 127.01962201, 'PUBLIC', '성북구', '2026-10-17 18:00:00', '2026-10-17 18:00:00'),
 (@c, '[seed][C 팔로우대기] 팔로워전용 (안 보여야 정상)', 37.59106685, 127.01752199, 'FOLLOWERS_ONLY', '성북구', '2026-10-24 15:00:00', '2026-10-24 15:00:00'),
 (@c, '[seed][C 팔로우대기] 제주시 (팔로잉 지도에 안 보여야 정상)', 33.49960000, 126.53120000, 'PUBLIC', '제주시', '2026-10-15 02:00:00', '2026-10-15 02:00:00'),
 (@c, '[seed][C 팔로우대기] 전주 한옥마을 (팔로잉 지도에 안 보여야 정상)', 35.81500000, 127.15300000, 'PUBLIC', '전주 한옥마을', '2026-10-20 06:00:00', '2026-10-20 06:00:00'),
 (@c, '[seed][C 팔로우대기] 강릉 안목해변 (팔로잉 지도에 안 보여야 정상)', 37.77200000, 128.94700000, 'PUBLIC', '강릉 안목해변', '2026-10-27 09:00:00', '2026-10-27 09:00:00'),
 (@d, '[seed][D 모르는사람] 벤치에서 쉬다 감', 37.58684092, 127.02651514, 'PUBLIC', '성북구', '2026-10-17 01:00:00', '2026-10-17 01:00:00'),
 (@d, '[seed][D 모르는사람] 노을 진짜 예쁨', 37.59337237, 127.02878029, 'PUBLIC', '성북구', '2026-10-25 07:00:00', '2026-10-25 07:00:00'),
 (@d, '[seed][D 모르는사람] 고양이 있음', 37.58935981, 127.02595544, 'PUBLIC', '성북구', '2026-11-08 18:00:00', '2026-11-08 18:00:00'),
 (@d, '[seed][D 모르는사람] 과제하다 탈출', 37.59137976, 127.02224089, 'PUBLIC', '성북구', '2026-10-28 14:00:00', '2026-10-28 14:00:00'),
 (@d, '[seed][D 모르는사람] 비 오는 날 산책', 37.59766176, 127.02589746, 'PUBLIC', '성북구', '2026-10-17 22:00:00', '2026-10-17 22:00:00'),
 (@d, '[seed][D 모르는사람] 카페 조용해서 좋음', 37.58764085, 127.01690102, 'PUBLIC', '성북구', '2026-10-29 07:00:00', '2026-10-29 07:00:00'),
 (@d, '[seed][D 모르는사람] 팔로워전용 (안 보여야 정상)', 37.58854810, 127.02266906, 'FOLLOWERS_ONLY', '성북구', '2026-11-08 04:00:00', '2026-11-08 04:00:00'),
 (@d, '[seed][D 모르는사람] 1.2km 밖 (주변에 안 보여야 정상)', 37.60455033, 127.02137486, 'PUBLIC', '성북구', '2026-10-24 05:00:00', '2026-10-24 05:00:00'),
 (@d, '[seed][D 모르는사람] 정문 밀집 1', 37.59125295, 127.02164845, 'PUBLIC', '성신여대 정문', '2026-11-08 18:00:00', '2026-11-08 18:00:00'),
 (@d, '[seed][D 모르는사람] 정문 밀집 2', 37.59189922, 127.02267684, 'PUBLIC', '성신여대 정문', '2026-10-14 14:00:00', '2026-10-14 14:00:00'),
 (@d, '[seed][D 모르는사람] 정문 밀집 3', 37.59048745, 127.02095543, 'PUBLIC', '성신여대 정문', '2026-10-28 16:00:00', '2026-10-28 16:00:00'),
 (@d, '[seed][D 모르는사람] 정문 밀집 4', 37.59045185, 127.02220361, 'PUBLIC', '성신여대 정문', '2026-10-21 14:00:00', '2026-10-21 14:00:00'),
 (@d, '[seed][D 모르는사람] 정문 밀집 5', 37.59081542, 127.02244300, 'PUBLIC', '성신여대 정문', '2026-10-14 10:00:00', '2026-10-14 10:00:00'),
 (@d, '[seed][D 모르는사람] 정문 밀집 6', 37.59212907, 127.02214415, 'PUBLIC', '성신여대 정문', '2026-10-28 22:00:00', '2026-10-28 22:00:00');

-- ---------- 좋아요 (인기순 테스트) ----------
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 학교 정문 도착!' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 12) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 붕어빵 맛집 발견' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 3) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 벤치에서 쉬다 감' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 1) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 노을 진짜 예쁨' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 13) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 고양이 있음' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 1) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 과제하다 탈출' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 1) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 비 오는 날 산책' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 3) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 강남역 다녀옴' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 12) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 홍대입구 다녀옴' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 29) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 잠실 다녀옴' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 8) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 해운대 다녀옴' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 21) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 대구 동성로 다녀옴' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 23) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed] 광주 양림동 다녀옴' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 14) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 정문 옆 벤치 (전체공개)' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 9) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 팔로워만 볼 수 있어요' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 4) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 고양이 있음' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 17) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 과제하다 탈출' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 11) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 비 오는 날 산책' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 2) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 카페 조용해서 좋음' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 23) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 버스킹 보고 감' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 16) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 시험 끝!' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 13) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 떡볶이 최고' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 18) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 벚꽃 명소' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 18) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 해운대 여행' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 4) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 대구 동성로 여행' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 17) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 광주 양림동 여행' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 4) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 제주시 여행' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 19) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 전주 한옥마을 여행' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 36) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 강릉 안목해변 여행' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 28) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 인천 차이나타운 여행' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 24) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 수원 행궁동 여행' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 22) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][A] 경주 황리단길 여행' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 29) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][B 비공개계정] 버스킹 보고 감' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 3) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][B 비공개계정] 시험 끝!' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 1) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][B 비공개계정] 떡볶이 최고' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 2) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][B 비공개계정] 강남역' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 17) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][B 비공개계정] 홍대입구' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 13) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][B 비공개계정] 잠실' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 12) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][B 비공개계정] 해운대' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 4) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][C 팔로우대기] 떡볶이 최고' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 3) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][C 팔로우대기] 벚꽃 명소' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 4) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][C 팔로우대기] 친구랑 사진 찍음' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 5) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][C 팔로우대기] 야경 좋다' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 8) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][C 팔로우대기] 팔로워전용 (안 보여야 정상)' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 3) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][C 팔로우대기] 제주시 (팔로잉 지도에 안 보여야 정상)' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 15) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][C 팔로우대기] 전주 한옥마을 (팔로잉 지도에 안 보여야 정상)' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 15) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][C 팔로우대기] 강릉 안목해변 (팔로잉 지도에 안 보여야 정상)' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 15) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 벤치에서 쉬다 감' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 15) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 노을 진짜 예쁨' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 6) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 고양이 있음' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 1) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 과제하다 탈출' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 3) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 비 오는 날 산책' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 6) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 카페 조용해서 좋음' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 30) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 팔로워전용 (안 보여야 정상)' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 5) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 1.2km 밖 (주변에 안 보여야 정상)' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 50) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 정문 밀집 1' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 4) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 정문 밀집 2' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 15) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 정문 밀집 3' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 16) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 정문 밀집 5' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 2) t;
SET @g = (SELECT guestbook_id FROM guestbooks WHERE content = '[seed][D 모르는사람] 정문 밀집 6' LIMIT 1);
INSERT INTO guestbook_likes (guestbook_id, user_id) SELECT @g, NULL FROM (SELECT 1 FROM information_schema.columns LIMIT 5) t;

-- ---------- 결과 확인 ----------
SELECT u.nickname, g.visibility, COUNT(*) AS 개수
FROM guestbooks g JOIN users u ON u.user_id = g.user_id
WHERE g.content LIKE '[seed]%' GROUP BY u.nickname, g.visibility ORDER BY u.nickname;

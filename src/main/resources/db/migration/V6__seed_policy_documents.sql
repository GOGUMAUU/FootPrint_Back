-- 기본 약관 문서. 이미 같은 (type, version)이 있는 DB(로컬 등)에서는 건너뛴다.
INSERT IGNORE INTO policy_documents (type, version, content, consent_required, required_at, withdrawable, published, published_at) VALUES
('TERMS',    'v1', '서비스 이용약관 (과제용 예시)',         TRUE,  'SIGNUP',  FALSE, TRUE, '2026-01-01 00:00:00.000000'),
('PRIVACY',  'v1', '개인정보 처리방침 (과제용 예시)',       FALSE, 'SIGNUP',  FALSE, TRUE, '2026-01-01 00:00:00.000000'),
('LOCATION', 'v1', '위치기반서비스 이용약관 (과제용 예시)',  TRUE,  'FEATURE', TRUE,  TRUE, '2026-01-01 00:00:00.000000');

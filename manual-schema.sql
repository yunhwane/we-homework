-- H2 콘솔에서 직접 실행할 SQL
-- URL: http://localhost:8080/h2-console
-- JDBC URL: jdbc:h2:mem:point-db
-- User: sa
-- Password: (비워둠)

DROP TABLE IF EXISTS points;

CREATE TABLE points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount INT NOT NULL,
    order_num BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_user_id ON points(user_id);
CREATE INDEX idx_order_num ON points(order_num);

-- 테이블 확인
SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'POINTS';
SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'POINTS';

-- 테스트 데이터 삽입
INSERT INTO points (user_id, amount, order_num) VALUES (1, 100, 1);
SELECT * FROM points;
DELETE FROM points WHERE user_id = 1;

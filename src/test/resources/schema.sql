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
CREATE INDEX idx_created_at ON points(created_at);
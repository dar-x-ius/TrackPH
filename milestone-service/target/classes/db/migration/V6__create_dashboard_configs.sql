CREATE TABLE IF NOT EXISTS dashboard_configs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        NOT NULL UNIQUE,
    widget_config TEXT,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dashboard_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

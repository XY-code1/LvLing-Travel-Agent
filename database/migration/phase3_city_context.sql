SET NAMES utf8mb4;
USE `scenic_ai_guide`;

CREATE TABLE IF NOT EXISTS `city` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city_code` varchar(64) NOT NULL,
  `city_name` varchar(100) NOT NULL,
  `province` varchar(100) NULL,
  `country` varchar(100) NOT NULL DEFAULT '中国',
  `description` text NULL,
  `slogan` varchar(255) NULL,
  `cover_image` varchar(500) NULL,
  `hero_images` text NULL,
  `theme_config` text NULL,
  `weather_code` varchar(64) NULL,
  `longitude` decimal(10,6) NULL,
  `latitude` decimal(10,6) NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_city_code` (`city_code`),
  KEY `idx_city_name_status` (`city_name`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

ALTER TABLE `scenic` ADD COLUMN `city_id` bigint NULL AFTER `id`;
ALTER TABLE `spot` ADD COLUMN `city_id` bigint NULL AFTER `id`;
ALTER TABLE `route` ADD COLUMN `city_id` bigint NULL AFTER `id`;
ALTER TABLE `route_spot` ADD COLUMN `city_id` bigint NULL AFTER `id`;
ALTER TABLE `admin_feature_item` ADD COLUMN `city_id` bigint NULL AFTER `id`;
ALTER TABLE `kb_document` ADD COLUMN `city_id` bigint NULL AFTER `id`;
ALTER TABLE `kb_chunk` ADD COLUMN `city_id` bigint NULL AFTER `id`;
ALTER TABLE `chat_session` ADD COLUMN `city_id` bigint NULL AFTER `id`;
ALTER TABLE `avatar_config` ADD COLUMN `city_id` bigint NULL AFTER `id`;

ALTER TABLE `scenic` ADD KEY `idx_scenic_city_id` (`city_id`);
ALTER TABLE `spot` ADD KEY `idx_spot_city_status_hot` (`city_id`, `status`, `is_hot`);
ALTER TABLE `route` ADD KEY `idx_route_city_status` (`city_id`, `status`);
ALTER TABLE `route_spot` ADD KEY `idx_route_spot_city_id` (`city_id`);
ALTER TABLE `admin_feature_item` ADD KEY `idx_feature_city_module` (`city_id`, `module_type`, `status`);
ALTER TABLE `kb_document` ADD KEY `idx_kb_document_city_id` (`city_id`);
ALTER TABLE `kb_chunk` ADD KEY `idx_kb_chunk_city_id` (`city_id`);
ALTER TABLE `chat_session` ADD KEY `idx_chat_session_city_id` (`city_id`);
ALTER TABLE `avatar_config` ADD KEY `idx_avatar_city_id` (`city_id`);

INSERT INTO `city` (`city_code`, `city_name`, `province`, `description`, `slogan`, `sort_order`)
SELECT 'wuxi-demo', '无锡', '江苏省', 'Guido 原有灵山示范数据的默认城市。', '太湖灵山，江南好风光', 10
WHERE NOT EXISTS (SELECT 1 FROM `city` WHERE `city_code` = 'wuxi-demo');

INSERT INTO `city` (`city_code`, `city_name`, `province`, `description`, `slogan`, `sort_order`)
SELECT 'hangzhou-demo', '杭州', '浙江省', '用于验证多城市隔离的轻量种子城市。', '西湖之外，发现杭州', 20
WHERE NOT EXISTS (SELECT 1 FROM `city` WHERE `city_code` = 'hangzhou-demo');

UPDATE `scenic` s JOIN `city` c ON c.city_code = 'wuxi-demo' SET s.city_id = c.id WHERE s.city_id IS NULL;
UPDATE `spot` s JOIN `scenic` a ON a.id = s.scenic_id SET s.city_id = a.city_id WHERE s.city_id IS NULL;
UPDATE `route` r JOIN `scenic` a ON a.id = r.scenic_id SET r.city_id = a.city_id WHERE r.city_id IS NULL;
UPDATE `route_spot` rs JOIN `route` r ON r.id = rs.route_id SET rs.city_id = r.city_id WHERE rs.city_id IS NULL;
UPDATE `admin_feature_item` f JOIN `scenic` a ON a.id = f.scenic_id SET f.city_id = a.city_id WHERE f.city_id IS NULL;
UPDATE `kb_document` d JOIN `scenic` a ON a.id = d.scenic_id SET d.city_id = a.city_id WHERE d.city_id IS NULL;
UPDATE `kb_chunk` k JOIN `kb_document` d ON d.id = k.doc_id SET k.city_id = d.city_id WHERE k.city_id IS NULL;
UPDATE `chat_session` s JOIN `scenic` a ON a.id = s.scenic_id SET s.city_id = a.city_id WHERE s.city_id IS NULL;
UPDATE `avatar_config` SET city_id = (SELECT id FROM city WHERE city_code = 'wuxi-demo' LIMIT 1) WHERE city_id IS NULL;

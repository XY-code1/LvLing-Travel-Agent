SET NAMES utf8mb4;
USE `scenic_ai_guide`;
CREATE TABLE IF NOT EXISTS `sos_request` (
  `id` bigint NOT NULL AUTO_INCREMENT, `request_no` varchar(40) NOT NULL,
  `user_id` bigint NULL, `city_id` bigint NULL, `scenic_id` bigint NULL,
  `help_type` varchar(32) NOT NULL, `urgency` varchar(20) NOT NULL DEFAULT 'NORMAL',
  `location_text` varchar(255) NOT NULL, `longitude` decimal(10,6) NULL, `latitude` decimal(10,6) NULL,
  `phone` varchar(24) NOT NULL, `description` text NOT NULL, `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL, `update_by` bigint NULL, `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_sos_request_no` (`request_no`),
  KEY `idx_sos_status_created` (`status`, `create_time`), KEY `idx_sos_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

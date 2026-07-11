SET NAMES utf8mb4;
SET time_zone = '+08:00';

CREATE DATABASE IF NOT EXISTS `scenic_ai_guide`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `scenic_ai_guide`;

CREATE TABLE `sys_admin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `real_name` varchar(50) NULL,
  `role` varchar(20) NOT NULL DEFAULT 'admin',
  `status` tinyint NOT NULL DEFAULT 1,
  `last_login_time` datetime NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `ip` varchar(50) NULL,
  `user_agent` varchar(255) NULL,
  `status` tinyint NOT NULL,
  `msg` varchar(255) NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `tourist_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `phone` varchar(20) NOT NULL,
  `password` varchar(100) NOT NULL,
  `nickname` varchar(50) NULL,
  `avatar` varchar(255) NULL,
  `gender` tinyint NULL,
  `interest_tags` varchar(255) NULL,
  `avatar_config_id` bigint NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `last_login_time` datetime NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_avatar_config_id` (`avatar_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `scenic` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `intro` text NULL,
  `address` varchar(255) NULL,
  `open_time` varchar(255) NULL,
  `ticket_info` varchar(500) NULL,
  `traffic_info` varchar(500) NULL,
  `service_phone` varchar(50) NULL,
  `notice` text NULL,
  `cover_image` varchar(255) NULL,
  `longitude` decimal(10,6) NULL,
  `latitude` decimal(10,6) NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `spot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scenic_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `alias` varchar(255) NULL,
  `intro` text NULL,
  `history_culture` mediumtext NULL,
  `guide_text` mediumtext NULL,
  `tags` varchar(255) NULL,
  `images` varchar(1000) NULL,
  `stay_minutes` int NULL,
  `suit_crowd` varchar(255) NULL,
  `is_hot` tinyint NOT NULL DEFAULT 0,
  `longitude` decimal(10,6) NULL,
  `latitude` decimal(10,6) NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_scenic_id` (`scenic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `route` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scenic_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `type` tinyint NOT NULL,
  `intro` text NULL,
  `estimate_minutes` int NULL,
  `suit_crowd` varchar(255) NULL,
  `interest_tags` varchar(255) NULL,
  `recommend_reason` varchar(500) NULL,
  `notice` varchar(500) NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_scenic_id` (`scenic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `route_spot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `route_id` bigint NOT NULL,
  `spot_id` bigint NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_spot` (`route_id`, `spot_id`),
  KEY `idx_route_id` (`route_id`),
  KEY `idx_spot_id` (`spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 知识库文档（本地存储 + 解析/向量化状态，无任何 Dify 字段）
CREATE TABLE `kb_document` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scenic_id` bigint NULL,
  `file_name` varchar(255) NOT NULL,
  `file_type` varchar(20) NOT NULL,
  `file_path` varchar(500) NOT NULL,
  `file_size` bigint NULL,
  `char_count` int NULL,
  `chunk_count` int NOT NULL DEFAULT 0,
  `parse_status` tinyint NOT NULL DEFAULT 0 COMMENT '0待解析 1解析中 2成功 3失败',
  `embed_status` tinyint NOT NULL DEFAULT 0 COMMENT '0待向量化 1向量化中 2成功 3失败',
  `fail_msg` varchar(500) NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_scenic_id` (`scenic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 知识库分块+向量（本地向量库核心表，余弦检索在应用层进行）
CREATE TABLE `kb_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `doc_id` bigint NOT NULL,
  `scenic_id` bigint NULL,
  `spot_id` bigint NULL,
  `chunk_index` int NOT NULL,
  `content` mediumtext NOT NULL,
  `title_path` varchar(500) NULL,
  `token_count` int NULL,
  `embedding` mediumtext NULL COMMENT '向量JSON数组字符串',
  `embed_dim` int NULL,
  `embed_model` varchar(100) NULL,
  `source_name` varchar(255) NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_doc_id` (`doc_id`),
  KEY `idx_scenic_id` (`scenic_id`),
  KEY `idx_spot_id` (`spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 知识库测试记录
CREATE TABLE `kb_test_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question` varchar(500) NOT NULL,
  `answer` mediumtext NULL,
  `hit` tinyint NOT NULL DEFAULT 0,
  `top_score` decimal(6,4) NULL,
  `sources` text NULL,
  `cost_ms` int NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `ai_service_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `service_type` varchar(30) NOT NULL,
  `provider` varchar(50) NOT NULL,
  `protocol` varchar(30) NULL,
  `base_url` varchar(255) NULL,
  `api_key` varchar(1000) NULL,
  `access_key_id` varchar(500) NULL,
  `access_key_secret` varchar(1000) NULL,
  `app_key` varchar(255) NULL,
  `region` varchar(50) NULL,
  `model_name` varchar(100) NULL,
  `dataset_id` varchar(100) NULL,
  `extra_config` text NULL,
  `timeout_ms` int NOT NULL DEFAULT 10000,
  `retry_count` int NOT NULL DEFAULT 1,
  `capability_verified` tinyint NOT NULL DEFAULT 0,
  `verified_time` datetime NULL,
  `verify_msg` varchar(500) NULL,
  `is_default` tinyint NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 0,
  `remark` varchar(255) NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_service_type` (`service_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `avatar_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `provider` varchar(50) NOT NULL DEFAULT '本地Canvas数字人',
  `instance_id` varchar(100) NULL,
  `avatar_image` varchar(255) NULL,
  `gender` varchar(20) NOT NULL DEFAULT 'FEMALE',
  `appearance` varchar(500) NULL,
  `outfit` varchar(100) NULL,
  `outfit_image` varchar(255) NULL,
  `render_config` text NULL,
  `voice` varchar(50) NULL,
  `speech_rate` int NOT NULL DEFAULT 0,
  `welcome_text` varchar(500) NULL,
  `is_default` tinyint NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_no` varchar(64) NOT NULL,
  `tourist_user_id` bigint NOT NULL,
  `scenic_id` bigint NULL,
  `avatar_config_id` bigint NULL,
  `title` varchar(100) NULL,
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_time` datetime NULL,
  `message_count` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_tourist_user_id` (`tourist_user_id`),
  KEY `idx_avatar_config_id` (`avatar_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `session_no` varchar(64) NOT NULL,
  `tourist_user_id` bigint NOT NULL,
  `input_type` varchar(10) NOT NULL,
  `question` varchar(1000) NULL,
  `asr_text` varchar(1000) NULL,
  `image_url` varchar(500) NULL,
  `answer` mediumtext NULL,
  `sources` text NULL,
  `hit_kb` tinyint NOT NULL DEFAULT 0,
  `emotion` varchar(20) NULL,
  `audio_url` varchar(500) NULL,
  `stream_url` varchar(500) NULL,
  `cost_ms` int NULL,
  `success` tinyint NOT NULL DEFAULT 1,
  `error_msg` varchar(500) NULL,
  `need_supplement` tinyint NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_session_no` (`session_no`),
  KEY `idx_tourist_user_id` (`tourist_user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_emotion` (`emotion`),
  KEY `idx_input_type` (`input_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `tourist_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tourist_user_id` bigint NOT NULL,
  `session_no` varchar(64) NULL,
  `score` tinyint NULL,
  `content` varchar(1000) NULL,
  `emotion` varchar(20) NULL,
  `handle_status` tinyint NOT NULL DEFAULT 0 COMMENT '0待处理 1已回复 2已忽略',
  `reply_content` varchar(1000) NULL,
  `reply_time` datetime NULL,
  `handler` varchar(64) NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tourist_user_id` (`tourist_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `admin_feature_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module_type` varchar(40) NOT NULL COMMENT 'FACILITY/TAG/ROUTE_TYPE/ROUTE_INTEREST/ROUTE_CONTENT/PROMPT/SENSITIVE_WORD/PUSH_MESSAGE/ACHIEVEMENT/VOICE/TODO',
  `scenic_id` bigint NULL,
  `related_id` bigint NULL,
  `title` varchar(120) NOT NULL,
  `category` varchar(80) NULL,
  `content` text NULL,
  `media_url` varchar(500) NULL,
  `longitude` decimal(10,6) NULL,
  `latitude` decimal(10,6) NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(500) NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_module_type` (`module_type`),
  KEY `idx_scenic_id` (`scenic_id`),
  KEY `idx_related_id` (`related_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `sentiment_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_date` date NOT NULL,
  `positive_count` int NOT NULL DEFAULT 0,
  `neutral_count` int NOT NULL DEFAULT 0,
  `negative_count` int NOT NULL DEFAULT 0,
  `complaint_count` int NOT NULL DEFAULT 0,
  `hot_questions` text NULL,
  `hot_spots` text NULL,
  `unanswered` text NULL,
  `ai_suggestion` text NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_report_date` (`report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `sys_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `log_type` varchar(20) NOT NULL,
  `biz_desc` varchar(255) NULL,
  `operator` varchar(64) NULL,
  `service_provider` varchar(50) NULL,
  `request_summary` text NULL,
  `response_summary` text NULL,
  `cost_ms` int NULL,
  `success` tinyint NOT NULL DEFAULT 1,
  `error_msg` varchar(1000) NULL,
  `ip` varchar(50) NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_log_type` (`log_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_success` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `stat_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `service_count` int NOT NULL DEFAULT 0,
  `qa_count` int NOT NULL DEFAULT 0,
  `voice_count` int NOT NULL DEFAULT 0,
  `image_count` int NOT NULL DEFAULT 0,
  `avatar_count` int NOT NULL DEFAULT 0,
  `avg_cost_ms` int NOT NULL DEFAULT 0,
  `kb_hit_rate` decimal(5,2) NOT NULL DEFAULT 0,
  `unanswered_count` int NOT NULL DEFAULT 0,
  `negative_count` int NOT NULL DEFAULT 0,
  `error_count` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `demo_switch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `switch_key` varchar(50) NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT 0,
  `remark` varchar(255) NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint NULL,
  `update_by` bigint NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_switch_key` (`switch_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

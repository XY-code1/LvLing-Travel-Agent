USE `scenic_ai_guide`;

SET @schema_name = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'tourist_user' AND column_name = 'avatar_config_id') = 0,
  'ALTER TABLE `tourist_user` ADD COLUMN `avatar_config_id` bigint NULL AFTER `interest_tags`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'tourist_user' AND index_name = 'idx_avatar_config_id') = 0,
  'ALTER TABLE `tourist_user` ADD INDEX `idx_avatar_config_id` (`avatar_config_id`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'chat_session' AND column_name = 'avatar_config_id') = 0,
  'ALTER TABLE `chat_session` ADD COLUMN `avatar_config_id` bigint NULL AFTER `scenic_id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'chat_session' AND index_name = 'idx_avatar_config_id') = 0,
  'ALTER TABLE `chat_session` ADD INDEX `idx_avatar_config_id` (`avatar_config_id`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `avatar_config`
SET `voice` = 'xiaoyun'
WHERE `deleted` = 0 AND `gender` = 'FEMALE';

UPDATE `avatar_config`
SET `voice` = 'xiaogang'
WHERE `deleted` = 0 AND `gender` = 'MALE';

UPDATE `tourist_user` user
JOIN (
  SELECT `id`
  FROM `avatar_config`
  WHERE `deleted` = 0 AND `enabled` = 1 AND `is_default` = 1
  ORDER BY `id` DESC
  LIMIT 1
) avatar ON 1 = 1
SET user.`avatar_config_id` = avatar.`id`
WHERE user.`deleted` = 0 AND user.`avatar_config_id` IS NULL;

UPDATE `chat_session` session
JOIN `tourist_user` user ON user.`id` = session.`tourist_user_id`
SET session.`avatar_config_id` = user.`avatar_config_id`
WHERE session.`deleted` = 0 AND session.`avatar_config_id` IS NULL;

USE `scenic_ai_guide`;

SET @schema_name = DATABASE();
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'avatar_config' AND column_name = 'gender') = 0,
  'ALTER TABLE `avatar_config` ADD COLUMN `gender` varchar(20) NOT NULL DEFAULT ''FEMALE'' AFTER `avatar_image`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'avatar_config' AND column_name = 'appearance') = 0,
  'ALTER TABLE `avatar_config` ADD COLUMN `appearance` varchar(500) NULL AFTER `gender`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'avatar_config' AND column_name = 'outfit') = 0,
  'ALTER TABLE `avatar_config` ADD COLUMN `outfit` varchar(100) NULL AFTER `appearance`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'avatar_config' AND column_name = 'outfit_image') = 0,
  'ALTER TABLE `avatar_config` ADD COLUMN `outfit_image` varchar(255) NULL AFTER `outfit`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'avatar_config' AND column_name = 'render_config') = 0,
  'ALTER TABLE `avatar_config` ADD COLUMN `render_config` text NULL AFTER `outfit_image`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE `avatar_config` ALTER COLUMN `provider` SET DEFAULT '本地Canvas数字人';

CREATE TEMPORARY TABLE `avatar_seed` (
  `name` varchar(100) NOT NULL,
  `provider` varchar(50) NOT NULL,
  `gender` varchar(20) NOT NULL,
  `appearance` varchar(500) NULL,
  `avatar_image` varchar(255) NOT NULL,
  `outfit` varchar(100) NULL,
  `outfit_image` varchar(255) NULL,
  `render_config` text NULL,
  `voice` varchar(50) NULL,
  `speech_rate` int NOT NULL,
  `welcome_text` varchar(500) NULL,
  `is_default` tinyint NOT NULL,
  `enabled` tinyint NOT NULL,
  `remark` varchar(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @avatar_render_config = '{"mouth":{"x":0.5,"y":0.13,"width":0.048,"height":0.018},"crop":{"x":0.5,"y":0.24,"scale":2.6},"breath":0.012,"blink":true}';

INSERT INTO `avatar_seed` VALUES
  ('灵山文化女导游','本地Canvas数字人','FEMALE','亲和、端庄、适合文化景区讲解的女性数字导游。','/files/avatar/female_cultural_qipao_06.png','淡雅旗袍','/files/avatar/female_cultural_qipao_06.png',@avatar_render_config,'xiaoyun',0,'你好，我是你的灵山文化导游，可以为你讲解景点、规划路线，也可以识别你拍到的景观。',1,1,'默认文化特色女性形象'),
  ('花纹连衣裙女导游','本地Canvas数字人','FEMALE','温和、自然、适合亲子和休闲游览讲解。','/files/avatar/female_cultural_floral_05.png','浅色花纹连衣裙','/files/avatar/female_cultural_floral_05.png',@avatar_render_config,'xiaoyun',0,'欢迎来到景区，我会结合你的兴趣为你介绍沿途景点。',0,1,'女性文化休闲形象'),
  ('商务休闲女导游','本地Canvas数字人','FEMALE','清爽、专业、适合服务咨询和综合导览。','/files/avatar/female_business_casual_01.png','白衬衫米色长裤','/files/avatar/female_business_casual_01.png',@avatar_render_config,'xiaoyun',0,'你好，我可以帮你查询景点信息、开放时间和路线建议。',0,1,'女性商务休闲形象'),
  ('职业套装女导游','本地Canvas数字人','FEMALE','稳重、正式，适合游客服务中心和管理型讲解场景。','/files/avatar/female_business_suit_02.png','黑色职业套装','/files/avatar/female_business_suit_02.png',@avatar_render_config,'xiaoyun',0,'您好，我是景区 AI 导游，请告诉我你想了解的内容。',0,1,'女性职业形象'),
  ('休闲卫衣女导游','本地Canvas数字人','FEMALE','轻松、年轻，适合家庭游客和轻量问答。','/files/avatar/female_casual_hoodie_03.png','白色卫衣牛仔裤','/files/avatar/female_casual_hoodie_03.png',@avatar_render_config,'xiaoyun',0,'嗨，我可以陪你边逛边了解景点故事。',0,1,'女性休闲形象'),
  ('运动休闲女导游','本地Canvas数字人','FEMALE','活力、轻快，适合路线推荐和运动型游客。','/files/avatar/female_activewear_04.png','浅紫运动上衣','/files/avatar/female_activewear_04.png',@avatar_render_config,'xiaoyun',0,'我可以根据你的体力和时间推荐更合适的游览路线。',0,1,'女性运动形象'),
  ('灵山文化男导游','本地Canvas数字人','MALE','沉稳、儒雅、适合文化景区深度讲解的男性数字导游。','/files/avatar/male_cultural_tang_06.png','灰色中式唐装','/files/avatar/male_cultural_tang_06.png',@avatar_render_config,'xiaogang',0,'你好，我是你的景区数字导游，可以为你讲解历史文化与参观路线。',0,1,'男性文化特色形象'),
  ('商务休闲男导游','本地Canvas数字人','MALE','清爽、专业，适合标准导览和问答服务。','/files/avatar/male_business_casual_01.png','白衬衫米色长裤','/files/avatar/male_business_casual_01.png',@avatar_render_config,'xiaogang',0,'您好，我可以帮你查询景点、路线、服务设施和注意事项。',0,1,'男性商务休闲形象'),
  ('职业套装男导游','本地Canvas数字人','MALE','正式、稳重，适合游客中心和综合咨询场景。','/files/avatar/male_business_suit_02.png','黑色职业西装','/files/avatar/male_business_suit_02.png',@avatar_render_config,'xiaogang',0,'欢迎来到景区，我会用简洁清晰的方式为你讲解。',0,1,'男性职业形象'),
  ('休闲卫衣男导游','本地Canvas数字人','MALE','亲切、年轻，适合轻松问答和亲子游客。','/files/avatar/male_casual_hoodie_03.png','白色卫衣牛仔裤','/files/avatar/male_casual_hoodie_03.png',@avatar_render_config,'xiaogang',0,'我可以陪你轻松逛景区，也可以帮你快速找到想去的景点。',0,1,'男性休闲形象'),
  ('运动休闲男导游','本地Canvas数字人','MALE','轻快、有活力，适合路线和体力友好型建议。','/files/avatar/male_activewear_04.png','灰色运动外套','/files/avatar/male_activewear_04.png',@avatar_render_config,'xiaogang',0,'告诉我你的游览时间，我可以帮你安排更顺路的路线。',0,1,'男性运动形象'),
  ('短袖休闲男导游','本地Canvas数字人','MALE','阳光、清爽，适合夏季和轻量导览场景。','/files/avatar/male_polo_casual_05.png','蓝色 POLO 衫','/files/avatar/male_polo_casual_05.png',@avatar_render_config,'xiaogang',0,'我可以快速回答你关心的路线、景点和服务问题。',0,1,'男性夏季休闲形象');

UPDATE `avatar_config` SET `is_default` = 0 WHERE `deleted` = 0;

UPDATE `avatar_config` target
JOIN `avatar_seed` seed ON target.`avatar_image` = seed.`avatar_image` AND target.`deleted` = 0
SET target.`name` = seed.`name`,
    target.`provider` = seed.`provider`,
    target.`gender` = seed.`gender`,
    target.`appearance` = seed.`appearance`,
    target.`outfit` = seed.`outfit`,
    target.`outfit_image` = seed.`outfit_image`,
    target.`render_config` = seed.`render_config`,
    target.`voice` = seed.`voice`,
    target.`speech_rate` = seed.`speech_rate`,
    target.`welcome_text` = seed.`welcome_text`,
    target.`is_default` = seed.`is_default`,
    target.`enabled` = seed.`enabled`,
    target.`remark` = seed.`remark`;

INSERT INTO `avatar_config` (
  `name`,`provider`,`gender`,`appearance`,`avatar_image`,`outfit`,`outfit_image`,
  `render_config`,`voice`,`speech_rate`,`welcome_text`,`is_default`,`enabled`,`remark`,`deleted`
)
SELECT
  seed.`name`, seed.`provider`, seed.`gender`, seed.`appearance`, seed.`avatar_image`,
  seed.`outfit`, seed.`outfit_image`, seed.`render_config`, seed.`voice`,
  seed.`speech_rate`, seed.`welcome_text`, seed.`is_default`, seed.`enabled`, seed.`remark`, 0
FROM `avatar_seed` seed
WHERE NOT EXISTS (
  SELECT 1 FROM `avatar_config` target
  WHERE target.`avatar_image` = seed.`avatar_image` AND target.`deleted` = 0
);

DROP TEMPORARY TABLE `avatar_seed`;

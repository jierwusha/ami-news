DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `username` text,
  `password` text,
  `email` text,
  `create_time` timestamp,
  `update_time` timestamp
);

DROP TABLE IF EXISTS `subscribe`;
CREATE TABLE `subscribe` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `user_id` integer,
  `channel_id` integer,
  `create_time` timestamp,
  `update_time` timestamp
);

DROP TABLE IF EXISTS `channel`;
CREATE TABLE `channel` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `title` text,
  `link` text,
  `url` text,
  `atom_link` text,
  `description` text,
  `icon` text,
  `create_time` timestamp,
  `update_time` timestamp
);

DROP TABLE IF EXISTS `item`;
CREATE TABLE `item` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `channel_id` integer,
  `title` text,
  `description` mediumtext,
  `link` text,
  `ai_description` text,
  `pub_date` text,
  `create_time` timestamp,
  `update_time` timestamp
);

DROP TABLE IF EXISTS `folder_channel`;
CREATE TABLE `folder_channel` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `folder_id` integer,
  `channel_id` integer,
  `create_time` timestamp,
  `update_time` timestamp
);

DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `user_id` integer,
  `name` text,
  `create_time` timestamp,
  `update_time` timestamp
);

DROP TABLE IF EXISTS `item_tag`;
CREATE TABLE `item_tag` (
    `id` integer PRIMARY KEY AUTO_INCREMENT,
    `item_id` integer,
    `tag_id` integer,
    `create_time` timestamp,
    `update_time` timestamp
);

DROP TABLE IF EXISTS `read_later`;
CREATE TABLE `read_later` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `user_id` integer,
  `item_id` integer,
  `create_time` timestamp,
  `update_time` timestamp
);

DROP TABLE IF EXISTS `folder`;
CREATE TABLE `folder` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `user_id` integer,
  `name` text,
  `create_time` timestamp,
  `update_time` timestamp
);

DROP TABLE IF EXISTS `hot_word`;
CREATE TABLE `hot_word` (
    `id` integer PRIMARY KEY AUTO_INCREMENT,
    `word` text,
    `user_id` integer,
    `count` integer,
    `create_time` timestamp,
    `update_time` timestamp
);

DROP TABLE IF EXISTS `hot_word_item`;
CREATE TABLE `hot_word_item` (
    `id` integer PRIMARY KEY AUTO_INCREMENT,
    `hot_word_id` integer,
    `item_id` integer,
    `create_time` timestamp,
    `update_time` timestamp
);

DROP TABLE IF EXISTS `qa_record`;
CREATE TABLE `qa_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` INTEGER NOT NULL COMMENT '用户ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `question` TEXT NOT NULL COMMENT '用户问题',
    `answer` TEXT COMMENT 'AI回答',
    `context_items` TEXT COMMENT '相关新闻ID，JSON格式',
    `retrieval_score` DOUBLE COMMENT '检索评分',
    `created_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_created_time (created_time)
);
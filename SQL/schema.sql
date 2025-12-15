/*
 * j-atomic-id Database Schema
 * Version: 1.0.0
 * Database: MySQL 5.7+ / 8.0+
 */

-- Create Database / 创建数据库
CREATE DATABASE IF NOT EXISTS `j_atomic_id` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `j_atomic_id`;

-- ----------------------------
-- Table structure for t_id_generator
-- ID 生成器核心表：记录每个业务 Tag 当前分发到的最大 ID
-- ----------------------------
DROP TABLE IF EXISTS `t_id_generator`;

CREATE TABLE `t_id_generator` (
  `biz_tag` varchar(128) NOT NULL COMMENT 'Business Tag (Unique Key) / 业务标识 (主键, 如 order, chat_100)',
  `max_id` bigint(20) NOT NULL DEFAULT '0' COMMENT 'Current Max ID / 当前已分配的最大ID',
  `step` int(11) DEFAULT '1' COMMENT 'Allocation Step (Verification only) / 步长 (仅供参考)',
  `description` varchar(255) DEFAULT NULL COMMENT 'Description / 业务描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time / 创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time / 更新时间',
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ID Generator Core Table / ID生成器核心表';

-- ----------------------------
-- Init Data (Optional) / 初始化测试数据 (可选)
-- ----------------------------
INSERT INTO `t_id_generator` (`biz_tag`, `max_id`, `step`, `description`)
VALUES ('test_order', 0, 1000, 'Test Order ID / 测试订单ID');
/*
 Navicat Premium Data Transfer

 Source Server         : jeecg-boot
 Source Server Type    : MySQL
 Source Server Version : 50612
 Source Host           : localhost:3306
 Source Schema         : jeecg-boot

 Target Server Type    : MySQL
 Target Server Version : 50612
 File Encoding         : 65001

 Date: 16/08/2021 09:28:44
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for generator_code
-- ----------------------------
DROP TABLE IF EXISTS `generator_code`;
CREATE TABLE `generator_code`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '唯一标识',
  `template_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '模板ID',
  `table_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '表名称 具有唯一性',
  `table_name_plus` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '创建的表名',
  `is_generator_code` int(11) NULL DEFAULT NULL COMMENT '是否生成代码',
  `code_path` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '代码存放路径',
  `is_generator_load` int(11) NULL DEFAULT NULL COMMENT '是否加载',
  `is_enable_url` int(11) NULL DEFAULT 1 COMMENT '是否启用接口',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间 ',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间 ',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人 ',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人 ',
  `fields_info` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '字段信息(name,type,length等)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `table_name_union`(`table_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;

/*
 Navicat Premium Data Transfer

 Source Server         : 本地
 Source Server Type    : MySQL
 Source Server Version : 50730
 Source Host           : localhost:3306
 Source Schema         : data_cloud

 Target Server Type    : MySQL
 Target Server Version : 50730
 File Encoding         : 65001

 Date: 03/05/2022 12:06:03
*/

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `eladmin` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `eladmin`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for dynamic_20201208203549
-- ----------------------------
DROP TABLE IF EXISTS `dynamic_20201208203549`;
CREATE TABLE `dynamic_20201208203549`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态（0禁用，1启用）',
  `create_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '测试1102' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dynamic_20201208203549
-- ----------------------------
INSERT INTO `dynamic_20201208203549` VALUES ('1336489949670727682', 1, '1214835832967581698', '2020-12-09 09:56:49', '1197789917762031617', '1214835832967581698', '2020-12-09 09:56:49', '名称1');
INSERT INTO `dynamic_20201208203549` VALUES ('1346385919621922818', 1, '1214835832967581698', '2021-01-05 17:19:53', '1197789917762031617', '1214835832967581698', '2021-01-05 17:19:53', 'we');

-- ----------------------------
-- Table structure for dynamic_20220501184411
-- ----------------------------
DROP TABLE IF EXISTS `dynamic_20220501184411`;
CREATE TABLE `dynamic_20220501184411`  (
  `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态（0禁用，1启用）',
  `create_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `age` int(11) NOT NULL COMMENT '年龄',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '測試模型1' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dynamic_20220501184411
-- ----------------------------

-- ----------------------------
-- Table structure for flow_business
-- ----------------------------
DROP TABLE IF EXISTS `flow_business`;
CREATE TABLE `flow_business`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `business_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务编码',
  `business_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务名称',
  `business_component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务组件',
  `business_audit_group` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务审核用户组',
  `process_definition_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '流程定义ID',
  `business_tempalte` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '消息模板',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '业务流程配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_business
-- ----------------------------
INSERT INTO `flow_business` VALUES ('1520746942496317441', 1, '1214835832967581698', '2022-05-01 20:48:20', '1197789917762031617', '1214835832967581698', '2022-05-01 20:48:20', NULL, '5011', '数据模型', '/masterdata/datamodel/index', '1214826565321543682', 'businessAudit:1:2508', '业务名称：{businessName}，发起人：{nickname}，业务编号：{businessKey}');

-- ----------------------------
-- Table structure for flow_category
-- ----------------------------
DROP TABLE IF EXISTS `flow_category`;
CREATE TABLE `flow_category`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分类名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '流程分类表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_category
-- ----------------------------
INSERT INTO `flow_category` VALUES ('1304285055312584706', 1, '1214835832967581698', '2022-05-01 18:08:04', '1197789917762031617', '1214835832967581698', '2022-05-01 18:08:04', NULL, '业务管理');

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表名称',
  `table_comment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表描述',
  `class_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '实体类名称',
  `package_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成功能作者',
  `column_json` json NULL COMMENT '表字段',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代码生成信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of gen_table
-- ----------------------------

-- ----------------------------
-- Table structure for market_api
-- ----------------------------
DROP TABLE IF EXISTS `market_api`;
CREATE TABLE `market_api`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `api_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'API名称',
  `api_version` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'API版本',
  `api_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'API路径',
  `req_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求方式',
  `res_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '返回格式',
  `deny` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'IP黑名单多个，隔开',
  `source_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源id',
  `limit_json` json NULL COMMENT '限流配置',
  `config_json` json NULL COMMENT '执行配置',
  `req_json` json NULL COMMENT '请求参数',
  `res_json` json NULL COMMENT '返回参数',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据API信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of market_api
-- ----------------------------
INSERT INTO `market_api` VALUES ('1297816482595483650', 2, '1214835832967581698', '2020-08-24 16:42:16', '1197789917762031617', '1214835832967581698', '2022-05-02 19:14:28', NULL, '部位信息', 'v1.0.0', '/part/info', 'GET', 'JSON', NULL, '1336474987430793217', '{\"times\": 5, \"enable\": \"0\", \"seconds\": 60}', '{\"sqlText\": \"SELECT id, part_name FROM robot_symptom_part WHERE 1 = 1 ${AND id = :id}\", \"tableId\": \"1336479264639406082\", \"sourceId\": \"1336474987430793217\", \"tableName\": \"robot_symptom_part\", \"configType\": \"1\", \"fieldParams\": [{\"reqable\": \"1\", \"resable\": \"1\", \"dataType\": \"varchar\", \"columnKey\": \"1\", \"dataScale\": null, \"columnName\": \"id\", \"dataLength\": 50, \"dataDefault\": null, \"columnComment\": \"主键\", \"dataPrecision\": null, \"columnNullable\": \"0\", \"columnPosition\": 1}, {\"reqable\": null, \"resable\": \"1\", \"dataType\": \"varchar\", \"columnKey\": \"0\", \"dataScale\": null, \"columnName\": \"part_name\", \"dataLength\": 255, \"dataDefault\": null, \"columnComment\": \"部位名称\", \"dataPrecision\": null, \"columnNullable\": \"1\", \"columnPosition\": 2}]}', '[{\"nullable\": \"0\", \"paramName\": \"id\", \"paramType\": \"1\", \"whereType\": \"1\", \"defaultValue\": \"111\", \"exampleValue\": \"111\", \"paramComment\": \"主键\"}]', '[{\"dataType\": \"varchar\", \"fieldName\": \"id\", \"exampleValue\": \"111\", \"fieldComment\": \"主键\", \"fieldAliasName\": null}, {\"dataType\": \"varchar\", \"fieldName\": \"part_name\", \"exampleValue\": \"部位名称\", \"fieldComment\": \"部位名称\", \"fieldAliasName\": null}]');
INSERT INTO `market_api` VALUES ('1298181433067651074', 3, '1214835832967581698', '2020-08-25 16:52:27', '1197789917762031617', '1214835832967581698', '2022-05-02 19:05:46', NULL, '症状信息', 'v1.0.0', '/symptom/info', 'GET', 'JSON', NULL, '1336474987430793217', '{\"times\": 5, \"enable\": \"0\", \"seconds\": 60}', '{\"sqlText\": \"select id, part_id, type_name from robot_symptom_type WHERE 1 = 1 ${AND type_name LIKE :type_name}\", \"tableId\": null, \"sourceId\": \"1336474987430793217\", \"tableName\": null, \"configType\": \"2\", \"fieldParams\": []}', '[{\"nullable\": \"0\", \"paramName\": \"type_name\", \"paramType\": \"1\", \"whereType\": \"3\", \"defaultValue\": \"症状名称\", \"exampleValue\": \"症状名称\", \"paramComment\": \"症状名称\"}]', '[{\"dataType\": \"varchar\", \"fieldName\": \"id\", \"exampleValue\": \"11\", \"fieldComment\": \"主键\", \"fieldAliasName\": \"\"}, {\"dataType\": \"varchar\", \"fieldName\": \"part_id\", \"exampleValue\": \"所属部位\", \"fieldComment\": \"所属部位\", \"fieldAliasName\": \"\"}, {\"dataType\": \"varchar\", \"fieldName\": \"type_name\", \"exampleValue\": \"症状名称\", \"fieldComment\": \"症状名称\", \"fieldAliasName\": \"\"}]');

-- ----------------------------
-- Table structure for market_api_log
-- ----------------------------
DROP TABLE IF EXISTS `market_api_log`;
CREATE TABLE `market_api_log`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `api_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调用api',
  `caller_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调用者id',
  `caller_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调用者ip',
  `caller_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调用url',
  `caller_params` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调用参数',
  `caller_date` datetime(0) NULL DEFAULT NULL COMMENT '调用时间',
  `caller_size` int(11) NULL DEFAULT NULL COMMENT '调用数据量',
  `time` int(11) NULL DEFAULT NULL COMMENT '调用耗时',
  `msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '信息记录',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0失败，1成功）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'api调用日志信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of market_api_log
-- ----------------------------
INSERT INTO `market_api_log` VALUES ('1277944406174965761', '1275774099624386562', '1214835832967581698', '192.168.0.107', '/v1/dept/info', '{\"pageNum\":1,\"pageSize\":20,\"id\":\"111\"}', '2020-06-30 20:37:44', 0, 241, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1277945307115659265', '1275774099624386562', '1214835832967581698', '192.168.0.107', '/v1/dept/info', '{\"pageNum\":1,\"pageSize\":20,\"id\":\"111\"}', '2020-06-30 20:41:19', 0, 28, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1277945401969844225', '1275774099624386562', '1214835832967581698', '192.168.0.107', '/v1/dept/info', '{\"pageNum\":1,\"pageSize\":20,\"id\":\"1197789917762031617\"}', '2020-06-30 20:41:42', 1, 36, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1278169492177879042', '1275774099624386562', '1214835832967581698', '192.168.0.107', '/v1/dept/info', '{\"dept_name\":\"xx科技\",\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2020-07-01 11:32:09', NULL, 151, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1297817787422519297', '1297816482595483650', '1214835832967581698', '192.168.3.36', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"ID\":\"62207ec3cd713e906c461dfbfddf6504\",\"pageNum\":\"1\"}', '2020-08-24 16:47:27', NULL, NULL, 'java.lang.String cannot be cast to java.lang.Integer', 0);
INSERT INTO `market_api_log` VALUES ('1297818772886827010', '1297816482595483650', '1214835832967581698', '192.168.3.36', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"ID\":\"62207ec3cd713e906c461dfbfddf6504\",\"pageNum\":\"1\"}', '2020-08-24 16:51:22', NULL, NULL, 'java.lang.String cannot be cast to java.lang.Integer', 0);
INSERT INTO `market_api_log` VALUES ('1297819115108478977', '1297816482595483650', '1214835832967581698', '192.168.3.36', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"ID\":\"62207ec3cd713e906c461dfbfddf6504\",\"pageNum\":\"1\"}', '2020-08-24 16:52:44', NULL, NULL, 'java.lang.String cannot be cast to java.lang.Integer', 0);
INSERT INTO `market_api_log` VALUES ('1297819887028187138', '1297816482595483650', '1214835832967581698', '192.168.3.36', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"ID\":\"62207ec3cd713e906c461dfbfddf6504\",\"pageNum\":\"1\"}', '2020-08-24 16:55:48', NULL, NULL, 'API调用查询数据脱敏出错', 0);
INSERT INTO `market_api_log` VALUES ('1297820525254455298', '1297816482595483650', '1214835832967581698', '192.168.3.36', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"ID\":\"62207ec3cd713e906c461dfbfddf6504\",\"pageNum\":\"1\"}', '2020-08-24 16:58:20', 1, 1684, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1298182334620733441', '1298181433067651074', '1214835832967581698', '192.168.3.24', '/services/v1.0.0/symptom/info', '{\"pageSize\":\"20\",\"type_name\":\"肩酸\",\"pageNum\":\"1\"}', '2020-08-25 16:56:02', 1, 1961, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1298182566519607297', '1298181433067651074', '1214835832967581698', '192.168.3.24', '/services/v1.0.0/symptom/info', '{\"pageSize\":\"20\",\"type_name\":\"肩酸\",\"pageNum\":\"1\"}', '2020-08-25 16:56:58', 1, 168, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1298182705204269058', '1298181433067651074', '1214835832967581698', '192.168.3.24', '/services/v1.0.0/symptom/info', '{\"pageSize\":\"20\",\"type_name\":\"脑壳痛\",\"pageNum\":\"1\"}', '2020-08-25 16:57:31', 1, 126, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1306888486627872769', '1297816482595483650', '1214835832967581698', '192.168.3.24', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"ID\":\"ss\",\"pageNum\":\"1\"}', '2020-09-18 17:31:11', 0, 1553, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1336492989958180865', NULL, NULL, '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"3244c36870e4a47ef1fc6e2c1acf00a2\",\"pageNum\":\"1\"}', '2020-12-09 10:08:54', NULL, NULL, 'api_key或secret_key空', 0);
INSERT INTO `market_api_log` VALUES ('1336493024087232514', NULL, NULL, '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"3244c36870e4a47ef1fc6e2c1acf00a2\",\"pageNum\":\"1\"}', '2020-12-09 10:09:02', NULL, NULL, 'api_key或secret_key空', 0);
INSERT INTO `market_api_log` VALUES ('1336493851963150337', NULL, NULL, '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"3244c36870e4a47ef1fc6e2c1acf00a2\",\"pageNum\":\"1\"}', '2020-12-09 10:12:20', NULL, NULL, 'api_key或secret_key空', 0);
INSERT INTO `market_api_log` VALUES ('1336495784656490497', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"3244c36870e4a47ef1fc6e2c1acf00a2\",\"pageNum\":\"1\"}', '2020-12-09 10:20:01', 1, 1283, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1336504430345965570', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"11\",\"pageNum\":\"1\"}', '2020-12-09 10:54:22', 0, 495, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1336522026826977281', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2020-12-09 12:04:17', 15, 636, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1336522611206770689', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2020-12-09 12:06:36', 15, 1440, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1336523120294612993', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2020-12-09 12:08:38', 15, 595, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1336546302409953281', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2020-12-09 13:40:45', 15, 484, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1336546356252233730', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2020-12-09 13:40:58', 15, 471, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1336546562481967106', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2020-12-09 13:41:47', 15, 485, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1339499510509957121', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2020-12-17 17:15:44', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1339499534694313986', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"11\",\"pageNum\":\"1\"}', '2020-12-17 17:15:50', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1339499596816150530', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"11\",\"pageNum\":\"1\"}', '2020-12-17 17:16:05', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1339501957894729729', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"11\",\"pageNum\":\"1\"}', '2020-12-17 17:25:28', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1339502935163367426', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"11\",\"pageNum\":\"1\"}', '2020-12-17 17:29:21', 0, 1823, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1339727473700597761', '1297816482595483650', '1214835832967581698', '61.164.216.254', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"11\",\"pageNum\":\"1\"}', '2020-12-18 08:21:35', 0, 701, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1520411325585534977', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-04-30 22:34:42', 15, 1270, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1520416691882823681', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-04-30 22:56:02', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1520416801538707457', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-04-30 22:56:28', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1520416991989469185', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"1\",\"pageNum\":\"1\"}', '2022-04-30 22:57:14', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1520417128165937154', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-04-30 22:57:46', 15, 2031, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1520417163356147713', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-04-30 22:57:54', 15, 81, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1520417178774409217', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-04-30 22:57:58', 15, 69, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1520417183216177153', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-04-30 22:57:59', 15, 77, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1520417188593274882', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-04-30 22:58:00', 15, 83, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1520417619981635586', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-04-30 22:59:43', 15, 103, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1521085459130777601', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-02 19:13:28', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1521085590970335233', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-02 19:14:00', 15, 1051, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1521085791202213890', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-02 19:14:48', 15, 73, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1521143846422024194', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-02 23:05:29', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1521143930098388993', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-02 23:05:49', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1521144244641828865', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-02 23:07:04', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1521144345825218562', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"1\",\"pageNum\":\"1\"}', '2022-05-02 23:07:28', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1521144696183820289', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"id\":\"\",\"pageNum\":\"1\"}', '2022-05-02 23:08:52', NULL, NULL, NULL, 0);
INSERT INTO `market_api_log` VALUES ('1521150542175309825', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-02 23:32:05', 15, 1823, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1521150566489690114', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-02 23:32:11', 15, 86, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1521150576707014658', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-02 23:32:14', 15, 60, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1521150581001981953', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-02 23:32:15', 15, 70, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1521159277358649345', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-03 00:06:48', 15, 498, NULL, 1);
INSERT INTO `market_api_log` VALUES ('1521163456223416322', '1297816482595483650', '1214835832967581698', '127.0.0.1', '/services/v1.0.0/part/info', '{\"pageSize\":\"20\",\"pageNum\":\"1\"}', '2022-05-03 00:23:24', 15, 326, NULL, 1);

-- ----------------------------
-- Table structure for market_api_mask
-- ----------------------------
DROP TABLE IF EXISTS `market_api_mask`;
CREATE TABLE `market_api_mask`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '脱敏主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `api_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据API',
  `mask_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '脱敏名称',
  `config_json` json NULL COMMENT '脱敏字段规则配置',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据API脱敏信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of market_api_mask
-- ----------------------------
INSERT INTO `market_api_mask` VALUES ('1336507994732597250', 1, '1214835832967581698', '2020-12-09 11:08:32', '1197789917762031617', '1214835832967581698', '2020-12-09 13:41:31', NULL, '1297816482595483650', '1', '[{\"cryptType\": \"6\", \"fieldName\": \"id\", \"cipherType\": \"2\"}]');

-- ----------------------------
-- Table structure for market_service_integration
-- ----------------------------
DROP TABLE IF EXISTS `market_service_integration`;
CREATE TABLE `market_service_integration`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `service_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '服务编号',
  `service_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '服务名称',
  `service_type` tinyint(4) NULL DEFAULT NULL COMMENT '服务类型（1http，2webservice）',
  `httpservice_json` json NULL COMMENT 'http接口配置',
  `webservice_json` json NULL COMMENT 'webservice接口配置',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '服务集成表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of market_service_integration
-- ----------------------------
INSERT INTO `market_service_integration` VALUES ('1298954518389604354', 1, '1214835832967581698', '2020-08-27 20:04:25', '1197789917762031617', '1214835832967581698', '2020-08-27 20:04:25', NULL, '20200827001', '中英文双向翻译', 1, '{\"url\": \"http://fy.webxml.com.cn/webservices/EnglishChinese.asmx/TranslatorString\", \"param\": \"{\\\"wordKey\\\": \\\"我\\\"}\", \"header\": null, \"httpMethod\": \"POST\"}', '{\"soap\": null, \"wsdl\": null, \"method\": null, \"targetNamespace\": null}');
INSERT INTO `market_service_integration` VALUES ('1298954821444845569', 1, '1214835832967581698', '2020-08-27 20:05:38', '1197789917762031617', '1214835832967581698', '2020-08-27 20:05:38', NULL, '20200827002', '简体字转换为繁体字', 2, '{\"url\": null, \"param\": null, \"header\": null, \"httpMethod\": null}', '{\"soap\": \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\"?>\\n<soap:Envelope xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\" xmlns:xsd=\\\"http://www.w3.org/2001/XMLSchema\\\" xmlns:soap=\\\"http://schemas.xmlsoap.org/soap/envelope/\\\">\\n  <soap:Body>\\n    <toTraditionalChinese xmlns=\\\"http://webxml.com.cn/\\\">\\n      <sText>?</sText>\\n    </toTraditionalChinese>\\n  </soap:Body>\\n</soap:Envelope>\", \"wsdl\": \"http://ws.webxml.com.cn/WebServices/TraditionalSimplifiedWebService.asmx?wsdl\", \"method\": \"toTraditionalChinese\", \"targetNamespace\": \"http://webxml.com.cn/\"}');

-- ----------------------------
-- Table structure for market_service_log
-- ----------------------------
DROP TABLE IF EXISTS `market_service_log`;
CREATE TABLE `market_service_log`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `service_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '服务id',
  `caller_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调用者id',
  `caller_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调用者ip',
  `caller_date` datetime(0) NULL DEFAULT NULL COMMENT '调用时间',
  `caller_header` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调用请求头',
  `caller_param` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调用请求参数',
  `caller_soap` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调用报文',
  `time` int(11) NULL DEFAULT NULL COMMENT '调用耗时',
  `msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '信息记录',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0失败，1成功）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '服务集成调用日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of market_service_log
-- ----------------------------
INSERT INTO `market_service_log` VALUES ('1296737806386778114', '1275774099624386562', '1214835832967581698', '0:0:0:0:0:0:0:1', '2020-08-21 17:16:00', NULL, '{\"wordKey\": \"我\"}', NULL, NULL, '找不到服务：202008210022', 0);
INSERT INTO `market_service_log` VALUES ('1296738063749271553', '1275774099624386562', '1214835832967581698', '0:0:0:0:0:0:0:1', '2020-08-21 17:17:01', NULL, '{\"wordKey\": \"我\"}', NULL, 146, NULL, 1);
INSERT INTO `market_service_log` VALUES ('1336507495606222850', '1298954518389604354', '1214835832967581698', '61.164.216.254', '2020-12-09 11:06:33', NULL, '{\"wordKey\": \"我\"}', NULL, NULL, '找不到服务：20200821002', 0);
INSERT INTO `market_service_log` VALUES ('1336507537746395138', '1298954518389604354', '1214835832967581698', '61.164.216.254', '2020-12-09 11:06:43', NULL, '{\"wordKey\": \"我\"}', NULL, 438, NULL, 1);

-- ----------------------------
-- Table structure for masterdata_model
-- ----------------------------
DROP TABLE IF EXISTS `masterdata_model`;
CREATE TABLE `masterdata_model`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `model_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模型名称',
  `model_logic_table` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '逻辑表',
  `model_physical_table` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '物理表',
  `is_sync` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否建模（0否，1是）',
  `flow_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工作流状态（1待提交，2已退回，3审核中，4通过，5不通过，6已撤销）',
  `process_instance_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '流程实例ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '主数据模型表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of masterdata_model
-- ----------------------------
INSERT INTO `masterdata_model` VALUES ('1336484666361831426', 1, '1214835832967581698', '2020-12-09 22:35:50', '1197789917762031617', '1214835832967581698', '2020-12-09 22:55:03', NULL, '测试1102', 'test_yw', 'dynamic_20201208203549', '1', '4', '2501');
INSERT INTO `masterdata_model` VALUES ('1520715703576014850', 1, '1214835832967581698', '2022-05-01 18:44:12', '1197789917762031617', '1214835832967581698', '2022-05-01 20:51:14', NULL, '測試模型1', 'test_table', 'dynamic_20220501184411', '1', '4', '5001');

-- ----------------------------
-- Table structure for masterdata_model_column
-- ----------------------------
DROP TABLE IF EXISTS `masterdata_model_column`;
CREATE TABLE `masterdata_model_column`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `model_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模型表主键',
  `column_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列类型',
  `column_length` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列长度',
  `column_scale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列小数位数',
  `default_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列默认值',
  `is_system` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否系统默认（0否，1是）',
  `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否主键（0否，1是）',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否必填（0否，1是）',
  `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否为插入字段（0否，1是）',
  `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否编辑字段（0否，1是）',
  `is_detail` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否详情字段（0否，1是）',
  `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否列表字段（0否，1是）',
  `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否查询字段（0否，1是）',
  `query_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '查询方式（EQ等于、NE不等于、GT大于、GE大于等于、LT小于、LE小于等于、LIKE模糊、BETWEEN范围）',
  `is_bind_dict` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否绑定数据标准（0否，1是）',
  `bind_dict_type_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '绑定数据标准类别',
  `bind_dict_column` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '绑定数据标准字典字段（GB_CODE，GB_NAME）',
  `html_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '显示类型（input文本框、textarea文本域、select下拉框、checkbox复选框、radio单选框、datetime日期控件）',
  `sort` int(11) NULL DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '主数据模型列信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of masterdata_model_column
-- ----------------------------
INSERT INTO `masterdata_model_column` VALUES ('1336484666936451073', 1, '1214835832967581698', '2020-12-09 09:35:50', '1214835832967581698', '2020-12-09 09:35:50', NULL, '1336484666361831426', 'id', '主键ID', 'varchar', '20', '0', NULL, '1', '1', '1', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'input', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1336484667393630210', 1, '1214835832967581698', '2020-12-09 09:35:50', '1214835832967581698', '2020-12-09 09:35:50', NULL, '1336484666361831426', 'status', '状态（0禁用，1启用）', 'tinyint', '0', '0', '1', '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'number', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1336484667741757442', 1, '1214835832967581698', '2020-12-09 09:35:50', '1214835832967581698', '2020-12-09 09:35:50', NULL, '1336484666361831426', 'create_by', '创建人', 'varchar', '20', '0', NULL, '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'input', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1336484668085690370', 1, '1214835832967581698', '2020-12-09 09:35:50', '1214835832967581698', '2020-12-09 09:35:50', NULL, '1336484666361831426', 'create_time', '创建日期', 'datetime', '0', '0', NULL, '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'datetime', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1336484668438011905', 1, '1214835832967581698', '2020-12-09 09:35:50', '1214835832967581698', '2020-12-09 09:35:50', NULL, '1336484666361831426', 'create_dept', '创建人所属部门', 'varchar', '20', '0', NULL, '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'input', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1336484668786139137', 1, '1214835832967581698', '2020-12-09 09:35:50', '1214835832967581698', '2020-12-09 09:35:50', NULL, '1336484666361831426', 'update_by', '更新人', 'varchar', '20', '0', NULL, '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'input', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1336484669130072065', 1, '1214835832967581698', '2020-12-09 09:35:50', '1214835832967581698', '2020-12-09 09:35:50', NULL, '1336484666361831426', 'update_time', '更新日期', 'datetime', '0', '0', NULL, '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'datetime', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1336484669478199297', 1, '1214835832967581698', '2020-12-09 09:35:51', '1214835832967581698', '2020-12-09 09:35:51', NULL, '1336484666361831426', 'name', '名称', 'varchar', '255', '0', NULL, '0', '0', '1', '1', '1', '1', '1', '0', NULL, '0', NULL, NULL, 'input', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1520715703802507265', 1, '1214835832967581698', '2022-05-01 18:44:12', '1214835832967581698', '2022-05-01 18:44:12', NULL, '1520715703576014850', 'id', '主键ID', 'varchar', '20', '0', NULL, '1', '1', '1', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'input', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1520715703865421825', 1, '1214835832967581698', '2022-05-01 18:44:12', '1214835832967581698', '2022-05-01 18:44:12', NULL, '1520715703576014850', 'status', '状态（0禁用，1启用）', 'tinyint', '0', '0', '1', '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'number', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1520715703928336386', 1, '1214835832967581698', '2022-05-01 18:44:12', '1214835832967581698', '2022-05-01 18:44:12', NULL, '1520715703576014850', 'create_by', '创建人', 'varchar', '20', '0', NULL, '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'input', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1520715704028999681', 1, '1214835832967581698', '2022-05-01 18:44:12', '1214835832967581698', '2022-05-01 18:44:12', NULL, '1520715703576014850', 'create_time', '创建日期', 'datetime', '0', '0', NULL, '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'datetime', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1520715704079331330', 1, '1214835832967581698', '2022-05-01 18:44:12', '1214835832967581698', '2022-05-01 18:44:12', NULL, '1520715703576014850', 'create_dept', '创建人所属部门', 'varchar', '20', '0', NULL, '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'input', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1520715704142245889', 1, '1214835832967581698', '2022-05-01 18:44:12', '1214835832967581698', '2022-05-01 18:44:12', NULL, '1520715703576014850', 'update_by', '更新人', 'varchar', '20', '0', NULL, '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'input', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1520715704192577537', 1, '1214835832967581698', '2022-05-01 18:44:12', '1214835832967581698', '2022-05-01 18:44:12', NULL, '1520715703576014850', 'update_time', '更新日期', 'datetime', '0', '0', NULL, '1', '0', '0', '0', '0', '0', '0', '0', NULL, '0', NULL, NULL, 'datetime', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1520715704280657921', 1, '1214835832967581698', '2022-05-01 18:44:12', '1214835832967581698', '2022-05-01 18:44:12', NULL, '1520715703576014850', 'name', '姓名', 'varchar', '255', '0', NULL, '0', '0', '1', '1', '1', '1', '1', '0', NULL, '0', NULL, NULL, 'input', NULL);
INSERT INTO `masterdata_model_column` VALUES ('1520715704343572481', 1, '1214835832967581698', '2022-05-01 18:44:12', '1214835832967581698', '2022-05-01 18:44:12', NULL, '1520715703576014850', 'age', '年龄', 'int', '255', '0', NULL, '0', '0', '1', '1', '1', '1', '1', '0', NULL, '0', NULL, NULL, 'input', NULL);

-- ----------------------------
-- Table structure for metadata_authorize
-- ----------------------------
DROP TABLE IF EXISTS `metadata_authorize`;
CREATE TABLE `metadata_authorize`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `object_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标表主键ID',
  `role_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色ID',
  `object_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '目标表类型',
  PRIMARY KEY (`id`, `object_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '元数据授权信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of metadata_authorize
-- ----------------------------
INSERT INTO `metadata_authorize` VALUES ('1339728732931301378', '1336474987430793217', '1319084037507244034', 'database');
INSERT INTO `metadata_authorize` VALUES ('1339728733359120386', '1336479261791473665', '1319084037507244034', 'table');
INSERT INTO `metadata_authorize` VALUES ('1339728733749190658', '1336479262852632577', '1319084037507244034', 'column');
INSERT INTO `metadata_authorize` VALUES ('1339728734139260930', '1336479263477583874', '1319084037507244034', 'column');
INSERT INTO `metadata_authorize` VALUES ('1339728734529331201', '1336479264106729474', '1319084037507244034', 'column');
INSERT INTO `metadata_authorize` VALUES ('1339728734923595777', '1336479264639406082', '1319084037507244034', 'table');
INSERT INTO `metadata_authorize` VALUES ('1339728735317860354', '1336479265583124482', '1319084037507244034', 'column');
INSERT INTO `metadata_authorize` VALUES ('1339728735707930626', '1336479266149355521', '1319084037507244034', 'column');
INSERT INTO `metadata_authorize` VALUES ('1339728736093806593', '1336479266728169473', '1319084037507244034', 'table');
INSERT INTO `metadata_authorize` VALUES ('1339728736483876865', '1336479267583807489', '1319084037507244034', 'column');
INSERT INTO `metadata_authorize` VALUES ('1339728736878141442', '1336479268242313218', '1319084037507244034', 'column');
INSERT INTO `metadata_authorize` VALUES ('1339728737268211713', '1336479268821127170', '1319084037507244034', 'column');

-- ----------------------------
-- Table structure for metadata_change_record
-- ----------------------------
DROP TABLE IF EXISTS `metadata_change_record`;
CREATE TABLE `metadata_change_record`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `version` tinyint(4) NULL DEFAULT NULL COMMENT '版本号',
  `object_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更改类型',
  `object_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '源数据表主键',
  `field_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改的源数据表的字段名',
  `field_old_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '该字段原来的值',
  `field_new_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '该字段最新的值',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '元数据变更记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of metadata_change_record
-- ----------------------------
INSERT INTO `metadata_change_record` VALUES ('1521161495918977026', 1, '1214835832967581698', '2022-05-03 00:15:37', '1197789917762031617', '1214835832967581698', '2022-05-03 00:15:37', NULL, 1, 'columnComment', '1520313001268334593', 'ACCOUNT_ID', NULL, '主键id');

-- ----------------------------
-- Table structure for metadata_column
-- ----------------------------
DROP TABLE IF EXISTS `metadata_column`;
CREATE TABLE `metadata_column`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `source_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属数据源',
  `table_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属数据表',
  `column_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段名称',
  `column_comment` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段注释',
  `column_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段是否主键(1是0否)',
  `column_nullable` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段是否允许为空(1是0否)',
  `column_position` int(11) NULL DEFAULT NULL COMMENT '字段序号',
  `data_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据类型',
  `data_length` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据长度',
  `data_precision` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据精度',
  `data_scale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据小数位',
  `data_default` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据默认值',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '元数据信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of metadata_column
-- ----------------------------
INSERT INTO `metadata_column` VALUES ('1336479262852632577', '1336474987430793217', '1336479261791473665', 'id', '主键', '1', '0', 1, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1336479263477583874', '1336474987430793217', '1336479261791473665', 'patient_name', '患者姓名', '0', '1', 2, 'varchar', '255', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1336479264106729474', '1336474987430793217', '1336479261791473665', 'patient_sex', '患者性别（1男2女）', '0', '1', 3, 'varchar', '255', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1336479265583124482', '1336474987430793217', '1336479264639406082', 'id', '主键', '1', '0', 1, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1336479266149355521', '1336474987430793217', '1336479264639406082', 'part_name', '部位名称', '0', '1', 2, 'varchar', '255', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1336479267583807489', '1336474987430793217', '1336479266728169473', 'id', '主键', '1', '0', 1, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1336479268242313218', '1336474987430793217', '1336479266728169473', 'part_id', '所属部位', '0', '1', 2, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1336479268821127170', '1336474987430793217', '1336479266728169473', 'type_name', '症状名称', '0', '1', 3, 'varchar', '255', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313001268334593', '1240185865539600385', '1520313001104756738', 'ACCOUNT_ID', '', '0', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313001389969410', '1240185865539600385', '1520313001104756738', 'ACCOUNT_PARENT', NULL, '0', '1', 2, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313001473855489', '1240185865539600385', '1520313001104756738', 'ACCOUNT_DESCRIPTION', NULL, '0', '1', 3, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313001549352961', '1240185865539600385', '1520313001104756738', 'ACCOUNT_TYPE', NULL, '0', '0', 4, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313001691959297', '1240185865539600385', '1520313001104756738', 'ACCOUNT_ROLLUP', NULL, '0', '0', 5, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313001792622594', '1240185865539600385', '1520313001104756738', 'CUSTOM_MEMBERS', NULL, '0', '1', 6, 'varchar', '255', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313001989754881', '1240185865539600385', '1520313001889091586', 'CATEGORY_ID', NULL, '0', '0', 1, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313002069446657', '1240185865539600385', '1520313001889091586', 'CATEGORY_PARENT', NULL, '0', '1', 2, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313002153332738', '1240185865539600385', '1520313001889091586', 'CATEGORY_DESCRIPTION', NULL, '0', '0', 3, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313002241413121', '1240185865539600385', '1520313001889091586', 'CATEGORY_ROLLUP', NULL, '0', '1', 4, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313002413379585', '1240185865539600385', '1520313002321104898', 'provn', NULL, '0', '1', 1, 'tinytext', '255', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313002488877057', '1240185865539600385', '1520313002321104898', 'city', NULL, '0', '1', 2, 'tinytext', '255', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313002555985921', '1240185865539600385', '1520313002321104898', 'gender', NULL, '0', '1', 3, 'tinytext', '255', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313002623094786', '1240185865539600385', '1520313002321104898', 'cnt', NULL, '0', '1', 4, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313002761506818', '1240185865539600385', '1520313002681815041', 'CUSTOMER_ID', NULL, '1', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313002803449857', '1240185865539600385', '1520313002681815041', 'ACCOUNT_NUM', NULL, '0', '0', 2, 'bigint', NULL, '19', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313002862170114', '1240185865539600385', '1520313002681815041', 'LNAME', NULL, '0', '0', 3, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313002920890369', '1240185865539600385', '1520313002681815041', 'FNAME', NULL, '0', '0', 4, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313002967027714', '1240185865539600385', '1520313002681815041', 'MI', NULL, '0', '1', 5, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003017359362', '1240185865539600385', '1520313002681815041', 'ADDRESS1', NULL, '0', '1', 6, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003059302401', '1240185865539600385', '1520313002681815041', 'ADDRESS2', NULL, '0', '1', 7, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003101245441', '1240185865539600385', '1520313002681815041', 'ADDRESS3', NULL, '0', '1', 8, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003138994177', '1240185865539600385', '1520313002681815041', 'ADDRESS4', NULL, '0', '1', 9, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003180937217', '1240185865539600385', '1520313002681815041', 'CITY', NULL, '0', '1', 10, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003235463169', '1240185865539600385', '1520313002681815041', 'STATE_PROVINCE', NULL, '0', '1', 11, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003285794818', '1240185865539600385', '1520313002681815041', 'POSTAL_CODE', NULL, '0', '0', 12, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003336126465', '1240185865539600385', '1520313002681815041', 'COUNTRY', NULL, '0', '0', 13, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003382263809', '1240185865539600385', '1520313002681815041', 'CUSTOMER_REGION_ID', NULL, '0', '0', 14, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313003432595457', '1240185865539600385', '1520313002681815041', 'PHONE1', NULL, '0', '0', 15, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003474538497', '1240185865539600385', '1520313002681815041', 'PHONE2', NULL, '0', '0', 16, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003524870145', '1240185865539600385', '1520313002681815041', 'BIRTHDATE', NULL, '0', '0', 17, 'date', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003566813186', '1240185865539600385', '1520313002681815041', 'MARITAL_STATUS', NULL, '0', '0', 18, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003608756226', '1240185865539600385', '1520313002681815041', 'YEARLY_INCOME', NULL, '0', '0', 19, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003659087874', '1240185865539600385', '1520313002681815041', 'GENDER', NULL, '0', '0', 20, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003709419522', '1240185865539600385', '1520313002681815041', 'TOTAL_CHILDREN', NULL, '0', '0', 21, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313003759751169', '1240185865539600385', '1520313002681815041', 'NUM_CHILDREN_AT_HOME', NULL, '0', '0', 22, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313003826860033', '1240185865539600385', '1520313002681815041', 'EDUCATION', NULL, '0', '0', 23, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003902357505', '1240185865539600385', '1520313002681815041', 'DATE_ACCNT_OPENED', NULL, '0', '0', 24, 'date', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313003969466370', '1240185865539600385', '1520313002681815041', 'MEMBER_CARD', NULL, '0', '1', 25, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313004040769538', '1240185865539600385', '1520313002681815041', 'OCCUPATION', NULL, '0', '1', 26, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313004107878402', '1240185865539600385', '1520313002681815041', 'HOUSEOWNER', NULL, '0', '1', 27, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313004166598658', '1240185865539600385', '1520313002681815041', 'NUM_CARS_OWNED', NULL, '0', '1', 28, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313004313399297', '1240185865539600385', '1520313004233707521', 'DEPARTMENT_ID', NULL, '0', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313004380508161', '1240185865539600385', '1520313004233707521', 'DEPARTMENT_DESCRIPTION', NULL, '0', '0', 2, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313004493754369', '1240185865539600385', '1520313004447617026', 'EMPLOYEE_ID', NULL, '0', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313004535697410', '1240185865539600385', '1520313004447617026', 'FULL_NAME', NULL, '0', '0', 2, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313004577640449', '1240185865539600385', '1520313004447617026', 'FIRST_NAME', NULL, '0', '0', 3, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313004619583490', '1240185865539600385', '1520313004447617026', 'LAST_NAME', NULL, '0', '0', 4, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313004653137922', '1240185865539600385', '1520313004447617026', 'POSITION_ID', NULL, '0', '1', 5, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313004695080961', '1240185865539600385', '1520313004447617026', 'POSITION_TITLE', NULL, '0', '1', 6, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313004741218306', '1240185865539600385', '1520313004447617026', 'STORE_ID', NULL, '0', '0', 7, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313004787355649', '1240185865539600385', '1520313004447617026', 'DEPARTMENT_ID', NULL, '0', '0', 8, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313004829298689', '1240185865539600385', '1520313004447617026', 'BIRTH_DATE', NULL, '0', '0', 9, 'date', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313004875436033', '1240185865539600385', '1520313004447617026', 'HIRE_DATE', NULL, '0', '1', 10, 'timestamp', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313004917379073', '1240185865539600385', '1520313004447617026', 'END_DATE', NULL, '0', '1', 11, 'timestamp', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313004967710721', '1240185865539600385', '1520313004447617026', 'SALARY', NULL, '0', '0', 12, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313005018042370', '1240185865539600385', '1520313004447617026', 'SUPERVISOR_ID', NULL, '0', '1', 13, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313005068374018', '1240185865539600385', '1520313004447617026', 'EDUCATION_LEVEL', NULL, '0', '0', 14, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313005131288578', '1240185865539600385', '1520313004447617026', 'MARITAL_STATUS', NULL, '0', '0', 15, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313005198397442', '1240185865539600385', '1520313004447617026', 'GENDER', NULL, '0', '0', 16, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313005240340482', '1240185865539600385', '1520313004447617026', 'MANAGEMENT_ROLE', NULL, '0', '1', 17, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313005328420865', '1240185865539600385', '1520313005286477826', 'EMPLOYEE_ID', NULL, '0', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313005374558210', '1240185865539600385', '1520313005286477826', 'SUPERVISOR_ID', NULL, '0', '0', 2, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313005412306946', '1240185865539600385', '1520313005286477826', 'DISTANCE', NULL, '0', '1', 3, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313005521358849', '1240185865539600385', '1520313005454249985', 'STORE_ID', NULL, '0', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313005575884802', '1240185865539600385', '1520313005454249985', 'ACCOUNT_ID', NULL, '0', '0', 2, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313005617827842', '1240185865539600385', '1520313005454249985', 'EXP_DATE', NULL, '0', '0', 3, 'timestamp', NULL, NULL, NULL, 'CURRENT_TIMESTAMP');
INSERT INTO `metadata_column` VALUES ('1520313005659770882', '1240185865539600385', '1520313005454249985', 'TIME_ID', NULL, '0', '0', 4, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313005701713921', '1240185865539600385', '1520313005454249985', 'CATEGORY_ID', NULL, '0', '0', 5, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313005743656962', '1240185865539600385', '1520313005454249985', 'CURRENCY_ID', NULL, '0', '0', 6, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313005781405697', '1240185865539600385', '1520313005454249985', 'AMOUNT', NULL, '0', '0', 7, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313005882068994', '1240185865539600385', '1520313005831737346', 'pg_in', NULL, '0', '1', 1, 'varchar', '100', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313005944983553', '1240185865539600385', '1520313005831737346', 'pg_out', NULL, '0', '1', 2, 'varchar', '100', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313006003703809', '1240185865539600385', '1520313005831737346', 'qq_out', NULL, '0', '1', 3, 'double', NULL, '22', NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313006108561410', '1240185865539600385', '1520313006045646849', 'PRODUCT_ID', NULL, '1', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313006154698754', '1240185865539600385', '1520313006045646849', 'TIME_ID', NULL, '1', '0', 2, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313006221807617', '1240185865539600385', '1520313006045646849', 'WAREHOUSE_ID', NULL, '1', '0', 3, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313006272139265', '1240185865539600385', '1520313006045646849', 'STORE_ID', NULL, '1', '0', 4, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313006339248130', '1240185865539600385', '1520313006045646849', 'UNITS_ORDERED', NULL, '0', '1', 5, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313006397968386', '1240185865539600385', '1520313006045646849', 'UNITS_SHIPPED', NULL, '0', '1', 6, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313006456688641', '1240185865539600385', '1520313006045646849', 'WAREHOUSE_SALES', NULL, '0', '1', 7, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313006515408898', '1240185865539600385', '1520313006045646849', 'WAREHOUSE_COST', NULL, '0', '1', 8, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313006565740545', '1240185865539600385', '1520313006045646849', 'SUPPLY_TIME', NULL, '0', '1', 9, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313006616072193', '1240185865539600385', '1520313006045646849', 'STORE_INVOICE', NULL, '0', '1', 10, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313006733512706', '1240185865539600385', '1520313006674792449', 'province', NULL, '0', '1', 1, 'varchar', '500', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313006775455745', '1240185865539600385', '1520313006674792449', 'city', NULL, '0', '1', 2, 'varchar', '500', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313006825787393', '1240185865539600385', '1520313006674792449', 'district', NULL, '0', '1', 3, 'varchar', '500', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313006867730433', '1240185865539600385', '1520313006674792449', 'wind_level', NULL, '0', '1', 4, 'varchar', '1', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313006909673473', '1240185865539600385', '1520313006674792449', 'wind_direct', NULL, '0', '1', 5, 'varchar', '2', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313006960005122', '1240185865539600385', '1520313006674792449', 'cnt', NULL, '0', '0', 6, 'double', NULL, '17', '0', '0');
INSERT INTO `metadata_column` VALUES ('1520313006993559553', '1240185865539600385', '1520313006674792449', 'max_temp', NULL, '0', '0', 7, 'double', NULL, '17', '0', '0');
INSERT INTO `metadata_column` VALUES ('1520313007085834241', '1240185865539600385', '1520313007035502593', 'PRODUCT_CLASS_ID', NULL, '0', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313007123582977', '1240185865539600385', '1520313007035502593', 'PRODUCT_ID', NULL, '1', '0', 2, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313007169720322', '1240185865539600385', '1520313007035502593', 'BRAND_NAME', NULL, '0', '1', 3, 'varchar', '60', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313007270383617', '1240185865539600385', '1520313007035502593', 'PRODUCT_NAME', NULL, '0', '0', 4, 'varchar', '60', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313007312326657', '1240185865539600385', '1520313007035502593', 'SKU', NULL, '0', '0', 5, 'bigint', NULL, '19', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313007345881090', '1240185865539600385', '1520313007035502593', 'SRP', NULL, '0', '1', 6, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313007387824130', '1240185865539600385', '1520313007035502593', 'GROSS_WEIGHT', NULL, '0', '1', 7, 'double', NULL, '22', NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313007438155777', '1240185865539600385', '1520313007035502593', 'NET_WEIGHT', NULL, '0', '1', 8, 'double', NULL, '22', NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313007475904513', '1240185865539600385', '1520313007035502593', 'RECYCLABLE_PACKAGE', NULL, '0', '1', 9, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313007526236161', '1240185865539600385', '1520313007035502593', 'LOW_FAT', NULL, '0', '1', 10, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313007568179201', '1240185865539600385', '1520313007035502593', 'UNITS_PER_CASE', NULL, '0', '1', 11, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313007610122241', '1240185865539600385', '1520313007035502593', 'CASES_PER_PALLET', NULL, '0', '1', 12, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313007647870977', '1240185865539600385', '1520313007035502593', 'SHELF_WIDTH', NULL, '0', '1', 13, 'double', NULL, '22', NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313007689814018', '1240185865539600385', '1520313007035502593', 'SHELF_HEIGHT', NULL, '0', '1', 14, 'double', NULL, '22', NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313007731757058', '1240185865539600385', '1520313007035502593', 'SHELF_DEPTH', NULL, '0', '1', 15, 'double', NULL, '22', NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313007769505794', '1240185865539600385', '1520313007035502593', 'PRODUCT_SUBCATEGORY', NULL, '0', '1', 16, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313007811448834', '1240185865539600385', '1520313007035502593', 'PRODUCT_CATEGORY', NULL, '0', '1', 17, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313007861780482', '1240185865539600385', '1520313007035502593', 'PRODUCT_DEPARTMENT', NULL, '0', '1', 18, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313007903723522', '1240185865539600385', '1520313007035502593', 'PRODUCT_FAMILY', NULL, '0', '1', 19, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313008029552642', '1240185865539600385', '1520313007958249474', 'PROMOTION_ID', NULL, '1', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313008079884289', '1240185865539600385', '1520313007958249474', 'PROMOTION_DISTRICT_ID', NULL, '0', '1', 2, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313008146993154', '1240185865539600385', '1520313007958249474', 'PROMOTION_NAME', NULL, '0', '1', 3, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313008201519105', '1240185865539600385', '1520313007958249474', 'MEDIA_TYPE', NULL, '0', '1', 4, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313008260239361', '1240185865539600385', '1520313007958249474', 'COST', NULL, '0', '1', 5, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313008327348225', '1240185865539600385', '1520313007958249474', 'START_DATE', NULL, '0', '1', 6, 'timestamp', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313008390262786', '1240185865539600385', '1520313007958249474', 'END_DATE', NULL, '0', '1', 7, 'timestamp', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313008516091906', '1240185865539600385', '1520313008448983041', 'REGION_ID', NULL, '1', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313008872607745', '1240185865539600385', '1520313008448983041', 'SALES_CITY', NULL, '0', '1', 2, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313008973271041', '1240185865539600385', '1520313008448983041', 'SALES_STATE_PROVINCE', NULL, '0', '1', 3, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313009052962818', '1240185865539600385', '1520313008448983041', 'SALES_DISTRICT', NULL, '0', '1', 4, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313009128460289', '1240185865539600385', '1520313008448983041', 'SALES_REGION', NULL, '0', '1', 5, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313009187180545', '1240185865539600385', '1520313008448983041', 'SALES_COUNTRY', NULL, '0', '1', 6, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313009258483713', '1240185865539600385', '1520313008448983041', 'SALES_DISTRICT_ID', NULL, '0', '1', 7, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313009354952706', '1240185865539600385', '1520313009300426753', 'EMPLOYEE_ID', NULL, '0', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313009388507138', '1240185865539600385', '1520313009300426753', 'FULL_NAME', NULL, '0', '0', 2, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313009438838785', '1240185865539600385', '1520313009300426753', 'FIRST_NAME', NULL, '0', '0', 3, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313009484976130', '1240185865539600385', '1520313009300426753', 'LAST_NAME', NULL, '0', '0', 4, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313009526919170', '1240185865539600385', '1520313009300426753', 'POSITION_ID', NULL, '0', '1', 5, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313009581445121', '1240185865539600385', '1520313009300426753', 'POSITION_TITLE', NULL, '0', '1', 6, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313009623388162', '1240185865539600385', '1520313009300426753', 'STORE_ID', NULL, '0', '0', 7, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313009665331202', '1240185865539600385', '1520313009300426753', 'DEPARTMENT_ID', NULL, '0', '0', 8, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313009724051457', '1240185865539600385', '1520313009300426753', 'BIRTH_DATE', NULL, '0', '0', 9, 'timestamp', NULL, NULL, NULL, 'CURRENT_TIMESTAMP');
INSERT INTO `metadata_column` VALUES ('1520313009786966018', '1240185865539600385', '1520313009300426753', 'HIRE_DATE', NULL, '0', '1', 10, 'timestamp', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313009849880578', '1240185865539600385', '1520313009300426753', 'END_DATE', NULL, '0', '1', 11, 'timestamp', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313009896017922', '1240185865539600385', '1520313009300426753', 'SALARY', NULL, '0', '0', 12, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313009942155266', '1240185865539600385', '1520313009300426753', 'SUPERVISOR_ID', NULL, '0', '1', 13, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313010000875521', '1240185865539600385', '1520313009300426753', 'EDUCATION_LEVEL', NULL, '0', '0', 14, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313010059595778', '1240185865539600385', '1520313009300426753', 'MARITAL_STATUS', NULL, '0', '0', 15, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313010122510338', '1240185865539600385', '1520313009300426753', 'GENDER', NULL, '0', '0', 16, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313010260922370', '1240185865539600385', '1520313010193813506', 'PAY_DATE', NULL, '0', '0', 1, 'timestamp', NULL, NULL, NULL, 'CURRENT_TIMESTAMP');
INSERT INTO `metadata_column` VALUES ('1520313010323836930', '1240185865539600385', '1520313010193813506', 'EMPLOYEE_ID', NULL, '0', '0', 2, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313010382557186', '1240185865539600385', '1520313010193813506', 'DEPARTMENT_ID', NULL, '0', '0', 3, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313010437083138', '1240185865539600385', '1520313010193813506', 'CURRENCY_ID', NULL, '0', '0', 4, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313010491609090', '1240185865539600385', '1520313010193813506', 'SALARY_PAID', NULL, '0', '0', 5, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313010546135042', '1240185865539600385', '1520313010193813506', 'OVERTIME_PAID', NULL, '0', '0', 6, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313010596466690', '1240185865539600385', '1520313010193813506', 'VACATION_ACCRUED', NULL, '0', '0', 7, 'double', NULL, '22', NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313010646798337', '1240185865539600385', '1520313010193813506', 'VACATION_USED', NULL, '0', '0', 8, 'double', NULL, '22', NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313010781016066', '1240185865539600385', '1520313010709712897', 'PRODUCT_ID', NULL, '0', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313010843930625', '1240185865539600385', '1520313010709712897', 'TIME_ID', NULL, '0', '0', 2, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313010911039489', '1240185865539600385', '1520313010709712897', 'CUSTOMER_ID', NULL, '0', '0', 3, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313010973954049', '1240185865539600385', '1520313010709712897', 'PROMOTION_ID', NULL, '0', '0', 4, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011032674305', '1240185865539600385', '1520313010709712897', 'STORE_ID', NULL, '0', '0', 5, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011091394562', '1240185865539600385', '1520313010709712897', 'STORE_SALES', NULL, '0', '0', 6, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011133337602', '1240185865539600385', '1520313010709712897', 'STORE_COST', NULL, '0', '0', 7, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011183669250', '1240185865539600385', '1520313010709712897', 'UNIT_SALES', NULL, '0', '0', 8, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011242389506', '1240185865539600385', '1520313010709712897', 'rand', NULL, '0', '0', 9, 'double', NULL, '22', NULL, '0');
INSERT INTO `metadata_column` VALUES ('1520313011343052802', '1240185865539600385', '1520313011284332545', 'PRODUCT_ID', NULL, '1', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011405967361', '1240185865539600385', '1520313011284332545', 'TIME_ID', NULL, '1', '0', 2, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011456299010', '1240185865539600385', '1520313011284332545', 'CUSTOMER_ID', NULL, '1', '0', 3, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011531796481', '1240185865539600385', '1520313011284332545', 'PROMOTION_ID', NULL, '1', '0', 4, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011590516737', '1240185865539600385', '1520313011284332545', 'STORE_ID', NULL, '1', '0', 5, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011653431298', '1240185865539600385', '1520313011284332545', 'STORE_SALES', NULL, '0', '0', 6, 'decimal', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011716345858', '1240185865539600385', '1520313011284332545', 'STORE_COST', NULL, '0', '0', 7, 'decimal', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011762483202', '1240185865539600385', '1520313011284332545', 'UNIT_SALES', NULL, '0', '0', 8, 'decimal', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011833786369', '1240185865539600385', '1520313011284332545', 'VERSION', NULL, '1', '0', 9, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313011947032578', '1240185865539600385', '1520313011879923714', 'source', NULL, '0', '1', 1, 'tinytext', '255', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313011997364225', '1240185865539600385', '1520313011879923714', 'target', NULL, '0', '1', 2, 'tinytext', '255', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012039307265', '1240185865539600385', '1520313011879923714', 'value', NULL, '0', '1', 3, 'double', NULL, '22', NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012127387649', '1240185865539600385', '1520313012081250306', 'SUBJECT_NAME', NULL, '0', '1', 1, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012173524993', '1240185865539600385', '1520313012081250306', 'SOURCELABEL', NULL, '0', '1', 2, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012215468033', '1240185865539600385', '1520313012081250306', 'SOURCEWIDTH', NULL, '0', '1', 3, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012249022466', '1240185865539600385', '1520313012081250306', 'OTHER_PARTY', NULL, '0', '1', 4, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012299354114', '1240185865539600385', '1520313012081250306', 'TARGETLABEL', NULL, '0', '1', 5, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012337102850', '1240185865539600385', '1520313012081250306', 'TARGETWIDTH', NULL, '0', '1', 6, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012379045889', '1240185865539600385', '1520313012081250306', 'RELATION', NULL, '0', '1', 7, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012458737666', '1240185865539600385', '1520313012420988930', 'SUBJECT_NAME', NULL, '0', '1', 1, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012500680705', '1240185865539600385', '1520313012420988930', 'SOURCELABEL', NULL, '0', '1', 2, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012538429442', '1240185865539600385', '1520313012420988930', 'SOURCEWIDTH', NULL, '0', '1', 3, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012580372481', '1240185865539600385', '1520313012420988930', 'OTHER_PARTY', NULL, '0', '1', 4, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012626509825', '1240185865539600385', '1520313012420988930', 'TARGETLABEL', NULL, '0', '1', 5, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012664258562', '1240185865539600385', '1520313012420988930', 'TARGETWIDTH', NULL, '0', '1', 6, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012706201601', '1240185865539600385', '1520313012420988930', 'RELATION', NULL, '0', '1', 7, 'varchar', '50', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012794281985', '1240185865539600385', '1520313012748144642', 'STORE_ID', NULL, '1', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313012853002242', '1240185865539600385', '1520313012748144642', 'STORE_TYPE', NULL, '0', '1', 2, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313012907528193', '1240185865539600385', '1520313012748144642', 'REGION_ID', NULL, '0', '1', 3, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313012953665538', '1240185865539600385', '1520313012748144642', 'STORE_NAME', NULL, '0', '1', 4, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313013008191490', '1240185865539600385', '1520313012748144642', 'STORE_NUMBER', NULL, '0', '1', 5, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313013066911745', '1240185865539600385', '1520313012748144642', 'STORE_STREET_ADDRESS', NULL, '0', '1', 6, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313013121437697', '1240185865539600385', '1520313012748144642', 'STORE_CITY', NULL, '0', '1', 7, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313013188546562', '1240185865539600385', '1520313012748144642', 'STORE_STATE', NULL, '0', '1', 8, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313013238878209', '1240185865539600385', '1520313012748144642', 'STORE_POSTAL_CODE', NULL, '0', '1', 9, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313013305987073', '1240185865539600385', '1520313012748144642', 'STORE_COUNTRY', NULL, '0', '1', 10, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313013356318721', '1240185865539600385', '1520313012748144642', 'STORE_MANAGER', NULL, '0', '1', 11, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313013423427586', '1240185865539600385', '1520313012748144642', 'STORE_PHONE', NULL, '0', '1', 12, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313013482147842', '1240185865539600385', '1520313012748144642', 'STORE_FAX', NULL, '0', '1', 13, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313013532479489', '1240185865539600385', '1520313012748144642', 'FIRST_OPENED_DATE', NULL, '0', '1', 14, 'timestamp', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313013582811138', '1240185865539600385', '1520313012748144642', 'LAST_REMODEL_DATE', NULL, '0', '1', 15, 'timestamp', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313013633142785', '1240185865539600385', '1520313012748144642', 'STORE_SQFT', NULL, '0', '1', 16, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313013675085826', '1240185865539600385', '1520313012748144642', 'GROCERY_SQFT', NULL, '0', '1', 17, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313013725417473', '1240185865539600385', '1520313012748144642', 'FROZEN_SQFT', NULL, '0', '1', 18, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313013767360513', '1240185865539600385', '1520313012748144642', 'MEAT_SQFT', NULL, '0', '1', 19, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313013817692161', '1240185865539600385', '1520313012748144642', 'COFFEE_BAR', NULL, '0', '1', 20, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313013859635202', '1240185865539600385', '1520313012748144642', 'VIDEO_STORE', NULL, '0', '1', 21, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313013914161153', '1240185865539600385', '1520313012748144642', 'SALAD_BAR', NULL, '0', '1', 22, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313013956104193', '1240185865539600385', '1520313012748144642', 'PREPARED_FOOD', NULL, '0', '1', 23, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313013998047234', '1240185865539600385', '1520313012748144642', 'FLORIST', NULL, '0', '1', 24, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313014048378882', '1240185865539600385', '1520313012748144642', 'SALES_REGION_ID', NULL, '0', '1', 25, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313014090321922', '1240185865539600385', '1520313012748144642', 'SALES_STATE', NULL, '0', '1', 26, 'longtext', '4294967295', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313014136459266', '1240185865539600385', '1520313012748144642', 'SALES_REGION', NULL, '0', '1', 27, 'longtext', '4294967295', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313014203568130', '1240185865539600385', '1520313012748144642', 'SALES_SUBREGION', NULL, '0', '1', 28, 'longtext', '4294967295', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313014262288385', '1240185865539600385', '1520313012748144642', 'SALES_AREA', NULL, '0', '1', 29, 'longtext', '4294967295', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313014396506114', '1240185865539600385', '1520313014316814337', 'ID', NULL, '1', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313014463614978', '1240185865539600385', '1520313014316814337', 'STORE_CITY', NULL, '0', '1', 2, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313014551695362', '1240185865539600385', '1520313014316814337', 'STORE_STATE', NULL, '0', '1', 3, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313014727856129', '1240185865539600385', '1520313014622998530', 'STORE_ID', NULL, '0', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313014815936514', '1240185865539600385', '1520313014622998530', 'STORE_TYPE', NULL, '0', '1', 2, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313014891433986', '1240185865539600385', '1520313014622998530', 'REGION_ID', NULL, '0', '1', 3, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313014954348545', '1240185865539600385', '1520313014622998530', 'STORE_NAME', NULL, '0', '1', 4, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313015042428930', '1240185865539600385', '1520313014622998530', 'STORE_NUMBER', NULL, '0', '1', 5, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313015117926402', '1240185865539600385', '1520313014622998530', 'STORE_STREET_ADDRESS', NULL, '0', '1', 6, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313015189229569', '1240185865539600385', '1520313014622998530', 'STORE_CITY', NULL, '0', '1', 7, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313015268921346', '1240185865539600385', '1520313014622998530', 'STORE_STATE', NULL, '0', '1', 8, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313015352807425', '1240185865539600385', '1520313014622998530', 'STORE_POSTAL_CODE', NULL, '0', '1', 9, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313015457665026', '1240185865539600385', '1520313014622998530', 'STORE_COUNTRY', NULL, '0', '1', 10, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313015537356801', '1240185865539600385', '1520313014622998530', 'STORE_MANAGER', NULL, '0', '1', 11, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313015625437186', '1240185865539600385', '1520313014622998530', 'STORE_PHONE', NULL, '0', '1', 12, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313015700934658', '1240185865539600385', '1520313014622998530', 'STORE_FAX', NULL, '0', '1', 13, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313015784820737', '1240185865539600385', '1520313014622998530', 'FIRST_OPENED_DATE', NULL, '0', '1', 14, 'timestamp', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313015868706818', '1240185865539600385', '1520313014622998530', 'LAST_REMODEL_DATE', NULL, '0', '1', 15, 'timestamp', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313015919038465', '1240185865539600385', '1520313014622998530', 'STORE_SQFT', NULL, '0', '1', 16, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313015969370114', '1240185865539600385', '1520313014622998530', 'GROCERY_SQFT', NULL, '0', '1', 17, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313016011313154', '1240185865539600385', '1520313014622998530', 'FROZEN_SQFT', NULL, '0', '1', 18, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313016082616321', '1240185865539600385', '1520313014622998530', 'MEAT_SQFT', NULL, '0', '1', 19, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313016149725186', '1240185865539600385', '1520313014622998530', 'COFFEE_BAR', NULL, '0', '1', 20, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313016208445442', '1240185865539600385', '1520313014622998530', 'VIDEO_STORE', NULL, '0', '1', 21, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313016254582786', '1240185865539600385', '1520313014622998530', 'SALAD_BAR', NULL, '0', '1', 22, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313016317497346', '1240185865539600385', '1520313014622998530', 'PREPARED_FOOD', NULL, '0', '1', 23, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313016376217601', '1240185865539600385', '1520313014622998530', 'FLORIST', NULL, '0', '1', 24, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313016535601153', '1240185865539600385', '1520313016451715074', 'ID', NULL, '1', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313016590127106', '1240185865539600385', '1520313016451715074', 'STORE_STATE', NULL, '0', '1', 2, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313016724344834', '1240185865539600385', '1520313016653041665', 'year', NULL, '0', '1', 1, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313016799842306', '1240185865539600385', '1520313016653041665', 'month', NULL, '0', '1', 2, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313016913088514', '1240185865539600385', '1520313016653041665', 'store_country', NULL, '0', '1', 3, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313017001168898', '1240185865539600385', '1520313016653041665', 'store_state', NULL, '0', '1', 4, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313017080860673', '1240185865539600385', '1520313016653041665', 'store_sales', NULL, '0', '0', 5, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313017248632833', '1240185865539600385', '1520313017147969537', 'TIME_ID', NULL, '1', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313017319936001', '1240185865539600385', '1520313017147969537', 'THE_DATE', NULL, '0', '1', 2, 'timestamp', NULL, NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313017403822081', '1240185865539600385', '1520313017147969537', 'THE_DAY', NULL, '0', '1', 3, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313017538039809', '1240185865539600385', '1520313017147969537', 'THE_MONTH', NULL, '0', '1', 4, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313017659674626', '1240185865539600385', '1520313017147969537', 'THE_YEAR', NULL, '0', '1', 5, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313017793892353', '1240185865539600385', '1520313017147969537', 'DAY_OF_MONTH', NULL, '0', '1', 6, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313017928110082', '1240185865539600385', '1520313017147969537', 'WEEK_OF_YEAR', NULL, '0', '1', 7, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313018045550594', '1240185865539600385', '1520313017147969537', 'MONTH_OF_YEAR', NULL, '0', '1', 8, 'smallint', NULL, '5', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313018129436674', '1240185865539600385', '1520313017147969537', 'QUARTER', NULL, '0', '1', 9, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313018183962625', '1240185865539600385', '1520313017147969537', 'FISCAL_PERIOD', NULL, '0', '1', 10, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313018297208833', '1240185865539600385', '1520313018242682882', 'IDTREE', NULL, '0', '1', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313018339151873', '1240185865539600385', '1520313018242682882', 'DTREE', NULL, '0', '1', 2, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313018381094913', '1240185865539600385', '1520313018242682882', 'IDTREERIC', NULL, '0', '1', 3, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313018431426561', '1240185865539600385', '1520313018242682882', 'SALES', NULL, '0', '1', 4, 'decimal', NULL, '10', '4', NULL);
INSERT INTO `metadata_column` VALUES ('1520313018532089857', '1240185865539600385', '1520313018473369601', 'WAREHOUSE_ID', NULL, '1', '0', 1, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313018578227202', '1240185865539600385', '1520313018473369601', 'WAREHOUSE_CLASS_ID', NULL, '0', '1', 2, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313018628558850', '1240185865539600385', '1520313018473369601', 'STORES_ID', NULL, '0', '1', 3, 'int', NULL, '10', '0', NULL);
INSERT INTO `metadata_column` VALUES ('1520313018674696194', '1240185865539600385', '1520313018473369601', 'WAREHOUSE_NAME', NULL, '0', '1', 4, 'varchar', '60', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313018708250625', '1240185865539600385', '1520313018473369601', 'WA_ADDRESS1', NULL, '0', '1', 5, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313018745999362', '1240185865539600385', '1520313018473369601', 'WA_ADDRESS2', NULL, '0', '1', 6, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313018787942402', '1240185865539600385', '1520313018473369601', 'WA_ADDRESS3', NULL, '0', '1', 7, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313018821496833', '1240185865539600385', '1520313018473369601', 'WA_ADDRESS4', NULL, '0', '1', 8, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313018855051265', '1240185865539600385', '1520313018473369601', 'WAREHOUSE_CITY', NULL, '0', '1', 9, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313018888605698', '1240185865539600385', '1520313018473369601', 'WAREHOUSE_STATE_PROVINCE', NULL, '0', '1', 10, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313018930548737', '1240185865539600385', '1520313018473369601', 'WAREHOUSE_POSTAL_CODE', NULL, '0', '1', 11, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313018968297474', '1240185865539600385', '1520313018473369601', 'WAREHOUSE_COUNTRY', NULL, '0', '1', 12, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313019001851905', '1240185865539600385', '1520313018473369601', 'WAREHOUSE_OWNER_NAME', NULL, '0', '1', 13, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313019035406337', '1240185865539600385', '1520313018473369601', 'WAREHOUSE_PHONE', NULL, '0', '1', 14, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313019077349378', '1240185865539600385', '1520313018473369601', 'WAREHOUSE_FAX', NULL, '0', '1', 15, 'varchar', '30', NULL, NULL, NULL);
INSERT INTO `metadata_column` VALUES ('1520313019115098113', '1240185865539600385', '1520313018473369601', 'CLASS_DESCRIPTION', NULL, '0', '1', 16, 'varchar', '30', NULL, NULL, NULL);

-- ----------------------------
-- Table structure for metadata_source
-- ----------------------------
DROP TABLE IF EXISTS `metadata_source`;
CREATE TABLE `metadata_source`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `db_type` tinyint(4) NULL DEFAULT NULL COMMENT '数据源类型',
  `source_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源名称',
  `is_sync` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '元数据同步（0否，1是）',
  `db_schema` json NULL COMMENT '数据源连接信息',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据源信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of metadata_source
-- ----------------------------
INSERT INTO `metadata_source` VALUES ('1240185865539600385', 1, '1214835832967581698', '2020-03-19 03:58:47', '1197789917762031617', '1214835832967581698', '2022-05-01 08:59:54', NULL, 1, '测试数据库1', '2', '{\"sid\": null, \"host\": \"localhost\", \"port\": 3306, \"dbName\": \"foodmart2\", \"password\": \"root\", \"username\": \"root\"}');
INSERT INTO `metadata_source` VALUES ('1336474987430793217', 1, '1214835832967581698', '2020-12-09 21:57:22', '1197789917762031617', '1214835832967581698', '2022-05-01 09:00:16', NULL, 1, '测试数据库2', '2', '{\"sid\": null, \"host\": \"localhost\", \"port\": 3306, \"dbName\": \"robot\", \"password\": \"root\", \"username\": \"root\"}');

-- ----------------------------
-- Table structure for metadata_table
-- ----------------------------
DROP TABLE IF EXISTS `metadata_table`;
CREATE TABLE `metadata_table`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `source_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属数据源',
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表名',
  `table_comment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表注释',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据库表信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of metadata_table
-- ----------------------------
INSERT INTO `metadata_table` VALUES ('1336479261791473665', '1336474987430793217', 'robot_patient', '患者表');
INSERT INTO `metadata_table` VALUES ('1336479264639406082', '1336474987430793217', 'robot_symptom_part', '部位表');
INSERT INTO `metadata_table` VALUES ('1336479266728169473', '1336474987430793217', 'robot_symptom_type', '症状表');
INSERT INTO `metadata_table` VALUES ('1520313001104756738', '1240185865539600385', 'account', NULL);
INSERT INTO `metadata_table` VALUES ('1520313001889091586', '1240185865539600385', 'category', NULL);
INSERT INTO `metadata_table` VALUES ('1520313002321104898', '1240185865539600385', 'chinagis1', NULL);
INSERT INTO `metadata_table` VALUES ('1520313002681815041', '1240185865539600385', 'customer', NULL);
INSERT INTO `metadata_table` VALUES ('1520313004233707521', '1240185865539600385', 'department', NULL);
INSERT INTO `metadata_table` VALUES ('1520313004447617026', '1240185865539600385', 'employee', NULL);
INSERT INTO `metadata_table` VALUES ('1520313005286477826', '1240185865539600385', 'employee_closure', NULL);
INSERT INTO `metadata_table` VALUES ('1520313005454249985', '1240185865539600385', 'expense_fact', NULL);
INSERT INTO `metadata_table` VALUES ('1520313005831737346', '1240185865539600385', 'flowtst', NULL);
INSERT INTO `metadata_table` VALUES ('1520313006045646849', '1240185865539600385', 'inventory_fact', NULL);
INSERT INTO `metadata_table` VALUES ('1520313006674792449', '1240185865539600385', 'map_data_sample', NULL);
INSERT INTO `metadata_table` VALUES ('1520313007035502593', '1240185865539600385', 'product', NULL);
INSERT INTO `metadata_table` VALUES ('1520313007958249474', '1240185865539600385', 'promotion', NULL);
INSERT INTO `metadata_table` VALUES ('1520313008448983041', '1240185865539600385', 'region', NULL);
INSERT INTO `metadata_table` VALUES ('1520313009300426753', '1240185865539600385', 'reserve_employee', NULL);
INSERT INTO `metadata_table` VALUES ('1520313010193813506', '1240185865539600385', 'salary', NULL);
INSERT INTO `metadata_table` VALUES ('1520313010709712897', '1240185865539600385', 'sales_fact_sample', NULL);
INSERT INTO `metadata_table` VALUES ('1520313011284332545', '1240185865539600385', 'sales_fact_virtual', NULL);
INSERT INTO `metadata_table` VALUES ('1520313011879923714', '1240185865539600385', 'sanky_date', NULL);
INSERT INTO `metadata_table` VALUES ('1520313012081250306', '1240185865539600385', 'siblings1', NULL);
INSERT INTO `metadata_table` VALUES ('1520313012420988930', '1240185865539600385', 'siblings2', NULL);
INSERT INTO `metadata_table` VALUES ('1520313012748144642', '1240185865539600385', 'store', NULL);
INSERT INTO `metadata_table` VALUES ('1520313014316814337', '1240185865539600385', 'store_city_key', NULL);
INSERT INTO `metadata_table` VALUES ('1520313014622998530', '1240185865539600385', 'store_ragged', NULL);
INSERT INTO `metadata_table` VALUES ('1520313016451715074', '1240185865539600385', 'store_state_key', NULL);
INSERT INTO `metadata_table` VALUES ('1520313016653041665', '1240185865539600385', 'test_exp', NULL);
INSERT INTO `metadata_table` VALUES ('1520313017147969537', '1240185865539600385', 'time_by_day', NULL);
INSERT INTO `metadata_table` VALUES ('1520313018242682882', '1240185865539600385', 'treemap_sum', NULL);
INSERT INTO `metadata_table` VALUES ('1520313018473369601', '1240185865539600385', 'warehouse', NULL);

-- ----------------------------
-- Table structure for oauth_client_details
-- ----------------------------
DROP TABLE IF EXISTS `oauth_client_details`;
CREATE TABLE `oauth_client_details`  (
  `client_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `resource_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `client_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `scope` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `authorized_grant_types` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `web_server_redirect_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `authorities` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `access_token_validity` int(11) NULL DEFAULT NULL,
  `refresh_token_validity` int(11) NULL DEFAULT NULL,
  `additional_information` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `autoapprove` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`client_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '终端信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of oauth_client_details
-- ----------------------------
INSERT INTO `oauth_client_details` VALUES ('datax', NULL, '$2a$10$3rV8TA7XlfVkZrP0kA0t7OqKoQa93Mw/VZii6nP62pqiD.AjKSUja', 'all', 'password,refresh_token,client_credentials,authorization_code', 'http://baidu.com', NULL, 86400, 604800, NULL, 'true');
INSERT INTO `oauth_client_details` VALUES ('normal-app', NULL, '$2a$10$tF1Qh5IU3BLXkt/rwhZ1x.sBnnFi7ZDIZ4VJawiSH2Ad26YE1U9nC', 'all', 'password,refresh_token', NULL, NULL, 86400, 604800, NULL, 'true');
INSERT INTO `oauth_client_details` VALUES ('trusted-app', NULL, '$2a$10$F2KzyEy9MeFLz.ic2wvJDegLFjG9xzo7n7s.TE7zdI6fWeIieVV1G', 'all', 'password,refresh_token', NULL, NULL, 0, 0, NULL, 'true');

-- ----------------------------
-- Table structure for qrtz_job
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_job`;
CREATE TABLE `qrtz_job`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `job_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务名称',
  `bean_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Spring Bean名称',
  `method_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '方法名称',
  `method_params` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '方法参数',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'cron表达式',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '定时任务信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_job
-- ----------------------------
INSERT INTO `qrtz_job` VALUES ('1346347501890191362', 0, '1214835832967581698', '2021-01-05 14:47:13', '1197789917762031617', '1214835832967581698', '2021-01-05 14:47:13', NULL, '测试无参数任务', 'quartzTask', 'withoutParams', NULL, '30 * * * * ?');
INSERT INTO `qrtz_job` VALUES ('1346347612309438465', 0, '1214835832967581698', '2021-01-05 14:47:39', '1197789917762031617', '1214835832967581698', '2021-01-05 14:47:39', NULL, '测试有参数任务', 'quartzTask', 'withParams', '我是参数', '45 * * * * ?');

-- ----------------------------
-- Table structure for qrtz_job_log
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_job_log`;
CREATE TABLE `qrtz_job_log`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `job_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务ID',
  `msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '信息记录',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '定时任务日志信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_job_log
-- ----------------------------
INSERT INTO `qrtz_job_log` VALUES ('1346347573382103042', 1, '2021-01-05 14:47:30', '1346347501890191362', '【测试无参数任务】任务执行结束，总共耗时：2毫秒');
INSERT INTO `qrtz_job_log` VALUES ('1346347636053393409', 1, '2021-01-05 14:47:45', '1346347612309438465', '【测试有参数任务】任务执行结束，总共耗时：0毫秒');

-- ----------------------------
-- Table structure for quality_check_report
-- ----------------------------
DROP TABLE IF EXISTS `quality_check_report`;
CREATE TABLE `quality_check_report`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `check_rule_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '核查规则主键',
  `check_date` datetime(0) NULL DEFAULT NULL COMMENT '核查时间',
  `check_result` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '核查结果',
  `check_total_count` int(11) NULL DEFAULT NULL COMMENT '核查数量',
  `check_error_count` int(11) NULL DEFAULT NULL COMMENT '不合规数量',
  `check_batch` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '核查批次号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '核查报告信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of quality_check_report
-- ----------------------------
INSERT INTO `quality_check_report` VALUES ('1340552514231197698', '1318749656079646721', '2020-12-20 15:00:00', NULL, 3, 0, '20201220020000');
INSERT INTO `quality_check_report` VALUES ('1340552515288162306', '1318749963664736258', '2020-12-20 15:00:00', NULL, 3, 0, '20201220020000');
INSERT INTO `quality_check_report` VALUES ('1340552516336738305', '1318750378762420225', '2020-12-20 15:00:00', NULL, 3, 0, '20201220020000');
INSERT INTO `quality_check_report` VALUES ('1340552517389508610', '1336564556213846017', '2020-12-20 15:00:00', NULL, 4, 0, '20201220020000');
INSERT INTO `quality_check_report` VALUES ('1346879442167140353', '1318749656079646721', '2021-01-07 02:00:04', NULL, 3, 0, '20210107020000');
INSERT INTO `quality_check_report` VALUES ('1346879443857444865', '1318749963664736258', '2021-01-07 02:00:04', NULL, 3, 0, '20210107020000');
INSERT INTO `quality_check_report` VALUES ('1346879445505806338', '1318750378762420225', '2021-01-07 02:00:04', NULL, 3, 0, '20210107020000');
INSERT INTO `quality_check_report` VALUES ('1346879447082864641', '1336564556213846017', '2021-01-07 02:00:04', NULL, 4, 0, '20210107020000');
INSERT INTO `quality_check_report` VALUES ('1520395642562703362', '1318749656079646721', '2022-04-30 21:32:20', NULL, 3, 0, '20220430213220');
INSERT INTO `quality_check_report` VALUES ('1520395642956967938', '1318749963664736258', '2022-04-30 21:32:20', NULL, 3, 0, '20220430213220');
INSERT INTO `quality_check_report` VALUES ('1520395643099574274', '1318750378762420225', '2022-04-30 21:32:20', NULL, 3, 0, '20220430213220');
INSERT INTO `quality_check_report` VALUES ('1520395643275735042', '1336564556213846017', '2022-04-30 21:32:20', NULL, 4, 0, '20220430213220');
INSERT INTO `quality_check_report` VALUES ('1520395670391910401', '1318749656079646721', '2022-04-30 21:32:30', NULL, 3, 0, '20220430213230');
INSERT INTO `quality_check_report` VALUES ('1520395670547099650', '1318749963664736258', '2022-04-30 21:32:30', NULL, 3, 0, '20220430213230');
INSERT INTO `quality_check_report` VALUES ('1520395670727454721', '1318750378762420225', '2022-04-30 21:32:30', NULL, 3, 0, '20220430213230');
INSERT INTO `quality_check_report` VALUES ('1520395670882643969', '1336564556213846017', '2022-04-30 21:32:30', NULL, 4, 0, '20220430213230');
INSERT INTO `quality_check_report` VALUES ('1520395712263647233', '1318749656079646721', '2022-04-30 21:32:40', NULL, 3, 0, '20220430213240');
INSERT INTO `quality_check_report` VALUES ('1520395712410447873', '1318749963664736258', '2022-04-30 21:32:40', NULL, 3, 0, '20220430213240');
INSERT INTO `quality_check_report` VALUES ('1520395712548859906', '1318750378762420225', '2022-04-30 21:32:40', NULL, 3, 0, '20220430213240');
INSERT INTO `quality_check_report` VALUES ('1520395712695660546', '1336564556213846017', '2022-04-30 21:32:40', NULL, 4, 0, '20220430213240');
INSERT INTO `quality_check_report` VALUES ('1520395754164744193', '1318749656079646721', '2022-04-30 21:32:50', NULL, 3, 0, '20220430213250');
INSERT INTO `quality_check_report` VALUES ('1520395754307350530', '1318749963664736258', '2022-04-30 21:32:50', NULL, 3, 0, '20220430213250');
INSERT INTO `quality_check_report` VALUES ('1520395754441568257', '1318750378762420225', '2022-04-30 21:32:50', NULL, 3, 0, '20220430213250');
INSERT INTO `quality_check_report` VALUES ('1520395754550620161', '1336564556213846017', '2022-04-30 21:32:50', NULL, 4, 0, '20220430213250');
INSERT INTO `quality_check_report` VALUES ('1520395796204253185', '1318749656079646721', '2022-04-30 21:33:00', NULL, 3, 0, '20220430213300');
INSERT INTO `quality_check_report` VALUES ('1520395796304916482', '1318749963664736258', '2022-04-30 21:33:00', NULL, 3, 0, '20220430213300');
INSERT INTO `quality_check_report` VALUES ('1520395796405579777', '1318750378762420225', '2022-04-30 21:33:00', NULL, 3, 0, '20220430213300');
INSERT INTO `quality_check_report` VALUES ('1520395796489465857', '1336564556213846017', '2022-04-30 21:33:00', NULL, 4, 0, '20220430213300');
INSERT INTO `quality_check_report` VALUES ('1520395838071795713', '1318749656079646721', '2022-04-30 21:33:10', NULL, 3, 0, '20220430213310');
INSERT INTO `quality_check_report` VALUES ('1520395838180847618', '1318749963664736258', '2022-04-30 21:33:10', NULL, 3, 0, '20220430213310');
INSERT INTO `quality_check_report` VALUES ('1520395838273122306', '1318750378762420225', '2022-04-30 21:33:10', NULL, 3, 0, '20220430213310');
INSERT INTO `quality_check_report` VALUES ('1520395838369591297', '1336564556213846017', '2022-04-30 21:33:10', NULL, 4, 0, '20220430213310');
INSERT INTO `quality_check_report` VALUES ('1520395880056778753', '1318749656079646721', '2022-04-30 21:33:20', NULL, 3, 0, '20220430213320');
INSERT INTO `quality_check_report` VALUES ('1520395880123887619', '1318749963664736258', '2022-04-30 21:33:20', NULL, 3, 0, '20220430213320');
INSERT INTO `quality_check_report` VALUES ('1520395880190996482', '1318750378762420225', '2022-04-30 21:33:20', NULL, 3, 0, '20220430213320');
INSERT INTO `quality_check_report` VALUES ('1520395880321019905', '1336564556213846017', '2022-04-30 21:33:20', NULL, 4, 0, '20220430213320');
INSERT INTO `quality_check_report` VALUES ('1520395921920126978', '1318749656079646721', '2022-04-30 21:33:30', NULL, 3, 0, '20220430213330');
INSERT INTO `quality_check_report` VALUES ('1520395922020790273', '1318749963664736258', '2022-04-30 21:33:30', NULL, 3, 0, '20220430213330');
INSERT INTO `quality_check_report` VALUES ('1520395922108870658', '1318750378762420225', '2022-04-30 21:33:30', NULL, 3, 0, '20220430213330');
INSERT INTO `quality_check_report` VALUES ('1520395922196951041', '1336564556213846017', '2022-04-30 21:33:30', NULL, 4, 0, '20220430213330');
INSERT INTO `quality_check_report` VALUES ('1520395963858972673', '1318749656079646721', '2022-04-30 21:33:40', NULL, 3, 0, '20220430213340');
INSERT INTO `quality_check_report` VALUES ('1520395963947053058', '1318749963664736258', '2022-04-30 21:33:40', NULL, 3, 0, '20220430213340');
INSERT INTO `quality_check_report` VALUES ('1520395964039327745', '1318750378762420225', '2022-04-30 21:33:40', NULL, 3, 0, '20220430213340');
INSERT INTO `quality_check_report` VALUES ('1520395964131602434', '1336564556213846017', '2022-04-30 21:33:40', NULL, 4, 0, '20220430213340');
INSERT INTO `quality_check_report` VALUES ('1520396007462957058', '1318749656079646721', '2022-04-30 21:33:50', NULL, 3, 0, '20220430213350');
INSERT INTO `quality_check_report` VALUES ('1520396007597174786', '1318749963664736258', '2022-04-30 21:33:50', NULL, 3, 0, '20220430213350');
INSERT INTO `quality_check_report` VALUES ('1520396007710420993', '1318750378762420225', '2022-04-30 21:33:50', NULL, 3, 0, '20220430213350');
INSERT INTO `quality_check_report` VALUES ('1520396007840444417', '1336564556213846017', '2022-04-30 21:33:50', NULL, 4, 0, '20220430213350');
INSERT INTO `quality_check_report` VALUES ('1520396047778607105', '1318749656079646721', '2022-04-30 21:34:00', NULL, 3, 0, '20220430213400');
INSERT INTO `quality_check_report` VALUES ('1520396047954767874', '1318749963664736258', '2022-04-30 21:34:00', NULL, 3, 0, '20220430213400');
INSERT INTO `quality_check_report` VALUES ('1520396048139317249', '1318750378762420225', '2022-04-30 21:34:00', NULL, 3, 0, '20220430213400');
INSERT INTO `quality_check_report` VALUES ('1520396048315478017', '1336564556213846017', '2022-04-30 21:34:00', NULL, 4, 0, '20220430213400');
INSERT INTO `quality_check_report` VALUES ('1520396089751007234', '1318749656079646721', '2022-04-30 21:34:10', NULL, 3, 0, '20220430213410');
INSERT INTO `quality_check_report` VALUES ('1520396089939750914', '1318749963664736258', '2022-04-30 21:34:10', NULL, 3, 0, '20220430213410');
INSERT INTO `quality_check_report` VALUES ('1520396090099134465', '1318750378762420225', '2022-04-30 21:34:10', NULL, 3, 0, '20220430213410');
INSERT INTO `quality_check_report` VALUES ('1520396090250129410', '1336564556213846017', '2022-04-30 21:34:10', NULL, 4, 0, '20220430213410');
INSERT INTO `quality_check_report` VALUES ('1520396131643715586', '1318749656079646721', '2022-04-30 21:34:20', NULL, 3, 0, '20220430213420');
INSERT INTO `quality_check_report` VALUES ('1520396131773739010', '1318749963664736258', '2022-04-30 21:34:20', NULL, 3, 0, '20220430213420');
INSERT INTO `quality_check_report` VALUES ('1520396131903762434', '1318750378762420225', '2022-04-30 21:34:20', NULL, 3, 0, '20220430213420');
INSERT INTO `quality_check_report` VALUES ('1520396132033785858', '1336564556213846017', '2022-04-30 21:34:20', NULL, 4, 0, '20220430213420');
INSERT INTO `quality_check_report` VALUES ('1520396173611921409', '1318749656079646721', '2022-04-30 21:34:30', NULL, 3, 0, '20220430213430');
INSERT INTO `quality_check_report` VALUES ('1520396173758722049', '1318749963664736258', '2022-04-30 21:34:30', NULL, 3, 0, '20220430213430');
INSERT INTO `quality_check_report` VALUES ('1520396173888745474', '1318750378762420225', '2022-04-30 21:34:30', NULL, 3, 0, '20220430213430');
INSERT INTO `quality_check_report` VALUES ('1520396174001991681', '1336564556213846017', '2022-04-30 21:34:30', NULL, 4, 0, '20220430213430');
INSERT INTO `quality_check_report` VALUES ('1520396215542378497', '1318749656079646721', '2022-04-30 21:34:40', NULL, 3, 0, '20220430213440');
INSERT INTO `quality_check_report` VALUES ('1520396215659819009', '1318749963664736258', '2022-04-30 21:34:40', NULL, 3, 0, '20220430213440');
INSERT INTO `quality_check_report` VALUES ('1520396215798231041', '1318750378762420225', '2022-04-30 21:34:40', NULL, 3, 0, '20220430213440');
INSERT INTO `quality_check_report` VALUES ('1520396215928254465', '1336564556213846017', '2022-04-30 21:34:40', NULL, 4, 0, '20220430213440');
INSERT INTO `quality_check_report` VALUES ('1520396257502195713', '1318749656079646721', '2022-04-30 21:34:50', NULL, 3, 0, '20220430213450');
INSERT INTO `quality_check_report` VALUES ('1520396257653190657', '1318749963664736258', '2022-04-30 21:34:50', NULL, 3, 0, '20220430213450');
INSERT INTO `quality_check_report` VALUES ('1520396257787408385', '1318750378762420225', '2022-04-30 21:34:50', NULL, 3, 0, '20220430213450');
INSERT INTO `quality_check_report` VALUES ('1520396257888071681', '1336564556213846017', '2022-04-30 21:34:50', NULL, 4, 0, '20220430213450');
INSERT INTO `quality_check_report` VALUES ('1520396299654950914', '1318749656079646721', '2022-04-30 21:35:00', NULL, 3, 0, '20220430213500');
INSERT INTO `quality_check_report` VALUES ('1520396299839500289', '1318749963664736258', '2022-04-30 21:35:00', NULL, 3, 0, '20220430213500');
INSERT INTO `quality_check_report` VALUES ('1520396299990495234', '1318750378762420225', '2022-04-30 21:35:00', NULL, 3, 0, '20220430213500');
INSERT INTO `quality_check_report` VALUES ('1520396300162461697', '1336564556213846017', '2022-04-30 21:35:00', NULL, 4, 0, '20220430213500');
INSERT INTO `quality_check_report` VALUES ('1520396341342138370', '1318749656079646721', '2022-04-30 21:35:10', NULL, 3, 0, '20220430213510');
INSERT INTO `quality_check_report` VALUES ('1520396341451190274', '1318749963664736258', '2022-04-30 21:35:10', NULL, 3, 0, '20220430213510');
INSERT INTO `quality_check_report` VALUES ('1520396341568630785', '1318750378762420225', '2022-04-30 21:35:10', NULL, 3, 0, '20220430213510');
INSERT INTO `quality_check_report` VALUES ('1520396341686071298', '1336564556213846017', '2022-04-30 21:35:10', NULL, 4, 0, '20220430213510');
INSERT INTO `quality_check_report` VALUES ('1520396383364870146', '1318749656079646721', '2022-04-30 21:35:20', NULL, 3, 0, '20220430213520');
INSERT INTO `quality_check_report` VALUES ('1520396383545225217', '1318749963664736258', '2022-04-30 21:35:20', NULL, 3, 0, '20220430213520');
INSERT INTO `quality_check_report` VALUES ('1520396383704608770', '1318750378762420225', '2022-04-30 21:35:20', NULL, 3, 0, '20220430213520');
INSERT INTO `quality_check_report` VALUES ('1520396383855603713', '1336564556213846017', '2022-04-30 21:35:20', NULL, 4, 0, '20220430213520');
INSERT INTO `quality_check_report` VALUES ('1520398648058339330', '1318749656079646721', '2022-04-30 21:44:18', NULL, 3, 0, '20220430214417');
INSERT INTO `quality_check_report` VALUES ('1520398648469381122', '1318749963664736258', '2022-04-30 21:44:18', NULL, 3, 0, '20220430214417');
INSERT INTO `quality_check_report` VALUES ('1520398648620376066', '1318750378762420225', '2022-04-30 21:44:18', NULL, 3, 0, '20220430214417');
INSERT INTO `quality_check_report` VALUES ('1520398648771371009', '1336564556213846017', '2022-04-30 21:44:18', NULL, 4, 0, '20220430214417');
INSERT INTO `quality_check_report` VALUES ('1520398751401795586', '1318749656079646721', '2022-04-30 21:44:45', NULL, 3, 0, '20220430214444');
INSERT INTO `quality_check_report` VALUES ('1520398751615705089', '1318749963664736258', '2022-04-30 21:44:45', NULL, 3, 0, '20220430214444');
INSERT INTO `quality_check_report` VALUES ('1520398751796060161', '1318750378762420225', '2022-04-30 21:44:45', NULL, 3, 0, '20220430214444');
INSERT INTO `quality_check_report` VALUES ('1520398751938666498', '1336564556213846017', '2022-04-30 21:44:45', NULL, 4, 0, '20220430214444');
INSERT INTO `quality_check_report` VALUES ('1520398794187890690', '1318749656079646721', '2022-04-30 21:44:55', NULL, 3, 0, '20220430214454');
INSERT INTO `quality_check_report` VALUES ('1520398794364051457', '1318749963664736258', '2022-04-30 21:44:55', NULL, 3, 0, '20220430214454');
INSERT INTO `quality_check_report` VALUES ('1520398794548600833', '1318750378762420225', '2022-04-30 21:44:55', NULL, 3, 0, '20220430214454');
INSERT INTO `quality_check_report` VALUES ('1520398794712178690', '1336564556213846017', '2022-04-30 21:44:55', NULL, 4, 0, '20220430214454');
INSERT INTO `quality_check_report` VALUES ('1520400230518890498', '1318749656079646721', '2022-04-30 21:50:36', NULL, 3, 0, '20220430215035');
INSERT INTO `quality_check_report` VALUES ('1520400230883794945', '1318749963664736258', '2022-04-30 21:50:36', NULL, 3, 0, '20220430215035');
INSERT INTO `quality_check_report` VALUES ('1520400231043178498', '1318750378762420225', '2022-04-30 21:50:36', NULL, 3, 0, '20220430215035');
INSERT INTO `quality_check_report` VALUES ('1520400231236116482', '1336564556213846017', '2022-04-30 21:50:36', NULL, 4, 0, '20220430215035');
INSERT INTO `quality_check_report` VALUES ('1520400347732910081', '1318749656079646721', '2022-04-30 21:51:05', NULL, 3, 0, '20220430215105');
INSERT INTO `quality_check_report` VALUES ('1520400347925848066', '1318749963664736258', '2022-04-30 21:51:05', NULL, 3, 0, '20220430215105');
INSERT INTO `quality_check_report` VALUES ('1520400348110397442', '1318750378762420225', '2022-04-30 21:51:05', NULL, 3, 0, '20220430215105');
INSERT INTO `quality_check_report` VALUES ('1520400348299141121', '1336564556213846017', '2022-04-30 21:51:05', NULL, 4, 0, '20220430215105');
INSERT INTO `quality_check_report` VALUES ('1520400447544762370', '1318749656079646721', '2022-04-30 21:51:29', NULL, 3, 0, '20220430215128');
INSERT INTO `quality_check_report` VALUES ('1520400447708340226', '1318749963664736258', '2022-04-30 21:51:29', NULL, 3, 0, '20220430215128');
INSERT INTO `quality_check_report` VALUES ('1520400447905472513', '1318750378762420225', '2022-04-30 21:51:29', NULL, 3, 0, '20220430215128');
INSERT INTO `quality_check_report` VALUES ('1520400448056467457', '1336564556213846017', '2022-04-30 21:51:29', NULL, 4, 0, '20220430215128');
INSERT INTO `quality_check_report` VALUES ('1520401442756726786', '1318749656079646721', '2022-04-30 21:55:24', NULL, 3, 0, '20220430215524');
INSERT INTO `quality_check_report` VALUES ('1520401443264237570', '1318749963664736258', '2022-04-30 21:55:24', NULL, 3, 0, '20220430215524');
INSERT INTO `quality_check_report` VALUES ('1520401443566227457', '1318750378762420225', '2022-04-30 21:55:24', NULL, 3, 0, '20220430215524');
INSERT INTO `quality_check_report` VALUES ('1520401443784331266', '1336564556213846017', '2022-04-30 21:55:24', NULL, 4, 0, '20220430215524');
INSERT INTO `quality_check_report` VALUES ('1520401546838380546', '1318749656079646721', '2022-04-30 21:55:51', NULL, 3, 0, '20220430215551');
INSERT INTO `quality_check_report` VALUES ('1520401547035512834', '1318749963664736258', '2022-04-30 21:55:51', NULL, 3, 0, '20220430215551');
INSERT INTO `quality_check_report` VALUES ('1520401547215867906', '1318750378762420225', '2022-04-30 21:55:51', NULL, 3, 0, '20220430215551');
INSERT INTO `quality_check_report` VALUES ('1520401547387834370', '1336564556213846017', '2022-04-30 21:55:51', NULL, 4, 0, '20220430215551');
INSERT INTO `quality_check_report` VALUES ('1520402371950166017', '1318749656079646721', '2022-04-30 21:59:06', NULL, 3, 0, '20220430215905');
INSERT INTO `quality_check_report` VALUES ('1520402372373790721', '1318749963664736258', '2022-04-30 21:59:06', NULL, 3, 0, '20220430215905');
INSERT INTO `quality_check_report` VALUES ('1520402372549951490', '1318750378762420225', '2022-04-30 21:59:06', NULL, 3, 0, '20220430215905');
INSERT INTO `quality_check_report` VALUES ('1520402372751278082', '1336564556213846017', '2022-04-30 21:59:06', NULL, 4, 0, '20220430215905');
INSERT INTO `quality_check_report` VALUES ('1520402389834678273', '1318749656079646721', '2022-04-30 21:59:12', NULL, 3, 0, '20220430215912');
INSERT INTO `quality_check_report` VALUES ('1520402390254108673', '1318749963664736258', '2022-04-30 21:59:12', NULL, 3, 0, '20220430215912');
INSERT INTO `quality_check_report` VALUES ('1520402390530932737', '1318750378762420225', '2022-04-30 21:59:12', NULL, 3, 0, '20220430215912');
INSERT INTO `quality_check_report` VALUES ('1520402390749036545', '1336564556213846017', '2022-04-30 21:59:12', NULL, 4, 0, '20220430215912');
INSERT INTO `quality_check_report` VALUES ('1520407899967377409', '1318749656079646721', '2022-04-30 22:21:03', NULL, 3, 0, '20220430222102');
INSERT INTO `quality_check_report` VALUES ('1520407900382613505', '1318749963664736258', '2022-04-30 22:21:03', NULL, 3, 0, '20220430222102');
INSERT INTO `quality_check_report` VALUES ('1520407903259906050', '1318750378762420225', '2022-04-30 22:21:03', NULL, 3, 0, '20220430222102');
INSERT INTO `quality_check_report` VALUES ('1520407903498981378', '1336564556213846017', '2022-04-30 22:21:03', NULL, 4, 0, '20220430222102');
INSERT INTO `quality_check_report` VALUES ('1520592971832020994', '1318749656079646721', '2022-05-01 10:36:30', NULL, 3, 0, '20220501103629');
INSERT INTO `quality_check_report` VALUES ('1520592972108845057', '1318749963664736258', '2022-05-01 10:36:30', NULL, 3, 0, '20220501103629');
INSERT INTO `quality_check_report` VALUES ('1520592972297588737', '1318750378762420225', '2022-05-01 10:36:30', NULL, 3, 0, '20220501103629');
INSERT INTO `quality_check_report` VALUES ('1520592972423417857', '1336564556213846017', '2022-05-01 10:36:30', NULL, 4, 0, '20220501103629');
INSERT INTO `quality_check_report` VALUES ('1521163128354656257', '1318749656079646721', '2022-05-03 00:22:03', NULL, 3, 0, '20220503002203');
INSERT INTO `quality_check_report` VALUES ('1521163129055105025', '1318749963664736258', '2022-05-03 00:22:03', NULL, 3, 0, '20220503002203');
INSERT INTO `quality_check_report` VALUES ('1521163129302568961', '1318750378762420225', '2022-05-03 00:22:03', NULL, 3, 0, '20220503002203');
INSERT INTO `quality_check_report` VALUES ('1521163129780719618', '1336564556213846017', '2022-05-03 00:22:03', NULL, 4, 0, '20220503002203');

-- ----------------------------
-- Table structure for quality_check_rule
-- ----------------------------
DROP TABLE IF EXISTS `quality_check_rule`;
CREATE TABLE `quality_check_rule`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `rule_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则名称',
  `rule_type_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则类型',
  `rule_item_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '核查类型',
  `rule_level_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则级别',
  `rule_db_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源类型',
  `rule_source_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源主键',
  `rule_source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源',
  `rule_table_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据表主键',
  `rule_table` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据表',
  `rule_table_comment` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据表名称',
  `rule_column_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段主键',
  `rule_column` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段',
  `rule_column_comment` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段名称',
  `config_json` json NULL COMMENT '核查配置',
  `rule_sql` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '核查脚本',
  `last_check_batch` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最近核查批次号（关联确定唯一核查报告）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '核查规则信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of quality_check_rule
-- ----------------------------
INSERT INTO `quality_check_rule` VALUES ('1318749656079646721', 1, '1214835832967581698', '2020-10-21 11:03:14', '1197789917762031617', '1214835832967581698', '2020-12-09 09:32:19', NULL, '唯一校验', '1310055102039498754', '1316555325628764161', '1316243646609117186', '1', '1336474987430793217', 'robot数据库', '1336479261791473665', 'robot_patient', '患者表', '1336479262852632577', 'id', '主键', '{\"accuracy\": {\"maxLength\": null}, \"relevance\": {\"relatedTable\": null, \"relatedColumn\": null, \"relatedTableId\": null, \"relatedColumnId\": null, \"relatedTableComment\": null, \"relatedColumnComment\": null}, \"consistent\": {\"gbTypeId\": null, \"gbTypeCode\": null, \"gbTypeName\": null, \"bindGbColumn\": null}, \"timeliness\": {\"threshold\": null}, \"ruleItemCode\": \"unique_key\"}', 'SELECT totalCount - errorCount AS errorCount, totalCount FROM (SELECT COUNT(DISTINCT id) AS errorCount, COUNT(*) AS totalCount FROM robot_patient) TEMP', '20220503002203');
INSERT INTO `quality_check_rule` VALUES ('1318749963664736258', 1, '1214835832967581698', '2020-10-21 11:04:27', '1197789917762031617', '1214835832967581698', '2022-04-30 20:20:01', NULL, '完整校验', '1310055106909085697', '1316555332956213250', '1316243649473826818', '1', '1336474987430793217', 'robot数据库', '1336479261791473665', 'robot_patient', '患者表', '1336479262852632577', 'id', '主键', '{\"accuracy\": {\"maxLength\": null}, \"relevance\": {\"relatedTable\": null, \"relatedColumn\": null, \"relatedTableId\": null, \"relatedColumnId\": null, \"relatedTableComment\": null, \"relatedColumnComment\": null}, \"consistent\": {\"gbTypeId\": null, \"gbTypeCode\": null, \"gbTypeName\": null, \"bindGbColumn\": null}, \"timeliness\": {\"threshold\": null}, \"ruleItemCode\": \"integrity_key\"}', 'SELECT SUM(CASE WHEN id IS NOT NULL AND TRIM(id) != \'\' THEN 0 ELSE 1 END), COUNT(*) FROM robot_patient', '20220503002203');
INSERT INTO `quality_check_rule` VALUES ('1318750378762420225', 1, '1214835832967581698', '2020-10-21 11:06:06', '1197789917762031617', '1214835832967581698', '2020-12-09 09:33:04', NULL, '一致校验', '1310055114131677186', '1316555329772736514', '1316243646609117186', '1', '1336474987430793217', 'robot数据库', '1336479261791473665', 'robot_patient', '患者表', '1336479264106729474', 'patient_sex', '患者性别（1男2女）', '{\"accuracy\": {\"maxLength\": null}, \"relevance\": {\"relatedTable\": null, \"relatedColumn\": null, \"relatedTableId\": null, \"relatedColumnId\": null, \"relatedTableComment\": null, \"relatedColumnComment\": null}, \"consistent\": {\"gbTypeId\": \"1303245849463218178\", \"gbTypeCode\": \"GB/T 2261.1-2003\", \"gbTypeName\": \"人的性别代码\", \"bindGbColumn\": \"gb_code\"}, \"timeliness\": {\"threshold\": null}, \"ruleItemCode\": \"consistent_key\"}', 'SELECT SUM(CASE WHEN patient_sex NOT IN (\'0\',\'1\',\'2\',\'9\') THEN 1 ELSE 0 END), COUNT(*) FROM robot_patient', '20220503002203');
INSERT INTO `quality_check_rule` VALUES ('1336564556213846017', 1, '1214835832967581698', '2020-12-09 14:53:17', '1197789917762031617', '1214835832967581698', '2020-12-09 14:53:17', NULL, '关联性校验', '1310055118023991297', '1316555336190021633', '1316243646609117186', '1', '1336474987430793217', 'robot数据库', '1336479266728169473', 'robot_symptom_type', '症状表', '1336479268242313218', 'part_id', '所属部位', '{\"accuracy\": {\"maxLength\": null}, \"relevance\": {\"relatedTable\": \"robot_symptom_part\", \"relatedColumn\": \"id\", \"relatedTableId\": \"1336479264639406082\", \"relatedColumnId\": \"1336479265583124482\", \"relatedTableComment\": \"部位表\", \"relatedColumnComment\": \"主键\"}, \"consistent\": {\"gbTypeId\": null, \"gbTypeCode\": null, \"gbTypeName\": null, \"bindGbColumn\": null}, \"timeliness\": {\"threshold\": null}, \"ruleItemCode\": \"relevance_key\"}', 'SELECT SUM(errorCount) AS errorCount, SUM(totalCount) AS totalCount FROM (SELECT COUNT(*) AS errorCount, 0 AS totalCount FROM robot_symptom_type a WHERE NOT EXISTS (SELECT 1 FROM robot_symptom_part b WHERE a.part_id = b.id)UNION SELECT 0 AS errorCount, COUNT(*) AS totalCount FROM robot_symptom_type) TEMP', '20220503002203');

-- ----------------------------
-- Table structure for quality_rule_item
-- ----------------------------
DROP TABLE IF EXISTS `quality_rule_item`;
CREATE TABLE `quality_rule_item`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `rule_type_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则类型',
  `item_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '核查项编码',
  `item_explain` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '核查项解释',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '规则核查类型信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of quality_rule_item
-- ----------------------------
INSERT INTO `quality_rule_item` VALUES ('1316555325628764161', '1310055102039498754', 'unique_key', '字段唯一性');
INSERT INTO `quality_rule_item` VALUES ('1316555329772736514', '1310055114131677186', 'consistent_key', '字典一致性');
INSERT INTO `quality_rule_item` VALUES ('1316555332956213250', '1310055106909085697', 'integrity_key', '非空完整性');
INSERT INTO `quality_rule_item` VALUES ('1316555336190021633', '1310055118023991297', 'relevance_key', '字段关联性');
INSERT INTO `quality_rule_item` VALUES ('1316555339457384449', '1310055122348318721', 'timeliness_key', '字段及时性');
INSERT INTO `quality_rule_item` VALUES ('1316555342435340289', '1310055110574907393', 'accuracy_key_length', '长度准确性');

-- ----------------------------
-- Table structure for quality_rule_level
-- ----------------------------
DROP TABLE IF EXISTS `quality_rule_level`;
CREATE TABLE `quality_rule_level`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则级别编码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则级别名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '规则级别信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of quality_rule_level
-- ----------------------------
INSERT INTO `quality_rule_level` VALUES ('1316243642557419521', '1', '低');
INSERT INTO `quality_rule_level` VALUES ('1316243646609117186', '2', '中');
INSERT INTO `quality_rule_level` VALUES ('1316243649473826818', '3', '高');

-- ----------------------------
-- Table structure for quality_rule_type
-- ----------------------------
DROP TABLE IF EXISTS `quality_rule_type`;
CREATE TABLE `quality_rule_type`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型编码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '规则类型信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of quality_rule_type
-- ----------------------------
INSERT INTO `quality_rule_type` VALUES ('1310055102039498754', '唯一性校验', 'unique');
INSERT INTO `quality_rule_type` VALUES ('1310055106909085697', '完整性校验', 'integrity');
INSERT INTO `quality_rule_type` VALUES ('1310055110574907393', '准确性校验', 'accuracy');
INSERT INTO `quality_rule_type` VALUES ('1310055114131677186', '一致性校验', 'consistent');
INSERT INTO `quality_rule_type` VALUES ('1310055118023991297', '关联性校验', 'relevance');
INSERT INTO `quality_rule_type` VALUES ('1310055122348318721', '及时性校验', 'timeliness');

-- ----------------------------
-- Table structure for quality_schedule_job
-- ----------------------------
DROP TABLE IF EXISTS `quality_schedule_job`;
CREATE TABLE `quality_schedule_job`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `job_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务名称',
  `bean_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'bean名称',
  `method_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '方法名称',
  `method_params` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '方法参数',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'cron表达式',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（1运行 0暂停）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据质量监控任务信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of quality_schedule_job
-- ----------------------------
INSERT INTO `quality_schedule_job` VALUES ('1310823026538962945', '数据质量监控任务', 'qualityTask', 'task', NULL, '* * 2 * * ?', 1);

-- ----------------------------
-- Table structure for quality_schedule_log
-- ----------------------------
DROP TABLE IF EXISTS `quality_schedule_log`;
CREATE TABLE `quality_schedule_log`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态（1成功 0失败）',
  `execute_job_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '执行任务主键',
  `execute_rule_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '执行规则主键',
  `execute_date` datetime(0) NULL DEFAULT NULL COMMENT '执行时间',
  `execute_result` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '执行结果',
  `execute_batch` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '执行批次号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据质量监控任务日志信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of quality_schedule_log
-- ----------------------------
INSERT INTO `quality_schedule_log` VALUES ('1340552514940035074', '1', '1310823026538962945', '1318749656079646721', '2020-12-20 15:00:00', NULL, '20201220020000');
INSERT INTO `quality_schedule_log` VALUES ('1340552515988611073', '1', '1310823026538962945', '1318749963664736258', '2020-12-20 15:00:00', NULL, '20201220020000');
INSERT INTO `quality_schedule_log` VALUES ('1340552517041381378', '1', '1310823026538962945', '1318750378762420225', '2020-12-20 15:00:00', NULL, '20201220020000');
INSERT INTO `quality_schedule_log` VALUES ('1340552518194814978', '1', '1310823026538962945', '1336564556213846017', '2020-12-20 15:00:00', NULL, '20201220020000');
INSERT INTO `quality_schedule_log` VALUES ('1346516951155232769', '0', '1310823026538962945', '1318749656079646721', '2021-01-06 02:00:03', '获取数据源接口出错', '20210106020000');
INSERT INTO `quality_schedule_log` VALUES ('1346516953088806914', '0', '1310823026538962945', '1318749963664736258', '2021-01-06 02:00:03', '获取数据源接口出错', '20210106020000');
INSERT INTO `quality_schedule_log` VALUES ('1346516953143332866', '0', '1310823026538962945', '1318750378762420225', '2021-01-06 02:00:03', '获取数据源接口出错', '20210106020000');
INSERT INTO `quality_schedule_log` VALUES ('1346516953336270850', '0', '1310823026538962945', '1336564556213846017', '2021-01-06 02:00:03', '获取数据源接口出错', '20210106020000');
INSERT INTO `quality_schedule_log` VALUES ('1346879443580620802', '1', '1310823026538962945', '1318749656079646721', '2021-01-07 02:00:04', NULL, '20210107020000');
INSERT INTO `quality_schedule_log` VALUES ('1346879444222349313', '1', '1310823026538962945', '1318749963664736258', '2021-01-07 02:00:04', NULL, '20210107020000');
INSERT INTO `quality_schedule_log` VALUES ('1346879446915092482', '1', '1310823026538962945', '1318750378762420225', '2021-01-07 02:00:04', NULL, '20210107020000');
INSERT INTO `quality_schedule_log` VALUES ('1346879447737176066', '1', '1310823026538962945', '1336564556213846017', '2021-01-07 02:00:04', NULL, '20210107020000');
INSERT INTO `quality_schedule_log` VALUES ('1520395642889859073', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:32:20', NULL, '20220430213220');
INSERT INTO `quality_schedule_log` VALUES ('1520395643057631233', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:32:20', NULL, '20220430213220');
INSERT INTO `quality_schedule_log` VALUES ('1520395643233792002', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:32:20', NULL, '20220430213220');
INSERT INTO `quality_schedule_log` VALUES ('1520395643368009729', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:32:20', NULL, '20220430213220');
INSERT INTO `quality_schedule_log` VALUES ('1520395670492573698', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:32:30', NULL, '20220430213230');
INSERT INTO `quality_schedule_log` VALUES ('1520395670685511682', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:32:30', NULL, '20220430213230');
INSERT INTO `quality_schedule_log` VALUES ('1520395670840700930', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:32:30', NULL, '20220430213230');
INSERT INTO `quality_schedule_log` VALUES ('1520395670979112961', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:32:30', NULL, '20220430213230');
INSERT INTO `quality_schedule_log` VALUES ('1520395712360116226', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:32:40', NULL, '20220430213240');
INSERT INTO `quality_schedule_log` VALUES ('1520395712511111170', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:32:40', NULL, '20220430213240');
INSERT INTO `quality_schedule_log` VALUES ('1520395712662106114', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:32:40', NULL, '20220430213240');
INSERT INTO `quality_schedule_log` VALUES ('1520395712775352322', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:32:40', NULL, '20220430213240');
INSERT INTO `quality_schedule_log` VALUES ('1520395754265407490', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:32:50', NULL, '20220430213250');
INSERT INTO `quality_schedule_log` VALUES ('1520395754399625218', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:32:50', NULL, '20220430213250');
INSERT INTO `quality_schedule_log` VALUES ('1520395754517065729', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:32:50', NULL, '20220430213250');
INSERT INTO `quality_schedule_log` VALUES ('1520395754634506242', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:32:50', NULL, '20220430213250');
INSERT INTO `quality_schedule_log` VALUES ('1520395796271362049', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:33:00', NULL, '20220430213300');
INSERT INTO `quality_schedule_log` VALUES ('1520395796372025346', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:33:00', NULL, '20220430213300');
INSERT INTO `quality_schedule_log` VALUES ('1520395796464300033', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:33:00', NULL, '20220430213300');
INSERT INTO `quality_schedule_log` VALUES ('1520395796552380418', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:33:00', NULL, '20220430213300');
INSERT INTO `quality_schedule_log` VALUES ('1520395838147293186', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:33:10', NULL, '20220430213310');
INSERT INTO `quality_schedule_log` VALUES ('1520395838239567874', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:33:10', NULL, '20220430213310');
INSERT INTO `quality_schedule_log` VALUES ('1520395838340231170', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:33:10', NULL, '20220430213310');
INSERT INTO `quality_schedule_log` VALUES ('1520395838428311553', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:33:10', NULL, '20220430213310');
INSERT INTO `quality_schedule_log` VALUES ('1520395880123887618', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:33:20', NULL, '20220430213320');
INSERT INTO `quality_schedule_log` VALUES ('1520395880190996481', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:33:20', NULL, '20220430213320');
INSERT INTO `quality_schedule_log` VALUES ('1520395880253911041', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:33:20', NULL, '20220430213320');
INSERT INTO `quality_schedule_log` VALUES ('1520395880388128769', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:33:20', NULL, '20220430213320');
INSERT INTO `quality_schedule_log` VALUES ('1520395921995624450', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:33:30', NULL, '20220430213330');
INSERT INTO `quality_schedule_log` VALUES ('1520395922083704834', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:33:30', NULL, '20220430213330');
INSERT INTO `quality_schedule_log` VALUES ('1520395922171785217', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:33:30', NULL, '20220430213330');
INSERT INTO `quality_schedule_log` VALUES ('1520395922259865601', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:33:30', NULL, '20220430213330');
INSERT INTO `quality_schedule_log` VALUES ('1520395963921887234', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:33:40', NULL, '20220430213340');
INSERT INTO `quality_schedule_log` VALUES ('1520395964009967618', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:33:40', NULL, '20220430213340');
INSERT INTO `quality_schedule_log` VALUES ('1520395964102242306', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:33:40', NULL, '20220430213340');
INSERT INTO `quality_schedule_log` VALUES ('1520395964177739778', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:33:40', NULL, '20220430213340');
INSERT INTO `quality_schedule_log` VALUES ('1520396007555231745', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:33:50', NULL, '20220430213350');
INSERT INTO `quality_schedule_log` VALUES ('1520396007672672258', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:33:50', NULL, '20220430213350');
INSERT INTO `quality_schedule_log` VALUES ('1520396007802695681', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:33:50', NULL, '20220430213350');
INSERT INTO `quality_schedule_log` VALUES ('1520396007924330498', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:33:50', NULL, '20220430213350');
INSERT INTO `quality_schedule_log` VALUES ('1520396047896047617', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:34:00', NULL, '20220430213400');
INSERT INTO `quality_schedule_log` VALUES ('1520396048080596993', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:34:00', NULL, '20220430213400');
INSERT INTO `quality_schedule_log` VALUES ('1520396048273534977', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:34:00', NULL, '20220430213400');
INSERT INTO `quality_schedule_log` VALUES ('1520396048441307138', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:34:00', NULL, '20220430213400');
INSERT INTO `quality_schedule_log` VALUES ('1520396089872642050', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:34:10', NULL, '20220430213410');
INSERT INTO `quality_schedule_log` VALUES ('1520396090057191426', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:34:10', NULL, '20220430213410');
INSERT INTO `quality_schedule_log` VALUES ('1520396090212380674', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:34:10', NULL, '20220430213410');
INSERT INTO `quality_schedule_log` VALUES ('1520396090342404098', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:34:10', NULL, '20220430213410');
INSERT INTO `quality_schedule_log` VALUES ('1520396131773739009', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:34:20', NULL, '20220430213420');
INSERT INTO `quality_schedule_log` VALUES ('1520396131903762433', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:34:20', NULL, '20220430213420');
INSERT INTO `quality_schedule_log` VALUES ('1520396132033785857', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:34:20', NULL, '20220430213420');
INSERT INTO `quality_schedule_log` VALUES ('1520396132168003585', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:34:20', NULL, '20220430213420');
INSERT INTO `quality_schedule_log` VALUES ('1520396173704196098', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:34:30', NULL, '20220430213430');
INSERT INTO `quality_schedule_log` VALUES ('1520396173850996737', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:34:30', NULL, '20220430213430');
INSERT INTO `quality_schedule_log` VALUES ('1520396173964242945', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:34:30', NULL, '20220430213430');
INSERT INTO `quality_schedule_log` VALUES ('1520396174090072066', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:34:30', NULL, '20220430213430');
INSERT INTO `quality_schedule_log` VALUES ('1520396215626264577', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:34:40', NULL, '20220430213440');
INSERT INTO `quality_schedule_log` VALUES ('1520396215752093697', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:34:40', NULL, '20220430213440');
INSERT INTO `quality_schedule_log` VALUES ('1520396215886311425', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:34:40', NULL, '20220430213440');
INSERT INTO `quality_schedule_log` VALUES ('1520396216020529153', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:34:40', NULL, '20220430213440');
INSERT INTO `quality_schedule_log` VALUES ('1520396257611247618', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:34:50', NULL, '20220430213450');
INSERT INTO `quality_schedule_log` VALUES ('1520396257745465345', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:34:50', NULL, '20220430213450');
INSERT INTO `quality_schedule_log` VALUES ('1520396257854517249', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:34:50', NULL, '20220430213450');
INSERT INTO `quality_schedule_log` VALUES ('1520396257980346370', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:34:50', NULL, '20220430213450');
INSERT INTO `quality_schedule_log` VALUES ('1520396299789168641', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:35:00', NULL, '20220430213500');
INSERT INTO `quality_schedule_log` VALUES ('1520396299944357889', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:35:00', NULL, '20220430213500');
INSERT INTO `quality_schedule_log` VALUES ('1520396300107935745', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:35:00', NULL, '20220430213500');
INSERT INTO `quality_schedule_log` VALUES ('1520396300263124994', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:35:00', NULL, '20220430213500');
INSERT INTO `quality_schedule_log` VALUES ('1520396341421830146', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:35:10', NULL, '20220430213510');
INSERT INTO `quality_schedule_log` VALUES ('1520396341526687746', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:35:10', NULL, '20220430213510');
INSERT INTO `quality_schedule_log` VALUES ('1520396341644128258', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:35:10', NULL, '20220430213510');
INSERT INTO `quality_schedule_log` VALUES ('1520396341761568770', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:35:10', NULL, '20220430213510');
INSERT INTO `quality_schedule_log` VALUES ('1520396383478116353', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:35:20', NULL, '20220430213520');
INSERT INTO `quality_schedule_log` VALUES ('1520396383641694209', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:35:20', NULL, '20220430213520');
INSERT INTO `quality_schedule_log` VALUES ('1520396383813660673', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:35:20', NULL, '20220430213520');
INSERT INTO `quality_schedule_log` VALUES ('1520396383956267010', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:35:20', NULL, '20220430213520');
INSERT INTO `quality_schedule_log` VALUES ('1520398648381300738', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:44:18', NULL, '20220430214417');
INSERT INTO `quality_schedule_log` VALUES ('1520398648578433025', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:44:18', NULL, '20220430214417');
INSERT INTO `quality_schedule_log` VALUES ('1520398648721039361', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:44:18', NULL, '20220430214417');
INSERT INTO `quality_schedule_log` VALUES ('1520398648909783041', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:44:18', NULL, '20220430214417');
INSERT INTO `quality_schedule_log` VALUES ('1520398751540207617', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:44:45', NULL, '20220430214444');
INSERT INTO `quality_schedule_log` VALUES ('1520398751749922818', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:44:45', NULL, '20220430214444');
INSERT INTO `quality_schedule_log` VALUES ('1520398751888334850', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:44:45', NULL, '20220430214444');
INSERT INTO `quality_schedule_log` VALUES ('1520398752056107009', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:44:45', NULL, '20220430214444');
INSERT INTO `quality_schedule_log` VALUES ('1520398794301136898', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:44:55', NULL, '20220430214454');
INSERT INTO `quality_schedule_log` VALUES ('1520398794485686273', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:44:55', NULL, '20220430214454');
INSERT INTO `quality_schedule_log` VALUES ('1520398794661847042', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:44:55', NULL, '20220430214454');
INSERT INTO `quality_schedule_log` VALUES ('1520398794821230593', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:44:55', NULL, '20220430214454');
INSERT INTO `quality_schedule_log` VALUES ('1520400230791520258', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:50:36', NULL, '20220430215035');
INSERT INTO `quality_schedule_log` VALUES ('1520400230992846850', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:50:36', NULL, '20220430215035');
INSERT INTO `quality_schedule_log` VALUES ('1520400231169007618', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:50:36', NULL, '20220430215035');
INSERT INTO `quality_schedule_log` VALUES ('1520400231370334210', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:50:36', NULL, '20220430215035');
INSERT INTO `quality_schedule_log` VALUES ('1520400347858739201', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:51:05', NULL, '20220430215105');
INSERT INTO `quality_schedule_log` VALUES ('1520400348060065793', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:51:05', NULL, '20220430215105');
INSERT INTO `quality_schedule_log` VALUES ('1520400348240420865', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:51:05', NULL, '20220430215105');
INSERT INTO `quality_schedule_log` VALUES ('1520400348424970242', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:51:05', NULL, '20220430215105');
INSERT INTO `quality_schedule_log` VALUES ('1520400447658008578', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:51:29', NULL, '20220430215128');
INSERT INTO `quality_schedule_log` VALUES ('1520400447850946561', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:51:29', NULL, '20220430215128');
INSERT INTO `quality_schedule_log` VALUES ('1520400448014524418', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:51:29', NULL, '20220430215128');
INSERT INTO `quality_schedule_log` VALUES ('1520400448148742146', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:51:29', NULL, '20220430215128');
INSERT INTO `quality_schedule_log` VALUES ('1520401443117436929', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:55:24', NULL, '20220430215524');
INSERT INTO `quality_schedule_log` VALUES ('1520401443452981249', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:55:24', NULL, '20220430215524');
INSERT INTO `quality_schedule_log` VALUES ('1520401443742388226', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:55:24', NULL, '20220430215524');
INSERT INTO `quality_schedule_log` VALUES ('1520401443981463553', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:55:24', NULL, '20220430215524');
INSERT INTO `quality_schedule_log` VALUES ('1520401546972598274', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:55:51', NULL, '20220430215551');
INSERT INTO `quality_schedule_log` VALUES ('1520401547148759042', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:55:51', NULL, '20220430215551');
INSERT INTO `quality_schedule_log` VALUES ('1520401547329114113', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:55:51', NULL, '20220430215551');
INSERT INTO `quality_schedule_log` VALUES ('1520401547547217921', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:55:51', NULL, '20220430215551');
INSERT INTO `quality_schedule_log` VALUES ('1520402372315070465', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:59:06', NULL, '20220430215905');
INSERT INTO `quality_schedule_log` VALUES ('1520402372482842625', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:59:06', NULL, '20220430215905');
INSERT INTO `quality_schedule_log` VALUES ('1520402372700946433', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:59:06', NULL, '20220430215905');
INSERT INTO `quality_schedule_log` VALUES ('1520402373804048386', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:59:06', NULL, '20220430215905');
INSERT INTO `quality_schedule_log` VALUES ('1520402390203777026', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 21:59:12', NULL, '20220430215912');
INSERT INTO `quality_schedule_log` VALUES ('1520402390472212482', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 21:59:12', NULL, '20220430215912');
INSERT INTO `quality_schedule_log` VALUES ('1520402390677733377', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 21:59:12', NULL, '20220430215912');
INSERT INTO `quality_schedule_log` VALUES ('1520402390908420098', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 21:59:12', NULL, '20220430215912');
INSERT INTO `quality_schedule_log` VALUES ('1520407900319698946', '1', '1310823026538962945', '1318749656079646721', '2022-04-30 22:21:03', NULL, '20220430222102');
INSERT INTO `quality_schedule_log` VALUES ('1520407903201185793', '1', '1310823026538962945', '1318749963664736258', '2022-04-30 22:21:03', NULL, '20220430222102');
INSERT INTO `quality_schedule_log` VALUES ('1520407903419289602', '1', '1310823026538962945', '1318750378762420225', '2022-04-30 22:21:03', NULL, '20220430222102');
INSERT INTO `quality_schedule_log` VALUES ('1520407903624810498', '1', '1310823026538962945', '1336564556213846017', '2022-04-30 22:21:03', NULL, '20220430222102');
INSERT INTO `quality_schedule_log` VALUES ('1520592972029153281', '1', '1310823026538962945', '1318749656079646721', '2022-05-01 10:36:30', NULL, '20220501103629');
INSERT INTO `quality_schedule_log` VALUES ('1520592972251451393', '1', '1310823026538962945', '1318749963664736258', '2022-05-01 10:36:30', NULL, '20220501103629');
INSERT INTO `quality_schedule_log` VALUES ('1520592972385669121', '1', '1310823026538962945', '1318750378762420225', '2022-05-01 10:36:30', NULL, '20220501103629');
INSERT INTO `quality_schedule_log` VALUES ('1520592972519886849', '1', '1310823026538962945', '1336564556213846017', '2022-05-01 10:36:30', NULL, '20220501103629');
INSERT INTO `quality_schedule_log` VALUES ('1521163128992190465', '1', '1310823026538962945', '1318749656079646721', '2022-05-03 00:22:03', NULL, '20220503002203');
INSERT INTO `quality_schedule_log` VALUES ('1521163129227071490', '1', '1310823026538962945', '1318749963664736258', '2022-05-03 00:22:03', NULL, '20220503002203');
INSERT INTO `quality_schedule_log` VALUES ('1521163129705222145', '1', '1310823026538962945', '1318750378762420225', '2022-05-03 00:22:03', NULL, '20220503002203');
INSERT INTO `quality_schedule_log` VALUES ('1521163129965268994', '1', '1310823026538962945', '1336564556213846017', '2022-05-03 00:22:03', NULL, '20220503002203');

-- ----------------------------
-- Table structure for standard_contrast
-- ----------------------------
DROP TABLE IF EXISTS `standard_contrast`;
CREATE TABLE `standard_contrast`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `source_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源主键',
  `source_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源',
  `table_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据表主键',
  `table_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据表',
  `table_comment` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据表名称',
  `column_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段主键',
  `column_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段',
  `column_comment` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段名称',
  `gb_type_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标准类别主键',
  `bind_gb_column` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '绑定标准字段',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '对照表信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of standard_contrast
-- ----------------------------
INSERT INTO `standard_contrast` VALUES ('1336483007929868290', 1, '1214835832967581698', '2020-12-09 09:29:14', '1197789917762031617', '1214835832967581698', '2022-04-30 18:42:52', NULL, '1336474987430793217', 'robot数据库', '1336479261791473665', 'robot_patient', '患者表', '1336479264106729474', 'patient_sex', '患者性别（1男2女）', '1303245849463218178', 'gb_code');

-- ----------------------------
-- Table structure for standard_contrast_dict
-- ----------------------------
DROP TABLE IF EXISTS `standard_contrast_dict`;
CREATE TABLE `standard_contrast_dict`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0未对照，1已对照）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `contrast_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典对照主键',
  `col_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典编码',
  `col_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典名称',
  `contrast_gb_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '对照的标准字典',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典对照信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of standard_contrast_dict
-- ----------------------------
INSERT INTO `standard_contrast_dict` VALUES ('1336483232853614594', 1, '1214835832967581698', '2020-12-09 09:30:08', '1197789917762031617', '1214835832967581698', '2020-12-09 09:30:08', NULL, '1336483007929868290', '1', '男', '1303247360368926722');
INSERT INTO `standard_contrast_dict` VALUES ('1336483277371957249', 1, '1214835832967581698', '2020-12-10 11:30:19', '1197789917762031617', '1214835832967581698', '2020-12-10 11:30:19', NULL, '1336483007929868290', '2', '女', '1303247362688376833');

-- ----------------------------
-- Table structure for standard_dict
-- ----------------------------
DROP TABLE IF EXISTS `standard_dict`;
CREATE TABLE `standard_dict`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `type_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属类别',
  `gb_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标准编码',
  `gb_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标准名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据标准字典表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of standard_dict
-- ----------------------------
INSERT INTO `standard_dict` VALUES ('1303247357105758209', 1, '1214835832967581698', '2020-09-08 16:16:38', '1197789917762031617', '1214835832967581698', '2020-09-08 16:16:38', NULL, '1303245849463218178', '0', '未知的性别');
INSERT INTO `standard_dict` VALUES ('1303247360368926722', 1, '1214835832967581698', '2020-09-08 16:16:38', '1197789917762031617', '1214835832967581698', '2020-09-08 16:16:38', NULL, '1303245849463218178', '1', '男性');
INSERT INTO `standard_dict` VALUES ('1303247362688376833', 1, '1214835832967581698', '2020-09-08 16:16:38', '1197789917762031617', '1214835832967581698', '2020-09-08 16:16:38', NULL, '1303245849463218178', '2', '女性');
INSERT INTO `standard_dict` VALUES ('1303247366693937153', 1, '1214835832967581698', '2020-09-08 16:16:38', '1197789917762031617', '1214835832967581698', '2020-09-08 16:16:38', NULL, '1303245849463218178', '9', '未说明的性别');
INSERT INTO `standard_dict` VALUES ('1303249289220210689', 1, '1214835832967581698', '2020-09-08 16:17:02', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:02', NULL, '1303245946938843137', '10', '未婚');
INSERT INTO `standard_dict` VALUES ('1303249292659539970', 1, '1214835832967581698', '2020-09-08 16:17:02', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:02', NULL, '1303245946938843137', '20', '已婚');
INSERT INTO `standard_dict` VALUES ('1303249295721381890', 1, '1214835832967581698', '2020-09-08 16:17:02', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:02', NULL, '1303245946938843137', '21', '初婚');
INSERT INTO `standard_dict` VALUES ('1303249298619645953', 1, '1214835832967581698', '2020-09-08 16:17:02', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:02', NULL, '1303245946938843137', '22', '再婚');
INSERT INTO `standard_dict` VALUES ('1303249302188998658', 1, '1214835832967581698', '2020-09-08 16:17:02', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:02', NULL, '1303245946938843137', '23', '复婚');
INSERT INTO `standard_dict` VALUES ('1303249306152615937', 1, '1214835832967581698', '2020-09-08 16:17:02', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:02', NULL, '1303245946938843137', '30', '丧偶');
INSERT INTO `standard_dict` VALUES ('1303249308417540097', 1, '1214835832967581698', '2020-09-08 16:17:02', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:02', NULL, '1303245946938843137', '40', '离婚');
INSERT INTO `standard_dict` VALUES ('1303249312116916225', 1, '1214835832967581698', '2020-09-08 16:17:02', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:02', NULL, '1303245946938843137', '90', '未说明的婚姻状况');
INSERT INTO `standard_dict` VALUES ('1303250886239223810', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '11', '国家公务员');
INSERT INTO `standard_dict` VALUES ('1303250889280094210', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '13', '国家公务员');
INSERT INTO `standard_dict` VALUES ('1303250891419189250', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '17', '职员');
INSERT INTO `standard_dict` VALUES ('1303250895265366018', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '21', '企业管理人员');
INSERT INTO `standard_dict` VALUES ('1303250898415288322', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '24', '工 人');
INSERT INTO `standard_dict` VALUES ('1303250902022389761', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '27', '农民');
INSERT INTO `standard_dict` VALUES ('1303250904572526594', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '31', '学生');
INSERT INTO `standard_dict` VALUES ('1303250907172995074', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '37', '现役军人');
INSERT INTO `standard_dict` VALUES ('1303250910394220545', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '51', '自由职业者');
INSERT INTO `standard_dict` VALUES ('1303250914454306817', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '54', '个体经营者');
INSERT INTO `standard_dict` VALUES ('1303250918308872194', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '70', '无业 人员');
INSERT INTO `standard_dict` VALUES ('1303250920888369153', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '80', '退（离）休人员');
INSERT INTO `standard_dict` VALUES ('1303250924432556033', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, '1303246143370682369', '90', '其他');
INSERT INTO `standard_dict` VALUES ('1303252645292556289', 1, '1214835832967581698', '2020-09-08 16:18:13', '1197789917762031617', '1214835832967581698', '2020-09-08 16:18:13', NULL, '1303246245158051841', '1', '香港同胞亲属');
INSERT INTO `standard_dict` VALUES ('1303252649616883713', 1, '1214835832967581698', '2020-09-08 16:18:13', '1197789917762031617', '1214835832967581698', '2020-09-08 16:18:13', NULL, '1303246245158051841', '2', '澳门同胞亲属');
INSERT INTO `standard_dict` VALUES ('1303252652871663617', 1, '1214835832967581698', '2020-09-08 16:18:13', '1197789917762031617', '1214835832967581698', '2020-09-08 16:18:13', NULL, '1303246245158051841', '3', '台湾同胞亲属');
INSERT INTO `standard_dict` VALUES ('1303252656952721409', 1, '1214835832967581698', '2020-09-08 16:18:13', '1197789917762031617', '1214835832967581698', '2020-09-08 16:18:13', NULL, '1303246245158051841', '4', '海外侨胞亲属');
INSERT INTO `standard_dict` VALUES ('1303253227483033601', 1, '1214835832967581698', '2020-09-08 16:18:50', '1197789917762031617', '1214835832967581698', '2020-09-08 16:18:50', NULL, '1303246401513316353', '1', '两院院士');
INSERT INTO `standard_dict` VALUES ('1303253232226791425', 1, '1214835832967581698', '2020-09-08 16:18:50', '1197789917762031617', '1214835832967581698', '2020-09-08 16:18:50', NULL, '1303246401513316353', '2', '中国科学院院士');
INSERT INTO `standard_dict` VALUES ('1303253234995032066', 1, '1214835832967581698', '2020-09-08 16:18:50', '1197789917762031617', '1214835832967581698', '2020-09-08 16:18:50', NULL, '1303246401513316353', '3', '中国工程院院士');

-- ----------------------------
-- Table structure for standard_type
-- ----------------------------
DROP TABLE IF EXISTS `standard_type`;
CREATE TABLE `standard_type`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `gb_type_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标准类别编码',
  `gb_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标准类别名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据标准类别表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of standard_type
-- ----------------------------
INSERT INTO `standard_type` VALUES ('1303245849463218178', 1, '1214835832967581698', '2020-09-08 16:16:38', '1197789917762031617', '1214835832967581698', '2020-09-08 16:16:38', NULL, 'GB/T 2261.1-2003', '人的性别代码');
INSERT INTO `standard_type` VALUES ('1303245946938843137', 1, '1214835832967581698', '2020-09-08 16:17:02', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:02', NULL, 'GB/T 2261.2-2003', '婚姻状况代码');
INSERT INTO `standard_type` VALUES ('1303246143370682369', 1, '1214835832967581698', '2020-09-08 16:17:48', '1197789917762031617', '1214835832967581698', '2020-09-08 16:17:48', NULL, 'GB/T 2261.4-2003', '从业状况(个人身份)代码');
INSERT INTO `standard_type` VALUES ('1303246245158051841', 1, '1214835832967581698', '2020-09-08 16:18:13', '1197789917762031617', '1214835832967581698', '2020-09-08 16:18:13', NULL, 'GB/T 2261.5-2003', '港澳台侨属代码');
INSERT INTO `standard_type` VALUES ('1303246401513316353', 1, '1214835832967581698', '2020-09-08 16:18:50', '1197789917762031617', '1214835832967581698', '2020-09-08 16:18:50', NULL, 'GB/T 2261.7-2003', '院士代码');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `config_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参数名称',
  `config_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参数键名',
  `config_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参数键值',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统参数配置信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES ('1265635179754459137', 1, '1214835832967581698', '2020-05-27 21:25:16', '1214835832967581698', '2020-07-06 10:47:20', '', '初始化密码', 'sys.user.password', '123456');

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `parent_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '父部门ID',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门名称',
  `dept_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门编码（数据权限）',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES ('1197789917762031617', '0', 'xx科技', '10', 1, '1', '2019-11-22 16:12:25', '1214835832967581698', '2021-01-05 21:29:07', '1');
INSERT INTO `sys_dept` VALUES ('1197790192543469570', '1197789917762031617', '研发部门', '10001', 1, '1', '2019-11-22 16:13:30', '1', '2019-11-22 16:13:30', NULL);
INSERT INTO `sys_dept` VALUES ('1197790560782389250', '1197789917762031617', '市场部门', '10002', 1, '1', '2019-11-22 16:14:58', '1', '2019-11-22 16:14:58', NULL);

-- ----------------------------
-- Table structure for sys_dept_relation
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept_relation`;
CREATE TABLE `sys_dept_relation`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门关系主键ID',
  `ancestor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '祖先节点',
  `descendant` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '后代节点',
  PRIMARY KEY (`id`, `ancestor`, `descendant`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dept_relation
-- ----------------------------
INSERT INTO `sys_dept_relation` VALUES ('1197790192677687298', '1197790192543469570', '1197790192543469570');
INSERT INTO `sys_dept_relation` VALUES ('1197790560828526593', '1197790560782389250', '1197790560782389250');
INSERT INTO `sys_dept_relation` VALUES ('1346448644767150081', '1197789917762031617', '1197789917762031617');

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `dict_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典名称',
  `dict_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典编码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典编码信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES ('1254701165563764738', 1, '1214835832967581698', '2020-04-27 17:17:24', '1214835832967581698', '2020-04-27 17:17:24', NULL, '菜单类型', 'sys_menu_type');
INSERT INTO `sys_dict` VALUES ('1254701402755850241', 1, '1214835832967581698', '2020-04-27 17:18:20', '1214835832967581698', '2020-04-27 17:18:20', NULL, '数据状态', 'sys_common_status');
INSERT INTO `sys_dict` VALUES ('1255033722255945729', 1, '1214835832967581698', '2020-04-28 15:18:51', '1214835832967581698', '2020-04-28 15:18:51', NULL, '数据范围', 'sys_data_scope');
INSERT INTO `sys_dict` VALUES ('1255037349741703169', 1, '1214835832967581698', '2020-04-28 15:33:16', '1214835832967581698', '2020-04-28 15:33:16', NULL, '数据库类型', 'data_db_type');
INSERT INTO `sys_dict` VALUES ('1255047550985297922', 1, '1214835832967581698', '2020-04-28 16:13:48', '1214835832967581698', '2020-04-28 16:13:48', NULL, '是否', 'sys_yes_no');
INSERT INTO `sys_dict` VALUES ('1255047909942222850', 1, '1214835832967581698', '2020-04-28 16:15:14', '1214835832967581698', '2020-04-28 16:15:14', NULL, '请求方式', 'data_req_method');
INSERT INTO `sys_dict` VALUES ('1255048146102509569', 1, '1214835832967581698', '2020-04-28 16:16:10', '1214835832967581698', '2020-04-28 16:16:10', NULL, '返回格式', 'data_res_type');
INSERT INTO `sys_dict` VALUES ('1255049472299491329', 1, '1214835832967581698', '2020-04-28 16:21:27', '1214835832967581698', '2020-04-28 16:21:27', NULL, '配置方式', 'data_config_type');
INSERT INTO `sys_dict` VALUES ('1255049868610887682', 1, '1214835832967581698', '2020-04-28 16:23:01', '1214835832967581698', '2020-04-28 16:23:01', NULL, 'SQL操作符', 'data_where_type');
INSERT INTO `sys_dict` VALUES ('1255050897825980418', 1, '1214835832967581698', '2020-04-28 16:27:06', '1214835832967581698', '2020-04-28 16:27:06', NULL, '参数类型', 'data_param_type');
INSERT INTO `sys_dict` VALUES ('1255052030422278145', 1, '1214835832967581698', '2020-04-28 16:31:36', '1214835832967581698', '2020-04-28 16:31:36', NULL, '脱敏类型', 'data_cipher_type');
INSERT INTO `sys_dict` VALUES ('1255054338933645314', 1, '1214835832967581698', '2020-04-28 16:40:47', '1214835832967581698', '2020-04-28 16:40:47', NULL, '正则规则类型', 'data_regex_crypto');
INSERT INTO `sys_dict` VALUES ('1255054468176928770', 1, '1214835832967581698', '2020-04-28 16:41:18', '1214835832967581698', '2020-04-28 16:41:18', NULL, '加密规则类型', 'data_algorithm_crypto');
INSERT INTO `sys_dict` VALUES ('1275048574979174401', 1, '1214835832967581698', '2020-06-22 20:50:44', '1214835832967581698', '2020-06-22 20:50:44', NULL, '任务状态', 'sys_job_status');
INSERT INTO `sys_dict` VALUES ('1275054601837506561', 1, '1214835832967581698', '2020-06-22 21:14:41', '1214835832967581698', '2020-06-22 21:14:41', NULL, '系统状态', 'sys_normal_status');
INSERT INTO `sys_dict` VALUES ('1280793187027292161', 1, '1214835832967581698', '2020-07-08 17:17:46', '1214835832967581698', '2020-07-08 17:17:46', NULL, 'API状态', 'data_api_status');
INSERT INTO `sys_dict` VALUES ('1285923703451848705', 1, '1214835832967581698', '2020-07-22 21:04:37', '1214835832967581698', '2020-07-22 21:04:37', NULL, '聚合函数', 'data_aggregate_type');
INSERT INTO `sys_dict` VALUES ('1296680107225706498', 1, '1296680107225706498', '2020-08-21 21:04:37', '1214835832967581698', '2020-08-21 21:04:37', NULL, '服务类型', 'data_service_type');
INSERT INTO `sys_dict` VALUES ('1300344099387244546', 1, '1296680107225706498', '2020-08-21 21:04:37', '1214835832967581698', '2020-08-21 21:04:37', NULL, '查询方式', 'data_query_type');
INSERT INTO `sys_dict` VALUES ('1300344451876577281', 1, '1296680107225706498', '2020-08-21 21:04:37', '1214835832967581698', '2020-08-21 21:04:37', NULL, '显示类型', 'data_html_type');
INSERT INTO `sys_dict` VALUES ('1300708138781016066', 1, '1296680107225706498', '2020-08-21 21:04:37', '1214835832967581698', '2020-08-21 21:04:37', NULL, 'ORACLE数据类型', 'data_type_oracle');
INSERT INTO `sys_dict` VALUES ('1301041843055632386', 1, '1296680107225706498', '2020-08-21 21:04:37', '1214835832967581698', '2020-08-21 21:04:37', NULL, 'MYSQL数据类型', 'data_type_mysql');
INSERT INTO `sys_dict` VALUES ('1309001146932670465', 1, '1296680107225706498', '2020-08-21 21:04:37', '1214835832967581698', '2020-08-21 21:04:37', NULL, '流程状态', 'sys_flow_status');
INSERT INTO `sys_dict` VALUES ('1310494826919211009', 1, '1296680107225706498', '2020-08-21 21:04:37', '1214835832967581698', '2020-08-21 21:04:37', NULL, '字典对照状态', 'data_contrast_status');

-- ----------------------------
-- Table structure for sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `dict_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典id',
  `item_text` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典项文本',
  `item_value` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典项值',
  `item_sort` int(11) NULL DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典项信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict_item
-- ----------------------------
INSERT INTO `sys_dict_item` VALUES ('1254702149128052737', 1, '1214835832967581698', '2020-04-27 17:21:18', '1214835832967581698', '2020-04-27 17:21:18', NULL, '1254701165563764738', '0', '模块', 1);
INSERT INTO `sys_dict_item` VALUES ('1254702177166974977', 1, '1214835832967581698', '2020-04-27 17:21:25', '1214835832967581698', '2020-04-27 17:21:25', NULL, '1254701165563764738', '1', '菜单', 2);
INSERT INTO `sys_dict_item` VALUES ('1254702210272616449', 1, '1214835832967581698', '2020-04-27 17:21:33', '1214835832967581698', '2020-04-27 17:21:33', NULL, '1254701165563764738', '2', '按钮', 3);
INSERT INTO `sys_dict_item` VALUES ('1254702351834570753', 1, '1214835832967581698', '2020-04-27 17:22:07', '1214835832967581698', '2020-04-27 17:22:07', NULL, '1254701402755850241', '0', '禁用', 1);
INSERT INTO `sys_dict_item` VALUES ('1254702385279950850', 1, '1214835832967581698', '2020-04-27 17:22:15', '1214835832967581698', '2020-04-27 17:22:15', NULL, '1254701402755850241', '1', '启用', 2);
INSERT INTO `sys_dict_item` VALUES ('1255034071784075266', 1, '1214835832967581698', '2020-04-28 15:20:15', '1214835832967581698', '2020-04-28 15:20:15', NULL, '1255033722255945729', '1', '全部数据权限', 1);
INSERT INTO `sys_dict_item` VALUES ('1255034118043054082', 1, '1214835832967581698', '2020-04-28 15:20:26', '1214835832967581698', '2020-04-28 15:20:26', NULL, '1255033722255945729', '2', '自定义数据权限', 2);
INSERT INTO `sys_dict_item` VALUES ('1255034148703416321', 1, '1214835832967581698', '2020-04-28 15:20:33', '1214835832967581698', '2020-04-28 15:20:33', NULL, '1255033722255945729', '3', '本部门数据权限', 3);
INSERT INTO `sys_dict_item` VALUES ('1255034178491363329', 1, '1214835832967581698', '2020-04-28 15:20:40', '1214835832967581698', '2020-04-28 15:20:40', NULL, '1255033722255945729', '4', '本部门及以下数据权限', 4);
INSERT INTO `sys_dict_item` VALUES ('1255034207415283713', 1, '1214835832967581698', '2020-04-28 15:20:47', '1214835832967581698', '2020-04-28 15:20:47', NULL, '1255033722255945729', '5', '仅本人数据权限', 5);
INSERT INTO `sys_dict_item` VALUES ('1255037454632857602', 1, '1214835832967581698', '2020-04-28 15:33:41', '1214835832967581698', '2020-04-28 15:33:41', NULL, '1255037349741703169', '1', 'MySql数据库', 1);
INSERT INTO `sys_dict_item` VALUES ('1255037499587407874', 1, '1214835832967581698', '2020-04-28 15:33:52', '1214835832967581698', '2020-04-28 15:33:52', NULL, '1255037349741703169', '2', 'MariaDB数据库', 2);
INSERT INTO `sys_dict_item` VALUES ('1255037543732457474', 1, '1214835832967581698', '2020-04-28 15:34:03', '1214835832967581698', '2020-04-28 15:34:03', NULL, '1255037349741703169', '3', 'Oracle11g及以下数据库', 3);
INSERT INTO `sys_dict_item` VALUES ('1255037611558547458', 1, '1214835832967581698', '2020-04-28 15:34:19', '1214835832967581698', '2020-04-28 15:34:19', NULL, '1255037349741703169', '4', 'Oracle12c+数据库', 4);
INSERT INTO `sys_dict_item` VALUES ('1255037682886881282', 1, '1214835832967581698', '2020-04-28 15:34:36', '1214835832967581698', '2020-04-28 15:34:36', NULL, '1255037349741703169', '5', 'PostgreSql数据库', 5);
INSERT INTO `sys_dict_item` VALUES ('1255037722741157890', 1, '1214835832967581698', '2020-04-28 15:34:45', '1214835832967581698', '2020-04-28 15:34:45', NULL, '1255037349741703169', '6', 'SQLServer2008及以下数据库', 6);
INSERT INTO `sys_dict_item` VALUES ('1255037772984725506', 1, '1214835832967581698', '2020-04-28 15:34:57', '1214835832967581698', '2020-04-28 15:34:57', NULL, '1255037349741703169', '7', 'SQLServer2012+数据库', 7);
INSERT INTO `sys_dict_item` VALUES ('1255037816378994690', 1, '1214835832967581698', '2020-04-28 15:35:08', '1214835832967581698', '2020-04-28 15:35:08', NULL, '1255037349741703169', '8', '其他数据库', 8);
INSERT INTO `sys_dict_item` VALUES ('1255047584040607746', 1, '1214835832967581698', '2020-04-28 16:13:56', '1214835832967581698', '2020-04-28 16:13:56', NULL, '1255047550985297922', '0', '否', 1);
INSERT INTO `sys_dict_item` VALUES ('1255047616940728322', 1, '1214835832967581698', '2020-04-28 16:14:04', '1214835832967581698', '2020-04-28 16:14:04', NULL, '1255047550985297922', '1', '是', 2);
INSERT INTO `sys_dict_item` VALUES ('1255048026803920898', 1, '1214835832967581698', '2020-04-28 16:15:42', '1214835832967581698', '2020-04-28 16:15:42', NULL, '1255047909942222850', 'GET', 'GET', 1);
INSERT INTO `sys_dict_item` VALUES ('1255048059242668033', 1, '1214835832967581698', '2020-04-28 16:15:50', '1214835832967581698', '2020-04-28 16:15:50', NULL, '1255047909942222850', 'POST', 'POST', 2);
INSERT INTO `sys_dict_item` VALUES ('1255048227744636929', 1, '1214835832967581698', '2020-04-28 16:16:30', '1214835832967581698', '2020-04-28 16:16:30', NULL, '1255048146102509569', 'JSON', 'JSON', 1);
INSERT INTO `sys_dict_item` VALUES ('1255049535197274113', 1, '1214835832967581698', '2020-04-28 16:21:42', '1214835832967581698', '2020-04-28 16:21:42', NULL, '1255049472299491329', '1', '表引导模式', 1);
INSERT INTO `sys_dict_item` VALUES ('1255049562602856449', 1, '1214835832967581698', '2020-04-28 16:21:48', '1214835832967581698', '2020-04-28 16:21:48', NULL, '1255049472299491329', '2', '脚本模式', 2);
INSERT INTO `sys_dict_item` VALUES ('1255049937749794817', 1, '1214835832967581698', '2020-04-28 16:23:18', '1214835832967581698', '2020-04-28 16:23:18', NULL, '1255049868610887682', '1', '等于', 1);
INSERT INTO `sys_dict_item` VALUES ('1255049969106411521', 1, '1214835832967581698', '2020-04-28 16:23:25', '1214835832967581698', '2020-04-28 16:23:25', NULL, '1255049868610887682', '2', '不等于', 2);
INSERT INTO `sys_dict_item` VALUES ('1255049996876898306', 1, '1214835832967581698', '2020-04-28 16:23:32', '1214835832967581698', '2020-04-28 16:23:32', NULL, '1255049868610887682', '3', '全模糊查询', 3);
INSERT INTO `sys_dict_item` VALUES ('1255050281636585473', 1, '1214835832967581698', '2020-04-28 16:24:40', '1214835832967581698', '2020-04-28 16:24:40', NULL, '1255049868610887682', '4', '左模糊查询', 4);
INSERT INTO `sys_dict_item` VALUES ('1255050325618057217', 1, '1214835832967581698', '2020-04-28 16:24:50', '1214835832967581698', '2020-04-28 16:24:50', NULL, '1255049868610887682', '5', '右模糊查询', 5);
INSERT INTO `sys_dict_item` VALUES ('1255050357075337217', 1, '1214835832967581698', '2020-04-28 16:24:57', '1214835832967581698', '2020-04-28 16:24:57', NULL, '1255049868610887682', '6', '大于', 6);
INSERT INTO `sys_dict_item` VALUES ('1255050386712289281', 1, '1214835832967581698', '2020-04-28 16:25:05', '1214835832967581698', '2020-04-28 16:25:05', NULL, '1255049868610887682', '7', '大于等于', 7);
INSERT INTO `sys_dict_item` VALUES ('1255050425413132290', 1, '1214835832967581698', '2020-04-28 16:25:14', '1214835832967581698', '2020-04-28 16:25:14', NULL, '1255049868610887682', '8', '小于', 8);
INSERT INTO `sys_dict_item` VALUES ('1255050459407966210', 1, '1214835832967581698', '2020-04-28 16:25:22', '1214835832967581698', '2020-04-28 16:25:22', NULL, '1255049868610887682', '9', '小于等于', 9);
INSERT INTO `sys_dict_item` VALUES ('1255050508485517313', 1, '1214835832967581698', '2020-04-28 16:25:34', '1214835832967581698', '2020-04-28 16:25:34', NULL, '1255049868610887682', '10', '是否为空', 10);
INSERT INTO `sys_dict_item` VALUES ('1255050549505810433', 1, '1214835832967581698', '2020-04-28 16:25:43', '1214835832967581698', '2020-04-28 16:25:43', NULL, '1255049868610887682', '11', '是否不为空', 11);
INSERT INTO `sys_dict_item` VALUES ('1255050756901560321', 1, '1214835832967581698', '2020-04-28 16:26:33', '1214835832967581698', '2020-04-28 16:26:33', NULL, '1255049868610887682', '12', 'IN', 12);
INSERT INTO `sys_dict_item` VALUES ('1255051004805898241', 1, '1214835832967581698', '2020-04-28 16:27:32', '1214835832967581698', '2020-04-28 16:27:32', NULL, '1255050897825980418', '1', '字符串', 1);
INSERT INTO `sys_dict_item` VALUES ('1255051030818971649', 1, '1214835832967581698', '2020-04-28 16:27:38', '1214835832967581698', '2020-04-28 16:27:38', NULL, '1255050897825980418', '2', '整型', 2);
INSERT INTO `sys_dict_item` VALUES ('1255051062423052289', 1, '1214835832967581698', '2020-04-28 16:27:46', '1214835832967581698', '2020-04-28 16:27:46', NULL, '1255050897825980418', '3', '浮点型', 3);
INSERT INTO `sys_dict_item` VALUES ('1255051089870577665', 1, '1214835832967581698', '2020-04-28 16:27:52', '1214835832967581698', '2020-04-28 16:27:52', NULL, '1255050897825980418', '4', '时间', 4);
INSERT INTO `sys_dict_item` VALUES ('1255051121646624770', 1, '1214835832967581698', '2020-04-28 16:28:00', '1214835832967581698', '2020-04-28 16:28:00', NULL, '1255050897825980418', '5', '集合', 5);
INSERT INTO `sys_dict_item` VALUES ('1255052103847763970', 1, '1214835832967581698', '2020-04-28 16:31:54', '1214835832967581698', '2020-04-28 16:31:54', NULL, '1255052030422278145', '1', '正则替换', 1);
INSERT INTO `sys_dict_item` VALUES ('1255052128799678465', 1, '1214835832967581698', '2020-04-28 16:32:00', '1214835832967581698', '2020-04-28 16:32:00', NULL, '1255052030422278145', '2', '加密算法', 2);
INSERT INTO `sys_dict_item` VALUES ('1255054729293324290', 1, '1214835832967581698', '2020-04-28 16:42:20', '1214835832967581698', '2020-04-28 16:42:20', NULL, '1255054338933645314', '1', '中文姓名', 1);
INSERT INTO `sys_dict_item` VALUES ('1255054769277624322', 1, '1214835832967581698', '2020-04-28 16:42:29', '1214835832967581698', '2020-04-28 16:42:29', NULL, '1255054338933645314', '2', '身份证号', 2);
INSERT INTO `sys_dict_item` VALUES ('1255054810838982657', 1, '1214835832967581698', '2020-04-28 16:42:39', '1214835832967581698', '2020-04-28 16:42:39', NULL, '1255054338933645314', '3', '固定电话', 3);
INSERT INTO `sys_dict_item` VALUES ('1255054840111030274', 1, '1214835832967581698', '2020-04-28 16:42:46', '1214835832967581698', '2020-04-28 16:42:46', NULL, '1255054338933645314', '4', '手机号码', 4);
INSERT INTO `sys_dict_item` VALUES ('1255054878862204929', 1, '1214835832967581698', '2020-04-28 16:42:56', '1214835832967581698', '2020-04-28 16:42:56', NULL, '1255054338933645314', '5', '地址', 5);
INSERT INTO `sys_dict_item` VALUES ('1255054911183511553', 1, '1214835832967581698', '2020-04-28 16:43:03', '1214835832967581698', '2020-04-28 16:43:03', NULL, '1255054338933645314', '6', '电子邮箱', 6);
INSERT INTO `sys_dict_item` VALUES ('1255054941030178817', 1, '1214835832967581698', '2020-04-28 16:43:10', '1214835832967581698', '2020-04-28 16:43:10', NULL, '1255054338933645314', '7', '银行卡号', 7);
INSERT INTO `sys_dict_item` VALUES ('1255054975704489986', 1, '1214835832967581698', '2020-04-28 16:43:19', '1214835832967581698', '2020-04-28 16:43:19', NULL, '1255054338933645314', '8', '公司开户银行联号', 8);
INSERT INTO `sys_dict_item` VALUES ('1255055043568328706', 1, '1214835832967581698', '2020-04-28 16:43:35', '1214835832967581698', '2020-04-28 16:43:35', NULL, '1255054468176928770', '1', 'BASE64加密', 1);
INSERT INTO `sys_dict_item` VALUES ('1255055072123150338', 1, '1214835832967581698', '2020-04-28 16:43:42', '1214835832967581698', '2020-04-28 16:43:42', NULL, '1255054468176928770', '2', 'MD5加密', 2);
INSERT INTO `sys_dict_item` VALUES ('1255055103777562626', 1, '1214835832967581698', '2020-04-28 16:43:49', '1214835832967581698', '2020-04-28 16:43:49', NULL, '1255054468176928770', '3', 'SHA_1加密', 3);
INSERT INTO `sys_dict_item` VALUES ('1255055137550098434', 1, '1214835832967581698', '2020-04-28 16:43:57', '1214835832967581698', '2020-04-28 16:43:57', NULL, '1255054468176928770', '4', 'SHA_256加密', 4);
INSERT INTO `sys_dict_item` VALUES ('1255055168852189186', 1, '1214835832967581698', '2020-04-28 16:44:05', '1214835832967581698', '2020-04-28 16:44:05', NULL, '1255054468176928770', '5', 'AES加密', 5);
INSERT INTO `sys_dict_item` VALUES ('1255055201391599617', 1, '1214835832967581698', '2020-04-28 16:44:12', '1214835832967581698', '2020-04-28 16:44:12', NULL, '1255054468176928770', '6', 'DES加密', 6);
INSERT INTO `sys_dict_item` VALUES ('1275048742365458434', 1, '1214835832967581698', '2020-06-22 20:51:24', '1214835832967581698', '2020-06-22 20:51:24', NULL, '1275048574979174401', '0', '暂停', 1);
INSERT INTO `sys_dict_item` VALUES ('1275048809193304065', 1, '1214835832967581698', '2020-06-22 20:51:40', '1214835832967581698', '2020-06-22 20:51:40', NULL, '1275048574979174401', '1', '运行', 2);
INSERT INTO `sys_dict_item` VALUES ('1275054736508219394', 1, '1214835832967581698', '2020-06-22 21:15:13', '1214835832967581698', '2020-06-22 21:15:13', NULL, '1275054601837506561', '0', '失败', 1);
INSERT INTO `sys_dict_item` VALUES ('1275054803906490370', 1, '1214835832967581698', '2020-06-22 21:15:29', '1214835832967581698', '2020-06-22 21:15:29', NULL, '1275054601837506561', '1', '成功', 2);
INSERT INTO `sys_dict_item` VALUES ('1280793322234875905', 1, '1214835832967581698', '2020-07-08 17:18:19', '1214835832967581698', '2020-07-08 17:18:19', NULL, '1280793187027292161', '1', '待发布', 1);
INSERT INTO `sys_dict_item` VALUES ('1280793374244245505', 1, '1214835832967581698', '2020-07-08 17:18:31', '1214835832967581698', '2020-07-08 17:18:31', NULL, '1280793187027292161', '2', '已发布', 2);
INSERT INTO `sys_dict_item` VALUES ('1280793418611593218', 1, '1214835832967581698', '2020-07-08 17:18:42', '1214835832967581698', '2020-07-08 17:18:42', NULL, '1280793187027292161', '3', '已下线', 3);
INSERT INTO `sys_dict_item` VALUES ('1285924274212737026', 1, '1214835832967581698', '2020-07-22 21:06:53', '1214835832967581698', '2020-07-22 21:06:53', NULL, '1285923703451848705', 'AVG', '平均值', 1);
INSERT INTO `sys_dict_item` VALUES ('1285924403900616706', 1, '1214835832967581698', '2020-07-22 21:07:24', '1214835832967581698', '2020-07-22 21:07:24', NULL, '1285923703451848705', 'COUNT', '计数', 2);
INSERT INTO `sys_dict_item` VALUES ('1285924488742998018', 1, '1214835832967581698', '2020-07-22 21:07:44', '1214835832967581698', '2020-07-22 21:07:44', NULL, '1285923703451848705', 'MAX', '最大值', 3);
INSERT INTO `sys_dict_item` VALUES ('1285924564915752961', 1, '1214835832967581698', '2020-07-22 21:08:02', '1214835832967581698', '2020-07-22 21:08:02', NULL, '1285923703451848705', 'MIN', '最小值', 4);
INSERT INTO `sys_dict_item` VALUES ('1285924644037103617', 1, '1214835832967581698', '2020-07-22 21:08:21', '1214835832967581698', '2020-07-22 21:08:21', NULL, '1285923703451848705', 'SUM', '求和', 5);
INSERT INTO `sys_dict_item` VALUES ('1296680479872815106', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1296680107225706498', '1', 'http接口', 1);
INSERT INTO `sys_dict_item` VALUES ('1296680800095338497', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1296680107225706498', '2', 'webservice接口', 2);
INSERT INTO `sys_dict_item` VALUES ('1300344676871569410', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344099387244546', 'eq', '=', 1);
INSERT INTO `sys_dict_item` VALUES ('1300344719984926721', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344099387244546', 'ne', '!=', 2);
INSERT INTO `sys_dict_item` VALUES ('1300344887987683330', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344099387244546', 'gt', '>', 3);
INSERT INTO `sys_dict_item` VALUES ('1300344940169011202', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344099387244546', 'ge', '>=', 4);
INSERT INTO `sys_dict_item` VALUES ('1300344991276687361', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344099387244546', 'lt', '<', 5);
INSERT INTO `sys_dict_item` VALUES ('1300345039674744833', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344099387244546', 'le', '<=', 6);
INSERT INTO `sys_dict_item` VALUES ('1300345083937226754', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344099387244546', 'like', 'like', 7);
INSERT INTO `sys_dict_item` VALUES ('1300345183094763522', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344099387244546', 'between', 'between', 8);
INSERT INTO `sys_dict_item` VALUES ('1300345958537633794', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344451876577281', 'input', '文本框', 1);
INSERT INTO `sys_dict_item` VALUES ('1300345964648734722', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344451876577281', 'textarea', '文本域', 2);
INSERT INTO `sys_dict_item` VALUES ('1300345968855621633', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344451876577281', 'select', '下拉框', 3);
INSERT INTO `sys_dict_item` VALUES ('1300345973049925633', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344451876577281', 'radio', '单选框', 4);
INSERT INTO `sys_dict_item` VALUES ('1300345977248423937', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344451876577281', 'checkbox', '复选框', 5);
INSERT INTO `sys_dict_item` VALUES ('1300345981438533633', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344451876577281', 'datetime', '日期控件', 6);
INSERT INTO `sys_dict_item` VALUES ('1300708143558328322', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300708138781016066', 'char', '字符串', 1);
INSERT INTO `sys_dict_item` VALUES ('1300708156141240321', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300708138781016066', 'number', '数值', 2);
INSERT INTO `sys_dict_item` VALUES ('1300708160343932930', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300708138781016066', 'date', '日期', 3);
INSERT INTO `sys_dict_item` VALUES ('1300708164542431234', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300708138781016066', 'clob', '长文本', 4);
INSERT INTO `sys_dict_item` VALUES ('1301041851154833410', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'tinyint', 'tinyint整型', 1);
INSERT INTO `sys_dict_item` VALUES ('1301041854644494338', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'int', 'int整型', 2);
INSERT INTO `sys_dict_item` VALUES ('1301041857957994497', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'bigint', 'bigint整型', 3);
INSERT INTO `sys_dict_item` VALUES ('1301041860990476290', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'float', '单精度', 4);
INSERT INTO `sys_dict_item` VALUES ('1301041864538857474', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'double', '双精度', 5);
INSERT INTO `sys_dict_item` VALUES ('1301041867428732929', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'decimal', '定点数', 6);
INSERT INTO `sys_dict_item` VALUES ('1301041870465409025', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'char', '定长字符串', 7);
INSERT INTO `sys_dict_item` VALUES ('1301041873263009793', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'varchar', '变长字符串', 8);
INSERT INTO `sys_dict_item` VALUES ('1301041875733454849', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'text', '长文本', 9);
INSERT INTO `sys_dict_item` VALUES ('1301041878837239810', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'date', 'date日期', 10);
INSERT INTO `sys_dict_item` VALUES ('1301041882624696322', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'time', 'time日期', 11);
INSERT INTO `sys_dict_item` VALUES ('1301041884780568578', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'year', 'year日期', 12);
INSERT INTO `sys_dict_item` VALUES ('1301041887540420609', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1301041843055632386', 'datetime', 'datetime日期', 13);
INSERT INTO `sys_dict_item` VALUES ('1302079329039069185', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1300344451876577281', 'number', '数字值', 7);
INSERT INTO `sys_dict_item` VALUES ('1309001150548160514', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1309001146932670465', '1', '待提交', 1);
INSERT INTO `sys_dict_item` VALUES ('1309001154742464514', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1309001146932670465', '2', '已退回', 2);
INSERT INTO `sys_dict_item` VALUES ('1309001158517338114', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1309001146932670465', '3', '审核中', 3);
INSERT INTO `sys_dict_item` VALUES ('1309001162443206658', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1309001146932670465', '4', '通过', 4);
INSERT INTO `sys_dict_item` VALUES ('1309001165593128962', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1309001146932670465', '5', '不通过', 5);
INSERT INTO `sys_dict_item` VALUES ('1309001167749001218', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1309001146932670465', '6', '已撤销', 6);
INSERT INTO `sys_dict_item` VALUES ('1310494837983784962', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1310494826919211009', '0', '未对照', 1);
INSERT INTO `sys_dict_item` VALUES ('1310494841284702210', 1, '1296680107225706498', '2020-08-21 21:04:37', '1296680107225706498', '2020-08-21 21:04:37', NULL, '1310494826919211009', '1', '已对照', 2);

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '日志主键ID',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属模块',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '日志标题',
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名称',
  `remote_addr` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求IP',
  `request_uri` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求URI',
  `class_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '方法类名',
  `method_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '方法名称',
  `params` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求参数',
  `time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求耗时',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '浏览器名称',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作系统',
  `ex_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '错误类型',
  `ex_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '日志信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_log
-- ----------------------------
INSERT INTO `sys_log` VALUES ('1265261406136143873', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '15', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 20:40:01');
INSERT INTO `sys_log` VALUES ('1265261962695118849', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '16', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 20:42:14');
INSERT INTO `sys_log` VALUES ('1265263362518913026', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '32', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 20:47:48');
INSERT INTO `sys_log` VALUES ('1265263766308753410', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '11', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 20:49:24');
INSERT INTO `sys_log` VALUES ('1265263890720198657', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '15', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 20:49:54');
INSERT INTO `sys_log` VALUES ('1265265416712851457', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '14', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 20:55:57');
INSERT INTO `sys_log` VALUES ('1265265809526198274', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '20', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 20:57:31');
INSERT INTO `sys_log` VALUES ('1265267017112457218', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '20', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:02:19');
INSERT INTO `sys_log` VALUES ('1265267219084972034', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '28', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:03:07');
INSERT INTO `sys_log` VALUES ('1265267267982168065', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '19', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:03:19');
INSERT INTO `sys_log` VALUES ('1265267809718472706', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '12', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:05:28');
INSERT INTO `sys_log` VALUES ('1265268643084734465', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '21', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:08:47');
INSERT INTO `sys_log` VALUES ('1265270421033422849', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '15', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:15:51');
INSERT INTO `sys_log` VALUES ('1265272879650541569', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '18', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:25:37');
INSERT INTO `sys_log` VALUES ('1265273650702028802', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '22', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:28:41');
INSERT INTO `sys_log` VALUES ('1265273816318316546', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '15', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:29:20');
INSERT INTO `sys_log` VALUES ('1265274458134908929', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '19', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:31:53');
INSERT INTO `sys_log` VALUES ('1265274502791663618', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '31', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:32:04');
INSERT INTO `sys_log` VALUES ('1265274557405696002', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '8', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:32:17');
INSERT INTO `sys_log` VALUES ('1265274576221343745', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '13', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:32:21');
INSERT INTO `sys_log` VALUES ('1265274629749051393', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '97', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:32:34');
INSERT INTO `sys_log` VALUES ('1265274945081020418', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1265274861165580290', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '17', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-26 21:33:49');
INSERT INTO `sys_log` VALUES ('1265627232303353857', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '14', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-27 20:53:41');
INSERT INTO `sys_log` VALUES ('1265632019614830594', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '17', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-27 21:12:42');
INSERT INTO `sys_log` VALUES ('1265637282912702465', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '26', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-27 21:33:37');
INSERT INTO `sys_log` VALUES ('1265640522375008258', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '19', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-27 21:46:30');
INSERT INTO `sys_log` VALUES ('1265976846751682562', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1265274861165580290', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '15', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:02:56');
INSERT INTO `sys_log` VALUES ('1265978978921926658', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1265274861165580290', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '16', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:11:24');
INSERT INTO `sys_log` VALUES ('1265979289124261890', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '18', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:12:38');
INSERT INTO `sys_log` VALUES ('1265979380824330242', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '24', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:13:00');
INSERT INTO `sys_log` VALUES ('1265980102571773954', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '19', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:15:52');
INSERT INTO `sys_log` VALUES ('1265980125481062402', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '31', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:15:57');
INSERT INTO `sys_log` VALUES ('1265980311502639105', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '29', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:16:42');
INSERT INTO `sys_log` VALUES ('1265980577702531074', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '21', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:17:45');
INSERT INTO `sys_log` VALUES ('1265981370732171265', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '14', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:20:54');
INSERT INTO `sys_log` VALUES ('1265981831040258049', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '13', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:22:44');
INSERT INTO `sys_log` VALUES ('1265981907770855426', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1265274861165580290', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '23', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:23:02');
INSERT INTO `sys_log` VALUES ('1265982263682715650', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1265274861165580290', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '19', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:24:27');
INSERT INTO `sys_log` VALUES ('1265982404875571202', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1265274861165580290', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '18', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:25:01');
INSERT INTO `sys_log` VALUES ('1265982486152794113', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1265274861165580290', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '34', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:25:20');
INSERT INTO `sys_log` VALUES ('1265982592616812545', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1265274861165580290', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '19', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-28 20:25:46');
INSERT INTO `sys_log` VALUES ('1266361799507185665', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '14', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-29 21:32:36');
INSERT INTO `sys_log` VALUES ('1267085228254515202', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '23', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-05-31 21:27:14');
INSERT INTO `sys_log` VALUES ('1272863883182260226', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '14', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-06-16 20:09:33');
INSERT INTO `sys_log` VALUES ('1272863961049513985', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '20', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-06-16 20:09:52');
INSERT INTO `sys_log` VALUES ('1273599081343668226', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '', '15', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-06-18 20:50:58');
INSERT INTO `sys_log` VALUES ('1278163103812476930', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '{\"id\":\"1214835832967581698\"}', '14', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-07-01 11:06:46');
INSERT INTO `sys_log` VALUES ('1278516062761086977', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '{\"id\":\"1214835832967581698\"}', '16', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-07-02 10:29:18');
INSERT INTO `sys_log` VALUES ('1278520069550338050', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.0.107', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '{\"id\":\"1214835832967581698\"}', '16', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-07-02 10:45:13');
INSERT INTO `sys_log` VALUES ('1319093199947554818', 'datax-service-system', '根据id获取用户详细信息', '1319084968579817473', 'ls', '192.168.2.187', '/users/1319084615276814337', 'cn.datax.service.system.controller.UserController', 'getUserById', '{\"id\":\"1319084615276814337\"}', '30', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-10-22 09:48:21');
INSERT INTO `sys_log` VALUES ('1321736221868789762', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.2.187', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '{\"id\":\"1214835832967581698\"}', '81', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-10-29 16:50:47');
INSERT INTO `sys_log` VALUES ('1321736248678780929', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.2.187', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '{\"id\":\"1214835832967581698\"}', '31', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-10-29 16:50:53');
INSERT INTO `sys_log` VALUES ('1321736734307880961', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.2.187', '/users/1214835832967581698', 'cn.datax.service.system.controller.UserController', 'getUserById', '{\"id\":\"1214835832967581698\"}', '34', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-10-29 16:52:49');
INSERT INTO `sys_log` VALUES ('1335762100828164097', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.30.11', '/users/1335761402136809473', 'cn.datax.service.system.controller.UserController', 'getUserById', '{\"id\":\"1335761402136809473\"}', '96', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-12-07 09:44:37');
INSERT INTO `sys_log` VALUES ('1335762124542758914', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.30.11', '/users/1335761402136809473', 'cn.datax.service.system.controller.UserController', 'getUserById', '{\"id\":\"1335761402136809473\"}', '103', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-12-07 09:44:42');
INSERT INTO `sys_log` VALUES ('1335762217010384897', 'datax-service-system', '根据id获取用户详细信息', '1214835832967581698', 'admin', '192.168.30.11', '/users/1335761402136809473', 'cn.datax.service.system.controller.UserController', 'getUserById', '{\"id\":\"1335761402136809473\"}', '102', 'Chrome', 'Windows 10 or Windows Server 2016', NULL, NULL, '2020-12-07 09:45:04');

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `op_os` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作系统',
  `op_browser` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '浏览器类型',
  `op_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录IP地址',
  `op_date` datetime(0) NULL DEFAULT NULL COMMENT '登录时间',
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录用户ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录用户名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '登录日志信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_login_log
-- ----------------------------
INSERT INTO `sys_login_log` VALUES ('1346331863834681345', 'Windows 10', 'Chrome 8', '0:0:0:0:0:0:0:1%0', '2021-01-05 13:45:05', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1346375822271246337', 'Windows 10', 'Chrome 8', '127.0.0.1', '2021-01-05 16:39:45', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1346377057556688898', 'Windows 10', 'Chrome 8', '0:0:0:0:0:0:0:1%0', '2021-01-05 16:44:40', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1346377788481269761', 'Windows 10', 'Chrome 8', '0:0:0:0:0:0:0:1%0', '2021-01-05 16:47:34', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1346386803638595585', 'Mac OS X', 'Chrome 8', '127.0.0.1', '2021-01-05 17:23:23', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1346388199251308545', 'Mac OS X', 'Chrome 8', '0:0:0:0:0:0:0:1%0', '2021-01-05 17:28:56', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1346770486128177154', 'Mac OS X', 'Chrome 8', '0:0:0:0:0:0:0:1%0', '2021-01-06 18:48:00', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1520306256294707202', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-04-30 15:37:12', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1520314505408544770', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-04-30 16:09:59', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1520349120684883969', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-04-30 18:27:32', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1520391758175797250', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-04-30 21:16:57', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1520928207765876737', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 08:48:37', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1520949413894270977', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 10:12:53', '1319084615276814337', 'zs');
INSERT INTO `sys_login_log` VALUES ('1520949456932024321', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 10:13:03', '1319084968579817473', 'ls');
INSERT INTO `sys_login_log` VALUES ('1520949578168381442', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 10:13:32', '1319093610569916418', 'zl');
INSERT INTO `sys_login_log` VALUES ('1520949867642466305', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 10:14:41', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1520952991333593089', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 10:27:06', '1319093610569916418', 'zl');
INSERT INTO `sys_login_log` VALUES ('1520954370093592577', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 10:32:34', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1520978448124780545', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 12:08:15', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1521127903100760066', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 22:02:08', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1521128937978163202', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 22:06:15', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1521129430188126209', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 22:08:12', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1521129474626777089', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 22:08:23', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1521130443435503618', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-02 22:12:14', '1214835832967581698', 'admin');
INSERT INTO `sys_login_log` VALUES ('1521160136205365250', 'Windows 10', 'Chrome 9', '127.0.0.1', '2022-05-03 00:10:13', '1214835832967581698', 'admin');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `parent_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '父资源ID',
  `menu_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '资源名称',
  `menu_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '对应路由path',
  `menu_component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '对应路由组件component',
  `menu_redirect` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '对应路由默认跳转地址redirect',
  `menu_perms` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识',
  `menu_icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图标',
  `menu_type` tinyint(4) NULL DEFAULT NULL COMMENT '类型（0模块，1菜单，2按钮）',
  `menu_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '资源编码',
  `menu_hidden` tinyint(4) NULL DEFAULT NULL COMMENT '资源隐藏（0否，1是）',
  `menu_sort` int(11) NULL DEFAULT NULL COMMENT '排序',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '资源权限信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES ('1323439314692685825', '0', '平台基础设置', '/basic', 'Layout', '/basic/index', NULL, 'form', 0, '10', 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439318413033473', '1323439314692685825', '看板', 'index', '/basic/index', NULL, NULL, 'form', 1, NULL, 1, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439321634258945', '1323439314692685825', '系统管理', 'system', '/basic/system/index', '/basic/system/post', NULL, 'form', 0, '1010', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439323769159681', '1323439321634258945', '岗位管理', 'post', '/basic/system/post/index', NULL, NULL, 'form', 1, '1011', 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439327296569346', '1323439323769159681', '岗位新增', NULL, NULL, NULL, 'system:post:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439329708294145', '1323439323769159681', '岗位修改', NULL, NULL, NULL, 'system:post:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439332476534786', '1323439323769159681', '岗位详情', NULL, NULL, NULL, 'system:post:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439336071053314', '1323439323769159681', '岗位删除', NULL, NULL, NULL, 'system:post:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439338231119873', '1323439321634258945', '部门管理', 'dept', '/basic/system/dept/index', NULL, NULL, 'form', 1, '1012', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439341569785858', '1323439338231119873', '部门新增', NULL, NULL, NULL, 'system:dept:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439344791011329', '1323439338231119873', '部门修改', NULL, NULL, NULL, 'system:dept:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439348771405826', '1323439338231119873', '部门详情', NULL, NULL, NULL, 'system:dept:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439351912939522', '1323439338231119873', '部门删除', NULL, NULL, NULL, 'system:dept:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439355318714370', '1323439321634258945', '菜单管理', 'menu', '/basic/system/menu/index', NULL, NULL, 'form', 1, '1013', 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439359127142401', '1323439355318714370', '菜单新增', NULL, NULL, NULL, 'system:menu:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439362759409666', '1323439355318714370', '菜单修改', NULL, NULL, NULL, 'system:menu:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439365372461057', '1323439355318714370', '菜单详情', NULL, NULL, NULL, 'system:menu:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439367612219394', '1323439355318714370', '菜单删除', NULL, NULL, NULL, 'system:menu:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439371785551873', '1323439321634258945', '角色管理', 'role', '/basic/system/role/index', NULL, NULL, 'form', 1, '1014', 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439375539453953', '1323439371785551873', '角色新增', NULL, NULL, NULL, 'system:role:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439378580324354', '1323439371785551873', '角色修改', NULL, NULL, NULL, 'system:role:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439381906407425', '1323439371785551873', '角色详情', NULL, NULL, NULL, 'system:role:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439384934694913', '1323439371785551873', '角色删除', NULL, NULL, NULL, 'system:role:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439387669381121', '1323439321634258945', '用户管理', 'user', '/basic/system/user/index', NULL, NULL, 'form', 1, '1015', 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439390789943298', '1323439387669381121', '用户新增', NULL, NULL, NULL, 'system:user:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439393298137089', '1323439387669381121', '用户修改', NULL, NULL, NULL, 'system:user:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439396758437890', '1323439387669381121', '用户详情', NULL, NULL, NULL, 'system:user:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439399979663361', '1323439387669381121', '用户删除', NULL, NULL, NULL, 'system:user:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439402764681218', '1323439387669381121', '重置密码', NULL, NULL, NULL, 'system:user:reset:password', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439406451474434', '1323439321634258945', '参数管理', 'config', '/basic/system/config/index', NULL, NULL, 'form', 1, '1016', 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439408812867585', '1323439406451474434', '参数新增', NULL, NULL, NULL, 'system:config:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439412055064578', '1323439406451474434', '参数修改', NULL, NULL, NULL, 'system:config:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439415838326785', '1323439406451474434', '参数详情', NULL, NULL, NULL, 'system:config:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439419256684545', '1323439406451474434', '参数删除', NULL, NULL, NULL, 'system:config:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439423211913218', '1323439406451474434', '刷新缓存', NULL, NULL, NULL, 'system:config:refresh', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439426693185537', '1323439321634258945', '字典管理', 'dict', '/basic/system/dict/index', NULL, NULL, 'form', 1, '1017', 0, 7, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439428819697665', '1323439426693185537', '字典新增', NULL, NULL, NULL, 'system:dict:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439431755710465', '1323439426693185537', '字典修改', NULL, NULL, NULL, 'system:dict:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439434893049857', '1323439426693185537', '字典详情', NULL, NULL, NULL, 'system:dict:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439437258637313', '1323439426693185537', '字典删除', NULL, NULL, NULL, 'system:dict:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439440840572930', '1323439426693185537', '刷新缓存', NULL, NULL, NULL, 'system:dict:refresh', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439444309262337', '1323439314692685825', '系统监控', 'monitor', '/basic/monitor/index', '/basic/monitor/loginlog', NULL, 'form', 0, '1020', 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439447442407426', '1323439444309262337', '登录日志', 'loginlog', '/basic/monitor/loginlog/index', NULL, NULL, 'form', 1, '1021', 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439450667827202', '1323439447442407426', '日志详情', NULL, NULL, NULL, 'monitor:loginlog:detail', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439454547558402', '1323439447442407426', '日志删除', NULL, NULL, NULL, 'monitor:loginlog:remove', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439457684897793', '1323439444309262337', '操作日志', 'operlog', '/basic/monitor/operlog/index', NULL, NULL, 'form', 1, '1022', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439461132615682', '1323439457684897793', '日志详情', NULL, NULL, NULL, 'monitor:operlog:detail', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323439463754055682', '1323439457684897793', '日志删除', NULL, NULL, NULL, 'monitor:operlog:remove', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446715848216577', '1323439314692685825', '任务调度', 'scheduler', '/basic/scheduler/index', '/basic/scheduler/job', NULL, 'form', 0, '1030', 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446719488872450', '1323446715848216577', '任务管理', 'taskjob', '/basic/scheduler/taskjob/index', NULL, NULL, 'form', 1, '1031', 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446723389575170', '1323446719488872450', '任务新增', NULL, NULL, NULL, 'scheduler:job:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446727512576001', '1323446719488872450', '任务修改', NULL, NULL, NULL, 'scheduler:job:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446731551690753', '1323446719488872450', '任务详情', NULL, NULL, NULL, 'scheduler:job:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446733736923137', '1323446719488872450', '任务删除', NULL, NULL, NULL, 'scheduler:job:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446737285304322', '1323446719488872450', '任务暂停', NULL, NULL, NULL, 'scheduler:job:pause', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446741144064001', '1323446719488872450', '任务恢复', NULL, NULL, NULL, 'scheduler:job:resume', NULL, 2, NULL, 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446745309007873', '1323446719488872450', '立即执行', NULL, NULL, NULL, 'scheduler:job:run', NULL, 2, NULL, 0, 7, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446748291158018', '1323446715848216577', '日志管理', 'tasklog', '/basic/scheduler/tasklog/index', NULL, NULL, 'form', 1, '1032', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446755090124802', '1323446748291158018', '日志详情', NULL, NULL, NULL, 'scheduler:log:detail', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446758726586370', '1323446748291158018', '日志删除', NULL, NULL, NULL, 'scheduler:log:remove', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446761884897282', '0', '元数据管理', '/metadata', 'Layout', '/metadata/index', NULL, 'form', 0, '20', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446765831737346', '1323446761884897282', '看板', 'index', '/metadata/index', NULL, NULL, 'form', 1, NULL, 1, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446768281210882', '1323446761884897282', '数据源', 'datasource', '/metadata/datasource/index', NULL, NULL, 'form', 1, '2011', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446772064473090', '1323446768281210882', '数据源新增', NULL, NULL, NULL, 'metadata:datasource:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446774992097282', '1323446768281210882', '数据源修改', NULL, NULL, NULL, 'metadata:datasource:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446778368512002', '1323446768281210882', '数据源详情', NULL, NULL, NULL, 'metadata:datasource:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446782034333697', '1323446768281210882', '数据源删除', NULL, NULL, NULL, 'metadata:datasource:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446785037455362', '1323446768281210882', '刷新缓存', NULL, NULL, NULL, 'metadata:datasource:refresh', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446788590030850', '1323446768281210882', '元数据同步', NULL, NULL, NULL, 'metadata:datasource:sync', NULL, 2, NULL, 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446792067108865', '1323446768281210882', '数据库文档', NULL, NULL, NULL, 'metadata:datasource:word', NULL, 2, NULL, 0, 7, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446796131389441', '1323446768281210882', '连通性检测', NULL, NULL, NULL, 'metadata:datasource:connect', NULL, 2, NULL, 0, 8, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446800178892801', '1323446761884897282', '元数据', 'datacolumn', '/metadata/datacolumn/index', NULL, NULL, 'form', 1, '2012', 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446803093934082', '1323446800178892801', '元数据详情', NULL, NULL, NULL, 'metadata:datacolumn:detail', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446806457765890', '1323446761884897282', '数据授权', 'dataauthorize', '/metadata/dataauthorize/index', NULL, NULL, 'form', 1, '2013', 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446810152947713', '1323446806457765890', '授权修改', NULL, NULL, NULL, 'metadata:dataauthorize:edit', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446812367540226', '1323446806457765890', '刷新缓存', NULL, NULL, NULL, 'metadata:dataauthorize:refresh', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446814506635265', '1323446761884897282', '变更记录', 'changerecord', '/metadata/changerecord/index', NULL, NULL, 'form', 1, '2014', 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446818050822146', '1323446814506635265', '变更记录新增', NULL, NULL, NULL, 'metadata:changerecord:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446821838278657', '1323446814506635265', '变更记录修改', NULL, NULL, NULL, 'metadata:changerecord:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446824904314882', '1323446814506635265', '变更记录详情', NULL, NULL, NULL, 'metadata:changerecord:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446828507222018', '1323446814506635265', '变更记录删除', NULL, NULL, NULL, 'metadata:changerecord:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446830755368961', '1323446761884897282', '数据检索', 'datasearch', '/metadata/datasearch/index', NULL, NULL, 'form', 1, '2015', 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446833439723522', '1323446761884897282', '数据地图', 'datamap', '/metadata/datamap/index', NULL, NULL, 'form', 1, '2016', 0, 7, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446835830476801', '1323446761884897282', '血缘流向（待开发）', 'datablood', '/metadata/datablood/index', NULL, NULL, 'form', 1, '2017', 0, 8, 0, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2022-04-30 16:09:45', NULL);
INSERT INTO `sys_menu` VALUES ('1323446838196064257', '1323446761884897282', 'SQL工作台', 'sqlconsole', '/metadata/sqlconsole/index', NULL, NULL, 'form', 1, '2018', 0, 9, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446842084184065', '0', '数据标准管理', '/standard', 'Layout', '/standard/index', NULL, 'form', 0, '30', 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446844382662657', '1323446842084184065', '看板', 'index', '/standard/index', NULL, NULL, 'form', 1, NULL, 1, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446848434360322', '1323446842084184065', '标准字典', 'datadict', '/standard/datadict/index', NULL, NULL, 'form', 1, '3011', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446851588476930', '1323446848434360322', '标准类别新增', NULL, NULL, NULL, 'standard:type:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446854285414401', '1323446848434360322', '标准类别修改', NULL, NULL, NULL, 'standard:type:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446856474841089', '1323446848434360322', '标准类别详情', NULL, NULL, NULL, 'standard:type:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446858899148801', '1323446848434360322', '标准类别删除', NULL, NULL, NULL, 'standard:type:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446861998739458', '1323446848434360322', '标准字典新增', NULL, NULL, NULL, 'standard:dict:add', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446864804728834', '1323446848434360322', '标准字典修改', NULL, NULL, NULL, 'standard:dict:edit', NULL, 2, NULL, 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446867434557441', '1323446848434360322', '标准字典详情', NULL, NULL, NULL, 'standard:dict:detail', NULL, 2, NULL, 0, 7, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446871129739266', '1323446848434360322', '标准字典删除', NULL, NULL, NULL, 'standard:dict:remove', NULL, 2, NULL, 0, 8, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446872239749167', '1323446848434360322', '刷新缓存', NULL, NULL, NULL, 'standard:dict:refresh', NULL, 2, NULL, 0, 9, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323446873914757121', '1323446842084184065', '对照表', 'dictcontrast', '/standard/dictcontrast/index', NULL, NULL, 'form', 1, '3012', 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457155479363586', '1323446873914757121', '对照表新增', NULL, NULL, NULL, 'standard:contrast:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457160046960641', '1323446873914757121', '对照表修改', NULL, NULL, NULL, 'standard:contrast:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457163289157633', '1323446873914757121', '对照表详情', NULL, NULL, NULL, 'standard:contrast:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457168091635713', '1323446873914757121', '对照表删除', NULL, NULL, NULL, 'standard:contrast:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457171208003585', '1323446873914757121', '对照字典新增', NULL, NULL, NULL, 'standard:contrast:dict:add', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457174412451841', '1323446873914757121', '对照字典修改', NULL, NULL, NULL, 'standard:contrast:dict:edit', NULL, 2, NULL, 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457176954200065', '1323446873914757121', '对照字典详情', NULL, NULL, NULL, 'standard:contrast:dict:detail', NULL, 2, NULL, 0, 7, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457180657770497', '1323446873914757121', '对照字典删除', NULL, NULL, NULL, 'standard:contrast:dict:remove', NULL, 2, NULL, 0, 8, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457183010775041', '1323446842084184065', '字典对照', 'dictmapping', '/standard/dictmapping/index', NULL, NULL, 'form', 1, '3013', 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457185179230209', '1323457183010775041', '自动对照', NULL, NULL, NULL, 'standard:mapping:auto', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457188840857601', '1323457183010775041', '手动对照', NULL, NULL, NULL, 'standard:mapping:manual', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457190950592513', '1323457183010775041', '取消对照', NULL, NULL, NULL, 'standard:mapping:cancel', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457193882411009', '1323446842084184065', '对照统计', 'contraststat', '/standard/contraststat/index', NULL, NULL, 'form', 1, '3014', 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457195987951617', '0', '数据质量管理', '/quality', 'Layout', '/quality/index', NULL, 'form', 0, '40', 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457199494389762', '1323457195987951617', '看板', 'index', '/quality/index', NULL, NULL, 'form', 1, NULL, 1, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457202715615233', '1323457195987951617', '规则配置', 'checkrule', '/quality/checkrule/index', NULL, NULL, 'form', 1, '4011', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457206838616066', '1323457202715615233', '规则新增', NULL, NULL, NULL, 'quality:rule:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457210680598530', '1323457202715615233', '规则修改', NULL, NULL, NULL, 'quality:rule:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457213478199298', '1323457202715615233', '规则详情', NULL, NULL, NULL, 'quality:rule:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457216112222210', '1323457202715615233', '规则删除', NULL, NULL, NULL, 'quality:rule:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457220054867969', '1323457195987951617', '问题统计', 'checkstat', '/quality/checkstat/index', NULL, NULL, 'form', 1, '4012', 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457223376756738', '1323457195987951617', '质量报告', 'checkreport', '/quality/checkreport/index', NULL, NULL, 'form', 1, '4013', 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457226472153090', '1323457195987951617', '定时任务', 'checkjob', '/quality/checkjob/index', NULL, NULL, 'form', 1, '4014', 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457229768876033', '1323457226472153090', '任务暂停', NULL, NULL, NULL, 'quality:job:pause', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457232570671106', '1323457226472153090', '任务恢复', NULL, NULL, NULL, 'quality:job:resume', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457235842228226', '1323457195987951617', '任务日志', 'checklog', '/quality/checklog/index', NULL, NULL, 'form', 1, '4015', 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457239495467009', '0', '主数据管理', '/masterdata', 'Layout', '/masterdata/index', NULL, 'form', 0, '50', 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457241680699394', '1323457239495467009', '看板', 'index', '/masterdata/index', NULL, NULL, 'form', 1, NULL, 1, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457245346521089', '1323457239495467009', '数据模型', 'datamodel', '/masterdata/datamodel/index', NULL, NULL, 'form', 1, '5011', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457248882319361', '1323457245346521089', '模型新增', NULL, NULL, NULL, 'masterdata:model:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457252309065730', '1323457245346521089', '模型修改', NULL, NULL, NULL, 'masterdata:model:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457256096522241', '1323457245346521089', '模型详情', NULL, NULL, NULL, 'masterdata:model:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457259636514817', '1323457245346521089', '模型删除', NULL, NULL, NULL, 'masterdata:model:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457262379589633', '1323457245346521089', '模型提交', NULL, NULL, NULL, 'masterdata:model:submit', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457265072332802', '1323457245346521089', '数据建模', NULL, NULL, NULL, 'masterdata:model:create', NULL, 2, NULL, 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457267387588610', '1323457239495467009', '数据管理', 'datamanage', '/masterdata/datamanage/index', NULL, NULL, 'form', 1, '5012', 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457271124713473', '1323457267387588610', '数据新增', NULL, NULL, NULL, 'masterdata:data:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457273783902210', '1323457267387588610', '数据修改', NULL, NULL, NULL, 'masterdata:data:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457276178849794', '1323457267387588610', '数据详情', NULL, NULL, NULL, 'masterdata:data:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457280285073410', '1323457267387588610', '数据删除', NULL, NULL, NULL, 'masterdata:data:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457284382908418', '0', '数据集市管理', '/market', 'Layout', '/market/index', NULL, 'form', 0, '60', 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457288312971266', '1323457284382908418', '看板', 'index', '/market/index', NULL, NULL, 'form', 1, NULL, 1, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457290468843522', '1323457284382908418', '数据服务', 'dataapi', '/market/dataapi/index', NULL, NULL, 'form', 1, '6011', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457293983670274', '1323457290468843522', '数据服务新增', NULL, NULL, NULL, 'market:api:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457297620131842', '1323457290468843522', '数据服务修改', NULL, NULL, NULL, 'market:api:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457300937826305', '1323457290468843522', '数据服务详情', NULL, NULL, NULL, 'market:api:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323457303185973249', '1323457290468843522', '数据服务删除', NULL, NULL, NULL, 'market:api:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465292143890434', '1323457290468843522', '数据服务提交', NULL, NULL, NULL, 'market:api:submit', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465296644378625', '1323457290468843522', '数据服务拷贝', NULL, NULL, NULL, 'market:api:copy', NULL, 2, NULL, 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465300733825026', '1323457290468843522', '数据服务发布', NULL, NULL, NULL, 'market:api:release', NULL, 2, NULL, 0, 7, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465303497871361', '1323457290468843522', '数据服务注销', NULL, NULL, NULL, 'market:api:cancel', NULL, 2, NULL, 0, 8, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465306433884161', '1323457290468843522', '数据服务文档', NULL, NULL, NULL, 'market:api:word', NULL, 2, NULL, 0, 9, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465310024208386', '1323457290468843522', '数据服务示例', NULL, NULL, NULL, 'market:api:example', NULL, 2, NULL, 0, 10, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465313249628161', '1323457284382908418', '数据脱敏', 'apimask', '/market/apimask/index', NULL, NULL, 'form', 1, '6012', 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465316550545409', '1323465313249628161', '数据脱敏新增', NULL, NULL, NULL, 'market:mask:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465319444615170', '1323465313249628161', '数据脱敏修改', NULL, NULL, NULL, 'market:mask:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465322640674817', '1323465313249628161', '数据脱敏详情', NULL, NULL, NULL, 'market:mask:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465324955930625', '1323465313249628161', '数据脱敏删除', NULL, NULL, NULL, 'market:mask:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465328038744065', '1323457284382908418', '接口日志', 'apilog', '/market/apilog/index', NULL, NULL, 'form', 1, '6013', 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465331868143617', '1323465328038744065', '日志详情', NULL, NULL, NULL, 'market:api:log:detail', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465335894675457', '1323465328038744065', '日志删除', NULL, NULL, NULL, 'market:api:log:remove', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465338889408514', '1323457284382908418', '服务集成', 'dataservice', '/market/dataservice/index', NULL, NULL, 'form', 1, '6014', 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465341871558657', '1323465338889408514', '服务集成新增', NULL, NULL, NULL, 'market:service:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465345898090498', '1323465338889408514', '服务集成修改', NULL, NULL, NULL, 'market:service:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465349958176769', '1323465338889408514', '服务集成详情', NULL, NULL, NULL, 'market:service:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465352999047170', '1323465338889408514', '服务集成删除', NULL, NULL, NULL, 'market:service:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465356161552386', '1323457284382908418', '服务日志', 'servicelog', '/market/servicelog/index', NULL, NULL, 'form', 1, '6015', 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465359139508225', '1323465356161552386', '日志详情', NULL, NULL, NULL, 'market:service:log:detail', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465362562060290', '1323465356161552386', '日志删除', NULL, NULL, NULL, 'market:service:log:remove', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465364864733186', '0', '可视化管理', '/visual', 'Layout', '/visual/index', NULL, 'form', 0, '70', 0, 7, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465367142240257', '1323465364864733186', '看板', 'index', '/visual/index', NULL, NULL, 'form', 1, NULL, 1, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465369847566337', '1323465364864733186', '数据集', 'dataset', '/visual/dataset/index', NULL, NULL, 'form', 1, '7011', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465372125073409', '1323465369847566337', '数据集新增', NULL, NULL, NULL, 'visual:set:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465374515826690', '1323465369847566337', '数据集修改', NULL, NULL, NULL, 'visual:set:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465378643021825', '1323465369847566337', '数据集详情', NULL, NULL, NULL, 'visual:set:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465380983443458', '1323465369847566337', '数据集删除', NULL, NULL, NULL, 'visual:set:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465383680380930', '1323465369847566337', '数据预览', NULL, NULL, NULL, 'visual:set:preview', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465385953693697', '1323465364864733186', '图表配置', 'datachart', '/visual/datachart/index', NULL, NULL, 'form', 1, '7012', 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465389925699585', '1323465385953693697', '图表新增', NULL, NULL, NULL, 'visual:chart:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465393704767490', '1323465385953693697', '图表修改', NULL, NULL, NULL, 'visual:chart:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465396888244225', '1323465385953693697', '图表配置', NULL, NULL, NULL, 'visual:chart:build', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465400029777921', '1323465385953693697', '图表删除', NULL, NULL, NULL, 'visual:chart:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465401029783632', '1323465385953693697', '图表拷贝', NULL, NULL, NULL, 'visual:chart:copy', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465402781241346', '1323465364864733186', '看板配置', 'databoard', '/visual/databoard/index', NULL, NULL, 'form', 1, '7013', 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465405113274369', '1323465402781241346', '看板新增', NULL, NULL, NULL, 'visual:board:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465407613079553', '1323465402781241346', '看板修改', NULL, NULL, NULL, 'visual:board:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465408371552934', '1323465402781241346', '看板配置', NULL, NULL, NULL, 'visual:board:build', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465410377125889', '1323465402781241346', '看板预览', NULL, NULL, NULL, 'visual:board:preview', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465414361714689', '1323465402781241346', '看板删除', NULL, NULL, NULL, 'visual:board:remove', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465415426742390', '1323465402781241346', '看板拷贝', NULL, NULL, NULL, 'visual:board:copy', NULL, 2, NULL, 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465416611367426', '1323465364864733186', '酷屏配置', 'datascreen', '/visual/datascreen/index', NULL, NULL, 'form', 1, '7014', 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465416705008130', '1323465416611367426', '酷屏新增', NULL, NULL, NULL, 'visual:screen:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465417236419686', '1323465416611367426', '酷屏修改', NULL, NULL, NULL, 'visual:screen:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465417607196160', '1323465416611367426', '酷屏配置', NULL, NULL, NULL, 'visual:screen:build', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465417802278401', '1323465416611367426', '酷屏预览', NULL, NULL, NULL, 'visual:screen:preview', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465418100034561', '1323465416611367426', '酷屏删除', NULL, NULL, NULL, 'visual:screen:remove', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465418218860801', '1323465416611367426', '酷屏拷贝', NULL, NULL, NULL, 'visual:screen:copy', NULL, 2, NULL, 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465418392440833', '0', '流程管理', '/workflow', 'Layout', '/workflow/index', NULL, 'form', 0, '80', 0, 8, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465421974376449', '1323465418392440833', '看板', 'index', '/workflow/index', NULL, NULL, 'form', 1, NULL, 1, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465424239300610', '1323465418392440833', '流程定义', 'definition', '/workflow/definition/index', NULL, NULL, 'form', 1, '8011', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465428202917890', '1323465424239300610', '流程分类新增', NULL, NULL, NULL, 'workflow:definition:type:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465431923265538', '1323465424239300610', '流程分类修改', NULL, NULL, NULL, 'workflow:definition:type:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465434813140993', '1323465424239300610', '流程分类详情', NULL, NULL, NULL, 'workflow:definition:type:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465438407659522', '1323465424239300610', '流程分类删除', NULL, NULL, NULL, 'workflow:definition:type:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465442094452738', '1323465424239300610', '流程定义导入', NULL, NULL, NULL, 'workflow:definition:import', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465445244375042', '1323465424239300610', '流程定义流程图', NULL, NULL, NULL, 'workflow:definition:resource', NULL, 2, NULL, 0, 6, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323465448629178369', '1323465424239300610', '流程定义激活', NULL, NULL, NULL, 'workflow:definition:activate', NULL, 2, NULL, 0, 7, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491848870330369', '1323465424239300610', '流程定义挂起', NULL, NULL, NULL, 'workflow:definition:suspend', NULL, 2, NULL, 0, 8, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491854062878722', '1323465424239300610', '流程定义删除', NULL, NULL, NULL, 'workflow:definition:remove', NULL, 2, NULL, 0, 9, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491858198462465', '1323465418392440833', '流程实例', 'instance', '/workflow/instance/index', '/workflow/instance/running', NULL, 'form', 0, '8020', 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491860798930945', '1323491858198462465', '运行中的流程', 'running', '/workflow/instance/running/index', NULL, NULL, 'form', 1, '8021', 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491863856578561', '1323491860798930945', '流程追踪', NULL, NULL, NULL, 'workflow:instance:track', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491866775814145', '1323491860798930945', '流程激活', NULL, NULL, NULL, 'workflow:instance:running:activate', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491870609408001', '1323491860798930945', '流程挂起', NULL, NULL, NULL, 'workflow:instance:running:suspend', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491873587363842', '1323491860798930945', '流程删除', NULL, NULL, NULL, 'workflow:instance:running:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491877102190593', '1323491858198462465', '我发起的流程', 'mystarted', '/workflow/instance/mystarted/index', NULL, NULL, 'form', 1, '8022', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491881074196481', '1323491858198462465', '我参与的流程', 'myinvolved', '/workflow/instance/myinvolved/index', NULL, NULL, 'form', 1, '8023', 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491884450611202', '1323465418392440833', '流程任务', 'task', '/workflow/task/index', '/workflow/task/todo', NULL, 'form', 0, '8030', 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491888380674050', '1323491884450611202', '待办任务', 'todo', '/workflow/task/todo/index', NULL, NULL, 'form', 1, '8031', 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491891807420417', '1323491888380674050', '任务签收', NULL, NULL, NULL, 'workflow:task:caim', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491895217389569', '1323491888380674050', '任务反签收', NULL, NULL, NULL, 'workflow:task:unclaim', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491898304397313', '1323491888380674050', '任务委派', NULL, NULL, NULL, 'workflow:task:delegate', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491900485435393', '1323491888380674050', '任务转办', NULL, NULL, NULL, 'workflow:task:assignee', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491903840878594', '1323491888380674050', '任务审核', NULL, NULL, NULL, 'workflow:task:exam', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491906328100865', '1323491884450611202', '已办任务', 'done', '/workflow/task/done/index', NULL, NULL, 'form', 1, '8032', 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491908735631361', '1323465418392440833', '业务配置', 'business', '/workflow/business/index', NULL, NULL, 'form', 1, '8041', 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491911856193538', '1323491908735631361', '业务配置新增', NULL, NULL, NULL, 'workflow:business:add', NULL, 2, NULL, 0, 1, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491915605901314', '1323491908735631361', '业务配置新增', NULL, NULL, NULL, 'workflow:business:edit', NULL, 2, NULL, 0, 2, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491918722269185', '1323491908735631361', '业务配置新增', NULL, NULL, NULL, 'workflow:business:detail', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491921226268673', '1323491908735631361', '业务配置新增', NULL, NULL, NULL, 'workflow:business:remove', NULL, 2, NULL, 0, 4, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1323491925059862529', '1323491908735631361', '刷新缓存', NULL, NULL, NULL, 'workflow:business:refresh', NULL, 2, NULL, 0, 5, 1, '1214835832967581698', '2020-11-03 13:32:40', '1214835832967581698', '2020-11-03 13:32:40', NULL);
INSERT INTO `sys_menu` VALUES ('1520391362007007233', '1323457226472153090', '立即执行', NULL, NULL, NULL, 'quality:job:run', NULL, 2, NULL, 0, 3, 1, '1214835832967581698', '2022-04-30 21:15:23', '1214835832967581698', '2022-04-30 21:15:23', NULL);

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '岗位名称',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '岗位信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES ('1214825605782228993', '项目经理', 1, '1', '2020-01-08 16:26:09', '1', '2020-01-08 16:26:09', NULL);
INSERT INTO `sys_post` VALUES ('1214825677672599554', '普通员工', 1, '1', '2020-01-08 16:26:27', '1', '2020-01-08 16:26:27', NULL);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色编码',
  `data_scope` tinyint(4) NULL DEFAULT NULL COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限）',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES ('1214826565321543682', '管理员', 'admin', 1, 1, '1', '2020-01-08 16:29:58', '1214835832967581698', '2022-04-30 21:16:15', NULL);
INSERT INTO `sys_role` VALUES ('1319084037507244034', '普通用户', 'simple', 1, 1, '1214835832967581698', '2020-10-22 09:11:57', '1214835832967581698', '2020-12-18 08:28:55', NULL);
INSERT INTO `sys_role` VALUES ('1319092939179286529', '审核用户', 'audit', 1, 1, '1319084968579817473', '2020-10-22 09:47:19', '1214835832967581698', '2020-11-03 16:07:36', NULL);

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色部门主键ID',
  `role_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色ID',
  `dept_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`id`, `role_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色和部门关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色菜单主键ID',
  `role_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色ID',
  `menu_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色和资源关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES ('1323537293931868161', '1319092939179286529', '1323465418392440833');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868162', '1319092939179286529', '1323465421974376449');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868163', '1319092939179286529', '1323465424239300610');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868164', '1319092939179286529', '1323465428202917890');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868165', '1319092939179286529', '1323465431923265538');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868166', '1319092939179286529', '1323465434813140993');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868167', '1319092939179286529', '1323465438407659522');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868168', '1319092939179286529', '1323465442094452738');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868169', '1319092939179286529', '1323465445244375042');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868170', '1319092939179286529', '1323465448629178369');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868171', '1319092939179286529', '1323491848870330369');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868172', '1319092939179286529', '1323491854062878722');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868173', '1319092939179286529', '1323491858198462465');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868174', '1319092939179286529', '1323491860798930945');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868175', '1319092939179286529', '1323491863856578561');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868176', '1319092939179286529', '1323491866775814145');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868177', '1319092939179286529', '1323491870609408001');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868178', '1319092939179286529', '1323491873587363842');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868179', '1319092939179286529', '1323491877102190593');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868180', '1319092939179286529', '1323491881074196481');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868181', '1319092939179286529', '1323491884450611202');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868182', '1319092939179286529', '1323491888380674050');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868183', '1319092939179286529', '1323491891807420417');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868184', '1319092939179286529', '1323491895217389569');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868185', '1319092939179286529', '1323491898304397313');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868186', '1319092939179286529', '1323491900485435393');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868187', '1319092939179286529', '1323491903840878594');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868188', '1319092939179286529', '1323491906328100865');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868189', '1319092939179286529', '1323491908735631361');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868190', '1319092939179286529', '1323491911856193538');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868191', '1319092939179286529', '1323491915605901314');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868192', '1319092939179286529', '1323491918722269185');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868193', '1319092939179286529', '1323491921226268673');
INSERT INTO `sys_role_menu` VALUES ('1323537293931868194', '1319092939179286529', '1323491925059862529');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271426', '1319084037507244034', '1323446761884897282');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271427', '1319084037507244034', '1323446765831737346');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271428', '1319084037507244034', '1323446768281210882');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271429', '1319084037507244034', '1323446772064473090');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271430', '1319084037507244034', '1323446774992097282');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271431', '1319084037507244034', '1323446778368512002');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271432', '1319084037507244034', '1323446782034333697');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271433', '1319084037507244034', '1323446785037455362');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271434', '1319084037507244034', '1323446788590030850');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271435', '1319084037507244034', '1323446792067108865');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271436', '1319084037507244034', '1323446796131389441');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271437', '1319084037507244034', '1323446800178892801');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271438', '1319084037507244034', '1323446803093934082');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271439', '1319084037507244034', '1323446806457765890');
INSERT INTO `sys_role_menu` VALUES ('1339729316279271440', '1319084037507244034', '1323446810152947713');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465730', '1319084037507244034', '1323446812367540226');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465731', '1319084037507244034', '1323446814506635265');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465732', '1319084037507244034', '1323446818050822146');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465733', '1319084037507244034', '1323446821838278657');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465734', '1319084037507244034', '1323446824904314882');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465735', '1319084037507244034', '1323446828507222018');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465736', '1319084037507244034', '1323446830755368961');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465737', '1319084037507244034', '1323446833439723522');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465738', '1319084037507244034', '1323446835830476801');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465739', '1319084037507244034', '1323446838196064257');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465740', '1319084037507244034', '1323446842084184065');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465741', '1319084037507244034', '1323446844382662657');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465742', '1319084037507244034', '1323446848434360322');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465743', '1319084037507244034', '1323446851588476930');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465744', '1319084037507244034', '1323446854285414401');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465745', '1319084037507244034', '1323446856474841089');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465746', '1319084037507244034', '1323446858899148801');
INSERT INTO `sys_role_menu` VALUES ('1339729316283465747', '1319084037507244034', '1323446861998739458');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660034', '1319084037507244034', '1323446864804728834');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660035', '1319084037507244034', '1323446867434557441');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660036', '1319084037507244034', '1323446871129739266');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660037', '1319084037507244034', '1323446872239749167');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660038', '1319084037507244034', '1323446873914757121');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660039', '1319084037507244034', '1323457155479363586');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660040', '1319084037507244034', '1323457160046960641');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660041', '1319084037507244034', '1323457163289157633');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660042', '1319084037507244034', '1323457168091635713');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660043', '1319084037507244034', '1323457171208003585');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660044', '1319084037507244034', '1323457174412451841');
INSERT INTO `sys_role_menu` VALUES ('1339729316287660045', '1319084037507244034', '1323457176954200065');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854338', '1319084037507244034', '1323457180657770497');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854339', '1319084037507244034', '1323457183010775041');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854340', '1319084037507244034', '1323457185179230209');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854341', '1319084037507244034', '1323457188840857601');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854342', '1319084037507244034', '1323457190950592513');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854343', '1319084037507244034', '1323457193882411009');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854344', '1319084037507244034', '1323457195987951617');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854345', '1319084037507244034', '1323457199494389762');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854346', '1319084037507244034', '1323457202715615233');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854347', '1319084037507244034', '1323457206838616066');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854348', '1319084037507244034', '1323457210680598530');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854349', '1319084037507244034', '1323457213478199298');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854350', '1319084037507244034', '1323457216112222210');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854351', '1319084037507244034', '1323457220054867969');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854352', '1319084037507244034', '1323457223376756738');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854353', '1319084037507244034', '1323457226472153090');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854354', '1319084037507244034', '1323457229768876033');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854355', '1319084037507244034', '1323457232570671106');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854356', '1319084037507244034', '1323457235842228226');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854357', '1319084037507244034', '1323457239495467009');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854358', '1319084037507244034', '1323457241680699394');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854359', '1319084037507244034', '1323457245346521089');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854360', '1319084037507244034', '1323457248882319361');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854361', '1319084037507244034', '1323457252309065730');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854362', '1319084037507244034', '1323457256096522241');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854363', '1319084037507244034', '1323457259636514817');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854364', '1319084037507244034', '1323457262379589633');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854365', '1319084037507244034', '1323457265072332802');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854366', '1319084037507244034', '1323457267387588610');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854367', '1319084037507244034', '1323457271124713473');
INSERT INTO `sys_role_menu` VALUES ('1339729316291854368', '1319084037507244034', '1323457273783902210');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048641', '1319084037507244034', '1323457276178849794');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048642', '1319084037507244034', '1323457280285073410');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048643', '1319084037507244034', '1323457284382908418');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048644', '1319084037507244034', '1323457288312971266');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048645', '1319084037507244034', '1323457290468843522');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048646', '1319084037507244034', '1323457293983670274');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048647', '1319084037507244034', '1323457297620131842');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048648', '1319084037507244034', '1323457300937826305');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048649', '1319084037507244034', '1323457303185973249');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048650', '1319084037507244034', '1323465292143890434');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048651', '1319084037507244034', '1323465296644378625');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048652', '1319084037507244034', '1323465300733825026');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048653', '1319084037507244034', '1323465303497871361');
INSERT INTO `sys_role_menu` VALUES ('1339729316296048654', '1319084037507244034', '1323465306433884161');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242945', '1319084037507244034', '1323465310024208386');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242946', '1319084037507244034', '1323465313249628161');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242947', '1319084037507244034', '1323465316550545409');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242948', '1319084037507244034', '1323465319444615170');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242949', '1319084037507244034', '1323465322640674817');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242950', '1319084037507244034', '1323465324955930625');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242951', '1319084037507244034', '1323465328038744065');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242952', '1319084037507244034', '1323465331868143617');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242953', '1319084037507244034', '1323465335894675457');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242954', '1319084037507244034', '1323465338889408514');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242955', '1319084037507244034', '1323465341871558657');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242956', '1319084037507244034', '1323465345898090498');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242957', '1319084037507244034', '1323465349958176769');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242958', '1319084037507244034', '1323465352999047170');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242959', '1319084037507244034', '1323465356161552386');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242960', '1319084037507244034', '1323465359139508225');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242961', '1319084037507244034', '1323465362562060290');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242962', '1319084037507244034', '1323465364864733186');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242963', '1319084037507244034', '1323465367142240257');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242964', '1319084037507244034', '1323465369847566337');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242965', '1319084037507244034', '1323465372125073409');
INSERT INTO `sys_role_menu` VALUES ('1339729316300242966', '1319084037507244034', '1323465374515826690');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437249', '1319084037507244034', '1323465378643021825');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437250', '1319084037507244034', '1323465380983443458');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437251', '1319084037507244034', '1323465383680380930');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437252', '1319084037507244034', '1323465385953693697');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437253', '1319084037507244034', '1323465389925699585');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437254', '1319084037507244034', '1323465393704767490');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437255', '1319084037507244034', '1323465396888244225');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437256', '1319084037507244034', '1323465400029777921');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437257', '1319084037507244034', '1323465401029783632');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437258', '1319084037507244034', '1323465402781241346');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437259', '1319084037507244034', '1323465405113274369');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437260', '1319084037507244034', '1323465407613079553');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437261', '1319084037507244034', '1323465408371552934');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437262', '1319084037507244034', '1323465410377125889');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437263', '1319084037507244034', '1323465414361714689');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437264', '1319084037507244034', '1323465415426742390');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437265', '1319084037507244034', '1323465416611367426');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437266', '1319084037507244034', '1323465416705008130');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437267', '1319084037507244034', '1323465417236419686');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437268', '1319084037507244034', '1323465417607196160');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437269', '1319084037507244034', '1323465417802278401');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437270', '1319084037507244034', '1323465418100034561');
INSERT INTO `sys_role_menu` VALUES ('1339729316304437271', '1319084037507244034', '1323465418218860801');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937410', '1214826565321543682', '1323439314692685825');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937411', '1214826565321543682', '1323439318413033473');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937412', '1214826565321543682', '1323439321634258945');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937413', '1214826565321543682', '1323439323769159681');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937414', '1214826565321543682', '1323439327296569346');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937415', '1214826565321543682', '1323439329708294145');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937416', '1214826565321543682', '1323439332476534786');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937417', '1214826565321543682', '1323439336071053314');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937418', '1214826565321543682', '1323439338231119873');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937419', '1214826565321543682', '1323439341569785858');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937420', '1214826565321543682', '1323439344791011329');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937421', '1214826565321543682', '1323439348771405826');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937422', '1214826565321543682', '1323439351912939522');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937423', '1214826565321543682', '1323439355318714370');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937424', '1214826565321543682', '1323439359127142401');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937425', '1214826565321543682', '1323439362759409666');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937426', '1214826565321543682', '1323439365372461057');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937427', '1214826565321543682', '1323439367612219394');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937428', '1214826565321543682', '1323439371785551873');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937429', '1214826565321543682', '1323439375539453953');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937430', '1214826565321543682', '1323439378580324354');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937431', '1214826565321543682', '1323439381906407425');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937432', '1214826565321543682', '1323439384934694913');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937433', '1214826565321543682', '1323439387669381121');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937434', '1214826565321543682', '1323439390789943298');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937435', '1214826565321543682', '1323439393298137089');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937436', '1214826565321543682', '1323439396758437890');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937437', '1214826565321543682', '1323439399979663361');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937438', '1214826565321543682', '1323439402764681218');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937439', '1214826565321543682', '1323439406451474434');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937440', '1214826565321543682', '1323439408812867585');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937441', '1214826565321543682', '1323439412055064578');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937442', '1214826565321543682', '1323439415838326785');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937443', '1214826565321543682', '1323439419256684545');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937444', '1214826565321543682', '1323439423211913218');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937445', '1214826565321543682', '1323439426693185537');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937446', '1214826565321543682', '1323439428819697665');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937447', '1214826565321543682', '1323439431755710465');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937448', '1214826565321543682', '1323439434893049857');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937449', '1214826565321543682', '1323439437258637313');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937450', '1214826565321543682', '1323439440840572930');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937451', '1214826565321543682', '1323439444309262337');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937452', '1214826565321543682', '1323439447442407426');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937453', '1214826565321543682', '1323439450667827202');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937454', '1214826565321543682', '1323439454547558402');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937455', '1214826565321543682', '1323439457684897793');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937456', '1214826565321543682', '1323439461132615682');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937457', '1214826565321543682', '1323439463754055682');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937458', '1214826565321543682', '1323446715848216577');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937459', '1214826565321543682', '1323446719488872450');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937460', '1214826565321543682', '1323446723389575170');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937461', '1214826565321543682', '1323446727512576001');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937462', '1214826565321543682', '1323446731551690753');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937463', '1214826565321543682', '1323446733736923137');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937464', '1214826565321543682', '1323446737285304322');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937465', '1214826565321543682', '1323446741144064001');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937466', '1214826565321543682', '1323446745309007873');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937467', '1214826565321543682', '1323446748291158018');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937468', '1214826565321543682', '1323446755090124802');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937469', '1214826565321543682', '1323446758726586370');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937470', '1214826565321543682', '1323446761884897282');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937471', '1214826565321543682', '1323446765831737346');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937472', '1214826565321543682', '1323446768281210882');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937473', '1214826565321543682', '1323446772064473090');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937474', '1214826565321543682', '1323446774992097282');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937475', '1214826565321543682', '1323446778368512002');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937476', '1214826565321543682', '1323446782034333697');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937477', '1214826565321543682', '1323446785037455362');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937478', '1214826565321543682', '1323446788590030850');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937479', '1214826565321543682', '1323446792067108865');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937480', '1214826565321543682', '1323446796131389441');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937481', '1214826565321543682', '1323446800178892801');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937482', '1214826565321543682', '1323446803093934082');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937483', '1214826565321543682', '1323446806457765890');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937484', '1214826565321543682', '1323446810152947713');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937485', '1214826565321543682', '1323446812367540226');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937486', '1214826565321543682', '1323446814506635265');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937487', '1214826565321543682', '1323446818050822146');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937488', '1214826565321543682', '1323446821838278657');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937489', '1214826565321543682', '1323446824904314882');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937490', '1214826565321543682', '1323446828507222018');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937491', '1214826565321543682', '1323446830755368961');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937492', '1214826565321543682', '1323446833439723522');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937493', '1214826565321543682', '1323446838196064257');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937494', '1214826565321543682', '1323446842084184065');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937495', '1214826565321543682', '1323446844382662657');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937496', '1214826565321543682', '1323446848434360322');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937497', '1214826565321543682', '1323446851588476930');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937498', '1214826565321543682', '1323446854285414401');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937499', '1214826565321543682', '1323446856474841089');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937500', '1214826565321543682', '1323446858899148801');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937501', '1214826565321543682', '1323446861998739458');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937502', '1214826565321543682', '1323446864804728834');
INSERT INTO `sys_role_menu` VALUES ('1520391580609937503', '1214826565321543682', '1323446867434557441');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520322', '1214826565321543682', '1323446871129739266');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520323', '1214826565321543682', '1323446872239749167');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520324', '1214826565321543682', '1323446873914757121');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520325', '1214826565321543682', '1323457155479363586');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520326', '1214826565321543682', '1323457160046960641');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520327', '1214826565321543682', '1323457163289157633');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520328', '1214826565321543682', '1323457168091635713');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520329', '1214826565321543682', '1323457171208003585');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520330', '1214826565321543682', '1323457174412451841');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520331', '1214826565321543682', '1323457176954200065');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520332', '1214826565321543682', '1323457180657770497');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520333', '1214826565321543682', '1323457183010775041');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520334', '1214826565321543682', '1323457185179230209');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520335', '1214826565321543682', '1323457188840857601');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520336', '1214826565321543682', '1323457190950592513');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520337', '1214826565321543682', '1323457193882411009');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520338', '1214826565321543682', '1323457195987951617');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520339', '1214826565321543682', '1323457199494389762');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520340', '1214826565321543682', '1323457202715615233');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520341', '1214826565321543682', '1323457206838616066');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520342', '1214826565321543682', '1323457210680598530');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520343', '1214826565321543682', '1323457213478199298');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520344', '1214826565321543682', '1323457216112222210');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520345', '1214826565321543682', '1323457220054867969');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520346', '1214826565321543682', '1323457223376756738');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520347', '1214826565321543682', '1323457226472153090');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520348', '1214826565321543682', '1323457229768876033');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520349', '1214826565321543682', '1323457232570671106');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520350', '1214826565321543682', '1520391362007007233');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520351', '1214826565321543682', '1323457235842228226');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520352', '1214826565321543682', '1323457239495467009');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520353', '1214826565321543682', '1323457241680699394');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520354', '1214826565321543682', '1323457245346521089');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520355', '1214826565321543682', '1323457248882319361');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520356', '1214826565321543682', '1323457252309065730');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520357', '1214826565321543682', '1323457256096522241');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520358', '1214826565321543682', '1323457259636514817');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520359', '1214826565321543682', '1323457262379589633');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520360', '1214826565321543682', '1323457265072332802');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520361', '1214826565321543682', '1323457267387588610');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520362', '1214826565321543682', '1323457271124713473');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520363', '1214826565321543682', '1323457273783902210');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520364', '1214826565321543682', '1323457276178849794');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520365', '1214826565321543682', '1323457280285073410');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520366', '1214826565321543682', '1323457284382908418');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520367', '1214826565321543682', '1323457288312971266');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520368', '1214826565321543682', '1323457290468843522');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520369', '1214826565321543682', '1323457293983670274');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520370', '1214826565321543682', '1323457297620131842');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520371', '1214826565321543682', '1323457300937826305');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520372', '1214826565321543682', '1323457303185973249');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520373', '1214826565321543682', '1323465292143890434');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520374', '1214826565321543682', '1323465296644378625');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520375', '1214826565321543682', '1323465300733825026');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520376', '1214826565321543682', '1323465303497871361');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520377', '1214826565321543682', '1323465306433884161');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520378', '1214826565321543682', '1323465310024208386');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520379', '1214826565321543682', '1323465313249628161');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520380', '1214826565321543682', '1323465316550545409');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520381', '1214826565321543682', '1323465319444615170');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520382', '1214826565321543682', '1323465322640674817');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520383', '1214826565321543682', '1323465324955930625');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520384', '1214826565321543682', '1323465328038744065');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520385', '1214826565321543682', '1323465331868143617');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520386', '1214826565321543682', '1323465335894675457');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520387', '1214826565321543682', '1323465338889408514');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520388', '1214826565321543682', '1323465341871558657');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520389', '1214826565321543682', '1323465345898090498');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520390', '1214826565321543682', '1323465349958176769');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520391', '1214826565321543682', '1323465352999047170');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520392', '1214826565321543682', '1323465356161552386');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520393', '1214826565321543682', '1323465359139508225');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520394', '1214826565321543682', '1323465362562060290');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520395', '1214826565321543682', '1323465364864733186');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520396', '1214826565321543682', '1323465367142240257');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520397', '1214826565321543682', '1323465369847566337');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520398', '1214826565321543682', '1323465372125073409');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520399', '1214826565321543682', '1323465374515826690');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520400', '1214826565321543682', '1323465378643021825');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520401', '1214826565321543682', '1323465380983443458');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520402', '1214826565321543682', '1323465383680380930');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520403', '1214826565321543682', '1323465385953693697');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520404', '1214826565321543682', '1323465389925699585');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520405', '1214826565321543682', '1323465393704767490');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520406', '1214826565321543682', '1323465396888244225');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520407', '1214826565321543682', '1323465400029777921');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520408', '1214826565321543682', '1323465401029783632');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520409', '1214826565321543682', '1323465402781241346');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520410', '1214826565321543682', '1323465405113274369');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520411', '1214826565321543682', '1323465407613079553');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520412', '1214826565321543682', '1323465408371552934');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520413', '1214826565321543682', '1323465410377125889');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520414', '1214826565321543682', '1323465414361714689');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520415', '1214826565321543682', '1323465415426742390');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520416', '1214826565321543682', '1323465416611367426');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520417', '1214826565321543682', '1323465416705008130');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520418', '1214826565321543682', '1323465417236419686');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520419', '1214826565321543682', '1323465417607196160');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520420', '1214826565321543682', '1323465417802278401');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520421', '1214826565321543682', '1323465418100034561');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520422', '1214826565321543682', '1323465418218860801');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520423', '1214826565321543682', '1323465418392440833');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520424', '1214826565321543682', '1323465421974376449');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520425', '1214826565321543682', '1323465424239300610');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520426', '1214826565321543682', '1323465428202917890');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520427', '1214826565321543682', '1323465431923265538');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520428', '1214826565321543682', '1323465434813140993');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520429', '1214826565321543682', '1323465438407659522');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520430', '1214826565321543682', '1323465442094452738');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520431', '1214826565321543682', '1323465445244375042');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520432', '1214826565321543682', '1323465448629178369');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520433', '1214826565321543682', '1323491848870330369');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520434', '1214826565321543682', '1323491854062878722');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520435', '1214826565321543682', '1323491858198462465');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520436', '1214826565321543682', '1323491860798930945');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520437', '1214826565321543682', '1323491863856578561');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520438', '1214826565321543682', '1323491866775814145');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520439', '1214826565321543682', '1323491870609408001');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520440', '1214826565321543682', '1323491873587363842');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520441', '1214826565321543682', '1323491877102190593');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520442', '1214826565321543682', '1323491881074196481');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520443', '1214826565321543682', '1323491884450611202');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520444', '1214826565321543682', '1323491888380674050');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520445', '1214826565321543682', '1323491891807420417');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520446', '1214826565321543682', '1323491895217389569');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520447', '1214826565321543682', '1323491898304397313');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520448', '1214826565321543682', '1323491900485435393');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520449', '1214826565321543682', '1323491903840878594');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520450', '1214826565321543682', '1323491906328100865');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520451', '1214826565321543682', '1323491908735631361');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520452', '1214826565321543682', '1323491911856193538');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520453', '1214826565321543682', '1323491915605901314');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520454', '1214826565321543682', '1323491918722269185');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520455', '1214826565321543682', '1323491921226268673');
INSERT INTO `sys_role_menu` VALUES ('1520391580622520456', '1214826565321543682', '1323491925059862529');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '电子邮件',
  `phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号码',
  `birthday` date NULL DEFAULT NULL COMMENT '出生日期',
  `dept_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES ('1214835832967581698', 'admin', '管理员', '$2a$10$TmZTXiXLdS9Y1OOFHeAt4uVIMwh0B.zyqOcGBDYOz5QL6o6qa2YTa', 'xxx@qq.com', '136xxx', '1992-12-03', '1197789917762031617', 1, '1214835832967581698', '2020-01-08 17:06:48', '1214835832967581698', '2020-01-08 17:06:48', NULL);
INSERT INTO `sys_user` VALUES ('1319084615276814337', 'zs', '张三', '$2a$10$lsPhICj3H/tspyXXwgFfcO.wbasZZp8eGBZKGfxxxmeAGOvzrhjaK', 'xxx@qq.com', '136xxx', '1995-09-30', '1197790192543469570', 1, '1214835832967581698', '2020-10-22 09:14:14', '1214835832967581698', '2020-10-22 09:14:14', NULL);
INSERT INTO `sys_user` VALUES ('1319084968579817473', 'ls', '李四', '$2a$10$/OdCjDYY/.gHNNHNQDmD0.8eY14hnG5OOhwfxKKNHbDml7Wzn2c6a', 'xxx@qq.com', '136xxx', '1993-06-11', '1197790560782389250', 1, '1214835832967581698', '2020-10-22 09:15:39', '1214835832967581698', '2020-10-22 09:15:39', NULL);
INSERT INTO `sys_user` VALUES ('1319093485260890113', 'ww', '王五', '$2a$10$zukr/0wKIaeN8dw3X.biAudTDDRmqTI5EeoeriIjug.Ntj2ro7w8m', 'xxx@qq.com', '136xxx', '1994-11-21', '1197790192543469570', 1, '1319084968579817473', '2020-10-22 09:49:29', '1319084968579817473', '2020-10-22 09:49:29', NULL);
INSERT INTO `sys_user` VALUES ('1319093610569916418', 'zl', '赵六', '$2a$10$veZ.csljplWVeYnk6n1AGO7bJq19HZMu9abB5IWQ0J9X5rqXpPpFK', 'xxx@qq.com', '136xxx', '1991-03-04', '1197790192543469570', 1, '1319084968579817473', '2020-10-22 09:49:59', '1319084968579817473', '2020-10-22 09:49:59', NULL);

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户岗位主键ID',
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `post_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`id`, `user_id`, `post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户和岗位关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------
INSERT INTO `sys_user_post` VALUES ('1214835833017913346', '1214835832967581698', '1214825605782228993');
INSERT INTO `sys_user_post` VALUES ('1319084615348117505', '1319084615276814337', '1214825677672599554');
INSERT INTO `sys_user_post` VALUES ('1319084968676286466', '1319084968579817473', '1214825677672599554');
INSERT INTO `sys_user_post` VALUES ('1319093485327998977', '1319093485260890113', '1214825677672599554');
INSERT INTO `sys_user_post` VALUES ('1319093610611859458', '1319093610569916418', '1214825677672599554');
INSERT INTO `sys_user_post` VALUES ('1335762265819500546', '1335761402136809473', '1214825605782228993');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户角色主键ID',
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `role_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`, `user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户和角色关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES ('1214835832975970305', '1214835832967581698', '1214826565321543682');
INSERT INTO `sys_user_role` VALUES ('1319084615310368770', '1319084615276814337', '1319084037507244034');
INSERT INTO `sys_user_role` VALUES ('1319084968609177601', '1319084968579817473', '1319084037507244034');
INSERT INTO `sys_user_role` VALUES ('1319093485302833153', '1319093485260890113', '1319092939179286529');
INSERT INTO `sys_user_role` VALUES ('1319093610590887938', '1319093610569916418', '1319092939179286529');

-- ----------------------------
-- Table structure for tbl_email
-- ----------------------------
DROP TABLE IF EXISTS `tbl_email`;
CREATE TABLE `tbl_email`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `subject` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `text` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '内容',
  `tos` json NULL COMMENT '接收人',
  `ccs` json NULL COMMENT '抄送人',
  `bccs` json NULL COMMENT '密送人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tbl_email
-- ----------------------------

-- ----------------------------
-- Table structure for tbl_file
-- ----------------------------
DROP TABLE IF EXISTS `tbl_file`;
CREATE TABLE `tbl_file`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `original_filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件原始名称',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件名称',
  `file_size` int(11) NULL DEFAULT NULL COMMENT '文件大小',
  `file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '访问路径',
  `content_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件类型',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件来源',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tbl_file
-- ----------------------------

-- ----------------------------
-- Table structure for visual_board
-- ----------------------------
DROP TABLE IF EXISTS `visual_board`;
CREATE TABLE `visual_board`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `board_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '看板名称',
  `board_thumbnail` longtext CHARACTER SET utf16le COLLATE utf16le_general_ci NULL COMMENT '看板缩略图(图片base64)',
  `board_json` json NULL COMMENT '看板配置',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '可视化看板配置信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of visual_board
-- ----------------------------
INSERT INTO `visual_board` VALUES ('1520600871677779970', 1, '1214835832967581698', '2022-05-01 11:07:54', '1197789917762031617', '1214835832967581698', '2022-05-01 12:10:32', NULL, '测试看板1', NULL, '{\"layout\": [{\"h\": 9, \"i\": \"1520600378972889090\", \"w\": 12, \"x\": 0, \"y\": 0}, {\"h\": 9, \"i\": \"1520601035113029633\", \"w\": 12, \"x\": 0, \"y\": 9}], \"interval\": []}');

-- ----------------------------
-- Table structure for visual_board_chart
-- ----------------------------
DROP TABLE IF EXISTS `visual_board_chart`;
CREATE TABLE `visual_board_chart`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `board_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '看板ID',
  `chart_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图表ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '可视化看板和图表关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of visual_board_chart
-- ----------------------------
INSERT INTO `visual_board_chart` VALUES ('1520616636283936769', '1520600871677779970', '1520600378972889090');
INSERT INTO `visual_board_chart` VALUES ('1520616636283936770', '1520600871677779970', '1520601035113029633');

-- ----------------------------
-- Table structure for visual_chart
-- ----------------------------
DROP TABLE IF EXISTS `visual_chart`;
CREATE TABLE `visual_chart`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `chart_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图表名称',
  `chart_thumbnail` longtext CHARACTER SET utf16le COLLATE utf16le_general_ci NULL COMMENT '图表缩略图(图片base64)',
  `chart_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '图表配置',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '可视化图表配置信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of visual_chart
-- ----------------------------
INSERT INTO `visual_chart` VALUES ('1520600378972889090', 1, '1214835832967581698', '2022-05-01 11:05:56', '1197789917762031617', '1214835832967581698', '2022-05-01 11:07:35', NULL, '测试图表1', NULL, '{\"dataSetId\":\"1326047453933334529\",\"chartType\":\"table\",\"rows\":[{\"col\":\"SALES_REGION\",\"alias\":\"区域\"},{\"col\":\"SALES_DISTRICT\",\"alias\":\"地区\"}],\"columns\":[{\"col\":\"SALES_COUNTRY\",\"alias\":\"城市\"}],\"measures\":[{\"col\":\"store_cost\",\"alias\":\"cost\",\"aggregateType\":\"sum\"},{\"col\":\"unit_sales\",\"alias\":\"\",\"aggregateType\":\"sum\"},{\"col\":\"store_sales\",\"alias\":\"\",\"aggregateType\":\"sum\"}],\"filters\":[],\"options\":{\"title\":{\"show\":false,\"text\":\"\",\"subtext\":\"\",\"left\":\"0%\",\"leftVal\":0,\"top\":\"0%\",\"topVal\":0,\"textStyle\":{\"fontSize\":18,\"color\":\"#333\"},\"subtextStyle\":{\"fontSize\":12,\"color\":\"#aaa\"}},\"legend\":{\"show\":true,\"type\":\"plain\",\"left\":\"0%\",\"leftVal\":0,\"top\":\"0%\",\"topVal\":0,\"orient\":\"horizontal\",\"data\":[\"Canada\",\"Mexico\",\"USA\"]}},\"theme\":\"default\"}');
INSERT INTO `visual_chart` VALUES ('1520601035113029633', 1, '1214835832967581698', '2022-05-01 11:08:33', '1197789917762031617', '1214835832967581698', '2022-05-01 11:09:04', NULL, '测试图表2', NULL, '{\"dataSetId\":\"1326047453933334529\",\"chartType\":\"scatter\",\"rows\":[{\"col\":\"SALES_REGION\",\"alias\":\"区域\"},{\"col\":\"SALES_DISTRICT\",\"alias\":\"地区\"}],\"columns\":[{\"col\":\"SALES_COUNTRY\",\"alias\":\"城市\"}],\"measures\":[{\"col\":\"store_cost\",\"alias\":\"cost\",\"aggregateType\":\"sum\"},{\"col\":\"unit_sales\",\"alias\":\"\",\"aggregateType\":\"sum\"},{\"col\":\"store_sales\",\"alias\":\"\",\"aggregateType\":\"sum\"}],\"filters\":[],\"options\":{\"title\":{\"show\":false,\"text\":\"\",\"subtext\":\"\",\"left\":\"0%\",\"leftVal\":0,\"top\":\"0%\",\"topVal\":0,\"textStyle\":{\"fontSize\":18,\"color\":\"#333\"},\"subtextStyle\":{\"fontSize\":12,\"color\":\"#aaa\"}},\"legend\":{\"show\":true,\"type\":\"plain\",\"left\":\"0%\",\"leftVal\":0,\"top\":\"0%\",\"topVal\":0,\"orient\":\"horizontal\",\"data\":[\"Canada\",\"Mexico\",\"USA\"]}},\"theme\":\"default\"}');

-- ----------------------------
-- Table structure for visual_data_set
-- ----------------------------
DROP TABLE IF EXISTS `visual_data_set`;
CREATE TABLE `visual_data_set`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `source_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源',
  `set_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据集名称',
  `set_sql` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据集sql',
  `schema_json` json NULL COMMENT '数据模型定义',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '可视化数据集信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of visual_data_set
-- ----------------------------
INSERT INTO `visual_data_set` VALUES ('1326047453933334529', 1, '1214835832967581698', '2020-11-10 14:22:04', '1197789917762031617', '1214835832967581698', '2022-05-01 09:02:10', NULL, '1240185865539600385', '测试数据集1', 'SELECT\n  b.the_year + 5 AS the_year,\n  b.month_of_year,\n  b.day_of_month,\n  date_add(b.the_date, interval 5 year) AS the_date,\n  r.SALES_DISTRICT,\n  r.SALES_REGION,\n  r.SALES_COUNTRY,\n  d.yearly_income,\n  d.total_children,\n  d.member_card,\n  d.num_cars_owned,\n  d.gender,\n  a.store_sales,\n  a.store_cost,\n  a.unit_sales\nFROM\n  foodmart2.sales_fact_sample a\n  JOIN foodmart2.time_by_day b ON a.time_id = b.time_id\n  JOIN foodmart2.store c ON a.store_id = c.store_id\n  JOIN foodmart2.region r ON c.REGION_ID = r.REGION_ID\n  JOIN foodmart2.customer d ON a.CUSTOMER_ID = d.CUSTOMER_ID\nWHERE\n  SALES_COUNTRY IS NOT NULL', '{\"columns\": [\"the_year\", \"month_of_year\", \"day_of_month\", \"the_date\", \"SALES_DISTRICT\", \"SALES_REGION\", \"SALES_COUNTRY\", \"yearly_income\", \"total_children\", \"member_card\", \"num_cars_owned\", \"gender\", \"store_sales\", \"store_cost\", \"unit_sales\"], \"measures\": [{\"col\": \"store_cost\", \"alias\": \"cost\"}, {\"col\": \"store_sales\", \"alias\": \"\"}, {\"col\": \"unit_sales\", \"alias\": \"\"}], \"dimensions\": [{\"col\": \"SALES_DISTRICT\", \"alias\": \"地区\"}, {\"col\": \"SALES_REGION\", \"alias\": \"区域\"}, {\"col\": \"SALES_COUNTRY\", \"alias\": \"城市\"}]}');

-- ----------------------------
-- Table structure for visual_screen
-- ----------------------------
DROP TABLE IF EXISTS `visual_screen`;
CREATE TABLE `visual_screen`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态（0不启用，1启用）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `create_dept` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人所属部门',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新日期',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `screen_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '酷屏名称',
  `screen_thumbnail` text CHARACTER SET utf16le COLLATE utf16le_general_ci NULL COMMENT '酷屏缩略图(图片base64)',
  `screen_json` json NULL COMMENT '酷屏配置',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '可视化酷屏配置信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of visual_screen
-- ----------------------------
INSERT INTO `visual_screen` VALUES ('1520607338275332097', 1, '1214835832967581698', '2022-05-01 11:33:36', '1197789917762031617', '1214835832967581698', '2022-05-01 12:03:36', NULL, '测试酷屏1', NULL, '{\"scale\": 100, \"width\": 1920, \"height\": 1080, \"layout\": [{\"h\": 420, \"i\": \"1520601035113029633\", \"w\": 860, \"x\": 200, \"y\": 100}, {\"h\": 340, \"i\": \"1520600378972889090\", \"w\": 1080, \"x\": 120, \"y\": 560}], \"property\": [{\"id\": \"1520601035113029633\", \"border\": \"BorderBox1\", \"chartName\": \"测试图表2\", \"milliseconds\": 10000, \"backgroundColor\": \"rgba(221, 21, 21, 0.1)\"}, {\"id\": \"1520600378972889090\", \"border\": \"BorderBox1\", \"chartName\": \"测试图表1\", \"milliseconds\": 5000, \"backgroundColor\": \"rgba(157, 194, 108, 0.1)\"}], \"backgroundImage\": \"images/bg2.png\"}');

-- ----------------------------
-- Table structure for visual_screen_chart
-- ----------------------------
DROP TABLE IF EXISTS `visual_screen_chart`;
CREATE TABLE `visual_screen_chart`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `screen_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '酷屏ID',
  `chart_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图表ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '可视化酷屏和图表关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of visual_screen_chart
-- ----------------------------
INSERT INTO `visual_screen_chart` VALUES ('1520614887791845377', '1520607338275332097', '1520601035113029633');
INSERT INTO `visual_screen_chart` VALUES ('1520614887800233986', '1520607338275332097', '1520600378972889090');

SET FOREIGN_KEY_CHECKS = 1;

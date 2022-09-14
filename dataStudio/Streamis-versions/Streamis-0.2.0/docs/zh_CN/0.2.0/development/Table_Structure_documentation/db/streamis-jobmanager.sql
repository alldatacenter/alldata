
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for linkis_stream_bml
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_bml`;
CREATE TABLE `linkis_stream_bml`  (
  `id` bigint(20) NOT NULL,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `bml_type` tinyint(1) NULL DEFAULT NULL,
  `org_identification` bigint(20) NULL DEFAULT NULL,
  ` latest_version` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_bml
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_bml_version
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_bml_version`;
CREATE TABLE `linkis_stream_bml_version`  (
  `id` bigint(20) NOT NULL,
  `bml_id` bigint(20) NULL DEFAULT NULL,
  `version` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `storage_path` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  ` attribute` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '物料版本' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_bml_version
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_cluster
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_cluster`;
CREATE TABLE `linkis_stream_cluster`  (
  `id` int(11) NOT NULL,
  `yarn_conf_dir` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `hdfs_conf_dir` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `resource_manager_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `savepoint_dir` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = 'flink 集群信息' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_cluster
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_configuration_config_key
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_configuration_config_key`;
CREATE TABLE `linkis_stream_configuration_config_key`  (
  `id` bigint(20) NOT NULL,
  `key` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `default_value` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `validate_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `validate_range` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `is_hidden` tinyint(1) NULL DEFAULT NULL,
  `is_advanced` tinyint(1) NULL DEFAULT NULL,
  `level` tinyint(1) NULL DEFAULT NULL,
  `treename` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `type` int(10) NULL DEFAULT NULL,
  `sort` int(10) NULL DEFAULT NULL,
  `status` tinyint(10) NULL DEFAULT NULL COMMENT '1 custom , 2 selected ',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `key_index`(`key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '配置信息' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_configuration_config_key
-- ----------------------------
INSERT INTO `linkis_stream_configuration_config_key` VALUES (1, 'wds.linkis.flink.resource', '资源配置', '资源配置', NULL, 'None', NULL, 0, 0, 1, '资源配置', 1, 0, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (2, 'wds.linkis.flink.taskmanager.num', 'Task Managers数量', 'Task Managers数量', '4', 'Regex', '^(?:[1-9]\\d?|[1234]\\d{2}|128)$', 0, 0, 2, '资源配置', 1, 1, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (3, 'wds.linkis.flink.jobmanager.memory', 'JobManager Memory', 'JobManager Memory', '1.5', 'Regex', '^([1-9]\\d{0,2}|1000)(G|g)$', 0, 0, 2, '资源配置', 1, 2, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (4, 'wds.linkis.flink.taskmanager.memory', 'TaskManager Memory', 'TaskManager Memory', '1.5', 'Regex', '^([1-9]\\d{0,2}|1000)(G|g)$', 0, 0, 2, '资源配置', 1, 3, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (5, 'wds.linkis.flink.jobmanager.cpus', 'JobManager CPUs', 'JobManager CPUs', '1', 'Regex', '^(?:[1-9]\\d?|[1234]\\d{2}|128)$', 0, 0, 2, '资源配置', 1, 4, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (6, 'wds.linkis.flink.taskManager.cpus', 'TaskManager CPUs', 'TaskManager CPUs', '1', 'Regex', '^(?:[1-9]\\d?|[1234]\\d{2}|128)$', 0, 0, 2, '资源配置', 1, 5, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (7, 'wds.linkis.flink.custom', '自定义参数', '自定义参数', NULL, 'None', NULL, 0, 0, 1, '自定义参数', 2, 0, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (8, 'wds.linkis.flink.produce', '生产配置', '生产配置', NULL, 'None', NULL, 0, 0, 1, '生产配置', 3, 0, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (9, 'wds.linkis.flink.checkpoint.interval', 'Checkpoint间隔', 'Checkpoint间隔', NULL, NULL, NULL, 0, 0, 2, '生产配置', 3, 1, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (10, 'wds.linkis.flink.reboot.strategy', '重启策略', '重启策略', '不重启,基于Checkpoint自动重启,无Checkpoint不重启', 'None', NULL, 0, 0, 2, '重启策略', 3, 2, 2);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (11, 'wds.linkis.flink.alert', '告警设置', '告警设置', NULL, 'None', NULL, 0, 0, 1, '告警设置', 4, 0, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (12, 'wds.linkis.flink.alert.rule', '告警规则', '告警规则', '任务日志中出现ERROR/EXCEPTION,任务核心指标出现异常', 'None', NULL, 0, 0, 2, '告警规则', 4, 1, 2);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (13, 'wds.linkis.flink.alert.user', '告警用户', '告警用户', NULL, NULL, NULL, 0, 0, 2, '告警用户', 4, 3, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (14, 'wds.linkis.flink.alert.leve', '告警级别', '告警级别', 'CLEARED,INDETERMINATE,WARNING,MINOR,MAJOR,CRITICAL', 'None', NULL, 0, 0, 2, '告警级别', 4, 2, 2);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (15, 'wds.linkis.flink.alert.failure.level', '失败时告警级别', '失败时告警级别', 'CLEARED,INDETERMINATE,WARNING,MINOR,MAJOR,CRITICAL', 'None', NULL, 0, 0, 2, '失败时告警级别', 4, 4, 2);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (16, 'wds.linkis.flink.alert.failure.user', '失败时告警用户', '失败时告警用户', NULL, 'None', NULL, 0, 0, 2, '失败时告警用户', 4, 5, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (17, 'wds.linkis.flink.authority', '权限设置', '权限设置', NULL, 'None', NULL, 0, 0, 1, '权限设置', 5, 0, 1);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (18, 'wds.linkis.flink.authority.author', '授权模式', '授权模式', '私密,指定全员可见,指定人员可见', 'None', NULL, 0, 0, 2, '授权模式', 5, 1, 2);
INSERT INTO `linkis_stream_configuration_config_key` VALUES (19, 'wds.linkis.flink.authority.visible', '可见人员', '可见人员', NULL, 'None', NULL, 0, 0, 2, '可见人员', 5, 2, 1);

-- ----------------------------
-- Table structure for linkis_stream_configuration_config_value
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_configuration_config_value`;
CREATE TABLE `linkis_stream_configuration_config_value`  (
  `id` bigint(20) NOT NULL,
  `configkey_id` bigint(20) NULL DEFAULT NULL,
  `config_value` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `type` int(10) NULL DEFAULT NULL,
  `job_id` bigint(20) NULL DEFAULT NULL,
  `job_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `config_key` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `key`(`config_key`) USING BTREE,
  INDEX `keyid`(`configkey_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '配置信息' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_configuration_config_value
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_frame_version
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_frame_version`;
CREATE TABLE `linkis_stream_frame_version`  (
  `id` bigint(20) NOT NULL,
  `frame` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `version` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `java_version` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '框架信息' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of linkis_stream_frame_version
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_job
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_job`;
CREATE TABLE `linkis_stream_job`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` bigint(20) NULL DEFAULT NULL,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `type` tinyint(1) NULL DEFAULT NULL,
  `current_task_id` bigint(20) NULL DEFAULT NULL,
  `current_version` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `current_release_time` datetime NULL DEFAULT NULL,
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '1:已完成 ，2:等待重启 ，3:告警 ，4:慢任务 ，5:运行中 ，6:失败任务',
  `org_identification` bigint(20) NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `label` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `current_released` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '作业表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_job
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_job_alarm_send_history
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_job_alarm_send_history`;
CREATE TABLE `linkis_stream_job_alarm_send_history`  (
  `id` bigint(20) NOT NULL,
  `job_id` bigint(20) NULL DEFAULT NULL,
  `task_id` bigint(20) NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `type` tinyint(1) NULL DEFAULT NULL,
  `rule_type` tinyint(1) NULL DEFAULT NULL,
  `content` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '报警历史信息' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_job_alarm_send_history
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_job_checkpoints
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_job_checkpoints`;
CREATE TABLE `linkis_stream_job_checkpoints`  (
  `id` bigint(20) NOT NULL,
  `config_value_id` bigint(20) NULL DEFAULT NULL,
  `path` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `size` int(20) NULL DEFAULT NULL,
  `status` tinyint(1) NULL DEFAULT NULL,
  `trigger_timestamp` datetime NULL DEFAULT NULL,
  `latest_ack_timestamp` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_job_checkpoints
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_job_code_resource
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_job_code_resource`;
CREATE TABLE `linkis_stream_job_code_resource`  (
  `id` bigint(20) NOT NULL,
  `job_version_id` bigint(20) NULL DEFAULT NULL,
  `bml_version_id` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '其他代码' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_job_code_resource
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_job_role
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_job_role`;
CREATE TABLE `linkis_stream_job_role`  (
  `id` bigint(20) NOT NULL,
  `job_id` bigint(20) NULL DEFAULT NULL,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `front_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_job_role
-- ----------------------------
INSERT INTO `linkis_stream_job_role` VALUES (1, -1, '管理员', '管理员', '2021-04-07 20:57:09', NULL);

-- ----------------------------
-- Table structure for linkis_stream_job_sql_resource
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_job_sql_resource`;
CREATE TABLE `linkis_stream_job_sql_resource`  (
  `id` bigint(20) NOT NULL,
  `job_version_id` bigint(20) NULL DEFAULT NULL,
  `execute_sql` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_job_sql_resource
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_job_user_role
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_job_user_role`;
CREATE TABLE `linkis_stream_job_user_role`  (
  `id` bigint(20) NOT NULL,
  `job_id` bigint(20) NULL DEFAULT NULL,
  `user_id` bigint(20) NULL DEFAULT NULL,
  `role_id` bigint(20) NULL DEFAULT NULL,
  `type` tinyint(1) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '作业角色关系' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_job_user_role
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_job_version
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_job_version`;
CREATE TABLE `linkis_stream_job_version`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  ` job_id` bigint(20) NULL DEFAULT NULL,
  `version` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `program_arguments` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `bml_version` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `bml_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_job_version
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_project
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_project`;
CREATE TABLE `linkis_stream_project`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `workspace_id` bigint(20) NULL DEFAULT NULL,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '项目表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_project
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_task
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_task`;
CREATE TABLE `linkis_stream_task` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `job_version_id` bigint(20) NOT NULL,
   `job_id` varchar(50) DEFAULT NULL,
   `version` varchar(50) DEFAULT NULL,
   `status` int(3) DEFAULT NULL,
   `start_time` datetime DEFAULT NULL,
   `last_update_time` datetime DEFAULT NULL,
   `end_time` datetime DEFAULT NULL,
   `err_desc` varchar(10240) DEFAULT NULL,
   `submit_user` varchar(50) DEFAULT NULL,
   `linkis_job_id` varchar(50) DEFAULT NULL,
   `linkis_job_info` mediumtext,
   PRIMARY KEY (`id`) USING BTREE
 ) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='任务表'

-- ----------------------------
-- Records of linkis_stream_task
-- ----------------------------

-- ----------------------------
-- Table structure for linkis_stream_user
-- ----------------------------
DROP TABLE IF EXISTS `linkis_stream_user`;
CREATE TABLE `linkis_stream_user`  (
  `id` bigint(20) NOT NULL,
  `username` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of linkis_stream_user
-- ----------------------------
INSERT INTO `linkis_stream_user` VALUES (1, 'hdfs', 'hdfs');

SET FOREIGN_KEY_CHECKS = 1;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` int(20) NOT NULL AUTO_INCREMENT,
  `type` int(2) NOT NULL,
  `role_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for role_user_relation
-- ----------------------------
DROP TABLE IF EXISTS `role_user_relation`;
CREATE TABLE `role_user_relation`  (
  `id` int(20) NOT NULL AUTO_INCREMENT,
  `role_id` int(20) NOT NULL,
  `user_id` int(20) NOT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for scheduler_config
-- ----------------------------
DROP TABLE IF EXISTS `scheduler_config`;
CREATE TABLE `scheduler_config`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `script_id` int(11) NULL DEFAULT NULL,
  `trigger_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `retry_times` int(11) NOT NULL DEFAULT 0,
  `retry_interval` int(11) NOT NULL DEFAULT 0,
  `active_start_time` datetime(3) NOT NULL,
  `active_end_time` datetime(3) NOT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `creator_id` int(11) NOT NULL,
  `update_id` int(11) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for script
-- ----------------------------
DROP TABLE IF EXISTS `script`;
CREATE TABLE `script`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` tinyint(4) NOT NULL,
  `status` tinyint(4) NOT NULL,
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `content_md5` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `creator_id` int(11) NOT NULL,
  `mender_id` int(11) NOT NULL,
  `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for script_job_apply
-- ----------------------------
DROP TABLE IF EXISTS `script_job_apply`;
CREATE TABLE `script_job_apply`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `script_id` int(11) NOT NULL,
  `scheduler_config_id` int(11) NOT NULL,
  `job_id` bigint(20) NULL DEFAULT NULL,
  `operator_id` int(11) NOT NULL,
  `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for script_param
-- ----------------------------
DROP TABLE IF EXISTS `script_param`;
CREATE TABLE `script_param`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `script_id` int(11) NULL DEFAULT NULL,
  `key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` tinyint(4) NULL DEFAULT NULL,
  `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_ds_process_definition
-- ----------------------------
DROP TABLE IF EXISTS `t_ds_process_definition`;
CREATE TABLE `t_ds_process_definition`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'self-increasing id',
  `code` bigint(20) NOT NULL COMMENT 'encoding',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT 'process definition name',
  `version` int(11) NULL DEFAULT 0 COMMENT 'process definition version',
  `description` text CHARACTER SET utf8 COLLATE utf8_bin NULL COMMENT 'description',
  `release_state` tinyint(4) NULL DEFAULT NULL COMMENT 'process definition release stateï¼š0:offline,1:online',
  `user_id` int(11) NULL DEFAULT NULL COMMENT 'process definition creator id',
  `global_params` text CHARACTER SET utf8 COLLATE utf8_bin NULL COMMENT 'global parameters',
  `flag` tinyint(4) NULL DEFAULT NULL COMMENT '0 not available, 1 available',
  `locations` longtext CHARACTER SET utf8 COLLATE utf8_bin NULL COMMENT 'Node location information',
  `warning_group_id` int(11) NULL DEFAULT NULL COMMENT 'alert group id',
  `timeout` int(11) NULL DEFAULT 0 COMMENT 'time out, unit: minute',
  `tenant_id` int(11) NOT NULL DEFAULT -1 COMMENT 'tenant id',
  `execution_type` tinyint(4) NULL DEFAULT 0 COMMENT 'execution_type 0:parallel,1:serial wait,2:serial discard,3:serial priority',
  `create_time` datetime NOT NULL COMMENT 'create time',
  `update_time` datetime NOT NULL COMMENT 'update time',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_ds_process_task_relation
-- ----------------------------
DROP TABLE IF EXISTS `t_ds_process_task_relation`;
CREATE TABLE `t_ds_process_task_relation`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'self-increasing id',
  `name` varchar(200) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT 'relation name',
  `project_code` bigint(20) NOT NULL COMMENT 'project code',
  `process_definition_code` bigint(20) NOT NULL COMMENT 'process code',
  `process_definition_version` int(11) NOT NULL COMMENT 'process version',
  `pre_task_code` bigint(20) NOT NULL COMMENT 'pre task code',
  `pre_task_version` int(11) NOT NULL COMMENT 'pre task version',
  `post_task_code` bigint(20) NOT NULL COMMENT 'post task code',
  `post_task_version` int(11) NOT NULL COMMENT 'post task version',
  `condition_type` tinyint(2) NULL DEFAULT NULL COMMENT 'condition type : 0 none, 1 judge 2 delay',
  `condition_params` text CHARACTER SET utf8 COLLATE utf8_bin NULL COMMENT 'condition params(json)',
  `create_time` datetime NOT NULL COMMENT 'create time',
  `update_time` datetime NOT NULL COMMENT 'update time',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_code`(`project_code`, `process_definition_code`) USING BTREE,
  INDEX `idx_pre_task_code_version`(`pre_task_code`, `pre_task_version`) USING BTREE,
  INDEX `idx_post_task_code_version`(`post_task_code`, `post_task_version`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_ds_process_task_relation_log
-- ----------------------------
DROP TABLE IF EXISTS `t_ds_process_task_relation_log`;
CREATE TABLE `t_ds_process_task_relation_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'self-increasing id',
  `name` varchar(200) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT 'relation name',
  `project_code` bigint(20) NOT NULL COMMENT 'project code',
  `process_definition_code` bigint(20) NOT NULL COMMENT 'process code',
  `process_definition_version` int(11) NOT NULL COMMENT 'process version',
  `pre_task_code` bigint(20) NOT NULL COMMENT 'pre task code',
  `pre_task_version` int(11) NOT NULL COMMENT 'pre task version',
  `post_task_code` bigint(20) NOT NULL COMMENT 'post task code',
  `post_task_version` int(11) NOT NULL COMMENT 'post task version',
  `condition_type` tinyint(2) NULL DEFAULT NULL COMMENT 'condition type : 0 none, 1 judge 2 delay',
  `condition_params` text CHARACTER SET utf8 COLLATE utf8_bin NULL COMMENT 'condition params(json)',
  `operator` int(11) NULL DEFAULT NULL COMMENT 'operator user id',
  `operate_time` datetime NULL DEFAULT NULL COMMENT 'operate time',
  `create_time` datetime NOT NULL COMMENT 'create time',
  `update_time` datetime NOT NULL COMMENT 'update time',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_process_code_version`(`process_definition_code`, `process_definition_version`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_ds_task_definition
-- ----------------------------
DROP TABLE IF EXISTS `t_ds_task_definition`;
CREATE TABLE `t_ds_task_definition`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'self-increasing id',
  `code` bigint(20) NOT NULL COMMENT 'encoding',
  `name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'task definition name',
  `version` int(11) NULL DEFAULT 0 COMMENT 'task definition version',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT 'description',
  `user_id` int(11) NULL DEFAULT NULL COMMENT 'task definition creator id',
  `task_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'task type',
  `task_params` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT 'job custom parameters',
  `flag` tinyint(2) NULL DEFAULT NULL COMMENT '0 not available, 1 available',
  `task_priority` tinyint(4) NULL DEFAULT 2 COMMENT 'job priority',
  `worker_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'worker grouping',
  `environment_code` bigint(20) NULL DEFAULT -1 COMMENT 'environment code',
  `fail_retry_times` int(11) NULL DEFAULT NULL COMMENT 'number of failed retries',
  `fail_retry_interval` int(11) NULL DEFAULT NULL COMMENT 'failed retry interval',
  `timeout_flag` tinyint(2) NULL DEFAULT 0 COMMENT 'timeout flag:0 close, 1 open',
  `timeout_notify_strategy` tinyint(4) NULL DEFAULT NULL COMMENT 'timeout notification policy: 0 warning, 1 fail',
  `timeout` int(11) NULL DEFAULT 0 COMMENT 'timeout length,unit: minute',
  `delay_time` int(11) NULL DEFAULT 0 COMMENT 'delay execution time,unit: minute',
  `resource_ids` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT 'resource id, separated by comma',
  `task_group_id` int(11) NULL DEFAULT NULL COMMENT 'task group id',
  `task_group_priority` tinyint(4) NULL DEFAULT 0 COMMENT 'task group priority',
  `create_time` datetime NOT NULL COMMENT 'create time',
  `update_time` datetime NOT NULL COMMENT 'update time',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_ds_task_definition_log
-- ----------------------------
DROP TABLE IF EXISTS `t_ds_task_definition_log`;
CREATE TABLE `t_ds_task_definition_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'self-increasing id',
  `code` bigint(20) NOT NULL COMMENT 'encoding',
  `name` varchar(200) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT 'task definition name',
  `version` int(11) NULL DEFAULT 0 COMMENT 'task definition version',
  `description` text CHARACTER SET utf8 COLLATE utf8_bin NULL COMMENT 'description',
  `user_id` int(11) NULL DEFAULT NULL COMMENT 'task definition creator id',
  `task_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT 'task type',
  `task_params` longtext CHARACTER SET utf8 COLLATE utf8_bin NULL COMMENT 'job custom parameters',
  `flag` tinyint(2) NULL DEFAULT NULL COMMENT '0 not available, 1 available',
  `task_priority` tinyint(4) NULL DEFAULT 2 COMMENT 'job priority',
  `worker_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT 'worker grouping',
  `environment_code` bigint(20) NULL DEFAULT -1 COMMENT 'environment code',
  `fail_retry_times` int(11) NULL DEFAULT NULL COMMENT 'number of failed retries',
  `fail_retry_interval` int(11) NULL DEFAULT NULL COMMENT 'failed retry interval',
  `timeout_flag` tinyint(2) NULL DEFAULT 0 COMMENT 'timeout flag:0 close, 1 open',
  `timeout_notify_strategy` tinyint(4) NULL DEFAULT NULL COMMENT 'timeout notification policy: 0 warning, 1 fail',
  `timeout` int(11) NULL DEFAULT 0 COMMENT 'timeout length,unit: minute',
  `delay_time` int(11) NULL DEFAULT 0 COMMENT 'delay execution time,unit: minute',
  `resource_ids` text CHARACTER SET utf8 COLLATE utf8_bin NULL COMMENT 'resource id, separated by comma',
  `operator` int(11) NULL DEFAULT NULL COMMENT 'operator user id',
  `task_group_id` int(11) NULL DEFAULT NULL COMMENT 'task group id',
  `task_group_priority` tinyint(4) NULL DEFAULT 0 COMMENT 'task group priority',
  `operate_time` datetime NULL DEFAULT NULL COMMENT 'operate time',
  `create_time` datetime NOT NULL COMMENT 'create time',
  `update_time` datetime NOT NULL COMMENT 'update time',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_code_version`(`code`, `version`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_st_datasource
-- ----------------------------
DROP TABLE IF EXISTS `t_st_datasource`;
CREATE TABLE `t_st_datasource`  (
  `id` bigint(20) NOT NULL,
  `datasource_name` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `plugin_name` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `plugin_version` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '1.0.0',
  `datasource_config` varchar(1023) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `description` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `create_user_id` int(11) NOT NULL,
  `update_user_id` int(11) NOT NULL,
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `t_st_datasource_datasource_name_uindex`(`datasource_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_st_job_definition
-- ----------------------------
DROP TABLE IF EXISTS `t_st_job_definition`;
CREATE TABLE `t_st_job_definition`  (
  `id` bigint(20) NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `job_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `create_user_id` int(11) NOT NULL,
  `update_user_id` int(11) NOT NULL,
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_st_job_instance
-- ----------------------------
DROP TABLE IF EXISTS `t_st_job_instance`;
CREATE TABLE `t_st_job_instance`  (
  `id` bigint(20) NOT NULL,
  `job_define_id` bigint(20) NOT NULL,
  `job_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `job_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `engine_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `engine_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `job_engine_id` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `create_user_id` int(20) NOT NULL,
  `update_user_id` int(20) NULL DEFAULT NULL,
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `end_time` timestamp(3) NULL DEFAULT NULL,
  `job_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_st_job_instance_history
-- ----------------------------
DROP TABLE IF EXISTS `t_st_job_instance_history`;
CREATE TABLE `t_st_job_instance_history`  (
  `id` bigint(20) NOT NULL,
  `dag` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_st_job_line
-- ----------------------------
DROP TABLE IF EXISTS `t_st_job_line`;
CREATE TABLE `t_st_job_line`  (
  `id` bigint(20) NOT NULL,
  `version_id` bigint(20) NOT NULL,
  `input_plugin_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `target_plugin_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `job_line_version_index`(`version_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_st_job_metrics
-- ----------------------------
DROP TABLE IF EXISTS `t_st_job_metrics`;
CREATE TABLE `t_st_job_metrics`  (
  `id` bigint(20) NOT NULL,
  `job_instance_id` bigint(20) NOT NULL,
  `pipeline_id` int(20) NOT NULL,
  `read_row_count` bigint(20) NOT NULL,
  `write_row_count` bigint(20) NOT NULL,
  `source_table_names` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `sink_table_names` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `read_qps` bigint(20) NULL DEFAULT NULL,
  `write_qps` bigint(20) NULL DEFAULT NULL,
  `record_delay` bigint(20) NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `create_user_id` int(20) NOT NULL,
  `update_user_id` int(20) NULL DEFAULT NULL,
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_st_job_task
-- ----------------------------
DROP TABLE IF EXISTS `t_st_job_task`;
CREATE TABLE `t_st_job_task`  (
  `id` bigint(20) NOT NULL,
  `version_id` bigint(20) NOT NULL,
  `plugin_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
  `transform_options` varchar(5000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `output_schema` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
  `connector_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `datasource_id` bigint(20) NULL DEFAULT NULL,
  `datasource_option` varchar(5000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `select_table_fields` varchar(5000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `scene_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `job_task_plugin_id_index`(`plugin_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_st_job_version
-- ----------------------------
DROP TABLE IF EXISTS `t_st_job_version`;
CREATE TABLE `t_st_job_version`  (
  `id` bigint(20) NOT NULL,
  `job_id` bigint(20) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `job_mode` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `env` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
  `engine_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `engine_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `create_user_id` int(11) NOT NULL,
  `update_user_id` int(11) NOT NULL,
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_st_virtual_table
-- ----------------------------
DROP TABLE IF EXISTS `t_st_virtual_table`;
CREATE TABLE `t_st_virtual_table`  (
  `id` bigint(20) NOT NULL,
  `datasource_id` bigint(20) NOT NULL,
  `virtual_database_name` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `virtual_table_name` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `table_fields` varchar(1023) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `virtual_table_config` varchar(1023) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `description` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `create_user_id` int(11) NOT NULL,
  `update_user_id` int(11) NOT NULL,
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` tinyint(4) NOT NULL,
  `type` tinyint(4) NOT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_login_log
-- ----------------------------
DROP TABLE IF EXISTS `user_login_log`;
CREATE TABLE `user_login_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `token` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `token_status` tinyint(1) NOT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 106 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
CREATE TABLE `QRTZ_BLOB_TRIGGERS` (
    `SCHED_NAME` varchar(120) NOT NULL,
    `TRIGGER_NAME` varchar(200) NOT NULL,
    `TRIGGER_GROUP` varchar(200) NOT NULL,
    `BLOB_DATA` blob,
    PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
    KEY `SCHED_NAME` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
CREATE TABLE `QRTZ_CALENDARS` (
    `SCHED_NAME` varchar(120) NOT NULL,
    `CALENDAR_NAME` varchar(200) NOT NULL,
    `CALENDAR` blob NOT NULL,
    PRIMARY KEY (`SCHED_NAME`,`CALENDAR_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
CREATE TABLE `QRTZ_CRON_TRIGGERS` (
    `SCHED_NAME` varchar(120) NOT NULL,
    `TRIGGER_NAME` varchar(200) NOT NULL,
    `TRIGGER_GROUP` varchar(200) NOT NULL,
    `CRON_EXPRESSION` varchar(120) NOT NULL,
    `TIME_ZONE_ID` varchar(80) DEFAULT NULL,
    PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
CREATE TABLE `QRTZ_FIRED_TRIGGERS` (
    `SCHED_NAME` varchar(120) NOT NULL,
    `ENTRY_ID` varchar(200) NOT NULL,
    `TRIGGER_NAME` varchar(200) NOT NULL,
    `TRIGGER_GROUP` varchar(200) NOT NULL,
    `INSTANCE_NAME` varchar(200) NOT NULL,
    `FIRED_TIME` bigint(13) NOT NULL,
    `SCHED_TIME` bigint(13) NOT NULL,
    `PRIORITY` int(11) NOT NULL,
    `STATE` varchar(16) NOT NULL,
    `JOB_NAME` varchar(200) DEFAULT NULL,
    `JOB_GROUP` varchar(200) DEFAULT NULL,
    `IS_NONCONCURRENT` varchar(1) DEFAULT NULL,
    `REQUESTS_RECOVERY` varchar(1) DEFAULT NULL,
    PRIMARY KEY (`SCHED_NAME`,`ENTRY_ID`),
    KEY `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`,`INSTANCE_NAME`),
    KEY `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`,`INSTANCE_NAME`,`REQUESTS_RECOVERY`),
    KEY `IDX_QRTZ_FT_J_G` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
    KEY `IDX_QRTZ_FT_JG` (`SCHED_NAME`,`JOB_GROUP`),
    KEY `IDX_QRTZ_FT_T_G` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
    KEY `IDX_QRTZ_FT_TG` (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
CREATE TABLE `QRTZ_JOB_DETAILS` (
    `SCHED_NAME` varchar(120) NOT NULL,
    `JOB_NAME` varchar(200) NOT NULL,
    `JOB_GROUP` varchar(200) NOT NULL,
    `DESCRIPTION` varchar(250) DEFAULT NULL,
    `JOB_CLASS_NAME` varchar(250) NOT NULL,
    `IS_DURABLE` varchar(1) NOT NULL,
    `IS_NONCONCURRENT` varchar(1) NOT NULL,
    `IS_UPDATE_DATA` varchar(1) NOT NULL,
    `REQUESTS_RECOVERY` varchar(1) NOT NULL,
    `JOB_DATA` blob,
    PRIMARY KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
    KEY `IDX_QRTZ_J_REQ_RECOVERY` (`SCHED_NAME`,`REQUESTS_RECOVERY`),
    KEY `IDX_QRTZ_J_GRP` (`SCHED_NAME`,`JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `QRTZ_LOCKS`;
CREATE TABLE `QRTZ_LOCKS` (
    `SCHED_NAME` varchar(120) NOT NULL,
    `LOCK_NAME` varchar(40) NOT NULL,
    PRIMARY KEY (`SCHED_NAME`,`LOCK_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS` (
    `SCHED_NAME` varchar(120) NOT NULL,
    `TRIGGER_GROUP` varchar(200) NOT NULL,
    PRIMARY KEY (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
CREATE TABLE `QRTZ_SCHEDULER_STATE` (
    `SCHED_NAME` varchar(120) NOT NULL,
    `INSTANCE_NAME` varchar(200) NOT NULL,
    `LAST_CHECKIN_TIME` bigint(13) NOT NULL,
    `CHECKIN_INTERVAL` bigint(13) NOT NULL,
    PRIMARY KEY (`SCHED_NAME`,`INSTANCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPLE_TRIGGERS` (
    `SCHED_NAME` varchar(120) NOT NULL,
    `TRIGGER_NAME` varchar(200) NOT NULL,
    `TRIGGER_GROUP` varchar(200) NOT NULL,
    `REPEAT_COUNT` bigint(7) NOT NULL,
    `REPEAT_INTERVAL` bigint(12) NOT NULL,
    `TIMES_TRIGGERED` bigint(10) NOT NULL,
    PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPROP_TRIGGERS` (
    `SCHED_NAME` varchar(120) NOT NULL,
    `TRIGGER_NAME` varchar(200) NOT NULL,
    `TRIGGER_GROUP` varchar(200) NOT NULL,
    `STR_PROP_1` varchar(512) DEFAULT NULL,
    `STR_PROP_2` varchar(512) DEFAULT NULL,
    `STR_PROP_3` varchar(512) DEFAULT NULL,
    `INT_PROP_1` int(11) DEFAULT NULL,
    `INT_PROP_2` int(11) DEFAULT NULL,
    `LONG_PROP_1` bigint(20) DEFAULT NULL,
    `LONG_PROP_2` bigint(20) DEFAULT NULL,
    `DEC_PROP_1` decimal(13,4) DEFAULT NULL,
    `DEC_PROP_2` decimal(13,4) DEFAULT NULL,
    `BOOL_PROP_1` varchar(1) DEFAULT NULL,
    `BOOL_PROP_2` varchar(1) DEFAULT NULL,
    PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
CREATE TABLE `QRTZ_TRIGGERS` (
     `SCHED_NAME` varchar(120) NOT NULL,
     `TRIGGER_NAME` varchar(200) NOT NULL,
     `TRIGGER_GROUP` varchar(200) NOT NULL,
     `JOB_NAME` varchar(200) NOT NULL,
     `JOB_GROUP` varchar(200) NOT NULL,
     `DESCRIPTION` varchar(250) DEFAULT NULL,
     `NEXT_FIRE_TIME` bigint(13) DEFAULT NULL,
     `PREV_FIRE_TIME` bigint(13) DEFAULT NULL,
     `PRIORITY` int(11) DEFAULT NULL,
     `TRIGGER_STATE` varchar(16) NOT NULL,
     `TRIGGER_TYPE` varchar(8) NOT NULL,
     `START_TIME` bigint(13) NOT NULL,
     `END_TIME` bigint(13) DEFAULT NULL,
     `CALENDAR_NAME` varchar(200) DEFAULT NULL,
     `MISFIRE_INSTR` smallint(2) DEFAULT NULL,
     `JOB_DATA` blob,
     PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
     KEY `IDX_QRTZ_T_J` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
     KEY `IDX_QRTZ_T_JG` (`SCHED_NAME`,`JOB_GROUP`),
     KEY `IDX_QRTZ_T_C` (`SCHED_NAME`,`CALENDAR_NAME`),
     KEY `IDX_QRTZ_T_G` (`SCHED_NAME`,`TRIGGER_GROUP`),
     KEY `IDX_QRTZ_T_STATE` (`SCHED_NAME`,`TRIGGER_STATE`),
     KEY `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
     KEY `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
     KEY `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`,`NEXT_FIRE_TIME`),
     KEY `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`,`TRIGGER_STATE`,`NEXT_FIRE_TIME`),
     KEY `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`),
     KEY `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_STATE`),
     KEY `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
     CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `QRTZ_JOB_DETAILS` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for dv_actual_values
-- ----------------------------
DROP TABLE IF EXISTS `dv_actual_values`;
CREATE TABLE `dv_actual_values` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `job_execution_id` bigint(20) DEFAULT NULL COMMENT '规则作业运行实例ID',
  `metric_name` varchar(255) DEFAULT NULL COMMENT '规则名称',
  `unique_code` varchar(255) DEFAULT NULL COMMENT '规则唯一编码',
  `actual_value` double DEFAULT NULL COMMENT '实际值',
  `data_time` datetime DEFAULT NULL COMMENT '数据时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则运行结果实际值';

-- ----------------------------
-- Table structure for dv_catalog_metadata_fetch_command
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_metadata_fetch_command`;
CREATE TABLE `dv_catalog_metadata_fetch_command` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_id` bigint(20) NOT NULL COMMENT '元数据抓取任务ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据抓取任务命令';

-- ----------------------------
-- Table structure for dv_catalog_entity_definition
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_entity_definition`;
CREATE TABLE `dv_catalog_entity_definition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(64) NOT NULL COMMENT '实体定义UUID',
  `name` varchar(255) NOT NULL COMMENT '实体定义的名字',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `properties` text COMMENT '实体参数，用List存储 例如 [{"name":"id","type":"string"}]',
  `super_uuid` varchar(64) NOT NULL DEFAULT '-1' COMMENT '父类ID',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `dv_entity_definition_un` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实体定义';

-- ----------------------------
-- Table structure for dv_catalog_entity_instance
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_entity_instance`;
CREATE TABLE `dv_catalog_entity_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(64) NOT NULL COMMENT '实体实例UUID',
  `type` varchar(127) NOT NULL COMMENT '实体类型',
  `datasource_id` bigint(20) NOT NULL COMMENT '数据源ID',
  `fully_qualified_name` varchar(255) NOT NULL COMMENT '全限定名',
  `display_name` varchar(255) NOT NULL COMMENT '展示名字',
  `description` varchar(1024) DEFAULT NULL COMMENT '描述',
  `properties` text COMMENT '其他参数，用map存储',
  `owner` varchar(255) DEFAULT NULL COMMENT '拥有者',
  `version` varchar(64) NOT NULL DEFAULT '1.0' COMMENT '版本',
  `status` varchar(255) DEFAULT 'active' COMMENT '实体状态：active/deleted',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid_un` (`uuid`) USING BTREE,
  UNIQUE KEY `datasource_fqn_status_un` (`datasource_id`,`fully_qualified_name`,`status`) USING BTREE,
  FULLTEXT KEY `full_idx_display_name_description` (`display_name`,`description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实体实例';

-- ----------------------------
-- Table structure for dv_catalog_entity_metric_job_rel
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_entity_metric_job_rel`;
CREATE TABLE `dv_catalog_entity_metric_job_rel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entity_uuid` varchar(64) NOT NULL COMMENT '实体UUID',
  `metric_job_id` bigint(20) NOT NULL COMMENT '规则作业ID',
  `metric_job_type` varchar(255) NOT NULL COMMENT '规则作业类型',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `dv_catalog_entity_metric_rel_un` (`entity_uuid`,`metric_job_id`,`metric_job_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实体和规则作业关联关系';

-- ----------------------------
-- Table structure for dv_catalog_entity_profile
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_entity_profile`;
CREATE TABLE `dv_catalog_entity_profile` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entity_uuid` varchar(64) NOT NULL COMMENT '实体UUID',
  `metric_name` varchar(255) NOT NULL COMMENT '规则名称',
  `actual_value` text NOT NULL COMMENT '实际值',
  `actual_value_type` varchar(255) DEFAULT NULL COMMENT '实际值类型',
  `data_date` varchar(255) DEFAULT NULL COMMENT '数据日期',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `dv_entity_definition_un` (`entity_uuid`,`metric_name`,`data_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实体概要信息';

-- ----------------------------
-- Table structure for dv_catalog_entity_rel
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_entity_rel`;
CREATE TABLE `dv_catalog_entity_rel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entity1_uuid` varchar(64) NOT NULL COMMENT '实体1UUID',
  `entity2_uuid` varchar(64) NOT NULL COMMENT '实体2UUID',
  `type` varchar(64) NOT NULL COMMENT '关系类型，upstream-2是1上游，downstream-2是1下游, child-2是1的子类，parent-2是1的父类',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `dv_entity_rel_un` (`entity1_uuid`,`entity2_uuid`,`type`),
  KEY `idx_entity2_uuid` (`entity2_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实体关联关系';

-- ----------------------------
-- Table structure for dv_catalog_entity_tag_rel
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_entity_tag_rel`;
CREATE TABLE `dv_catalog_entity_tag_rel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entity_uuid` varchar(64) NOT NULL COMMENT '实体UUID',
  `tag_uuid` varchar(64) NOT NULL COMMENT '标签UUID',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `dv_entity_rel_un` (`entity_uuid`,`tag_uuid`),
  KEY `idx_entity2_uuid` (`tag_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实体标签关联关系';

-- ----------------------------
-- Table structure for dv_catalog_schema_change
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_schema_change`;
CREATE TABLE `dv_catalog_schema_change` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_uuid` varchar(64) NOT NULL COMMENT '父实体UUID',
  `entity_uuid` varchar(64) NOT NULL COMMENT '实体UUID',
  `change_type` varchar(64) NOT NULL COMMENT '变更类型',
  `database_name` varchar(128) DEFAULT NULL COMMENT '数据库',
  `table_name` varchar(128) DEFAULT NULL COMMENT '表',
  `column_name` varchar(128) DEFAULT NULL COMMENT '列',
  `change_before` text DEFAULT NULL COMMENT '变更前',
  `change_after` text DEFAULT NULL COMMENT '变更后',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Schema变更记录表';

-- ----------------------------
-- Table structure for dv_catalog_tag
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_tag`;
CREATE TABLE `dv_catalog_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(64) NOT NULL COMMENT '标签UUID',
  `category_uuid` varchar(64) NOT NULL COMMENT '标签分类UUID',
  `name` varchar(256) NOT NULL COMMENT '标签名称',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `cu_uuid_name_un` (`uuid`,`category_uuid`,`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签';

-- ----------------------------
-- Table structure for dv_catalog_tag_category
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_tag_category`;
CREATE TABLE `dv_catalog_tag_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(64) NOT NULL COMMENT '标签分类UUID',
  `name` varchar(256) NOT NULL COMMENT '标签分类名称',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid_name_un` (`uuid`,`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签分类';

-- ----------------------------
-- Table structure for dv_catalog_metadata_fetch_task
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_metadata_fetch_task`;
CREATE TABLE `dv_catalog_metadata_fetch_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` varchar(128) DEFAULT NULL COMMENT '类型',
  `datasource_id` bigint(20) NOT NULL DEFAULT '-1' COMMENT '数据源ID',
  `database_name` varchar(128) DEFAULT NULL COMMENT '数据库',
  `table_name` varchar(128) DEFAULT NULL COMMENT '表',
  `status` int(11) DEFAULT NULL COMMENT '任务状态',
  `parameter` text COMMENT '任务参数',
  `execute_host` varchar(255) DEFAULT NULL COMMENT '执行任务的主机',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `schedule_time` datetime DEFAULT NULL COMMENT '调度时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据抓取任务';

-- ----------------------------
-- Table structure for dv_catalog_metadata_fetch_task_schedule
-- ----------------------------
DROP TABLE IF EXISTS `dv_catalog_metadata_fetch_task_schedule`;
CREATE TABLE `dv_catalog_metadata_fetch_task_schedule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` varchar(255) NOT NULL COMMENT '调度类型',
  `param` text COMMENT '调度参数',
  `datasource_id` bigint(20) NOT NULL COMMENT '数据源ID',
  `cron_expression` varchar(255) DEFAULT NULL COMMENT 'CRON表达式',
  `status` tinyint(1) DEFAULT NULL COMMENT '调度状态',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据抓取任务调度';

-- ----------------------------
-- Table structure for dv_command
-- ----------------------------
DROP TABLE IF EXISTS `dv_command`;
CREATE TABLE `dv_command` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'Command type: 0 start task, 1 stop task',
  `parameter` text COMMENT 'json command parameters',
  `job_execution_id` bigint(20) NOT NULL COMMENT 'task id',
  `priority` int(11) DEFAULT NULL COMMENT 'process instance priority: 0 Highest,1 High,2 Medium,3 Low,4 Lowest',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则执行命令';

-- ----------------------------
-- Table structure for dv_datasource
-- ----------------------------
DROP TABLE IF EXISTS `dv_datasource`;
CREATE TABLE `dv_datasource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(64) NOT NULL COMMENT '数据源UUID',
  `name` varchar(255) NOT NULL COMMENT '数据源名称',
  `type` varchar(255) NOT NULL COMMENT '数据源类型',
  `param` text NOT NULL COMMENT '数据源参数',
  `param_code` text NULL COMMENT '数据源参数MD5值',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `datasource_un` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源';

-- ----------------------------
-- Table structure for dv_env
-- ----------------------------
DROP TABLE IF EXISTS `dv_env`;
CREATE TABLE `dv_env` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT '服务器环境配置',
  `env` text NOT NULL COMMENT '数据源UUID',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `env_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运行环境配置信息';

-- ----------------------------
-- Table structure for dv_error_data_storage
-- ----------------------------
DROP TABLE IF EXISTS `dv_error_data_storage`;
CREATE TABLE `dv_error_data_storage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT '存储名称',
  `type` varchar(255) NOT NULL COMMENT '存储类型',
  `param` text NOT NULL COMMENT '存储参数',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name_wp_un` (`name`,`workspace_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错误数据存储';

-- ----------------------------
-- Table structure for dv_issue
-- ----------------------------
DROP TABLE IF EXISTS `dv_issue`;
CREATE TABLE `dv_issue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(1024) DEFAULT NULL COMMENT '告警标题',
  `content` text NOT NULL COMMENT '告警内容',
  `status` varchar(255) NOT NULL COMMENT 'good / bad alert',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警信息';

-- ----------------------------
-- Table structure for dv_job
-- ----------------------------
DROP TABLE IF EXISTS `dv_job`;
CREATE TABLE `dv_job` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL COMMENT '作业名称',
  `type` int(11) NOT NULL DEFAULT '0' COMMENT '作业类型',
  `datasource_id` bigint(20) NOT NULL COMMENT '数据源ID',
  `datasource_id_2` bigint(20) DEFAULT NULL COMMENT '数据源2ID',
  `schema_name` varchar(128) DEFAULT NULL COMMENT '数据库名',
  `table_name` varchar(128) DEFAULT NULL COMMENT '表名',
  `column_name` varchar(128) DEFAULT NULL COMMENT '列名',
  `selected_column` text DEFAULT NULL COMMENT 'DataProfile 选中的列',
  `metric_type` varchar(255) DEFAULT NULL COMMENT '规则类型',
  `execute_platform_type` varchar(128) DEFAULT NULL COMMENT '运行平台类型',
  `execute_platform_parameter` text COMMENT '运行平台参数',
  `engine_type` varchar(128) DEFAULT NULL COMMENT '运行引擎类型',
  `engine_parameter` text COMMENT '运行引擎参数',
  `error_data_storage_id` bigint(20) DEFAULT NULL COMMENT '错误数据存储ID',
  `parameter` longtext COMMENT '作业参数',
  `retry_times` int(11) DEFAULT NULL COMMENT '重试次数',
  `retry_interval` int(11) DEFAULT NULL COMMENT '重试间隔',
  `timeout` int(11) DEFAULT NULL COMMENT '任务超时时间',
  `timeout_strategy` int(11) DEFAULT NULL COMMENT '超时策略',
  `tenant_code` bigint(20) DEFAULT NULL COMMENT '代理用户',
  `env` bigint(20) DEFAULT NULL COMMENT '环境配置',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_name` (`name`,`datasource_id`,`schema_name`,`table_name`,`column_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则作业';

-- ----------------------------
-- Table structure for dv_job_execution
-- ----------------------------
DROP TABLE IF EXISTS `dv_job_execution`;
CREATE TABLE `dv_job_execution` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT '作业运行实例名称',
  `job_id` bigint(20) NOT NULL DEFAULT '-1' COMMENT '作业ID',
  `job_type` int(11) NOT NULL DEFAULT '0' COMMENT '作业类型',
  `datasource_id` bigint(20) NOT NULL DEFAULT '-1' COMMENT '数据源ID',
  `execute_platform_type` varchar(128) DEFAULT NULL COMMENT '运行平台类型',
  `execute_platform_parameter` text COMMENT '运行平台参数',
  `engine_type` varchar(128) DEFAULT NULL COMMENT '运行引擎类型',
  `engine_parameter` text COMMENT '运行引擎参数',
  `error_data_storage_type` varchar(128) DEFAULT NULL COMMENT '错误数据存储类型',
  `error_data_storage_parameter` text COMMENT '错误数据存储参数',
  `error_data_file_name` varchar(255) DEFAULT NULL COMMENT '错误数据存储文件名',
  `parameter` longtext NOT NULL COMMENT '作业运行参数',
  `status` int(11) DEFAULT NULL COMMENT '运行平台参数',
  `retry_times` int(11) DEFAULT NULL COMMENT '重试次数',
  `retry_interval` int(11) DEFAULT NULL COMMENT '重试间隔',
  `timeout` int(11) DEFAULT NULL COMMENT '超时时间',
  `timeout_strategy` int(11) DEFAULT NULL COMMENT '超时处理策略',
  `tenant_code` varchar(255) DEFAULT NULL COMMENT '代理用户',
  `execute_host` varchar(255) DEFAULT NULL COMMENT '执行任务的主机',
  `application_id` varchar(255) DEFAULT NULL COMMENT 'yarn application id',
  `application_tag` varchar(255) DEFAULT NULL COMMENT 'yarn application tags',
  `process_id` int(11) DEFAULT NULL COMMENT 'process id',
  `execute_file_path` varchar(255) DEFAULT NULL COMMENT 'execute file path',
  `log_path` varchar(255) DEFAULT NULL COMMENT 'log path',
  `env` text COMMENT '运行环境的配置信息',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `schedule_time` datetime DEFAULT NULL COMMENT '调度时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则作业运行实例';

-- ----------------------------
-- Table structure for dv_job_execution_result
-- ----------------------------
DROP TABLE IF EXISTS `dv_job_execution_result`;
CREATE TABLE `dv_job_execution_result` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `job_execution_id` bigint(20) DEFAULT NULL COMMENT '任务执行实例ID',
  `metric_unique_key` varchar(255) DEFAULT NULL COMMENT '规则运行唯一标识',
  `metric_type` varchar(255) DEFAULT NULL COMMENT '规则类型',
  `metric_dimension` varchar(255) DEFAULT NULL COMMENT '规则维度',
  `metric_name` varchar(255) DEFAULT NULL COMMENT '规则名称',
  `database_name` varchar(128) DEFAULT NULL COMMENT '数据库名称',
  `table_name` varchar(128) DEFAULT NULL COMMENT '表名称',
  `column_name` varchar(128) DEFAULT NULL COMMENT '列名称',
  `actual_value` double DEFAULT NULL COMMENT '实际值',
  `expected_value` double DEFAULT NULL COMMENT '期望值',
  `expected_type` varchar(255) DEFAULT NULL COMMENT '期望值类型',
  `result_formula` varchar(255) DEFAULT NULL COMMENT '计算结果公式',
  `operator` varchar(255) DEFAULT NULL COMMENT '比较符',
  `threshold` double DEFAULT NULL COMMENT '阈值',
  `state` int(2) NOT NULL DEFAULT '0' COMMENT '结果 success/fail',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `execution_id_un` (`job_execution_id`,`metric_unique_key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则作业运行结果';

-- ----------------------------
-- Table structure for dv_job_issue_rel
-- ----------------------------
DROP TABLE IF EXISTS `dv_job_issue_rel`;
CREATE TABLE `dv_job_issue_rel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `job_id` bigint(20) NOT NULL COMMENT '规则ID',
  `issue_id` bigint(20) NOT NULL COMMENT 'ISSUE ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `dv_entity_rel_un` (`job_id`,`issue_id`),
  KEY `idx_entity2_uuid` (`issue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实体和告警信息关联关系';

-- ----------------------------
-- Table structure for dv_job_schedule
-- ----------------------------
DROP TABLE IF EXISTS `dv_job_schedule`;
CREATE TABLE `dv_job_schedule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` varchar(255) NOT NULL COMMENT '作业调度类型',
  `param` text COMMENT '调度参数',
  `job_id` bigint(20) NOT NULL COMMENT '作业ID',
  `cron_expression` varchar(255) DEFAULT NULL COMMENT 'CRON 表达式',
  `status` tinyint(1) DEFAULT NULL COMMENT '状态',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则作业调度';

-- ----------------------------
-- Table structure for dv_server
-- ----------------------------
DROP TABLE IF EXISTS `dv_server`;
CREATE TABLE `dv_server` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `host` varchar(255) NOT NULL COMMENT '机器IP地址',
  `port` int(11) NOT NULL COMMENT '端口',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `server_un` (`host`,`port`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集群节点信息';

-- ----------------------------
-- Table structure for dv_sla
-- ----------------------------
DROP TABLE IF EXISTS `dv_sla`;
CREATE TABLE `dv_sla` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `name` varchar(255) NOT NULL COMMENT 'SLA 名字',
  `description` varchar(255) NOT NULL COMMENT '描述',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警管理';

-- ----------------------------
-- Table structure for dv_sla_job
-- ----------------------------
DROP TABLE IF EXISTS `dv_sla_job`;
CREATE TABLE `dv_sla_job` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `sla_id` bigint(20) NOT NULL COMMENT 'SLA ID',
  `job_id` bigint(20) NOT NULL COMMENT '规则作业ID',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique` (`workspace_id`,`sla_id`,`job_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警和规则作业关联关系';

-- ----------------------------
-- Table structure for dv_sla_notification
-- ----------------------------
DROP TABLE IF EXISTS `dv_sla_notification`;
CREATE TABLE `dv_sla_notification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` varchar(40) NOT NULL COMMENT '类型',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `sla_id` bigint(20) NOT NULL COMMENT 'SLA ID',
  `sender_id` bigint(20) NOT NULL COMMENT '发送者ID',
  `config` text COMMENT '工作空间ID',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警管理中的通知组件';

-- ----------------------------
-- Table structure for dv_sla_sender
-- ----------------------------
DROP TABLE IF EXISTS `dv_sla_sender`;
CREATE TABLE `dv_sla_sender` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` varchar(40) NOT NULL COMMENT '类型',
  `name` varchar(255) NOT NULL COMMENT '名称',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `config` text NOT NULL COMMENT '配置信息',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警管理中的发送者信息';

-- ----------------------------
-- Table structure for dv_tenant
-- ----------------------------
DROP TABLE IF EXISTS `dv_tenant`;
CREATE TABLE `dv_tenant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tenant` varchar(255) NOT NULL COMMENT '租户名',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `tenant_name` (`tenant`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运行环境的租户';

-- ----------------------------
-- Table structure for dv_user
-- ----------------------------
DROP TABLE IF EXISTS `dv_user`;
CREATE TABLE `dv_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码',
  `email` varchar(255) NOT NULL COMMENT '邮箱',
  `phone` varchar(127) DEFAULT NULL COMMENT '手机号码',
  `admin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为管理员',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_un` (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ----------------------------
-- Table structure for dv_user_workspace
-- ----------------------------
DROP TABLE IF EXISTS `dv_user_workspace`;
CREATE TABLE `dv_user_workspace` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `role_id` bigint(20) DEFAULT NULL COMMENT '角色ID',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和工作空间关联关系';

-- ----------------------------
-- Table structure for dv_workspace
-- ----------------------------
DROP TABLE IF EXISTS `dv_workspace`;
CREATE TABLE `dv_workspace` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT '工作空间名称',
  `create_by` bigint(20) NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新用户ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `workspace_un` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作空间';

INSERT INTO `dv_user` (`id`, `username`, `password`, `email`, `phone`, `admin`) VALUES ('1', 'admin', '$2a$10$9ZcicUYFl/.knBi9SE53U.Nml8bfNeArxr35HQshxXzimbA6Ipgqq', 'admin@gmail.com', NULL, '0');
INSERT INTO `dv_workspace` (`id`, `name`, `create_by`, `update_by`) VALUES ('1', "admin\'s default", '1', '1');
INSERT INTO `dv_user_workspace` (`id`, `user_id`, `workspace_id`, `role_id`,`create_by`,`update_by`) VALUES ('1', '1', '1', '1','1', '1');

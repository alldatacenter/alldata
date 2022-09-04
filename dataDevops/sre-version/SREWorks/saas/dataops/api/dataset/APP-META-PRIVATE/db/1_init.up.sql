-- CREATE DATABASE IF NOT EXISTS `dataset` DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;

-- USE `dataset`;

-- ----------------------------
-- Table structure
-- ----------------------------
CREATE TABLE IF NOT EXISTS `data_subject` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '主题ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '主题名称',
  `abbreviation` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主题缩写',
  `build_in` tinyint(1) NOT NULL COMMENT '是否内置主题,1:是 0:否',
  `type` varchar(64) NOT NULL DEFAULT '' COMMENT '主题类型',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据主题表';

CREATE TABLE IF NOT EXISTS `data_domain` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '数据域ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据域名称',
  `abbreviation` varchar(64) NOT NULL COMMENT '数据域缩写',
  `build_in` tinyint(1) NOT NULL COMMENT '是否内置数据域,1:是 0:否',
  `subject_id` int unsigned NOT NULL COMMENT '所属数据主题ID',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
  PRIMARY KEY (`id`),
  KEY `subject_id` (`subject_id`),
  CONSTRAINT `data_domain_ibfk_1` FOREIGN KEY (`subject_id`) REFERENCES `data_subject` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据域表';

CREATE TABLE IF NOT EXISTS `data_model_config` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '数据模型ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '数据模型名称',
  `label` varchar(64) NOT NULL COMMENT '数据模型标识',
  `build_in` tinyint(1) NOT NULL COMMENT '是否内置数据模型,1:是 0:否',
  `domain_id` int unsigned NOT NULL COMMENT '所属数据域ID',
  `team_id` int DEFAULT NULL COMMENT '所属团队',
  `source_type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据源类型',
  `source_id` varchar(256) NOT NULL COMMENT '数据源ID',
  `source_table` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据表',
  `granularity` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '时间粒度',
  `query` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '查询条件',
  `query_fields` varchar(2048) DEFAULT NULL COMMENT '查询参数字段列表',
  `value_fields` varchar(2048) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '数值字段列表(有顺序)',
  `group_fields` varchar(2048) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '分组字段列表(有顺序)',
  `model_fields` varchar(2048) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '模型字段列表(自动生成)',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
  PRIMARY KEY (`id`),
  KEY `domain_id` (`domain_id`),
  CONSTRAINT `data_model_config_ibfk_1` FOREIGN KEY (`domain_id`) REFERENCES `data_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据模型配置表';

CREATE TABLE IF NOT EXISTS `data_interface_config` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '数据接口ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '数据接口名称',
  `label` varchar(64) NOT NULL COMMENT '数据接口标识',
  `build_in` tinyint(1) NOT NULL COMMENT '是否内置数据接口,1:是 0:否',
  `model_id` int unsigned NOT NULL COMMENT '所属数据模型ID',
  `team_id` int DEFAULT NULL COMMENT '所属团队',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `owners` varchar(1024) DEFAULT NULL COMMENT '负责人列表',
  `request_method` varchar(32) NOT NULL COMMENT '请求方法',
  `content_type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '资源类型',
  `response_fields` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '响应字段列表,逗号分隔',
  `sort_fields` varchar(2048) DEFAULT NULL COMMENT '排序字段配置列表(有顺序)',
  `paging` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否分页,1:是 0:否',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_label` (`label`),
  KEY `model_id` (`model_id`),
  CONSTRAINT `data_interface_config_ibfk_1` FOREIGN KEY (`model_id`) REFERENCES `data_model_config` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据接口配置表';

CREATE TABLE IF NOT EXISTS `data_interface_params` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '参数ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '参数名称',
  `label` varchar(64) NOT NULL COMMENT '参数标识',
  `interface_id` int unsigned NOT NULL COMMENT '数据接口ID',
  `type` varchar(64) NOT NULL COMMENT '参数类型',
  `required` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否必填,1:是 0:否',
  `default_value` varchar(128) DEFAULT NULL COMMENT '默认值',
  PRIMARY KEY (`id`),
  KEY `interface_id` (`interface_id`),
  CONSTRAINT `data_interface_params_ibfk_1` FOREIGN KEY (`interface_id`) REFERENCES `data_interface_config` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据接口参数表';

CREATE TABLE IF NOT EXISTS `interface_config` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '数据接口ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '数据接口名称',
  `alias` varchar(64) NOT NULL COMMENT '数据接口别名',
  `data_source_type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据源类型',
  `data_source_id` varchar(256) NOT NULL COMMENT '数据源ID',
  `data_source_table` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据表',
  `mode` varchar(32) NOT NULL COMMENT '脚本/向导',
  `ql_template` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '查询模板',
  `query_fields` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '查询参数字段列表',
  `group_fields` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '分组字段列表(有顺序)',
  `sort_fields` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '排序字段配置列表(有顺序)',
  `request_params` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '请求参数',
  `response_params` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '返回参数',
  `build_in` tinyint(1) NOT NULL COMMENT '是否内置数据接口,1:是 0:否',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `last_modifier` varchar(64) DEFAULT NULL COMMENT '最近修改人',
  `request_method` varchar(32) NOT NULL COMMENT '请求方法',
  `content_type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '资源类型',
  `paging` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否分页,1:是 0:否',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据接口配置表'
;

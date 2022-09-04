CREATE TABLE IF NOT EXISTS `common_definition` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '定义ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '定义名称',
  `category` varchar(64) NOT NULL COMMENT '定义分类 event/risk/alert/incident',
  `app_id` varchar(128) NOT NULL COMMENT '所属应用ID',
  `app_name` varchar(128) NOT NULL COMMENT '所属应用名称',
  `app_component_name` varchar(128) DEFAULT NULL COMMENT '所属应用组件名称',
  `metric_id` int DEFAULT NULL COMMENT '关联指标',
  `failure_ref_incident_id` int DEFAULT NULL COMMENT '故障关联定义ID',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `receivers` varchar(1024) DEFAULT NULL COMMENT '通知接收人列表',
  `last_modifier` varchar(64) DEFAULT NULL COMMENT '最后修改人',
  `ex_config` varchar(2048) DEFAULT NULL COMMENT '扩展配置',
  `description` varchar(2048) DEFAULT NULL COMMENT '定义说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_app_component` (`app_id`, `app_component_name`, `name`, `category`),
  KEY `key_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='事件定义表'
;

CREATE TABLE IF NOT EXISTS `incident_type` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '异常类型ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `label` varchar(128) NOT NULL COMMENT '类型标识',
  `name` varchar(128) NOT NULL COMMENT '类型名称',
  `mq_topic` varchar(128) NOT NULL COMMENT '消息队列topic',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `last_modifier` varchar(64) DEFAULT NULL COMMENT '最后修改人',
  `description` varchar(2048) DEFAULT NULL COMMENT '类型说明',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='异常类型表'
;

CREATE TABLE IF NOT EXISTS `incident_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '异常实例ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `def_id` int NOT NULL COMMENT '定义ID',
  `app_instance_id` varchar(128) DEFAULT NULL COMMENT '应用实例ID',
  `app_component_instance_id` varchar(128) DEFAULT NULL COMMENT '应用组件实例ID',
  `gmt_occur` datetime NOT NULL COMMENT '开始发生时间',
  `gmt_last_occur` datetime NOT NULL COMMENT '最近发生时间',
  `occur_times` int DEFAULT 1 COMMENT '持续发生次数',
  `gmt_recovery` datetime DEFAULT NULL COMMENT '恢复时间',
  `source` varchar(32) NOT NULL COMMENT '异常来源',
  `cause` text COMMENT '异常产生原因',
  `trace_id` varchar(128) DEFAULT NULL COMMENT '自愈任务链ID',
  `span_id` varchar(128) DEFAULT NULL COMMENT '层次ID',
  `gmt_self_healing_start` datetime DEFAULT NULL COMMENT '自愈开始时间',
  `gmt_self_healing_end` datetime DEFAULT NULL COMMENT '自愈结束时间',
  `self_healing_status` varchar(32) DEFAULT NULL COMMENT '自愈状态',
  `options` varchar(5120) DEFAULT NULL COMMENT '扩展信息(包括用户自定义参数)',
  `description` varchar(2048) DEFAULT NULL COMMENT '实例说明',
  PRIMARY KEY (`id`),
  KEY `key_app` (`app_instance_id`, `app_component_instance_id`),
  KEY `key_trace_id` (`trace_id`),
  CONSTRAINT `def_ibfk_3` FOREIGN KEY (`def_id`) REFERENCES `common_definition` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='异常实例表'
;

CREATE TABLE IF NOT EXISTS `alert_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '告警实例ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `def_id` int NOT NULL COMMENT '定义ID',
  `app_instance_id` varchar(128) DEFAULT NULL COMMENT '应用实例ID',
  `app_component_instance_id` varchar(128) DEFAULT NULL COMMENT '应用组件实例ID',
  `metric_instance_id` varchar(128) DEFAULT NULL COMMENT '指标实例ID',
  `metric_instance_labels` varchar(2048) DEFAULT NULL COMMENT '指标实例标签',
  `gmt_occur` datetime NOT NULL COMMENT '发生时间',
  `source` varchar(32) NOT NULL COMMENT '告警来源',
  `level` varchar(16) NOT NULL COMMENT '告警等级',
  `receivers` varchar(256) DEFAULT NULL COMMENT '告警接收人',
  `content` text COMMENT '告警内容',
  PRIMARY KEY (`id`),
  KEY `key_app` (`app_instance_id`, `app_component_instance_id`),
  KEY `key_metric` (`metric_instance_id`),
  CONSTRAINT `def_ibfk_4` FOREIGN KEY (`def_id`) REFERENCES `common_definition` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='告警实例表'
;

CREATE TABLE IF NOT EXISTS `risk_type` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '风险类型ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `label` varchar(128) NOT NULL COMMENT '类型标识',
  `name` varchar(128) NOT NULL COMMENT '类型名称',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `last_modifier` varchar(64) DEFAULT NULL COMMENT '最后修改人',
  `description` varchar(2048) DEFAULT NULL COMMENT '类型说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_risk_type` (`label`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='风险类型表'
;

CREATE TABLE IF NOT EXISTS `risk_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '风险实例ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `def_id` int NOT NULL COMMENT '定义ID',
  `app_instance_id` varchar(128) DEFAULT NULL COMMENT '应用实例ID',
  `app_component_instance_id` varchar(128) DEFAULT NULL COMMENT '应用组件实例ID',
  `gmt_occur` datetime DEFAULT NULL COMMENT '发生时间',
  `source` varchar(32) NOT NULL COMMENT '风险来源',
  `content` text COMMENT '风险详情',
  PRIMARY KEY (`id`),
  KEY `key_app` (`app_instance_id`, `app_component_instance_id`),
  CONSTRAINT `def_ibfk_5` FOREIGN KEY (`def_id`) REFERENCES `common_definition` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='风险实例表'
;

CREATE TABLE IF NOT EXISTS `failure_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '故障实例ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `def_id` int NOT NULL COMMENT '定义ID',
  `app_instance_id` varchar(128) DEFAULT NULL COMMENT '应用实例ID',
  `app_component_instance_id` varchar(128) DEFAULT NULL COMMENT '应用组件实例ID',
  `incident_id` bigint NOT NULL COMMENT '异常实例ID',
  `name` varchar(128) NOT NULL COMMENT '故障名称',
  `level` varchar(8) NOT NULL COMMENT '故障等级',
  `gmt_occur` datetime NOT NULL COMMENT '故障发生时间',
  `gmt_recovery` datetime DEFAULT NULL COMMENT '故障恢复时间',
  `content` text COMMENT '故障详情',
  PRIMARY KEY (`id`),
  KEY `key_app` (`app_instance_id`, `app_component_instance_id`),
  CONSTRAINT `def_ibfk_6` FOREIGN KEY (`def_id`) REFERENCES `common_definition` (`id`),
  CONSTRAINT `incident_ibfk_1` FOREIGN KEY (`incident_id`) REFERENCES `incident_instance` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='故障实例表'
;

CREATE TABLE IF NOT EXISTS `failure_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '故障记录ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `failure_id` bigint NOT NULL COMMENT '故障实例ID',
  `def_id` int NOT NULL COMMENT '定义ID',
  `app_instance_id` varchar(128) DEFAULT NULL COMMENT '应用实例ID',
  `app_component_instance_id` varchar(128) DEFAULT NULL COMMENT '应用组件实例ID',
  `incident_id` bigint NOT NULL COMMENT '异常实例ID',
  `name` varchar(128) NOT NULL COMMENT '故障名称',
  `level` varchar(8) NOT NULL COMMENT '故障等级',
  `gmt_occur` datetime NOT NULL COMMENT '故障发生时间',
  `gmt_recovery` datetime DEFAULT NULL COMMENT '故障恢复时间',
  `content` text COMMENT '故障详情',
  PRIMARY KEY (`id`),
  KEY `failure_id` (`failure_id`),
  KEY `key_app` (`app_instance_id`, `app_component_instance_id`),
  CONSTRAINT `def_ibfk_7` FOREIGN KEY (`def_id`) REFERENCES `common_definition` (`id`),
  CONSTRAINT `incident_ibfk_2` FOREIGN KEY (`incident_id`) REFERENCES `incident_instance` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='故障记录表'
;

CREATE TABLE IF NOT EXISTS `event_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件实例ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `def_id` int NOT NULL COMMENT '定义ID',
  `app_instance_id` varchar(128) DEFAULT NULL COMMENT '应用实例ID',
  `app_component_instance_id` varchar(128) DEFAULT NULL COMMENT '应用组件实例ID',
  `gmt_occur` datetime DEFAULT NULL COMMENT '发生时间',
  `source` varchar(32) NOT NULL COMMENT '事件来源',
  `type` varchar(32) NOT NULL COMMENT '事件类别',
  `content` text COMMENT '事件详情',
  PRIMARY KEY (`id`),
  KEY `key_app` (`app_instance_id`, `app_component_instance_id`),
  CONSTRAINT `def_ibfk_8` FOREIGN KEY (`def_id`) REFERENCES `common_definition` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='事件实例表'
;

REPLACE INTO risk_type(id, label, name, creator, last_modifier, description) VALUES (1, 'SERVICE_UNAVAILABLE', '服务不可用异常',  'sreworks', 'sreworks', '服务不可用异常类型(该类型异常参与计算服务可用率)
');


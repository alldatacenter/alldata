CREATE TABLE IF NOT EXISTS `metric` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '指标ID',
  `uid` varchar(128) NOT NULL COMMENT '指标唯一身份ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '指标名称',
  `alias` varchar(128) NOT NULL COMMENT '指标别名',
  `type` varchar(64) NOT NULL COMMENT '指标类型',
  `labels` varchar(2048) DEFAULT '{}' COMMENT '标签 {"k1":"v1", "k2":"v2"}',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `last_modifier` varchar(64) DEFAULT NULL COMMENT '最近修改人',
  `description` varchar(2048) DEFAULT NULL COMMENT '指标说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_uid` (`uid`),
  KEY `key_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='指标表'
;

CREATE TABLE IF NOT EXISTS `metric_instance` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '实例ID',
  `uid` varchar(128) NOT NULL COMMENT '实例唯一身份ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `metric_id` int unsigned NOT NULL COMMENT '指标ID',
  `labels` varchar(2048) DEFAULT '{}' COMMENT '标签 {"k1":"v1", "k2":"v2"}',
  `description` varchar(2048) DEFAULT NULL COMMENT '实例说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_uid` (`uid`),
  CONSTRAINT `metric_instance_ibfk_1` FOREIGN KEY (`metric_id`) REFERENCES `metric` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='指标实例表'
;

CREATE TABLE IF NOT EXISTS `metric_anomaly_detection_config` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `rule_id` int NOT NULL COMMENT '规则ID',
  `metric_id` int unsigned NOT NULL COMMENT '指标ID',
  `enable` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用,1:是 0:否',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `owners` varchar(1024) DEFAULT NULL COMMENT '负责人列表',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
  PRIMARY KEY (`id`),
  CONSTRAINT `metric_instance_ibfk_2` FOREIGN KEY (`metric_id`) REFERENCES `metric` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='指标异常检测配置表'
;

CREATE TABLE IF NOT EXISTS `datasource` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '数据源名称',
  `type` varchar(64) NOT NULL COMMENT '数据源类型',
  `connect_config` varchar(2048) NOT NULL COMMENT '数据源链接配置',
  `build_in` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置数据源,1:是 0:否',
  `app_id` varchar(128) DEFAULT NULL COMMENT '归属应用',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `last_modifier` varchar(64) DEFAULT NULL COMMENT '最近修改人',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
  PRIMARY KEY (`id`),
  KEY `key_app` (`app_id`),
  KEY `key_type_name` (`type`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据源表'
;

REPLACE INTO datasource(id, name, type, connect_config, build_in, app_id, creator, last_modifier, description) VALUES (1, 'sreworks_es', 'es', '{"schema":"http","port":${DATA_ES_PORT},"host":"${DATA_ES_HOST}","username":"${DATA_ES_USER}","password":"${DATA_ES_PASSWORD}"}', true, 0, 'sreworks', 'sreworks', 'sreworks_es数据源');

REPLACE INTO datasource(id, name, type, connect_config, build_in, app_id, creator, last_modifier, description) VALUES (2, 'sreworks_meta_mysql', 'mysql', '{"password":"${DB_PASSWORD}","port":${DB_PORT},"host":"${DB_HOST}","db":"sreworks_meta","username":"${DB_USER}"}', true, 0, 'sreworks', 'sreworks', 'sreworks_meta数据源');
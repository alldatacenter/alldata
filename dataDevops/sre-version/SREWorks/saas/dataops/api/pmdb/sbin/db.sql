CREATE TABLE IF NOT EXISTS `metric` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '指标ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '指标名称',
  `type` varchar(64) NOT NULL COMMENT '指标类型',
  `index_path` varchar(256) NOT NULL COMMENT '指标索引路径a.b.c',
  `tags` varchar(2048) DEFAULT NULL COMMENT '标签k1.k2.k3,k1.k4.k5',
  `build_in` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置指标,1:是 0:否',
  `entity` varchar(64) NOT NULL COMMENT '所属实体',
  `source_type` varchar(64) NOT NULL COMMENT '数据源类型',
  `source_id` varchar(128) NOT NULL COMMENT '数据源ID',
  `source_table` varchar(128) NOT NULL COMMENT '数据源表',
  `team_id` int DEFAULT NULL COMMENT '所属团队',
  `app_id` int DEFAULT NULL COMMENT '所属应用',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `owners` varchar(1024) DEFAULT NULL COMMENT '负责人列表',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
  PRIMARY KEY (`id`),
  KEY `key_team_app` (`team_id`,`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='指标表';

CREATE TABLE IF NOT EXISTS `metric_instance` (
  `id` varchar(128) NOT NULL COMMENT '指标实例ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '实例名称',
  `metric_id` int unsigned NOT NULL COMMENT '指标ID',
  `metric_name` varchar(128) NOT NULL COMMENT '指标名称',
  `index_path` varchar(256) NOT NULL COMMENT '指标选择器a.b.c',
  `index_tags` varchar(2048) DEFAULT NULL COMMENT '标签 k1.k2.k3=v1,k1.k4.k5=v2',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
  PRIMARY KEY (`id`),
  CONSTRAINT `metric_instance_ibfk_1` FOREIGN KEY (`metric_id`) REFERENCES `metric` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='指标实例表';

CREATE TABLE IF NOT EXISTS `metric_anomaly_detection_config` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `rule_id` int NOT NULL COMMENT '规则ID',
  `metric_id` int unsigned NOT NULL COMMENT '指标ID',
  `enable` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用,1:是 0:否',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `owners` varchar(1024) DEFAULT NULL COMMENT '负责人列表',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
  PRIMARY KEY (`id`),
  CONSTRAINT `metric_instance_ibfk_2` FOREIGN KEY (`metric_id`) REFERENCES `metric` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='指标异常检测配置表';

-- CREATE TABLE IF NOT EXISTS `metric_time_series_prediction_config` (
--   `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '配置ID',
--   `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
--   `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
--   `rule_id` int NOT NULL COMMENT '规则ID',
--   `metric_id` int unsigned NOT NULL COMMENT '指标ID',
--   `enable` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用,1:是 0:否',
--   `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
--   `owners` varchar(1024) DEFAULT NULL COMMENT '负责人列表',
--   `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
--   PRIMARY KEY (`id`),
--   CONSTRAINT `metric_instance_ibfk_3` FOREIGN KEY (`metric_id`) REFERENCES `metric` (`id`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='指标异常检测配置表';

CREATE TABLE IF NOT EXISTS `datasource` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '数据源名称',
  `type` varchar(64) NOT NULL COMMENT '数据源类型',
  `endpoint` varchar(128) NOT NULL COMMENT '数据源链接',
  `port` int unsigned NOT NULL DEFAULT '0' COMMENT '端口',
  `access_key` varchar(128) DEFAULT NULL COMMENT '用户',
  `secret_key` varchar(128) DEFAULT NULL COMMENT '密码',
  `source_db` varchar(128) DEFAULT NULL COMMENT '数据源库',
  `build_in` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置数据源,1:是 0:否',
  `team_id` int DEFAULT NULL COMMENT '所属团队',
  `app_id` int DEFAULT NULL COMMENT '所属应用',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `owners` varchar(1024) DEFAULT NULL COMMENT '负责人列表',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
  PRIMARY KEY (`id`),
  KEY `key_team_app` (`team_id`,`app_id`),
  KEY `key_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='数据源表';


REPLACE INTO datasource(id, name, type, endpoint, port, source_db, build_in, team_id, app_id, creator, owners, description) VALUES (1, 'metricbeat', 'es', 'elasticsearch-master.data.svc.cluster.local', 9200, NULL, true, 0, 0, 'sreworks', 'sreworks', 'metricbeat指标数据源');

REPLACE INTO datasource(id, name, type, endpoint, port, access_key, secret_key, source_db, build_in, team_id, app_id, creator, owners, description) VALUES (2, 'sreworks', 'mysql', 'data-mysql.data.svc.cluster.local', 3306, 'root', 'root', 'sreworks', true, 0, 0, 'sreworks', 'sreworks', 'sreworks数据源');

REPLACE INTO metric(id, name, type, index_path, tags, build_in, entity, source_type, source_id, source_table, team_id, app_id, description) VALUES (1, 'pod_cpu_usage', '性能指标', 'kubernetes.pod.cpu.usage.node.pct', 'service.type,metricset.name,kubernetes.pod.name', 1, 'KUBERNETES_POD', 'es', '1', 'metricbeat-7.13.0', 0, 0, 'POD CPU使用率');

REPLACE INTO metric_instance(id, name, metric_id, metric_name, index_path, index_tags, description) VALUES ('b2dbf55f70d79d5ebebac51d7dd23462', 'mall_pod_cpu_usage', 1, 'pod_cpu_usage', 'kubernetes.pod.cpu.usage.node.pct', 'service.type=kubernetes,metricset.name=pod,kubernetes.pod.name=dev-sreworks38-mall-7466869dcc-2mmsd', '限定某一POD CPU使用率');

-- INSERT INTO metric_anomaly_detection_config(rule_id, metric_id, enable, description) VALUES (10001, 1, 1, '限定某一POD CPU使用率异常检测');
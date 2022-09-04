CREATE TABLE IF NOT EXISTS `sw_domain` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '数据域ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据域名称',
  `abbreviation` varchar(64) NOT NULL COMMENT '数据域英文缩写',
  `build_in` tinyint(1) NOT NULL COMMENT '是否内置数据域,1:是 0:否',
  `subject` varchar(128) DEFAULT NULL COMMENT '数仓主题',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  UNIQUE KEY `uk_abbreviation` (`abbreviation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据域表'
;

/******************************************/
/*   DatabaseName = warehouse   */
/*   TableName = sw_entity   */
/******************************************/
CREATE TABLE IF NOT EXISTS `sw_entity` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '模型名(建议大写)',
  `alias` varchar(128) NOT NULL COMMENT '实体别名',
  `table_name` varchar(128) NOT NULL COMMENT '存储表(索引)名(${layer}_lower(实体名称)_${partition})',
  `table_alias` varchar(128) NOT NULL COMMENT '存储表(索引)别名(${layer}_lower(实体名称))',
  `build_in` tinyint(1) NOT NULL COMMENT '是否内置实体,1:是 0:否',
  `layer` varchar(8) DEFAULT 'ods' COMMENT '数仓分层',
  `partition_format` varchar(64) DEFAULT 'd' COMMENT '分区规范(ES日期规范, 默认按天分区)',
  `lifecycle` int DEFAULT '365' COMMENT '生命周期',
  `icon` varchar(128) DEFAULT NULL COMMENT '实体图标',
  `description` text COMMENT '实体备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table` (`table_name`),
  UNIQUE KEY `uk_entity` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实体定义表'
;

/******************************************/
/*   DatabaseName = warehouse   */
/*   TableName = sw_entity_field   */
/******************************************/
CREATE TABLE IF NOT EXISTS `sw_entity_field` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `entity_id` bigint unsigned NOT NULL COMMENT '实体ID',
  `field` varchar(128) NOT NULL COMMENT '列名',
  `alias` varchar(128) NOT NULL COMMENT '列别名',
  `dim` varchar(128) NOT NULL COMMENT '存储列名',
  `type` varchar(128) NOT NULL COMMENT '列类型',
  `build_in` tinyint(1) NOT NULL COMMENT '是否内置字段,1:是 0:否',
  `nullable` tinyint(1) NOT NULL COMMENT '是否可空,1:是 0:否',
  `description` text COMMENT '列备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entity_field` (`entity_id`,`field`),
  KEY `idx_field` (`field`),
  CONSTRAINT `entity_ibfk_1` FOREIGN KEY (`entity_id`) REFERENCES `sw_entity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实体字段定义表'
;

/******************************************/
/*   DatabaseName = warehouse   */
/*   TableName = sw_model   */
/******************************************/
CREATE TABLE IF NOT EXISTS `sw_model` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `name` varchar(128) NOT NULL COMMENT '模型名称(建议大写)',
  `alias` varchar(128) NOT NULL COMMENT '模型别名',
  `table_name` varchar(128) NOT NULL COMMENT '存储表(索引)名(${layer}_${domain/app}_lower(模型名称)_${partition})',
  `table_alias` varchar(128) NOT NULL COMMENT '存储表(索引)别名(${layer}_${domain/app}_lower(模型名称))',
  `build_in` tinyint(1) NOT NULL COMMENT '是否内置模型,1:是 0:否',
  `layer` varchar(8) NOT NULL COMMENT '数仓分层',
  `domain_id` int unsigned DEFAULT NULL COMMENT '所属数据域ID',
  `partition_format` varchar(64) DEFAULT 'd' COMMENT '分区规范(ES日期规范, 默认按天分区)',
  `data_mode` varchar(8) DEFAULT NULL COMMENT '数据存储模式',
  `stat_period` varchar(8) DEFAULT NULL COMMENT '数据统计周期',
  `lifecycle` int DEFAULT '365' COMMENT '生命周期',
  `tag` varchar(128) DEFAULT NULL COMMENT '模型标签',
  `description` text COMMENT '模型备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table` (`table_name`),
  UNIQUE KEY `uk_model` (`name`),
  KEY `key_category` (`layer`,`domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型定义表'
;

/******************************************/
/*   DatabaseName = warehouse   */
/*   TableName = sw_model_field   */
/******************************************/
CREATE TABLE IF NOT EXISTS `sw_model_field` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `model_id` bigint(20) unsigned NOT NULL COMMENT '模型ID',
  `field` varchar(128) NOT NULL COMMENT '列名',
  `alias` varchar(128) NOT NULL COMMENT '列别名',
  `dim` varchar(128) NOT NULL COMMENT '存储列名',
  `type` varchar(128) NOT NULL COMMENT '列类型',
  `build_in` tinyint(1) NOT NULL COMMENT '是否内置字段,1:是 0:否',
  `nullable` tinyint(1) NOT NULL COMMENT '是否可空,1:是 0:否',
  `description` text COMMENT '列备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_field` (`model_id`,`field`),
  KEY `idx_field` (`field`),
  CONSTRAINT `model_ibfk_1` FOREIGN KEY (`model_id`) REFERENCES `sw_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型字段定义表'
;

insert into sw_domain (`id`, `name`, `abbreviation`, `build_in`, `subject`, `description`) values
(1,'稳定性域', 'stability', 1, '运维服务主题', '运维稳定性相关模型数据域'),
(2,'成本域', 'cost', 1, '运维服务主题', '运维成本相关模型数据域'),
(3,'效率域', 'efficiency', 1, '运维服务主题', '运维效率相关模型数据域'),
(4,'原始数据域', 'original', 1, '运维数据主题', '原始相关模型数据域'),
(5,'衍生数据域', 'derivation', 1, '运维数据主题', '衍生相关模型数据域'),
(6,'资源域', 'resource', 1, '运维对象主题', '资源模型数据域'),
(7,'系统域', 'system', 1, '运维对象主题', '系统模型数据域'),
(8,'平台域', 'platform', 1, '运维对象主题', '平台模型数据域'),
(9,'业务域', 'business', 1, '运维对象主题', '业务模型数据域')
;


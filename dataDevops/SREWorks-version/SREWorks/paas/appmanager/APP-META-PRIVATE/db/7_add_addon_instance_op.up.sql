DROP TABLE IF EXISTS `am_addon_meta`;
DROP TABLE IF EXISTS `am_addon_instance`;
DROP TABLE IF EXISTS `am_addon_instance_task`;
DROP TABLE IF EXISTS `am_addon_component_rel`;

CREATE TABLE `am_addon_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '最后修改时间',
  `addon_instance_id` varchar(64) DEFAULT '' COMMENT '资源实例ID',
  `namespace_id` varchar(32) DEFAULT NULL COMMENT '该附加组件部署到的 Namespace 标识',
  `addon_id` varchar(32) DEFAULT NULL COMMENT '附加组件唯一标识',
  `addon_name` varchar(255) NOT NULL DEFAULT '' COMMENT '组件名称',
  `addon_version` varchar(64) DEFAULT '' COMMENT '附加组件唯一标识',
  `addon_attrs` longtext DEFAULT NULL COMMENT '附加组件属性',
  `addon_ext` longtext COMMENT '附加组件扩展信息',
  `data_output` longtext COMMENT '附加组件 config var 字典内容',
  PRIMARY KEY (`id`),
  KEY `idx_gmt_create` (`gmt_create`),
  KEY `idx_gmt_modified` (`gmt_modified`),
  KEY `uk_addon_instance_id` (`addon_instance_id`) USING BTREE,
  KEY `idx_addon_name_version` (`namespace_id`,`addon_name`,`addon_version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附加组件实例';

CREATE TABLE `am_addon_instance_task` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `namespace_id` varchar(32) DEFAULT NULL COMMENT '命名空间 ID',
  `addon_id` varchar(64) NOT NULL COMMENT '组件ID',
  `addon_name` varchar(64) DEFAULT NULL COMMENT '组件实例名称',
  `addon_version` varchar(64) NOT NULL COMMENT '组件版本',
  `addon_attrs` longtext DEFAULT NULL COMMENT '附加组件属性',
  `task_status` varchar(16) NOT NULL COMMENT '状态',
  `task_error_message` text COMMENT '错误信息',
  `task_process_id` bigint(20) DEFAULT NULL COMMENT '处理流程ID',
  `task_ext` longtext NOT NULL COMMENT '扩展信息',
  PRIMARY KEY (`id`),
  KEY `idx_gmt_create` (`gmt_create`) USING BTREE,
  KEY `idx_gmt_modified` (`gmt_modified`) USING BTREE,
  KEY `idx_addon_id_version` (`addon_id`,`addon_version`) USING BTREE,
  KEY `idx_task_status` (`task_status`) USING BTREE,
  KEY `idx_task_process_id` (`task_process_id`) USING BTREE,
  KEY `idx_addon_name_version` (`addon_name`,`addon_version`) USING BTREE,
  KEY `idx_namespace` (`namespace_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附加组件实例任务';

CREATE TABLE `am_addon_meta` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '最后修改时间',
  `addon_type` varchar(16) DEFAULT NULL COMMENT '类型（可选 CORE-SERVICE / THIRDPARTY）',
  `addon_id` varchar(32) DEFAULT NULL COMMENT '附加组件唯一标识',
  `addon_version` varchar(32) DEFAULT NULL COMMENT '版本号',
  `addon_label` varchar(255) DEFAULT NULL COMMENT '存储相对路径',
  `addon_description` text COMMENT '附加组件描述',
  `addon_schema` longtext COMMENT '附加组件定义 Schema',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_addon_id_version` (`addon_id`,`addon_version`),
  KEY `idx_addon_type` (`addon_type`),
  KEY `idx_gmt_create` (`gmt_create`),
  KEY `idx_gmt_modified` (`gmt_modified`),
  KEY `idx_addon_label` (`addon_label`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附加组件元信息';

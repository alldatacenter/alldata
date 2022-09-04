CREATE TABLE `am_k8s_micro_service_meta` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `app_id` varchar(32) NOT NULL COMMENT '应用标示',
  `micro_service_id` varchar(128) NOT NULL COMMENT '微服务标示',
  `name` varchar(128) NULL COMMENT '微服务名称',
  `description` varchar(1024) NULL COMMENT '微服务描述',
  `micro_service_ext` text NULL COMMENT '扩展信息',
  `options` mediumtext NULL COMMENT 'options',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_micro_service` (`app_id`,`micro_service_id`)
) DEFAULT CHARACTER SET=utf8mb4 COMMENT='k8s微应用元信息';

CREATE TABLE `am_app_instance` (
	`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
	`gmt_create` datetime NOT NULL COMMENT '创建时间',
	`gmt_modified` datetime NOT NULL COMMENT '修改时间',
	`app_id` varchar(64) NOT NULL COMMENT '应用唯一标示',
	`cluster_id` varchar(32) NOT NULL COMMENT '集群 ID',
	`namespace_id` varchar(32) NOT NULL COMMENT 'Namespace ID',
	`stage_id` varchar(32) NOT NULL COMMENT 'Stage ID',
	`gmt_deploy` datetime NULL COMMENT '部署时间',
	`gmt_last_upgrade` datetime NULL COMMENT '最后升级时间',
	`package_version` varchar(32) NULL COMMENT '应用包版本',
	`latest_version` varchar(32) NULL COMMENT '最新版本',
	`status` varchar(32) NULL COMMENT '应用状态',
	`visit` int NOT NULL COMMENT '可访问',
	`deploy_app_id` bigint unsigned NULL COMMENT '部署单ID',
	PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET=utf8mb4 COMMENT='应用实例表';


CREATE TABLE `am_app_package_task` (
	`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
	`gmt_create` datetime NOT NULL COMMENT '创建时间',
	`gmt_modified` datetime NOT NULL COMMENT '修改时间',
	`app_id` varchar(32) NOT NULL COMMENT '应用唯一标识',
	`app_package_id` bigint unsigned NULL COMMENT '映射 app package 表主键 ID',
	`package_creator` varchar(64) NOT NULL COMMENT '创建人',
	`task_status` varchar(16) NOT NULL COMMENT '任务状态',
	`package_version` varchar(32) NOT NULL COMMENT '版本号',
	`package_options` text NOT NULL COMMENT '包配置选项信息',
	PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用包任务表';


ALTER TABLE `am_component_package_task`
ADD COLUMN `app_package_task_id` bigint NULL COMMENT '应用包任务ID',
ADD KEY `idx_app_package_task_id` (`app_package_task_id`);


ALTER TABLE `am_addon_meta`
	MODIFY COLUMN `addon_label` varchar(255) NULL COMMENT '附加组件名',
	ADD COLUMN `addon_config_schema` text NULL COMMENT '附件组件配置Schema',
	DROP KEY `idx_addon_label`,
	ADD KEY `idx_addon_label` (`addon_label`(190));
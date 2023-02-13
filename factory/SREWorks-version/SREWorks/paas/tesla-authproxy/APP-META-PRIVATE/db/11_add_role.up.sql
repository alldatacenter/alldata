CREATE TABLE `ta_role` (
	`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
	`gmt_create` datetime NOT NULL COMMENT '创建时间',
	`gmt_modified` datetime NOT NULL COMMENT '修改时间',
	`tenant_id` varchar(32) NOT NULL COMMENT '租户 ID',
	`role_id` varchar(64) NOT NULL COMMENT '角色 ID',
	`locale` varchar(16) NOT NULL COMMENT '语言',
	`name` varchar(64) NOT NULL COMMENT '角色名称',
	`description` text NULL COMMENT '角色描述',
	PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET=utf8mb4 COMMENT='角色表';

ALTER TABLE `ta_role`
	ADD UNIQUE KEY `uk_role` (`tenant_id`,`role_id`,`locale`);


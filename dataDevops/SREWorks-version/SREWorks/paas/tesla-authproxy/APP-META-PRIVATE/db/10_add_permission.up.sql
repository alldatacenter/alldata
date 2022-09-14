ALTER TABLE `ta_user`
	ADD COLUMN `user_id` varchar(64) NULL COMMENT '用户 ID',
	ADD COLUMN `tenant_id` varchar(64) NULL COMMENT '租户 ID',
	ADD KEY `idx_user_id` (`user_id`),
	ADD KEY `idx_tenant_id` (`tenant_id`);

CREATE TABLE `ta_role_permission_rel` (
	`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
	`gmt_create` datetime NOT NULL COMMENT '创建时间',
	`gmt_modified` datetime NOT NULL COMMENT '修改时间',
	`tenant_id` varchar(32) NOT NULL COMMENT '租户 ID',
	`service_code` varchar(32) NOT NULL COMMENT '服务代码',
	`role_id` varchar(64) NOT NULL COMMENT '角色 ID',
	`resource_path` varchar(256) NOT NULL COMMENT '资源路径',
	PRIMARY KEY (`id`),
	KEY `idx_location` (`tenant_id`,`service_code`,`role_id`),
	KEY `idx_resource_path` (`resource_path`(64))
) DEFAULT CHARACTER SET=utf8mb4 COMMENT='角色权限关联表';

CREATE TABLE `ta_user_role_rel` (
	`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
	`gmt_create` datetime NOT NULL COMMENT '创建时间',
	`gmt_modified` datetime NOT NULL COMMENT '修改时间',
	`tenant_id` varchar(32) NULL COMMENT '租户 ID',
	`user_id` varchar(64) NULL COMMENT '用户 ID',
	`role_id` varchar(64) NULL COMMENT '角色 ID',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uk_binding` (`tenant_id`,`user_id`,`role_id`)
) DEFAULT CHARACTER SET=utf8mb4 COMMENT='用户角色表';



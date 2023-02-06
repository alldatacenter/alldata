ALTER TABLE `ps_group_projects` ADD `soft_deleted_at` bigint(20) NOT NULL DEFAULT 0 COMMENT '删除时间(时间戳)';
ALTER TABLE `ps_group_projects` drop INDEX `idx_org_id`;
ALTER TABLE `ps_group_projects` ADD UNIQUE KEY `uk_soft_deleted_at_org_id_name` (`soft_deleted_at`,`org_id`,`name`);
ALTER TABLE `ps_group_projects` RENAME TO `erda_project`;
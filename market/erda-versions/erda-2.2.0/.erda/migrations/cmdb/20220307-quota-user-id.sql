ALTER TABLE `ps_group_projects_quota`
    MODIFY `creator_id` VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '创建人 user id',
    MODIFY `updater_id` VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '创建人 user id';

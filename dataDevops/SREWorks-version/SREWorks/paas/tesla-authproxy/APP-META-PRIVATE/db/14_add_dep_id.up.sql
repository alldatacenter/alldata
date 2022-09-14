ALTER TABLE `ta_user`
    ADD COLUMN `dep_id` varchar(32) NULL COMMENT '部门 ID',
    ADD KEY `idx_dep_id` (`dep_id`);

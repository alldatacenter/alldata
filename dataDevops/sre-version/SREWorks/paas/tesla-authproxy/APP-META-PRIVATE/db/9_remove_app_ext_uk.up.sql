ALTER TABLE `ta_app_ext`
    ADD KEY `idx_ext_app_key_unique` (`ext_app_key`),
    DROP KEY `uk_ext_app_key_unique`;

ALTER TABLE `ta_user`
    MODIFY COLUMN `emp_id` varchar(128) NULL COMMENT '工号',
    MODIFY COLUMN `email` varchar(128) NULL COMMENT '邮箱',
    MODIFY COLUMN `aliww` varchar(64) NULL COMMENT '阿里旺旺';

ALTER TABLE `ta_app_ext`
    MODIFY COLUMN `ext_app_name` varchar(64) NOT NULL COMMENT '扩展应用名称',
    MODIFY COLUMN `ext_app_key` varchar(128) NOT NULL COMMENT '扩展应用Key',
    MODIFY COLUMN `memo` varchar(128) NULL COMMENT '备注信息';

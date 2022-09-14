CREATE TABLE `ta_switch_view_user`
(
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`   datetime        NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime        NOT NULL COMMENT '修改时间',
    `emp_id`       varchar(64)     NOT NULL COMMENT '工号',
    `login_name`   varchar(64)     NULL COMMENT '登陆名称',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_id` (`emp_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='允许切换视图的用户表';


CREATE TABLE IF NOT EXISTS `msp_tenant`
(
    `id`                 varchar(100) NOT NULL COMMENT '域id',
    `type`               varchar(10)  NOT NULL COMMENT '类型（dop 、msp）',
    `related_project_id` varchar(100)   NOT NULL COMMENT '项目id',
    `related_workspace`  varchar(10)  NOT NULL COMMENT '环境（ DEV、TEST、STAGING、PROD、DEFAULT）',
    `created_at`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         tinyint(1)   NOT NULL COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT 'MSP Tenant';
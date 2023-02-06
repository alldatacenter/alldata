CREATE TABLE IF NOT EXISTS `msp_project`
(
    `id`           varchar(100) NOT NULL COMMENT 'MSP project ID',
    `name`         varchar(100) NOT NULL COMMENT 'MSP 项目名称',
    `display_name` varchar(100) NOT NULL COMMENT 'MSP 项目展示名称',
    `type`         varchar(10)  NOT NULL COMMENT 'MSP 项目类型',
    `created_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   tinyint(1)   NOT NULL COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT 'MSP Project';
CREATE TABLE `erda_notify_channel`
(
    `id`               varchar(100) NOT NULL COMMENT 'id',
    `name`             varchar(50)  NOT NULL COMMENT '渠道名称',
    `type`             varchar(20)  NOT NULL COMMENT '渠道类型',
    `config`           mediumtext   NOT NULL COMMENT '渠道配置',
    `scope_type`       varchar(20)  NOT NULL COMMENT '域类型',
    `scope_id`         varchar(20)  NOT NULL COMMENT '域id',
    `creator_id`       varchar(100) NOT NULL COMMENT '创建人id',
    `channel_provider` varchar(100) NOT NULL COMMENT '渠道提供商',
    `kms_key`          varchar(200) NOT NULL COMMENT '加密Key',
    `is_enabled`       tinyint(1) NOT NULL COMMENT '是否启用',
    `created_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       tinyint(1) NOT NULL COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'Notify Channel';
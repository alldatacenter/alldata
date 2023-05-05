CREATE TABLE `alert_notify_index`
(
    `id`          bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `notify_id`   bigint(20)    NOT NULL COMMENT '关联通知的notify_id',
    `notify_name` varchar(150)  NOT NULL COMMENT '告警列表中的标题',
    `status`      varchar(150)  NOT NULL COMMENT '通知发送状态',
    `channel`     varchar(150)  NOT NULL COMMENT '通知发送方式',
    `attributes`  varchar(1024) NOT NULL COMMENT '存储告警策略相关值',
    `created_at`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `send_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `scope_type`  varchar(150)  NOT NULL COMMENT '类型',
    `scope_id`    varchar(150)  NOT NULL COMMENT 'id',
    `org_id`      bigint(20)    NOT NULL COMMENT '组织id',
    PRIMARY KEY (`id`)
) ENGINE InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '告警通知索引表'

CREATE TABLE `erda_dashboard_history`
(
    `id`              varchar(36)  NOT NULL COMMENT 'id',
    `type`            varchar(50)  NOT NULL COMMENT '导入导出类型',
    `status`          varchar(50)  NOT NULL COMMENT '操作状态',
    `scope`           varchar(50)  NOT NULL COMMENT 'Scope',
    `scope_id`        varchar(100) NOT NULL COMMENT 'ScopeId',
    `target_scope`    varchar(50)  NOT NULL COMMENT '目标Scope',
    `target_scope_id` varchar(100) NOT NULL COMMENT '目标ScopeId',
    `operator_id`     varchar(100) NOT NULL COMMENT '操作人id',
    `file`            mediumtext   NOT NULL COMMENT '导出文件',
    `file_uuid`       varchar(100) NOT NULL COMMENT '文件id',
    `updated_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间',
    `is_deleted`      tinyint(1) NOT NULL COMMENT '是否删除',
    `error_message`   mediumtext   NOT NULL COMMENT '错误信息',
    `org_id`          varchar(100) NOT NULL COMMENT '组织id',
    `org_name`        varchar(50)  NOT NULL COMMENT '组织名称',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=71 DEFAULT CHARSET=utf8mb4 COMMENT ='大盘导入导出记录表';
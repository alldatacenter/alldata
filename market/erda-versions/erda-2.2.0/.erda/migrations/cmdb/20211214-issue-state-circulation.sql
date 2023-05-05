CREATE TABLE `erda_issue_state_transition`
(
    `id`         varchar(36)  NOT NULL COMMENT 'primary key',
    `created_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '表记录创建时间',
    `updated_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '表记录更新时间',
    `project_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属项目 ID',
    `state_from` bigint(20) NOT NULL DEFAULT '0' COMMENT '更新前状态',
    `state_to`   bigint(20) NOT NULL DEFAULT '0' COMMENT '更新后状态',
    `creator`    varchar(255) NOT NULL DEFAULT '' COMMENT '创建人',
    `issue_id`   bigint(20) NOT NULL DEFAULT '0' COMMENT 'issue_id',
    PRIMARY KEY (`id`),
    KEY          `idx_project_id` (`project_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='issue状态流转表';
CREATE TABLE `erda_workspace`
(
    `id`                    varchar(36)  NOT NULL COMMENT 'Primary Key',
    `created_at`            datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at`            datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    `project_id`            bigint(20) NOT NULL  DEFAULT 0 COMMENT 'project ID, ps_group_projects primary key',
    `org_name`              varchar(64) NOT NULL DEFAULT '' COMMENT '组织名称',
    `org_id`                bigint(20) NOT NULL DEFAULT 0 COMMENT '组织ID',
    `workspace`             varchar(16) NOT NULL DEFAULT '' COMMENT '部署环境',
    `deployment_abilities`  varchar(2048) NOT NULL DEFAULT '' COMMENT '环境集群能力列表, json形式',
    `soft_deleted_at`       bigint(20) NOT NULL DEFAULT 0 COMMENT '删除时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='ERDA环境能力表';
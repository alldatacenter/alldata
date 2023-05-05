create table `project_namespace`
(
    `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary Key',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',

    `project_id`    BIGINT(20)   NOT NULL DEFAULT 0 COMMENT 'ps_group_projects 主键',
    `project_name`  VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '项目名称',
    `cluster_name`  varchar(41)  NOT NULL DEFAULT '' comment '命名空间所在的集群',
    `k8s_namespace` varchar(128) NOT NULL DEFAULT '' comment 'k8s 命名空间',
    Index idx_project_id (`project_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='项目与 K8s 命名空间关系表（可与 s_pod_info 互相补充）';

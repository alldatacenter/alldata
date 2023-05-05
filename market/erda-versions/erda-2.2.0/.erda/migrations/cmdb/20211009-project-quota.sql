CREATE TABLE `ps_group_projects_quota`
(
    `id`                   BIGINT(20)     NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary Key',
    `created_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',

    `project_id`           BIGINT(20)     NOT NULL DEFAULT 0 COMMENT 'ps_group_projects 主键',
    `project_name`         VARCHAR(50)    NOT NULL DEFAULT '' COMMENT '项目名称',

    `prod_cluster_name`    VARCHAR(191)   NOT NULL DEFAULT '' COMMENT '生产环境对应的集群标识',
    `staging_cluster_name` VARCHAR(191)   NOT NULL DEFAULT '' COMMENT '预发环境对应的集群标识',
    `test_cluster_name`    VARCHAR(191)   NOT NULL DEFAULT '' COMMENT '测试环境对应的集群标识',
    `dev_cluster_name`     VARCHAR(191)   NOT NULL DEFAULT '' COMMENT '开发环境对应的集群标识',

    `prod_cpu_quota`       BIGINT NOT NULL DEFAULT 0.0 COMMENT '生产环境 CPU 配额',
    `prod_mem_quota`       BIGINT NOT NULL DEFAULT 0.0 COMMENT '生产环境 Mem 配额',
    `staging_cpu_quota`    BIGINT NOT NULL DEFAULT 0.0 COMMENT '预发环境 CPU 配额',
    `staging_mem_quota`    BIGINT NOT NULL DEFAULT 0.0 COMMENT '预发环境 Mem 配额',
    `test_cpu_quota`       BIGINT NOT NULL DEFAULT 0.0 COMMENT '测试环境 CPU 配额',
    `test_mem_quota`       BIGINT NOT NULL DEFAULT 0.0 COMMENT '测试环境 Mem 配额',
    `dev_cpu_quota`        BIGINT NOT NULL DEFAULT 0.0 COMMENT '开发环境 CPU 配额',
    `dev_mem_quota`        BIGINT NOT NULL DEFAULT 0.0 COMMENT '开发环境 Mem 配额',

    `creator_id`           BIGINT         NOT NULL DEFAULT 0 COMMENT '',
    `updater_id`           BIGINT         NOT NULL DEFAULT 0 COMMENT '',

    INDEX idx_project_id (`project_id`),
    INDEX idx_prod_cluster_name (`prod_cluster_name`),
    INDEX idx_staging_cluster_name (`staging_cluster_name`),
    INDEX idx_test_cluster_name (`test_cluster_name`),
    INDEX idx_dev_cluster_name (`dev_cluster_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='项目各环境 quota 表';

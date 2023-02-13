CREATE TABLE `cmp_project_resource_daily`
(
    `id`           BIGINT(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary Key',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',

    `project_id`   BIGINT(20)   NOT NULL COMMENT '项目 id',
    `project_name` VARCHAR(255) NOT NULL COMMENT '项目标识',
    `cluster_name` VARCHAR(41)  NOT NULL COMMENT '集群标识',

    `cpu_quota`    BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时CPU 配额',
    `cpu_request`  BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时CPU 请求值',
    `mem_quota`    BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时内存配额',
    `mem_request`  BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时内存请求值',
    INDEX idx_project_id (project_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='项目资源变化日表';

CREATE TABLE `cmp_cluster_resource_daily`
(
    `id`            BIGINT(20)  NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary Key',
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',

    `cluster_name`  VARCHAR(41) NOT NULL COMMENT '集群标识',
    `cpu_total`     BIGINT      NOT NULL DEFAULT 0 COMMENT '采集时 CPU 总量',
    `cpu_requested` BIGINT      NOT NULL DEFAULT 0 COMMENT '采集时 CPU 请求值',
    `mem_total`     BIGINT      NOT NULL DEFAULT 0 COMMENT '采集时内存总量',
    `mem_requested` BIGINT      NOT NULL DEFAULT 0 COMMENT '采集时内存请求值',
    INDEX idx_cluster_name (cluster_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='集群资源变化日表';

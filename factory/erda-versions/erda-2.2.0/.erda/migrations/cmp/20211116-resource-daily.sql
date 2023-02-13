ALTER TABLE `cmp_project_resource_daily`
    ADD COLUMN `project_display_name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '项目显示名称',
    ADD COLUMN `owner_user_id`        BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '项目所有者 ID',
    ADD COLUMN `owner_user_name`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '项目所有者用户名',
    ADD COLUMN `owner_user_nickname`  VARCHAR(255) NOT NULL DEFAULT '' COMMENT '项目所有者用户昵称'
;

CREATE INDEX `idx_owner_user_id`
    ON `cmp_project_resource_daily` (owner_user_id)
;

create table `cmp_application_resource_daily`
(
    `id`                       BIGINT(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary Key',
    `created_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',

    `project_id`               BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '项目 id',
    `application_id`           BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '应用 id',
    `application_name`         VARCHAR(255) NOT NULL DEFAULT '' COMMENT '应用标识',
    `application_display_name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '应用显示名称',

    `prod_cpu_request`         BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时 CPU 请求量，单位毫核',
    `prod_mem_request`         BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时内存请求量，单位 Byte',
    `prod_pods_count`          INTEGER      NOT NULL DEFAULT 0 COMMENT '采集时 pods 数量',

    `staging_cpu_request`      BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时 CPU 请求量，单位毫核',
    `staging_mem_request`      BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时内存请求量，单位 Byte',
    `staging_pods_count`       INTEGER      NOT NULL DEFAULT 0 COMMENT '采集时 pods 数量',

    `test_cpu_request`         BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时 CPU 请求量，单位毫核',
    `test_mem_request`         BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时内存请求量，单位 Byte',
    `test_pods_count`          INTEGER      NOT NULL DEFAULT 0 COMMENT '采集时 pods 数量',

    `dev_cpu_request`          BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时 CPU 请求量，单位毫核',
    `dev_mem_request`          BIGINT       NOT NULL DEFAULT 0 COMMENT '采集时内存请求量，单位 Byte',
    `dev_pods_count`           INTEGER      NOT NULL DEFAULT 0 COMMENT '采集时 pods 数量',
    INDEX idx_project_id (project_id),
    INDEX idx_application_id (application_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '应用资源变化日表';
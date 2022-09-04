drop table if exists am_task_host_registry;
CREATE TABLE IF NOT EXISTS `am_task_host_registry`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`    datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`  datetime DEFAULT NULL COMMENT '最后修改时间',
    `hostname`      varchar(128) NOT NULL COMMENT '任务实例主机名',
    `instance_info` longtext DEFAULT NULL COMMENT '任务实例当前信息',
    PRIMARY KEY (`id`),
    KEY `idx_hostname` (`hostname`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务主机注册表';

drop table if exists am_workflow_definition;
CREATE TABLE IF NOT EXISTS `am_workflow_definition`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`    datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`  datetime DEFAULT NULL COMMENT '最后修改时间',
    `workflow_type` varchar(128) NOT NULL COMMENT 'Workflow 类型唯一标识',
    `ds_kind`       varchar(64)  NOT NULL COMMENT '动态脚本 Kind',
    `ds_name`       varchar(32)  NOT NULL COMMENT '动态脚本 Name',
    `ds_revision`   int(64)      NOT NULL COMMENT '动态脚本版本',
    PRIMARY KEY (`id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Workflow 类型定义表';

drop table if exists am_policy_definition;
CREATE TABLE IF NOT EXISTS `am_policy_definition`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime DEFAULT NULL COMMENT '最后修改时间',
    `policy_type`  varchar(128) NOT NULL COMMENT 'Workflow 类型唯一标识',
    `ds_kind`      varchar(64)  NOT NULL COMMENT '动态脚本 Kind',
    `ds_name`      varchar(32)  NOT NULL COMMENT '动态脚本 Name',
    `ds_revision`  int(64)      NOT NULL COMMENT '动态脚本版本',
    PRIMARY KEY (`id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Policy 类型定义表';

drop table if exists am_workflow_instance;
CREATE TABLE IF NOT EXISTS `am_workflow_instance`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`             datetime              DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`           datetime              DEFAULT NULL COMMENT '最后修改时间',
    `app_id`                 varchar(64)  NOT NULL COMMENT '应用 ID',
    `gmt_start`              datetime              DEFAULT NULL COMMENT '开始时间',
    `gmt_end`                datetime              DEFAULT NULL COMMENT '结束时间',
    `workflow_status`        varchar(16)  NOT NULL COMMENT '工作流状态 (PENDING, RUNNING, SUSPEND, SUCCESS, FAILURE, EXCEPTION, TERMINATED)',
    `workflow_error_message` longtext              DEFAULT NULL COMMENT '工作流执行出错信息 (仅 workflow_status==EXCEPTION 下存在)',
    `workflow_configuration` longtext              DEFAULT NULL COMMENT 'Workflow Configuration',
    `workflow_sha256`        varchar(255) NOT NULL COMMENT 'Workflow Configuration SHA256',
    `workflow_options`       longtext              DEFAULT NULL COMMENT 'Workflow 启动选项 (JSON)',
    `workflow_creator`       varchar(64)  NOT NULL COMMENT '创建人',
    `lock_version`           int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_gmt_start` (`gmt_start`),
    KEY `idx_gmt_end` (`gmt_end`),
    KEY `idx_workflow_status` (`workflow_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Workflow 实例表';

drop table if exists am_workflow_task;
CREATE TABLE IF NOT EXISTS `am_workflow_task`
(
    `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime              DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime              DEFAULT NULL COMMENT '最后修改时间',
    `workflow_instance_id` bigint       NOT NULL COMMENT 'Workflow 实例 ID',
    `app_id`               varchar(64)  NOT NULL COMMENT '应用 ID',
    `gmt_start`            datetime              DEFAULT NULL COMMENT '开始时间',
    `gmt_end`              datetime              DEFAULT NULL COMMENT '结束时间',
    `task_type`            varchar(64)  NOT NULL COMMENT 'Workflow 任务类型',
    `task_stage`           varchar(16)  NOT NULL COMMENT 'Workflow 任务节点运行阶段 (pre-render, post-render, post-deploy)',
    `task_properties`      longtext              DEFAULT NULL COMMENT 'Workflow 任务节点属性 (JSONObject 字符串)',
    `task_status`          varchar(16)  NOT NULL COMMENT 'Workflow 任务节点状态 (PENDING, RUNNING, WAITING[完成UserFunction后等待完成], SUSPEND, SUCCESS, FAILURE, EXCEPTION, TERMINATED)',
    `task_error_message`   longtext              DEFAULT NULL COMMENT 'Workflow 任务节点执行出错信息 (仅 task_status==EXCEPTIOIN 下存在)',
    `client_hostname`      varchar(128) NOT NULL COMMENT 'Workflow 任务当前所在执行节点 Hostname',
    `deploy_app_id`        bigint                DEFAULT '0' COMMENT '部署单 ID',
    `lock_version`         int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_gmt_start` (`gmt_start`),
    KEY `idx_gmt_end` (`gmt_end`),
    KEY `idx_instance_id` (`workflow_instance_id`),
    KEY `idx_task` (`task_type`, `task_stage`),
    KEY `idx_task_status` (`task_status`),
    KEY `idx_deploy_app_id` (`deploy_app_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Workflow 任务表';

drop table if exists am_workflow_snapshot;
CREATE TABLE IF NOT EXISTS `am_workflow_snapshot`
(
    `id`                   bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime        DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime        DEFAULT NULL COMMENT '最后修改时间',
    `workflow_instance_id` bigint NOT NULL COMMENT 'Workflow 实例 ID (reference am_workflow_instance.id)',
    `workflow_task_id`     bigint NOT NULL COMMENT 'Workflow 任务 ID',
    `snapshot_context`     longtext        DEFAULT NULL COMMENT '快照内容 Context',
    `snapshot_task`        longtext        DEFAULT NULL COMMENT '快照内容 Task',
    `snapshot_workflow`    longtext        DEFAULT NULL COMMENT '快照内容 Workflow',
    `lock_version`         int    NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`),
    KEY `idx_workflow_instance_id` (`workflow_instance_id`),
    KEY `idx_workflow_task_id` (`workflow_task_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Workflow 快照表';
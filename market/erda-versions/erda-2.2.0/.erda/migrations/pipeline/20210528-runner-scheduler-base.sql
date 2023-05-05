-- MIGRATION_BASE

CREATE TABLE `dice_runner_tasks`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `job_id`           varchar(150) DEFAULT NULL,
    `status`           varchar(150) DEFAULT NULL,
    `open_api_token`   text,
    `context_data_url` varchar(255) DEFAULT NULL,
    `result_data_url`  varchar(255) DEFAULT NULL,
    `commands`         text,
    `targets`          text,
    `work_dir`         varchar(255) DEFAULT NULL,
    `created_at`       datetime     DEFAULT NULL,
    `updated_at`       datetime     DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY                `idx_status` (`status`),
    KEY                `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='action runner 任务执行信息';


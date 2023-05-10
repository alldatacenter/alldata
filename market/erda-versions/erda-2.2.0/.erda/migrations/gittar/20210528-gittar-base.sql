-- MIGRATION_BASE

CREATE TABLE `dice_repo_caches`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT,
    `type_name`  varchar(150) DEFAULT NULL,
    `key_name`   varchar(150) DEFAULT NULL,
    `value`      text,
    `created_at` timestamp NULL DEFAULT NULL,
    `updated_at` timestamp NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY          `type_name` (`type_name`),
    KEY          `key_name` (`key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Gittar 仓库缓存表';

CREATE TABLE `dice_repo_check_runs`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT,
    `repo_id`      bigint(20) DEFAULT NULL,
    `name`         varchar(100) COLLATE utf8mb4_bin DEFAULT NULL,
    `type`         varchar(100) COLLATE utf8mb4_bin DEFAULT NULL,
    `external_id`  varchar(100) COLLATE utf8mb4_bin DEFAULT NULL,
    `commit`       varchar(100) COLLATE utf8mb4_bin DEFAULT NULL,
    `status`       varchar(50) COLLATE utf8mb4_bin  DEFAULT '',
    `output`       text COLLATE utf8mb4_bin,
    `result`       varchar(100) COLLATE utf8mb4_bin DEFAULT '',
    `created_at`   timestamp NOT NULL               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `completed_at` timestamp NULL DEFAULT NULL,
    `mr_id`        int(11) NOT NULL DEFAULT '0',
    `pipeline_id`  int(11) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY            `idx_repo_id` (`repo_id`),
    KEY            `idx_name` (`name`),
    KEY            `idx_type` (`type`),
    KEY            `idx_external_id` (`external_id`),
    KEY            `idx_commit` (`commit`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Gittar check-run 表';

CREATE TABLE `dice_repo_files`
(
    `id`         bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `repo_id`    bigint(20) DEFAULT NULL,
    `commit_id`  varchar(64) DEFAULT NULL,
    `remark`     text,
    `uuid`       varchar(32) DEFAULT NULL,
    `deleted_at` datetime    DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8mb4 COMMENT='Gittar 文件表(目前只有备份)';

CREATE TABLE `dice_repo_merge_requests`
(
    `id`                   bigint(20) NOT NULL AUTO_INCREMENT,
    `repo_id`              bigint(20) DEFAULT NULL,
    `title`                varchar(255) DEFAULT NULL,
    `description`          text,
    `state`                varchar(150) DEFAULT NULL,
    `author_id`            varchar(150) DEFAULT NULL,
    `assignee_id`          varchar(150) DEFAULT NULL,
    `merge_user_id`        varchar(255) DEFAULT NULL,
    `close_user_id`        varchar(255) DEFAULT NULL,
    `merge_commit_sha`     varchar(255) DEFAULT NULL,
    `repo_merge_id`        int(11) DEFAULT NULL,
    `source_branch`        varchar(255) DEFAULT NULL,
    `source_sha`           varchar(255) DEFAULT NULL,
    `target_branch`        varchar(255) DEFAULT NULL,
    `target_sha`           varchar(255) DEFAULT NULL,
    `remove_source_branch` tinyint(1) DEFAULT NULL,
    `created_at`           timestamp NULL DEFAULT NULL,
    `updated_at`           timestamp NULL DEFAULT NULL,
    `merge_at`             timestamp NULL DEFAULT NULL,
    `close_at`             timestamp NULL DEFAULT NULL,
    `score`                int(11) NOT NULL DEFAULT '0',
    `score_num`            int(11) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_unique_merge_id` (`repo_id`,`repo_merge_id`),
    KEY                    `idx_author_id` (`author_id`),
    KEY                    `idx_assignee_id` (`assignee_id`),
    KEY                    `idx_repo_id` (`repo_id`),
    KEY                    `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Gittar 合并请求';

CREATE TABLE `dice_repo_notes`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT,
    `repo_id`       bigint(20) DEFAULT NULL,
    `type`          varchar(150) DEFAULT NULL,
    `discussion_id` varchar(255) DEFAULT NULL,
    `old_commit_id` varchar(255) DEFAULT NULL,
    `new_commit_id` varchar(255) DEFAULT NULL,
    `merge_id`      bigint(20) DEFAULT NULL,
    `note`          text,
    `data`          text,
    `author_id`     varchar(255) DEFAULT NULL,
    `created_at`    timestamp NULL DEFAULT NULL,
    `updated_at`    timestamp NULL DEFAULT NULL,
    `score`         int(11) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY             `idx_type` (`type`),
    KEY             `idx_merge_id` (`merge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Gittar 评论表';

CREATE TABLE `dice_repo_web_hook_tasks`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`       timestamp NULL DEFAULT NULL,
    `updated_at`       timestamp NULL DEFAULT NULL,
    `deleted_at`       timestamp NULL DEFAULT NULL,
    `hook_id`          bigint(20) DEFAULT NULL,
    `url`              varchar(255) DEFAULT NULL,
    `event`            varchar(255) DEFAULT NULL,
    `is_delivered`     tinyint(1) DEFAULT NULL,
    `is_succeed`       tinyint(1) DEFAULT NULL,
    `request_content`  text,
    `response_content` text,
    `response_status`  varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY                `idx_gittar_web_hook_tasks_deleted_at` (`deleted_at`),
    KEY                `idx_hook_id` (`hook_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Gittar webhook 任务表';

CREATE TABLE `dice_repo_web_hooks`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`  timestamp NULL DEFAULT NULL,
    `updated_at`  timestamp NULL DEFAULT NULL,
    `deleted_at`  timestamp NULL DEFAULT NULL,
    `hook_type`   varchar(150) DEFAULT NULL,
    `name`        varchar(150) DEFAULT NULL,
    `repo_id`     bigint(20) DEFAULT NULL,
    `token`       varchar(255) DEFAULT NULL,
    `url`         varchar(255) DEFAULT NULL,
    `is_active`   tinyint(1) DEFAULT NULL,
    `push_events` tinyint(1) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY           `idx_repo_id` (`repo_id`),
    KEY           `idx_gittar_web_hooks_deleted_at` (`deleted_at`),
    KEY           `idx_hook_type` (`hook_type`),
    KEY           `idx_hook_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Gittar webhoob 表';

CREATE TABLE `dice_repos`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT,
    `org_id`       bigint(20) DEFAULT NULL,
    `project_id`   bigint(20) DEFAULT NULL,
    `app_id`       bigint(20) DEFAULT NULL,
    `org_name`     varchar(150) DEFAULT NULL,
    `project_name` varchar(150) DEFAULT NULL,
    `app_name`     varchar(150) DEFAULT NULL,
    `path`         varchar(150) DEFAULT NULL,
    `size`         bigint(20) DEFAULT NULL,
    `config`       text,
    `is_external`  tinyint(1) DEFAULT '0',
    `is_locked`    tinyint(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY            `idx_org_name` (`org_name`),
    KEY            `idx_project_name` (`project_name`),
    KEY            `idx_app_name` (`app_name`),
    KEY            `idx_path` (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Gittar 仓库表';


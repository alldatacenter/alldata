CREATE TABLE `erda_member_active_rank` (
    `id` varchar(36) NOT NULL COMMENT 'primary key',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    `org_id` varchar(36) NOT NULL COMMENT 'org id',
    `user_id` varchar(36) NOT NULL COMMENT 'user id',
    `issue_score` bigint(20) NOT NULL DEFAULT '0' COMMENT 'issue',
    `commit_score` bigint(20) NOT NULL DEFAULT '0' COMMENT 'commit',
    `quality_score` bigint(20) NOT NULL DEFAULT '0' COMMENT 'quality',
    `total_score` bigint(20) NOT NULL DEFAULT '0' COMMENT 'total',
    `soft_deleted_at` bigint(20) NOT NULL DEFAULT 0 COMMENT 'deleted at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_soft_deleted_at_org_id_user_id` (`soft_deleted_at`,`org_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='member active rank';

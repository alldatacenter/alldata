CREATE TABLE IF NOT EXISTS `erda_project_home` (
    `id` varchar(36) NOT NULL COMMENT 'primary key',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    `project_id` varchar(36) NOT NULL COMMENT 'project id',
    `readme` text NOT NULL COMMENT 'text',
    `links` text NOT NULL COMMENT 'links',
    `updater_id`    varchar(191)  NOT NULL default '' COMMENT 'updater user id',
    `soft_deleted_at` bigint(20) NOT NULL DEFAULT 0 COMMENT 'deleted at',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='project home content';

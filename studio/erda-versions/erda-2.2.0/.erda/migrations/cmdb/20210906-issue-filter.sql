CREATE TABLE `erda_issue_filter_bookmark` (
    `id` varchar(36) NOT NULL COMMENT 'primary key',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'entity create time',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'entity update time',
    `name` varchar(100) NOT NULL COMMENT 'bookmark name',
    `user_id` varchar(36) NOT NULL COMMENT 'user who has bookmarks',
    `project_id` varchar(36) NOT NULL COMMENT 'project that bookmarks belong to',
    `page_key` varchar(50) NOT NULL COMMENT 'different 8 pages',
    `filter_entity` varchar(255) NOT NULL COMMENT 'base64 of filter json',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_project_id` (`user_id`, `project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='personal issue filter bookmark of project';

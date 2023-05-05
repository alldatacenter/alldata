CREATE TABLE `erda_token` (
  `id` varchar(36) NOT NULL COMMENT 'primary key',
  `secret_key` char(32) NOT NULL DEFAULT '' COMMENT 'secret key',
  `code` varchar(191) NOT NULL DEFAULT '' COMMENT 'code',
  `access_key` varchar(4096) NOT NULL DEFAULT '' COMMENT 'access key',
  `status` varchar(4096) NOT NULL DEFAULT '' COMMENT 'status',
  `description` varchar(255) NOT NULL DEFAULT '' COMMENT 'description',
  `data` text NOT NULL COMMENT 'data',
  `scope` varchar(24) NOT NULL DEFAULT '' COMMENT 'scope',
  `scope_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'scopeId',
  `type` varchar(24) NOT NULL DEFAULT '' COMMENT 'token type',
  `creator_id` varchar(255) NOT NULL DEFAULT '' COMMENT 'creator',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  `expired_at` datetime DEFAULT NULL COMMENT 'expired time',
  `refresh` varchar(4096) NOT NULL DEFAULT '' COMMENT 'refresh',
  `soft_deleted_at` bigint(20) NOT NULL DEFAULT '0' COMMENT 'deleted at',
  PRIMARY KEY (`id`),
  KEY `idx_expired_at` (`expired_at`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_scope` (`scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='erda token';

update erda_access_key set creator_id = '' where creator_id is null;

INSERT INTO `erda_token` (id, secret_key, access_key, data, description, scope, scope_id, type, creator_id, created_at, updated_at)
SELECT UUID(), secret_key, access_key, '', description, scope, scope_id, 'AccessKey', creator_id, created_at, updated_at
FROM `erda_access_key`;

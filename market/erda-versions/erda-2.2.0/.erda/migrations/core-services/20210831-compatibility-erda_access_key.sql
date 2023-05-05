ALTER TABLE `erda_access_key` MODIFY COLUMN `access_key_id` char(24) NOT NULL DEFAULT '' COMMENT 'deprecated!';
ALTER TABLE `erda_access_key` MODIFY COLUMN `is_system` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'deprecated!';
ALTER TABLE `erda_access_key` MODIFY COLUMN `id` VARCHAR(36) NOT NULL COMMENT 'Primary Key';
DROP INDEX `uk_ak` ON `erda_access_key`;
ALTER TABLE `erda_access_key` ADD UNIQUE KEY `uk_ak` (`access_key`);
ALTER TABLE `erda_access_key` ADD `scope` varchar(24) DEFAULT '' NOT NULL COMMENT 'Scope';
ALTER TABLE `erda_access_key` ADD `scope_id` varchar(128) DEFAULT '' NOT NULL COMMENT 'ScopeId';
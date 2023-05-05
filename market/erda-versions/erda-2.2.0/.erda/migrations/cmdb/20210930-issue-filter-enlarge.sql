ALTER TABLE `erda_issue_filter_bookmark` MODIFY COLUMN `filter_entity` varchar(4096) NOT NULL COMMENT 'base64 of filter json';

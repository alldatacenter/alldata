ALTER TABLE `pipeline_definition` ADD COLUMN  `ref` varchar(40) NOT NULL DEFAULT '' COMMENT 'branch';

UPDATE `pipeline_definition` SET ref = (SELECT ref FROM `pipeline_source` WHERE id = `pipeline_definition`.pipeline_source_id ) WHERE soft_deleted_at = 0
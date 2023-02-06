ALTER TABLE `pipeline_definition` ADD COLUMN  `location` varchar(100) NOT NULL DEFAULT '' COMMENT '地址(cicd/org/project)';
ALTER TABLE `pipeline_definition` DROP INDEX `idx_name`;
UPDATE pipeline_definition set location = SUBSTRING_INDEX((SELECT remote FROM pipeline_source WHERE pipeline_definition.pipeline_source_id = pipeline_source.id),'/',2);
ALTER TABLE `pipeline_definition` ADD UNIQUE KEY `uk_location_name` (`soft_deleted_at`,`location`,`name`);

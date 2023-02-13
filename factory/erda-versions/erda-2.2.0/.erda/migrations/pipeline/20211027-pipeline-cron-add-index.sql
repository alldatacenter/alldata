ALTER TABLE `pipeline_crons` ADD INDEX `idx_enable` (`enable`);
ALTER TABLE `pipeline_crons` ADD INDEX `idx_namespace_enable` (`pipeline_source`, `pipeline_yml_name`, `enable`);

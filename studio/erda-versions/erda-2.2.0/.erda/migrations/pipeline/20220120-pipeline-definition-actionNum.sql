ALTER TABLE `pipeline_definition` ADD COLUMN  `total_action_num` int(20) NOT NULL DEFAULT '0' COMMENT '总action数量';
ALTER TABLE `pipeline_definition` ADD COLUMN  `executed_action_num` int(20) NOT NULL DEFAULT '0' COMMENT '执行结束的action数量';

UPDATE pipeline_definition set location = CONCAT('cicd/',SUBSTRING_INDEX((SELECT remote FROM pipeline_source WHERE pipeline_definition.pipeline_source_id = pipeline_source.id),'/',2));
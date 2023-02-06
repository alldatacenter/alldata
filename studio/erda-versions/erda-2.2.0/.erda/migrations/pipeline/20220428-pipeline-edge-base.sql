ALTER TABLE `pipeline_bases` ADD COLUMN `is_edge` TINYINT(1) COMMENT '是否是边缘集群' AFTER `pipeline_definition_id`;
ALTER TABLE `pipeline_tasks` ADD COLUMN `is_edge` TINYINT(1) COMMENT '是否是边缘集群' AFTER `time_updated`;

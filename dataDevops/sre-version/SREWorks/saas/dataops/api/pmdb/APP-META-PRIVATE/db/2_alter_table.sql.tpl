ALTER TABLE `metric` RENAME INDEX `key_uid` TO `uk_uid`;
ALTER TABLE `metric` RENAME INDEX `key_name` TO `idx_name`;

ALTER TABLE `metric_instance` DROP FOREIGN KEY `metric_instance_ibfk_1`;
ALTER TABLE `metric_instance` RENAME INDEX `key_uid` TO `uk_uid`;
ALTER TABLE `metric_instance` RENAME INDEX `metric_instance_ibfk_1` TO `idx_metric_id`;

ALTER TABLE `metric_anomaly_detection_config` DROP FOREIGN KEY `metric_instance_ibfk_2`;
ALTER TABLE `metric_anomaly_detection_config` RENAME INDEX `metric_instance_ibfk_2` TO `idx_metric_id`;

ALTER TABLE `metric` MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT COMMENT '指标ID';

ALTER TABLE `metric_instance` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '实例ID';
ALTER TABLE `metric_instance` MODIFY COLUMN `metric_id` int NOT NULL COMMENT '指标ID';
-- ALTER TABLE `metric_instance` ADD INDEX `idx_metric_id`(`metric_id`);

ALTER TABLE `metric_anomaly_detection_config` MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT COMMENT '配置ID';
ALTER TABLE `metric_anomaly_detection_config` MODIFY COLUMN `metric_id` int NOT NULL COMMENT '指标ID';
-- ALTER TABLE `metric_anomaly_detection_config` ADD INDEX `idx_metric_id`(`metric_id`);

ALTER TABLE `datasource` RENAME INDEX `key_app` TO `idx_app`;
ALTER TABLE `datasource` RENAME INDEX `key_type_name` TO `idx_type_name`;
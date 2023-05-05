ALTER TABLE `sp_trace_request_history` MODIFY COLUMN `response_body` MEDIUMTEXT DEFAULT NULL COMMENT '请求响应体';

ALTER TABLE `sp_trace_request_history` ADD COLUMN `name` VARCHAR(100) NOT NULL COMMENT '链路调试名称' AFTER `request_id`;

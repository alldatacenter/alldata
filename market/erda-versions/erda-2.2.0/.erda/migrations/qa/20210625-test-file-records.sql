CREATE TABLE `dice_test_file_records` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key id',
  `file_name` varchar(50) NOT NULL COMMENT 'file name',
  `description` varchar(255) NOT NULL COMMENT 'file description',
  `api_file_uuid` varchar(50) NOT NULL COMMENT 'api files uuid',
  `project_id` bigint(20) unsigned NOT NULL COMMENT 'project id',
  `type` varchar(255) NOT NULL COMMENT 'task type import export',
  `state` varchar(50) NOT NULL COMMENT 'test file task state',
  `operator_id` varchar(50) NOT NULL COMMENT 'test file operator info',
  `extra` varchar(2048) NOT NULL COMMENT 'file extra request info',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='test file records table';

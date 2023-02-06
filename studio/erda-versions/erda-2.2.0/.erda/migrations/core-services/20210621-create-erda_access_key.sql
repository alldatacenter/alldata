CREATE TABLE `erda_access_key` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `access_key_id` char(24) NOT NULL COMMENT 'Access Key ID',
  `secret_key` char(32) NOT NULL COMMENT 'Secret Key',
  `is_system` tinyint(1) NOT NULL COMMENT 'identify weather used for system component communication',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'status of access key',
  `subject_type` varchar(64) NOT NULL COMMENT 'authentication subject type. eg: organization, micro_service',
  `subject` varchar(256) NOT NULL COMMENT 'authentication subject identifier. eg: id, name or something',
  `description` varchar(255) NOT NULL DEFAULT "" COMMENT 'description',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ak` (`access_key_id`),
  UNIQUE KEY `uk_sk` (`secret_key`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COMMENT='store secret key pair';

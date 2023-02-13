CREATE TABLE `erda_issue_subscriber` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  `issue_id` bigint(20) NOT NULL COMMENT 'issuer id',
  `user_id` varchar(255) NOT NULL COMMENT 'subscriber',
  PRIMARY KEY (`id`),
  KEY `idx_issue_id_user_id` (`issue_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT DYNAMIC COMMENT='subscribe for issue';

CREATE TABLE `kratos_uc_userid_mapping` (
  `id` varchar(50) NOT NULL COMMENT 'uc userid',
  `user_id` varchar(191) NOT NULL COMMENT 'kratos user uuid',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='id mapping';

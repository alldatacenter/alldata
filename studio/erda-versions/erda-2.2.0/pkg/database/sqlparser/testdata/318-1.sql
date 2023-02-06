CREATE TABLE `dice_api_assets` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `asset_id` varchar(191) DEFAULT NULL,
  `asset_name` varchar(191) DEFAULT NULL,
  `desc` varchar(1024) DEFAULT NULL,
  `logo` varchar(1024) DEFAULT NULL,
  `org_id` bigint(20) DEFAULT NULL,
  `project_id` bigint(20) DEFAULT NULL,
  `app_id` bigint(20) DEFAULT NULL,
  `creator_id` varchar(191) DEFAULT NULL,
  `updater_id` varchar(191) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4;

CREATE TABLE `dice_api_asset_versions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `org_id` bigint(20) DEFAULT NULL,
  `asset_id` varchar(191) DEFAULT NULL,
  `major` int(11) DEFAULT NULL,
  `minor` int(11) DEFAULT NULL,
  `patch` int(11) DEFAULT NULL,
  `desc` varchar(1024) DEFAULT '',
  `spec_protocol` varchar(32) DEFAULT NULL,
  `creator_id` varchar(191) DEFAULT NULL,
  `updater_id` varchar(191) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4;

CREATE TABLE `dice_api_asset_version_specs` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `org_id` bigint(20) DEFAULT NULL,
  `asset_id` varchar(191) DEFAULT NULL,
  `version_id` bigint(20) DEFAULT NULL,
  `spec_protocol` varchar(32) DEFAULT NULL,
  `spec` longtext,
  `creator_id` varchar(191) DEFAULT NULL,
  `updater_id` varchar(191) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4;

CREATE TABLE `dice_api_asset_version_instances` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(191) DEFAULT NULL,
  `asset_id` varchar(191) DEFAULT NULL,
  `version_id` bigint(20) DEFAULT NULL,
  `type` varchar(32) DEFAULT NULL,
  `runtime_id` bigint(20) DEFAULT NULL,
  `service_name` varchar(191) DEFAULT NULL,
  `endpoint_id` varchar(191) DEFAULT NULL,
  `url` varchar(1024) DEFAULT NULL,
  `creator_id` varchar(191) DEFAULT NULL,
  `updater_id` varchar(191) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4;

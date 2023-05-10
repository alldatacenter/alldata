# base
# baseline

CREATE TABLE `dice_api_assets` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT Comment 'id',
  `asset_id` varchar(191) not NULL Comment '',
  `asset_name` varchar(191) not NULL Comment '',
  `descriptions` varchar(1024) not NULL Comment '',
  `logo` varchar(1024) not NULL Comment '',
  `org_id` bigint(20) not NULL Comment '',
  `project_id` bigint(20) not NULL Comment '',
  `app_id` bigint(20) not NULL Comment '',
  `creator_id` varchar(191) not NULL Comment '',
  `updater_id` varchar(191) not NULL Comment '',
  `created_at` datetime not NULL default CURRENT_TIMESTAMP Comment '',
  `updated_at` datetime not NULL default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP Comment '',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 comment '';

CREATE TABLE `dice_api_asset_versions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT Comment '',
  `org_id` bigint(20) not NULL Comment '',
  `asset_id` varchar(191) not NULL Comment '',
  `major` int(11) not NULL Comment '',
  `minor` int(11) not NULL Comment '',
  `patch` int(11) not NULL Comment '',
  `descriptions` varchar(1024) not null Comment '',
  `spec_protocol` varchar(32) not NULL Comment '',
  `creator_id` varchar(191) not NULL Comment '',
  `updater_id` varchar(191) not NULL Comment '',
  `created_at` datetime not NULL default CURRENT_TIMESTAMP Comment '',
  `updated_at` datetime not NULL default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP Comment '',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 comment '';

CREATE TABLE `dice_api_asset_version_specs` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT Comment '',
  `org_id` bigint(20) not NULL Comment '',
  `asset_id` varchar(191) not NULL Comment '',
  `version_id` bigint(20) not NULL Comment '',
  `spec_protocol` varchar(32) not NULL Comment '',
  `spec` longtext not null Comment '',
  `creator_id` varchar(191) not NULL Comment '',
  `updater_id` varchar(191) not NULL Comment '',
  `created_at` datetime not NULL default CURRENT_TIMESTAMP Comment '',
  `updated_at` datetime not NULL default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP Comment '',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 comment '';

CREATE TABLE `dice_api_asset_version_instances` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT Comment '',
  `naming` varchar(191) not NULL Comment '',
  `asset_id` varchar(191) not NULL Comment '',
  `version_id` bigint(20) not NULL Comment '',
  `ins_type` varchar(32) not NULL Comment '',
  `runtime_id` bigint(20) not NULL Comment '',
  `service_name` varchar(191) not NULL Comment '',
  `endpoint_id` varchar(191) not NULL Comment '',
  `url` varchar(1024) not NULL Comment '',
  `creator_id` varchar(191) not NULL Comment '',
  `updater_id` varchar(191) not NULL Comment '',
  `created_at` datetime not NULL default CURRENT_TIMESTAMP Comment '',
  `updated_at` datetime not NULL default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP Comment '',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 comment '';
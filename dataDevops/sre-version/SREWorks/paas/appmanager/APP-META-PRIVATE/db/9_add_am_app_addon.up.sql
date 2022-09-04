CREATE TABLE `am_app_addon` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `app_id` varchar(32) NOT NULL COMMENT '应用唯一标识',
  `addon_type` varchar(16) NOT NULL COMMENT '类型（可选 CORE-SERVICE / THIRDPARTY）',
  `addon_id` varchar(32) NOT NULL COMMENT '附加组件唯一标识',
  `addon_version` varchar(32) NOT NULL COMMENT '版本号',
  `addon_config` longtext NOT NULL COMMENT '附加组件配置',
  `name` varchar(64) NOT NULL COMMENT '名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_addon` (`app_id`,`name`),
  KEY `idx_app_addon_type` (`app_id`,`addon_type`)
)  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用addon关联表';

CREATE TABLE `am_micro_service_meta` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '修改时间',
  `app_id` varchar(32) NOT NULL COMMENT '应用标示',
  `micro_service_id` varchar(128) NOT NULL COMMENT '微服务标示',
  `name` varchar(128) NOT NULL COMMENT '微服务名称',
  `description` varchar(1024) DEFAULT NULL COMMENT '描述信息',
  `git_repo` varchar(256) NOT NULL COMMENT 'git 地址',
  `branch` varchar(1024) NOT NULL COMMENT '代码分支',
  `language` varchar(64) DEFAULT NULL COMMENT '开发语言',
  `service_type` varchar(50) NOT NULL COMMENT '服务类型',
  `micro_service_ext` text COMMENT '扩展信息 JSON',
  `commit` varchar(128) DEFAULT NULL COMMENT 'commit',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_micro_service` (`app_id`,`micro_service_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='应用微服务元信息';

CREATE TABLE `erda_pipeline_action` (
  `id` varchar(36) NOT NULL DEFAULT '' COMMENT '主键',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `soft_deleted_at` bigint(20) NOT NULL COMMENT '是否删除',
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '名称',
  `category` varchar(255) NOT NULL DEFAULT '' COMMENT '分类',
  `display_name` varchar(255) NOT NULL DEFAULT '' COMMENT '展示名称',
  `logo_url` varchar(500) NOT NULL DEFAULT '' COMMENT '图标',
  `desc` varchar(500) NOT NULL DEFAULT '' COMMENT '描述',
  `is_public` tinyint(1) NOT NULL COMMENT '是否公开',
  `is_default` tinyint(1) NOT NULL COMMENT '是否默认',
  `readme` longtext NOT NULL COMMENT 'readme 信息',
  `dice` text NOT NULL COMMENT 'dice.yml 信息',
  `spec` text NOT NULL COMMENT 'spec.yml 信息',
  `version_info` varchar(128) NOT NULL DEFAULT '' COMMENT '版本',
  `location` varchar(255) NOT NULL DEFAULT '' COMMENT '地址。用作过滤',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线 Action 定义表';

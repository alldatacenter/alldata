CREATE TABLE `erda_issue_relation` (
  `id` varchar(36) NOT NULL DEFAULT '' COMMENT '主键',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `soft_deleted_at` bigint(20) NOT NULL COMMENT '是否删除',
  `relation` varchar(50) NOT NULL DEFAULT '' COMMENT '关联主键',
  `issue_id` bigint(20) NOT NULL COMMENT 'issue id',
  `type` varchar(40) NOT NULL DEFAULT '' COMMENT '关联类型',
  `extra` varchar(1000) NOT NULL DEFAULT '' COMMENT '标签',
  `org_id` bigint(20) NOT NULL COMMENT '企业id',
  `org_name` varchar(50) NOT NULL DEFAULT '' COMMENT '企业名称',
  PRIMARY KEY (`id`),
  KEY `idx_delete_org_relation` (`soft_deleted_at`,`org_id`,`relation`,`type`),
  KEY `idx_delete_org_issue` (`soft_deleted_at`,`org_id`,`issue_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务工作流程节点表';

/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_permission_meta   */
/******************************************/
CREATE TABLE `ta_permission_meta` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `service_code` varchar(50) NOT NULL COMMENT 'Tesla服务标识、可以是Aone应用ID',
  `service_name` varchar(50) NOT NULL COMMENT 'tesa服务名称',
  `permission_code` varchar(100) NOT NULL COMMENT '权限标识',
  `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
  `permission_type` tinyint(4) NOT NULL COMMENT '权限类型',
  `is_enable` tinyint(4) NOT NULL COMMENT '是否生效',
  `apply_url` varchar(200) DEFAULT NULL COMMENT '权限申请页面URL',
  `memo` varchar(100) DEFAULT NULL COMMENT '备注信息',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_permission_name` (`permission_name`),
  KEY `idx_service_code` (`service_code`),
  KEY `idx_service_code_permission_code` (`service_code`,`permission_code`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8 COMMENT='tesla服务权限元数据'
;


/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_service_meta   */
/******************************************/
CREATE TABLE `ta_service_meta` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `service_code` varchar(50) NOT NULL COMMENT '服务标识',
  `service_name` varchar(50) NOT NULL COMMENT '服务名称',
  `owners` varchar(200) NOT NULL COMMENT '负责人工号',
  `memo` varchar(100) NOT NULL COMMENT '备注，服务说明',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='tesla服务元数据表'
;

ALTER TABLE `ta_permission_res`
	ADD UNIQUE KEY `uk_appid_permissionid` (`app_id`,`permission_id`)
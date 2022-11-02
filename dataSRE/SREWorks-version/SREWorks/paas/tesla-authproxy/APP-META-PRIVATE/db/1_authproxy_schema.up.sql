/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_app   */
/******************************************/
DROP TABLE IF EXISTS `ta_app`;
CREATE TABLE `ta_app` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` varchar(45) NOT NULL COMMENT '应用名称',
  `app_accesskey` varchar(45) NOT NULL COMMENT '在ACL上注册的accessKey',
  `admin_role_name` varchar(45) DEFAULT NULL COMMENT '该应用的默认角色',
  `memo` varchar(45) DEFAULT NULL COMMENT '备注信息',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '修改时间',
  `index_url` varchar(1000) DEFAULT NULL COMMENT '应用首页地址',
  `login_enable` int(11) NOT NULL DEFAULT '0' COMMENT '是否开启登录验证',
  `login_url` varchar(1000) DEFAULT NULL COMMENT '登录 URL 地址（固定）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_id_unique` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COMMENT='对接权代服务的应用信息表'
;

/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_app_ext   */
/******************************************/
DROP TABLE IF EXISTS `ta_app_ext`;
CREATE TABLE `ta_app_ext` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ext_app_name` varchar(45) NOT NULL COMMENT '扩展应用名称',
  `ext_app_key` varchar(45) NOT NULL COMMENT '扩展应用Key',
  `memo` varchar(45) DEFAULT NULL COMMENT '备注信息',
  `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ext_app_name_unique` (`ext_app_name`),
  UNIQUE KEY `uk_ext_app_key_unique` (`ext_app_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对接权代服务的扩展应用信息表'
;

/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_config   */
/******************************************/
DROP TABLE IF EXISTS `ta_config`;
CREATE TABLE `ta_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `name` varchar(256) NOT NULL COMMENT '配置项名称',
  `value` text NOT NULL COMMENT '配置项值',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`(64))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='系统配置项'
;

/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_menu   */
/******************************************/
DROP TABLE IF EXISTS `ta_menu`;
CREATE TABLE `ta_menu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` varchar(45) NOT NULL COMMENT '应用名',
  `menu_code` varchar(50) DEFAULT NULL COMMENT '菜单编码',
  `menu_name` varchar(45) DEFAULT NULL COMMENT '菜单名称',
  `is_leaf` int(11) DEFAULT NULL COMMENT '是否叶子菜单',
  `parent_code` varchar(50) DEFAULT NULL COMMENT '上级菜单编码',
  `icon` varchar(20) DEFAULT NULL COMMENT '菜单图标',
  `idx` int(11) DEFAULT NULL COMMENT '排序值',
  `menu_title` varchar(45) DEFAULT NULL COMMENT '菜单标题',
  `menu_url` varchar(45) DEFAULT NULL COMMENT '菜单URL',
  `header_title_set` varchar(45) DEFAULT NULL COMMENT '自定义扩展属性',
  `is_enable` int(11) DEFAULT NULL COMMENT '是否可用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_code_unique` (`menu_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单信息表'
;

/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_op_log   */
/******************************************/
DROP TABLE IF EXISTS `ta_op_log`;
CREATE TABLE `ta_op_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `op_time` datetime DEFAULT NULL COMMENT '操作时间',
  `op_user` varchar(45) DEFAULT NULL COMMENT '操作人',
  `op_action` varchar(45) DEFAULT NULL COMMENT '操作',
  `op_result` int(11) DEFAULT NULL COMMENT '结果',
  `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '更新时间',
  `memo` varchar(45) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_id_unique` (`id`),
  KEY `idx_gmt_create` (`gmt_create`),
  KEY `idx_user_action` (`op_user`,`op_action`)
) ENGINE=InnoDB AUTO_INCREMENT=5769 DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表'
;

/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_permission_res   */
/******************************************/
DROP TABLE IF EXISTS `ta_permission_res`;
CREATE TABLE `ta_permission_res` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` varchar(45) NOT NULL COMMENT '应用名称',
  `res_path` varchar(100) NOT NULL COMMENT '资源路径',
  `memo` varchar(45) DEFAULT NULL COMMENT '备注',
  `permission_id` varchar(100) NOT NULL COMMENT '权限点标识',
  `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_id_unique` (`id`),
  KEY `idx_ta_permission_res_app_id_res_path` (`app_id`,`res_path`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='权限资源信息表'
;

/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_role_menu_rel   */
/******************************************/
DROP TABLE IF EXISTS `ta_role_menu_rel`;
CREATE TABLE `ta_role_menu_rel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` varchar(45) NOT NULL COMMENT '应用名称',
  `role_name` varchar(45) DEFAULT NULL COMMENT '角色名称',
  `menu_code` varchar(50) DEFAULT NULL COMMENT '菜单编码',
  `memo` varchar(45) DEFAULT NULL COMMENT '备注信息',
  `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关系表'
;

/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_user_callback   */
/******************************************/
DROP TABLE IF EXISTS `ta_user_callback`;
CREATE TABLE `ta_user_callback` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `trigger_type` varchar(64) NOT NULL COMMENT '回调触发类型',
  `url` varchar(1024) NOT NULL COMMENT '回调 URL 地址',
  PRIMARY KEY (`id`),
  KEY `idx_gmt_create` (`gmt_create`),
  KEY `idx_trigger_type` (`trigger_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户操作回调表'
;

/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_user   */
/******************************************/
CREATE TABLE IF NOT EXISTS `ta_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bid` varchar(45) DEFAULT NULL COMMENT '业务部门ID',
  `emp_id` varchar(45) DEFAULT NULL COMMENT '工号',
  `buc_id` bigint(20) DEFAULT NULL COMMENT 'BUC编码',
  `aliyun_pk` varchar(45) DEFAULT NULL COMMENT '阿里云PK',
  `login_name` varchar(45) DEFAULT NULL COMMENT '登录名',
  `login_pwd` varchar(45) DEFAULT NULL COMMENT '登录密码',
  `nick_name` varchar(45) DEFAULT NULL COMMENT '昵称',
  `email` varchar(45) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(45) DEFAULT NULL COMMENT '手机号码',
  `dingding` varchar(45) DEFAULT NULL COMMENT '钉钉',
  `aliww` varchar(45) DEFAULT NULL COMMENT '阿里旺旺',
  `memo` varchar(45) DEFAULT NULL COMMENT '备注',
  `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '更新时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `is_first_login` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否首次登陆',
  `is_locked` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否被锁定',
  `access_key_id` varchar(45) DEFAULT NULL COMMENT 'AccessKeyId',
  `access_key_secret` varchar(45) DEFAULT NULL COMMENT 'AccessKeySecret',
  `status` int(11) NOT NULL DEFAULT '0' COMMENT '当前用户状态（0:正常,-1:被删除）',
  `lang` varchar(10) DEFAULT NULL COMMENT '当前用户语言',
  `is_immutable` tinyint(4) NOT NULL DEFAULT '0' COMMENT '当前用户密码是否不可修改',
  `secret_key` varchar(64) DEFAULT NULL COMMENT 'Secret Key，用于以第三方 APP 身份访问',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_id_unique` (`emp_id`),
  UNIQUE KEY `uk_buc_id_unique` (`buc_id`),
  UNIQUE KEY `uk_login_name_unique` (`login_name`),
  UNIQUE KEY `uk_aliyun_pk_unique` (`aliyun_pk`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表'
;

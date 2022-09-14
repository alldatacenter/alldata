/******************************************/
/*   数据库全名 = teslaknowledgegraph   */
/*   表名称 = consumer_node   */
/******************************************/
CREATE TABLE IF NOT EXISTS `consumer_node` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `host` varchar(255) NOT NULL COMMENT '节点ip',
  `enable` varchar(64) NOT NULL DEFAULT 'true' COMMENT '是否是消费节点',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='消费节点'
;

/******************************************/
/*   数据库全名 = teslaknowledgegraph   */
/*   表名称 = consumer_history   */
/******************************************/
CREATE TABLE IF NOT EXISTS `consumer_history` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `consumer_id` bigint(20) unsigned NOT NULL COMMENT '消费配置ID',
  `detail` text NOT NULL COMMENT '消费详情',
  `states` varchar(128) NOT NULL COMMENT '消费状态',
  `name` varchar(1024) DEFAULT NULL COMMENT '消费名',
  PRIMARY KEY (`id`),
  KEY `idx_normal` (`consumer_id`,`states`(100),`name`(100))
) ENGINE=InnoDB AUTO_INCREMENT=2894148 DEFAULT CHARSET=utf8mb4 COMMENT='数据消费历史'
;

/******************************************/
/*   数据库全名 = teslaknowledgegraph   */
/*   表名称 = consumer   */
/******************************************/
CREATE TABLE IF NOT EXISTS `consumer` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `modifier` varchar(1024) NOT NULL COMMENT '修改人',
  `creator` varchar(1024) NOT NULL COMMENT '创建人',
  `import_config` text NOT NULL COMMENT '导入配置',
  `source_info` text NOT NULL COMMENT '信息源',
  `source_type` varchar(256) NOT NULL COMMENT '信息源的类型',
  `client` varchar(256) DEFAULT NULL COMMENT '消费者',
  `offset` varchar(256) DEFAULT NULL COMMENT '消费进度',
  `status` text COMMENT '最新消费信息，主要用于提供错误信息',
  `name` varchar(1024) DEFAULT NULL COMMENT '消费名',
  `enable` varchar(256) DEFAULT NULL COMMENT '是否生效，true/false',
  `app_name` varchar(256) DEFAULT NULL COMMENT '归属方',
  `user_import_config` text COMMENT 'import_config的用户配置，node+relation分割配置',
  `effective_threshold` int unsigned DEFAULT '300' COMMENT '生效时长',
  `notifiers` varchar(1024) DEFAULT '' COMMENT '通知的用户',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`(100)),
  KEY `idx_normal` (`source_type`(100),`client`(100),`enable`(100),`app_name`(100))
) ENGINE=InnoDB AUTO_INCREMENT=1230 DEFAULT CHARSET=utf8mb4 COMMENT='消费数据'
;

/******************************************/
/*   数据库全名 = teslaknowledgegraph   */
/*   表名称 = config   */
/******************************************/
CREATE TABLE IF NOT EXISTS `config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `category` text NOT NULL COMMENT '表现层名',
  `nr_type` text NOT NULL COMMENT '实体类型名',
  `nr_id` text NOT NULL COMMENT '实体id',
  `name` text NOT NULL COMMENT '配置名',
  `modifier` text NOT NULL COMMENT '配置最后修改人',
  `content` text NOT NULL COMMENT '配置的内容',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`category`(100),`nr_type`(100),`nr_id`(100),`name`(100)),
  KEY `idx_name` (`name`(100))
) ENGINE=InnoDB AUTO_INCREMENT=480 DEFAULT CHARSET=utf8mb4 COMMENT='表现层配置表， 每一行是一个配置'
;

/******************************************/
/*   数据库全名 = teslaknowledgegraph   */
/*   表名称 = chatops_history   */
/******************************************/
CREATE TABLE IF NOT EXISTS `chatops_history` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `category` varchar(256) NOT NULL DEFAULT '' COMMENT 'category',
  `sender_nick` varchar(256) NOT NULL DEFAULT '' COMMENT '发送人昵称',
  `sender_id` varchar(256) NOT NULL DEFAULT '' COMMENT '发送人id',
  `sender_empid` varchar(256) NOT NULL DEFAULT '' COMMENT '发送人用户ID',
  `is_conversation` int(11) NOT NULL DEFAULT '0' COMMENT '是否群聊 0为单聊 1为群聊 默认为0',
  `conversation_title` varchar(256) NOT NULL DEFAULT '' COMMENT '群名称',
  `is_content_help` int(11) NOT NULL DEFAULT '0' COMMENT '是否是帮助信息 0为否 1为是 默认为0',
  `send_content` longtext NOT NULL COMMENT '用户发送信息',
  `back_content` longtext NOT NULL COMMENT '回复用户的信息',
  `rate` int(11) NOT NULL DEFAULT '0' COMMENT '用户评分 1-10 默认0分',
  `suggest` longtext NOT NULL COMMENT '用户建议',
  `service_id` bigint DEFAULT NULL COMMENT '服务ID',
  `feedback_id` varchar(256) DEFAULT NULL COMMENT '反馈人钉钉ID',
  `feedback_empid` varchar(256) DEFAULT NULL COMMENT '反馈人用户ID',
  `conversation_id` varchar(256) DEFAULT NULL COMMENT '钉钉群ID',
  PRIMARY KEY (`id`),
  KEY `idx_all` (`category`(128),`sender_nick`(128),`sender_id`(128),`sender_empid`(128),`is_conversation`,`conversation_title`(128),`is_content_help`,`rate`)
) ENGINE=InnoDB AUTO_INCREMENT=276088 DEFAULT CHARSET=utf8mb4 COMMENT='聊天机器人的聊天历史记录'
;

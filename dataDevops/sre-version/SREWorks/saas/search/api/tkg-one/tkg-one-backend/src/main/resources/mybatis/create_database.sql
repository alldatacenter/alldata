CREATE USER IF NOT EXISTS "TESLAKNOWLEDGEGRAPH_APP"@"localhost" IDENTIFIED BY "123456";
CREATE database TESLAKNOWLEDGEGRAPH_APP DEFAULT charset utf8 COLLATE utf8_general_ci;
GRANT ALL PRIVILEGES ON TESLAKNOWLEDGEGRAPH_APP.* TO "TESLAKNOWLEDGEGRAPH_APP"@"localhost" IDENTIFIED BY "123456";

DROP TABLE IF EXISTS `backend_index`;
DROP TABLE IF EXISTS `backend`;

CREATE TABLE IF NOT EXISTS `backend` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `name` varchar(256) DEFAULT NULL COMMENT '后端存储集群名称',
  `type` varchar(256) DEFAULT NULL COMMENT '后端存储集群类型',
  `host` varchar(20) DEFAULT NULL COMMENT '后端存储集群IP地址',
  `port` bigint(10) unsigned DEFAULT NULL COMMENT '后端存储集群端口号',
  `is_default_backend` boolean DEFAULT FALSE COMMENT '是否为兜底集群',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  UNIQUE KEY `uk_host_port` (`host`, `port`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='后端集群信息';

CREATE TABLE IF NOT EXISTS `backend_index` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `index` varchar(256) DEFAULT NULL COMMENT '索引名称',
  `backend_id` bigint(20) unsigned DEFAULT NULL COMMENT '后端集群ID',
  `alias` varchar(256) DEFAULT NULL COMMENT '索引别名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_index` (`index`),
  UNIQUE KEY `uk_index_alias` (`index`, `alias`),
  FOREIGN KEY(backend_id) REFERENCES backend(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='后端集群信息';

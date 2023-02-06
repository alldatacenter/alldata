-- MIGRATION_BASE

CREATE TABLE `cloud_resource_routing`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `created_at`    timestamp NULL DEFAULT NULL,
    `updated_at`    timestamp NULL DEFAULT NULL,
    `resource_id`   varchar(128) DEFAULT NULL COMMENT '云资源id',
    `resource_name` varchar(64)  DEFAULT NULL COMMENT '云资源名称',
    `resource_type` varchar(32)  DEFAULT NULL COMMENT '云资源类型',
    `vendor`        varchar(32)  DEFAULT NULL COMMENT '云服务提供商',
    `org_id`        varchar(64)  DEFAULT NULL COMMENT 'org id',
    `cluster_name`  varchar(64)  DEFAULT NULL COMMENT '集群名',
    `project_id`    varchar(64)  DEFAULT NULL COMMENT '引用云资源的项目id',
    `addon_id`      varchar(64)  DEFAULT NULL COMMENT '引用云资源的addon_id',
    `status`        varchar(16)  DEFAULT NULL COMMENT '引用状态',
    `record_id`     bigint(20) unsigned DEFAULT NULL,
    `detail`        text,
    PRIMARY KEY (`id`),
    KEY             `idx_cloud_resource_routing_resource_id` (`resource_id`),
    KEY             `idx_cloud_resource_routing_project_id` (`project_id`),
    KEY             `idx_cloud_resource_routing_record_id` (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COMMENT='云addon关联的云资源信息';

CREATE TABLE `dice_init_sql_version`
(
    `id`      int(11) unsigned NOT NULL AUTO_INCREMENT,
    `version` varchar(64) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DICE init sql 初始化版本';

CREATE TABLE `edge_apps`
(
    `id`                     bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增Id',
    `org_id`                 bigint(20) NOT NULL COMMENT '企业Id',
    `cluster_id`             bigint(20) NOT NULL COMMENT '关联集群ID',
    `name`                   varchar(50)  NOT NULL COMMENT '应用名',
    `type`                   varchar(50)  NOT NULL COMMENT '发布类型',
    `image`                  varchar(512) NOT NULL COMMENT '镜像',
    `registry_addr`          varchar(512) NOT NULL COMMENT '镜像仓库地址',
    `registry_user`          varchar(100) NOT NULL COMMENT '镜像仓库用户名',
    `registry_password`      varchar(512) NOT NULL COMMENT '镜像仓库密码',
    `product_id`             bigint(20) NOT NULL COMMENT '制品ID',
    `addon_name`             varchar(50)  NOT NULL COMMENT '中间件',
    `addon_version`          varchar(50)  NOT NULL COMMENT '中间件版本',
    `config_set_name`        varchar(50)  NOT NULL COMMENT '配置集',
    `replicas`               bigint(20) NOT NULL COMMENT '副本',
    `health_check_type`      varchar(50)  NOT NULL COMMENT '健康检查类型',
    `health_check_http_port` varchar(50)  NOT NULL COMMENT '健康检查http端口',
    `health_check_http_path` varchar(50)  NOT NULL COMMENT '健康检查http路径',
    `health_check_exec`      varchar(50)  NOT NULL COMMENT '健康检查command',
    `edge_sites`             varchar(2048) DEFAULT NULL COMMENT '发布站点',
    `depend_app`             varchar(2048) DEFAULT NULL COMMENT '依赖应用',
    `port_maps`              varchar(2048) DEFAULT NULL COMMENT '依赖应用',
    `extra_data`             varchar(2048) DEFAULT NULL COMMENT '依赖应用',
    `limit_cpu`              float         DEFAULT NULL COMMENT 'CPU LIMIT',
    `request_cpu`            float         DEFAULT NULL COMMENT 'CPU REQUEST',
    `limit_mem`              float         DEFAULT NULL COMMENT 'MEMORY LIMIT',
    `request_mem`            float         DEFAULT NULL COMMENT 'MEMORY REQUEST',
    `description`            varchar(100)  DEFAULT NULL COMMENT '应用描述',
    `created_at`             datetime     NOT NULL COMMENT '创建时间',
    `updated_at`             datetime      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_org_edgeapp_name` (`org_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='边缘应用';

CREATE TABLE `edge_configsets`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置集ID',
    `org_id`       bigint(20) NOT NULL COMMENT '企业ID',
    `cluster_id`   bigint(20) NOT NULL COMMENT '关联集群ID',
    `name`         varchar(50) NOT NULL COMMENT '配置集名称',
    `display_name` varchar(50) NOT NULL COMMENT '配置集显示名称',
    `description`  varchar(2048) DEFAULT NULL COMMENT '配置集描述',
    `created_at`   datetime    NOT NULL COMMENT '创建时间',
    `updated_at`   datetime      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `edge_configsets_un` (`cluster_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='边缘配置集';

CREATE TABLE `edge_configsets_item`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置项ID',
    `configset_id` bigint(20) NOT NULL COMMENT '配置集ID',
    `scope`        varchar(10)   NOT NULL COMMENT '配置项范围',
    `site_id`      bigint(20) DEFAULT NULL COMMENT '关联站点ID',
    `item_key`     varchar(100)  NOT NULL COMMENT '配置项Key',
    `item_value`   varchar(2048) NOT NULL COMMENT '配置项Value',
    `created_at`   datetime      NOT NULL COMMENT '创建时间',
    `updated_at`   datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `edge_configsets_item_un` (`configset_id`,`scope`,`site_id`,`item_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='边缘配置项';

CREATE TABLE `edge_sites`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT '站点ID',
    `org_id`       bigint(20) NOT NULL COMMENT '企业ID',
    `cluster_id`   bigint(20) NOT NULL COMMENT '关联集群ID',
    `name`         varchar(50) NOT NULL COMMENT '站点名称',
    `display_name` varchar(50) NOT NULL COMMENT '站点显示名称',
    `description`  varchar(2048) DEFAULT NULL COMMENT '站点描述',
    `status`       varchar(50) NOT NULL COMMENT '站点状态',
    `logo`         varchar(500)  DEFAULT NULL COMMENT '站点Logo',
    `created_at`   datetime    NOT NULL COMMENT '创建时间',
    `updated_at`   datetime      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `edge_sites_un` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='边缘站点';

CREATE TABLE `ops_orgak`
(
    `id`          bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `created_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `org_id`      varchar(64)       DEFAULT NULL COMMENT '企业ID',
    `vendor`      varchar(64)       DEFAULT NULL COMMENT '云供应商',
    `access_key`  mediumtext COMMENT '云供应商access_key',
    `secret_key`  mediumtext COMMENT '云供应商secret_key',
    `description` mediumtext COMMENT '云供应商ak,sk描述',
    PRIMARY KEY (`id`),
    KEY           `idx_ops_orgak_org_id` (`org_id`),
    KEY           `idx_ops_orgak_vendor` (`vendor`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='云账号记录表';

CREATE TABLE `ops_record`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `created_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `record_type`  varchar(64)       DEFAULT NULL COMMENT '操作记录类型',
    `user_id`      varchar(64)       DEFAULT NULL COMMENT '操作用户ID',
    `org_id`       varchar(64)       DEFAULT NULL COMMENT '操作用户所属企业ID',
    `cluster_name` varchar(64)       DEFAULT NULL COMMENT '操作相关集群名',
    `status`       varchar(64)       DEFAULT NULL COMMENT '操作结果状态，是否成功',
    `detail`       text COMMENT '操作详情',
    `pipeline_id`  bigint(20) unsigned DEFAULT NULL COMMENT '操作相关流水线ID',
    PRIMARY KEY (`id`),
    KEY            `idx_ops_record_org_id` (`org_id`),
    KEY            `idx_ops_record_cluster_name` (`cluster_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云管操作日志记录表';

CREATE TABLE `tb_addon_management`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `addon_id`     varchar(64)  NOT NULL COMMENT 'addon实例ID',
    `name`         varchar(128) NOT NULL COMMENT 'addon实例名称',
    `project_id`   varchar(45) DEFAULT NULL COMMENT '项目ID',
    `org_id`       varchar(45) DEFAULT NULL COMMENT '组织ID',
    `addon_config` text COMMENT 'addon参数配置',
    `cpu`          double(8, 2
) NOT NULL COMMENT 'cpu核数',
  `mem` int(11) NOT NULL COMMENT '内存大小（M）',
  `nodes` int(4) NOT NULL COMMENT '节点数',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_addon_id` (`addon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云addon信息(ops)';


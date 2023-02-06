-- MIGRATION_BASE

CREATE TABLE `tb_gateway_api`
(
    `id`                 varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一主键',
    `zone_id`            varchar(32)           DEFAULT '' COMMENT '所属的zone',
    `consumer_id`        varchar(32)  NOT NULL DEFAULT '' COMMENT '消费者id',
    `api_path`           varchar(256) NOT NULL DEFAULT '' COMMENT 'api路径',
    `method`             varchar(128) NOT NULL DEFAULT '' COMMENT '方法',
    `redirect_addr`      varchar(256) NOT NULL DEFAULT '' COMMENT '转发地址',
    `description`        varchar(256)          DEFAULT NULL COMMENT '描述',
    `group_id`           varchar(32)  NOT NULL DEFAULT '' COMMENT '服务Id',
    `policies`           varchar(1024)         DEFAULT NULL COMMENT '策略配置',
    `upstream_api_id`    varchar(32)  NOT NULL DEFAULT '' COMMENT '对应的后端api',
    `dice_app`           varchar(128)          DEFAULT '' COMMENT 'dice应用名',
    `dice_service`       varchar(128)          DEFAULT '' COMMENT 'dice服务名',
    `register_type`      varchar(16)  NOT NULL DEFAULT 'auto' COMMENT '注册类型',
    `net_type`           varchar(16)  NOT NULL DEFAULT 'outer' COMMENT '网络类型',
    `need_auth`          tinyint(1) NOT NULL DEFAULT '0' COMMENT '需要鉴权标识',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `redirect_type`      varchar(32)  NOT NULL DEFAULT 'url' COMMENT '转发类型',
    `runtime_service_id` varchar(32)  NOT NULL DEFAULT '' COMMENT '关联的service的id',
    `swagger`            blob COMMENT 'swagger文档',
    PRIMARY KEY (`id`),
    KEY                  `idx_service_id` (`runtime_service_id`,`is_deleted`),
    KEY                  `idx_consumer_id` (`consumer_id`,`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微服务 API';

CREATE TABLE `tb_gateway_api_in_package`
(
    `id`          varchar(32) NOT NULL DEFAULT '' COMMENT '唯一id',
    `dice_api_id` varchar(32) NOT NULL DEFAULT '' COMMENT 'dice服务api的id',
    `package_id`  varchar(32) NOT NULL DEFAULT '' COMMENT '产品包id',
    `zone_id`     varchar(32)          DEFAULT NULL COMMENT '所属的zone',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  varchar(1)  NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='被流量入口引用的微服务 API';

CREATE TABLE `tb_gateway_az_info`
(
    `id`              varchar(32)   NOT NULL DEFAULT '' COMMENT '唯一id',
    `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      varchar(1)    NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `org_id`          varchar(32)   NOT NULL COMMENT '企业标识id',
    `project_id`      varchar(32)   NOT NULL COMMENT '项目标识id',
    `env`             varchar(32)   NOT NULL COMMENT '应用所属环境',
    `az`              varchar(32)   NOT NULL COMMENT '集群名',
    `type`            varchar(16)   NOT NULL DEFAULT '' COMMENT '集群类型',
    `wildcard_domain` varchar(1024) NOT NULL DEFAULT '' COMMENT '集群泛域名',
    `master_addr`     varchar(1024) NOT NULL DEFAULT '' COMMENT '集群管控地址',
    `need_update`     tinyint(1) DEFAULT '1' COMMENT '待更新标识',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 网关集群信息';

CREATE TABLE `tb_gateway_consumer`
(
    `id`              varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `consumer_id`     varchar(128) NOT NULL DEFAULT '' COMMENT '消费者id',
    `consumer_name`   varchar(128) NOT NULL DEFAULT '' COMMENT '消费者名称',
    `config`          varchar(1024)         DEFAULT NULL COMMENT '配置信息，存放key等',
    `endpoint`        varchar(256) NOT NULL DEFAULT '' COMMENT '终端',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `org_id`          varchar(32)  NOT NULL COMMENT '企业id',
    `project_id`      varchar(32)  NOT NULL COMMENT '项目id',
    `env`             varchar(32)  NOT NULL COMMENT '环境',
    `az`              varchar(32)  NOT NULL COMMENT '集群名',
    `auth_config`     blob COMMENT '鉴权配置',
    `description`     varchar(256)          DEFAULT NULL COMMENT '备注',
    `type`            varchar(16)  NOT NULL DEFAULT 'project' COMMENT '调用方类型',
    `cloudapi_app_id` varchar(128) NOT NULL DEFAULT '' COMMENT '阿里云APP id',
    `client_id`       varchar(32)  NOT NULL DEFAULT '' COMMENT '对应的客户端id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `consumer_id` (`consumer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 网关调用方';

CREATE TABLE `tb_gateway_consumer_api`
(
    `id`          varchar(32) NOT NULL DEFAULT '' COMMENT '唯一id',
    `consumer_id` varchar(32) NOT NULL DEFAULT '' COMMENT '消费者id',
    `api_id`      varchar(32) NOT NULL DEFAULT '' COMMENT 'apiId',
    `policies`    varchar(512)         DEFAULT NULL COMMENT '策略信息',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  varchar(1)  NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 的调用方授权信息(已废弃)';

CREATE TABLE `tb_gateway_default_policy`
(
    `id`          varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `name`        varchar(32)  NOT NULL DEFAULT '' COMMENT '名称',
    `level`       varchar(32)  NOT NULL COMMENT '策略级别',
    `tenant_id`   varchar(128) NOT NULL DEFAULT '' COMMENT '租户id',
    `dice_app`    varchar(128)          DEFAULT '' COMMENT 'dice应用名',
    `config`      blob COMMENT '具体配置',
    `package_id`  varchar(32)  NOT NULL DEFAULT '' COMMENT '流量入口id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 默认策略';

CREATE TABLE `tb_gateway_domain`
(
    `id`                 varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一主键',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `domain`             varchar(255) NOT NULL COMMENT '域名',
    `cluster_name`       varchar(32)  NOT NULL DEFAULT '' COMMENT '所属集群',
    `type`               varchar(32)  NOT NULL COMMENT '域名类型',
    `runtime_service_id` varchar(32)           DEFAULT NULL COMMENT '所属服务id',
    `package_id`         varchar(32)           DEFAULT NULL COMMENT '所属流量入口id',
    `component_name`     varchar(32)           DEFAULT NULL COMMENT '所属平台组件的名称',
    `ingress_name`       varchar(128)          DEFAULT NULL COMMENT '所属平台组件的ingress的名称',
    `project_id`         varchar(32)  NOT NULL DEFAULT '' COMMENT '项目标识id',
    `project_name`       varchar(50)  NOT NULL DEFAULT '' COMMENT '项目名称',
    `workspace`          varchar(32)  NOT NULL DEFAULT '' COMMENT '所属环境',
    PRIMARY KEY (`id`),
    KEY                  `idx_runtime_service` (`runtime_service_id`,`is_deleted`),
    KEY                  `idx_package` (`package_id`,`is_deleted`),
    KEY                  `idx_cluster_domain` (`cluster_name`,`domain`,`is_deleted`),
    KEY                  `idx_cluster` (`is_deleted`,`cluster_name`,`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT DYNAMIC COMMENT='域名管理';

CREATE TABLE `tb_gateway_ingress_policy`
(
    `id`               varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `name`             varchar(32)  NOT NULL DEFAULT '' COMMENT '名称',
    `regions`          varchar(128) NOT NULL DEFAULT '' COMMENT '作用域',
    `az`               varchar(32)  NOT NULL COMMENT '集群名',
    `zone_id`          varchar(32)           DEFAULT NULL COMMENT '所属的zone',
    `config`           blob COMMENT '具体配置',
    `configmap_option` blob COMMENT 'ingress configmap option',
    `main_snippet`     blob COMMENT 'ingress configmap main 配置',
    `http_snippet`     blob COMMENT 'ingress configmap http 配置',
    `server_snippet`   blob COMMENT 'ingress configmap server 配置',
    `annotations`      blob COMMENT '包含的annotations',
    `location_snippet` blob COMMENT 'nginx location 配置',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Ingress 策略管理';

CREATE TABLE `tb_gateway_kong_info`
(
    `id`                varchar(32)   NOT NULL DEFAULT '' COMMENT '唯一id',
    `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        varchar(1)    NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `az`                varchar(32)   NOT NULL COMMENT '集群名',
    `project_id`        varchar(32)   NOT NULL COMMENT '项目id',
    `project_name`      varchar(256)  NOT NULL COMMENT '项目名',
    `env`               varchar(32)            DEFAULT '' COMMENT '环境名',
    `kong_addr`         varchar(256)  NOT NULL COMMENT 'kong admin地址',
    `endpoint`          varchar(256)  NOT NULL COMMENT 'kong gateway地址',
    `inner_addr`        varchar(1024) NOT NULL DEFAULT '' COMMENT 'kong内网地址',
    `service_name`      varchar(32)   NOT NULL DEFAULT '' COMMENT 'kong的服务名称',
    `addon_instance_id` varchar(64)   NOT NULL DEFAULT '' COMMENT 'addon id',
    `need_update`       tinyint(1) DEFAULT '1' COMMENT '待更新标识',
    `tenant_id`         varchar(128)  NOT NULL DEFAULT '' COMMENT '租户id',
    `tenant_group`      varchar(128)  NOT NULL DEFAULT '' COMMENT '租户分组',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Kong 实例信息';

CREATE TABLE `tb_gateway_org_client`
(
    `id`            varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `org_id`        varchar(32)  NOT NULL COMMENT '企业id',
    `name`          varchar(128) NOT NULL DEFAULT '' COMMENT '消费者名称',
    `client_secret` varchar(32)  NOT NULL COMMENT '客户端凭证',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业级 API 网关客户端';

CREATE TABLE `tb_gateway_package`
(
    `id`                   varchar(32)   NOT NULL DEFAULT '' COMMENT '唯一主键',
    `dice_org_id`          varchar(32)   NOT NULL DEFAULT '' COMMENT 'dice企业标识id',
    `dice_project_id`      varchar(32)            DEFAULT '' COMMENT 'dice项目标识id',
    `dice_env`             varchar(32)   NOT NULL COMMENT 'dice环境',
    `dice_cluster_name`    varchar(32)   NOT NULL COMMENT 'dice集群名',
    `zone_id`              varchar(32)            DEFAULT NULL COMMENT '所属的zone',
    `scene`                varchar(32)   NOT NULL DEFAULT 'openapi' COMMENT '场景',
    `package_name`         varchar(1024) NOT NULL COMMENT '产品包名称',
    `bind_domain`          varchar(1024)          DEFAULT NULL COMMENT '绑定的域名',
    `description`          varchar(256)           DEFAULT NULL COMMENT '描述',
    `acl_type`             varchar(16)   NOT NULL DEFAULT 'off' COMMENT '授权方式',
    `auth_type`            varchar(16)   NOT NULL DEFAULT '' COMMENT '鉴权方式',
    `create_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`           varchar(1)    NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `runtime_service_id`   varchar(32)   NOT NULL DEFAULT '' COMMENT '关联的service的id',
    `cloudapi_instance_id` varchar(128)  NOT NULL DEFAULT '' COMMENT '阿里云API网关的实例id',
    `cloudapi_group_id`    varchar(128)  NOT NULL DEFAULT '' COMMENT '阿里云API网关的分组id',
    `cloudapi_domain`      varchar(1024) NOT NULL DEFAULT '' COMMENT '阿里云API网关上的分组二级域名',
    `cloudapi_vpc_grant`   varchar(128)  NOT NULL DEFAULT '' COMMENT '阿里云API网关的VPC Grant',
    `cloudapi_need_bind`   tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要绑定阿里云API网关',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流量入口';

CREATE TABLE `tb_gateway_package_api`
(
    `id`                 varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一主键',
    `package_id`         varchar(32)           DEFAULT '' COMMENT '所属的产品包id',
    `api_path`           varchar(256) NOT NULL DEFAULT '' COMMENT 'api路径',
    `method`             varchar(128) NOT NULL DEFAULT '' COMMENT '方法',
    `redirect_addr`      varchar(256) NOT NULL DEFAULT '' COMMENT '转发地址',
    `description`        varchar(256)          DEFAULT NULL COMMENT '描述',
    `dice_app`           varchar(128)          DEFAULT '' COMMENT 'dice应用名',
    `dice_service`       varchar(128)          DEFAULT '' COMMENT 'dice服务名',
    `acl_type`           varchar(16)           DEFAULT NULL COMMENT '独立的授权类型',
    `origin`             varchar(16)  NOT NULL DEFAULT 'custom' COMMENT '来源',
    `dice_api_id`        varchar(32)           DEFAULT NULL COMMENT '对应dice服务api的id',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `zone_id`            varchar(32)  NOT NULL DEFAULT '' COMMENT '所属的zone',
    `redirect_type`      varchar(32)  NOT NULL DEFAULT 'url' COMMENT '转发类型',
    `runtime_service_id` varchar(32)  NOT NULL DEFAULT '' COMMENT '关联的service的id',
    `redirect_path`      varchar(256) NOT NULL DEFAULT '' COMMENT '转发路径',
    `cloudapi_api_id`    varchar(128) NOT NULL DEFAULT '' COMMENT '阿里云API网关上的api id',
    PRIMARY KEY (`id`),
    KEY                  `idx_package_id` (`package_id`,`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流量入口下的路由规则';

CREATE TABLE `tb_gateway_package_api_in_consumer`
(
    `id`             varchar(32) NOT NULL DEFAULT '' COMMENT '唯一id',
    `consumer_id`    varchar(32) NOT NULL DEFAULT '' COMMENT '消费者id',
    `package_id`     varchar(32) NOT NULL DEFAULT '' COMMENT '产品包id',
    `package_api_id` varchar(32) NOT NULL DEFAULT '' COMMENT '产品包 api id',
    `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     varchar(1)  NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流量入口路由的授权信息';

CREATE TABLE `tb_gateway_package_in_consumer`
(
    `id`          varchar(32) NOT NULL DEFAULT '' COMMENT '唯一id',
    `consumer_id` varchar(32) NOT NULL DEFAULT '' COMMENT '消费者id',
    `package_id`  varchar(32) NOT NULL DEFAULT '' COMMENT '产品包id',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  varchar(1)  NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流量入口的授权信息';

CREATE TABLE `tb_gateway_package_rule`
(
    `id`                varchar(32)   NOT NULL DEFAULT '' COMMENT '唯一id',
    `dice_org_id`       varchar(32)   NOT NULL DEFAULT '' COMMENT 'dice企业标识id',
    `dice_project_id`   varchar(32)            DEFAULT '' COMMENT 'dice项目标识id',
    `dice_env`          varchar(32)   NOT NULL COMMENT 'dice环境',
    `dice_cluster_name` varchar(32)   NOT NULL COMMENT 'dice集群名',
    `category`          varchar(32)   NOT NULL DEFAULT '' COMMENT '插件类目',
    `enabled`           tinyint(1) DEFAULT '1' COMMENT '插件开关',
    `plugin_id`         varchar(128)  NOT NULL DEFAULT '' COMMENT '插件id',
    `plugin_name`       varchar(128)  NOT NULL DEFAULT '' COMMENT '插件名称',
    `config`            blob COMMENT '插件具体配置',
    `consumer_id`       varchar(32)   NOT NULL DEFAULT '' COMMENT '消费者id',
    `consumer_name`     varchar(128)  NOT NULL DEFAULT '' COMMENT '消费者名称',
    `package_id`        varchar(32)   NOT NULL DEFAULT '' COMMENT '产品包id',
    `package_name`      varchar(1024) NOT NULL COMMENT '产品包名称',
    `api_id`            varchar(32)            DEFAULT NULL COMMENT '产品包api id',
    `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        varchar(1)    NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `package_zone_need` tinyint(1) DEFAULT '1' COMMENT '是否在package的zone内生效',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流量入口的策略信息';

CREATE TABLE `tb_gateway_plugin_instance`
(
    `id`          varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `plugin_id`   varchar(128) NOT NULL DEFAULT '' COMMENT '插件id',
    `plugin_name` varchar(128) NOT NULL DEFAULT '' COMMENT '插件名称',
    `policy_id`   varchar(32)  NOT NULL DEFAULT '' COMMENT '策略id',
    `consumer_id` varchar(32)           DEFAULT NULL COMMENT '消费者id',
    `group_id`    varchar(32)           DEFAULT NULL COMMENT '组id',
    `route_id`    varchar(32)           DEFAULT NULL COMMENT '路由id',
    `service_id`  varchar(32)           DEFAULT NULL COMMENT '服务id',
    `api_id`      varchar(32)           DEFAULT '' COMMENT 'apiID',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY           `plugin_id` (`plugin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Kong Plugin 实例信息';

CREATE TABLE `tb_gateway_policy`
(
    `id`           varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `zone_id`      varchar(32)  NOT NULL DEFAULT '' COMMENT '所属的zone',
    `policy_name`  varchar(128)          DEFAULT '' COMMENT '策略名称',
    `display_name` varchar(128) NOT NULL DEFAULT '' COMMENT '策略展示名称',
    `category`     varchar(128) NOT NULL DEFAULT '' COMMENT '策略类目',
    `description`  varchar(128) NOT NULL DEFAULT '' COMMENT '描述类目',
    `plugin_id`    varchar(128)          DEFAULT '' COMMENT '插件id',
    `plugin_name`  varchar(128) NOT NULL DEFAULT '' COMMENT '插件名称',
    `config`       blob COMMENT 'plugin具体配置',
    `consumer_id`  varchar(32)           DEFAULT NULL COMMENT '消费者id',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `enabled`      tinyint(1) DEFAULT '1' COMMENT '插件开关',
    `api_id`       varchar(32)           DEFAULT '' COMMENT 'api id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Kong Plugin 元信息';


INSERT INTO `tb_gateway_policy` (`id`, `zone_id`, `policy_name`, `display_name`, `category`, `description`, `plugin_id`,
                                 `plugin_name`, `config`, `consumer_id`, `create_time`, `update_time`, `is_deleted`,
                                 `enabled`, `api_id`)
VALUES ('3', '', 'key-auth', 'key鉴权', 'auth', '鉴权策略', '', 'key-auth',
        _binary '{\"CARRIER\":\"SERVICE\",\"key_names\":\"Access-Token,appKey,x-app-key\"}', '', '2018-08-30 13:33:31',
        '2018-08-31 11:43:46', 'N', 1, ''),
       ('4', '', '', '调用鉴权', 'authentication', '认证鉴权', '', 'basic-auth', _binary '{\"CARRIER\":\"SERVICE\"}', '',
        '2018-08-30 18:02:25', '2018-09-03 16:23:52', 'N', 1, ''),
       ('5', '', 'acl', 'acl', 'basic', 'acl', '', 'acl',
        _binary '{\"CARRIER\":\"SERVICE\",\"hide_groups_header\":true}', '', '2018-12-14 18:22:18',
        '2018-12-14 18:22:42', 'N', 1, ''),
       ('6', '', 'authorization code', 'oauth2授权码模式', 'auth', '鉴权策略', '', 'oauth2',
        _binary '{\"CARRIER\":\"SERVICE\",\"token_expiration\":3600,\"enable_authorization_code\":true,\"accept_http_if_already_terminated\":true,\"refresh_token_ttl\":86400}',
        '', '2019-05-16 15:07:00', '2019-05-16 15:07:00', 'N', 1, ''),
       ('7', '', 'client credentials', 'oauth2客户端模式', 'auth', '鉴权策略', '', 'oauth2',
        _binary '{\"CARRIER\":\"SERVICE\",\"token_expiration\":3600,\"enable_client_credentials\":true,\"accept_http_if_already_terminated\":true,\"global_credentials\":true}',
        '', '2019-05-16 15:07:00', '2019-05-16 15:07:00', 'N', 1, ''),
       ('8', '', 'sign-auth', 'sign鉴权', 'auth', '鉴权策略', '', 'sign-auth',
        _binary '{\"CARRIER\":\"SERVICE\",\"key_names\":\"appKey,x-app-key\",\"sign_names\":\"sign,x-sign\"}', '',
        '2018-08-30 13:33:31', '2018-08-31 11:43:46', 'N', 1, '');

CREATE TABLE `tb_gateway_publish`
(
    `id`                     varchar(32)   NOT NULL DEFAULT '' COMMENT '唯一主键',
    `create_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`             varchar(1)    NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `dice_publish_id`        varchar(32)   NOT NULL DEFAULT '' COMMENT 'dice市场租户id',
    `dice_publish_item_id`   varchar(32)   NOT NULL DEFAULT '' COMMENT 'dice市场商品id',
    `dice_publish_item_name` varchar(128)  NOT NULL DEFAULT '' COMMENT 'dice市场商品名称',
    `version`                varchar(32)   NOT NULL DEFAULT '' COMMENT '版本',
    `published`              tinyint(1) DEFAULT '0' COMMENT '发布状态',
    `owner_email`            varchar(1024) NOT NULL DEFAULT '' COMMENT '负责人邮箱地址',
    `api_register_id`        varchar(32)   NOT NULL DEFAULT '' COMMENT 'api register',
    `package_id`             varchar(32)            DEFAULT '' COMMENT '生成的流量入口id',
    PRIMARY KEY (`id`),
    KEY                      `idx_publish` (`dice_publish_id`,`dice_publish_item_id`,`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 发布管理(已废弃)';

CREATE TABLE `tb_gateway_register`
(
    `id`                 varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一主键',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `org_id`             varchar(32)  NOT NULL DEFAULT '' COMMENT '所属企业',
    `project_id`         varchar(32)  NOT NULL DEFAULT '' COMMENT '所属项目',
    `workspace`          varchar(32)  NOT NULL DEFAULT '' COMMENT '所属环境',
    `app_id`             varchar(32)  NOT NULL DEFAULT '' COMMENT '所属应用',
    `app_name`           varchar(128) NOT NULL DEFAULT '' COMMENT '应用名称',
    `service_name`       varchar(128) NOT NULL DEFAULT '' COMMENT '服务名称',
    `cluster_name`       varchar(32)  NOT NULL DEFAULT '' COMMENT '所属集群',
    `origin`             varchar(16)  NOT NULL DEFAULT 'action' COMMENT '注册来源',
    `runtime_name`       varchar(128) NOT NULL DEFAULT '' COMMENT 'runtime名称/分支名称',
    `swagger`            longblob COMMENT 'swagger文档',
    `md5_sum`            varchar(128) NOT NULL DEFAULT '' COMMENT 'swagger摘要',
    `runtime_service_id` varchar(32)  NOT NULL DEFAULT '' COMMENT 'runtime service',
    `registered`         tinyint(1) DEFAULT '0' COMMENT '注册状态',
    `last_error`         blob COMMENT '注册失败信息',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 注册管理(已废弃)';

CREATE TABLE `tb_gateway_route`
(
    `id`          varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `route_id`    varchar(128) NOT NULL DEFAULT '' COMMENT '路由id',
    `protocols`   varchar(128)          DEFAULT NULL COMMENT '协议列表',
    `methods`     varchar(128)          DEFAULT NULL COMMENT '方法列表',
    `hosts`       varchar(1024)         DEFAULT NULL COMMENT '主机列表',
    `paths`       varchar(1024)         DEFAULT NULL COMMENT '路径列表',
    `service_id`  varchar(128) NOT NULL DEFAULT '' COMMENT '绑定服务id',
    `config`      varchar(1024)         DEFAULT '' COMMENT '选填配置',
    `api_id`      varchar(32)  NOT NULL DEFAULT '' COMMENT 'apiid',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY           `route_id` (`route_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Kong Route 配置信息';

CREATE TABLE `tb_gateway_runtime_service`
(
    `id`                varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一主键',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `project_id`        varchar(32)  NOT NULL DEFAULT '' COMMENT '所属项目',
    `workspace`         varchar(32)  NOT NULL DEFAULT '' COMMENT '所属环境',
    `cluster_name`      varchar(32)  NOT NULL DEFAULT '' COMMENT '所属集群',
    `runtime_id`        varchar(32)  NOT NULL DEFAULT '' COMMENT '所属runtime',
    `runtime_name`      varchar(128) NOT NULL DEFAULT '' COMMENT 'runtime名称',
    `app_id`            varchar(32)  NOT NULL DEFAULT '' COMMENT '所属应用',
    `app_name`          varchar(128) NOT NULL DEFAULT '' COMMENT '应用名称',
    `service_name`      varchar(128) NOT NULL DEFAULT '' COMMENT '服务名称',
    `inner_address`     varchar(1024)         DEFAULT NULL COMMENT '服务内部地址',
    `use_apigw`         tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否使用api网关',
    `is_endpoint`       tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否是endpoint',
    `release_id`        varchar(128) NOT NULL DEFAULT '' COMMENT '对应的releaseId',
    `group_namespace`   varchar(128) NOT NULL DEFAULT '' COMMENT 'serviceGroup的namespace',
    `group_name`        varchar(128) NOT NULL DEFAULT '' COMMENT 'serviceGroup的name',
    `service_port`      int(11) NOT NULL DEFAULT '0' COMMENT '服务监听端口',
    `is_security`       tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要安全加密',
    `backend_protocol`  varchar(16)  NOT NULL DEFAULT '' COMMENT '后端协议',
    `project_namespace` varchar(128) NOT NULL DEFAULT '' COMMENT '项目级 namespace',
    PRIMARY KEY (`id`),
    KEY                 `idx_config_tenant` (`project_id`,`workspace`,`cluster_name`,`is_deleted`),
    KEY                 `idx_runtime_id` (`runtime_id`,`is_deleted`),
    KEY                 `idx_runtime_name` (`project_id`,`workspace`,`cluster_name`,`app_id`,`runtime_name`,`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dice 部署服务实例信息';

CREATE TABLE `tb_gateway_service`
(
    `id`           varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `service_id`   varchar(128) NOT NULL DEFAULT '' COMMENT '服务id',
    `service_name` varchar(64)           DEFAULT NULL COMMENT '服务名称',
    `url`          varchar(1024)         DEFAULT NULL COMMENT '具体路径',
    `protocol`     varchar(32)           DEFAULT NULL COMMENT '协议',
    `host`         varchar(1024)         DEFAULT NULL COMMENT '主机',
    `port`         varchar(32)           DEFAULT NULL COMMENT '端口',
    `path`         varchar(1024)         DEFAULT NULL COMMENT '路径',
    `config`       varchar(1024)         DEFAULT NULL COMMENT '选填配置',
    `api_id`       varchar(32)  NOT NULL DEFAULT '' COMMENT 'apiid',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY            `service_id` (`service_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Kong Service 配置信息';

CREATE TABLE `tb_gateway_subscribe`
(
    `id`                   varchar(32)   NOT NULL DEFAULT '' COMMENT '唯一主键',
    `create_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`           varchar(1)    NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `confirmed`            tinyint(1) DEFAULT '0' COMMENT '订阅状态',
    `subscriber_email`     varchar(1024) NOT NULL DEFAULT '' COMMENT '订阅者邮箱地址',
    `dice_publish_id`      varchar(32)   NOT NULL DEFAULT '' COMMENT 'dice市场租户id',
    `dice_publish_item_id` varchar(32)   NOT NULL DEFAULT '' COMMENT 'dice市场商品id',
    `publish_id`           varchar(32)            DEFAULT '' COMMENT '发布id',
    `consumer_id`          varchar(32)   NOT NULL DEFAULT '' COMMENT '消费者id',
    `description`          varchar(256)           DEFAULT NULL COMMENT '描述',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 订阅管理(已废弃)';

CREATE TABLE `tb_gateway_upstream`
(
    `id`                 varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `zone_id`            varchar(32)  NOT NULL DEFAULT '' COMMENT '所属的zone',
    `org_id`             varchar(32)  NOT NULL COMMENT '企业标识id',
    `project_id`         varchar(32)  NOT NULL COMMENT '项目标识id',
    `upstream_name`      varchar(128) NOT NULL COMMENT '后端名称',
    `dice_app`           varchar(128)          DEFAULT '' COMMENT 'dice应用名',
    `dice_service`       varchar(128)          DEFAULT '' COMMENT 'dice服务名',
    `env`                varchar(32)  NOT NULL COMMENT '应用所属环境',
    `az`                 varchar(32)  NOT NULL COMMENT '集群名',
    `last_register_id`   varchar(64)  NOT NULL COMMENT '应用最近一次注册id',
    `valid_register_id`  varchar(64)  NOT NULL COMMENT '应用当前生效的注册id',
    `auto_bind`          tinyint(1) NOT NULL DEFAULT '1' COMMENT 'api是否自动绑定',
    `runtime_service_id` varchar(32)  NOT NULL DEFAULT '' COMMENT '关联的service的id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 网关注册 SDK 的服务元信息';

CREATE TABLE `tb_gateway_upstream_api`
(
    `id`           varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `upstream_id`  varchar(32)  NOT NULL COMMENT '应用标识id',
    `register_id`  varchar(64)  NOT NULL COMMENT '应用注册id',
    `api_name`     varchar(256) NOT NULL COMMENT '标识api的名称，应用下唯一',
    `path`         varchar(256) NOT NULL COMMENT '注册的api路径',
    `gateway_path` varchar(256) NOT NULL COMMENT 'gateway的api路径',
    `method`       varchar(256) NOT NULL COMMENT '注册的api方法',
    `address`      varchar(256) NOT NULL COMMENT '注册的转发地址',
    `doc`          blob COMMENT 'api描述',
    `api_id`       varchar(32)           DEFAULT '' COMMENT 'api标识id',
    `is_inner`     tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否是内部api',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 网关注册 SDK 的服务 API 信息';

CREATE TABLE `tb_gateway_upstream_lb`
(
    `id`                 varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `zone_id`            varchar(32)  NOT NULL DEFAULT '' COMMENT '所属的zone',
    `org_id`             varchar(32)  NOT NULL COMMENT '企业标识id',
    `project_id`         varchar(32)  NOT NULL COMMENT '项目标识id',
    `lb_name`            varchar(128) NOT NULL COMMENT '负载均衡名称',
    `env`                varchar(32)  NOT NULL COMMENT '应用所属环境',
    `az`                 varchar(32)  NOT NULL COMMENT '集群名',
    `kong_upstream_id`   varchar(128)          DEFAULT '' COMMENT 'kong的upstream_id',
    `config`             blob COMMENT '负载均衡配置',
    `healthcheck_path`   varchar(128) NOT NULL DEFAULT '' COMMENT 'HTTP健康检查路径',
    `last_deployment_id` int(11) NOT NULL COMMENT '最近一次target上线请求的部署id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 网关注册 SDK 的服务负载均衡信息';

CREATE TABLE `tb_gateway_upstream_lb_target`
(
    `id`             varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一id',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `lb_id`          varchar(32)  NOT NULL DEFAULT '' COMMENT '关联的lb id',
    `target`         varchar(64)  NOT NULL COMMENT '目的地址',
    `weight`         int(11) NOT NULL DEFAULT '100' COMMENT '轮询权重',
    `healthy`        tinyint(1) DEFAULT '1' COMMENT '是否健康',
    `kong_target_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'kong的target_id',
    `deployment_id`  int(11) NOT NULL COMMENT '上线时的deployment_id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 网关注册 SDK 的服务节点信息';

CREATE TABLE `tb_gateway_upstream_register_record`
(
    `id`            varchar(32) NOT NULL DEFAULT '' COMMENT '唯一id',
    `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    varchar(1)  NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `upstream_id`   varchar(32) NOT NULL COMMENT '应用标识id',
    `register_id`   varchar(64) NOT NULL COMMENT '应用注册id',
    `upstream_apis` blob COMMENT 'api注册列表',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 网关注册 SDK 的注册记录';

CREATE TABLE `tb_gateway_zone`
(
    `id`                varchar(32)   NOT NULL DEFAULT '' COMMENT '唯一id',
    `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        varchar(1)    NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `name`              varchar(1024) NOT NULL DEFAULT '' COMMENT '名称',
    `type`              varchar(16)   NOT NULL DEFAULT '' COMMENT '类型',
    `kong_policies`     blob COMMENT '包含的kong策略id',
    `ingress_policies`  blob COMMENT '包含的ingress策略id',
    `bind_domain`       varchar(1024)          DEFAULT NULL COMMENT '绑定的域名',
    `tenant_id`         varchar(128)  NOT NULL DEFAULT '' COMMENT '租户id',
    `dice_org_id`       varchar(32)   NOT NULL DEFAULT '' COMMENT 'dice企业标识id',
    `dice_project_id`   varchar(32)            DEFAULT '' COMMENT 'dice项目标识id',
    `dice_env`          varchar(32)   NOT NULL COMMENT 'dice应用所属环境',
    `dice_cluster_name` varchar(32)   NOT NULL COMMENT 'dice集群名',
    `dice_app`          varchar(128)           DEFAULT '' COMMENT 'dice应用名',
    `dice_service`      varchar(128)           DEFAULT '' COMMENT 'dice服务名',
    `package_api_id`    varchar(32)   NOT NULL DEFAULT '' COMMENT '流量入口中指定api的id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 网关控制路由和策略的作用对象';

CREATE TABLE `tb_gateway_zone_in_package`
(
    `id`              varchar(32)  NOT NULL DEFAULT '' COMMENT '唯一主键',
    `package_id`      varchar(32)           DEFAULT '' COMMENT '所属的产品包id',
    `package_zone_id` varchar(32)           DEFAULT '' COMMENT '产品包的zone id',
    `zone_id`         varchar(32)           DEFAULT '' COMMENT '依赖的zone id',
    `route_prefix`    varchar(128) NOT NULL COMMENT '路由前缀',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流量入口下的路由作用对象(已废弃)';


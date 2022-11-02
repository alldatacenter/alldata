CREATE TABLE `action`
(
    `id`                      bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uuid`                    varchar(255)        NOT NULL COMMENT 'action uuid',
    `app_code`                varchar(100)        NOT NULL COMMENT '所属产品线',
    `action_id`               bigint(20)          NOT NULL COMMENT '操作唯一ID',
    `element_id`              varchar(255)        NOT NULL COMMENT '操作elementId',
    `action_type`             varchar(50)         NOT NULL COMMENT '操作类型',
    `action_name`             varchar(255)        NOT NULL COMMENT '操作名称',
    `action_label`            varchar(255)        NOT NULL COMMENT '操作label',
    `action_meta_data`        longtext            NOT NULL COMMENT '操作实例meta信息',
    `node`                    varchar(255)                 DEFAULT '' COMMENT '执行操作上层的node信息',
    `entity_type`             varchar(255)                 DEFAULT '' COMMENT '对象实体类型',
    `entity_value`            varchar(255)                 DEFAULT '' COMMENT '对象实体值',
    `emp_id`                  varchar(255)        NOT NULL DEFAULT '' COMMENT '执行人工号',
    `processor`               text                NOT NULL COMMENT '执行人信息',
    `status`                  varchar(50)         NOT NULL COMMENT '执行状态',
    `exec_data`               longtext COMMENT '执行结果',
    `create_time`             bigint(20)          NOT NULL COMMENT '创建时间',
    `start_time`              bigint(20)                   DEFAULT '0' COMMENT '开始时间',
    `end_time`                bigint(20)                   DEFAULT NULL COMMENT '结束时间',
    `order_id`                varchar(50)                  DEFAULT NULL COMMENT 'changfree orderId',
    `sync_result`             tinyint(4)                   DEFAULT '0' COMMENT '是否同步结束',
    `tesla_order_id`          bigint(20) unsigned          DEFAULT NULL COMMENT 'tesla工单ID',
    `tesla_order_sync_result` tinyint(4)                   DEFAULT NULL COMMENT 'tesla工单是否同步结束',
    `sync_tesla_order`        tinyint(4)                   DEFAULT '1' COMMENT '是否同步到tesla工单系统',
    `bpms_data`               text COMMENT '创建老工单数据',
    `bpms_result`             text COMMENT 'bpms审批结果',
    `order_type`              varchar(50)                  DEFAULT NULL COMMENT '同步工单类型',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='操作审计';

CREATE TABLE IF NOT EXISTS `productops_app` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config` longtext COLLATE utf8mb4_general_ci,
  `environments` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `gmt_create` bigint DEFAULT NULL,
  `gmt_modified` bigint DEFAULT NULL,
  `last_modifier` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `template_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stage_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKec3i54ctu08wtyq3kw041flu9` (`stage_id`,`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=218 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `productops_element` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config` longtext COLLATE utf8mb4_general_ci,
  `element_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `gmt_create` bigint DEFAULT NULL,
  `gmt_modified` bigint DEFAULT NULL,
  `last_modifier` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stage_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpyamu37fju8qn696gi8u9e55r` (`stage_id`,`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2869 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `productops_node` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config` longtext COLLATE utf8mb4_general_ci,
  `gmt_create` bigint DEFAULT NULL,
  `gmt_modified` bigint DEFAULT NULL,
  `last_modifier` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `node_type_path` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `parent_node_type_path` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `service_type` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stage_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk` (`node_type_path`(128),`stage_id`(128))
) ENGINE=InnoDB AUTO_INCREMENT=2025 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `productops_node_element` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config` longtext COLLATE utf8mb4_general_ci,
  `element_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `gmt_create` bigint DEFAULT NULL,
  `gmt_modified` bigint DEFAULT NULL,
  `last_modifier` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `node_name` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `node_order` bigint DEFAULT NULL,
  `node_type_path` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tags` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stage_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_1ym6qxm6xum47l6nk8ah848e4` (`element_id`,`stage_id`) USING BTREE,
  UNIQUE KEY `UKl1xdqqt0ent4ff048ko9txc4f` (`stage_id`,`element_id`)
) ENGINE=InnoDB AUTO_INCREMENT=940 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


CREATE TABLE IF NOT EXISTS `productops_tab` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config` longtext COLLATE utf8mb4_general_ci,
  `elements` longtext COLLATE utf8mb4_general_ci,
  `gmt_create` bigint DEFAULT NULL,
  `gmt_modified` bigint DEFAULT NULL,
  `label` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `last_modifier` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `node_type_path` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tab_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stage_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8tjppual7clhwrc66djf7038p` (`stage_id`,`tab_id`),
  UNIQUE KEY `UK_7tm0b6wfwfrvqwsxodokfxdac` (`tab_id`(128),`stage_id`(128),`node_type_path`(128)) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2126 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


CREATE TABLE IF NOT EXISTS `productops_component` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config` longtext COLLATE utf8mb4_general_ci,
  `interfaces` longtext COLLATE utf8mb4_general_ci,
  `gmt_create` bigint DEFAULT NULL,
  `gmt_modified` bigint DEFAULT NULL,
  `last_modifier` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `alias` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `component_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stage_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8tjppual7clhwrc66djf7038p` (`stage_id`,`component_id`),
  UNIQUE KEY `UK_7tm0b6wfwfrvqwsxodokfxdac` (`component_id`(128),`stage_id`(128)) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
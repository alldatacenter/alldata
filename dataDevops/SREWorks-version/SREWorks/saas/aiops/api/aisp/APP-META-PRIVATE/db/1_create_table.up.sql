/******************************************/
/*   DatabaseName = tdata_aisp   */
/*   TableName = aisp_analyse_instance   */
/******************************************/
CREATE TABLE `aisp_analyse_instance`
(
    `instance_code` varchar(128) NOT NULL COMMENT '检测实例id',
    `gmt_create`    datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`  datetime DEFAULT NULL COMMENT '最后修改时间',
    `scene_code`    varchar(128) NOT NULL COMMENT '场景code',
    `detector_code` varchar(128) NOT NULL COMMENT '检测器code',
    `entity_id`     varchar(128) NOT NULL COMMENT '实体id',
    `model_param`   json     DEFAULT NULL COMMENT '可扩展应用参数',
    PRIMARY KEY (`instance_code`),
    KEY             `idx_scene_code` (`scene_code`),
    KEY             `idx_detector_code` (`detector_code`),
    KEY             `idx_entity_id` (`entity_id`),
    KEY             `midx_detector_entity` (`detector_code`,`entity_id`),
    KEY             `idx_scene_detector_entity` (`scene_code`,`detector_code`,`entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
;

/******************************************/
/*   DatabaseName = tdata_aisp   */
/*   TableName = aisp_analyse_task   */
/******************************************/
CREATE TABLE `aisp_analyse_task`
(
    `task_uuid`     varchar(128) NOT NULL COMMENT '任务id',
    `gmt_create`    datetime(3)     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`  datetime     DEFAULT NULL COMMENT '最后修改时间',
    `cost_time`     bigint(20) DEFAULT NULL COMMENT '耗时',
    `scene_code`    varchar(128) DEFAULT NULL COMMENT '场景code',
    `detector_code` varchar(128) DEFAULT NULL COMMENT '检测器code',
    `instance_code` varchar(128) DEFAULT NULL COMMENT '检测实例code',
    `task_type`     varchar(32)  NOT NULL COMMENT '任务类型',
    `task_status`   varchar(32)  NOT NULL COMMENT '任务状态',
    `task_req` longtext DEFAULT NULL COMMENT '任务请求',
    `task_result` longtext DEFAULT NULL COMMENT '任务结果',
    PRIMARY KEY (`task_uuid`),
    KEY             `idx_scene_code` (`scene_code`),
    KEY             `idx_detector_code` (`detector_code`),
    KEY             `idx_gmt_create` (`gmt_create`),
    KEY             `idx_task_type` (`task_type`),
    KEY             `idx_instance_code` (`instance_code`),
    KEY             `idx_task_status` (`task_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
;

/******************************************/
/*   DatabaseName = tdata_aisp   */
/*   TableName = aisp_detector_config   */
/******************************************/
CREATE TABLE `aisp_detector_config`
(
    `detector_code` varchar(128) NOT NULL COMMENT '检测器code',
    `gmt_create`    datetime              DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`  datetime              DEFAULT NULL COMMENT '最后修改时间',
    `detector_url`  varchar(128) NOT NULL DEFAULT '*' COMMENT '检测器名称',
    `comment`       longtext COMMENT '备注',
    PRIMARY KEY (`detector_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
;

/******************************************/
/*   DatabaseName = tdata_aisp   */
/*   TableName = aisp_scene_config   */
/******************************************/
CREATE TABLE `aisp_scene_config`
(
    `scene_code`   varchar(128) NOT NULL COMMENT '场景code',
    `gmt_create`   datetime              DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime              DEFAULT NULL COMMENT '最后修改时间',
    `owners`       varchar(128) NOT NULL DEFAULT '' COMMENT 'owner列表',
    `product_name` varchar(128) DEFAULT NULL COMMENT '产品名称',
    `scene_name`   varchar(128) NOT NULL DEFAULT '*' COMMENT '场景名称',
    `scene_model_param` json NULL COMMENT '场景级别模型参数',
    `comment`      longtext COMMENT '备注',
    PRIMARY KEY (`scene_code`),
    KEY            `idx_product_name` (`product_name`),
    KEY            `idx_scene_name` (`scene_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
;

DROP TABLE IF EXISTS `tc_dag`;
DROP TABLE IF EXISTS `tc_dag_config`;
DROP TABLE IF EXISTS `tc_dag_inst`;
DROP TABLE IF EXISTS `tc_dag_inst_edge`;
DROP TABLE IF EXISTS `tc_dag_inst_node`;
DROP TABLE IF EXISTS `tc_dag_inst_node_std`;
DROP TABLE IF EXISTS `tc_dag_node`;
DROP TABLE IF EXISTS `tc_dag_options`;
DROP TABLE IF EXISTS `tc_dag_service_node`;

CREATE TABLE `tc_dag`
(
    `id`                   bigint(32)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`           bigint(32)   NOT NULL COMMENT '创建时间',
    `gmt_modified`         bigint(32)   NOT NULL COMMENT '更新时间',
    `app_id`               varchar(128) NOT NULL DEFAULT 'tesla' COMMENT '应用',
    `name`                 varchar(128) NOT NULL COMMENT '名称',
    `alias`                varchar(128)          DEFAULT NULL COMMENT '别名',
    `content`              longtext     NOT NULL COMMENT '图',
    `input_params`         longtext COMMENT '输入参数',
    `has_feedback`         tinyint(1)            DEFAULT '0' COMMENT '是否打开反馈',
    `has_history`          tinyint(1)            DEFAULT '1' COMMENT '是否保存历史',
    `description`          longtext COMMENT '描述',
    `entity`               longtext COMMENT '实体信息',
    `notice`               varchar(128)          DEFAULT NULL COMMENT '通知',
    `creator`              varchar(128)          DEFAULT NULL COMMENT '创建人',
    `modifier`             varchar(128)          DEFAULT NULL COMMENT '最后修改人',
    `last_update_by`       varchar(128) NOT NULL DEFAULT 'WEB' COMMENT '最后更新方',
    `ex_schedule_task_id`  varchar(128)          DEFAULT NULL COMMENT '外部执行的taskid, 如果为空，则不执行',
    `default_show_history` tinyint(1)            DEFAULT '0' COMMENT '默认展示历史',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_id_name` (`app_id`, `name`) USING BTREE,
    UNIQUE KEY `UKtptpcr7yvef7wxlkxu17q2udv` (`app_id`, `name`),
    KEY `idx_app_id` (`app_id`) USING BTREE,
    KEY `idx_name` (`name`) USING BTREE,
    KEY `idx_creator` (`creator`) USING BTREE,
    KEY `idx_modifier` (`modifier`) USING BTREE,
    KEY `idx_has_feedback` (`has_feedback`) USING BTREE,
    KEY `idx_ has_history` (`has_history`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='场景定义';



CREATE TABLE `tc_dag_config`
(
    `id`           bigint(32)   NOT NULL COMMENT '主键',
    `gmt_create`   bigint(32)   NOT NULL COMMENT '创建时间',
    `gmt_modified` bigint(32)   NOT NULL COMMENT '最后修改时间',
    `name`         varchar(128) NOT NULL COMMENT '名称',
    `content`      longtext COMMENT '内容',
    `comment`      longtext COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='配置';



CREATE TABLE `tc_dag_inst`
(
    `id`                    bigint(32)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`            bigint(32)   NOT NULL COMMENT '创建时间',
    `gmt_modified`          bigint(32)   NOT NULL COMMENT '修改时间',
    `gmt_access`            bigint(32)   NOT NULL COMMENT '上次调度的时间',
    `app_id`                varchar(128) NOT NULL DEFAULT 'tesla' COMMENT '启动的产品方',
    `dag_id`                bigint(32)   NOT NULL COMMENT 'dag的id，用于关联',
    `tc_dag_detail`         longtext     NOT NULL COMMENT '启动时，将dag的全部信息拷贝过来',
    `status`                varchar(128) NOT NULL COMMENT '执行过程中的状态',
    `status_detail`         longtext COMMENT '状态对应的详细说明，一般是错误信息',
    `global_params`         longtext COMMENT '全局参数，提供给faas使用，允许用户修改',
    `global_object`         longtext COMMENT '全局对象，提供给faas使用，允许用户修改',
    `global_variable`       longtext COMMENT '全局变量，启动时提供的，不允许用户修改',
    `global_result`         longtext COMMENT '全局结果，key是node_id，value只有result和output，没有data，因为data太大了，使用data需要实时从作业平台获取',
    `lock_id`               varchar(128)          DEFAULT NULL COMMENT '锁，分布式调度使用',
    `creator`               varchar(128) NOT NULL COMMENT '启动人，工号',
    `is_sub`                tinyint(1)   NOT NULL DEFAULT '0' COMMENT '是否归属于另外一个dag',
    `tag`                   varchar(128)          DEFAULT NULL COMMENT '标签，目前使用于entityValue',
    `standalone_ip`         varchar(128)          DEFAULT NULL COMMENT '单独调度指定的机器ip',
    `evaluation_create_ret` longtext COMMENT '评价服务创建后的反馈',
    `channel`               varchar(128)          DEFAULT NULL COMMENT '调用渠道',
    `env`                   varchar(128)          DEFAULT NULL COMMENT '调用环境',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`) USING BTREE,
    KEY `idx_lock_id` (`lock_id`) USING BTREE,
    KEY `idx_app_id` (`app_id`) USING BTREE,
    KEY `idx_is_sub` (`is_sub`) USING BTREE,
    KEY `idx_tag` (`tag`) USING BTREE,
    KEY `idx_standalone_ip` (`standalone_ip`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='场景实例';



CREATE TABLE `tc_dag_inst_edge`
(
    `id`           bigint(32)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`   bigint(32)   NOT NULL COMMENT '创建时间',
    `gmt_modified` bigint(32)   NOT NULL COMMENT '最后修改时间',
    `dag_inst_id`  bigint(32)   NOT NULL COMMENT '场景实例',
    `source`       varchar(128) NOT NULL COMMENT '源节点',
    `target`       varchar(128) NOT NULL COMMENT '目前节点',
    `label`        varchar(128)          DEFAULT NULL COMMENT '前端标识',
    `shape`        varchar(128) NOT NULL COMMENT '前端标识',
    `style`        longtext     NOT NULL COMMENT '前端标识',
    `data`         longtext     NOT NULL COMMENT '内容，包含判断条件',
    `is_pass`      int(1)                DEFAULT NULL COMMENT '是否通过',
    `exception`    longtext COMMENT '错误信息',
    `status`       varchar(128) NOT NULL DEFAULT 'INIT' COMMENT '判断状态',
    PRIMARY KEY (`id`),
    KEY `idx_dag_inst_id` (`dag_inst_id`) USING BTREE,
    KEY `idx_is_pass` (`is_pass`) USING BTREE,
    KEY `idx_source` (`source`) USING BTREE,
    KEY `idx_target` (`target`) USING BTREE,
    KEY `idx_status` (`status`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='边实例';



CREATE TABLE `tc_dag_inst_node`
(
    `id`                       bigint(32)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`               bigint(32)   NOT NULL COMMENT '创建时间',
    `gmt_modified`             bigint(32)   NOT NULL COMMENT '修改时间',
    `gmt_start`                bigint(32)   NOT NULL DEFAULT '999999999999999' COMMENT '开始执行时间',
    `dag_inst_id`              bigint(32)   NOT NULL COMMENT '场景实例id',
    `node_id`                  varchar(128) NOT NULL COMMENT '节点id',
    `status`                   varchar(32)  NOT NULL COMMENT '状态',
    `status_detail`            longtext COMMENT '状态详情，一般为错误详情',
    `task_id`                  varchar(128)          DEFAULT NULL COMMENT '执行id，本地的id或者作业的id',
    `stop_task_id`             varchar(128)          DEFAULT NULL COMMENT '停止的执行id，只有本地的才有',
    `lock_id`                  varchar(128)          DEFAULT NULL COMMENT '锁',
    `sub_dag_inst_id`          bigint(32)            DEFAULT NULL COMMENT '如果node是场景的映射，该场景的实例id',
    `tc_dag_or_node_detail`    longtext     NOT NULL COMMENT '映射场景或者node的详情',
    `tc_dag_content_node_spec` longtext     NOT NULL COMMENT '映射的是node，node在场景的content定义中的信息',
    `retry_times`              bigint(32)            DEFAULT '0' COMMENT '重试次数，只执行了一次，重试的次数是0',
    `drg_serial`               varchar(128)          DEFAULT NULL COMMENT '独立环编号，如果不属于环则为空或空字符串',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_lock_id` (`dag_inst_id`, `node_id`) USING BTREE,
    KEY `dag_inst_id` (`dag_inst_id`) USING BTREE,
    KEY `idx_dag_inst_id` (`node_id`) USING BTREE,
    KEY `idx_node_id` (`status`) USING BTREE,
    KEY `idx_status` (`lock_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='节点实例';



CREATE TABLE `tc_dag_inst_node_std`
(
    `id`               bigint(32)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`       bigint(32)  NOT NULL COMMENT '创建时间',
    `gmt_modified`     bigint(32)  NOT NULL COMMENT '修改时间',
    `gmt_access`       bigint(32)   DEFAULT NULL COMMENT '最后调度时间',
    `status`           varchar(32) NOT NULL COMMENT '状态 INIT/RUNNING/SUCCESS/EXCEPTION',
    `stdout`           longtext COMMENT '标准输出',
    `stderr`           longtext COMMENT '错误输出',
    `global_params`    longtext COMMENT '全局参数',
    `ip`               varchar(128) DEFAULT NULL COMMENT '执行机器的ip',
    `standalone_ip`    varchar(128) DEFAULT NULL COMMENT '指定执行机器的ip',
    `comment`          longtext COMMENT '执行备注',
    `is_stop`          tinyint(1)   DEFAULT NULL COMMENT '是否为停止操作',
    `stop_id`          bigint(32)   DEFAULT NULL COMMENT '执行停止的目标id',
    `lock_id`          varchar(128) DEFAULT NULL COMMENT '锁',
    `dag_inst_node_id` bigint(32)  NOT NULL COMMENT '场景节点实例id',
    PRIMARY KEY (`id`),
    KEY `idx_ip` (`ip`) USING BTREE,
    KEY `idx_is_stop` (`is_stop`) USING BTREE,
    KEY `idx_status` (`status`) USING BTREE,
    KEY `idx_stop_id` (`stop_id`) USING BTREE,
    KEY `idx_lock_id` (`lock_id`) USING BTREE,
    KEY `idx_standalone_ip` (`standalone_ip`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='执行输出';



CREATE TABLE `tc_dag_node`
(
    `id`                 bigint(32)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`         bigint(32)   NOT NULL COMMENT '创建时间',
    `gmt_modified`       bigint(32)   NOT NULL COMMENT '最后修改时间',
    `app_id`             varchar(128) NOT NULL COMMENT '产品',
    `name`               varchar(128) NOT NULL COMMENT '名称，产品下唯一',
    `alias`              varchar(128)          DEFAULT NULL COMMENT '别名',
    `description`        longtext COMMENT '说明',
    `is_share`           tinyint(1)   NOT NULL DEFAULT '0' COMMENT '是否共享',
    `input_params`       longtext COMMENT '输入参数',
    `output_params`      longtext COMMENT '输出参数',
    `type`               varchar(128) NOT NULL COMMENT '执行类型 API FAAS TASK',
    `detail`             longtext COMMENT '根据不同的执行类型，有不同的内容',
    `is_show`            tinyint(1)   NOT NULL DEFAULT '1' COMMENT '是否展示',
    `format_type`        varchar(128)          DEFAULT NULL COMMENT '展示类型',
    `format_detail`      longtext COMMENT '展示详情',
    `creator`            varchar(128)          DEFAULT NULL COMMENT '创建人',
    `modifier`           varchar(128)          DEFAULT NULL COMMENT '修改人',
    `is_support_chatops` tinyint(1)            DEFAULT '0' COMMENT '是否支持机器人',
    `chatops_detail`     longtext COMMENT '机器人展示详情',
    `last_update_by`     varchar(128) NOT NULL DEFAULT 'WEB' COMMENT '最后由谁修改',
    `run_timeout`        bigint(32)            DEFAULT '0' COMMENT '执行超时时间',
    `max_retry_times`    bigint(32)            DEFAULT '0' COMMENT '重试次数上限，如果是-1则没有上限，0就是不重试',
    `retry_expression`   longtext COMMENT '重试判断，如果为空，则重试判断不通过',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_id_name` (`app_id`, `name`) USING BTREE,
    UNIQUE KEY `UKbx4edwumljlwe4wmoqf4vbwka` (`app_id`, `name`),
    KEY `idx_app_id` (`app_id`) USING BTREE,
    KEY `idx_name` (`name`) USING BTREE,
    KEY `idx_is_share` (`is_share`) USING BTREE,
    KEY `idx_last_update_by` (`last_update_by`) USING BTREE,
    KEY `idx_creator` (`creator`) USING BTREE,
    KEY `idx_modifier` (`modifier`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='节点';



CREATE TABLE `tc_dag_options`
(
    `id`     bigint(32)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `locale` varchar(32)  NOT NULL COMMENT '地区标识',
    `name`   varchar(128) NOT NULL COMMENT '名称',
    `value`  longtext COMMENT '内容',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_locale_name` (`locale`, `name`) USING BTREE,
    UNIQUE KEY `UK87jx3r3tqd5fx71yqprq1mecj` (`locale`, `name`),
    KEY `idx_locale` (`locale`) USING BTREE,
    KEY `idx_name` (`name`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='国产化';



CREATE TABLE `tc_dag_service_node`
(
    `id`           bigint(32)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`   bigint(32)   NOT NULL COMMENT '创建时间',
    `gmt_modified` bigint(32)   NOT NULL COMMENT '修改时间',
    `ip`           varchar(128) NOT NULL COMMENT '机器ip',
    `enable`       tinyint(1)   NOT NULL DEFAULT '1' COMMENT '是够允许',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_ip` (`ip`) USING BTREE,
    UNIQUE KEY `UKc13j43iwbnwlir1tlw0bv4q5x` (`ip`),
    KEY `idx_enable` (`enable`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='节点状态';
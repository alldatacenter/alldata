CREATE TABLE QRTZ_JOB_DETAILS
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    JOB_NAME          VARCHAR(190) NOT NULL,
    JOB_GROUP         VARCHAR(190) NOT NULL,
    DESCRIPTION       VARCHAR(250) NULL,
    JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
    IS_DURABLE        VARCHAR(1)   NOT NULL,
    IS_NONCONCURRENT  VARCHAR(1)   NOT NULL,
    IS_UPDATE_DATA    VARCHAR(1)   NOT NULL,
    REQUESTS_RECOVERY VARCHAR(1)   NOT NULL,
    JOB_DATA          BLOB         NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE = InnoDB;

CREATE TABLE QRTZ_TRIGGERS
(
    SCHED_NAME     VARCHAR(120) NOT NULL,
    TRIGGER_NAME   VARCHAR(190) NOT NULL,
    TRIGGER_GROUP  VARCHAR(190) NOT NULL,
    JOB_NAME       VARCHAR(190) NOT NULL,
    JOB_GROUP      VARCHAR(190) NOT NULL,
    DESCRIPTION    VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT(13)   NULL,
    PREV_FIRE_TIME BIGINT(13)   NULL,
    PRIORITY       INTEGER      NULL,
    TRIGGER_STATE  VARCHAR(16)  NOT NULL,
    TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
    START_TIME     BIGINT(13)   NOT NULL,
    END_TIME       BIGINT(13)   NULL,
    CALENDAR_NAME  VARCHAR(190) NULL,
    MISFIRE_INSTR  SMALLINT(2)  NULL,
    JOB_DATA       BLOB         NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE = InnoDB;

CREATE TABLE QRTZ_SIMPLE_TRIGGERS
(
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(190) NOT NULL,
    TRIGGER_GROUP   VARCHAR(190) NOT NULL,
    REPEAT_COUNT    BIGINT(7)    NOT NULL,
    REPEAT_INTERVAL BIGINT(12)   NOT NULL,
    TIMES_TRIGGERED BIGINT(10)   NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

CREATE TABLE QRTZ_CRON_TRIGGERS
(
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(190) NOT NULL,
    TRIGGER_GROUP   VARCHAR(190) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID    VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

CREATE TABLE QRTZ_SIMPROP_TRIGGERS
(
    SCHED_NAME    VARCHAR(120)   NOT NULL,
    TRIGGER_NAME  VARCHAR(190)   NOT NULL,
    TRIGGER_GROUP VARCHAR(190)   NOT NULL,
    STR_PROP_1    VARCHAR(512)   NULL,
    STR_PROP_2    VARCHAR(512)   NULL,
    STR_PROP_3    VARCHAR(512)   NULL,
    INT_PROP_1    INT            NULL,
    INT_PROP_2    INT            NULL,
    LONG_PROP_1   BIGINT         NULL,
    LONG_PROP_2   BIGINT         NULL,
    DEC_PROP_1    NUMERIC(13, 4) NULL,
    DEC_PROP_2    NUMERIC(13, 4) NULL,
    BOOL_PROP_1   VARCHAR(1)     NULL,
    BOOL_PROP_2   VARCHAR(1)     NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

CREATE TABLE QRTZ_BLOB_TRIGGERS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    BLOB_DATA     BLOB         NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    INDEX (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

CREATE TABLE QRTZ_CALENDARS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(190) NOT NULL,
    CALENDAR      BLOB         NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
) ENGINE = InnoDB;

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

CREATE TABLE QRTZ_FIRED_TRIGGERS
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    ENTRY_ID          VARCHAR(95)  NOT NULL,
    TRIGGER_NAME      VARCHAR(190) NOT NULL,
    TRIGGER_GROUP     VARCHAR(190) NOT NULL,
    INSTANCE_NAME     VARCHAR(190) NOT NULL,
    FIRED_TIME        BIGINT(13)   NOT NULL,
    SCHED_TIME        BIGINT(13)   NOT NULL,
    PRIORITY          INTEGER      NOT NULL,
    STATE             VARCHAR(16)  NOT NULL,
    JOB_NAME          VARCHAR(190) NULL,
    JOB_GROUP         VARCHAR(190) NULL,
    IS_NONCONCURRENT  VARCHAR(1)   NULL,
    REQUESTS_RECOVERY VARCHAR(1)   NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
) ENGINE = InnoDB;

CREATE TABLE QRTZ_SCHEDULER_STATE
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    INSTANCE_NAME     VARCHAR(190) NOT NULL,
    LAST_CHECKIN_TIME BIGINT(13)   NOT NULL,
    CHECKIN_INTERVAL  BIGINT(13)   NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
) ENGINE = InnoDB;

CREATE TABLE QRTZ_LOCKS
(
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
) ENGINE = InnoDB;

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS (SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS (SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS (SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS (SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP,
                                                             TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);

CREATE TABLE `tc_dag`
(
    `id`                   bigint(32)   NOT NULL AUTO_INCREMENT,
    `gmt_create`           bigint(32)   NOT NULL,
    `gmt_modified`         bigint(32)   NOT NULL,
    `app_id`               varchar(128) NOT NULL DEFAULT 'tesla',
    `name`                 varchar(128) NOT NULL,
    `alias`                varchar(128)          DEFAULT NULL,
    `content`              longtext     NOT NULL,
    `input_params`         longtext,
    `has_feedback`         tinyint(1)            DEFAULT '0',
    `has_history`          tinyint(1)            DEFAULT '1',
    `description`          longtext,
    `entity`               longtext,
    `notice`               varchar(128)          DEFAULT NULL,
    `creator`              varchar(128)          DEFAULT NULL,
    `modifier`             varchar(128)          DEFAULT NULL,
    `last_update_by`       varchar(128) NOT NULL DEFAULT 'WEB',
    `ex_schedule_task_id`  varchar(128)          DEFAULT NULL,
    `default_show_history` tinyint(1)            DEFAULT '0',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `app_id_name` (`app_id`, `name`) USING BTREE,
    KEY `app_id` (`app_id`) USING BTREE,
    KEY `name` (`name`) USING BTREE,
    KEY `creator` (`creator`) USING BTREE,
    KEY `modifier` (`modifier`) USING BTREE,
    KEY `is_feedback` (`has_feedback`) USING BTREE,
    KEY `has_history` (`has_history`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 5
  DEFAULT CHARSET = utf8mb4;



CREATE TABLE `tc_dag_config`
(
    `id`           bigint(32)   NOT NULL,
    `gmt_create`   bigint(32)   NOT NULL,
    `gmt_modified` bigint(32)   NOT NULL,
    `name`         varchar(128) NOT NULL,
    `content`      longtext,
    `comment`      longtext,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `name` (`name`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;



CREATE TABLE `tc_dag_faas`
(
    `id`              bigint(32)   NOT NULL AUTO_INCREMENT,
    `gmt_create`      bigint(32)   NOT NULL,
    `gmt_modified`    bigint(32)   NOT NULL,
    `app_id`          varchar(128) NOT NULL,
    `name`            varchar(128) NOT NULL,
    `is_rsync_active` tinyint(1)   NOT NULL DEFAULT '0',
    `rsync_detail`    longtext,
    `last_rsync_time` bigint(32)            DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `name` (`name`) USING BTREE,
    KEY `app_id` (`app_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  DEFAULT CHARSET = utf8mb4;



CREATE TABLE `tc_dag_inst`
(
    `id`                           bigint(32)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                   bigint(32)   NOT NULL COMMENT '创建时间',
    `gmt_modified`                 bigint(32)   NOT NULL COMMENT '修改时间',
    `gmt_access`                   bigint(32)   NOT NULL COMMENT '上次调度的时间',
    `app_id`                       varchar(128) NOT NULL DEFAULT 'tesla' COMMENT '启动的产品方',
    `dag_id`                       bigint(32)   NOT NULL COMMENT 'dag的id，用于关联',
    `tc_dag_detail`                longtext     NOT NULL COMMENT '启动时，将dag的全部信息拷贝过来',
    `status`                       varchar(128) NOT NULL COMMENT '执行过程中的状态',
    `status_detail`                longtext COMMENT '状态对应的详细说明，一般是错误信息',
    `global_params`                longtext COMMENT '全局参数，提供给faas使用，允许用户修改',
    `global_object`                longtext COMMENT '全局对象，提供给faas使用，允许用户修改',
    `global_variable`              longtext COMMENT '全局变量，启动时提供的，不允许用户修改',
    `global_result`                longtext COMMENT '全局结果，key是node_id，value只有result和output，没有data，因为data太大了，使用data需要实时从执行侧获取',
    `lock_id`                      varchar(128)          DEFAULT NULL COMMENT '锁，分布式调度使用',
    `creator`                      varchar(128) NOT NULL COMMENT '启动人，工号',
    `is_sub`                       tinyint(1)   NOT NULL DEFAULT '0' COMMENT '是否归属于另外一个dag',
    `tag`                          varchar(128)          DEFAULT NULL COMMENT '标签，目前使用于entityValue',
    `ex_schedule_task_instance_id` varchar(128)          DEFAULT NULL COMMENT '外部调度的作业id',
    `standalone_ip`                varchar(128)          DEFAULT NULL COMMENT '单独调度指定的机器ip',
    `evaluation_create_ret`        longtext COMMENT '评价服务创建后的反馈',
    `channel`                      varchar(128)          DEFAULT NULL COMMENT '调用渠道',
    `env`                          varchar(128)          DEFAULT NULL COMMENT '调用环境',
    `drg_detail`                   longtext,
    `father_dag_inst_node_id`      bigint(20) unsigned   DEFAULT NULL COMMENT '父节点',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_status` (`status`) USING BTREE,
    KEY `idx_lock_id` (`lock_id`) USING BTREE,
    KEY `idx_app_id` (`app_id`) USING BTREE,
    KEY `idx_is_sub` (`is_sub`) USING BTREE,
    KEY `idx_tag` (`tag`) USING BTREE,
    KEY `idx_standalone_ip` (`standalone_ip`) USING BTREE,
    KEY `idx_dag_id_status_gmt_create` (`dag_id`, `status`, `gmt_create`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_dag_id` (`dag_id`) USING BTREE,
    KEY `idx_gmt_access_status` (`gmt_access`, `status`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 681
  DEFAULT CHARSET = utf8mb4;



CREATE TABLE `tc_dag_inst_edge`
(
    `id`           bigint(32)   NOT NULL AUTO_INCREMENT,
    `gmt_create`   bigint(32)   NOT NULL,
    `gmt_modified` bigint(32)   NOT NULL,
    `dag_inst_id`  bigint(32)   NOT NULL,
    `source`       varchar(128) NOT NULL,
    `target`       varchar(128) NOT NULL,
    `label`        varchar(128)          DEFAULT NULL,
    `shape`        varchar(128)          DEFAULT NULL,
    `style`        longtext,
    `data`         longtext     NOT NULL,
    `is_pass`      int(1)                DEFAULT NULL,
    `exception`    longtext,
    `status`       varchar(128) NOT NULL DEFAULT 'INIT',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `dag_inst_id` (`dag_inst_id`) USING BTREE,
    KEY `is_pass` (`is_pass`) USING BTREE,
    KEY `source` (`source`) USING BTREE,
    KEY `target` (`target`) USING BTREE,
    KEY `status` (`status`) USING BTREE,
    KEY `idx_gmt_create` (`gmt_create`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 681
  DEFAULT CHARSET = utf8mb4;



CREATE TABLE `tc_dag_inst_node`
(
    `id`                       bigint(32)   NOT NULL AUTO_INCREMENT,
    `gmt_create`               bigint(32)   NOT NULL,
    `gmt_modified`             bigint(32)   NOT NULL,
    `gmt_start`                bigint(32)   NOT NULL DEFAULT '999999999999999',
    `dag_inst_id`              bigint(32)   NOT NULL,
    `node_id`                  varchar(128) NOT NULL,
    `status`                   varchar(32)  NOT NULL,
    `status_detail`            longtext,
    `task_id`                  varchar(128)          DEFAULT NULL,
    `stop_task_id`             varchar(128)          DEFAULT NULL,
    `lock_id`                  varchar(128)          DEFAULT NULL,
    `sub_dag_inst_id`          bigint(32)            DEFAULT NULL,
    `tc_dag_or_node_detail`    longtext     NOT NULL,
    `tc_dag_content_node_spec` longtext     NOT NULL,
    `retry_times`              bigint(32)            DEFAULT '0',
    `drg_serial`               varchar(128)          DEFAULT NULL,
    `global_params`            longtext COMMENT '作业参数',
    `global_object`            longtext COMMENT '用户传入的对象',
    `global_result`            longtext COMMENT '作业结果',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk` (`dag_inst_id`, `node_id`) USING BTREE,
    KEY `dag_inst_id` (`dag_inst_id`) USING BTREE,
    KEY `node_id` (`node_id`) USING BTREE,
    KEY `status` (`status`) USING BTREE,
    KEY `lock_id` (`lock_id`) USING BTREE,
    KEY `idx_gmt_create` (`gmt_create`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1361
  DEFAULT CHARSET = utf8mb4;



CREATE TABLE `tc_dag_inst_node_std`
(
    `id`               bigint(32)  NOT NULL AUTO_INCREMENT,
    `gmt_create`       bigint(32)  NOT NULL,
    `gmt_modified`     bigint(32)  NOT NULL,
    `gmt_access`       bigint(32)   DEFAULT NULL,
    `status`           varchar(32) NOT NULL COMMENT '状态 RUNNING/SUCCESS/EXCEPTION',
    `stdout`           longtext COMMENT '标准输出',
    `stderr`           longtext COMMENT '错误输出',
    `global_params`    longtext COMMENT '全局参数',
    `ip`               varchar(128) DEFAULT NULL,
    `standalone_ip`    varchar(128) DEFAULT NULL,
    `comment`          longtext,
    `is_stop`          tinyint(1)   DEFAULT NULL,
    `stop_id`          bigint(32)   DEFAULT NULL,
    `lock_id`          varchar(128) DEFAULT NULL,
    `dag_inst_node_id` bigint(32)   DEFAULT NULL,
    `dag_inst_id`      bigint(32)   DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `ip` (`ip`) USING BTREE,
    KEY `is_stop` (`is_stop`) USING BTREE,
    KEY `status` (`status`) USING BTREE,
    KEY `stop_id` (`stop_id`) USING BTREE,
    KEY `idx_lock_id` (`lock_id`) USING BTREE,
    KEY `idx_standalone_ip` (`standalone_ip`) USING BTREE,
    KEY `idx_gmt_create` (`gmt_create`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1347
  DEFAULT CHARSET = utf8mb4;



CREATE TABLE `tc_dag_node`
(
    `id`                 bigint(32)   NOT NULL AUTO_INCREMENT,
    `gmt_create`         bigint(32)   NOT NULL,
    `gmt_modified`       bigint(32)   NOT NULL,
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
    `run_timeout`        bigint(32)            DEFAULT NULL COMMENT '执行超时时间',
    `max_retry_times`    bigint(32)            DEFAULT '0' COMMENT '重试次数上限，如果是-1则没有上限，0就是不重试',
    `retry_expression`   longtext COMMENT '重试判断，如果为空，则重试判断不通过',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk` (`app_id`, `name`) USING BTREE,
    KEY `app_id` (`app_id`) USING BTREE,
    KEY `name` (`name`) USING BTREE,
    KEY `is_share` (`is_share`) USING BTREE,
    KEY `last_update_by` (`last_update_by`) USING BTREE,
    KEY `creator` (`creator`) USING BTREE,
    KEY `modifier` (`modifier`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 190799
  DEFAULT CHARSET = utf8mb4;



CREATE TABLE `tc_dag_options`
(
    `id`     bigint(32)   NOT NULL AUTO_INCREMENT,
    `locale` varchar(128) NOT NULL,
    `name`   varchar(256) NOT NULL,
    `value`  longtext,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `locale_name` (`locale`, `name`(128)) USING BTREE,
    KEY `locale` (`locale`) USING BTREE,
    KEY `name` (`name`(191)) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 523862
  DEFAULT CHARSET = utf8mb4;



CREATE TABLE `tc_dag_service_node`
(
    `id`           bigint(32)   NOT NULL AUTO_INCREMENT,
    `gmt_create`   bigint(32)   NOT NULL,
    `gmt_modified` bigint(32)   NOT NULL,
    `ip`           varchar(128) NOT NULL,
    `enable`       tinyint(1)   NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `ip` (`ip`) USING BTREE,
    KEY `enable` (`enable`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 23
  DEFAULT CHARSET = utf8mb4;



CREATE TABLE IF NOT EXISTS `sreworks_config`
(
    `id`           bigint                                                  NOT NULL AUTO_INCREMENT,
    `name`         varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `content`      longtext                                                NOT NULL,
    `gmt_create`   bigint                                                  NOT NULL,
    `gmt_modified` bigint                                                  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `type` (`name`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `sreworks_job`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT,
    `gmt_create`    bigint       NOT NULL,
    `gmt_modified`  bigint       NOT NULL,
    `creator`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
    `operator`      varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
    `app_id`        varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
    `name`          varchar(255) NOT NULL,
    `alias`         varchar(255) NOT NULL,
    `tags`          longtext,
    `description`   longtext,
    `options`       longtext,
    `trigger_type`  varchar(255)                                            DEFAULT NULL,
    `trigger_conf`  varchar(255)                                            DEFAULT NULL,
    `schedule_type` varchar(255)                                            DEFAULT NULL,
    `scene_type`    varchar(255)                                            DEFAULT NULL,
    `var_conf`      longtext,
    `notify_conf`   longtext,
    `event_conf`    longtext,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_name` (`name`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 82
  DEFAULT CHARSET = utf8;


CREATE TABLE IF NOT EXISTS `sreworks_job_task`
(
    `id`                  bigint                                                  NOT NULL AUTO_INCREMENT,
    `gmt_create`          bigint                                                  NOT NULL,
    `gmt_modified`        bigint                                                  NOT NULL,
    `creator`             varchar(255)                                            DEFAULT NULL,
    `operator`            varchar(255)                                            DEFAULT NULL,
    `app_id`              varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
    `name`                varchar(255)                                            NOT NULL,
    `alias`               varchar(255)                                            NOT NULL,
    `tags`                longtext CHARACTER SET utf8 COLLATE utf8_general_ci,
    `exec_timeout`        bigint                                                  NOT NULL,
    `exec_type`           varchar(255)                                            NOT NULL,
    `exec_content`        longtext CHARACTER SET utf8 COLLATE utf8_general_ci     NOT NULL,
    `exec_retry_times`    bigint                                                  DEFAULT NULL,
    `exec_retry_interval` bigint                                                  DEFAULT NULL,
    `var_conf`            longtext CHARACTER SET utf8 COLLATE utf8_general_ci,
    `scene_type`          varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `scene_conf`          longtext,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_name` (`name`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 79
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `sreworks_job_worker`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `gmt_create`     bigint       NOT NULL,
    `gmt_modified`   bigint       NOT NULL,
    `group_name`     varchar(255) NOT NULL,
    `address`        varchar(255) NOT NULL,
    `enable`         int          NOT NULL,
    `exec_type_list` longtext     NOT NULL,
    `pool_size`      int DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `n_address` (`address`) USING BTREE,
    KEY `n_group_name` (`group_name`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 359
  DEFAULT CHARSET = utf8;







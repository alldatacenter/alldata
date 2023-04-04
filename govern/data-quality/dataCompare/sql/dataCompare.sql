SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for db_config
-- ----------------------------
DROP TABLE IF EXISTS `db_config`;
CREATE TABLE `db_config` (
                             `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
                             `connect_name` varchar(255) NOT NULL COMMENT 'db connect_name',
                             `type` varchar(255) NOT NULL COMMENT 'db config type',
                             `url` varchar(255) NOT NULL COMMENT 'db config url',
                             `user_name` varchar(255) DEFAULT NULL COMMENT 'db config user_name',
                             `pwd` varchar(255) DEFAULT NULL COMMENT 'db config pwd',
                             `create_time` datetime DEFAULT NULL COMMENT 'create_time',
                             `create_by` varchar(255) DEFAULT NULL COMMENT 'create_by',
                             PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8 COMMENT='db config';

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table` (
                             `table_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
                             `table_name` varchar(200) DEFAULT '' COMMENT '表名称',
                             `table_comment` varchar(500) DEFAULT '' COMMENT '表描述',
                             `sub_table_name` varchar(64) DEFAULT NULL COMMENT '关联子表的表名',
                             `sub_table_fk_name` varchar(64) DEFAULT NULL COMMENT '子表关联的外键名',
                             `class_name` varchar(100) DEFAULT '' COMMENT '实体类名称',
                             `tpl_category` varchar(200) DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作 sub主子表操作）',
                             `package_name` varchar(100) DEFAULT NULL COMMENT '生成包路径',
                             `module_name` varchar(30) DEFAULT NULL COMMENT '生成模块名',
                             `business_name` varchar(30) DEFAULT NULL COMMENT '生成业务名',
                             `function_name` varchar(50) DEFAULT NULL COMMENT '生成功能名',
                             `function_author` varchar(50) DEFAULT NULL COMMENT '生成功能作者',
                             `gen_type` char(1) DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
                             `gen_path` varchar(200) DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
                             `options` varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
                             `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                             `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                             `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                             PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='代码生成业务表';

-- ----------------------------
-- Records of gen_table
-- ----------------------------

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column` (
                                    `column_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
                                    `table_id` varchar(64) DEFAULT NULL COMMENT '归属表编号',
                                    `column_name` varchar(200) DEFAULT NULL COMMENT '列名称',
                                    `column_comment` varchar(500) DEFAULT NULL COMMENT '列描述',
                                    `column_type` varchar(100) DEFAULT NULL COMMENT '列类型',
                                    `java_type` varchar(500) DEFAULT NULL COMMENT 'JAVA类型',
                                    `java_field` varchar(200) DEFAULT NULL COMMENT 'JAVA字段名',
                                    `is_pk` char(1) DEFAULT NULL COMMENT '是否主键（1是）',
                                    `is_increment` char(1) DEFAULT NULL COMMENT '是否自增（1是）',
                                    `is_required` char(1) DEFAULT NULL COMMENT '是否必填（1是）',
                                    `is_insert` char(1) DEFAULT NULL COMMENT '是否为插入字段（1是）',
                                    `is_edit` char(1) DEFAULT NULL COMMENT '是否编辑字段（1是）',
                                    `is_list` char(1) DEFAULT NULL COMMENT '是否列表字段（1是）',
                                    `is_query` char(1) DEFAULT NULL COMMENT '是否查询字段（1是）',
                                    `query_type` varchar(200) DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
                                    `html_type` varchar(200) DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
                                    `dict_type` varchar(200) DEFAULT '' COMMENT '字典类型',
                                    `sort` int(11) DEFAULT NULL COMMENT '排序',
                                    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                    PRIMARY KEY (`column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='代码生成业务表字段';

-- ----------------------------
-- Records of gen_table_column
-- ----------------------------

-- ----------------------------
-- Table structure for job_config
-- ----------------------------
DROP TABLE IF EXISTS `job_config`;
CREATE TABLE `job_config` (
                              `id` int(11) NOT NULL AUTO_INCREMENT,
                              `origin_table_name` varchar(255) DEFAULT NULL,
                              `origin_table_primary` varchar(255) DEFAULT NULL,
                              `origin_table_fields` varchar(255) DEFAULT NULL,
                              `to_table_name` varchar(255) DEFAULT NULL,
                              `to_table_primary` varchar(255) DEFAULT NULL,
                              `to_table_fields` varchar(255) DEFAULT NULL,
                              `db_config_id` int(11) DEFAULT NULL,
                              `create_time` datetime DEFAULT NULL,
                              `schdule_time` varchar(255) DEFAULT NULL,
                              `schdule_status` tinyint(1) DEFAULT '1' COMMENT '0:true,1:false',
                              `create_by` varchar(255) DEFAULT NULL COMMENT 'create_by',
                              `origin_table_filter` text,
                              `to_table_filter` text,
                              `origin_table_group` text,
                              `to_table_group` text,
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8 COMMENT='job_config';

-- ----------------------------
-- Table structure for job_instance
-- ----------------------------
DROP TABLE IF EXISTS `job_instance`;
CREATE TABLE `job_instance` (
                                `id` int(11) NOT NULL AUTO_INCREMENT,
                                `job_config_id` int(11) NOT NULL,
                                `origin_table_pv` varchar(255) NOT NULL COMMENT 'origin_table_pv',
                                `origin_table_uv` varchar(255) NOT NULL COMMENT 'origin_table_uv',
                                `to_table_pv` varchar(255) DEFAULT NULL COMMENT 'to_table_pv',
                                `to_table_uv` varchar(255) DEFAULT NULL COMMENT 'to_table_uv',
                                `pv_diff` varchar(255) DEFAULT NULL COMMENT 'pv_diff',
                                `uv_diff` varchar(255) DEFAULT NULL COMMENT 'uv_diff',
                                `magnitude_sql` longtext COMMENT 'magnitude_sql',
                                `origin_table_count` varchar(255) DEFAULT NULL COMMENT 'origin_table_count',
                                `to_table_count` varchar(255) DEFAULT NULL COMMENT 'to_table_count',
                                `count_diff` varchar(255) DEFAULT NULL COMMENT 'count_diff',
                                `consistency_sql` longtext COMMENT 'consistency_sql',
                                `dt` varchar(255) DEFAULT NULL COMMENT 'dt',
                                `create_time` datetime DEFAULT NULL,
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8 COMMENT='job_instance';

-- ----------------------------
-- Table structure for QRTZ_BLOB_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
CREATE TABLE `QRTZ_BLOB_TRIGGERS` (
                                      `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
                                      `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                      `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                      `blob_data` blob COMMENT '存放持久化Trigger对象',
                                      PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                      CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `QRTZ_TRIGGERS` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Blob类型的触发器表';

-- ----------------------------
-- Records of QRTZ_BLOB_TRIGGERS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_CALENDARS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
CREATE TABLE `QRTZ_CALENDARS` (
                                  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
                                  `calendar_name` varchar(200) NOT NULL COMMENT '日历名称',
                                  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
                                  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='日历信息表';

-- ----------------------------
-- Records of QRTZ_CALENDARS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_CRON_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
CREATE TABLE `QRTZ_CRON_TRIGGERS` (
                                      `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
                                      `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                      `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                      `cron_expression` varchar(200) NOT NULL COMMENT 'cron表达式',
                                      `time_zone_id` varchar(80) DEFAULT NULL COMMENT '时区',
                                      PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                      CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `QRTZ_TRIGGERS` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Cron类型的触发器表';

-- ----------------------------
-- Records of QRTZ_CRON_TRIGGERS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_FIRED_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
CREATE TABLE `QRTZ_FIRED_TRIGGERS` (
                                       `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
                                       `entry_id` varchar(95) NOT NULL COMMENT '调度器实例id',
                                       `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                       `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                       `instance_name` varchar(200) NOT NULL COMMENT '调度器实例名',
                                       `fired_time` bigint(13) NOT NULL COMMENT '触发的时间',
                                       `sched_time` bigint(13) NOT NULL COMMENT '定时器制定的时间',
                                       `priority` int(11) NOT NULL COMMENT '优先级',
                                       `state` varchar(16) NOT NULL COMMENT '状态',
                                       `job_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
                                       `job_group` varchar(200) DEFAULT NULL COMMENT '任务组名',
                                       `is_nonconcurrent` varchar(1) DEFAULT NULL COMMENT '是否并发',
                                       `requests_recovery` varchar(1) DEFAULT NULL COMMENT '是否接受恢复执行',
                                       PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='已触发的触发器表';

-- ----------------------------
-- Records of QRTZ_FIRED_TRIGGERS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_JOB_DETAILS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
CREATE TABLE `QRTZ_JOB_DETAILS` (
                                    `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
                                    `job_name` varchar(200) NOT NULL COMMENT '任务名称',
                                    `job_group` varchar(200) NOT NULL COMMENT '任务组名',
                                    `description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
                                    `job_class_name` varchar(250) NOT NULL COMMENT '执行任务类名称',
                                    `is_durable` varchar(1) NOT NULL COMMENT '是否持久化',
                                    `is_nonconcurrent` varchar(1) NOT NULL COMMENT '是否并发',
                                    `is_update_data` varchar(1) NOT NULL COMMENT '是否更新数据',
                                    `requests_recovery` varchar(1) NOT NULL COMMENT '是否接受恢复执行',
                                    `job_data` blob COMMENT '存放持久化job对象',
                                    PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='任务详细信息表';

-- ----------------------------
-- Records of QRTZ_JOB_DETAILS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_LOCKS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_LOCKS`;
CREATE TABLE `QRTZ_LOCKS` (
                              `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
                              `lock_name` varchar(40) NOT NULL COMMENT '悲观锁名称',
                              PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='存储的悲观锁信息表';

-- ----------------------------
-- Records of QRTZ_LOCKS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_PAUSED_TRIGGER_GRPS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS` (
                                            `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
                                            `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                            PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='暂停的触发器表';

-- ----------------------------
-- Records of QRTZ_PAUSED_TRIGGER_GRPS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_SCHEDULER_STATE
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
CREATE TABLE `QRTZ_SCHEDULER_STATE` (
                                        `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
                                        `instance_name` varchar(200) NOT NULL COMMENT '实例名称',
                                        `last_checkin_time` bigint(13) NOT NULL COMMENT '上次检查时间',
                                        `checkin_interval` bigint(13) NOT NULL COMMENT '检查间隔时间',
                                        PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='调度器状态表';

-- ----------------------------
-- Records of QRTZ_SCHEDULER_STATE
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_SIMPLE_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPLE_TRIGGERS` (
                                        `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
                                        `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                        `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                        `repeat_count` bigint(7) NOT NULL COMMENT '重复的次数统计',
                                        `repeat_interval` bigint(12) NOT NULL COMMENT '重复的间隔时间',
                                        `times_triggered` bigint(10) NOT NULL COMMENT '已经触发的次数',
                                        PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                        CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `QRTZ_TRIGGERS` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='简单触发器的信息表';

-- ----------------------------
-- Records of QRTZ_SIMPLE_TRIGGERS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_SIMPROP_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPROP_TRIGGERS` (
                                         `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
                                         `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                         `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                         `str_prop_1` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
                                         `str_prop_2` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
                                         `str_prop_3` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
                                         `int_prop_1` int(11) DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
                                         `int_prop_2` int(11) DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
                                         `long_prop_1` bigint(20) DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
                                         `long_prop_2` bigint(20) DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
                                         `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
                                         `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
                                         `bool_prop_1` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
                                         `bool_prop_2` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
                                         PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                         CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `QRTZ_TRIGGERS` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='同步机制的行锁表';

-- ----------------------------
-- Records of QRTZ_SIMPROP_TRIGGERS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
CREATE TABLE `QRTZ_TRIGGERS` (
                                 `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
                                 `trigger_name` varchar(200) NOT NULL COMMENT '触发器的名字',
                                 `trigger_group` varchar(200) NOT NULL COMMENT '触发器所属组的名字',
                                 `job_name` varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
                                 `job_group` varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
                                 `description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
                                 `next_fire_time` bigint(13) DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
                                 `prev_fire_time` bigint(13) DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
                                 `priority` int(11) DEFAULT NULL COMMENT '优先级',
                                 `trigger_state` varchar(16) NOT NULL COMMENT '触发器状态',
                                 `trigger_type` varchar(8) NOT NULL COMMENT '触发器的类型',
                                 `start_time` bigint(13) NOT NULL COMMENT '开始时间',
                                 `end_time` bigint(13) DEFAULT NULL COMMENT '结束时间',
                                 `calendar_name` varchar(200) DEFAULT NULL COMMENT '日程表名称',
                                 `misfire_instr` smallint(2) DEFAULT NULL COMMENT '补偿执行的策略',
                                 `job_data` blob COMMENT '存放持久化job对象',
                                 PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                 KEY `sched_name` (`sched_name`,`job_name`,`job_group`),
                                 CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `QRTZ_JOB_DETAILS` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='触发器详细信息表';

-- ----------------------------
-- Records of QRTZ_TRIGGERS
-- ----------------------------

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
                              `config_id` int(5) NOT NULL AUTO_INCREMENT COMMENT '参数主键',
                              `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
                              `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
                              `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
                              `config_type` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
                              `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                              `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                              `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                              `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                              `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                              PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8 COMMENT='参数配置表';

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES ('1', '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', '2022-11-25 05:04:08', '', null, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` VALUES ('2', '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', '2022-11-25 05:04:08', '', null, '初始化密码 123456');
INSERT INTO `sys_config` VALUES ('3', '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', '2022-11-25 05:04:08', '', null, '深黑主题theme-dark，浅色主题theme-light，深蓝主题theme-blue');
INSERT INTO `sys_config` VALUES ('4', '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 'admin', '2022-11-25 05:04:08', '', null, '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES ('5', '用户管理-密码字符范围', 'sys.account.chrtype', '0', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '默认任意字符范围，0任意（密码可以输入任意字符），1数字（密码只能为0-9数字），2英文字母（密码只能为a-z和A-Z字母），3字母和数字（密码必须包含字母，数字）,4字母数字和特殊字符（目前支持的特殊字符包括：~!@#$%^&*()-=_+）');
INSERT INTO `sys_config` VALUES ('6', '用户管理-初始密码修改策略', 'sys.account.initPasswordModify', '0', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框');
INSERT INTO `sys_config` VALUES ('7', '用户管理-账号密码更新周期', 'sys.account.passwordValidateDays', '0', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框');
INSERT INTO `sys_config` VALUES ('8', '主框架页-菜单导航显示风格', 'sys.index.menuStyle', 'default', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '菜单导航显示风格（default为左侧导航菜单，topnav为顶部导航菜单）');
INSERT INTO `sys_config` VALUES ('9', '主框架页-是否开启页脚', 'sys.index.footer', 'true', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '是否开启底部页脚显示（true显示，false隐藏）');
INSERT INTO `sys_config` VALUES ('10', '主框架页-是否开启页签', 'sys.index.tagsView', 'true', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '是否开启菜单多页签显示（true显示，false隐藏）');

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
                            `dept_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '部门id',
                            `parent_id` bigint(20) DEFAULT '0' COMMENT '父部门id',
                            `ancestors` varchar(50) DEFAULT '' COMMENT '祖级列表',
                            `dept_name` varchar(30) DEFAULT '' COMMENT '部门名称',
                            `order_num` int(4) DEFAULT '0' COMMENT '显示顺序',
                            `leader` varchar(20) DEFAULT NULL COMMENT '负责人',
                            `phone` varchar(11) DEFAULT NULL COMMENT '联系电话',
                            `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
                            `status` char(1) DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
                            `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8 COMMENT='部门表';

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES ('100', '0', '0', '若依科技', '0', '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-11-25 05:03:51', '', null);
INSERT INTO `sys_dept` VALUES ('101', '100', '0,100', '深圳总公司', '1', '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-11-25 05:03:51', '', null);
INSERT INTO `sys_dept` VALUES ('102', '100', '0,100', '长沙分公司', '2', '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-11-25 05:03:51', '', null);
INSERT INTO `sys_dept` VALUES ('103', '101', '0,100,101', '研发部门', '1', '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-11-25 05:03:51', '', null);
INSERT INTO `sys_dept` VALUES ('104', '101', '0,100,101', '市场部门', '2', '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-11-25 05:03:51', '', null);
INSERT INTO `sys_dept` VALUES ('105', '101', '0,100,101', '测试部门', '3', '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-11-25 05:03:51', '', null);
INSERT INTO `sys_dept` VALUES ('106', '101', '0,100,101', '财务部门', '4', '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-11-25 05:03:51', '', null);
INSERT INTO `sys_dept` VALUES ('107', '101', '0,100,101', '运维部门', '5', '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-11-25 05:03:51', '', null);
INSERT INTO `sys_dept` VALUES ('108', '102', '0,100,102', '市场部门', '1', '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-11-25 05:03:51', '', null);
INSERT INTO `sys_dept` VALUES ('109', '102', '0,100,102', '财务部门', '2', '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-11-25 05:03:51', '', null);

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
                                 `dict_code` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典编码',
                                 `dict_sort` int(4) DEFAULT '0' COMMENT '字典排序',
                                 `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
                                 `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
                                 `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
                                 `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
                                 `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
                                 `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
                                 `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                 `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8 COMMENT='字典数据表';

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES ('1', '1', '男', '0', 'sys_user_sex', '', '', 'Y', '0', 'admin', '2022-11-25 05:04:06', '', null, '性别男');
INSERT INTO `sys_dict_data` VALUES ('2', '2', '女', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', '2022-11-25 05:04:06', '', null, '性别女');
INSERT INTO `sys_dict_data` VALUES ('3', '3', '未知', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', '2022-11-25 05:04:06', '', null, '性别未知');
INSERT INTO `sys_dict_data` VALUES ('4', '1', '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', '2022-11-25 05:04:06', '', null, '显示菜单');
INSERT INTO `sys_dict_data` VALUES ('5', '2', '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:06', '', null, '隐藏菜单');
INSERT INTO `sys_dict_data` VALUES ('6', '1', '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', '2022-11-25 05:04:06', '', null, '正常状态');
INSERT INTO `sys_dict_data` VALUES ('7', '2', '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '停用状态');
INSERT INTO `sys_dict_data` VALUES ('8', '1', '正常', '0', 'sys_job_status', '', 'primary', 'Y', '0', 'admin', '2022-11-25 05:04:07', '', null, '正常状态');
INSERT INTO `sys_dict_data` VALUES ('9', '2', '暂停', '1', 'sys_job_status', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '停用状态');
INSERT INTO `sys_dict_data` VALUES ('10', '1', '默认', 'DEFAULT', 'sys_job_group', '', '', 'Y', '0', 'admin', '2022-11-25 05:04:07', '', null, '默认分组');
INSERT INTO `sys_dict_data` VALUES ('11', '2', '系统', 'SYSTEM', 'sys_job_group', '', '', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '系统分组');
INSERT INTO `sys_dict_data` VALUES ('12', '1', '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', '2022-11-25 05:04:07', '', null, '系统默认是');
INSERT INTO `sys_dict_data` VALUES ('13', '2', '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '系统默认否');
INSERT INTO `sys_dict_data` VALUES ('14', '1', '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', '2022-11-25 05:04:07', '', null, '通知');
INSERT INTO `sys_dict_data` VALUES ('15', '2', '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '公告');
INSERT INTO `sys_dict_data` VALUES ('16', '1', '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', '2022-11-25 05:04:07', '', null, '正常状态');
INSERT INTO `sys_dict_data` VALUES ('17', '2', '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '关闭状态');
INSERT INTO `sys_dict_data` VALUES ('18', '99', '其他', '0', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '其他操作');
INSERT INTO `sys_dict_data` VALUES ('19', '1', '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '新增操作');
INSERT INTO `sys_dict_data` VALUES ('20', '2', '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '修改操作');
INSERT INTO `sys_dict_data` VALUES ('21', '3', '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '删除操作');
INSERT INTO `sys_dict_data` VALUES ('22', '4', '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '授权操作');
INSERT INTO `sys_dict_data` VALUES ('23', '5', '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '导出操作');
INSERT INTO `sys_dict_data` VALUES ('24', '6', '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '导入操作');
INSERT INTO `sys_dict_data` VALUES ('25', '7', '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '强退操作');
INSERT INTO `sys_dict_data` VALUES ('26', '8', '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '生成操作');
INSERT INTO `sys_dict_data` VALUES ('27', '9', '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '清空操作');
INSERT INTO `sys_dict_data` VALUES ('28', '1', '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '正常状态');
INSERT INTO `sys_dict_data` VALUES ('29', '2', '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '停用状态');

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
                                 `dict_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典主键',
                                 `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
                                 `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
                                 `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                 `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`dict_id`),
                                 UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8 COMMENT='字典类型表';

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO `sys_dict_type` VALUES ('1', '用户性别', 'sys_user_sex', '0', 'admin', '2022-11-25 05:04:05', '', null, '用户性别列表');
INSERT INTO `sys_dict_type` VALUES ('2', '菜单状态', 'sys_show_hide', '0', 'admin', '2022-11-25 05:04:05', '', null, '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES ('3', '系统开关', 'sys_normal_disable', '0', 'admin', '2022-11-25 05:04:05', '', null, '系统开关列表');
INSERT INTO `sys_dict_type` VALUES ('4', '任务状态', 'sys_job_status', '0', 'admin', '2022-11-25 05:04:05', '', null, '任务状态列表');
INSERT INTO `sys_dict_type` VALUES ('5', '任务分组', 'sys_job_group', '0', 'admin', '2022-11-25 05:04:05', '', null, '任务分组列表');
INSERT INTO `sys_dict_type` VALUES ('6', '系统是否', 'sys_yes_no', '0', 'admin', '2022-11-25 05:04:06', '', null, '系统是否列表');
INSERT INTO `sys_dict_type` VALUES ('7', '通知类型', 'sys_notice_type', '0', 'admin', '2022-11-25 05:04:06', '', null, '通知类型列表');
INSERT INTO `sys_dict_type` VALUES ('8', '通知状态', 'sys_notice_status', '0', 'admin', '2022-11-25 05:04:06', '', null, '通知状态列表');
INSERT INTO `sys_dict_type` VALUES ('9', '操作类型', 'sys_oper_type', '0', 'admin', '2022-11-25 05:04:06', '', null, '操作类型列表');
INSERT INTO `sys_dict_type` VALUES ('10', '系统状态', 'sys_common_status', '0', 'admin', '2022-11-25 05:04:06', '', null, '登录状态列表');

-- ----------------------------
-- Table structure for sys_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job` (
                           `job_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
                           `job_name` varchar(64) NOT NULL DEFAULT '' COMMENT '任务名称',
                           `job_group` varchar(64) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
                           `invoke_target` varchar(500) NOT NULL COMMENT '调用目标字符串',
                           `cron_expression` varchar(255) DEFAULT '' COMMENT 'cron执行表达式',
                           `misfire_policy` varchar(20) DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
                           `concurrent` char(1) DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
                           `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1暂停）',
                           `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                           `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                           `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                           `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                           `remark` varchar(500) DEFAULT '' COMMENT '备注信息',
                           PRIMARY KEY (`job_id`,`job_name`,`job_group`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8 COMMENT='定时任务调度表';

-- ----------------------------
-- Records of sys_job
-- ----------------------------
INSERT INTO `sys_job` VALUES ('1', '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '0/10 * * * * ?', '3', '1', '1', 'admin', '2022-11-25 05:04:10', '', null, '');
INSERT INTO `sys_job` VALUES ('2', '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')', '0/15 * * * * ?', '3', '1', '1', 'admin', '2022-11-25 05:04:10', '', null, '');
INSERT INTO `sys_job` VALUES ('3', '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)', '0/20 * * * * ?', '3', '1', '1', 'admin', '2022-11-25 05:04:10', '', null, '');

-- ----------------------------
-- Table structure for sys_job_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log` (
                               `job_log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
                               `job_name` varchar(64) NOT NULL COMMENT '任务名称',
                               `job_group` varchar(64) NOT NULL COMMENT '任务组名',
                               `invoke_target` varchar(500) NOT NULL COMMENT '调用目标字符串',
                               `job_message` varchar(500) DEFAULT NULL COMMENT '日志信息',
                               `status` char(1) DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
                               `exception_info` varchar(2000) DEFAULT '' COMMENT '异常信息',
                               `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                               PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='定时任务调度日志表';

-- ----------------------------
-- Records of sys_job_log
-- ----------------------------
INSERT INTO `sys_job_log` VALUES ('1', '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '系统默认（无参） 总共耗时：2毫秒', '0', '', '2022-11-25 05:08:23');

-- ----------------------------
-- Table structure for sys_logininfor
-- ----------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor` (
                                  `info_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '访问ID',
                                  `login_name` varchar(50) DEFAULT '' COMMENT '登录账号',
                                  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
                                  `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
                                  `browser` varchar(50) DEFAULT '' COMMENT '浏览器类型',
                                  `os` varchar(50) DEFAULT '' COMMENT '操作系统',
                                  `status` char(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
                                  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
                                  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
                                  PRIMARY KEY (`info_id`)
) ENGINE=InnoDB AUTO_INCREMENT=323 DEFAULT CHARSET=utf8 COMMENT='系统访问记录';

-- ----------------------------
-- Records of sys_logininfor
-- ----------------------------
INSERT INTO `sys_logininfor` VALUES ('100', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '1', '验证码错误', '2022-11-25 05:07:29');
INSERT INTO `sys_logininfor` VALUES ('101', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-25 05:07:53');
INSERT INTO `sys_logininfor` VALUES ('102', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '退出成功', '2022-11-25 05:09:43');
INSERT INTO `sys_logininfor` VALUES ('103', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-25 05:21:10');
INSERT INTO `sys_logininfor` VALUES ('104', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '退出成功', '2022-11-25 05:23:15');
INSERT INTO `sys_logininfor` VALUES ('105', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '1', '验证码错误', '2022-11-25 06:16:31');
INSERT INTO `sys_logininfor` VALUES ('106', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-25 06:16:40');
INSERT INTO `sys_logininfor` VALUES ('107', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-25 06:39:08');
INSERT INTO `sys_logininfor` VALUES ('108', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-25 06:44:23');
INSERT INTO `sys_logininfor` VALUES ('109', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-25 06:51:09');
INSERT INTO `sys_logininfor` VALUES ('110', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-25 10:12:07');
INSERT INTO `sys_logininfor` VALUES ('111', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 04:53:02');
INSERT INTO `sys_logininfor` VALUES ('112', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 05:00:47');
INSERT INTO `sys_logininfor` VALUES ('113', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 05:06:27');
INSERT INTO `sys_logininfor` VALUES ('114', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 05:16:46');
INSERT INTO `sys_logininfor` VALUES ('115', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 06:17:17');
INSERT INTO `sys_logininfor` VALUES ('116', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 06:27:33');
INSERT INTO `sys_logininfor` VALUES ('117', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 06:34:26');
INSERT INTO `sys_logininfor` VALUES ('118', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 09:53:04');
INSERT INTO `sys_logininfor` VALUES ('119', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 10:02:49');
INSERT INTO `sys_logininfor` VALUES ('120', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 10:04:39');
INSERT INTO `sys_logininfor` VALUES ('121', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 10:11:37');
INSERT INTO `sys_logininfor` VALUES ('122', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 12:40:46');
INSERT INTO `sys_logininfor` VALUES ('123', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-29 13:22:58');
INSERT INTO `sys_logininfor` VALUES ('124', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 02:01:21');
INSERT INTO `sys_logininfor` VALUES ('125', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 02:09:58');
INSERT INTO `sys_logininfor` VALUES ('126', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 02:11:41');
INSERT INTO `sys_logininfor` VALUES ('127', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 02:24:33');
INSERT INTO `sys_logininfor` VALUES ('128', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 02:31:40');
INSERT INTO `sys_logininfor` VALUES ('129', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 02:33:32');
INSERT INTO `sys_logininfor` VALUES ('130', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 02:36:10');
INSERT INTO `sys_logininfor` VALUES ('131', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 02:45:14');
INSERT INTO `sys_logininfor` VALUES ('132', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 03:11:08');
INSERT INTO `sys_logininfor` VALUES ('133', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 03:14:02');
INSERT INTO `sys_logininfor` VALUES ('134', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 03:15:53');
INSERT INTO `sys_logininfor` VALUES ('135', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 03:18:22');
INSERT INTO `sys_logininfor` VALUES ('136', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 04:51:34');
INSERT INTO `sys_logininfor` VALUES ('137', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 04:54:36');
INSERT INTO `sys_logininfor` VALUES ('138', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 04:56:21');
INSERT INTO `sys_logininfor` VALUES ('139', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 06:05:30');
INSERT INTO `sys_logininfor` VALUES ('140', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 06:19:22');
INSERT INTO `sys_logininfor` VALUES ('141', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 06:28:48');
INSERT INTO `sys_logininfor` VALUES ('142', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 06:32:54');
INSERT INTO `sys_logininfor` VALUES ('143', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 06:47:23');
INSERT INTO `sys_logininfor` VALUES ('144', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 07:06:37');
INSERT INTO `sys_logininfor` VALUES ('145', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 07:12:55');
INSERT INTO `sys_logininfor` VALUES ('146', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 09:35:33');
INSERT INTO `sys_logininfor` VALUES ('147', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 09:37:34');
INSERT INTO `sys_logininfor` VALUES ('148', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 09:40:20');
INSERT INTO `sys_logininfor` VALUES ('149', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 09:46:36');
INSERT INTO `sys_logininfor` VALUES ('150', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 09:49:03');
INSERT INTO `sys_logininfor` VALUES ('151', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 09:50:17');
INSERT INTO `sys_logininfor` VALUES ('152', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 09:51:39');
INSERT INTO `sys_logininfor` VALUES ('153', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-11-30 09:53:03');
INSERT INTO `sys_logininfor` VALUES ('154', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 03:23:46');
INSERT INTO `sys_logininfor` VALUES ('155', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 03:26:48');
INSERT INTO `sys_logininfor` VALUES ('156', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 03:31:48');
INSERT INTO `sys_logininfor` VALUES ('157', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 03:36:19');
INSERT INTO `sys_logininfor` VALUES ('158', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 03:58:22');
INSERT INTO `sys_logininfor` VALUES ('159', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 04:04:17');
INSERT INTO `sys_logininfor` VALUES ('160', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 04:10:04');
INSERT INTO `sys_logininfor` VALUES ('161', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 05:19:26');
INSERT INTO `sys_logininfor` VALUES ('162', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 05:59:14');
INSERT INTO `sys_logininfor` VALUES ('163', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 06:11:17');
INSERT INTO `sys_logininfor` VALUES ('164', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 06:19:43');
INSERT INTO `sys_logininfor` VALUES ('165', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-01 06:54:55');
INSERT INTO `sys_logininfor` VALUES ('166', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 11:24:29');
INSERT INTO `sys_logininfor` VALUES ('167', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 11:30:22');
INSERT INTO `sys_logininfor` VALUES ('168', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 11:33:52');
INSERT INTO `sys_logininfor` VALUES ('169', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 11:35:17');
INSERT INTO `sys_logininfor` VALUES ('170', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 11:37:30');
INSERT INTO `sys_logininfor` VALUES ('171', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 11:42:16');
INSERT INTO `sys_logininfor` VALUES ('172', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 11:45:08');
INSERT INTO `sys_logininfor` VALUES ('173', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 11:48:42');
INSERT INTO `sys_logininfor` VALUES ('174', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 11:53:27');
INSERT INTO `sys_logininfor` VALUES ('175', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 11:56:07');
INSERT INTO `sys_logininfor` VALUES ('176', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 11:58:48');
INSERT INTO `sys_logininfor` VALUES ('177', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 12:03:20');
INSERT INTO `sys_logininfor` VALUES ('178', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 12:09:24');
INSERT INTO `sys_logininfor` VALUES ('179', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 12:14:59');
INSERT INTO `sys_logininfor` VALUES ('180', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 12:16:20');
INSERT INTO `sys_logininfor` VALUES ('181', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-04 12:17:33');
INSERT INTO `sys_logininfor` VALUES ('182', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-05 08:53:20');
INSERT INTO `sys_logininfor` VALUES ('183', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-05 09:17:13');
INSERT INTO `sys_logininfor` VALUES ('184', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-05 10:07:45');
INSERT INTO `sys_logininfor` VALUES ('185', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-05 10:11:09');
INSERT INTO `sys_logininfor` VALUES ('186', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-05 10:12:48');
INSERT INTO `sys_logininfor` VALUES ('187', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-06 12:43:51');
INSERT INTO `sys_logininfor` VALUES ('188', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-06 12:45:54');
INSERT INTO `sys_logininfor` VALUES ('189', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-06 12:49:33');
INSERT INTO `sys_logininfor` VALUES ('190', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-06 12:50:55');
INSERT INTO `sys_logininfor` VALUES ('191', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-06 12:52:21');
INSERT INTO `sys_logininfor` VALUES ('192', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-06 12:53:36');
INSERT INTO `sys_logininfor` VALUES ('193', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-06 12:56:03');
INSERT INTO `sys_logininfor` VALUES ('194', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-08 06:56:20');
INSERT INTO `sys_logininfor` VALUES ('195', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-08 06:58:10');
INSERT INTO `sys_logininfor` VALUES ('196', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 02:59:48');
INSERT INTO `sys_logininfor` VALUES ('197', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 03:40:39');
INSERT INTO `sys_logininfor` VALUES ('198', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 03:46:00');
INSERT INTO `sys_logininfor` VALUES ('199', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 03:47:24');
INSERT INTO `sys_logininfor` VALUES ('200', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 03:49:38');
INSERT INTO `sys_logininfor` VALUES ('201', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 08:19:20');
INSERT INTO `sys_logininfor` VALUES ('202', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 08:25:09');
INSERT INTO `sys_logininfor` VALUES ('203', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 08:28:03');
INSERT INTO `sys_logininfor` VALUES ('204', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 08:33:10');
INSERT INTO `sys_logininfor` VALUES ('205', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 08:37:05');
INSERT INTO `sys_logininfor` VALUES ('206', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 08:46:47');
INSERT INTO `sys_logininfor` VALUES ('207', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 08:58:01');
INSERT INTO `sys_logininfor` VALUES ('208', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 09:00:10');
INSERT INTO `sys_logininfor` VALUES ('209', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 09:02:04');
INSERT INTO `sys_logininfor` VALUES ('210', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 09:04:06');
INSERT INTO `sys_logininfor` VALUES ('211', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 09:06:50');
INSERT INTO `sys_logininfor` VALUES ('212', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 09:10:20');
INSERT INTO `sys_logininfor` VALUES ('213', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 09:14:51');
INSERT INTO `sys_logininfor` VALUES ('214', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 09:17:37');
INSERT INTO `sys_logininfor` VALUES ('215', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 09:21:24');
INSERT INTO `sys_logininfor` VALUES ('216', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 09:24:15');
INSERT INTO `sys_logininfor` VALUES ('217', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 09:52:10');
INSERT INTO `sys_logininfor` VALUES ('218', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 10:01:08');
INSERT INTO `sys_logininfor` VALUES ('219', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 10:04:19');
INSERT INTO `sys_logininfor` VALUES ('220', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-12 10:06:12');
INSERT INTO `sys_logininfor` VALUES ('221', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 01:58:49');
INSERT INTO `sys_logininfor` VALUES ('222', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 03:17:34');
INSERT INTO `sys_logininfor` VALUES ('223', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 03:35:23');
INSERT INTO `sys_logininfor` VALUES ('224', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 03:40:58');
INSERT INTO `sys_logininfor` VALUES ('225', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 03:43:12');
INSERT INTO `sys_logininfor` VALUES ('226', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 03:46:46');
INSERT INTO `sys_logininfor` VALUES ('227', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 03:53:09');
INSERT INTO `sys_logininfor` VALUES ('228', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 03:55:33');
INSERT INTO `sys_logininfor` VALUES ('229', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 04:01:44');
INSERT INTO `sys_logininfor` VALUES ('230', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 04:56:07');
INSERT INTO `sys_logininfor` VALUES ('231', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 05:09:20');
INSERT INTO `sys_logininfor` VALUES ('232', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 05:11:51');
INSERT INTO `sys_logininfor` VALUES ('233', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 05:13:11');
INSERT INTO `sys_logininfor` VALUES ('234', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 05:18:22');
INSERT INTO `sys_logininfor` VALUES ('235', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 05:20:54');
INSERT INTO `sys_logininfor` VALUES ('236', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 05:23:54');
INSERT INTO `sys_logininfor` VALUES ('237', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 05:29:19');
INSERT INTO `sys_logininfor` VALUES ('238', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:00:57');
INSERT INTO `sys_logininfor` VALUES ('239', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:04:07');
INSERT INTO `sys_logininfor` VALUES ('240', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:06:25');
INSERT INTO `sys_logininfor` VALUES ('241', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:08:09');
INSERT INTO `sys_logininfor` VALUES ('242', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:13:22');
INSERT INTO `sys_logininfor` VALUES ('243', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:14:52');
INSERT INTO `sys_logininfor` VALUES ('244', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:20:01');
INSERT INTO `sys_logininfor` VALUES ('245', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:21:42');
INSERT INTO `sys_logininfor` VALUES ('246', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:24:11');
INSERT INTO `sys_logininfor` VALUES ('247', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:39:42');
INSERT INTO `sys_logininfor` VALUES ('248', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:48:33');
INSERT INTO `sys_logininfor` VALUES ('249', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 06:52:56');
INSERT INTO `sys_logininfor` VALUES ('250', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 07:49:11');
INSERT INTO `sys_logininfor` VALUES ('251', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 08:26:58');
INSERT INTO `sys_logininfor` VALUES ('252', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 08:40:24');
INSERT INTO `sys_logininfor` VALUES ('253', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 08:58:14');
INSERT INTO `sys_logininfor` VALUES ('254', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-13 10:34:33');
INSERT INTO `sys_logininfor` VALUES ('255', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-14 03:21:06');
INSERT INTO `sys_logininfor` VALUES ('256', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 01:58:35');
INSERT INTO `sys_logininfor` VALUES ('257', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 02:16:00');
INSERT INTO `sys_logininfor` VALUES ('258', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 05:42:22');
INSERT INTO `sys_logininfor` VALUES ('259', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 08:16:03');
INSERT INTO `sys_logininfor` VALUES ('260', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 08:19:09');
INSERT INTO `sys_logininfor` VALUES ('261', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 08:23:42');
INSERT INTO `sys_logininfor` VALUES ('262', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 08:26:32');
INSERT INTO `sys_logininfor` VALUES ('263', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 08:33:36');
INSERT INTO `sys_logininfor` VALUES ('264', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 08:48:16');
INSERT INTO `sys_logininfor` VALUES ('265', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 08:51:39');
INSERT INTO `sys_logininfor` VALUES ('266', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 08:53:38');
INSERT INTO `sys_logininfor` VALUES ('267', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 08:58:36');
INSERT INTO `sys_logininfor` VALUES ('268', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:07:09');
INSERT INTO `sys_logininfor` VALUES ('269', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:09:53');
INSERT INTO `sys_logininfor` VALUES ('270', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:28:21');
INSERT INTO `sys_logininfor` VALUES ('271', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:30:25');
INSERT INTO `sys_logininfor` VALUES ('272', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:34:11');
INSERT INTO `sys_logininfor` VALUES ('273', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:36:12');
INSERT INTO `sys_logininfor` VALUES ('274', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:46:02');
INSERT INTO `sys_logininfor` VALUES ('275', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:48:14');
INSERT INTO `sys_logininfor` VALUES ('276', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:52:46');
INSERT INTO `sys_logininfor` VALUES ('277', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:55:32');
INSERT INTO `sys_logininfor` VALUES ('278', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:56:58');
INSERT INTO `sys_logininfor` VALUES ('279', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 09:59:22');
INSERT INTO `sys_logininfor` VALUES ('280', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 10:01:07');
INSERT INTO `sys_logininfor` VALUES ('281', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 10:03:49');
INSERT INTO `sys_logininfor` VALUES ('282', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 10:07:10');
INSERT INTO `sys_logininfor` VALUES ('283', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 10:09:49');
INSERT INTO `sys_logininfor` VALUES ('284', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 10:11:01');
INSERT INTO `sys_logininfor` VALUES ('285', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 10:12:35');
INSERT INTO `sys_logininfor` VALUES ('286', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 10:17:08');
INSERT INTO `sys_logininfor` VALUES ('287', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 10:19:21');
INSERT INTO `sys_logininfor` VALUES ('288', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 10:21:35');
INSERT INTO `sys_logininfor` VALUES ('289', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 11:05:23');
INSERT INTO `sys_logininfor` VALUES ('290', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-15 11:09:24');
INSERT INTO `sys_logininfor` VALUES ('291', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-19 02:13:20');
INSERT INTO `sys_logininfor` VALUES ('292', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-19 12:47:13');
INSERT INTO `sys_logininfor` VALUES ('293', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-19 12:52:26');
INSERT INTO `sys_logininfor` VALUES ('294', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-19 12:54:25');
INSERT INTO `sys_logininfor` VALUES ('295', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-19 13:01:36');
INSERT INTO `sys_logininfor` VALUES ('296', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-19 13:05:10');
INSERT INTO `sys_logininfor` VALUES ('297', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-19 13:08:56');
INSERT INTO `sys_logininfor` VALUES ('298', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-19 13:10:32');
INSERT INTO `sys_logininfor` VALUES ('299', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-19 13:11:35');
INSERT INTO `sys_logininfor` VALUES ('300', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 01:59:13');
INSERT INTO `sys_logininfor` VALUES ('301', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 02:01:20');
INSERT INTO `sys_logininfor` VALUES ('302', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 02:29:56');
INSERT INTO `sys_logininfor` VALUES ('303', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 02:36:21');
INSERT INTO `sys_logininfor` VALUES ('304', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 02:40:10');
INSERT INTO `sys_logininfor` VALUES ('305', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 02:41:37');
INSERT INTO `sys_logininfor` VALUES ('306', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 02:44:07');
INSERT INTO `sys_logininfor` VALUES ('307', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 03:19:55');
INSERT INTO `sys_logininfor` VALUES ('308', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 03:32:26');
INSERT INTO `sys_logininfor` VALUES ('309', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 06:21:25');
INSERT INTO `sys_logininfor` VALUES ('310', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 06:24:01');
INSERT INTO `sys_logininfor` VALUES ('311', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 06:26:26');
INSERT INTO `sys_logininfor` VALUES ('312', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 06:27:33');
INSERT INTO `sys_logininfor` VALUES ('313', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 06:30:28');
INSERT INTO `sys_logininfor` VALUES ('314', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 06:35:25');
INSERT INTO `sys_logininfor` VALUES ('315', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 06:38:28');
INSERT INTO `sys_logininfor` VALUES ('316', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 06:46:18');
INSERT INTO `sys_logininfor` VALUES ('317', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 06:47:46');
INSERT INTO `sys_logininfor` VALUES ('318', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 06:55:47');
INSERT INTO `sys_logininfor` VALUES ('319', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 07:00:05');
INSERT INTO `sys_logininfor` VALUES ('320', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 07:01:52');
INSERT INTO `sys_logininfor` VALUES ('321', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 07:20:05');
INSERT INTO `sys_logininfor` VALUES ('322', 'admin', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', '0', '登录成功', '2022-12-20 07:24:09');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
                            `menu_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
                            `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
                            `parent_id` bigint(20) DEFAULT '0' COMMENT '父菜单ID',
                            `order_num` int(4) DEFAULT '0' COMMENT '显示顺序',
                            `url` varchar(200) DEFAULT '#' COMMENT '请求地址',
                            `target` varchar(20) DEFAULT '' COMMENT '打开方式（menuItem页签 menuBlank新窗口）',
                            `menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
                            `visible` char(1) DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
                            `is_refresh` char(1) DEFAULT '1' COMMENT '是否刷新（0刷新 1不刷新）',
                            `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
                            `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) DEFAULT '' COMMENT '备注',
                            PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2008 DEFAULT CHARSET=utf8 COMMENT='菜单权限表';

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES ('1', '系统管理', '0', '1', '#', 'menuItem', 'M', '1', '1', '', 'fa fa-gear', 'admin', '2022-11-29 10:10:28', 'admin', '2022-12-12 09:52:29', '系统管理目录');
INSERT INTO `sys_menu` VALUES ('2', '系统监控', '0', '2', '#', 'menuItem', 'M', '1', '1', '', 'fa fa-video-camera', 'admin', '2022-11-29 10:10:29', 'admin', '2022-12-12 09:24:32', '系统监控目录');
INSERT INTO `sys_menu` VALUES ('3', '系统工具', '0', '3', '#', 'menuItem', 'M', '1', '1', '', 'fa fa-bars', 'admin', '2022-11-29 10:10:29', 'admin', '2022-12-12 09:24:39', '系统工具目录');
INSERT INTO `sys_menu` VALUES ('4', '若依官网', '0', '4', 'http://ruoyi.vip', 'menuBlank', 'C', '1', '1', '', 'fa fa-location-arrow', 'admin', '2022-11-29 10:10:29', 'admin', '2022-12-12 09:24:48', '若依官网地址');
INSERT INTO `sys_menu` VALUES ('5', '数据配置管理', '0', '5', '#', '', 'M', '0', '1', '', 'fa fa-gear', 'admin', '2022-11-29 12:15:43', '', null, '数据配置管理');
INSERT INTO `sys_menu` VALUES ('100', '用户管理', '1', '1', '/system/user', '', 'C', '0', '1', 'system:user:view', 'fa fa-user-o', 'admin', '2022-11-29 10:10:29', '', null, '用户管理菜单');
INSERT INTO `sys_menu` VALUES ('101', '角色管理', '1', '2', '/system/role', '', 'C', '0', '1', 'system:role:view', 'fa fa-user-secret', 'admin', '2022-11-29 10:10:29', '', null, '角色管理菜单');
INSERT INTO `sys_menu` VALUES ('102', '菜单管理', '1', '3', '/system/menu', '', 'C', '0', '1', 'system:menu:view', 'fa fa-th-list', 'admin', '2022-11-29 10:10:29', '', null, '菜单管理菜单');
INSERT INTO `sys_menu` VALUES ('103', '部门管理', '1', '4', '/system/dept', '', 'C', '0', '1', 'system:dept:view', 'fa fa-outdent', 'admin', '2022-11-29 10:10:29', '', null, '部门管理菜单');
INSERT INTO `sys_menu` VALUES ('104', '岗位管理', '1', '5', '/system/post', '', 'C', '0', '1', 'system:post:view', 'fa fa-address-card-o', 'admin', '2022-11-29 10:10:29', '', null, '岗位管理菜单');
INSERT INTO `sys_menu` VALUES ('105', '字典管理', '1', '6', '/system/dict', '', 'C', '0', '1', 'system:dict:view', 'fa fa-bookmark-o', 'admin', '2022-11-29 10:10:29', '', null, '字典管理菜单');
INSERT INTO `sys_menu` VALUES ('106', '参数设置', '1', '7', '/system/config', '', 'C', '0', '1', 'system:config:view', 'fa fa-sun-o', 'admin', '2022-11-29 10:10:29', '', null, '参数设置菜单');
INSERT INTO `sys_menu` VALUES ('107', '通知公告', '1', '8', '/system/notice', '', 'C', '0', '1', 'system:notice:view', 'fa fa-bullhorn', 'admin', '2022-11-29 10:10:29', '', null, '通知公告菜单');
INSERT INTO `sys_menu` VALUES ('108', '日志管理', '1', '9', '#', '', 'M', '0', '1', '', 'fa fa-pencil-square-o', 'admin', '2022-11-29 10:10:29', '', null, '日志管理菜单');
INSERT INTO `sys_menu` VALUES ('109', '在线用户', '2', '1', '/monitor/online', '', 'C', '0', '1', 'monitor:online:view', 'fa fa-user-circle', 'admin', '2022-11-29 10:10:29', '', null, '在线用户菜单');
INSERT INTO `sys_menu` VALUES ('110', '定时任务', '2', '2', '/monitor/job', '', 'C', '0', '1', 'monitor:job:view', 'fa fa-tasks', 'admin', '2022-11-29 10:10:29', '', null, '定时任务菜单');
INSERT INTO `sys_menu` VALUES ('111', '数据监控', '2', '3', '/monitor/data', '', 'C', '0', '1', 'monitor:data:view', 'fa fa-bug', 'admin', '2022-11-29 10:10:30', '', null, '数据监控菜单');
INSERT INTO `sys_menu` VALUES ('112', '服务监控', '2', '4', '/monitor/server', '', 'C', '0', '1', 'monitor:server:view', 'fa fa-server', 'admin', '2022-11-29 10:10:30', '', null, '服务监控菜单');
INSERT INTO `sys_menu` VALUES ('113', '缓存监控', '2', '5', '/monitor/cache', '', 'C', '0', '1', 'monitor:cache:view', 'fa fa-cube', 'admin', '2022-11-29 10:10:30', '', null, '缓存监控菜单');
INSERT INTO `sys_menu` VALUES ('114', '表单构建', '3', '1', '/tool/build', '', 'C', '0', '1', 'tool:build:view', 'fa fa-wpforms', 'admin', '2022-11-29 10:10:30', '', null, '表单构建菜单');
INSERT INTO `sys_menu` VALUES ('115', '代码生成', '3', '2', '/tool/gen', '', 'C', '0', '1', 'tool:gen:view', 'fa fa-code', 'admin', '2022-11-29 10:10:30', '', null, '代码生成菜单');
INSERT INTO `sys_menu` VALUES ('116', '系统接口', '3', '3', '/tool/swagger', '', 'C', '0', '1', 'tool:swagger:view', 'fa fa-gg', 'admin', '2022-11-29 10:10:30', '', null, '系统接口菜单');
INSERT INTO `sys_menu` VALUES ('117', '数据配置', '5', '1', '/system/dbconfig', 'menuItem', 'C', '0', '1', 'system:dbconfig:view', 'fa fa-gg', 'admin', '2022-11-29 12:15:53', 'admin', '2022-11-30 07:17:43', '数据配置');
INSERT INTO `sys_menu` VALUES ('500', '操作日志', '108', '1', '/monitor/operlog', '', 'C', '0', '1', 'monitor:operlog:view', 'fa fa-address-book', 'admin', '2022-11-29 10:10:30', '', null, '操作日志菜单');
INSERT INTO `sys_menu` VALUES ('501', '登录日志', '108', '2', '/monitor/logininfor', '', 'C', '0', '1', 'monitor:logininfor:view', 'fa fa-file-image-o', 'admin', '2022-11-29 10:10:30', '', null, '登录日志菜单');
INSERT INTO `sys_menu` VALUES ('1000', '用户查询', '100', '1', '#', '', 'F', '0', '1', 'system:user:list', '#', 'admin', '2022-11-29 10:10:30', '', null, '');
INSERT INTO `sys_menu` VALUES ('1001', '用户新增', '100', '2', '#', '', 'F', '0', '1', 'system:user:add', '#', 'admin', '2022-11-29 10:10:30', '', null, '');
INSERT INTO `sys_menu` VALUES ('1002', '用户修改', '100', '3', '#', '', 'F', '0', '1', 'system:user:edit', '#', 'admin', '2022-11-29 10:10:30', '', null, '');
INSERT INTO `sys_menu` VALUES ('1003', '用户删除', '100', '4', '#', '', 'F', '0', '1', 'system:user:remove', '#', 'admin', '2022-11-29 10:10:30', '', null, '');
INSERT INTO `sys_menu` VALUES ('1004', '用户导出', '100', '5', '#', '', 'F', '0', '1', 'system:user:export', '#', 'admin', '2022-11-29 10:10:30', '', null, '');
INSERT INTO `sys_menu` VALUES ('1005', '用户导入', '100', '6', '#', '', 'F', '0', '1', 'system:user:import', '#', 'admin', '2022-11-29 10:10:30', '', null, '');
INSERT INTO `sys_menu` VALUES ('1006', '重置密码', '100', '7', '#', '', 'F', '0', '1', 'system:user:resetPwd', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1007', '角色查询', '101', '1', '#', '', 'F', '0', '1', 'system:role:list', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1008', '角色新增', '101', '2', '#', '', 'F', '0', '1', 'system:role:add', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1009', '角色修改', '101', '3', '#', '', 'F', '0', '1', 'system:role:edit', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1010', '角色删除', '101', '4', '#', '', 'F', '0', '1', 'system:role:remove', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1011', '角色导出', '101', '5', '#', '', 'F', '0', '1', 'system:role:export', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1012', '菜单查询', '102', '1', '#', '', 'F', '0', '1', 'system:menu:list', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1013', '菜单新增', '102', '2', '#', '', 'F', '0', '1', 'system:menu:add', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1014', '菜单修改', '102', '3', '#', '', 'F', '0', '1', 'system:menu:edit', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1015', '菜单删除', '102', '4', '#', '', 'F', '0', '1', 'system:menu:remove', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1016', '部门查询', '103', '1', '#', '', 'F', '0', '1', 'system:dept:list', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1017', '部门新增', '103', '2', '#', '', 'F', '0', '1', 'system:dept:add', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1018', '部门修改', '103', '3', '#', '', 'F', '0', '1', 'system:dept:edit', '#', 'admin', '2022-11-29 10:10:31', '', null, '');
INSERT INTO `sys_menu` VALUES ('1019', '部门删除', '103', '4', '#', '', 'F', '0', '1', 'system:dept:remove', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1020', '岗位查询', '104', '1', '#', '', 'F', '0', '1', 'system:post:list', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1021', '岗位新增', '104', '2', '#', '', 'F', '0', '1', 'system:post:add', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1022', '岗位修改', '104', '3', '#', '', 'F', '0', '1', 'system:post:edit', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1023', '岗位删除', '104', '4', '#', '', 'F', '0', '1', 'system:post:remove', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1024', '岗位导出', '104', '5', '#', '', 'F', '0', '1', 'system:post:export', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1025', '字典查询', '105', '1', '#', '', 'F', '0', '1', 'system:dict:list', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1026', '字典新增', '105', '2', '#', '', 'F', '0', '1', 'system:dict:add', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1027', '字典修改', '105', '3', '#', '', 'F', '0', '1', 'system:dict:edit', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1028', '字典删除', '105', '4', '#', '', 'F', '0', '1', 'system:dict:remove', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1029', '字典导出', '105', '5', '#', '', 'F', '0', '1', 'system:dict:export', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1030', '参数查询', '106', '1', '#', '', 'F', '0', '1', 'system:config:list', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1031', '参数新增', '106', '2', '#', '', 'F', '0', '1', 'system:config:add', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1032', '参数修改', '106', '3', '#', '', 'F', '0', '1', 'system:config:edit', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1033', '参数删除', '106', '4', '#', '', 'F', '0', '1', 'system:config:remove', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1034', '参数导出', '106', '5', '#', '', 'F', '0', '1', 'system:config:export', '#', 'admin', '2022-11-29 10:10:32', '', null, '');
INSERT INTO `sys_menu` VALUES ('1035', '公告查询', '107', '1', '#', '', 'F', '0', '1', 'system:notice:list', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1036', '公告新增', '107', '2', '#', '', 'F', '0', '1', 'system:notice:add', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1037', '公告修改', '107', '3', '#', '', 'F', '0', '1', 'system:notice:edit', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1038', '公告删除', '107', '4', '#', '', 'F', '0', '1', 'system:notice:remove', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1039', '操作查询', '500', '1', '#', '', 'F', '0', '1', 'monitor:operlog:list', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1040', '操作删除', '500', '2', '#', '', 'F', '0', '1', 'monitor:operlog:remove', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1041', '详细信息', '500', '3', '#', '', 'F', '0', '1', 'monitor:operlog:detail', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1042', '日志导出', '500', '4', '#', '', 'F', '0', '1', 'monitor:operlog:export', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1043', '登录查询', '501', '1', '#', '', 'F', '0', '1', 'monitor:logininfor:list', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1044', '登录删除', '501', '2', '#', '', 'F', '0', '1', 'monitor:logininfor:remove', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1045', '日志导出', '501', '3', '#', '', 'F', '0', '1', 'monitor:logininfor:export', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1046', '账户解锁', '501', '4', '#', '', 'F', '0', '1', 'monitor:logininfor:unlock', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1047', '在线查询', '109', '1', '#', '', 'F', '0', '1', 'monitor:online:list', '#', 'admin', '2022-11-29 10:10:33', '', null, '');
INSERT INTO `sys_menu` VALUES ('1048', '批量强退', '109', '2', '#', '', 'F', '0', '1', 'monitor:online:batchForceLogout', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1049', '单条强退', '109', '3', '#', '', 'F', '0', '1', 'monitor:online:forceLogout', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1050', '任务查询', '110', '1', '#', '', 'F', '0', '1', 'monitor:job:list', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1051', '任务新增', '110', '2', '#', '', 'F', '0', '1', 'monitor:job:add', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1052', '任务修改', '110', '3', '#', '', 'F', '0', '1', 'monitor:job:edit', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1053', '任务删除', '110', '4', '#', '', 'F', '0', '1', 'monitor:job:remove', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1054', '状态修改', '110', '5', '#', '', 'F', '0', '1', 'monitor:job:changeStatus', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1055', '任务详细', '110', '6', '#', '', 'F', '0', '1', 'monitor:job:detail', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1056', '任务导出', '110', '7', '#', '', 'F', '0', '1', 'monitor:job:export', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1057', '生成查询', '115', '1', '#', '', 'F', '0', '1', 'tool:gen:list', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1058', '生成修改', '115', '2', '#', '', 'F', '0', '1', 'tool:gen:edit', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1059', '生成删除', '115', '3', '#', '', 'F', '0', '1', 'tool:gen:remove', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1060', '预览代码', '115', '4', '#', '', 'F', '0', '1', 'tool:gen:preview', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1061', '生成代码', '115', '5', '#', '', 'F', '0', '1', 'tool:gen:code', '#', 'admin', '2022-11-29 10:10:34', '', null, '');
INSERT INTO `sys_menu` VALUES ('1062', '查询', '117', '1', '#', '', 'F', '0', '1', 'system:dbconfig:list', '#', 'admin', '2022-11-29 13:04:30', '', null, '');
INSERT INTO `sys_menu` VALUES ('1063', '配置新增', '117', '2', '#', 'menuItem', 'F', '0', '1', 'system:dbconfig:add', '#', 'admin', '2022-11-29 13:04:30', 'admin', '2022-11-30 02:16:00', '');
INSERT INTO `sys_menu` VALUES ('1064', '修改', '117', '3', '#', '', 'F', '0', '1', 'system:dbconfig:edit', '#', 'admin', '2022-11-29 13:04:30', '', null, '');
INSERT INTO `sys_menu` VALUES ('1065', '删除', '117', '4', '#', '', 'F', '0', '1', 'system:dbconfig:remove', '#', 'admin', '2022-11-29 13:04:30', '', null, '');
INSERT INTO `sys_menu` VALUES ('1066', '导出', '117', '5', '#', '', 'F', '0', '1', 'system:dbconfig:export', '#', 'admin', '2022-11-29 13:04:30', '', null, '');
INSERT INTO `sys_menu` VALUES ('2000', '任务配置', '5', '2', '/system/jobconfig', 'menuItem', 'C', '0', '1', 'system:jobconfig:view', 'fa fa-archive', 'admin', '2022-11-30 07:16:51', 'admin', '2022-11-30 07:19:45', '');
INSERT INTO `sys_menu` VALUES ('2001', '查询', '2000', '1', 'system:dbconfig:list', 'menuItem', 'F', '0', '1', 'system:jobconfig:list', '#', 'admin', '2022-11-30 07:18:54', 'admin', '2022-11-30 07:23:02', '');
INSERT INTO `sys_menu` VALUES ('2002', '新增', '2000', '2', '#', 'menuItem', 'F', '0', '1', 'system:dbconfig:add', '#', 'admin', '2022-11-30 07:22:19', '', null, '');
INSERT INTO `sys_menu` VALUES ('2003', '修改', '2000', '3', '#', 'menuItem', 'F', '0', '1', 'system:dbconfig:edit', '#', 'admin', '2022-11-30 07:22:51', '', null, '');
INSERT INTO `sys_menu` VALUES ('2004', '删除', '2000', '4', '#', 'menuItem', 'F', '0', '1', 'system:dbconfig:remove', '#', 'admin', '2022-11-30 07:23:53', '', null, '');
INSERT INTO `sys_menu` VALUES ('2005', '运行实例', '5', '3', '/system/jobInstance', 'menuItem', 'C', '0', '1', '/system/jobInstance', 'fa fa-asterisk', 'admin', '2022-12-12 03:41:51', 'admin', '2022-12-12 03:50:16', '');
INSERT INTO `sys_menu` VALUES ('2006', '查看详情', '2005', '1', '#', 'menuItem', 'F', '0', '1', 'system:jobInstance:detail', '#', 'admin', '2022-12-12 03:42:42', 'admin', '2022-12-19 12:50:41', '');
INSERT INTO `sys_menu` VALUES ('2007', '查看差异', '2005', '2', '#', 'menuItem', 'F', '0', '1', 'system:jobInstance:diffDetail', '#', 'admin', '2022-12-19 12:50:09', 'admin', '2022-12-19 12:51:15', '');

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
                              `notice_id` int(4) NOT NULL AUTO_INCREMENT COMMENT '公告ID',
                              `notice_title` varchar(50) NOT NULL COMMENT '公告标题',
                              `notice_type` char(1) NOT NULL COMMENT '公告类型（1通知 2公告）',
                              `notice_content` varchar(2000) DEFAULT NULL COMMENT '公告内容',
                              `status` char(1) DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
                              `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                              `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                              `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                              `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                              `remark` varchar(255) DEFAULT NULL COMMENT '备注',
                              PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8 COMMENT='通知公告表';

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES ('1', '温馨提醒：2018-07-01 若依新版本发布啦', '2', '新版本内容', '0', 'admin', '2022-11-25 05:04:10', '', null, '管理员');
INSERT INTO `sys_notice` VALUES ('2', '维护通知：2018-07-01 若依系统凌晨维护', '1', '维护内容', '0', 'admin', '2022-11-25 05:04:10', '', null, '管理员');

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
                                `oper_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志主键',
                                `title` varchar(50) DEFAULT '' COMMENT '模块标题',
                                `business_type` int(2) DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
                                `method` varchar(100) DEFAULT '' COMMENT '方法名称',
                                `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
                                `operator_type` int(1) DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
                                `oper_name` varchar(50) DEFAULT '' COMMENT '操作人员',
                                `dept_name` varchar(50) DEFAULT '' COMMENT '部门名称',
                                `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
                                `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
                                `oper_location` varchar(255) DEFAULT '' COMMENT '操作地点',
                                `oper_param` varchar(2000) DEFAULT '' COMMENT '请求参数',
                                `json_result` varchar(2000) DEFAULT '' COMMENT '返回参数',
                                `status` int(1) DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
                                `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
                                `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
                                PRIMARY KEY (`oper_id`)
) ENGINE=InnoDB AUTO_INCREMENT=197 DEFAULT CHARSET=utf8 COMMENT='操作日志记录';

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
                            `post_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
                            `post_code` varchar(64) NOT NULL COMMENT '岗位编码',
                            `post_name` varchar(50) NOT NULL COMMENT '岗位名称',
                            `post_sort` int(4) NOT NULL COMMENT '显示顺序',
                            `status` char(1) NOT NULL COMMENT '状态（0正常 1停用）',
                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8 COMMENT='岗位信息表';

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES ('1', 'ceo', '董事长', '1', '0', 'admin', '2022-11-25 05:03:52', '', null, '');
INSERT INTO `sys_post` VALUES ('2', 'se', '项目经理', '2', '0', 'admin', '2022-11-25 05:03:52', '', null, '');
INSERT INTO `sys_post` VALUES ('3', 'hr', '人力资源', '3', '0', 'admin', '2022-11-25 05:03:52', '', null, '');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
                            `role_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                            `role_name` varchar(30) NOT NULL COMMENT '角色名称',
                            `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
                            `role_sort` int(4) NOT NULL COMMENT '显示顺序',
                            `data_scope` char(1) DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
                            `status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
                            `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8 COMMENT='角色信息表';

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES ('1', '超级管理员', 'admin', '1', '1', '0', '0', 'admin', '2022-11-25 05:03:52', '', null, '超级管理员');
INSERT INTO `sys_role` VALUES ('2', '普通角色', 'common', '2', '2', '0', '0', 'admin', '2022-11-25 05:03:52', '', null, '普通角色');

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
                                 `role_id` bigint(20) NOT NULL COMMENT '角色ID',
                                 `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
                                 PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='角色和部门关联表';

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------
INSERT INTO `sys_role_dept` VALUES ('2', '100');
INSERT INTO `sys_role_dept` VALUES ('2', '101');
INSERT INTO `sys_role_dept` VALUES ('2', '105');

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
                                 `role_id` bigint(20) NOT NULL COMMENT '角色ID',
                                 `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
                                 PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='角色和菜单关联表';

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES ('2', '1');
INSERT INTO `sys_role_menu` VALUES ('2', '2');
INSERT INTO `sys_role_menu` VALUES ('2', '3');
INSERT INTO `sys_role_menu` VALUES ('2', '4');
INSERT INTO `sys_role_menu` VALUES ('2', '5');
INSERT INTO `sys_role_menu` VALUES ('2', '100');
INSERT INTO `sys_role_menu` VALUES ('2', '101');
INSERT INTO `sys_role_menu` VALUES ('2', '102');
INSERT INTO `sys_role_menu` VALUES ('2', '103');
INSERT INTO `sys_role_menu` VALUES ('2', '104');
INSERT INTO `sys_role_menu` VALUES ('2', '105');
INSERT INTO `sys_role_menu` VALUES ('2', '106');
INSERT INTO `sys_role_menu` VALUES ('2', '107');
INSERT INTO `sys_role_menu` VALUES ('2', '108');
INSERT INTO `sys_role_menu` VALUES ('2', '109');
INSERT INTO `sys_role_menu` VALUES ('2', '110');
INSERT INTO `sys_role_menu` VALUES ('2', '111');
INSERT INTO `sys_role_menu` VALUES ('2', '112');
INSERT INTO `sys_role_menu` VALUES ('2', '113');
INSERT INTO `sys_role_menu` VALUES ('2', '114');
INSERT INTO `sys_role_menu` VALUES ('2', '115');
INSERT INTO `sys_role_menu` VALUES ('2', '116');
INSERT INTO `sys_role_menu` VALUES ('2', '117');
INSERT INTO `sys_role_menu` VALUES ('2', '500');
INSERT INTO `sys_role_menu` VALUES ('2', '501');
INSERT INTO `sys_role_menu` VALUES ('2', '1000');
INSERT INTO `sys_role_menu` VALUES ('2', '1001');
INSERT INTO `sys_role_menu` VALUES ('2', '1002');
INSERT INTO `sys_role_menu` VALUES ('2', '1003');
INSERT INTO `sys_role_menu` VALUES ('2', '1004');
INSERT INTO `sys_role_menu` VALUES ('2', '1005');
INSERT INTO `sys_role_menu` VALUES ('2', '1006');
INSERT INTO `sys_role_menu` VALUES ('2', '1007');
INSERT INTO `sys_role_menu` VALUES ('2', '1008');
INSERT INTO `sys_role_menu` VALUES ('2', '1009');
INSERT INTO `sys_role_menu` VALUES ('2', '1010');
INSERT INTO `sys_role_menu` VALUES ('2', '1011');
INSERT INTO `sys_role_menu` VALUES ('2', '1012');
INSERT INTO `sys_role_menu` VALUES ('2', '1013');
INSERT INTO `sys_role_menu` VALUES ('2', '1014');
INSERT INTO `sys_role_menu` VALUES ('2', '1015');
INSERT INTO `sys_role_menu` VALUES ('2', '1016');
INSERT INTO `sys_role_menu` VALUES ('2', '1017');
INSERT INTO `sys_role_menu` VALUES ('2', '1018');
INSERT INTO `sys_role_menu` VALUES ('2', '1019');
INSERT INTO `sys_role_menu` VALUES ('2', '1020');
INSERT INTO `sys_role_menu` VALUES ('2', '1021');
INSERT INTO `sys_role_menu` VALUES ('2', '1022');
INSERT INTO `sys_role_menu` VALUES ('2', '1023');
INSERT INTO `sys_role_menu` VALUES ('2', '1024');
INSERT INTO `sys_role_menu` VALUES ('2', '1025');
INSERT INTO `sys_role_menu` VALUES ('2', '1026');
INSERT INTO `sys_role_menu` VALUES ('2', '1027');
INSERT INTO `sys_role_menu` VALUES ('2', '1028');
INSERT INTO `sys_role_menu` VALUES ('2', '1029');
INSERT INTO `sys_role_menu` VALUES ('2', '1030');
INSERT INTO `sys_role_menu` VALUES ('2', '1031');
INSERT INTO `sys_role_menu` VALUES ('2', '1032');
INSERT INTO `sys_role_menu` VALUES ('2', '1033');
INSERT INTO `sys_role_menu` VALUES ('2', '1034');
INSERT INTO `sys_role_menu` VALUES ('2', '1035');
INSERT INTO `sys_role_menu` VALUES ('2', '1036');
INSERT INTO `sys_role_menu` VALUES ('2', '1037');
INSERT INTO `sys_role_menu` VALUES ('2', '1038');
INSERT INTO `sys_role_menu` VALUES ('2', '1039');
INSERT INTO `sys_role_menu` VALUES ('2', '1040');
INSERT INTO `sys_role_menu` VALUES ('2', '1041');
INSERT INTO `sys_role_menu` VALUES ('2', '1042');
INSERT INTO `sys_role_menu` VALUES ('2', '1043');
INSERT INTO `sys_role_menu` VALUES ('2', '1044');
INSERT INTO `sys_role_menu` VALUES ('2', '1045');
INSERT INTO `sys_role_menu` VALUES ('2', '1046');
INSERT INTO `sys_role_menu` VALUES ('2', '1047');
INSERT INTO `sys_role_menu` VALUES ('2', '1048');
INSERT INTO `sys_role_menu` VALUES ('2', '1049');
INSERT INTO `sys_role_menu` VALUES ('2', '1050');
INSERT INTO `sys_role_menu` VALUES ('2', '1051');
INSERT INTO `sys_role_menu` VALUES ('2', '1052');
INSERT INTO `sys_role_menu` VALUES ('2', '1053');
INSERT INTO `sys_role_menu` VALUES ('2', '1054');
INSERT INTO `sys_role_menu` VALUES ('2', '1055');
INSERT INTO `sys_role_menu` VALUES ('2', '1056');
INSERT INTO `sys_role_menu` VALUES ('2', '1057');
INSERT INTO `sys_role_menu` VALUES ('2', '1058');
INSERT INTO `sys_role_menu` VALUES ('2', '1059');
INSERT INTO `sys_role_menu` VALUES ('2', '1060');
INSERT INTO `sys_role_menu` VALUES ('2', '1061');
INSERT INTO `sys_role_menu` VALUES ('2', '1062');
INSERT INTO `sys_role_menu` VALUES ('2', '1063');
INSERT INTO `sys_role_menu` VALUES ('2', '1064');
INSERT INTO `sys_role_menu` VALUES ('2', '1065');
INSERT INTO `sys_role_menu` VALUES ('2', '1066');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                            `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
                            `login_name` varchar(30) NOT NULL COMMENT '登录账号',
                            `user_name` varchar(30) DEFAULT '' COMMENT '用户昵称',
                            `user_type` varchar(2) DEFAULT '00' COMMENT '用户类型（00系统用户 01注册用户）',
                            `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
                            `phonenumber` varchar(11) DEFAULT '' COMMENT '手机号码',
                            `sex` char(1) DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
                            `avatar` varchar(100) DEFAULT '' COMMENT '头像路径',
                            `password` varchar(50) DEFAULT '' COMMENT '密码',
                            `salt` varchar(20) DEFAULT '' COMMENT '盐加密',
                            `status` char(1) DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
                            `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                            `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
                            `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
                            `pwd_update_date` datetime DEFAULT NULL COMMENT '密码最后更新时间',
                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8 COMMENT='用户信息表';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES ('1', '103', 'admin', 'admin', '00', 'ry@163.com', '15888888888', '1', '', '29c67a30398638269fe600f73a054934', '111111', '0', '0', '127.0.0.1', '2022-12-20 15:24:08', '2022-11-25 05:03:52', 'admin', '2022-11-25 05:03:52', '', '2022-12-20 07:24:09', '管理员');
INSERT INTO `sys_user` VALUES ('2', '105', 'ry', '若依', '00', 'ry@qq.com', '15666666666', '1', '', '8e6d98b90472783cc73c17047ddccf36', '222222', '0', '0', '127.0.0.1', '2022-11-25 05:03:52', '2022-11-25 05:03:52', 'admin', '2022-11-25 05:03:52', '', null, '测试员');

-- ----------------------------
-- Table structure for sys_user_online
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_online`;
CREATE TABLE `sys_user_online` (
                                   `sessionId` varchar(50) NOT NULL DEFAULT '' COMMENT '用户会话id',
                                   `login_name` varchar(50) DEFAULT '' COMMENT '登录账号',
                                   `dept_name` varchar(50) DEFAULT '' COMMENT '部门名称',
                                   `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
                                   `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
                                   `browser` varchar(50) DEFAULT '' COMMENT '浏览器类型',
                                   `os` varchar(50) DEFAULT '' COMMENT '操作系统',
                                   `status` varchar(10) DEFAULT '' COMMENT '在线状态on_line在线off_line离线',
                                   `start_timestamp` datetime DEFAULT NULL COMMENT 'session创建时间',
                                   `last_access_time` datetime DEFAULT NULL COMMENT 'session最后访问时间',
                                   `expire_time` int(5) DEFAULT '0' COMMENT '超时时间，单位为分钟',
                                   PRIMARY KEY (`sessionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='在线用户记录';

-- ----------------------------
-- Records of sys_user_online
-- ----------------------------
INSERT INTO `sys_user_online` VALUES ('a7346ff6-e689-4450-9b41-676bc0eedc98', 'admin', '研发部门', '127.0.0.1', '内网IP', 'Chrome 10', 'Windows 10', 'on_line', '2022-12-20 15:24:06', '2022-12-20 15:24:08', '1800000');

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post` (
                                 `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                 `post_id` bigint(20) NOT NULL COMMENT '岗位ID',
                                 PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户与岗位关联表';

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------
INSERT INTO `sys_user_post` VALUES ('1', '1');
INSERT INTO `sys_user_post` VALUES ('2', '2');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
                                 `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                 `role_id` bigint(20) NOT NULL COMMENT '角色ID',
                                 PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户和角色关联表';

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES ('1', '1');
INSERT INTO `sys_user_role` VALUES ('2', '2');
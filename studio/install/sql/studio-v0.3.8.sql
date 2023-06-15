SET FOREIGN_KEY_CHECKS=0;

use studio;
-- ----------------------------
-- Table structure for system_dc_db_config
-- ----------------------------
-- DROP TABLE IF EXISTS `system_dc_db_config`;
CREATE TABLE IF NOT EXISTS `system_dc_db_config` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `connect_name` varchar(255) NOT NULL COMMENT 'db connect_name',
    `type` varchar(255) NOT NULL COMMENT 'db config type',
    `url` varchar(255) NOT NULL COMMENT 'db config url',
    `user_name` varchar(255) DEFAULT NULL COMMENT 'db config user_name',
    `pwd` varchar(255) DEFAULT NULL COMMENT 'db config pwd',
    `create_time` datetime DEFAULT NULL COMMENT 'create_time',
    `create_by` varchar(255) DEFAULT NULL COMMENT 'create_by',
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8 COMMENT='system_dc_db_config';


-- ----------------------------
-- Table structure for system_dc_job_config
-- ----------------------------
-- DROP TABLE IF EXISTS `system_dc_job_config`;
CREATE TABLE IF NOT EXISTS `system_dc_job_config` (
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
    ) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8 COMMENT='system_dc_job_config';

-- ----------------------------
-- Table structure for system_dc_job_instance
-- ----------------------------
-- DROP TABLE IF EXISTS `system_dc_job_instance`;
CREATE TABLE IF NOT EXISTS `system_dc_job_instance` (
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
    ) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8 COMMENT='system_dc_job_instance';


-- ----------------------------
-- Table structure for QRTZ_TRIGGERS
-- ----------------------------
-- DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
CREATE TABLE IF NOT EXISTS `QRTZ_TRIGGERS` (
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
-- Table structure for system_dc_QRTZ_BLOB_TRIGGERS
-- ----------------------------
-- DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
CREATE TABLE IF NOT EXISTS `QRTZ_BLOB_TRIGGERS` (
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
-- DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
CREATE TABLE IF NOT EXISTS `QRTZ_CALENDARS` (
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
-- DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
CREATE TABLE IF NOT EXISTS `QRTZ_CRON_TRIGGERS` (
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
-- DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
CREATE TABLE IF NOT EXISTS `QRTZ_FIRED_TRIGGERS` (
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
-- DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
CREATE TABLE IF NOT EXISTS `QRTZ_JOB_DETAILS` (
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
-- DROP TABLE IF EXISTS `QRTZ_LOCKS`;
CREATE TABLE IF NOT EXISTS `QRTZ_LOCKS` (
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
-- DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
CREATE TABLE IF NOT EXISTS `QRTZ_PAUSED_TRIGGER_GRPS` (
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
-- DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
CREATE TABLE IF NOT EXISTS `QRTZ_SCHEDULER_STATE` (
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
-- DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
CREATE TABLE IF NOT EXISTS `QRTZ_SIMPLE_TRIGGERS` (
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
-- DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
CREATE TABLE IF NOT EXISTS `QRTZ_SIMPROP_TRIGGERS` (
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
-- Table structure for system_dc_config
-- ----------------------------
-- DROP TABLE IF EXISTS `system_dc_config`;
CREATE TABLE IF NOT EXISTS `system_dc_config` (
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
-- Records of system_dc_config
-- ----------------------------
INSERT INTO `system_dc_config` VALUES ('1', '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', '2022-11-25 05:04:08', '', null, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `system_dc_config` VALUES ('2', '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', '2022-11-25 05:04:08', '', null, '初始化密码 123456');
INSERT INTO `system_dc_config` VALUES ('3', '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', '2022-11-25 05:04:08', '', null, '深黑主题theme-dark，浅色主题theme-light，深蓝主题theme-blue');
INSERT INTO `system_dc_config` VALUES ('4', '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 'admin', '2022-11-25 05:04:08', '', null, '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `system_dc_config` VALUES ('5', '用户管理-密码字符范围', 'sys.account.chrtype', '0', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '默认任意字符范围，0任意（密码可以输入任意字符），1数字（密码只能为0-9数字），2英文字母（密码只能为a-z和A-Z字母），3字母和数字（密码必须包含字母，数字）,4字母数字和特殊字符（目前支持的特殊字符包括：~!@#$%^&*()-=_+）');
INSERT INTO `system_dc_config` VALUES ('6', '用户管理-初始密码修改策略', 'sys.account.initPasswordModify', '0', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框');
INSERT INTO `system_dc_config` VALUES ('7', '用户管理-账号密码更新周期', 'sys.account.passwordValidateDays', '0', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框');
INSERT INTO `system_dc_config` VALUES ('8', '主框架页-菜单导航显示风格', 'sys.index.menuStyle', 'default', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '菜单导航显示风格（default为左侧导航菜单，topnav为顶部导航菜单）');
INSERT INTO `system_dc_config` VALUES ('9', '主框架页-是否开启页脚', 'sys.index.footer', 'true', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '是否开启底部页脚显示（true显示，false隐藏）');
INSERT INTO `system_dc_config` VALUES ('10', '主框架页-是否开启页签', 'sys.index.tagsView', 'true', 'Y', 'admin', '2022-11-25 05:04:09', '', null, '是否开启菜单多页签显示（true显示，false隐藏）');

-- ----------------------------
-- Table structure for system_dc_dict_data
-- ----------------------------
-- DROP TABLE IF EXISTS `system_dc_dict_data`;
CREATE TABLE IF NOT EXISTS `system_dc_dict_data` (
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
-- Records of system_dc_dict_data
-- ----------------------------
INSERT INTO `system_dc_dict_data` VALUES ('1', '1', '男', '0', 'system_dc_user_sex', '', '', 'Y', '0', 'admin', '2022-11-25 05:04:06', '', null, '性别男');
INSERT INTO `system_dc_dict_data` VALUES ('2', '2', '女', '1', 'system_dc_user_sex', '', '', 'N', '0', 'admin', '2022-11-25 05:04:06', '', null, '性别女');
INSERT INTO `system_dc_dict_data` VALUES ('3', '3', '未知', '2', 'system_dc_user_sex', '', '', 'N', '0', 'admin', '2022-11-25 05:04:06', '', null, '性别未知');
INSERT INTO `system_dc_dict_data` VALUES ('4', '1', '显示', '0', 'system_dc_show_hide', '', 'primary', 'Y', '0', 'admin', '2022-11-25 05:04:06', '', null, '显示菜单');
INSERT INTO `system_dc_dict_data` VALUES ('5', '2', '隐藏', '1', 'system_dc_show_hide', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:06', '', null, '隐藏菜单');
INSERT INTO `system_dc_dict_data` VALUES ('6', '1', '正常', '0', 'system_dc_normal_disable', '', 'primary', 'Y', '0', 'admin', '2022-11-25 05:04:06', '', null, '正常状态');
INSERT INTO `system_dc_dict_data` VALUES ('7', '2', '停用', '1', 'system_dc_normal_disable', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '停用状态');
INSERT INTO `system_dc_dict_data` VALUES ('8', '1', '正常', '0', 'system_dc_job_status', '', 'primary', 'Y', '0', 'admin', '2022-11-25 05:04:07', '', null, '正常状态');
INSERT INTO `system_dc_dict_data` VALUES ('9', '2', '暂停', '1', 'system_dc_job_status', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '停用状态');
INSERT INTO `system_dc_dict_data` VALUES ('10', '1', '默认', 'DEFAULT', 'system_dc_job_group', '', '', 'Y', '0', 'admin', '2022-11-25 05:04:07', '', null, '默认分组');
INSERT INTO `system_dc_dict_data` VALUES ('11', '2', '系统', 'SYSTEM', 'system_dc_job_group', '', '', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '系统分组');
INSERT INTO `system_dc_dict_data` VALUES ('12', '1', '是', 'Y', 'system_dc_yes_no', '', 'primary', 'Y', '0', 'admin', '2022-11-25 05:04:07', '', null, '系统默认是');
INSERT INTO `system_dc_dict_data` VALUES ('13', '2', '否', 'N', 'system_dc_yes_no', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '系统默认否');
INSERT INTO `system_dc_dict_data` VALUES ('14', '1', '通知', '1', 'system_dc_notice_type', '', 'warning', 'Y', '0', 'admin', '2022-11-25 05:04:07', '', null, '通知');
INSERT INTO `system_dc_dict_data` VALUES ('15', '2', '公告', '2', 'system_dc_notice_type', '', 'success', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '公告');
INSERT INTO `system_dc_dict_data` VALUES ('16', '1', '正常', '0', 'system_dc_notice_status', '', 'primary', 'Y', '0', 'admin', '2022-11-25 05:04:07', '', null, '正常状态');
INSERT INTO `system_dc_dict_data` VALUES ('17', '2', '关闭', '1', 'system_dc_notice_status', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '关闭状态');
INSERT INTO `system_dc_dict_data` VALUES ('18', '99', '其他', '0', 'system_dc_oper_type', '', 'info', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '其他操作');
INSERT INTO `system_dc_dict_data` VALUES ('19', '1', '新增', '1', 'system_dc_oper_type', '', 'info', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '新增操作');
INSERT INTO `system_dc_dict_data` VALUES ('20', '2', '修改', '2', 'system_dc_oper_type', '', 'info', 'N', '0', 'admin', '2022-11-25 05:04:07', '', null, '修改操作');
INSERT INTO `system_dc_dict_data` VALUES ('21', '3', '删除', '3', 'system_dc_oper_type', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '删除操作');
INSERT INTO `system_dc_dict_data` VALUES ('22', '4', '授权', '4', 'system_dc_oper_type', '', 'primary', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '授权操作');
INSERT INTO `system_dc_dict_data` VALUES ('23', '5', '导出', '5', 'system_dc_oper_type', '', 'warning', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '导出操作');
INSERT INTO `system_dc_dict_data` VALUES ('24', '6', '导入', '6', 'system_dc_oper_type', '', 'warning', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '导入操作');
INSERT INTO `system_dc_dict_data` VALUES ('25', '7', '强退', '7', 'system_dc_oper_type', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '强退操作');
INSERT INTO `system_dc_dict_data` VALUES ('26', '8', '生成代码', '8', 'system_dc_oper_type', '', 'warning', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '生成操作');
INSERT INTO `system_dc_dict_data` VALUES ('27', '9', '清空数据', '9', 'system_dc_oper_type', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '清空操作');
INSERT INTO `system_dc_dict_data` VALUES ('28', '1', '成功', '0', 'system_dc_common_status', '', 'primary', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '正常状态');
INSERT INTO `system_dc_dict_data` VALUES ('29', '2', '失败', '1', 'system_dc_common_status', '', 'danger', 'N', '0', 'admin', '2022-11-25 05:04:08', '', null, '停用状态');

-- ----------------------------
-- Table structure for system_dc_dict_type
-- ----------------------------
-- DROP TABLE IF EXISTS `system_dc_dict_type`;
CREATE TABLE IF NOT EXISTS `system_dc_dict_type` (
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
-- Records of system_dc_dict_type
-- ----------------------------
INSERT INTO `system_dc_dict_type` VALUES ('1', '用户性别', 'system_dc_user_sex', '0', 'admin', '2022-11-25 05:04:05', '', null, '用户性别列表');
INSERT INTO `system_dc_dict_type` VALUES ('2', '菜单状态', 'system_dc_show_hide', '0', 'admin', '2022-11-25 05:04:05', '', null, '菜单状态列表');
INSERT INTO `system_dc_dict_type` VALUES ('3', '系统开关', 'system_dc_normal_disable', '0', 'admin', '2022-11-25 05:04:05', '', null, '系统开关列表');
INSERT INTO `system_dc_dict_type` VALUES ('4', '任务状态', 'system_dc_job_status', '0', 'admin', '2022-11-25 05:04:05', '', null, '任务状态列表');
INSERT INTO `system_dc_dict_type` VALUES ('5', '任务分组', 'system_dc_job_group', '0', 'admin', '2022-11-25 05:04:05', '', null, '任务分组列表');
INSERT INTO `system_dc_dict_type` VALUES ('6', '系统是否', 'system_dc_yes_no', '0', 'admin', '2022-11-25 05:04:06', '', null, '系统是否列表');
INSERT INTO `system_dc_dict_type` VALUES ('7', '通知类型', 'system_dc_notice_type', '0', 'admin', '2022-11-25 05:04:06', '', null, '通知类型列表');
INSERT INTO `system_dc_dict_type` VALUES ('8', '通知状态', 'system_dc_notice_status', '0', 'admin', '2022-11-25 05:04:06', '', null, '通知状态列表');
INSERT INTO `system_dc_dict_type` VALUES ('9', '操作类型', 'system_dc_oper_type', '0', 'admin', '2022-11-25 05:04:06', '', null, '操作类型列表');
INSERT INTO `system_dc_dict_type` VALUES ('10', '系统状态', 'system_dc_common_status', '0', 'admin', '2022-11-25 05:04:06', '', null, '登录状态列表');

-- ----------------------------
-- Table structure for system_dc_job
-- ----------------------------
-- DROP TABLE IF EXISTS `system_dc_job`;
CREATE TABLE IF NOT EXISTS `system_dc_job` (
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
-- Records of system_dc_job
-- ----------------------------
INSERT INTO `system_dc_job` VALUES ('1', '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '0/10 * * * * ?', '3', '1', '1', 'admin', '2022-11-25 05:04:10', '', null, '');
INSERT INTO `system_dc_job` VALUES ('2', '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')', '0/15 * * * * ?', '3', '1', '1', 'admin', '2022-11-25 05:04:10', '', null, '');
INSERT INTO `system_dc_job` VALUES ('3', '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)', '0/20 * * * * ?', '3', '1', '1', 'admin', '2022-11-25 05:04:10', '', null, '');

-- ----------------------------
-- Table structure for system_dc_job_log
-- ----------------------------
-- DROP TABLE IF EXISTS `system_dc_job_log`;
CREATE TABLE IF NOT EXISTS `system_dc_job_log` (
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
-- Records of system_dc_job_log
-- ----------------------------
INSERT INTO `system_dc_job_log` VALUES ('1', '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '系统默认（无参） 总共耗时：2毫秒', '0', '', '2022-11-25 05:08:23');



SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_ddh_access_token
-- ----------------------------
CREATE TABLE `t_ddh_access_token`  (
                                       `id` int(10) NOT NULL,
                                       `user_id` int(10) NULL DEFAULT NULL,
                                       `token` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                       `create_time` datetime NULL DEFAULT NULL,
                                       `update_time` datetime NULL DEFAULT NULL,
                                       `expire_time` datetime NULL DEFAULT NULL,
                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

DROP TABLE IF EXISTS `t_ddh_cluster_yarn_scheduler`;
CREATE TABLE `t_ddh_cluster_yarn_scheduler`  (
                                                 `id` int(10) NOT NULL AUTO_INCREMENT,
                                                 `cluster_id` int(11) NULL DEFAULT NULL,
                                                 `scheduler` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                                 `in_use` int(2) NULL DEFAULT NULL COMMENT '1: 是  2：否',
                                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Table structure for t_ddh_alert_group
-- ----------------------------
CREATE TABLE `t_ddh_alert_group`  (
                                      `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `alert_group_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警组名称',
                                      `alert_group_category` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警组类别',
                                      `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                      PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '告警组表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_expression
-- ----------------------------
CREATE TABLE `t_ddh_cluster_alert_expression`  (
                                                   `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增 ID',
                                                   `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '指标名称',
                                                   `expr` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '监控指标表达式',
                                                   `service_category` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务类别',
                                                   `value_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '阈值类型  BOOL  INT  FLOAT  ',
                                                   `is_predefined` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否预定义',
                                                   `state` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '表达式状态',
                                                   `is_delete` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否删除',
                                                   `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                                   `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
                                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 134002 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '表达式常量表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_group_map
-- ----------------------------
CREATE TABLE `t_ddh_cluster_alert_group_map`  (
                                                  `id` int(10) NOT NULL AUTO_INCREMENT,
                                                  `cluster_id` int(10) NULL DEFAULT NULL,
                                                  `alert_group_id` int(10) NULL DEFAULT NULL,
                                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_alert_history
-- ----------------------------
CREATE TABLE `t_ddh_cluster_alert_history`  (
                                                `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                `alert_group_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警组',
                                                `alert_target_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警指标',
                                                `alert_info` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警详情',
                                                `alert_advice` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警建议',
                                                `hostname` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主机',
                                                `alert_level` int(11) NULL DEFAULT NULL COMMENT '告警级别 1：警告2：异常',
                                                `is_enabled` int(11) NULL DEFAULT NULL COMMENT '是否处理 1:未处理2：已处理',
                                                `service_role_instance_id` int(11) NULL DEFAULT NULL COMMENT '集群服务角色实例id',
                                                `service_instance_id` int(11) NULL DEFAULT NULL COMMENT '集群服务实例id',
                                                `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                                `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                                `cluster_id` int(10) NULL DEFAULT NULL COMMENT '集群id',
                                                PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群告警历史表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_quota
-- ----------------------------
CREATE TABLE `t_ddh_cluster_alert_quota`  (
                                              `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                              `alert_quota_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警指标名称',
                                              `service_category` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务分类',
                                              `alert_expr` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警指标表达式',
                                              `alert_level` int(11) NULL DEFAULT NULL COMMENT '告警级别 1:警告2：异常',
                                              `alert_group_id` int(11) NULL DEFAULT NULL COMMENT '告警组',
                                              `notice_group_id` int(11) NULL DEFAULT NULL COMMENT '通知组',
                                              `alert_advice` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警建议',
                                              `compare_method` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '比较方式 !=;>;<',
                                              `alert_threshold` bigint(200) NULL DEFAULT NULL COMMENT '告警阀值',
                                              `alert_tactic` int(11) NULL DEFAULT NULL COMMENT '告警策略 1:单次2：连续',
                                              `interval_duration` int(11) NULL DEFAULT NULL COMMENT '间隔时长 单位分钟',
                                              `trigger_duration` int(11) NULL DEFAULT NULL COMMENT '触发时长 单位秒',
                                              `service_role_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务角色名称',
                                              `quota_state` int(2) NULL DEFAULT NULL COMMENT '1: 启用  2：未启用',
                                              `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                              PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 620 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群告警指标表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_rule
-- ----------------------------
CREATE TABLE `t_ddh_cluster_alert_rule`  (
                                             `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增 ID',
                                             `expression_id` bigint(20) NOT NULL COMMENT '表达式 ID',
                                             `is_predefined` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否预定义',
                                             `compare_method` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '比较方式 如 大于 小于 等于 等',
                                             `threshold_value` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '阈值',
                                             `persistent_time` bigint(20) NOT NULL COMMENT '持续时长',
                                             `strategy` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '告警策略：单次，连续',
                                             `repeat_interval` bigint(11) NULL DEFAULT NULL COMMENT '连续告警时 间隔时长',
                                             `alert_level` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '告警级别',
                                             `alert_desc` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '告警描述',
                                             `receiver_group_id` bigint(20) NULL DEFAULT NULL COMMENT '接收组 ID',
                                             `state` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '状态',
                                             `is_delete` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否删除',
                                             `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                             `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
                                             `cluster_id` int(10) NULL DEFAULT NULL COMMENT '集群id',
                                             PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 134002 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '规则表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_cluster_group
-- ----------------------------
CREATE TABLE `t_ddh_cluster_group`  (
                                        `id` int(10) NOT NULL AUTO_INCREMENT,
                                        `group_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                        `cluster_id` int(10) NULL DEFAULT NULL,
                                        PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_host
-- ----------------------------
CREATE TABLE `t_ddh_cluster_host`  (
                                       `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                       `hostname` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主机名',
                                       `ip` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'IP',
                                       `rack` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '机架',
                                       `core_num` int(11) NULL DEFAULT NULL COMMENT '核数',
                                       `total_mem` int(11) NULL DEFAULT NULL COMMENT '总内存',
                                       `total_disk` int(11) NULL DEFAULT NULL COMMENT '总磁盘',
                                       `used_mem` int(11) NULL DEFAULT NULL COMMENT '已用内存',
                                       `used_disk` int(11) NULL DEFAULT NULL COMMENT '已用磁盘',
                                       `average_load` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '平均负载',
                                       `check_time` datetime NULL DEFAULT NULL COMMENT '检测时间',
                                       `cluster_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '集群id',
                                       `host_state` int(2) NULL DEFAULT NULL COMMENT '1:健康 2、有一个角色异常3、有多个角色异常',
                                       `managed` int(2) NULL DEFAULT NULL COMMENT '1:受管 2：断线',
                                       `cpu_architecture` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'cpu架构',
                                       `node_label` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '节点标签',
                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群主机表 ' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_info
-- ----------------------------
CREATE TABLE `t_ddh_cluster_info`  (
                                       `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `create_by` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
                                       `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                       `cluster_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '集群名称',
                                       `cluster_code` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '集群编码',
                                       `cluster_frame` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '集群框架',
                                       `frame_version` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '集群版本',
                                       `cluster_state` int(11) NULL DEFAULT NULL COMMENT '集群状态 1:待配置2：正在运行',
                                       `frame_id` int(10) NULL DEFAULT NULL,
                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群信息表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_node_label
-- ----------------------------
CREATE TABLE `t_ddh_cluster_node_label`  (
                                             `id` int(10) NOT NULL,
                                             `cluster_id` int(10) NULL DEFAULT NULL,
                                             `node_label` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                             PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_rack
-- ----------------------------
CREATE TABLE `t_ddh_cluster_rack`  (
                                       `id` int(10) NOT NULL AUTO_INCREMENT,
                                       `rack` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                       `clusterId` int(10) NULL DEFAULT NULL,
                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_role_user
-- ----------------------------
CREATE TABLE `t_ddh_cluster_role_user`  (
                                            `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `cluster_id` int(11) NULL DEFAULT NULL COMMENT '集群id',
                                            `user_type` int(2) NULL DEFAULT NULL COMMENT '集群用户类型1：管理员2：普通用户',
                                            `user_id` int(11) NULL DEFAULT NULL COMMENT '用户id',
                                            PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群角色用户中间表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_service_command
-- ----------------------------
CREATE TABLE `t_ddh_cluster_service_command`  (
                                                  `command_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键',
                                                  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
                                                  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                                  `command_name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '命令名称',
                                                  `command_state` int(11) NULL DEFAULT NULL COMMENT '命令状态 0：待运行 1：正在运行2：成功3：失败4、取消',
                                                  `command_progress` int(11) NULL DEFAULT NULL COMMENT '命令进度',
                                                  `cluster_id` int(10) NULL DEFAULT NULL,
                                                  `service_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                                  `command_type` int(2) NULL DEFAULT NULL COMMENT '命令类型1：安装服务 2：启动服务 3：停止服务 4：重启服务 5：更新配置后启动 6：更新配置后重启',
                                                  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
                                                  `service_instance_id` int(10) NULL DEFAULT NULL COMMENT '服务实例id',
                                                  UNIQUE INDEX `command_id`(`command_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务操作指令表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_service_command_host
-- ----------------------------
CREATE TABLE `t_ddh_cluster_service_command_host`  (
                                                       `command_host_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '1' COMMENT '主键',
                                                       `hostname` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主机',
                                                       `command_state` int(11) NULL DEFAULT NULL COMMENT '命令状态 1：正在运行2：成功3：失败4、取消',
                                                       `command_progress` int(11) NULL DEFAULT NULL COMMENT '命令进度',
                                                       `command_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '操作指令id',
                                                       `create_time` datetime NULL DEFAULT NULL,
                                                       UNIQUE INDEX `command_host_id`(`command_host_id`) USING BTREE,
                                                       UNIQUE INDEX `command_host_id_2`(`command_host_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务操作指令主机表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_service_command_host_command
-- ----------------------------
CREATE TABLE `t_ddh_cluster_service_command_host_command`  (
                                                               `host_command_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '1' COMMENT '主键',
                                                               `command_name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '指令名称',
                                                               `command_state` int(11) NULL DEFAULT NULL COMMENT '指令状态',
                                                               `command_progress` int(11) NULL DEFAULT NULL COMMENT '指令进度',
                                                               `command_host_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主机id',
                                                               `hostname` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主机',
                                                               `service_role_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务角色名称',
                                                               `service_role_type` int(2) NULL DEFAULT NULL COMMENT '服务角色类型',
                                                               `command_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '指令id',
                                                               `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                                               `command_type` int(2) NULL DEFAULT NULL COMMENT '1：安装服务 2：启动服务 3：停止服务 4：重启服务 5：更新配置后启动 6：更新配置后重启',
                                                               `result_msg` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                                                               UNIQUE INDEX `host_command_id`(`host_command_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务操作指令主机指令表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_cluster_service_dashboard
-- ----------------------------
CREATE TABLE `t_ddh_cluster_service_dashboard`  (
                                                    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主机',
                                                    `service_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务名称',
                                                    `dashboard_url` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '总览页面地址',
                                                    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务总览仪表盘' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_cluster_service_instance
-- ----------------------------
CREATE TABLE `t_ddh_cluster_service_instance`  (
                                                   `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                   `cluster_id` int(11) NULL DEFAULT NULL COMMENT '集群id',
                                                   `service_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务名称',
                                                   `service_state` int(11) NULL DEFAULT NULL COMMENT '服务状态 1、待安装 2：正在运行 3：存在告警 4：存在异常',
                                                   `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                                   `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                                   `need_restart` int(2) NULL DEFAULT NULL COMMENT '是否需要重启 1：正常 2：需要重启',
                                                   `frame_service_id` int(10) NULL DEFAULT NULL COMMENT '框架服务id',
                                                   `sort_num` int(2) NULL DEFAULT NULL COMMENT '排序字段',
                                                   `label` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_service_instance_role_group
-- ----------------------------
CREATE TABLE `t_ddh_cluster_service_instance_role_group`  (
                                                              `id` int(10) NOT NULL AUTO_INCREMENT,
                                                              `role_group_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                                              `service_instance_id` int(11) NULL DEFAULT NULL,
                                                              `service_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                                              `cluster_id` int(11) NULL DEFAULT NULL,
                                                              `role_group_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                                              `create_time` datetime NULL DEFAULT NULL,
                                                              PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_service_role_group_config
-- ----------------------------
CREATE TABLE `t_ddh_cluster_service_role_group_config`  (
                                                            `id` int(10) NOT NULL AUTO_INCREMENT,
                                                            `role_group_id` int(10) NULL DEFAULT NULL,
                                                            `config_json` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                                                            `config_json_md5` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                                            `config_version` int(2) NULL DEFAULT NULL,
                                                            `config_file_json` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                                                            `config_file_json_md5` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                                            `cluster_id` int(10) NULL DEFAULT NULL,
                                                            `create_time` datetime NULL DEFAULT NULL,
                                                            `update_time` datetime NULL DEFAULT NULL,
                                                            `service_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                                            PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_service_role_instance
-- ----------------------------
CREATE TABLE `t_ddh_cluster_service_role_instance`  (
                                                        `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                        `service_role_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务角色名称',
                                                        `hostname` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主机',
                                                        `service_role_state` int(2) NULL DEFAULT NULL COMMENT '服务角色状态 1:正在运行2：停止',
                                                        `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                                        `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                                        `service_id` int(11) NULL DEFAULT NULL COMMENT '服务id',
                                                        `role_type` int(11) NULL DEFAULT NULL COMMENT '角色类型 1:master2:worker3:client',
                                                        `cluster_id` int(10) NULL DEFAULT NULL COMMENT '集群id',
                                                        `service_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务名称',
                                                        `role_group_id` int(10) NULL DEFAULT NULL COMMENT '角色组id',
                                                        `need_restart` int(10) NULL DEFAULT NULL COMMENT '是否需要重启 1：正常 2：需要重启',
                                                        PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务角色实例表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_cluster_service_role_instance_webuis
-- ----------------------------
CREATE TABLE `t_ddh_cluster_service_role_instance_webuis`  (
                                                               `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                               `service_role_instance_id` int(10) NULL DEFAULT NULL COMMENT '服务角色id',
                                                               `web_url` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'URL地址',
                                                               `service_instance_id` int(10) NULL DEFAULT NULL,
                                                               `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                                               PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务角色对应web ui表 ' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_user
-- ----------------------------
CREATE TABLE `t_ddh_cluster_user`  (
                                       `id` int(10) NOT NULL AUTO_INCREMENT,
                                       `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                       `cluster_id` int(10) NULL DEFAULT NULL,
                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_user_group
-- ----------------------------
CREATE TABLE `t_ddh_cluster_user_group`  (
                                             `id` int(10) NOT NULL AUTO_INCREMENT,
                                             `user_id` int(10) NULL DEFAULT NULL,
                                             `group_id` int(10) NULL DEFAULT NULL,
                                             `cluster_id` int(10) NULL DEFAULT NULL,
                                             `user_group_type` int(2) NULL DEFAULT NULL COMMENT '1:主用户组 2：附加组',
                                             PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_cluster_variable
-- ----------------------------
CREATE TABLE `t_ddh_cluster_variable`  (
                                           `id` int(10) NOT NULL AUTO_INCREMENT,
                                           `cluster_id` int(10) NULL DEFAULT NULL,
                                           `variable_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                           `variable_value` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                           PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_cluster_yarn_queue
-- ----------------------------
CREATE TABLE `t_ddh_cluster_yarn_queue`  (
                                             `id` int(10) NOT NULL AUTO_INCREMENT,
                                             `queue_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                             `min_core` int(10) NULL DEFAULT NULL,
                                             `min_mem` int(10) NULL DEFAULT NULL,
                                             `max_core` int(10) NULL DEFAULT NULL,
                                             `max_mem` int(10) NULL DEFAULT NULL,
                                             `app_num` int(10) NULL DEFAULT NULL,
                                             `weight` int(2) NULL DEFAULT NULL,
                                             `schedule_policy` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'fifo ,fair ,drf',
                                             `allow_preemption` int(2) NULL DEFAULT NULL COMMENT '1: true 2:false',
                                             `cluster_id` int(10) NULL DEFAULT NULL,
                                             `am_share` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                             `create_time` datetime NULL DEFAULT NULL,
                                             PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_cluster_zk
-- ----------------------------
CREATE TABLE `t_ddh_cluster_zk`  (
                                     `id` int(10) NOT NULL AUTO_INCREMENT,
                                     `zk_server` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                     `myid` int(10) NULL DEFAULT NULL,
                                     `cluster_id` int(10) NULL DEFAULT NULL,
                                     PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_command
-- ----------------------------
CREATE TABLE `t_ddh_command`  (
                                  `id` int(10) NOT NULL,
                                  `command_type` int(2) NULL DEFAULT NULL,
                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_frame_info
-- ----------------------------
CREATE TABLE `t_ddh_frame_info`  (
                                     `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `frame_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '框架名称',
                                     `frame_code` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '框架编码',
                                     `frame_version` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                     PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群框架表' ROW_FORMAT = DYNAMIC;



-- ----------------------------
-- Table structure for t_ddh_frame_service
-- ----------------------------
CREATE TABLE `t_ddh_frame_service`  (
                                        `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                        `frame_id` int(11) NULL DEFAULT NULL COMMENT '版本id',
                                        `service_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务名称',
                                        `label` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                        `service_version` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务版本',
                                        `service_desc` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务描述',
                                        `dependencies` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务依赖',
                                        `package_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '安装包名称',
                                        `service_config` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                                        `service_json` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                                        `service_json_md5` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                        `frame_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                        `config_file_json` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                                        `config_file_json_md5` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                        `decompress_package_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                        `sort_num` int(2) NULL DEFAULT NULL COMMENT '排序字段',
                                        PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群框架版本服务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_frame_service_role
-- ----------------------------
CREATE TABLE `t_ddh_frame_service_role`  (
                                             `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `service_id` int(11) NULL DEFAULT NULL COMMENT '服务id',
                                             `service_role_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色名称',
                                             `service_role_type` int(11) NULL DEFAULT NULL COMMENT '角色类型 1:master2:worker3:client',
                                             `cardinality` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                             `service_role_json` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                                             `service_role_json_md5` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                             `frame_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                             `jmx_port` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                             `log_file` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                             PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '框架服务角色表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_install_step
-- ----------------------------
CREATE TABLE `t_ddh_install_step`  (
                                       `id` int(10) NOT NULL AUTO_INCREMENT,
                                       `step_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                       `step_desc` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                       `install_type` int(1) NULL DEFAULT NULL COMMENT '1:集群配置2：添加服务3：添加主机',
                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_notice_group
-- ----------------------------
CREATE TABLE `t_ddh_notice_group`  (
                                       `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `notice_group_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '通知组名称',
                                       `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '通知组表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_notice_group_user
-- ----------------------------
CREATE TABLE `t_ddh_notice_group_user`  (
                                            `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `notice_group_id` int(11) NULL DEFAULT NULL COMMENT '通知组id',
                                            `user_id` int(11) NULL DEFAULT NULL COMMENT '用户id',
                                            PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '通知组-用户中间表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_role_info
-- ----------------------------
CREATE TABLE `t_ddh_role_info`  (
                                    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `role_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色名称',
                                    `role_code` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色编码',
                                    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '角色信息表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_ddh_session
-- ----------------------------
CREATE TABLE `t_ddh_session`  (
                                  `id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                                  `user_id` int(10) NULL DEFAULT NULL,
                                  `ip` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                  `last_login_time` datetime NULL DEFAULT NULL,
                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_ddh_user_info
-- ----------------------------
CREATE TABLE `t_ddh_user_info`  (
                                    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `username` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户名',
                                    `password` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
                                    `email` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '邮箱',
                                    `phone` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '手机号',
                                    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                    `user_type` int(2) NULL DEFAULT NULL COMMENT '1：超级管理员 2：普通用户',
                                    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;


SET FOREIGN_KEY_CHECKS = 1;

/*
 Navicat MySQL Data Transfer

 Source Server         : 日常 - AppManager
 Source Server Type    : MySQL
 Source Server Version : 80018
 Source Host           : rm-8vbo5942x7hjna4v8.mysql.zhangbei.rds.aliyuncs.com:3306
 Source Schema         : appmanager

 Target Server Type    : MySQL
 Target Server Version : 80018
 File Encoding         : 65001

 Date: 22/11/2020 22:04:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for am_addon_instance
-- ----------------------------
DROP TABLE IF EXISTS `am_addon_instance`;
CREATE TABLE `am_addon_instance`
(
    `id`                bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`        datetime(0)                                                   NULL     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`      datetime(0)                                                   NULL     DEFAULT NULL COMMENT '最后修改时间',
    `addon_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL     DEFAULT '' COMMENT '资源实例ID',
    `namespace_id`      varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL     DEFAULT NULL COMMENT '该附加组件部署到的 Namespace 标识',
    `addon_id`          varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL     DEFAULT NULL COMMENT '附加组件唯一标识',
    `addon_name`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '组件名称',
    `addon_version`     varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL     DEFAULT '' COMMENT '附加组件唯一标识',
    `addon_attrs`       longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci         NULL COMMENT '附加组件属性',
    `addon_ext`         longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '附加组件扩展信息',
    `data_output`       longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '附加组件 config var 字典内容',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE,
    INDEX `uk_addon_instance_id` (`addon_instance_id`) USING BTREE,
    INDEX `idx_addon_name_version` (`namespace_id`, `addon_name`, `addon_version`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 90
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '附加组件实例'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_addon_instance_task
-- ----------------------------
DROP TABLE IF EXISTS `am_addon_instance_task`;
CREATE TABLE `am_addon_instance_task`
(
    `id`                 bigint(20) UNSIGNED                                          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`         datetime(0)                                                  NOT NULL COMMENT '创建时间',
    `gmt_modified`       datetime(0)                                                  NOT NULL COMMENT '修改时间',
    `namespace_id`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '命名空间 ID',
    `addon_id`           varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组件ID',
    `addon_name`         varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组件实例名称',
    `addon_version`      varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组件版本',
    `addon_attrs`        longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '附加组件属性',
    `task_status`        varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态',
    `task_error_message` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL COMMENT '错误信息',
    `task_process_id`    bigint(20)                                                   NULL DEFAULT NULL COMMENT '处理流程ID',
    `task_ext`           longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NOT NULL COMMENT '扩展信息',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE,
    INDEX `idx_addon_id_version` (`addon_id`, `addon_version`) USING BTREE,
    INDEX `idx_task_status` (`task_status`) USING BTREE,
    INDEX `idx_task_process_id` (`task_process_id`) USING BTREE,
    INDEX `idx_addon_name_version` (`addon_name`, `addon_version`) USING BTREE,
    INDEX `idx_namespace` (`namespace_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 20
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '附加组件实例任务'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_addon_meta
-- ----------------------------
DROP TABLE IF EXISTS `am_addon_meta`;
CREATE TABLE `am_addon_meta`
(
    `id`                  bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`          datetime(0)                                                   NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`        datetime(0)                                                   NULL DEFAULT NULL COMMENT '最后修改时间',
    `addon_type`          varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '类型（可选 CORE-SERVICE / THIRDPARTY）',
    `addon_id`            varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '附加组件唯一标识',
    `addon_version`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '版本号',
    `addon_label`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '附加组件名',
    `addon_description`   mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL COMMENT '附加组件描述',
    `addon_schema`        longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '附加组件定义 Schema',
    `addon_config_schema` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL COMMENT '附件组件配置Schema',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_addon_id_version` (`addon_id`, `addon_version`) USING BTREE,
    INDEX `idx_addon_type` (`addon_type`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE,
    INDEX `idx_addon_label` (`addon_label`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 37
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '附加组件元信息'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_app_addon
-- ----------------------------
DROP TABLE IF EXISTS `am_app_addon`;
CREATE TABLE `am_app_addon`
(
    `id`            bigint(20) UNSIGNED                                          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`    datetime(0)                                                  NOT NULL COMMENT '创建时间',
    `gmt_modified`  datetime(0)                                                  NOT NULL COMMENT '修改时间',
    `app_id`        varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用唯一标识',
    `addon_type`    varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型（可选 CORE-SERVICE / THIRDPARTY）',
    `addon_id`      varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '附加组件唯一标识',
    `addon_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '版本号',
    `addon_config`  longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NOT NULL COMMENT '附加组件配置',
    `name`          varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_app_addon` (`app_id`, `name`) USING BTREE,
    INDEX `idx_app_addon_type` (`app_id`, `addon_type`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '应用addon关联表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_app_meta
-- ----------------------------
DROP TABLE IF EXISTS `am_app_meta`;
CREATE TABLE `am_app_meta`
(
    `id`           bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime(0)                                                  NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                  NULL DEFAULT NULL COMMENT '最后修改时间',
    `app_id`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用唯一标识',
    `app_type`     varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用类型',
    `app_name`     varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用名称',
    `app_ext`      longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NULL COMMENT '扩展信息 JSON',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_app_id` (`app_id`) USING BTREE,
    INDEX `idx_app_type` (`app_type`) USING BTREE,
    INDEX `idx_app_name` (`app_name`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '应用元信息'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_app_package
-- ----------------------------
DROP TABLE IF EXISTS `am_app_package`;
CREATE TABLE `am_app_package`
(
    `id`              bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`      datetime(0)                                                   NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`    datetime(0)                                                   NULL DEFAULT NULL COMMENT '最后修改时间',
    `app_id`          varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '应用唯一标识',
    `package_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '版本号',
    `package_path`    varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '存储位置相对路径',
    `package_creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '创建者',
    `package_ext`     longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '扩展信息 JSON',
    `package_md5`     varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '包 MD5',
    `component_count` bigint(20)                                                    NULL DEFAULT NULL COMMENT '组件包计数',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_package_id` (`app_id`, `package_version`) USING BTREE,
    INDEX `idx_package_creator` (`package_creator`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 75
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '应用包详情表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_app_package_component_rel
-- ----------------------------
DROP TABLE IF EXISTS `am_app_package_component_rel`;
CREATE TABLE `am_app_package_component_rel`
(
    `id`                   bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime(0) NULL DEFAULT NULL COMMENT '最后修改时间',
    `app_package_id`       bigint(20)  NOT NULL COMMENT '所属应用包 ID',
    `component_package_id` bigint(20)  NOT NULL COMMENT '所属组件包 ID',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_relation` (`app_package_id`, `component_package_id`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 64
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '应用包中组件引用关系表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_app_package_tag
-- ----------------------------
DROP TABLE IF EXISTS `am_app_package_tag`;
CREATE TABLE `am_app_package_tag`
(
    `id`             bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`     datetime(0)                                                  NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`   datetime(0)                                                  NULL DEFAULT NULL COMMENT '最后修改时间',
    `app_package_id` bigint(20)                                                   NOT NULL COMMENT '所属应用包 ID',
    `tag`            varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标签',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_package_tag` (`app_package_id`, `tag`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '应用包标签表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_app_package_task
-- ----------------------------
DROP TABLE IF EXISTS `am_app_package_task`;
CREATE TABLE `am_app_package_task`
(
    `id`              bigint(20) UNSIGNED                                          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`      datetime(0)                                                  NOT NULL COMMENT '创建时间',
    `gmt_modified`    datetime(0)                                                  NOT NULL COMMENT '修改时间',
    `app_id`          varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用唯一标识',
    `app_package_id`  bigint(20) UNSIGNED                                          NULL DEFAULT NULL COMMENT '映射 app package 表主键 ID',
    `package_creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建人',
    `task_status`     varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务状态',
    `package_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '版本号',
    `package_options` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '包配置选项信息',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '应用包任务表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_component_package
-- ----------------------------
DROP TABLE IF EXISTS `am_component_package`;
CREATE TABLE `am_component_package`
(
    `id`              bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`      datetime(0)                                                   NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`    datetime(0)                                                   NULL DEFAULT NULL COMMENT '最后修改时间',
    `app_id`          varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '应用唯一标识',
    `component_type`  varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '组件类型',
    `package_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '版本号',
    `package_path`    varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '存储位置相对路径',
    `package_creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '创建者',
    `package_ext`     longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '扩展信息 JSON',
    `component_name`  varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '组件类型下的唯一组件标识',
    `package_md5`     varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '包 MD5',
    `package_addon`   mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL COMMENT '包 Addon 描述信息',
    `package_options` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL COMMENT '包配置选项信息',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_package_id` (`app_id`, `component_type`, `component_name`, `package_version`) USING BTREE,
    INDEX `idx_package_creator` (`package_creator`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 60
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '组件包详情表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_component_package_task
-- ----------------------------
DROP TABLE IF EXISTS `am_component_package_task`;
CREATE TABLE `am_component_package_task`
(
    `id`                   bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime(0)                                                   NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime(0)                                                   NULL DEFAULT NULL COMMENT '最后修改时间',
    `app_id`               varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '应用唯一标识',
    `component_type`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '组件类型',
    `component_name`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '组件类型下的唯一组件标识',
    `package_version`      varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '版本号',
    `package_path`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '存储位置相对路径',
    `package_creator`      varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '创建者',
    `package_md5`          varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '包 MD5',
    `package_addon`        mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL COMMENT '包 Addon 描述信息',
    `package_options`      mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL COMMENT '包配置选项信息',
    `package_ext`          longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '扩展信息 JSON',
    `task_status`          varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '任务状态',
    `task_log`             longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '任务日志',
    `component_package_id` bigint(20)                                                    NULL DEFAULT NULL COMMENT '映射 component package 表主键 ID',
    `app_package_task_id`  bigint(20)                                                    NULL DEFAULT NULL COMMENT '应用包任务ID',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_package_task_id` (`app_id`, `component_type`, `component_name`, `package_version`) USING BTREE,
    INDEX `idx_package_task_status` (`task_status`) USING BTREE,
    INDEX `idx_package_creator` (`package_creator`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE,
    INDEX `idx_app_package_task_id` (`app_package_task_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 69
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '组件包创建任务详情表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_deploy_app
-- ----------------------------
DROP TABLE IF EXISTS `am_deploy_app`;
CREATE TABLE `am_deploy_app`
(
    `id`                   bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime(0)                                                  NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime(0)                                                  NULL DEFAULT NULL COMMENT '最后修改时间',
    `app_package_id`       bigint(20)                                                   NOT NULL COMMENT '当前部署单使用的应用包 ID',
    `app_id`               varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用唯一标识',
    `namespace_id`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部署目标 Namespace ID',
    `gmt_start`            datetime(0)                                                  NULL DEFAULT NULL COMMENT '部署开始时间',
    `gmt_end`              datetime(0)                                                  NULL DEFAULT NULL COMMENT '部署结束时间',
    `deploy_status`        varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态',
    `deploy_error_message` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL COMMENT '错误信息',
    `deploy_creator`       varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部署工单发起人',
    `deploy_ext`           longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NULL COMMENT '扩展信息 JSON (DeployAppSchema)',
    `deploy_process_id`    bigint(20)                                                   NULL DEFAULT NULL COMMENT '部署流程 ID',
    `deploy_global_params` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NULL COMMENT '部署过程中的全局参数',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_app_package_id` (`app_package_id`) USING BTREE,
    INDEX `idx_app_id` (`app_id`) USING BTREE,
    INDEX `idx_namespace_id` (`namespace_id`) USING BTREE,
    INDEX `idx_deploy_status` (`deploy_status`) USING BTREE,
    INDEX `idx_deploy_creator` (`deploy_creator`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE,
    INDEX `idx_deploy_process_id` (`deploy_process_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 44
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '部署工单 - AppPackage 表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_deploy_component
-- ----------------------------
DROP TABLE IF EXISTS `am_deploy_component`;
CREATE TABLE `am_deploy_component`
(
    `id`                   bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime(0)                                                  NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime(0)                                                  NULL DEFAULT NULL COMMENT '最后修改时间',
    `deploy_id`            bigint(20)                                                   NOT NULL COMMENT '所属部署单 ID',
    `component_package_id` bigint(20)                                                   NOT NULL COMMENT '当前部署项对应的组件包 ID',
    `app_id`               varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用唯一标识',
    `namespace_id`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部署目标 Namespace ID',
    `env_id`               varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部署目标环境 ID',
    `gmt_start`            datetime(0)                                                  NULL DEFAULT NULL COMMENT '部署开始时间',
    `gmt_end`              datetime(0)                                                  NULL DEFAULT NULL COMMENT '部署结束时间',
    `deploy_status`        varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态',
    `deploy_error_message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NULL COMMENT '错误信息',
    `deploy_creator`       varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部署工单发起人',
    `deploy_process_id`    varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部署流程 ID',
    `deploy_ext`           longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NULL COMMENT '扩展信息 JSON',
    `deploy_options`       longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_deploy_id` (`deploy_id`) USING BTREE,
    INDEX `idx_component_package_id` (`component_package_id`) USING BTREE,
    INDEX `idx_app_id` (`app_id`) USING BTREE,
    INDEX `idx_namespace_id` (`namespace_id`) USING BTREE,
    INDEX `idx_env_id` (`env_id`) USING BTREE,
    INDEX `idx_deploy_process_id` (`deploy_process_id`) USING BTREE,
    INDEX `idx_deploy_status` (`deploy_status`) USING BTREE,
    INDEX `idx_deploy_creator` (`deploy_creator`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '部署工单 - ComponentPackage 表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_env
-- ----------------------------
DROP TABLE IF EXISTS `am_env`;
CREATE TABLE `am_env`
(
    `id`           bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime(0)                                                  NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                  NULL DEFAULT NULL COMMENT '最后修改时间',
    `namespace_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属 Namespace ID',
    `env_id`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '环境 ID',
    `env_name`     varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '环境名称',
    `env_creator`  varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '环境创建者',
    `env_modifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '环境最后修改者',
    `env_ext`      longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NULL COMMENT '环境扩展信息',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_namespace_id_env_id` (`namespace_id`, `env_id`) USING BTREE,
    INDEX `idx_env_name` (`env_name`) USING BTREE,
    INDEX `idx_env_creator` (`env_creator`) USING BTREE,
    INDEX `idx_env_modifier` (`env_modifier`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 4
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '环境表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_k8s_micro_service_meta
-- ----------------------------
DROP TABLE IF EXISTS `am_k8s_micro_service_meta`;
CREATE TABLE `am_k8s_micro_service_meta`
(
    `id`                bigint(20) UNSIGNED                                            NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`        datetime(0)                                                    NOT NULL COMMENT '创建时间',
    `gmt_modified`      datetime(0)                                                    NOT NULL COMMENT '修改时间',
    `app_id`            varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NOT NULL COMMENT '应用标示',
    `micro_service_id`  varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '微服务标示',
    `name`              varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '微服务名称',
    `description`       varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '微服务描述',
    `micro_service_ext` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NULL COMMENT '扩展信息',
    `options`           longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci      NULL COMMENT 'options',
    `component_type`    varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL DEFAULT NULL COMMENT '组件类型',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_app_micro_service` (`app_id`, `micro_service_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 9
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'k8s微应用元信息'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_micro_service_host
-- ----------------------------
DROP TABLE IF EXISTS `am_micro_service_host`;
CREATE TABLE `am_micro_service_host`
(
    `id`           bigint(20) UNSIGNED                                          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`   datetime(0)                                                  NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                  NOT NULL COMMENT '修改时间',
    `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 'namespace',
    `env_id`       varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '环境id',
    `app_id`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '应用ID',
    `ip`           varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 'ip',
    `deploy_type`  varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '部署方式，独占，共享',
    `service_id`   varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '微服务id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 50
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '微服务关联机器信息'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_micro_service_meta
-- ----------------------------
DROP TABLE IF EXISTS `am_micro_service_meta`;
CREATE TABLE `am_micro_service_meta`
(
    `id`                bigint(20) UNSIGNED                                            NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`        datetime(0)                                                    NOT NULL COMMENT '创建时间',
    `gmt_modified`      datetime(0)                                                    NULL DEFAULT NULL COMMENT '修改时间',
    `app_id`            varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NOT NULL COMMENT '应用标示',
    `micro_service_id`  varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '微服务标示',
    `name`              varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '微服务名称',
    `description`       varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述信息',
    `git_repo`          varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT 'git 地址',
    `branch`            varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '代码分支',
    `language`          varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL DEFAULT NULL COMMENT '开发语言',
    `service_type`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NOT NULL COMMENT '服务类型',
    `micro_service_ext` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NULL COMMENT '扩展信息 JSON',
    `commit`            varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT 'commit',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_app_micro_service` (`app_id`, `micro_service_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 398
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '应用微服务元信息'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_ms_port_resource
-- ----------------------------
DROP TABLE IF EXISTS `am_ms_port_resource`;
CREATE TABLE `am_ms_port_resource`
(
    `id`           bigint(20) UNSIGNED                                          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`   datetime(0)                                                  NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                  NOT NULL COMMENT '修改时间',
    `port`         int(10) UNSIGNED                                             NULL DEFAULT NULL COMMENT '端口',
    `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '环境隔离空间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 5
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '已使用port资源'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_namespace
-- ----------------------------
DROP TABLE IF EXISTS `am_namespace`;
CREATE TABLE `am_namespace`
(
    `id`                 bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`         datetime(0)                                                  NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`       datetime(0)                                                  NULL DEFAULT NULL COMMENT '最后修改时间',
    `namespace_id`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Namespace ID',
    `namespace_name`     varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Namespace Name',
    `namespace_creator`  varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Namespace 创建者',
    `namespace_modifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Namespace 最后修改者',
    `namespace_ext`      longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NULL COMMENT 'Namespace 扩展信息',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_namespace_id` (`namespace_id`) USING BTREE,
    INDEX `idx_namespace_name` (`namespace_name`) USING BTREE,
    INDEX `idx_namespace_creator` (`namespace_creator`) USING BTREE,
    INDEX `idx_namespace_modifier` (`namespace_modifier`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 15
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '命名空间'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for am_template
-- ----------------------------
DROP TABLE IF EXISTS `am_template`;
CREATE TABLE `am_template`
(
    `id`               bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`       datetime(0)                                                   NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`     datetime(0)                                                   NULL DEFAULT NULL COMMENT '最后修改时间',
    `template_id`      varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '模板全局唯一 ID',
    `template_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '模板版本',
    `template_type`    varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '模板类型',
    `template_name`    varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '模板名称',
    `template_path`    varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '存储位置相对路径',
    `template_ext`     longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '模板扩展信息',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_template_id_version` (`template_id`, `template_version`) USING BTREE,
    INDEX `idx_template_type` (`template_type`) USING BTREE,
    INDEX `idx_template_name` (`template_name`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE,
    INDEX `idx_gmt_modified` (`gmt_modified`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '应用模板'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for channel_task
-- ----------------------------
DROP TABLE IF EXISTS `channel_task`;
CREATE TABLE `channel_task`
(
    `id`             bigint(20) UNSIGNED                                           NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`     bigint(20)                                                    NULL DEFAULT NULL COMMENT '任务创建时间',
    `gmt_modified`   bigint(20)                                                    NULL DEFAULT NULL COMMENT '任务更新时间',
    `task_uuid`      varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '任务标识',
    `command`        mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL COMMENT '执行命令',
    `working_dir`    varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工作目录',
    `timeout`        bigint(20)                                                    NULL DEFAULT NULL COMMENT '超时时间',
    `exec_user`      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '执行用户',
    `task_status`    int(11)                                                       NULL DEFAULT NULL COMMENT '任务状态',
    `task_name`      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务名',
    `user_name`      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务发起者',
    `host_count`     int(11)                                                       NULL DEFAULT NULL COMMENT '机器总数',
    `task_remarks`   longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '任务备注',
    `end_time`       bigint(20)                                                    NULL DEFAULT NULL COMMENT '任务结束时间',
    `command_type`   int(11)                                                       NULL DEFAULT NULL COMMENT '命令类型',
    `start_time`     bigint(20)                                                    NULL DEFAULT NULL COMMENT '任务开始时间',
    `script_path`    varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上传文件存储路径',
    `script_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '上传文件内容',
    `end_point`      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务回调地址',
    `file_name`      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '脚本名',
    `file_mode`      int(11)                                                       NULL DEFAULT NULL COMMENT '文件权限',
    `exist_write`    tinyint(4)                                                    NULL DEFAULT NULL COMMENT '文件存在是否写入',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_task_uuid` (`task_uuid`) USING BTREE,
    INDEX `idx_user_status` (`user_name`, `task_status`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 281
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '通道任务表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for channel_task_host
-- ----------------------------
DROP TABLE IF EXISTS `channel_task_host`;
CREATE TABLE `channel_task_host`
(
    `id`            bigint(20) UNSIGNED                                           NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`    bigint(20)                                                    NULL DEFAULT NULL COMMENT 'host任务创建时间',
    `gmt_modified`  bigint(20)                                                    NULL DEFAULT NULL COMMENT 'host任务更新时间',
    `task_uuid`     varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务唯一标识',
    `host_uuid`     varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'host唯一标识',
    `host_status`   int(11)                                                       NULL DEFAULT NULL COMMENT 'host状态',
    `host_address`  varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'host地址',
    `host_type`     int(11)                                                       NULL DEFAULT NULL COMMENT 'host地址类型',
    `error_code`    varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '错误码',
    `error_message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '错误信息',
    `exit_code`     bigint(20)                                                    NULL DEFAULT NULL COMMENT '退出码',
    `end_time`      bigint(20)                                                    NULL DEFAULT NULL COMMENT 'host完成时间',
    `host_output`   longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '命令输出内容',
    `start_time`    bigint(20)                                                    NULL DEFAULT NULL COMMENT '任务开始时间',
    `agent_uuid`    varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'agent执行uuid',
    `stderr`        longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '错误输出',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task_status` (`task_uuid`, `host_status`) USING BTREE,
    INDEX `idx_host_uuid` (`host_uuid`) USING BTREE,
    INDEX `idx_agent_uuid` (`agent_uuid`) USING BTREE,
    INDEX `idx_task_host_status` (`task_uuid`, `host_uuid`, `host_status`) USING BTREE COMMENT 'task&host&status联合索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 422
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '通道任务host表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for shedlock
-- ----------------------------
DROP TABLE IF EXISTS `shedlock`;
CREATE TABLE `shedlock`
(
    `name`       varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `lock_until` timestamp(3)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `locked_at`  timestamp(3)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `locked_by`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    PRIMARY KEY (`name`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tc_dag
-- ----------------------------
DROP TABLE IF EXISTS `tc_dag`;
CREATE TABLE `tc_dag`
(
    `id`                   bigint(32)                                                    NOT NULL AUTO_INCREMENT,
    `gmt_create`           bigint(32)                                                    NOT NULL,
    `gmt_modified`         bigint(32)                                                    NOT NULL,
    `app_id`               varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'tesla',
    `name`                 varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `alias`                varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL,
    `content`              longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL,
    `input_params`         longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL,
    `has_feedback`         tinyint(1)                                                    NULL     DEFAULT 0,
    `has_history`          tinyint(1)                                                    NULL     DEFAULT 1,
    `description`          longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL,
    `entity`               longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL,
    `notice`               varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL,
    `creator`              varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL,
    `modifier`             varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL,
    `last_update_by`       varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'WEB',
    `ex_schedule_task_id`  varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL,
    `default_show_history` tinyint(1)                                                    NULL     DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `app_id_name` (`app_id`, `name`) USING BTREE,
    INDEX `app_id` (`app_id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE,
    INDEX `creator` (`creator`) USING BTREE,
    INDEX `modifier` (`modifier`) USING BTREE,
    INDEX `is_feedback` (`has_feedback`) USING BTREE,
    INDEX `has_history` (`has_history`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 81788
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tc_dag_config
-- ----------------------------
DROP TABLE IF EXISTS `tc_dag_config`;
CREATE TABLE `tc_dag_config`
(
    `id`           bigint(32)                                                    NOT NULL,
    `gmt_create`   bigint(32)                                                    NOT NULL,
    `gmt_modified` bigint(32)                                                    NOT NULL,
    `name`         varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `content`      longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL,
    `comment`      longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tc_dag_faas
-- ----------------------------
DROP TABLE IF EXISTS `tc_dag_faas`;
CREATE TABLE `tc_dag_faas`
(
    `id`              bigint(32)                                                    NOT NULL AUTO_INCREMENT,
    `gmt_create`      bigint(32)                                                    NOT NULL,
    `gmt_modified`    bigint(32)                                                    NOT NULL,
    `app_id`          varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `name`            varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `is_rsync_active` tinyint(1)                                                    NOT NULL DEFAULT 0,
    `rsync_detail`    longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL,
    `last_rsync_time` bigint(32)                                                    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE,
    INDEX `app_id` (`app_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tc_dag_inst
-- ----------------------------
DROP TABLE IF EXISTS `tc_dag_inst`;
CREATE TABLE `tc_dag_inst`
(
    `id`                           bigint(32)                                                    NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                   bigint(32)                                                    NOT NULL COMMENT '创建时间',
    `gmt_modified`                 bigint(32)                                                    NOT NULL COMMENT '修改时间',
    `gmt_access`                   bigint(32)                                                    NOT NULL COMMENT '上次调度的时间',
    `app_id`                       varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'tesla' COMMENT '启动的产品方',
    `dag_id`                       bigint(32)                                                    NOT NULL COMMENT 'dag的id，用于关联',
    `tc_dag_detail`                longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL COMMENT '启动时，将dag的全部信息拷贝过来',
    `status`                       varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行过程中的状态',
    `status_detail`                longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '状态对应的详细说明，一般是错误信息',
    `global_params`                longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '全局参数，提供给faas使用，允许用户修改',
    `global_object`                longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '全局对象，提供给faas使用，允许用户修改',
    `global_variable`              longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '全局变量，启动时提供的，不允许用户修改',
    `global_result`                longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '全局结果，key是node_id，value只有result和output，没有data，因为data太大了，使用data需要实时从执行侧获取',
    `lock_id`                      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '锁，分布式调度使用',
    `creator`                      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '启动人，工号',
    `is_sub`                       tinyint(1)                                                    NOT NULL DEFAULT 0 COMMENT '是否归属于另外一个dag',
    `tag`                          varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '标签，目前使用于entityValue',
    `ex_schedule_task_instance_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '外部调度的作业id',
    `standalone_ip`                varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '单独调度指定的机器ip',
    `evaluation_create_ret`        longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '评价服务创建后的反馈',
    `channel`                      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '调用渠道',
    `env`                          varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '调用环境',
    `drg_detail`                   longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_status` (`status`) USING BTREE,
    INDEX `idx_lock_id` (`lock_id`) USING BTREE,
    INDEX `idx_app_id` (`app_id`) USING BTREE,
    INDEX `idx_is_sub` (`is_sub`) USING BTREE,
    INDEX `idx_tag` (`tag`) USING BTREE,
    INDEX `idx_standalone_ip` (`standalone_ip`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 64317
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tc_dag_inst_edge
-- ----------------------------
DROP TABLE IF EXISTS `tc_dag_inst_edge`;
CREATE TABLE `tc_dag_inst_edge`
(
    `id`           bigint(32)                                                    NOT NULL AUTO_INCREMENT,
    `gmt_create`   bigint(32)                                                    NOT NULL,
    `gmt_modified` bigint(32)                                                    NOT NULL,
    `dag_inst_id`  bigint(32)                                                    NOT NULL,
    `source`       varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `target`       varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `label`        varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL,
    `shape`        varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `style`        longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL,
    `data`         longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL,
    `is_pass`      int(1)                                                        NULL     DEFAULT NULL,
    `exception`    longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL,
    `status`       varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'INIT',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `dag_inst_id` (`dag_inst_id`) USING BTREE,
    INDEX `is_pass` (`is_pass`) USING BTREE,
    INDEX `source` (`source`) USING BTREE,
    INDEX `target` (`target`) USING BTREE,
    INDEX `status` (`status`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 67036
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tc_dag_inst_node
-- ----------------------------
DROP TABLE IF EXISTS `tc_dag_inst_node`;
CREATE TABLE `tc_dag_inst_node`
(
    `id`                       bigint(32)                                                    NOT NULL AUTO_INCREMENT,
    `gmt_create`               bigint(32)                                                    NOT NULL,
    `gmt_modified`             bigint(32)                                                    NOT NULL,
    `gmt_start`                bigint(32)                                                    NOT NULL DEFAULT 999999999999999,
    `dag_inst_id`              bigint(32)                                                    NOT NULL,
    `node_id`                  varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `status`                   varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `status_detail`            longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL,
    `task_id`                  varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL,
    `stop_task_id`             varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL,
    `lock_id`                  varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL,
    `sub_dag_inst_id`          bigint(32)                                                    NULL     DEFAULT NULL,
    `tc_dag_or_node_detail`    longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL,
    `tc_dag_content_node_spec` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL,
    `retry_times`              bigint(32)                                                    NULL     DEFAULT 0,
    `drg_serial`               varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk` (`dag_inst_id`, `node_id`) USING BTREE,
    INDEX `dag_inst_id` (`dag_inst_id`) USING BTREE,
    INDEX `node_id` (`node_id`) USING BTREE,
    INDEX `status` (`status`) USING BTREE,
    INDEX `lock_id` (`lock_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 143950
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tc_dag_inst_node_std
-- ----------------------------
DROP TABLE IF EXISTS `tc_dag_inst_node_std`;
CREATE TABLE `tc_dag_inst_node_std`
(
    `id`               bigint(32)                                                    NOT NULL AUTO_INCREMENT,
    `gmt_create`       bigint(32)                                                    NOT NULL,
    `gmt_modified`     bigint(32)                                                    NOT NULL,
    `gmt_access`       bigint(32)                                                    NULL DEFAULT NULL,
    `status`           varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '状态 RUNNING/SUCCESS/EXCEPTION',
    `stdout`           longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '标准输出',
    `stderr`           longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '错误输出',
    `global_params`    longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '全局参数',
    `ip`               varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
    `standalone_ip`    varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
    `comment`          longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL,
    `is_stop`          tinyint(1)                                                    NULL DEFAULT NULL,
    `stop_id`          bigint(32)                                                    NULL DEFAULT NULL,
    `lock_id`          varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
    `dag_inst_node_id` bigint(32)                                                    NULL DEFAULT NULL,
    `dag_inst_id`      bigint(32)                                                    NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `ip` (`ip`) USING BTREE,
    INDEX `is_stop` (`is_stop`) USING BTREE,
    INDEX `status` (`status`) USING BTREE,
    INDEX `stop_id` (`stop_id`) USING BTREE,
    INDEX `idx_lock_id` (`lock_id`) USING BTREE,
    INDEX `idx_standalone_ip` (`standalone_ip`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 65668
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tc_dag_node
-- ----------------------------
DROP TABLE IF EXISTS `tc_dag_node`;
CREATE TABLE `tc_dag_node`
(
    `id`                 bigint(32)                                                    NOT NULL AUTO_INCREMENT,
    `gmt_create`         bigint(32)                                                    NOT NULL,
    `gmt_modified`       bigint(32)                                                    NOT NULL,
    `app_id`             varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '产品',
    `name`               varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称，产品下唯一',
    `alias`              varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '别名',
    `description`        longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '说明',
    `is_share`           tinyint(1)                                                    NOT NULL DEFAULT 0 COMMENT '是否共享',
    `input_params`       longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '输入参数',
    `output_params`      longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '输出参数',
    `type`               varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行类型 API FAAS TASK',
    `detail`             longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '根据不同的执行类型，有不同的内容',
    `is_show`            tinyint(1)                                                    NOT NULL DEFAULT 1 COMMENT '是否展示',
    `format_type`        varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '展示类型',
    `format_detail`      longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '展示详情',
    `creator`            varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '创建人',
    `modifier`           varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '修改人',
    `is_support_chatops` tinyint(1)                                                    NULL     DEFAULT 0 COMMENT '是否支持机器人',
    `chatops_detail`     longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '机器人展示详情',
    `last_update_by`     varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'WEB' COMMENT '最后由谁修改',
    `run_timeout`        bigint(32)                                                    NULL     DEFAULT NULL COMMENT '执行超时时间',
    `max_retry_times`    bigint(32)                                                    NULL     DEFAULT 0 COMMENT '重试次数上限，如果是-1则没有上限，0就是不重试',
    `retry_expression`   longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL COMMENT '重试判断，如果为空，则重试判断不通过',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk` (`app_id`, `name`) USING BTREE,
    INDEX `app_id` (`app_id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE,
    INDEX `is_share` (`is_share`) USING BTREE,
    INDEX `last_update_by` (`last_update_by`) USING BTREE,
    INDEX `creator` (`creator`) USING BTREE,
    INDEX `modifier` (`modifier`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 187255
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tc_dag_options
-- ----------------------------
DROP TABLE IF EXISTS `tc_dag_options`;
CREATE TABLE `tc_dag_options`
(
    `id`     bigint(32)                                                    NOT NULL AUTO_INCREMENT,
    `locale` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `name`   varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `value`  longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `locale_name` (`locale`, `name`(128)) USING BTREE,
    INDEX `locale` (`locale`) USING BTREE,
    INDEX `name` (`name`(191)) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 513734
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tc_dag_service_node
-- ----------------------------
DROP TABLE IF EXISTS `tc_dag_service_node`;
CREATE TABLE `tc_dag_service_node`
(
    `id`           bigint(32)                                                    NOT NULL AUTO_INCREMENT,
    `gmt_create`   bigint(32)                                                    NOT NULL,
    `gmt_modified` bigint(32)                                                    NOT NULL,
    `ip`           varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `enable`       tinyint(1)                                                    NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `ip` (`ip`) USING BTREE,
    INDEX `enable` (`enable`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 23
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

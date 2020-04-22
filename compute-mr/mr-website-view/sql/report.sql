SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for dimension_browser
-- ----------------------------
DROP TABLE IF EXISTS `dimension_browser`;
CREATE TABLE `dimension_browser`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `browser_name` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '浏览器名称',
  `browser_version` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '浏览器版本号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '浏览器维度信息表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of dimension_browser
-- ----------------------------
INSERT INTO `dimension_browser` VALUES (1, 'Chrome', '46.0.2490.71');
INSERT INTO `dimension_browser` VALUES (2, 'Chrome', 'all');
INSERT INTO `dimension_browser` VALUES (3, 'IE', '11.0');
INSERT INTO `dimension_browser` VALUES (4, 'IE', 'all');

-- ----------------------------
-- Table structure for dimension_currency_type
-- ----------------------------
DROP TABLE IF EXISTS `dimension_currency_type`;
CREATE TABLE `dimension_currency_type`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `currency_name` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '货币名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '支付货币类型维度信息表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of dimension_currency_type
-- ----------------------------
INSERT INTO `dimension_currency_type` VALUES (1, '测试ctd');
INSERT INTO `dimension_currency_type` VALUES (2, 'RMB');
INSERT INTO `dimension_currency_type` VALUES (3, 'all');

-- ----------------------------
-- Table structure for dimension_date
-- ----------------------------
DROP TABLE IF EXISTS `dimension_date`;
CREATE TABLE `dimension_date`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `year` int(11) NULL DEFAULT NULL,
  `season` int(11) NULL DEFAULT NULL,
  `month` int(11) NULL DEFAULT NULL,
  `week` int(11) NULL DEFAULT NULL,
  `day` int(11) NULL DEFAULT NULL,
  `calendar` date NULL DEFAULT NULL,
  `type` enum('year','season','month','week','day') CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '日期格式',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '时间维度信息表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of dimension_date
-- ----------------------------
INSERT INTO `dimension_date` VALUES (1, 2020, 4, 12, 50, 12, '2020-03-27', 'day');
INSERT INTO `dimension_date` VALUES (2, 2020, 4, 12, 51, 13, '2020-03-28', 'day');
INSERT INTO `dimension_date` VALUES (3, 2020, 4, 12, 51, 14, '2020-03-29', 'day');
INSERT INTO `dimension_date` VALUES (4, 2020, 4, 12, 51, 15, '2020-03-30', 'day');
INSERT INTO `dimension_date` VALUES (5, 2020, 4, 12, 50, 12, '2015-12-29', 'day');
INSERT INTO `dimension_date` VALUES (6, 2020, 4, 12, 50, 8, '2015-12-25', 'day');

-- ----------------------------
-- Table structure for dimension_event
-- ----------------------------
DROP TABLE IF EXISTS `dimension_event`;
CREATE TABLE `dimension_event`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '事件种类category',
  `action` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '事件action名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '事件维度信息表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of dimension_event
-- ----------------------------
INSERT INTO `dimension_event` VALUES (1, 'category', 'action');
INSERT INTO `dimension_event` VALUES (2, 'all', 'all');
INSERT INTO `dimension_event` VALUES (3, '订单事件', 'all');
INSERT INTO `dimension_event` VALUES (4, '订单事件', '订单产生');
INSERT INTO `dimension_event` VALUES (5, 'event的category名称', 'all');
INSERT INTO `dimension_event` VALUES (6, 'event的category名称', 'event的action名称');

-- ----------------------------
-- Table structure for dimension_inbound
-- ----------------------------
DROP TABLE IF EXISTS `dimension_inbound`;
CREATE TABLE `dimension_inbound`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parent_id` int(11) NULL DEFAULT NULL COMMENT '父级外链id',
  `name` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '外链名称',
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '外链url',
  `type` int(11) NULL DEFAULT NULL COMMENT '外链类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '外链源数据维度信息表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of dimension_inbound
-- ----------------------------
INSERT INTO `dimension_inbound` VALUES (1, NULL, 'all', NULL, 1);
INSERT INTO `dimension_inbound` VALUES (2, NULL, '其他外链', NULL, 1);
INSERT INTO `dimension_inbound` VALUES (3, NULL, 'baidu', 'www.baidu.com', 0);
INSERT INTO `dimension_inbound` VALUES (4, NULL, 'google', 'www.google.*', 0);
INSERT INTO `dimension_inbound` VALUES (5, NULL, '搜狗', 'www.sogou.com', 0);
INSERT INTO `dimension_inbound` VALUES (6, NULL, 'yahoo', '[\\w|.]+.yahoo.com', 0);
INSERT INTO `dimension_inbound` VALUES (7, NULL, '搜搜', 'www.soso.com', 0);
INSERT INTO `dimension_inbound` VALUES (8, NULL, '114', 'so.114.com.cn', 0);
INSERT INTO `dimension_inbound` VALUES (9, NULL, '有道', 'www.youdao.com', 0);
INSERT INTO `dimension_inbound` VALUES (10, NULL, '狗狗搜索', 'www.gougou.hk', 0);
INSERT INTO `dimension_inbound` VALUES (11, NULL, 'bing', 'cn.bing.com', 0);
INSERT INTO `dimension_inbound` VALUES (12, NULL, '360搜索', 'www.360sosou.com', 0);
INSERT INTO `dimension_inbound` VALUES (13, NULL, '好搜', 'www.haosou.com', 0);

-- ----------------------------
-- Table structure for dimension_kpi
-- ----------------------------
DROP TABLE IF EXISTS `dimension_kpi`;
CREATE TABLE `dimension_kpi`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `kpi_name` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'kpi维度名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = 'kpi维度相关信息表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of dimension_kpi
-- ----------------------------
INSERT INTO `dimension_kpi` VALUES (1, 'hourly_active_user');
INSERT INTO `dimension_kpi` VALUES (2, 'sessions');
INSERT INTO `dimension_kpi` VALUES (3, 'hourly_sessions');
INSERT INTO `dimension_kpi` VALUES (4, 'hourly_sessions_length');
INSERT INTO `dimension_kpi` VALUES (5, 'view_depth_of_user');
INSERT INTO `dimension_kpi` VALUES (6, 'view_depth_of_session');

-- ----------------------------
-- Table structure for dimension_location
-- ----------------------------
DROP TABLE IF EXISTS `dimension_location`;
CREATE TABLE `dimension_location`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `country` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '国家名称',
  `province` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '省份名称',
  `city` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '城市名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '地域信息维度表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of dimension_location
-- ----------------------------
INSERT INTO `dimension_location` VALUES (1, 'china', 'all', 'all');
INSERT INTO `dimension_location` VALUES (2, 'china', 'shanghai', 'all');
INSERT INTO `dimension_location` VALUES (3, 'china', 'beijing', 'all');
INSERT INTO `dimension_location` VALUES (4, 'china', 'guangdong', 'all');
INSERT INTO `dimension_location` VALUES (5, 'china', 'guangdong', 'guangzhou');
INSERT INTO `dimension_location` VALUES (6, 'china', 'hongkong', 'all');
INSERT INTO `dimension_location` VALUES (7, 'all', 'all', 'all');

-- ----------------------------
-- Table structure for dimension_os
-- ----------------------------
DROP TABLE IF EXISTS `dimension_os`;
CREATE TABLE `dimension_os`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `os_name` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '操作系统名称',
  `os_version` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '操作系统版本号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '操作系统信息维度表' ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for dimension_payment_type
-- ----------------------------
DROP TABLE IF EXISTS `dimension_payment_type`;
CREATE TABLE `dimension_payment_type`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `payment_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '支付方式名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '支付方式维度信息表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of dimension_payment_type
-- ----------------------------
INSERT INTO `dimension_payment_type` VALUES (1, '测试ptd');
INSERT INTO `dimension_payment_type` VALUES (2, 'alipay');
INSERT INTO `dimension_payment_type` VALUES (3, 'weixinpay');
INSERT INTO `dimension_payment_type` VALUES (4, 'all');

-- ----------------------------
-- Table structure for dimension_platform
-- ----------------------------
DROP TABLE IF EXISTS `dimension_platform`;
CREATE TABLE `dimension_platform`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `platform_name` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '平台名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '平台维度信息表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of dimension_platform
-- ----------------------------
INSERT INTO `dimension_platform` VALUES (1, 'all');
INSERT INTO `dimension_platform` VALUES (2, 'website');
INSERT INTO `dimension_platform` VALUES (3, 'test');
INSERT INTO `dimension_platform` VALUES (4, 'test2');

-- ----------------------------
-- Table structure for event_info
-- ----------------------------
DROP TABLE IF EXISTS `event_info`;
CREATE TABLE `event_info`  (
  `event_dimension_id` int(11) NOT NULL DEFAULT 0,
  `key` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `value` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `times` int(11) NULL DEFAULT 0 COMMENT '触发次数',
  PRIMARY KEY (`event_dimension_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '描述event的属性信息，在本次项目中不会用到' ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for member_info
-- ----------------------------
DROP TABLE IF EXISTS `member_info`;
CREATE TABLE `member_info`  (
  `member_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '会员id，是一个最多32位的字母数字字符串',
  `last_visit_date` date NULL DEFAULT NULL COMMENT '最后访问日期',
  `created` date NULL DEFAULT NULL,
  PRIMARY KEY (`member_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of member_info
-- ----------------------------
INSERT INTO `member_info` VALUES ('beifengnet', '2020-03-30', '2020-03-30');
INSERT INTO `member_info` VALUES ('gerryliu', '2020-03-29', '2020-03-29');

-- ----------------------------
-- Table structure for order_info
-- ----------------------------
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info`  (
  `order_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `platform` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT 'unknown',
  `s_time` bigint(20) NULL DEFAULT NULL,
  `currency_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `payment_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `amount` int(11) NOT NULL DEFAULT 0 COMMENT '订单金额',
  PRIMARY KEY (`order_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '描述订单的相关信息，该table在本次项目中的主要目标就是为了去重数据' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of order_info
-- ----------------------------
INSERT INTO `order_info` VALUES ('oid121201', 'website', 1450002889919, 'RMB', 'alipay', 123);
INSERT INTO `order_info` VALUES ('oid121202', 'website', 1450002951538, 'RMB', 'weixinpay', 54);

-- ----------------------------
-- Table structure for stats_device_browser
-- ----------------------------
DROP TABLE IF EXISTS `stats_device_browser`;
CREATE TABLE `stats_device_browser`  (
  `date_dimension_id` int(11) NOT NULL,
  `platform_dimension_id` int(11) NOT NULL,
  `browser_dimension_id` int(11) NOT NULL DEFAULT 0,
  `active_users` int(11) NULL DEFAULT 0 COMMENT '活跃用户数',
  `new_install_users` int(11) NULL DEFAULT 0 COMMENT '新增用户数',
  `total_install_users` int(11) NULL DEFAULT 0 COMMENT '总用户数',
  `sessions` int(11) NULL DEFAULT 0 COMMENT '会话个数',
  `sessions_length` int(11) NULL DEFAULT 0 COMMENT '会话长度',
  `total_members` int(11) UNSIGNED NULL DEFAULT 0 COMMENT '总会员数',
  `active_members` int(11) UNSIGNED NULL DEFAULT 0 COMMENT '活跃会员数',
  `new_members` int(11) UNSIGNED NULL DEFAULT 0 COMMENT '新增会员数',
  `pv` int(11) NULL DEFAULT 0 COMMENT 'pv数',
  `created` date NULL DEFAULT NULL,
  PRIMARY KEY (`platform_dimension_id`, `date_dimension_id`, `browser_dimension_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '统计浏览器相关分析数据的统计表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of stats_device_browser
-- ----------------------------
INSERT INTO `stats_device_browser` VALUES (1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, '2020-03-29');
INSERT INTO `stats_device_browser` VALUES (1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, '2020-03-29');
INSERT INTO `stats_device_browser` VALUES (2, 1, 1, 1, 1, 2, 1, 475, 1, 0, 0, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (2, 1, 2, 1, 1, 2, 1, 475, 1, 0, 0, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (2, 1, 3, 1, 1, 1, 2, 475, 0, 0, 0, 1, '2020-03-29');
INSERT INTO `stats_device_browser` VALUES (2, 1, 4, 1, 1, 1, 2, 475, 0, 0, 0, 1, '2020-03-29');
INSERT INTO `stats_device_browser` VALUES (3, 1, 1, 0, 0, 0, 1, 1, 2, 0, 1, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (3, 1, 2, 0, 0, 0, 1, 1, 2, 0, 1, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (3, 1, 3, 0, 0, 0, 1, 101, 1, 0, 1, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (3, 1, 4, 0, 0, 0, 1, 101, 1, 0, 1, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (5, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, '2020-03-29');
INSERT INTO `stats_device_browser` VALUES (5, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, '2020-03-29');
INSERT INTO `stats_device_browser` VALUES (1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, '2020-03-29');
INSERT INTO `stats_device_browser` VALUES (1, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, '2020-03-29');
INSERT INTO `stats_device_browser` VALUES (2, 2, 1, 1, 1, 2, 2, 475, 1, 0, 0, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (2, 2, 2, 1, 1, 2, 2, 475, 1, 0, 0, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (2, 2, 3, 1, 1, 1, 2, 475, 0, 0, 0, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (2, 2, 4, 1, 1, 1, 2, 475, 0, 0, 0, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (3, 2, 1, 0, 0, 0, 1, 101, 2, 0, 1, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (3, 2, 2, 0, 0, 0, 1, 101, 2, 0, 1, 1, '2020-03-30');
INSERT INTO `stats_device_browser` VALUES (3, 2, 3, 0, 0, 0, 1, 101, 1, 0, 1, 1, '2015-12-14');
INSERT INTO `stats_device_browser` VALUES (3, 2, 4, 0, 0, 0, 1, 101, 1, 0, 1, 1, '2015-12-14');
INSERT INTO `stats_device_browser` VALUES (5, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, '2015-12-12');
INSERT INTO `stats_device_browser` VALUES (5, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, '2015-12-12');

-- ----------------------------
-- Table structure for stats_device_location
-- ----------------------------
DROP TABLE IF EXISTS `stats_device_location`;
CREATE TABLE `stats_device_location`  (
  `date_dimension_id` int(11) NOT NULL,
  `platform_dimension_id` int(11) NOT NULL,
  `location_dimension_id` int(11) NOT NULL DEFAULT 0,
  `active_users` int(11) NULL DEFAULT 0 COMMENT '活跃用户数',
  `sessions` int(11) NULL DEFAULT 0 COMMENT '会话个数',
  `bounce_sessions` int(11) NULL DEFAULT 0 COMMENT '跳出会话个数',
  `created` date NULL DEFAULT NULL,
  PRIMARY KEY (`platform_dimension_id`, `date_dimension_id`, `location_dimension_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '统计地域相关分析数据的统计表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of stats_device_location
-- ----------------------------
INSERT INTO `stats_device_location` VALUES (1, 1, 1, 1, 1, 1, '2020-03-28');
INSERT INTO `stats_device_location` VALUES (1, 1, 2, 1, 1, 1, '2020-03-28');
INSERT INTO `stats_device_location` VALUES (1, 1, 3, 1, 1, 1, '2020-03-28');
INSERT INTO `stats_device_location` VALUES (2, 1, 1, 2, 2, 2, '2020-03-29');
INSERT INTO `stats_device_location` VALUES (2, 1, 2, 2, 2, 2, '2020-03-29');
INSERT INTO `stats_device_location` VALUES (2, 1, 3, 2, 2, 2, '2020-03-29');
INSERT INTO `stats_device_location` VALUES (3, 1, 1, 1, 1, 0, '2020-03-30');
INSERT INTO `stats_device_location` VALUES (3, 1, 4, 1, 1, 0, '2020-03-30');
INSERT INTO `stats_device_location` VALUES (3, 1, 5, 1, 1, 0, '2020-03-30');
INSERT INTO `stats_device_location` VALUES (1, 2, 1, 1, 1, 1, '2020-03-28');
INSERT INTO `stats_device_location` VALUES (1, 2, 2, 1, 1, 1, '2020-03-28');
INSERT INTO `stats_device_location` VALUES (1, 2, 3, 1, 1, 1, '2020-03-28');
INSERT INTO `stats_device_location` VALUES (2, 2, 1, 2, 2, 2, '2020-03-29');
INSERT INTO `stats_device_location` VALUES (2, 2, 2, 2, 2, 2, '2020-03-29');
INSERT INTO `stats_device_location` VALUES (2, 2, 3, 2, 2, 2, '2020-03-29');
INSERT INTO `stats_device_location` VALUES (3, 2, 1, 1, 1, 0, '2020-03-30');
INSERT INTO `stats_device_location` VALUES (3, 2, 4, 1, 1, 0, '2020-03-30');
INSERT INTO `stats_device_location` VALUES (3, 2, 5, 1, 1, 0, '2020-03-30');

-- ----------------------------
-- Table structure for stats_event
-- ----------------------------
DROP TABLE IF EXISTS `stats_event`;
CREATE TABLE `stats_event`  (
  `platform_dimension_id` int(11) NOT NULL DEFAULT 0,
  `date_dimension_id` int(11) NOT NULL DEFAULT 0,
  `event_dimension_id` int(11) NOT NULL DEFAULT 0,
  `times` int(11) NULL DEFAULT 0 COMMENT '触发次数',
  `created` date NULL DEFAULT NULL,
  PRIMARY KEY (`platform_dimension_id`, `date_dimension_id`, `event_dimension_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '统计事件相关分析数据的统计表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of stats_event
-- ----------------------------
INSERT INTO `stats_event` VALUES (1, 2, 2, 1, '2020-03-29');
INSERT INTO `stats_event` VALUES (1, 2, 3, 1, '2020-03-29');
INSERT INTO `stats_event` VALUES (1, 2, 4, 1, '2020-03-29');
INSERT INTO `stats_event` VALUES (1, 6, 2, 2, '2020-03-25');
INSERT INTO `stats_event` VALUES (1, 6, 5, 2, '2020-03-29');
INSERT INTO `stats_event` VALUES (1, 6, 6, 2, '2020-03-29');
INSERT INTO `stats_event` VALUES (2, 2, 2, 1, '2020-03-29');
INSERT INTO `stats_event` VALUES (2, 2, 3, 1, '2020-03-29');
INSERT INTO `stats_event` VALUES (2, 2, 4, 1, '2020-03-29');
INSERT INTO `stats_event` VALUES (2, 6, 2, 2, '2020-03-25');
INSERT INTO `stats_event` VALUES (2, 6, 5, 2, '2020-03-25');
INSERT INTO `stats_event` VALUES (2, 6, 6, 2, '2020-03-25');

-- ----------------------------
-- Table structure for stats_hourly
-- ----------------------------
DROP TABLE IF EXISTS `stats_hourly`;
CREATE TABLE `stats_hourly`  (
  `platform_dimension_id` int(11) NOT NULL,
  `date_dimension_id` int(11) NOT NULL,
  `kpi_dimension_id` int(11) NOT NULL,
  `hour_00` int(11) NULL DEFAULT 0,
  `hour_01` int(11) NULL DEFAULT 0,
  `hour_02` int(11) NULL DEFAULT 0,
  `hour_03` int(11) NULL DEFAULT 0,
  `hour_04` int(11) NULL DEFAULT 0,
  `hour_05` int(11) NULL DEFAULT 0,
  `hour_06` int(11) NULL DEFAULT 0,
  `hour_07` int(11) NULL DEFAULT 0,
  `hour_08` int(11) NULL DEFAULT 0,
  `hour_09` int(11) NULL DEFAULT 0,
  `hour_10` int(11) NULL DEFAULT 0,
  `hour_11` int(11) NULL DEFAULT 0,
  `hour_12` int(11) NULL DEFAULT 0,
  `hour_13` int(11) NULL DEFAULT 0,
  `hour_14` int(11) NULL DEFAULT 0,
  `hour_15` int(11) NULL DEFAULT 0,
  `hour_16` int(11) NULL DEFAULT 0,
  `hour_17` int(11) NULL DEFAULT 0,
  `hour_18` int(11) NULL DEFAULT 0,
  `hour_19` int(11) NULL DEFAULT 0,
  `hour_20` int(11) NULL DEFAULT 0,
  `hour_21` int(11) NULL DEFAULT 0,
  `hour_22` int(11) NULL DEFAULT 0,
  `hour_23` int(11) NULL DEFAULT 0,
  `created` date NULL DEFAULT NULL,
  PRIMARY KEY (`platform_dimension_id`, `date_dimension_id`, `kpi_dimension_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '按小时统计信息的统计表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of stats_hourly
-- ----------------------------
INSERT INTO `stats_hourly` VALUES (1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, '2020-03-28');
INSERT INTO `stats_hourly` VALUES (1, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, '2020-03-29');
INSERT INTO `stats_hourly` VALUES (1, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, '2020-03-29');
INSERT INTO `stats_hourly` VALUES (1, 2, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 475, 0, 0, 0, 0, 0, '2020-03-29');
INSERT INTO `stats_hourly` VALUES (1, 5, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, '2020-03-28');
INSERT INTO `stats_hourly` VALUES (2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, '2020-03-28');
INSERT INTO `stats_hourly` VALUES (2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, '2020-03-29');
INSERT INTO `stats_hourly` VALUES (2, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, '2020-03-29');
INSERT INTO `stats_hourly` VALUES (2, 2, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 475, 0, 0, 0, 0, 0, '2020-03-29');
INSERT INTO `stats_hourly` VALUES (2, 5, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, '2020-03-28');

-- ----------------------------
-- Table structure for stats_inbound
-- ----------------------------
DROP TABLE IF EXISTS `stats_inbound`;
CREATE TABLE `stats_inbound`  (
  `platform_dimension_id` int(11) NOT NULL DEFAULT 0,
  `date_dimension_id` int(11) NOT NULL,
  `inbound_dimension_id` int(11) NOT NULL,
  `active_users` int(11) NULL DEFAULT 0 COMMENT '活跃用户数',
  `sessions` int(11) NULL DEFAULT 0 COMMENT '会话个数',
  `bounce_sessions` int(11) NULL DEFAULT 0 COMMENT '跳出会话个数',
  `created` date NULL DEFAULT NULL,
  PRIMARY KEY (`platform_dimension_id`, `date_dimension_id`, `inbound_dimension_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '统计外链信息的统计表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of stats_inbound
-- ----------------------------
INSERT INTO `stats_inbound` VALUES (1, 4, 1, 1, 2, 1, '2020-03-30');
INSERT INTO `stats_inbound` VALUES (1, 4, 3, 1, 1, 0, '2020-03-30');
INSERT INTO `stats_inbound` VALUES (1, 4, 13, 1, 1, 1, '2020-03-30');
INSERT INTO `stats_inbound` VALUES (2, 4, 1, 1, 2, 1, '2020-03-30');
INSERT INTO `stats_inbound` VALUES (2, 4, 3, 1, 1, 0, '2020-03-30');
INSERT INTO `stats_inbound` VALUES (2, 4, 13, 1, 1, 1, '2020-03-30');

-- ----------------------------
-- Table structure for stats_order
-- ----------------------------
DROP TABLE IF EXISTS `stats_order`;
CREATE TABLE `stats_order`  (
  `platform_dimension_id` int(11) NOT NULL DEFAULT 0,
  `date_dimension_id` int(11) NOT NULL DEFAULT 0,
  `currency_type_dimension_id` int(11) NOT NULL DEFAULT 0,
  `payment_type_dimension_id` int(11) NOT NULL DEFAULT 0,
  `orders` int(11) NULL DEFAULT 0 COMMENT '订单个数',
  `success_orders` int(11) NULL DEFAULT 0 COMMENT '成功支付的订单个数',
  `refund_orders` int(11) NULL DEFAULT 0 COMMENT '退款订单个数',
  `order_amount` int(11) NULL DEFAULT 0 COMMENT '订单金额',
  `revenue_amount` int(11) NULL DEFAULT 0 COMMENT '收入金额，也就是成功支付过的金额',
  `refund_amount` int(11) NULL DEFAULT 0 COMMENT '退款金额',
  `total_revenue_amount` int(11) NULL DEFAULT 0 COMMENT '迄今为止，总的订单交易额',
  `total_refund_amount` int(11) NULL DEFAULT 0 COMMENT '迄今为止，总的退款金额',
  `created` date NULL DEFAULT NULL,
  PRIMARY KEY (`platform_dimension_id`, `date_dimension_id`, `currency_type_dimension_id`, `payment_type_dimension_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '统计订单信息的统计表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of stats_order
-- ----------------------------
INSERT INTO `stats_order` VALUES (1, 2, 2, 2, 1, 0, 0, 123, 0, 0, 0, 0, '2020-03-28');
INSERT INTO `stats_order` VALUES (1, 2, 2, 3, 1, 0, 0, 54, 0, 0, 0, 0, '2020-03-30');
INSERT INTO `stats_order` VALUES (1, 2, 2, 4, 2, 0, 0, 177, 0, 0, 0, 0, '2020-03-30');
INSERT INTO `stats_order` VALUES (1, 2, 3, 2, 1, 0, 0, 123, 0, 0, 0, 0, '2020-03-30');
INSERT INTO `stats_order` VALUES (1, 2, 3, 3, 1, 0, 0, 54, 0, 0, 0, 0, '2020-03-30');
INSERT INTO `stats_order` VALUES (1, 2, 3, 4, 2, 0, 0, 177, 0, 0, 177, 0, '2020-03-30');
INSERT INTO `stats_order` VALUES (2, 2, 2, 2, 1, 1, 0, 123, 123, 0, 123, 0, '2020-03-30');
INSERT INTO `stats_order` VALUES (2, 2, 2, 3, 1, 1, 1, 54, 54, 54, 54, 54, '2020-03-30');
INSERT INTO `stats_order` VALUES (2, 2, 2, 4, 2, 0, 0, 177, 0, 0, 0, 0, '2020-03-30');
INSERT INTO `stats_order` VALUES (2, 2, 3, 2, 1, 0, 0, 123, 0, 0, 0, 0, '2020-03-30');
INSERT INTO `stats_order` VALUES (2, 2, 3, 3, 1, 0, 0, 54, 0, 0, 0, 0, '2020-03-30');
INSERT INTO `stats_order` VALUES (2, 2, 3, 4, 2, 0, 0, 177, 0, 0, 0, 0, '2020-03-30');

-- ----------------------------
-- Table structure for stats_user
-- ----------------------------
DROP TABLE IF EXISTS `stats_user`;
CREATE TABLE `stats_user`  (
  `date_dimension_id` int(11) NOT NULL,
  `platform_dimension_id` int(11) NOT NULL,
  `active_users` int(11) NULL DEFAULT 0 COMMENT '活跃用户数',
  `new_install_users` int(11) NULL DEFAULT 0 COMMENT '新增用户数',
  `total_install_users` int(11) NULL DEFAULT 0 COMMENT '总用户数',
  `sessions` int(11) NULL DEFAULT 0 COMMENT '会话个数',
  `sessions_length` int(11) NULL DEFAULT 0 COMMENT '会话长度',
  `total_members` int(11) UNSIGNED NULL DEFAULT 0 COMMENT '总会员数',
  `active_members` int(11) UNSIGNED NULL DEFAULT 0 COMMENT '活跃会员数',
  `new_members` int(11) UNSIGNED NULL DEFAULT 0 COMMENT '新增会员数',
  `created` date NULL DEFAULT NULL,
  PRIMARY KEY (`platform_dimension_id`, `date_dimension_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '统计用户基本信息的统计表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of stats_user
-- ----------------------------
INSERT INTO `stats_user` VALUES (1, 1, 1, 1, 1, 1, 1, 1, 1, 1, '2020-03-28');
INSERT INTO `stats_user` VALUES (2, 1, 2, 2, 3, 2, 475, 1, 0, 0, '2020-03-29');
INSERT INTO `stats_user` VALUES (3, 1, 0, 0, 0, 1, 101, 2, 0, 1, '2020-03-30');
INSERT INTO `stats_user` VALUES (5, 1, 1, 0, 0, 0, 0, 0, 0, 0, '2020-03-28');
INSERT INTO `stats_user` VALUES (1, 2, 1, 1, 1, 1, 1, 1, 1, 1, '2020-03-28');
INSERT INTO `stats_user` VALUES (2, 2, 2, 2, 3, 2, 475, 1, 0, 0, '2020-03-29');
INSERT INTO `stats_user` VALUES (3, 2, 0, 0, 0, 1, 101, 2, 0, 1, '2020-03-30');
INSERT INTO `stats_user` VALUES (5, 2, 1, 0, 0, 0, 0, 0, 0, 0, '2020-03-28');

-- ----------------------------
-- Table structure for stats_view_depth
-- ----------------------------
DROP TABLE IF EXISTS `stats_view_depth`;
CREATE TABLE `stats_view_depth`  (
  `platform_dimension_id` int(11) NOT NULL DEFAULT 0,
  `date_dimension_id` int(11) NOT NULL DEFAULT 0,
  `kpi_dimension_id` int(11) NOT NULL DEFAULT 0,
  `pv1` int(11) NULL DEFAULT 0,
  `pv2` int(11) NULL DEFAULT 0,
  `pv3` int(11) NULL DEFAULT 0,
  `pv4` int(11) NULL DEFAULT 0,
  `pv5_10` int(11) NULL DEFAULT 0,
  `pv10_30` int(11) NULL DEFAULT 0,
  `pv30_60` int(11) NULL DEFAULT 0,
  `pv60_plus` int(11) NULL DEFAULT 0,
  `created` date NULL DEFAULT NULL,
  PRIMARY KEY (`platform_dimension_id`, `date_dimension_id`, `kpi_dimension_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '统计用户浏览深度相关分析数据的统计表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of stats_view_depth
-- ----------------------------
INSERT INTO `stats_view_depth` VALUES (1, 2, 5, 2, 1, 0, 0, 0, 0, 0, 0, '2020-03-29');
INSERT INTO `stats_view_depth` VALUES (1, 2, 6, 2, 1, 0, 0, 0, 0, 0, 0, '2020-03-29');
INSERT INTO `stats_view_depth` VALUES (2, 2, 5, 2, 1, 0, 0, 0, 0, 0, 0, '2020-03-29');
INSERT INTO `stats_view_depth` VALUES (2, 2, 6, 2, 1, 0, 0, 0, 0, 0, 0, '2020-03-29');

SET FOREIGN_KEY_CHECKS = 1;

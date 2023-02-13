
DROP TABLE IF EXISTS `tc_dag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tc_dag` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT,
  `gmt_create` bigint(32) NOT NULL,
  `gmt_modified` bigint(32) NOT NULL,
  `app_id` varchar(128) NOT NULL DEFAULT 'tesla',
  `name` varchar(128) NOT NULL,
  `alias` varchar(128) DEFAULT NULL,
  `content` longtext NOT NULL,
  `input_params` longtext,
  `has_feedback` tinyint(1) DEFAULT '0',
  `has_history` tinyint(1) DEFAULT '1',
  `description` longtext,
  `entity` longtext,
  `notice` varchar(128) DEFAULT NULL,
  `creator` varchar(128) DEFAULT NULL,
  `modifier` varchar(128) DEFAULT NULL,
  `last_update_by` varchar(128) NOT NULL DEFAULT 'WEB',
  `ex_schedule_task_id` varchar(128) DEFAULT NULL,
  `default_show_history` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `app_id_name` (`app_id`,`name`) USING BTREE,
  KEY `app_id` (`app_id`) USING BTREE,
  KEY `name` (`name`) USING BTREE,
  KEY `creator` (`creator`) USING BTREE,
  KEY `modifier` (`modifier`) USING BTREE,
  KEY `is_feedback` (`has_feedback`) USING BTREE,
  KEY `has_history` (`has_history`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=81670 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tc_dag_config`
--

DROP TABLE IF EXISTS `tc_dag_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tc_dag_config` (
  `id` bigint(32) NOT NULL,
  `gmt_create` bigint(32) NOT NULL,
  `gmt_modified` bigint(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `content` longtext,
  `comment` longtext,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tc_dag_faas`
--

DROP TABLE IF EXISTS `tc_dag_faas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tc_dag_faas` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT,
  `gmt_create` bigint(32) NOT NULL,
  `gmt_modified` bigint(32) NOT NULL,
  `app_id` varchar(128) NOT NULL,
  `name` varchar(128) NOT NULL,
  `is_rsync_active` tinyint(1) NOT NULL DEFAULT '0',
  `rsync_detail` longtext,
  `last_rsync_time` bigint(32) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `name` (`name`) USING BTREE,
  KEY `app_id` (`app_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tc_dag_inst`
--

DROP TABLE IF EXISTS `tc_dag_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tc_dag_inst` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` bigint(32) NOT NULL COMMENT '创建时间',
  `gmt_modified` bigint(32) NOT NULL COMMENT '修改时间',
  `gmt_access` bigint(32) NOT NULL COMMENT '上次调度的时间',
  `app_id` varchar(128) NOT NULL DEFAULT 'tesla' COMMENT '启动的产品方',
  `dag_id` bigint(32) NOT NULL COMMENT 'dag的id，用于关联',
  `tc_dag_detail` longtext NOT NULL COMMENT '启动时，将dag的全部信息拷贝过来',
  `status` varchar(128) NOT NULL COMMENT '执行过程中的状态',
  `status_detail` longtext COMMENT '状态对应的详细说明，一般是错误信息',
  `global_params` longtext COMMENT '全局参数，提供给faas使用，允许用户修改',
  `global_object` longtext COMMENT '全局对象，提供给faas使用，允许用户修改',
  `global_variable` longtext COMMENT '全局变量，启动时提供的，不允许用户修改',
  `global_result` longtext COMMENT '全局结果，key是node_id，value只有result和output，没有data，因为data太大了，使用data需要实时从执行侧获取',
  `lock_id` varchar(128) DEFAULT NULL COMMENT '锁，分布式调度使用',
  `creator` varchar(128) NOT NULL COMMENT '启动人，工号',
  `is_sub` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否归属于另外一个dag',
  `tag` varchar(128) DEFAULT NULL COMMENT '标签，目前使用于entityValue',
  `ex_schedule_task_instance_id` varchar(128) DEFAULT NULL COMMENT '外部调度的作业id',
  `standalone_ip` varchar(128) DEFAULT NULL COMMENT '单独调度指定的机器ip',
  `evaluation_create_ret` longtext COMMENT '评价服务创建后的反馈',
  `channel` varchar(128) DEFAULT NULL COMMENT '调用渠道',
  `env` varchar(128) DEFAULT NULL COMMENT '调用环境',
  `drg_detail` longtext,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_lock_id` (`lock_id`) USING BTREE,
  KEY `idx_app_id` (`app_id`) USING BTREE,
  KEY `idx_is_sub` (`is_sub`) USING BTREE,
  KEY `idx_tag` (`tag`) USING BTREE,
  KEY `idx_standalone_ip` (`standalone_ip`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=57616 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tc_dag_inst_edge`
--

DROP TABLE IF EXISTS `tc_dag_inst_edge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tc_dag_inst_edge` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT,
  `gmt_create` bigint(32) NOT NULL,
  `gmt_modified` bigint(32) NOT NULL,
  `dag_inst_id` bigint(32) NOT NULL,
  `source` varchar(128) NOT NULL,
  `target` varchar(128) NOT NULL,
  `label` varchar(128) DEFAULT NULL,
  `shape` varchar(128) NOT NULL,
  `style` longtext NOT NULL,
  `data` longtext NOT NULL,
  `is_pass` int(1) DEFAULT NULL,
  `exception` longtext,
  `status` varchar(128) NOT NULL DEFAULT 'INIT',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `dag_inst_id` (`dag_inst_id`) USING BTREE,
  KEY `is_pass` (`is_pass`) USING BTREE,
  KEY `source` (`source`) USING BTREE,
  KEY `target` (`target`) USING BTREE,
  KEY `status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=60702 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tc_dag_inst_node`
--

DROP TABLE IF EXISTS `tc_dag_inst_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tc_dag_inst_node` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT,
  `gmt_create` bigint(32) NOT NULL,
  `gmt_modified` bigint(32) NOT NULL,
  `gmt_start` bigint(32) NOT NULL DEFAULT '999999999999999',
  `dag_inst_id` bigint(32) NOT NULL,
  `node_id` varchar(128) NOT NULL,
  `status` varchar(32) NOT NULL,
  `status_detail` longtext,
  `task_id` varchar(128) DEFAULT NULL,
  `stop_task_id` varchar(128) DEFAULT NULL,
  `lock_id` varchar(128) DEFAULT NULL,
  `sub_dag_inst_id` bigint(32) DEFAULT NULL,
  `tc_dag_or_node_detail` longtext NOT NULL,
  `tc_dag_content_node_spec` longtext NOT NULL,
  `retry_times` bigint(32) DEFAULT '0',
  `drg_serial` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk` (`dag_inst_id`,`node_id`) USING BTREE,
  KEY `dag_inst_id` (`dag_inst_id`) USING BTREE,
  KEY `node_id` (`node_id`) USING BTREE,
  KEY `status` (`status`) USING BTREE,
  KEY `lock_id` (`lock_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=130327 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tc_dag_inst_node_std`
--

DROP TABLE IF EXISTS `tc_dag_inst_node_std`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tc_dag_inst_node_std` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT,
  `gmt_create` bigint(32) NOT NULL,
  `gmt_modified` bigint(32) NOT NULL,
  `gmt_access` bigint(32) DEFAULT NULL,
  `status` varchar(32) NOT NULL COMMENT '状态 RUNNING/SUCCESS/EXCEPTION',
  `stdout` longtext COMMENT '标准输出',
  `stderr` longtext COMMENT '错误输出',
  `global_params` longtext COMMENT '全局参数',
  `ip` varchar(128) DEFAULT NULL,
  `standalone_ip` varchar(128) DEFAULT NULL,
  `comment` longtext,
  `is_stop` tinyint(1) DEFAULT NULL,
  `stop_id` bigint(32) DEFAULT NULL,
  `lock_id` varchar(128) DEFAULT NULL,
  `dag_inst_node_id` varchar(128) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `ip` (`ip`) USING BTREE,
  KEY `is_stop` (`is_stop`) USING BTREE,
  KEY `status` (`status`) USING BTREE,
  KEY `stop_id` (`stop_id`) USING BTREE,
  KEY `idx_lock_id` (`lock_id`) USING BTREE,
  KEY `idx_standalone_ip` (`standalone_ip`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=57545 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tc_dag_node`
--

DROP TABLE IF EXISTS `tc_dag_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tc_dag_node` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT,
  `gmt_create` bigint(32) NOT NULL,
  `gmt_modified` bigint(32) NOT NULL,
  `app_id` varchar(128) NOT NULL COMMENT '产品',
  `name` varchar(128) NOT NULL COMMENT '名称，产品下唯一',
  `alias` varchar(128) DEFAULT NULL COMMENT '别名',
  `description` longtext COMMENT '说明',
  `is_share` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否共享',
  `input_params` longtext COMMENT '输入参数',
  `output_params` longtext COMMENT '输出参数',
  `type` varchar(128) NOT NULL COMMENT '执行类型 API FAAS TASK',
  `detail` longtext COMMENT '根据不同的执行类型，有不同的内容',
  `is_show` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否展示',
  `format_type` varchar(128) DEFAULT NULL COMMENT '展示类型',
  `format_detail` longtext COMMENT '展示详情',
  `creator` varchar(128) DEFAULT NULL COMMENT '创建人',
  `modifier` varchar(128) DEFAULT NULL COMMENT '修改人',
  `is_support_chatops` tinyint(1) DEFAULT '0' COMMENT '是否支持机器人',
  `chatops_detail` longtext COMMENT '机器人展示详情',
  `last_update_by` varchar(128) NOT NULL DEFAULT 'WEB' COMMENT '最后由谁修改',
  `run_timeout` bigint(32) DEFAULT NULL COMMENT '执行超时时间',
  `max_retry_times` bigint(32) DEFAULT '0' COMMENT '重试次数上限，如果是-1则没有上限，0就是不重试',
  `retry_expression` longtext COMMENT '重试判断，如果为空，则重试判断不通过',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk` (`app_id`,`name`) USING BTREE,
  KEY `app_id` (`app_id`) USING BTREE,
  KEY `name` (`name`) USING BTREE,
  KEY `is_share` (`is_share`) USING BTREE,
  KEY `last_update_by` (`last_update_by`) USING BTREE,
  KEY `creator` (`creator`) USING BTREE,
  KEY `modifier` (`modifier`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=186818 DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `tc_dag_options`
--

DROP TABLE IF EXISTS `tc_dag_options`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tc_dag_options` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT,
  `locale` varchar(128) NOT NULL,
  `name` varchar(256) NOT NULL,
  `value` longtext,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `locale_name` (`locale`,`name`(128)) USING BTREE,
  KEY `locale` (`locale`) USING BTREE,
  KEY `name` (`name`(191)) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=512626 DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `tc_dag_service_node`
--

DROP TABLE IF EXISTS `tc_dag_service_node`;
CREATE TABLE `tc_dag_service_node` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT,
  `gmt_create` bigint(32) NOT NULL,
  `gmt_modified` bigint(32) NOT NULL,
  `ip` varchar(128) NOT NULL,
  `enable` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `ip` (`ip`) USING BTREE,
  KEY `enable` (`enable`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4;

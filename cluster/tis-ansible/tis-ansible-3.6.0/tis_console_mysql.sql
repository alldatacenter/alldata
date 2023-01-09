-- MySQL dump 10.13  Distrib 5.1.73, for redhat-linux-gnu (x86_64)
--
-- Host: 10.1.6.134    Database: tis_console
-- ------------------------------------------------------
-- Server version   5.5.37

--
-- Table structure for table `app_trigger_job_relation`
--

DROP TABLE  `app_trigger_job_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_trigger_job_relation` (
  `at_id` bigint(20) NOT NULL AUTO_INCREMENT comment 'at_id',
  `app_id` bigint(20) NOT NULL comment 'app_id',
  `job_type` tinyint(4) NOT NULL comment 'job_type',
  `job_id` bigint(20) NOT NULL comment 'job_id',
  `crontab` varchar(20) NOT NULL comment 'crontab',
  `gmt_create` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment 'gmt_create',
  `gmt_modified` datetime NOT NULL COMMENT 'gmt_modified',
  `project_name` varchar(40) NOT NULL comment 'project_name',
  `is_stop` char(1) NOT NULL DEFAULT 'Y' comment 'is_stop',
  PRIMARY KEY (`at_id`),
  KEY `application_idx_app_id` (`app_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `application`
--

DROP TABLE IF EXISTS `application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `application` (
  `app_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '应用id',
  `app_type` tinyint not null default 1,
  `project_name` varchar(40) NOT NULL COMMENT '应用名',
  `recept` varchar(30) NOT NULL COMMENT 'recept',
  `manager` varchar(30) DEFAULT NULL COMMENT 'manager',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_auto_deploy` char(1) NOT NULL DEFAULT 'N' COMMENT '自动部署',
  `work_flow_id` int(11) DEFAULT NULL,
  `dpt_id` int(11) NOT NULL COMMENT 'dpt_id',
  `dpt_name` varchar(50) DEFAULT NULL COMMENT 'dpt_name',
  `full_build_cron_time` varchar(50) DEFAULT 'full_build_cron_time',
  `last_process_time` datetime DEFAULT NULL,
  PRIMARY KEY (`app_id`),
  UNIQUE KEY `application_idx_projname_delete` (`project_name`),
  KEY `fk_ref2` (`dpt_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COMMENT='应用信息';
/*!40101 SET character_set_client = @saved_cs_client */;




--
-- Table structure for table `cluster_snapshot`
--

DROP TABLE IF EXISTS `cluster_snapshot`;
CREATE TABLE `cluster_snapshot` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT comment 'id',
  `gmt_create` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP comment 'gmt_create',
  `data_type` varchar(20) NOT NULL comment 'data_type',
  `incr_number` int(11) NOT NULL comment 'incr_number',
  `app_id` bigint(20) NOT NULL comment 'app_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `cluster_snapshot_idx_projname_delete` (`app_id`,`data_type`,`gmt_create`)
) ENGINE=MEMORY  DEFAULT CHARSET=utf8mb4 MAX_ROWS=100000000;


--
-- Table structure for table `cluster_snapshot_pre_day`
--

DROP TABLE IF EXISTS `cluster_snapshot_pre_day`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cluster_snapshot_pre_day` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT comment 'id',
  `gmt_create` date NOT NULL comment 'gmt_create',
  `data_type` varchar(20) NOT NULL comment 'data_type',
  `incr_number` int(11) NOT NULL comment 'incr_number',
  `app_id` bigint(20) NOT NULL comment 'app_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `cluster_snapshot_pre_day_idx_projname_delete` (`app_id`,`data_type`,`gmt_create`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;



--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `department` (
  `dpt_id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'dpt_id',
  `parent_id` int(11) DEFAULT NULL COMMENT 'parent_id',
  `name` varchar(30) NOT NULL COMMENT 'name',
  `gmt_create` datetime NOT NULL COMMENT 'gmt_create',
  `gmt_modified` datetime NOT NULL COMMENT 'gmt_modified',
  `full_name` varchar(100) NOT NULL COMMENT 'full_name',
  `leaf` char(1) NOT NULL COMMENT 'leaf',
  `template_flag` int(11) DEFAULT NULL COMMENT 'template_flag',
  PRIMARY KEY (`dpt_id`),
  UNIQUE KEY `normal222g` (`full_name`),
  KEY `fk_reference_1` (`parent_id`),
  KEY `normal1` (`leaf`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

-- insert into department (dpt_id,parent_id,name,gmt_create,gmt_modified,full_name,leaf)
-- values(1,-1,'tis',now(),now(),'tis','N');

-- insert into department (dpt_id,parent_id,name,gmt_create,gmt_modified,full_name,leaf)
-- values(2,1,'default',now(),now(),'/tis/default','Y');


--
-- Table structure for table `nums`
--

DROP TABLE IF EXISTS `nums`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nums` (
  `a` int(11) NOT NULL comment 'a'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

insert into nums(a) values(1);
insert  into nums
select a + 1 from nums;
insert  into nums
select a + 2 from nums;
insert  into nums
select a + 4 from nums;
insert  into nums
select a + 8 from nums;
insert  into nums
select a + 16 from nums;
insert  into nums
select a + 32 from nums;
insert  into nums
select a + 64 from nums;
insert  into nums
select a + 128 from nums;

--
-- Table structure for table `operation_log`
--

DROP TABLE IF EXISTS `operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `operation_log` (
  `op_id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'op_id',
  `usr_id` varchar(30) NOT NULL COMMENT 'usr_id',
  `usr_name` varchar(30) NOT NULL COMMENT 'usr_name',
  `op_type` varchar(30) NOT NULL COMMENT 'op_type',
  `op_desc` longtext NOT NULL COMMENT 'op_desc',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create_time',
  `tab_name` varchar(30) DEFAULT NULL COMMENT 'tab_name',
  `app_name` varchar(40) DEFAULT NULL COMMENT 'app_name',
  `runtime` smallint(6) DEFAULT NULL COMMENT 'runtim',
  `memo` varchar(256) DEFAULT NULL COMMENT 'memo',
  PRIMARY KEY (`op_id`),
  KEY `fk_reference_14` (`usr_id`),
  KEY `fk_reference_15` (`create_time`),
  KEY `operation_log_history_list` (`tab_name`,`op_type`,`app_name`,`runtime`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COMMENT='operation_log';
/*!40101 SET character_set_client = @saved_cs_client */;



--
-- Table structure for table `resource_parameters`
--

DROP TABLE IF EXISTS `resource_parameters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_parameters` (
  `rp_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '..',
  `key_name` varchar(40) NOT NULL COMMENT '...',
  `daily_value` varchar(100) DEFAULT NULL COMMENT '...',
  `ready_value` varchar(100) DEFAULT NULL COMMENT '...',
  `online_value` varchar(100) DEFAULT NULL COMMENT '...',
  `param_desc` varchar(200) DEFAULT NULL COMMENT 'param_desc 描述',
  `gmt_create` datetime NOT NULL COMMENT '...',
  `gmt_update` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '...',
  PRIMARY KEY (`rp_id`),
  UNIQUE KEY `resource_parameters` (`key_name`),
  KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COMMENT='resource_parameters';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `server_group`
--
DROP TABLE IF EXISTS `server_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `server_group` (
  `gid` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` int(11) NOT NULL COMMENT 'appid',
  `runt_environment` smallint(6) NOT NULL COMMENT '针对应用类型：0：日常 1 daily 2：线上',
  `group_index` smallint(6) NOT NULL COMMENT 'group_index',
  `publish_snapshot_id` int(11) DEFAULT NULL COMMENT '被发布的快照版本',
  `create_time` datetime NOT NULL COMMENT 'create_time',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'update_time',
  `is_deleted` char(1) NOT NULL DEFAULT 'N' COMMENT 'is_deleted',
  PRIMARY KEY (`gid`),
  UNIQUE KEY `uniq_idx_appname_runtime_group_index` (`app_id`,`runt_environment`,`group_index`),
  KEY `fk_reference_3` (`app_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COMMENT='server_group';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `snapshot`
--

DROP TABLE IF EXISTS `snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `snapshot` (
  `sn_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'sn_id',
  `create_time` datetime NOT NULL COMMENT 'create_time',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'update_time',
  `app_id` int(11) NOT NULL COMMENT 'app_id',
  `res_schema_id` bigint(20) NOT NULL COMMENT 'res_schema_id',
  `res_solr_id` bigint(20) NOT NULL COMMENT 'res_solr_id',
  `res_jar_id` bigint(20) DEFAULT NULL COMMENT 'res_jar_id',
  `res_core_prop_id` bigint(20) DEFAULT NULL COMMENT 'res_core_prop_id',
  `res_ds_id` bigint(20) DEFAULT NULL COMMENT 'res_ds_id',
  `res_application_id` bigint(20) DEFAULT NULL COMMENT 'res_application_id',
  `create_user_id` bigint(20) NOT NULL COMMENT '...',
  `create_user_name` varchar(30) NOT NULL COMMENT '...',
  `pre_sn_id` int(11) NOT NULL COMMENT '...',
  `memo` varchar(256) DEFAULT NULL COMMENT '...',
  `biz_id` int(11) DEFAULT NULL COMMENT '绑定的业务线id',
  PRIMARY KEY (`sn_id`),
  KEY `app_id` (`app_id`),
  KEY `snapshop` (`sn_id`,`res_schema_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COMMENT='记录当前发布的记录，对以下记录改动之后都会新添加一条记录1 配置文件修改2 代码包上传之后的';
/*!40101 SET character_set_client = @saved_cs_client */;



--
-- Table structure for table `table_dump`
--

DROP TABLE IF EXISTS `table_dump`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `table_dump` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT comment 'id',
  `datasource_table_id` int(11) NOT NULL comment 'datasource_table_id',
  `hive_table_name` varchar(50) DEFAULT NULL comment 'hive_table_name',
  `state` tinyint(1) DEFAULT NULL COMMENT 'state',
  `info` text COMMENT 'info',
  `is_valid` tinyint(1) NOT NULL DEFAULT '1' comment 'is_valid',
  `create_time` datetime DEFAULT NULL comment 'create_time',
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment 'op_time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_table_dump_hive_table_name` (`hive_table_name`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `trigger_job`
--

DROP TABLE IF EXISTS `trigger_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trigger_job` (
  `job_id` bigint(20) NOT NULL AUTO_INCREMENT comment 'job_id',
  `domain` varchar(10) DEFAULT NULL comment 'domain',
  `crontab` varchar(30) NOT NULL comment 'crontab',
  `gmt_create` datetime NOT NULL comment 'gmt_create',
  `gmt_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment 'gmt_modified',
  `is_stop` char(1) NOT NULL DEFAULT 'Y' comment 'is_stop',
  `is_stop_ready` char(1) NOT NULL DEFAULT 'Y' comment 'is_stop_ready',
  PRIMARY KEY (`job_id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_domain_gmt` (`domain`,`gmt_modified`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `upload_resource`
--

DROP TABLE IF EXISTS `upload_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `upload_resource` (
  `ur_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resource_type` varchar(25) NOT NULL COMMENT 'resource_type',
  `md5_code` char(32) NOT NULL COMMENT 'md5_code',
  `content` mediumblob NOT NULL COMMENT 'content',
  `create_time` datetime NOT NULL COMMENT 'create_time',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'update_time',
  `memo` varchar(200) DEFAULT NULL COMMENT 'memo',
  PRIMARY KEY (`ur_id`),
  KEY `upload_resource_idx_res_type` (`resource_type`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COMMENT='upload_resource';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usr_dpt_relation`
--

DROP TABLE IF EXISTS `usr_dpt_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `usr_dpt_relation` (
  `usr_id` varchar(40) NOT NULL comment 'usr_id',
  `dpt_id` bigint(20) NOT NULL comment 'dpt_id',
  `dpt_name` varchar(100) NOT NULL comment 'dpt_name',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment 'create_time',
  `update_time` datetime NOT NULL COMMENT 'update_time',
  `user_name` varchar(100) NOT NULL comment 'user_name',
  `real_name` varchar(32) DEFAULT NULL comment 'real_name',
  `pass_word` varchar(32) DEFAULT NULL comment 'password',
  `r_id` bigint(20) NOT NULL comment 'r_id',
  `role_name` varchar(20) DEFAULT NULL comment 'role_name',
  `extra_dpt_relation` char(1) NOT NULL DEFAULT 'N' comment 'extra_dpt_relation' ,
  PRIMARY KEY (`usr_id`),
  UNIQUE KEY `idx_user_name_usr_dpt_relation` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;
-- add admin
-- insert into usr_dpt_relation(
-- usr_id,dpt_id,dpt_name,create_time,update_time,user_name,real_name,pass_word,r_id,extra_dpt_relation)
-- values('d84d8eafba5b436295e28153189b997a',-1,'none',now(),now(),'admin','Admin','e10adc3949ba59abbe56e057f20f883e',-1,'N');



--
-- Table structure for table `work_flow`
--

DROP TABLE IF EXISTS `work_flow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `work_flow` (
  `id` int(11) NOT NULL AUTO_INCREMENT comment 'id',
  `name` varchar(50) DEFAULT NULL comment 'name',
  `op_user_id` int(11) NOT NULL comment 'op_user_id',
  `op_user_name` varchar(50) CHARACTER SET utf8 DEFAULT NULL comment 'op_user_name',
  `git_path` varchar(50) NOT NULL comment 'git_path',
  `in_change` tinyint(4) NOT NULL DEFAULT '0'  comment 'in_change',
  `create_time` datetime DEFAULT NULL comment 'create_time',
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment 'op_time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 ;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `work_flow_build_history`
--

DROP TABLE IF EXISTS `work_flow_build_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `work_flow_build_history` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT comment 'id',
  `start_time` datetime DEFAULT NULL comment 'start_time',
  `end_time` datetime DEFAULT NULL comment 'end_time',
  `state` tinyint(4) DEFAULT NULL comment 'state',
  `trigger_type` tinyint(1) DEFAULT NULL comment 'trigger_type',
  `op_user_id` int(11) DEFAULT NULL comment 'op_user_id',
  `op_user_name` varchar(50) DEFAULT NULL comment 'op_user_name',
  `app_id` int(11) DEFAULT NULL comment 'app_id',
  `app_name` varchar(40) DEFAULT NULL comment 'app_name',
  `start_phase` tinyint(4) DEFAULT NULL COMMENT '1:dump 2:join 3 build 4 backflow ',
  `history_id` int(11) DEFAULT NULL comment 'history_id',
  `work_flow_id` int(11) DEFAULT NULL comment 'work_flow_id',
  `create_time` datetime DEFAULT NULL comment 'create_time',
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment 'op_time',
  `end_phase` tinyint(4) DEFAULT NULL comment 'end_phase',
  `last_ver` smallint(4) DEFAULT 0 comment 'last_ver',
  `asyn_sub_task_status` text COMMENT 'asyn_sub_task_status',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `work_flow_build_phase`
--

DROP TABLE IF EXISTS `work_flow_build_phase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `work_flow_build_phase` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT comment 'id',
  `work_flow_build_history_id` int(11) DEFAULT NULL comment 'work_flow_build_history_id' ,
  `phase` tinyint(4) DEFAULT NULL COMMENT 'phase index',
  `result` tinyint(1) DEFAULT NULL comment 'result',
  `phase_info` text COMMENT 'phase status',
  `create_time` datetime DEFAULT NULL comment 'create_time',
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment 'op_time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `work_flow_publish_history`
--

DROP TABLE IF EXISTS `work_flow_publish_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `work_flow_publish_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT comment 'id',
  `create_time` datetime DEFAULT NULL comment 'create_time',
  `op_user_id` int(11) DEFAULT NULL comment 'op_user_id',
  `op_user_name` varchar(50) CHARACTER SET utf8 DEFAULT NULL comment 'op_user_name',
  `workflow_id` int(11) DEFAULT NULL comment 'workflow_id',
  `workflow_name` varchar(50) DEFAULT NULL comment 'workflow_name' ,
  `publish_state` tinyint(4) DEFAULT NULL comment 'publish_state',
  `type` tinyint(4) DEFAULT NULL COMMENT 'type' ,
  `publish_reason` text CHARACTER SET utf8mb4  comment 'publish_reason',
  `git_sha1` varchar(40) DEFAULT NULL comment 'git_sha1',
  `in_use` tinyint(1) NOT NULL DEFAULT '0' comment 'in_use',
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment 'op_time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

DROP TABLE IF EXISTS `datasource_db`;
CREATE TABLE `datasource_db` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `extend_class` varchar(256) NOT NULL,
  `sync_online` tinyint(4) NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `datasource_table`;
CREATE TABLE `datasource_table` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `db_id` int(11) NOT NULL,
  `sync_online` tinyint(4) NOT NULL DEFAULT '0',
  `git_tag` varchar(50) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;


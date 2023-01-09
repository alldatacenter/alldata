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
  `at_id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  `app_id` bigint NOT NULL ,
  `job_type` smallint NOT NULL ,
  `job_id` bigint NOT NULL ,
  `crontab` varchar(20) NOT NULL ,
  `gmt_create` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP  ,
  `gmt_modified` timestamp NOT NULL ,
  `project_name` varchar(40) NOT NULL ,
  `is_stop` char(1) NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`at_id`)
);

CREATE INDEX application_idx_app_id ON app_trigger_job_relation (app_id);

--
-- Table structure for table `application`
--

DROP TABLE IF EXISTS `application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `application` (
  `app_id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  `app_type` smallint not null default 1,
  `project_name` varchar(40) NOT NULL ,
  `recept` varchar(30) NOT NULL ,
  `manager` varchar(30) DEFAULT NULL ,
  `create_time` timestamp NOT NULL ,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `is_auto_deploy` char(1) NOT NULL DEFAULT 'N' ,
  `work_flow_id` bigint DEFAULT NULL,
  `dpt_id` bigint NOT NULL ,
  `dpt_name` varchar(50) DEFAULT NULL ,
  `full_build_cron_time` varchar(50) DEFAULT 'full_build_cron_time',
  `last_process_time` timestamp DEFAULT NULL,
  PRIMARY KEY (`app_id`)
) ;
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE UNIQUE INDEX application_idx_projname_delete ON application (project_name);
CREATE  INDEX application_idx_fk_ref2 ON application (dpt_id);


--
-- Table structure for table `cluster_snapshot`
--

DROP TABLE IF EXISTS `cluster_snapshot`;
CREATE TABLE `cluster_snapshot` (
  `id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  `gmt_create` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `data_type` varchar(20) NOT NULL ,
  `incr_number` bigint NOT NULL ,
  `app_id` bigint NOT NULL ,
  PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX cluster_snapshot_idx_projname_delete ON cluster_snapshot (`app_id`,`data_type`,`gmt_create`);

--
-- Table structure for table `cluster_snapshot_pre_day`
--

DROP TABLE IF EXISTS `cluster_snapshot_pre_day`;

CREATE TABLE `cluster_snapshot_pre_day` (
  `id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  `gmt_create` date NOT NULL ,
  `data_type` varchar(20) NOT NULL ,
  `incr_number` bigint NOT NULL ,
  `app_id` bigint NOT NULL ,
  PRIMARY KEY (`id`)
) ;


CREATE UNIQUE INDEX cluster_snapshot_pre_day_idx_projname_delete ON cluster_snapshot (`app_id`,`data_type`,`gmt_create`);


--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `department` (
  `dpt_id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  `parent_id` bigint DEFAULT NULL ,
  `name` varchar(30) NOT NULL ,
  `gmt_create` timestamp NOT NULL ,
  `gmt_modified` timestamp NOT NULL ,
  `full_name` varchar(100) NOT NULL ,
  `leaf` char(1) NOT NULL ,
  `template_flag` smallint DEFAULT NULL ,
  PRIMARY KEY (`dpt_id`)
) ;
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE UNIQUE INDEX idx_department_full_name ON department (`full_name`);
CREATE INDEX idx_department_fk_reference_parent_id ON department (`parent_id`);
CREATE INDEX idx_department_leaf ON department (`leaf`);


--
-- Table structure for table `nums`
--

DROP TABLE IF EXISTS `nums`;

CREATE TABLE `nums` (
  `a` smallint NOT NULL
);


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
  `op_id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  `usr_id` varchar(30) NOT NULL ,
  `usr_name` varchar(30) NOT NULL ,
  `op_type` varchar(30) NOT NULL ,
  `op_desc` CLOB NOT NULL ,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `tab_name` varchar(30) DEFAULT NULL ,
  `app_name` varchar(40) DEFAULT NULL ,
  `runtime` smallint DEFAULT NULL,
  `memo` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`op_id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE  INDEX idx_operation_log_usr_id ON operation_log (`usr_id`);
CREATE  INDEX idx_operation_log_create_time ON operation_log (`create_time`);
CREATE  INDEX idx_operation_log_log_history_list ON operation_log (`tab_name`,`op_type`,`app_name`,`runtime`);

--
-- Table structure for table `resource_parameters`
--

--DROP TABLE IF EXISTS `resource_parameters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
-- CREATE TABLE `resource_parameters` (
--   `rp_id` bigint(20) NOT NULL AUTO_INCREMENT ,
--   `key_name` varchar(40) NOT NULL ,
--   `daily_value` varchar(100) DEFAULT NULL ,
--   `ready_value` varchar(100) DEFAULT NULL ,
--   `online_value` varchar(100) DEFAULT NULL ,
--   `param_desc` varchar(200) DEFAULT NULL ,
--   `gmt_create` timestamp NOT NULL ,
--   `gmt_update` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
--   PRIMARY KEY (`rp_id`)
-- ) ;
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `server_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `server_group` (
  `gid` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  `app_id` bigint NOT NULL ,
  `runt_environment` smallint NOT NULL ,
  `group_index` smallint NOT NULL ,
  `publish_snapshot_id` bigint DEFAULT NULL ,
  `create_time` timestamp NOT NULL ,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `is_deleted` char(1) NOT NULL DEFAULT 'N',
  PRIMARY KEY (`gid`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE UNIQUE INDEX uniq_idx_server_group_app_id_runt_environment_group_index ON server_group (`app_id`,`runt_environment`,`group_index`);
CREATE INDEX  uniq_idx_server_group_app_id ON server_group (`app_id`);
--
-- Table structure for table `snapshot`
--

DROP TABLE IF EXISTS `snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `snapshot` (
  `sn_id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  `create_time` timestamp NOT NULL ,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `app_id` bigint NOT NULL ,
  `res_schema_id` bigint NOT NULL ,
  `res_solr_id` bigint NOT NULL ,
  `res_jar_id` bigint DEFAULT NULL ,
  `res_core_prop_id` bigint DEFAULT NULL ,
  `res_ds_id` bigint DEFAULT NULL ,
  `res_application_id` bigint DEFAULT NULL ,
  `create_user_id` bigint NOT NULL ,
  `create_user_name` varchar(30) NOT NULL ,
  `pre_sn_id` bigint NOT NULL ,
  `memo` varchar(256) DEFAULT NULL ,
  `biz_id` bigint DEFAULT NULL ,
  PRIMARY KEY (`sn_id`)
) ;
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE INDEX uniq_idx_snapshot_app_id ON snapshot (`app_id`);
CREATE INDEX uniq_idx_snapshot_snapshop ON snapshot (`sn_id`,`res_schema_id`);

--
-- Table structure for table `table_dump`
--

DROP TABLE IF EXISTS `table_dump`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `table_dump` (
  `id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  `datasource_table_id` bigint NOT NULL ,
  `hive_table_name` varchar(50) DEFAULT NULL ,
  `state` smallint DEFAULT NULL ,
  `info` clob ,
  `is_valid` smallint NOT NULL DEFAULT 1 ,
  `create_time` timestamp DEFAULT NULL ,
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
CREATE UNIQUE INDEX uniq_idx_table_dump_hive_table_name ON table_dump (`hive_table_name`);

--
-- Table structure for table `trigger_job`
--

DROP TABLE IF EXISTS `trigger_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trigger_job` (
  `job_id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  `domain` varchar(10) DEFAULT NULL ,
  `crontab` varchar(30) NOT NULL ,
  `gmt_create` timestamp NOT NULL ,
  `gmt_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `is_stop` char(1) NOT NULL DEFAULT 'Y' ,
  `is_stop_ready` char(1) NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`job_id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE INDEX uniq_idx_trigger_job_job_id ON trigger_job (`job_id`);
CREATE INDEX uniq_idx_trigger_job_idx_domain_gmt ON trigger_job (`domain`,`gmt_modified`);
--
-- Table structure for table `upload_resource`
--

DROP TABLE IF EXISTS `upload_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `upload_resource` (
  `ur_id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  `resource_type` varchar(25) NOT NULL ,
  `md5_code` char(32) NOT NULL ,
  `content` blob NOT NULL ,
  `create_time` timestamp NOT NULL ,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `memo` varchar(200) DEFAULT NULL ,
  PRIMARY KEY (`ur_id`)
);

CREATE INDEX upload_resource_idx_res_type ON upload_resource (`resource_type`);
--
-- Table structure for table `usr_dpt_relation`
--

DROP TABLE IF EXISTS `usr_dpt_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `usr_dpt_relation` (
  `usr_id` varchar(40) NOT NULL ,
  `dpt_id` bigint NOT NULL ,
  `dpt_name` varchar(100) NOT NULL ,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `update_time` timestamp NOT NULL ,
  `user_name` varchar(100) NOT NULL ,
  `real_name` varchar(32) DEFAULT NULL ,
  `pass_word` varchar(32) DEFAULT NULL ,
  `r_id` bigint NOT NULL ,
  `role_name` varchar(20) DEFAULT NULL ,
  `extra_dpt_relation` char(1) NOT NULL DEFAULT 'N' ,
  PRIMARY KEY (`usr_id`)
) ;

CREATE INDEX idx_user_name_usr_dpt_relation ON usr_dpt_relation (`user_name`);


--
-- Table structure for table `work_flow`
--

DROP TABLE IF EXISTS `work_flow`;

CREATE TABLE `work_flow` (
  `id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  `name` varchar(50) DEFAULT NULL ,
  `op_user_id` int NOT NULL ,
  `op_user_name` varchar(50) DEFAULT NULL ,
  `git_path` varchar(50) NOT NULL ,
  `in_change` smallint NOT NULL DEFAULT 0  ,
  `create_time` timestamp DEFAULT NULL ,
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
)  ;



DROP TABLE IF EXISTS `work_flow_build_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `work_flow_build_history` (
  `id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  `start_time` timestamp DEFAULT NULL ,
  `end_time` timestamp DEFAULT NULL ,
  `state` smallint DEFAULT NULL ,
  `trigger_type` smallint DEFAULT NULL ,
  `op_user_id` bigint DEFAULT NULL ,
  `op_user_name` varchar(50) DEFAULT NULL ,
  `app_id` bigint DEFAULT NULL ,
  `app_name` varchar(40) DEFAULT NULL ,
  `start_phase` smallint DEFAULT NULL ,
  `history_id` bigint DEFAULT NULL ,
  `work_flow_id` bigint DEFAULT NULL ,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `end_phase` smallint DEFAULT NULL ,
  `last_ver` smallint DEFAULT 0 ,
  `asyn_sub_task_status` clob ,
  PRIMARY KEY (`id`)
) ;

--
-- Table structure for table `work_flow_build_phase`
--

DROP TABLE IF EXISTS `work_flow_build_phase`;

CREATE TABLE `work_flow_build_phase` (
  `id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  `work_flow_build_history_id` bigint DEFAULT NULL  ,
  `phase` smallint DEFAULT NULL ,
  `result` smallint DEFAULT NULL ,
  `phase_info` clob ,
  `create_time` timestamp not null DEFAULT CURRENT_TIMESTAMP ,
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`)
) ;

--
-- Table structure for table `work_flow_publish_history`
--

DROP TABLE IF EXISTS `work_flow_publish_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `work_flow_publish_history` (
  `id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  `create_time` timestamp not null DEFAULT CURRENT_TIMESTAMP ,
  `op_user_id` bigint DEFAULT NULL ,
  `op_user_name` varchar(50)  DEFAULT NULL ,
  `workflow_id` bigint DEFAULT NULL ,
  `workflow_name` varchar(50) DEFAULT NULL ,
  `publish_state` smallint DEFAULT NULL ,
  `type` smallint DEFAULT NULL ,
  `publish_reason` clob,
  `git_sha1` varchar(40) DEFAULT NULL ,
  `in_use` smallint NOT NULL DEFAULT 0 ,
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ;

DROP TABLE IF EXISTS `datasource_db`;
CREATE TABLE `datasource_db` (
  `id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  `name` varchar(50) NOT NULL,
  `extend_class` varchar(256) NOT NULL,
  `sync_online` smallint NOT NULL DEFAULT 0,
  `create_time` timestamp DEFAULT NULL,
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ;

DROP TABLE IF EXISTS `datasource_table`;
CREATE TABLE `datasource_table` (
  `id` bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  `name` varchar(50) NOT NULL,
  `db_id` bigint NOT NULL,
  `sync_online` smallint NOT NULL DEFAULT 0,
  `git_tag` varchar(50) DEFAULT NULL,
  `create_time` timestamp not null DEFAULT CURRENT_TIMESTAMP,
  `op_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`)
) ;
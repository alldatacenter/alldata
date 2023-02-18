/*
 Navicat MySQL Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50722
 Source Host           : localhost:3306
 Source Schema         : datasophon

 Target Server Type    : MySQL
 Target Server Version : 50722
 File Encoding         : 65001

 Date: 29/12/2022 12:05:33
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_ddh_access_token
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_access_token`;
CREATE TABLE `t_ddh_access_token`  (
  `id` int(10) NOT NULL,
  `user_id` int(10) NULL DEFAULT NULL,
  `token` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `expire_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_ddh_access_token
-- ----------------------------
INSERT INTO `t_ddh_access_token` VALUES (0, 1, 'test', '2022-06-15 09:51:54', '2022-06-15 09:51:57', '2023-01-01 09:51:59');

-- ----------------------------
-- Table structure for t_ddh_alert_group
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_alert_group`;
CREATE TABLE `t_ddh_alert_group`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `alert_group_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警组名称',
  `alert_group_category` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '告警组类别',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '告警组表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_alert_group
-- ----------------------------
INSERT INTO `t_ddh_alert_group` VALUES (1, 'HIVE告警组', 'HIVE', '2022-07-14 15:52:45');
INSERT INTO `t_ddh_alert_group` VALUES (2, 'HDFS告警组', 'HDFS', '2022-07-14 15:52:47');
INSERT INTO `t_ddh_alert_group` VALUES (3, 'YARN告警组', 'YARN', '2022-07-14 15:52:50');
INSERT INTO `t_ddh_alert_group` VALUES (8, 'HBASE告警组', 'HBASE', '2022-07-14 15:52:52');
INSERT INTO `t_ddh_alert_group` VALUES (10, 'KAFKA告警组', 'KAFKA', '2022-07-14 15:52:57');
INSERT INTO `t_ddh_alert_group` VALUES (11, '主机告警组', 'NODE', '2022-07-14 15:52:59');
INSERT INTO `t_ddh_alert_group` VALUES (12, 'ZOOKEEPER告警组', 'ZOOKEEPER', '2022-07-14 15:53:02');
INSERT INTO `t_ddh_alert_group` VALUES (13, 'ALERTMANAGER告警组', 'ALERTMANAGER', '2022-07-14 15:53:05');
INSERT INTO `t_ddh_alert_group` VALUES (14, 'GRAFANA告警组', 'GRAFANA', '2022-07-14 15:53:07');
INSERT INTO `t_ddh_alert_group` VALUES (15, 'PROMETHEUS告警组', 'PROMETHEUS', '2022-07-14 15:53:09');
INSERT INTO `t_ddh_alert_group` VALUES (16, 'SPARK告警组', 'SPARK3', '2022-07-15 14:12:38');
INSERT INTO `t_ddh_alert_group` VALUES (17, 'TRINO告警组', 'TRINO', '2022-07-24 23:23:01');
INSERT INTO `t_ddh_alert_group` VALUES (18, 'RANGER告警组', 'RANGER', '2022-09-09 11:29:14');
INSERT INTO `t_ddh_alert_group` VALUES (19, 'STARROCKS告警组', 'STARROCKS', '2022-09-13 14:53:57');
INSERT INTO `t_ddh_alert_group` VALUES (20, 'ELASTICSEARCH告警组', 'ELASTICSEARCH', '2022-10-08 16:15:55');
INSERT INTO `t_ddh_alert_group` VALUES (21, 'DS告警组', 'DS', '2022-11-20 21:00:00');
INSERT INTO `t_ddh_alert_group` VALUES (22, 'SP告警组', 'STREAMPARK', '2022-11-21 18:20:10');

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_expression
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_alert_expression`;
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
-- Records of t_ddh_cluster_alert_expression
-- ----------------------------
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101001, '主机内存使用率(%)', '(1-(node_memory_MemAvailable_bytes/(node_memory_MemTotal_bytes)))*100', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101002, '主机CPU使用率(%)', '(1-avg(irate(node_cpu_seconds_total{mode=\"idle\"}[5m]))by(instance))*100', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101003, '主机CPU系统使用率(%)', 'avg(irate(node_cpu_seconds_total{mode=\"system\"}[5m]))by(instance)*100', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101004, '主机CPU用户使用率(%)', 'avg(irate(node_cpu_seconds_total{mode=\"user\"}[5m]))by(instance)*100', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101005, '主机磁盘IO使用率(%)', 'avg(irate(node_cpu_seconds_total{mode=\"iowait\"}[5m]))by(instance)*100', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101006, '主机交换分区使用率(%)', '(1-((node_memory_SwapFree_bytes+1)/(node_memory_SwapTotal_bytes+1)))*100', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101007, '主机磁盘使用率(%)', '(node_filesystem_size_bytes{fstype=~\"ext.*|xfs\",mountpoint!~\".*pod.*\"}-node_filesystem_free_bytes{fstype=~\"ext.*|xfs\",mountpoint!~\".*pod.*\"})*100/(node_filesystem_avail_bytes{fstype=~\"ext.*|xfs\",mountpoint!~\".*pod.*\"}+(node_filesystem_size_bytes{fstype=~\"ext.*|xfs\",mountpoint!~\".*pod.*\"}-node_filesystem_free_bytes{fstype=~\"ext.*|xfs\",mountpoint!~\".*pod.*\"}))', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101008, '主机入网带宽', 'irate(node_network_receive_bytes_total[5m])*8', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101009, '主机出网带宽', 'irate(node_network_transmit_bytes_total[5m])*8', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101010, '系统平均负载[1m]', 'node_load1', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101011, '系统平均负载[5m]', 'node_load5', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101012, '系统平均负载[15m]', 'node_load15', 'NODE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101013, 'Ntp服务存活', 'cluster_basic_isNtpServiceAlive', 'NODE', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (101014, 'Ntp时间同步', 'cluster_basic_isNtpClockSyncNormal', 'NODE', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (102001, 'AlertManager进程存活', 'alertmanager_isAlertmanagerProcessAlive', 'ALERTMANAGER', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (103001, 'Elasticsearch进程存活', 'Elastic_isEsProcessAlive', 'ELASTICSEARCH', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (103002, 'ElasticsearchCPU使用率(%)', 'es_os_cpu_percent{job=\"ELASTICSEARCH-ElasticSearch\"}', 'ELASTICSEARCH', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (103003, 'Elasticsearch内存使用率(%)', 'es_os_mem_used_percent', 'ELASTICSEARCH', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (103004, 'Elasticsearch磁盘使用率(%)', '100-es_fs_path_available_bytes*100/es_fs_path_total_bytes', 'ELASTICSEARCH', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (104001, 'FlinkHistoryServer进程存活', 'up{job=\"FLINK-FlinkHistoryServer\"}', 'FLINK', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (105001, 'Grafana进程存活', 'grafana_isGrafanaProcessAlive', 'GRAFANA', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (106001, 'HBaseMaster进程存活', 'hbase_isHMasterProcessAlive', 'HBASE', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (106002, 'HRegionServer进程存活', 'hbase_isHRegionServerProcessAlive', 'HBASE', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (106003, 'HThriftServer进程存活', 'hbase_isHThriftServerProcessAlive', 'HBASE', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107001, 'NameNode进程存活', 'hdfs_isNameNodeProcessAlive', 'HDFS', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107002, 'NameNodeRPC延迟[5m]', 'avg_over_time(Hadoop_NameNode_RpcProcessingTimeAvgTime{job=\"HDFS-NameNode\"}[5m])', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107003, 'NameNodeRPC延迟[15m]', 'avg_over_time(Hadoop_NameNode_RpcProcessingTimeAvgTime{job=\"HDFS-NameNode\"}[15m])', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107004, 'NameNode堆内存使用率(%)', 'java_lang_Memory_HeapMemoryUsage_used{job=\"HDFS-NameNode\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"HDFS-NameNode\"}', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107005, 'NameNode老年代GC持续时间[5m]', 'avg_over_time(Hadoop_NameNode_GcTimeMillisConcurrentMarkSweep{job=\"HDFS-NameNode\"}[5m])/(5*60*1000)', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107006, 'NameNode新生代GC持续时间[5m]', 'avg_over_time(Hadoop_NameNode_GcTimeMillisParNew{job=\"HDFS-NameNode\"}[5m])/(5*60*1000)', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107007, 'NameNodeGC持续时间[5m]', 'avg_over_time(Hadoop_NameNode_GcTimeMillis{job=\"HDFS-NameNode\"}[5m])/(5*60*1000)', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107008, 'DataNode进程存活', 'hdfs_isDataNodeProcessAlive', 'HDFS', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107009, 'DataNodeRPC[5m]', 'avg_over_time(Hadoop_DataNode_RpcProcessingTimeAvgTime{job=\"HDFS-DataNode\"}[5m])', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107010, 'DataNodeRPC[15m]', 'avg_over_time(Hadoop_DataNode_RpcProcessingTimeAvgTime{job=\"HDFS-DataNode\"}[15m])', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107011, 'DataNode堆内存使用率(%)', 'java_lang_Memory_HeapMemoryUsage_used{job=\"HDFS-DataNode\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"HDFS-DataNode\"}', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107012, 'DataNode老年代GC持续时间[5m]', 'avg_over_time(Hadoop_DataNode_GcTimeMillisConcurrentMarkSweep{job=\"HDFS-DataNode\"}[5m])/(5*60*1000)', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107013, 'DataNode新生代GC持续时间[5m]', 'avg_over_time(Hadoop_DataNode_GcTimeMillisParNew{job=\"HDFS-DataNode\"}[5m])/(5*60*1000)', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107014, 'DataNodeGC持续时间[5m]', 'avg_over_time(Hadoop_DataNode_GcTimeMillis{job=\"HDFS-DataNode\"}[5m])/(5*60*1000)', 'HDFS', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107015, 'JournalNode进程存活', 'hdfs_isJournalNodeProcessAlive', 'HDFS', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107016, 'ZKFailoverController进程存活', 'hdfs_isDFSZKFCProcessAlive', 'HDFS', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107017, 'HttpFs进程存活', 'supplement_isHttpFsServerProcessNormal', 'HDFS', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107018, 'HDFS坏盘', 'Hadoop_NameNode_VolumeFailuresTotal{name=\"FSNamesystem\"}', 'HDFS', 'INT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (107019, 'HDFS块丢失', 'Hadoop_NameNode_MissingBlocks{name=\"FSNamesystem\"}', 'HDFS', 'INT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (108001, 'HiveServer2进程存活', 'hive_isHiveServer2ProcessAlive', 'HIVE', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (108002, 'HiveServer2堆内存使用率(%)', 'java_lang_Memory_HeapMemoryUsage_used{job=\"HIVE-HiveServer2\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"HIVE-HiveServer2\"}', 'HIVE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (108003, 'HiveServer2老年代GC持续时间[5m]', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"HIVE-HiveServer2\",name=\"PS MarkSweep\"}[5m])/(5*60*1000)', 'HIVE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (108004, 'HiveServer2新生代GC持续时间[5m]', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"HIVE-HiveServer2\",name=\"PS Scavenge\"}[5m])/(5*60*1000)', 'HIVE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (108005, 'HiveMetastore进程存活', 'hive_isHiveMetaStoreProcessAlive', 'HIVE', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (108006, 'HiveMetastore堆内存使用率(%)', 'java_lang_Memory_HeapMemoryUsage_used{job=\"HIVE-MetaStore\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"HIVE-MetaStore\"}', 'HIVE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (108007, 'HiveMetastore老年代GC持续时间[5m]', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"HIVE-MetaStore\",name=\"PS MarkSweep\"}[5m])/(5*60*1000)', 'HIVE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (108008, 'HiveMetastore新生代GC持续时间[5m]', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"HIVE-MetaStore\",name=\"PS Scavenge\"}[5m])/(5*60*1000)', 'HIVE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (108009, 'MySQL进程存活', 'hive_isMysqlProcessAlive', 'HIVE', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (109001, 'HueServer进程存活', 'hue_isHueProcessAlive', 'HUE', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (110001, 'InfluxDB进程存活', 'supplement_isInfluxDBProcessAlive', 'INFLUXDB', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (111001, 'KafkaEagle进程存活', 'KafkaEagle_isKafkaEagleProcessAlive', 'KAFKAEAGLE', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (112001, 'Kibana进程存活', 'kibana_isKibanaProcessAlive', 'KIBANA', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (113001, 'KylinServer进程存活', 'up{job=\"KYLIN-KylinServer\"}', 'KYLIN', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (113002, 'KylinServer堆内存使用率(%)', 'java_lang_Memory_HeapMemoryUsage_used{job=\"KYLIN-KylinServer\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"KYLIN-KylinServer\"}', 'KYLIN', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (113003, 'KylinServer老年代GC持续时间[5m]', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"KYLIN-KylinServer\",name=\"ConcurrentMarkSweep\"}[5m])/(5*60*1000)', 'KYLIN', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (113004, 'KylinServer新生代GC持续时间[5m]', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"KYLIN-KylinServer\",name=\"ParNew\"}[5m])/(5*60*1000)', 'KYLIN', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (114001, 'LivyServer进程存活', 'up{job=\"LIVY-LivyServer\"}', 'LIVY', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (114002, 'LivyServer堆内存使用率(%)', 'java_lang_Memory_HeapMemoryUsage_used{job=\"LIVY-LivyServer\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"LIVY-LivyServer\"}', 'LIVY', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (114003, 'LivyServer老年代GC持续时间[5m]', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"LIVY-LivyServer\",name=\"PS MarkSweep\"}[5m])/(5*60*1000)', 'LIVY', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (114004, 'LivyServer新生代GC持续时间[5m]', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"LIVY-LivyServer\",name=\"PS Scavenge\"}[5m])/(5*60*1000)', 'LIVY', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (115001, 'NodeExporter进程存活', 'up{job=\"NODEEXPORTER-NodeExporter\"}', 'NODEEXPORTER', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (116001, 'OozieServer进程存活', 'up{job=\"OOZIE-OozieServer\"}', 'OOZIE', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (116002, 'OozieServer堆内存使用率(%)', 'java_lang_Memory_HeapMemoryUsage_used{job=\"OOZIE-OozieServer\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"OOZIE-OozieServer\"}', 'OOZIE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (116003, 'OozieServer老年代GC持续时间[5m]', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"OOZIE-OozieServer\",name=\"PS MarkSweep\"}[5m])/(5*60*1000)', 'OOZIE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (116004, 'OozieServer新生代GC持续时间[5m]', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"OOZIE-OozieServer\",name=\"PS Scavenge\"}[5m])/(5*60*1000)', 'OOZIE', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (117001, 'Prometheus进程存活', 'up{job=\"prometheus\"}', 'PROMETHEUS', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (118001, 'RangerServer进程存活', 'up{job=\"RANGER-RangerAdmin\"}', 'RANGER', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (119001, 'SparkHistoryServer进程存活', 'spark_isHistoryServerProcessAlive', 'SPARK', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (120001, 'TezUI进程存活', 'hive_isTezUIProcessAlive', 'TEZ', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (121001, 'MonitorAgent进程存活', 'up{job=\"USDPMONITOR-MonitorAgent\"}', 'USDPMONITOR', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (122001, 'ZkUI进程存活', 'zk_isZKUIProcessAlive', 'ZKUI', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (123001, 'QuarumPeermain进程存活', 'zk_isZKProcessAlive', 'ZOOKEEPER', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (124001, 'ResourceManager进程存活', 'yarn_isResourceManagerProcessAlive', 'YARN', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (124002, 'ResourceManager堆内存使用率(%)', 'java_lang_Memory_HeapMemoryUsage_used{job=\"YARN-ResourceManager\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"YARN-ResourceManager\"}', 'YARN', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (124003, 'ResourceManager老年代GC持续时间[5m]', 'avg_over_time(Hadoop_ResourceManager_GcTimeMillisPS_MarkSweep{job=\"YARN-ResourceManager\"}[5m])/(5*60*1000)', 'YARN', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (124004, 'ResourceManager新生代GC持续时间[5m]', 'avg_over_time(Hadoop_ResourceManager_GcTimeMillisPS_Scavenge{job=\"YARN-ResourceManager\"}[5m])/(5*60*1000)', 'YARN', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (124005, 'ResourceManagerGC持续时间[5m]', 'avg_over_time(Hadoop_ResourceManager_GcTimeMillis{job=\"YARN-ResourceManager\"}[5m])/(5*60*1000)', 'YARN', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (124006, 'NodeManager进程存活', 'yarn_isNodeManagerProcessAlive', 'YARN', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (124007, 'NodeManager堆内存使用率(%)', 'java_lang_Memory_HeapMemoryUsage_used{job=\"YARN-NodeManager\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"YARN-NodeManager\"}', 'YARN', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (124008, 'NodeManager老年代GC持续时间[5m]', 'avg_over_time(Hadoop_NodeManager_GcTimeMillisPS_MarkSweep{job=\"YARN-NodeManager\"}[5m])/(5*60*1000)', 'YARN', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (124009, 'NodeManager新生代GC持续时间[5m]', 'avg_over_time(Hadoop_NodeManager_GcTimeMillisPS_Scavenge{job=\"YARN-NodeManager\"}[5m])/(5*60*1000)', 'YARN', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (124010, 'NodeManagerGC持续时间[5m]', 'avg_over_time(Hadoop_NodeManager_GcTimeMillis{job=\"YARN-NodeManager\"}[5m])/(5*60*1000)', 'YARN', 'FLOAT', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (125001, 'PrestoCoordinator进程存活', 'presto_isCoordinatorProcessAlive', 'PRESTO', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (125002, 'PrestoWorker进程存活', 'presto_isWorkerProcessAlive', 'PRESTO', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (126001, 'UdsMaster进程存活', 'uds_isMasterProcessAlive', 'UDS', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (126002, 'UdsWorker进程存活', 'uds_isWorkerProcessAlive', 'UDS', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (126003, 'UdsWeb进程存活', 'uds_isWebProcessAlive', 'UDS', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (127001, 'KuduMaster进程存活', 'kudu_isMasterProcessAlive', 'KUDU', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (127002, 'KuduTserver进程存活', 'kudu_isTServerProcessAlive', 'KUDU', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (128001, 'ImpalaImpalad进程存活', 'impala_isImpaladProcessAlive', 'IMPALA', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (128002, 'ImpalaCatalog进程存活', 'impala_isCatalogdProcessAlive', 'IMPALA', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (128003, 'ImpalaStatestored进程存活', 'impala_isStatestoredProcessAlive', 'IMPALA', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (129001, 'ZeppelinServer进程存活', 'supplement_isZeppelinServerProcessNormal', 'ZEPPELIN', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (130001, 'AirflowWebserver进程存活', 'supplement_isAirflowWebserverProcessAlive', 'AIRFLOW', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (130002, 'AirflowScheduler进程存活', 'supplement_isAirflowSchedulerProcessAlive', 'AIRFLOW', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (131001, 'AtlasIndexServer进程存活', 'supplement_isAtlasIndexServerProcessAlive', 'ATLAS', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (131002, 'AtlasServer进程存活', 'supplement_isAtlasServerProcessAlive', 'ATLAS', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (132001, 'AlertServer进程存活', 'DolphinScheduler_isAlertProcessAlive', 'DOLPHINSCHEDULER', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (132002, 'ApiServer进程存活', 'DolphinScheduler_isAPIProcessAlive', 'DOLPHINSCHEDULER', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (132003, 'LoggerServer进程存活', 'DolphinScheduler_isLoggerProcessAlive', 'DOLPHINSCHEDULER', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (132004, 'MasterServer进程存活', 'DolphinScheduler_isMasterProcessAlive', 'DOLPHINSCHEDULER', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (132005, 'WorkerServer进程存活', 'DolphinScheduler_isWorkerProcessAlive', 'DOLPHINSCHEDULER', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (133001, 'TrinoCoordinator进程存活', 'trino_isCoordinatorProcessAlive', 'TRINO', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (133002, 'TrinoWorker进程存活', 'trino_isWorkerProcessAlive', 'TRINO', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_expression` VALUES (134001, 'Neo4j进程存活', 'supplement_isNeo4jServerProcessAlive', 'NEO4J', 'BOOL', 'TRUE', 'VALID', 'FALSE', NULL, NULL);

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_group_map
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_alert_group_map`;
CREATE TABLE `t_ddh_cluster_alert_group_map`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `cluster_id` int(10) NULL DEFAULT NULL,
  `alert_group_id` int(10) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_ddh_cluster_alert_group_map
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_history
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_alert_history`;
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
-- Records of t_ddh_cluster_alert_history
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_quota
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_alert_quota`;
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
) ENGINE = InnoDB AUTO_INCREMENT = 625 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群告警指标表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_ddh_cluster_alert_quota
-- ----------------------------
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (464, '主机内存使用率', 'NODE', '(1-(node_memory_MemAvailable_bytes/(node_memory_MemTotal_bytes)))*100', 2, 11, 1, '1234', '>', 95, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (465, '主机CPU使用率', 'NODE', '(1-avg(irate(node_cpu_seconds_total{mode=\"idle\"}[5m]))by(instance))*100', 2, 11, 1, '444', '>', 95, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (466, '主机CPU系统使用率', 'NODE', 'avg(irate(node_cpu_seconds_total{mode=\"system\"}[5m]))by(instance)*100', 1, 11, 1, 'cpu使用过高，评估是否有任务倾斜', '>', 95, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (467, '主机CPU用户使用率', 'NODE', 'avg(irate(node_cpu_seconds_total{mode=\"user\"}[5m]))by(instance)*100', 2, 11, 1, 'cpu使用过高，评估是否有任务倾斜', '>', 95, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (468, '主机磁盘IO使用率', 'NODE', 'avg(irate(node_cpu_seconds_total{mode=\"iowait\"}[5m]))by(instance)*100', 1, 11, 1, '磁盘IO过高，评估任务执行是否过于密集', '>', 95, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (469, '主机交换分区使用率', 'NODE', '(1-((node_memory_SwapFree_bytes+1)/(node_memory_SwapTotal_bytes+1)))*100', 1, 11, 1, '主机交换分区使用率过高，评估是否存在任务密集执行', '>', 95, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (470, '主机磁盘使用率', 'NODE', '(node_filesystem_size_bytes{fstype=~\"ext.*|xfs\",mountpoint!~\".*pod.*\"}-node_filesystem_free_bytes{fstype=~\"ext.*|xfs\",mountpoint!~\".*pod.*\"})*100/(node_filesystem_avail_bytes{fstype=~\"ext.*|xfs\",mountpoint!~\".*pod.*\"}+(node_filesystem_size_bytes{fstype=~\"ext.*|xfs\",mountpoint!~\".*pod.*\"}-node_filesystem_free_bytes{fstype=~\"ext.*|xfs\",mountpoint!~\".*pod.*\"}))', 1, 11, 1, '1', '>', 95, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (471, '主机入网带宽', 'NODE', 'irate(node_network_receive_bytes_total[5m])*8', 1, 11, 1, '网络流量过高', '>', 8589934592, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (472, '主机出网带宽', 'NODE', 'irate(node_network_transmit_bytes_total[5m])*8', 1, 11, 1, '网络流量过高', '>', 8589934592, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (473, '系统平均负载[1m]', 'NODE', 'node_load1', 1, 11, 1, '系统负载过高', '>', 100, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (474, '系统平均负载[5m]', 'NODE', 'node_load5', 1, 11, 1, '系统负载过高', '>', 100, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (475, '系统平均负载[15m]', 'NODE', 'node_load15', 1, 11, 1, '系统负载过高', '>', 100, 1, 1, 60, 'node', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (479, 'NameNode进程存活', 'HDFS', 'up{job=\"namenode\"}', 2, 2, 2, '查看日志，分析宕机原因，解决问题后重新启动', '!=', 1, 1, 1, 15, 'NameNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (480, 'NameNodeRPC延迟[5m]', 'HDFS', 'avg_over_time(Hadoop_NameNode_RpcProcessingTimeAvgTime{job=\"HDFS-NameNode\"}[5m])', 2, 2, 2, '请检查网络流量使用情况', '>', 5, 1, 1, 60, 'NameNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (481, 'NameNodeRPC延迟[15m]', 'HDFS', 'avg_over_time(Hadoop_NameNode_RpcProcessingTimeAvgTime{job=\"HDFS-NameNode\"}[15m])', 1, 2, 2, '请检查网络流量使用情况', '>', 5, 1, 1, 60, 'NameNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (482, 'NameNode堆内存使用率', 'HDFS', 'java_lang_Memory_HeapMemoryUsage_used{job=\"HDFS-NameNode\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"HDFS-NameNode\"}', 1, 2, 2, 'NameNode堆内存不足，增大NameNode堆内存', '>', 95, 1, 1, 60, 'NameNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (483, 'NameNode老年代GC持续时间[5m]', 'HDFS', 'avg_over_time(Hadoop_NameNode_GcTimeMillisConcurrentMarkSweep{job=\"HDFS-NameNode\"}[5m])/(5*60*1000)', 1, 2, 2, '老年代GC时间过长，可考虑加大堆内存', '>', 60, 1, 1, 60, 'NameNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (484, 'NameNode新生代GC持续时间[5m]', 'HDFS', 'avg_over_time(Hadoop_NameNode_GcTimeMillisParNew{job=\"HDFS-NameNode\"}[5m])/(5*60*1000)', 1, 2, 2, '新生代GC时间过长，可考虑加大堆内存', '>', 60, 1, 1, 60, 'NameNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (485, 'NameNodeGC持续时间[5m]', 'HDFS', 'avg_over_time(Hadoop_NameNode_GcTimeMillis{job=\"HDFS-NameNode\"}[5m])/(5*60*1000)', 1, 2, 2, 'GC时间过长，可考虑加大堆内存', '>', 60, 1, 1, 60, 'NameNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (486, 'DataNode进程存活', 'HDFS', 'up{job=\"datanode\"}', 2, 2, 2, '查看日志，分析宕机原因，解决问题后重新启动', '!=', 1, 1, 1, 15, 'DataNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (487, 'DataNodeRPC[5m]', 'HDFS', 'avg_over_time(Hadoop_DataNode_RpcProcessingTimeAvgTime{job=\"HDFS-DataNode\"}[5m])', 1, 2, 2, '请检查网络流量使用情况', '>', 5, 1, 1, 60, 'DataNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (488, 'DataNodeRPC[15m]', 'HDFS', 'avg_over_time(Hadoop_DataNode_RpcProcessingTimeAvgTime{job=\"HDFS-DataNode\"}[15m])', 1, 2, 2, '请检查网络流量使用情况', '>', 5, 1, 1, 60, 'DataNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (489, 'DataNode堆内存使用率', 'HDFS', 'java_lang_Memory_HeapMemoryUsage_used{job=\"HDFS-DataNode\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"HDFS-DataNode\"}', 1, 2, 2, 'NameNode堆内存不足，增大NameNode堆内存', '>', 95, 1, 1, 60, 'DataNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (490, 'DataNode老年代GC持续时间[5m]', 'HDFS', 'avg_over_time(Hadoop_DataNode_GcTimeMillisConcurrentMarkSweep{job=\"HDFS-DataNode\"}[5m])/(5*60*1000)', 1, 2, 2, '老年代GC时间过长，可考虑加大堆内存', '>', 60, 1, 1, 60, 'DataNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (491, 'DataNode新生代GC持续时间[5m]', 'HDFS', 'avg_over_time(Hadoop_DataNode_GcTimeMillisParNew{job=\"HDFS-DataNode\"}[5m])/(5*60*1000)', 1, 2, 2, '新生代GC时间过长，可考虑加大堆内存', '>', 60, 1, 1, 60, 'DataNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (492, 'DataNodeGC持续时间[5m]', 'HDFS', 'avg_over_time(Hadoop_DataNode_GcTimeMillis{job=\"HDFS-DataNode\"}[5m])/(5*60*1000)', 1, 2, 2, 'GC时间过长，可考虑加大堆内存', '>', 60, 1, 1, 60, 'DataNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (493, 'JournalNode进程存活', 'HDFS', 'up{job=\"journalnode\"}', 2, 2, 2, 'JournalNode宕机，请重新启动', '!=', 1, 1, 1, 15, 'JournalNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (494, 'ZKFailoverController进程存活', 'HDFS', 'up{job=\"zkfc\"}', 2, 2, 2, 'ZKFC宕机，请重新启动', '!=', 1, 1, 1, 15, 'ZKFC', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (496, 'HDFS坏盘', 'HDFS', 'Hadoop_NameNode_VolumeFailuresTotal{name=\"FSNamesystem\"}', 1, 2, 2, '存在坏盘', '>', 0, 1, 1, 60, 'NameNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (497, 'HDFS块丢失', 'HDFS', 'Hadoop_NameNode_MissingBlocks{name=\"FSNamesystem\"}', 1, 2, 2, '存在块丢失', '>', 0, 1, 1, 60, 'NameNode', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (512, 'AlertManager进程存活', 'ALERTMANAGER', 'up{job=\"alertmanager\"}', 2, 13, 1, 'AlertManager宕机，请重新启动', '!=', 1, 1, 1, 15, 'AlertManager', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (518, 'Grafana进程存活', 'GRAFANA', 'up{job=\"grafana\"}', 2, 14, 1, 'Grafana宕机，请重新启动', '!=', 1, 1, 1, 15, 'Grafana', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (519, 'HBaseMaster进程存活', 'HBASE', 'up{job=\"hbasemaster\"}', 2, 8, 1, 'Hbase Master宕机，请重新启动', '!=', 1, 1, 1, 15, 'HbaseMaster', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (520, 'HRegionServer进程存活', 'HBASE', 'up{job=\"regionserver\"}', 2, 8, 1, 'RegionServer宕机，请重新启动', '!=', 1, 1, 1, 15, 'RegionServer', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (541, 'HiveServer2进程存活', 'HIVE', 'up{job=\"hiveserver2\"}', 2, 1, 1, 'HiveServer2宕机，请重新启动', '!=', 1, 1, 1, 15, 'HiveServer2', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (542, 'HiveServer2堆内存使用率', 'HIVE', 'java_lang_Memory_HeapMemoryUsage_used{job=\"HIVE-HiveServer2\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"HIVE-HiveServer2\"}', 1, 1, 1, 'HiveServer2堆内存不足，增大NameNode堆内存', '>', 95, 1, 1, 60, 'HiveServer2', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (543, 'HiveServer2老年代GC持续时间[5m]', 'HIVE', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"HIVE-HiveServer2\",name=\"PS MarkSweep\"}[5m])/(5*60*1000)', 1, 1, NULL, '请联系管理员', '>', 60, NULL, NULL, 60, 'HiveServer2', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (544, 'HiveServer2新生代GC持续时间[5m]', 'HIVE', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"HIVE-HiveServer2\",name=\"PS Scavenge\"}[5m])/(5*60*1000)', 1, 1, NULL, '请联系管理员', '>', 60, NULL, NULL, 60, 'HiveServer2', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (545, 'HiveMetastore进程存活', 'HIVE', 'up{job=\"hivemetastore\"}', 2, 1, 1, 'HiveMetastore宕机，请重新启动', '!=', 1, 1, 1, 15, 'HiveMetaStore', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (546, 'HiveMetastore堆内存使用率', 'HIVE', 'java_lang_Memory_HeapMemoryUsage_used{job=\"HIVE-MetaStore\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"HIVE-MetaStore\"}', 1, 1, NULL, '请联系管理员', '>', 95, NULL, NULL, 60, 'HiveMetaStore', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (547, 'HiveMetastore老年代GC持续时间[5m]', 'HIVE', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"HIVE-MetaStore\",name=\"PS MarkSweep\"}[5m])/(5*60*1000)', 1, 1, NULL, '请联系管理员', '>', 60, NULL, NULL, 60, 'HiveMetaStore', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (548, 'HiveMetastore新生代GC持续时间[5m]', 'HIVE', 'avg_over_time(java_lang_GarbageCollector_CollectionTime{job=\"HIVE-MetaStore\",name=\"PS Scavenge\"}[5m])/(5*60*1000)', 1, 1, NULL, '请联系管理员', '>', 60, NULL, NULL, 60, 'HiveMetaStore', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (567, 'Prometheus进程存活', 'PROMETHEUS', 'up{job=\"prometheus\"}', 2, 15, 1, 'Prometheus宕机，请重新启动', '!=', 1, 1, 1, 15, 'Prometheus', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (568, 'RangerServer进程存活', 'RANGER', 'up{job=\"rangeradmin\"}', 2, 18, 1, '请联系管理员', '!=', 1, 1, 1, 15, 'RangerAdmin', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (569, 'SparkHistoryServer进程存活', 'SPARK', 'up{job=\"sparkhistoryserver\"}', 2, 16, 1, '请联系管理员', '!=', 1, 1, 1, 15, 'SparkHistoryServer', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (573, 'ZK进程存活', 'ZOOKEEPER', 'up{job=\"zkserver\"}', 2, 12, 1, 'zk宕机，请重新启动', '!=', 1, 1, 1, 15, 'ZkServer', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (574, 'ResourceManager进程存活', 'YARN', 'up{job=\"resourcemanager\"}', 2, 3, 1, 'ResourceManager宕机，请重新启动', '!=', 1, 1, 1, 15, 'ResourceManager', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (575, 'ResourceManager堆内存使用率', 'YARN', 'java_lang_Memory_HeapMemoryUsage_used{job=\"YARN-ResourceManager\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"YARN-ResourceManager\"}', 1, 3, 1, '请联系管理员', '>', 95, 1, 1, 60, 'ResourceManager', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (576, 'ResourceManager老年代GC持续时间[5m]', 'YARN', 'avg_over_time(Hadoop_ResourceManager_GcTimeMillisPS_MarkSweep{job=\"YARN-ResourceManager\"}[5m])/(5*60*1000)', 1, 3, 1, '请联系管理员', '>', 60, 1, 1, 60, 'ResourceManager', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (577, 'ResourceManager新生代GC持续时间[5m]', 'YARN', 'avg_over_time(Hadoop_ResourceManager_GcTimeMillisPS_Scavenge{job=\"YARN-ResourceManager\"}[5m])/(5*60*1000)', 1, 3, 1, '请联系管理员', '>', 60, 1, 1, 60, 'ResourceManager', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (578, 'ResourceManagerGC持续时间[5m]', 'YARN', 'avg_over_time(Hadoop_ResourceManager_GcTimeMillis{job=\"YARN-ResourceManager\"}[5m])/(5*60*1000)', 1, 3, 1, '请联系管理员', '>', 60, 1, 1, 60, 'ResourceManager', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (579, 'NodeManager进程存活', 'YARN', 'up{job=\"nodemanager\"}', 2, 3, 1, 'NodeManager宕机，请重新启动', '!=', 1, 1, 1, 15, 'NodeManager', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (580, 'NodeManager堆内存使用率', 'YARN', 'java_lang_Memory_HeapMemoryUsage_used{job=\"YARN-NodeManager\"}*100/java_lang_Memory_HeapMemoryUsage_max{job=\"YARN-NodeManager\"}', 1, 3, 1, '请联系管理员', '>', 95, 1, 1, 60, 'NodeManager', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (581, 'NodeManager老年代GC持续时间[5m]', 'YARN', 'avg_over_time(Hadoop_NodeManager_GcTimeMillisPS_MarkSweep{job=\"YARN-NodeManager\"}[5m])/(5*60*1000)', 1, 3, 1, '请联系管理员', '>', 60, 1, 1, 60, 'NodeManager', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (582, 'NodeManager新生代GC持续时间[5m]', 'YARN', 'avg_over_time(Hadoop_NodeManager_GcTimeMillisPS_Scavenge{job=\"YARN-NodeManager\"}[5m])/(5*60*1000)', 1, 3, 1, '请联系管理员', '>', 60, 1, 1, 60, 'NodeManager', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (583, 'NodeManagerGC持续时间[5m]', 'YARN', 'avg_over_time(Hadoop_NodeManager_GcTimeMillis{job=\"YARN-NodeManager\"}[5m])/(5*60*1000)', 1, 3, 1, '请联系管理员', '>', 60, 1, 1, 60, 'NodeManager', 1, '2022-07-14 14:22:36');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (611, 'KafkaBorker进程存活', 'KAFKA', 'up{job=\"kafkabroker\"}', 2, 10, 1, 'KafkaBroker宕机，请重新启动', '!=', 1, 1, 1, 15, 'KafkaBroker', 1, '2022-07-15 14:32:25');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (612, 'TrinoCoordinator进程存活', 'TRINO', 'up{job=\"trinocoordinator\"}', 2, 17, 1, '重新启动', '!=', 1, 1, 1, 15, 'TrinoCoordinator', 1, NULL);
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (613, 'TrinoWorker进程存活', 'TRINO', 'up{job=\"trinoworker\"}', 2, 17, 1, '重新启动', '!=', 1, 1, 1, 15, 'TrinoWorker', 1, NULL);
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (614, '主机状态', 'NODE', 'up{job=\"node\"}', 2, 11, 1, '重新启动该服务器', '!=', 1, 1, 1, 0, 'node', 1, NULL);
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (615, 'FE进程存活', 'STARROCKS', 'up{group=\'fe\'}', 2, 19, 1, '重新启动', '!=', 1, 1, 1, 15, 'FE', 1, '2022-09-13 14:54:39');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (616, 'BE进程存活', 'STARROCKS', 'up{group=\'be\'}', 2, 19, 1, '重新启动', '!=', 1, 1, 1, 15, 'BE', 1, '2022-09-13 14:55:16');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (617, 'SparkMaster进程存活', 'SPARK3', 'up{job=\"sparkmaster\"}', 2, 16, 1, '重新启动', '!=', 1, 1, 1, 15, 'SparkMaster', 1, '2022-09-16 10:24:38');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (618, 'SparkWorker进程存活', 'SPARK3', 'up{job=\"sparkworker\"}', 2, 16, 1, '重新启动', '!=', 1, 1, 1, 15, 'SparkWorker', 1, '2022-09-16 10:25:18');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (619, 'ElasticSearch进程存活', 'ELASTICSEARCH', 'com_datasophon_ddh_worker_metrics_esMetrics_EsUp', 2, 20, 1, '重新启动', '!=', 1, 1, 1, 15, 'ElasticSearch', 1, '2022-10-08 16:17:00');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (620, 'DS API存活', 'DS', 'up{job=\'apiserver\'}', 2, 21, 1, '重新启动', '!=', 1, 1, 1, 15, 'ApiServer', 1, '2022-11-20 21:00:54');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (621, 'DSMaster存活', 'DS', 'up{job=\'masterserver\'}', 2, 21, 1, '重新启动', '!=', 1, 1, 1, 15, 'MasterServer', 1, '2022-11-20 21:01:33');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (622, 'DSWorker存活', 'DS', 'up{job=\'workerserver\'}', 2, 21, 1, '重新启动', '!=', 1, 1, 1, 15, 'WorkerServer', 1, '2022-11-20 21:02:10');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (623, 'DSAlert存活', 'DS', 'up{job=\'alertserver\'}', 2, 21, 1, '重新启动', '!=', 1, 1, 1, 15, 'AlertServer', 1, '2022-11-20 21:02:46');
INSERT INTO `t_ddh_cluster_alert_quota` VALUES (624, 'StreamPark存活', 'STREAMPARK', 'up{job=\'streampark\'}', 2, 22, 1, '重新启动', '!=', 1, 1, 1, 15, 'StreamPark', 1, '2022-11-21 18:20:51');

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_rule
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_alert_rule`;
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
-- Records of t_ddh_cluster_alert_rule
-- ----------------------------
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101001, 101001, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', '主机内存使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101002, 101002, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', '主机CPU使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101003, 101003, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', '主机CPU系统使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101004, 101004, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', '主机CPU用户使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101005, 101005, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', '主机磁盘IO使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101006, 101006, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', '主机交换分区使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101007, 101007, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', '主机磁盘使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101008, 101008, 'TRUE', '>', '8589934592', 60, 'REPEAT', 30, 'WARN', '主机入网带宽', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101009, 101009, 'TRUE', '>', '8589934592', 60, 'REPEAT', 30, 'WARN', '主机出网带宽', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101010, 101010, 'TRUE', '>', '100', 60, 'REPEAT', 30, 'WARN', '系统平均负载[1m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101011, 101011, 'TRUE', '>', '100', 60, 'REPEAT', 30, 'WARN', '系统平均负载[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101012, 101012, 'TRUE', '>', '100', 60, 'REPEAT', 30, 'WARN', '系统平均负载[15m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101013, 101013, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'Ntp服务存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (101014, 101014, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'Ntp时间同步', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (102001, 102001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'AlertManager进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (103001, 103001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'Elasticsearch进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (103002, 103002, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'ElasticsearchCPU使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (103003, 103003, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'Elasticsearch内存使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (103004, 103004, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'Elasticsearch磁盘使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (104001, 104001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'FlinkHistoryServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (105001, 105001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'Grafana进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (106001, 106001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'HBaseMaster进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (106002, 106002, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'HRegionServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (106003, 106003, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'HThriftServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107001, 107001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'NameNode进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107002, 107002, 'TRUE', '>', '5', 60, 'REPEAT', 30, 'WARN', 'NameNodeRPC延迟[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107003, 107003, 'TRUE', '>', '5', 60, 'REPEAT', 30, 'WARN', 'NameNodeRPC延迟[15m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107004, 107004, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'NameNode堆内存使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107005, 107005, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'NameNode老年代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107006, 107006, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'NameNode新生代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107007, 107007, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'NameNodeGC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107008, 107008, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'DataNode进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107009, 107009, 'TRUE', '>', '5', 60, 'REPEAT', 30, 'WARN', 'DataNodeRPC[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107010, 107010, 'TRUE', '>', '5', 60, 'REPEAT', 30, 'WARN', 'DataNodeRPC[15m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107011, 107011, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'DataNode堆内存使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107012, 107012, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'DataNode老年代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107013, 107013, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'DataNode新生代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107014, 107014, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'DataNodeGC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107015, 107015, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'JournalNode进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107016, 107016, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'ZKFailoverController进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107017, 107017, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'HttpFs进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107018, 107018, 'TRUE', '>', '0', 60, 'REPEAT', 30, 'WARN', 'HDFS坏盘', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (107019, 107019, 'TRUE', '>', '0', 60, 'REPEAT', 30, 'WARN', 'HDFS块丢失', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (108001, 108001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'HiveServer2进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (108002, 108002, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'HiveServer2堆内存使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (108003, 108003, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'HiveServer2老年代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (108004, 108004, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'HiveServer2新生代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (108005, 108005, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'HiveMetastore进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (108006, 108006, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'HiveMetastore堆内存使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (108007, 108007, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'HiveMetastore老年代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (108008, 108008, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'HiveMetastore新生代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (108009, 108009, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'MySQL进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (109001, 109001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'HueServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (110001, 110001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'InfluxDB进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (111001, 111001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'KafkaEagle进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (112001, 112001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'Kibana进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (113001, 113001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'KylinServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (113002, 113002, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'KylinServer堆内存使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (113003, 113003, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'KylinServer老年代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (113004, 113004, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'KylinServer新生代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (114001, 114001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'LivyServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (114002, 114002, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'LivyServer堆内存使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (114003, 114003, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'LivyServer老年代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (114004, 114004, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'LivyServer新生代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (115001, 115001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'NodeExporter进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (116001, 116001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'OozieServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (116002, 116002, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'OozieServer堆内存使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (116003, 116003, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'OozieServer老年代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (116004, 116004, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'OozieServer新生代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (117001, 117001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'Prometheus进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (118001, 118001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'RangerServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (119001, 119001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'SparkHistoryServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (120001, 120001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'TezUI进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (121001, 121001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'MonitorAgent进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (122001, 122001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'ZkUI进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (123001, 123001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'QuarumPeermain进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (124001, 124001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'ResourceManager进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (124002, 124002, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'ResourceManager堆内存使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (124003, 124003, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'ResourceManager老年代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (124004, 124004, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'ResourceManager新生代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (124005, 124005, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'ResourceManagerGC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (124006, 124006, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'NodeManager进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (124007, 124007, 'TRUE', '>', '95', 60, 'REPEAT', 30, 'WARN', 'NodeManager堆内存使用率', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (124008, 124008, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'NodeManager老年代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (124009, 124009, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'NodeManager新生代GC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (124010, 124010, 'TRUE', '>', '60', 60, 'REPEAT', 30, 'WARN', 'NodeManagerGC持续时间[5m]', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (125001, 125001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'PrestoCoordinator进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (125002, 125002, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'PrestoWorker进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (126001, 126001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'UdsMaster进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (126002, 126002, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'UdsWorker进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (126003, 126003, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'UdsWeb进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (127001, 127001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'KuduMaster进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (127002, 127002, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'KuduTserver进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (128001, 128001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'ImpalaImpalad进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (128002, 128002, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'ImpalaCatalog进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (128003, 128003, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'ImpalaStatestored进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (129001, 129001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'ZeppelinServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (130001, 130001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'AirflowWebserver进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (130002, 130002, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'AirflowScheduler进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (131001, 131001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'AtlasIndexServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (131002, 131002, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'AtlasServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (132001, 132001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'AlertServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (132002, 132002, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'ApiServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (132003, 132003, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'LoggerServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (132004, 132004, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'MasterServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (132005, 132005, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'WorkerServer进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (133001, 133001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'TrinoCoordinator进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (133002, 133002, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'TrinoWorker进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);
INSERT INTO `t_ddh_cluster_alert_rule` VALUES (134001, 134001, 'TRUE', '!=', '1', 60, 'REPEAT', 30, 'WARN', 'Neo4j进程存活', NULL, 'VALID', 'FALSE', NULL, NULL, NULL);

-- ----------------------------
-- Table structure for t_ddh_cluster_group
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_group`;
CREATE TABLE `t_ddh_cluster_group`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `group_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `cluster_id` int(10) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_group
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_host
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_host`;
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群主机表 ' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_host
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_info
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_info`;
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群信息表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_info
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_node_label
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_node_label`;
CREATE TABLE `t_ddh_cluster_node_label`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `cluster_id` int(10) NULL DEFAULT NULL,
  `node_label` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_node_label
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_queue_capacity
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_queue_capacity`;
CREATE TABLE `t_ddh_cluster_queue_capacity`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `cluster_id` int(10) NULL DEFAULT NULL,
  `queue_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `capacity` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `node_label` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `acl_users` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `parent` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_queue_capacity
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_rack
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_rack`;
CREATE TABLE `t_ddh_cluster_rack`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `rack` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `cluster_id` int(10) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_rack
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_role_user
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_role_user`;
CREATE TABLE `t_ddh_cluster_role_user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cluster_id` int(11) NULL DEFAULT NULL COMMENT '集群id',
  `user_type` int(2) NULL DEFAULT NULL COMMENT '集群用户类型1：管理员2：普通用户',
  `user_id` int(11) NULL DEFAULT NULL COMMENT '用户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群角色用户中间表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_role_user
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_service_command
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_service_command`;
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
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务操作指令表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_service_command
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_service_command_host
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_service_command_host`;
CREATE TABLE `t_ddh_cluster_service_command_host`  (
  `command_host_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '1' COMMENT '主键',
  `hostname` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主机',
  `command_state` int(11) NULL DEFAULT NULL COMMENT '命令状态 1：正在运行2：成功3：失败4、取消',
  `command_progress` int(11) NULL DEFAULT NULL COMMENT '命令进度',
  `command_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '操作指令id',
  `create_time` datetime NULL DEFAULT NULL,
  UNIQUE INDEX `command_host_id`(`command_host_id`) USING BTREE,
  UNIQUE INDEX `command_host_id_2`(`command_host_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务操作指令主机表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_service_command_host
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_service_command_host_command
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_service_command_host_command`;
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
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务操作指令主机指令表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_service_command_host_command
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_service_dashboard
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_service_dashboard`;
CREATE TABLE `t_ddh_cluster_service_dashboard`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主机',
  `service_name` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT '服务名称',
  `dashboard_url` varchar(256) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT '总览页面地址',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = latin1 COLLATE = latin1_swedish_ci COMMENT = '集群服务总览仪表盘' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_service_dashboard
-- ----------------------------
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (1, 'HDFS', 'http://${grafanaHost}:3000/d/huM_B3dZz/2-hdfs?orgId=1&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (2, 'YARN', 'http://${grafanaHost}:3000/d/-ZErfqOWz/3-yarn?orgId=1&refresh=30s&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (3, 'HIVE', 'http://${grafanaHost}:3000/d/WYNeBqdZz/5-hive?orgId=1&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (4, 'HBASE', 'http://${grafanaHost}:3000/d/_S8XBqOWz/4-hbase?orgId=1&refresh=30s&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (5, 'KAFKA', 'http://${grafanaHost}:3000/d/DGHHkJKWk/6-kafka?orgId=1&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (6, 'ZOOKEEPER', 'http://${grafanaHost}:3000/d/000000261/8-zookeeper?orgId=1&refresh=1m&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (7, 'RANGER', 'http://${grafanaHost}:3000/d/qgVDEd3nk/ranger?orgId=1&refresh=30s&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (8, 'PROMETHEUS', 'http://${grafanaHost}:3000/d/dd4t3A6nz/prometheus-2-0-overview?orgId=1&refresh=30s&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (9, 'GRAFANA', 'http://${grafanaHost}:3000/d/eea-11_sik/grafana?orgId=1&refresh=5m&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (10, 'ALERTMANAGER', 'http://${grafanaHost}:3000/d/eea-9_siks/alertmanager?orgId=1&refresh=5m&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (11, 'SPARK3', 'http://${grafanaHost}:3000/d/rCUqf3dWz/7-spark?orgId=1&from=now-30m&to=now&refresh=5m&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (12, 'TOTAL', 'http://${grafanaHost}:3000/d/_4gf-qOZz/1-zong-lan?orgId=1&refresh=30s&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (13, 'TRINO', 'http://${grafanaHost}:3000/d/TGzKne5Wk/trino?orgId=1&refresh=30s&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (14, 'STARROCKS', 'http://${grafanaHost}:3000/d/wpcA3tG7z/starrocks?orgId=1&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (15, 'FLINK', 'http://${grafanaHost}:3000/d/-0rFuzoZk/flink-dashboard?orgId=1&refresh=30s&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (16, 'ELASTICSEARCH', 'http://${grafanaHost}:3000/d/3788af4adc3046dd92b3af31d0150c79/elasticsearch-cluster?orgId=1&refresh=5m&var-cluster=ddp_es&var-name=All&var-interval=5m&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (17, 'DS', 'http://${grafanaHost}:3000/d/X_NPpJOVk/dolphinscheduler?refresh=1m&kiosk');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (18, 'STREAMPARK', 'http://${grafanaHost}:3000/d/98U0T1OVz/streampark?kiosk&refresh=1m');
INSERT INTO `t_ddh_cluster_service_dashboard` VALUES (19, 'DINKY', 'http://${grafanaHost}:3000/d/9qU9T1OVk/dinky?kiosk&refresh=1m');

-- ----------------------------
-- Table structure for t_ddh_cluster_service_instance
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_service_instance`;
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_service_instance
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_service_instance_role_group
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_service_instance_role_group`;
CREATE TABLE `t_ddh_cluster_service_instance_role_group`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `role_group_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `service_instance_id` int(11) NULL DEFAULT NULL,
  `service_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `cluster_id` int(11) NULL DEFAULT NULL,
  `role_group_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_service_instance_role_group
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_service_role_group_config
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_service_role_group_config`;
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
-- Records of t_ddh_cluster_service_role_group_config
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_service_role_instance
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_service_role_instance`;
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群服务角色实例表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_service_role_instance
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_service_role_instance_webuis
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_service_role_instance_webuis`;
CREATE TABLE `t_ddh_cluster_service_role_instance_webuis`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `service_role_instance_id` int(10) NULL DEFAULT NULL COMMENT '服务角色id',
  `web_url` varchar(256) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT 'URL地址',
  `service_instance_id` int(10) NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = latin1 COLLATE = latin1_swedish_ci COMMENT = '集群服务角色对应web ui表 ' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_service_role_instance_webuis
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_user
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_user`;
CREATE TABLE `t_ddh_cluster_user`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `cluster_id` int(10) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_user
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_user_group
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_user_group`;
CREATE TABLE `t_ddh_cluster_user_group`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `user_id` int(10) NULL DEFAULT NULL,
  `group_id` int(10) NULL DEFAULT NULL,
  `cluster_id` int(10) NULL DEFAULT NULL,
  `user_group_type` int(2) NULL DEFAULT NULL COMMENT '1:主用户组 2：附加组',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_user_group
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_variable
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_variable`;
CREATE TABLE `t_ddh_cluster_variable`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `cluster_id` int(10) NULL DEFAULT NULL,
  `variable_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `variable_value` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_ddh_cluster_variable
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_yarn_queue
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_yarn_queue`;
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_yarn_queue
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_yarn_scheduler
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_yarn_scheduler`;
CREATE TABLE `t_ddh_cluster_yarn_scheduler`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `cluster_id` int(11) NULL DEFAULT NULL,
  `scheduler` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `in_use` int(2) NULL DEFAULT NULL COMMENT '1: 是  2：否',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_yarn_scheduler
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_cluster_zk
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_cluster_zk`;
CREATE TABLE `t_ddh_cluster_zk`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `zk_server` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `myid` int(10) NULL DEFAULT NULL,
  `cluster_id` int(10) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_cluster_zk
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_command
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_command`;
CREATE TABLE `t_ddh_command`  (
  `id` int(10) NOT NULL,
  `command_type` int(2) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_ddh_command
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_frame_info
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_frame_info`;
CREATE TABLE `t_ddh_frame_info`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `frame_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '框架名称',
  `frame_code` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '框架编码',
  `frame_version` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群框架表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_frame_info
-- ----------------------------
INSERT INTO `t_ddh_frame_info` VALUES (9, NULL, 'DDP-1.0.0', NULL);

-- ----------------------------
-- Table structure for t_ddh_frame_service
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_frame_service`;
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '集群框架版本服务表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_frame_service
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_frame_service_role
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_frame_service_role`;
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '框架服务角色表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_frame_service_role
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_install_step
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_install_step`;
CREATE TABLE `t_ddh_install_step`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `step_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `step_desc` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `install_type` int(1) NULL DEFAULT NULL COMMENT '1:集群配置2：添加服务3：添加主机',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_install_step
-- ----------------------------
INSERT INTO `t_ddh_install_step` VALUES (1, '安装主机', NULL, 1);
INSERT INTO `t_ddh_install_step` VALUES (2, '主机环境校验', NULL, 1);
INSERT INTO `t_ddh_install_step` VALUES (3, '分发安装启动主机agent', NULL, 1);
INSERT INTO `t_ddh_install_step` VALUES (4, '选择服务', NULL, 1);
INSERT INTO `t_ddh_install_step` VALUES (5, '分配服务Master角色', NULL, 1);
INSERT INTO `t_ddh_install_step` VALUES (6, '分配服务Worker与Client角色', NULL, 1);
INSERT INTO `t_ddh_install_step` VALUES (7, '服务配置', NULL, 1);
INSERT INTO `t_ddh_install_step` VALUES (8, '服务安装总览', NULL, 1);
INSERT INTO `t_ddh_install_step` VALUES (9, '服务安装启动', NULL, 1);

-- ----------------------------
-- Table structure for t_ddh_notice_group
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_notice_group`;
CREATE TABLE `t_ddh_notice_group`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_group_name` varchar(32) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT '通知组名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = latin1 COLLATE = latin1_swedish_ci COMMENT = '通知组表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_notice_group
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_notice_group_user
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_notice_group_user`;
CREATE TABLE `t_ddh_notice_group_user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_group_id` int(11) NULL DEFAULT NULL COMMENT '通知组id',
  `user_id` int(11) NULL DEFAULT NULL COMMENT '用户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = latin1 COLLATE = latin1_swedish_ci COMMENT = '通知组-用户中间表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_notice_group_user
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_role_info
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_role_info`;
CREATE TABLE `t_ddh_role_info`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT '角色名称',
  `role_code` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT '角色编码',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = latin1 COLLATE = latin1_swedish_ci COMMENT = '角色信息表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_role_info
-- ----------------------------

-- ----------------------------
-- Table structure for t_ddh_session
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_session`;
CREATE TABLE `t_ddh_session`  (
  `id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `user_id` int(10) NULL DEFAULT NULL,
  `ip` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `last_login_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_session
-- ----------------------------
INSERT INTO `t_ddh_session` VALUES ('3f229c41-84ee-4a09-a0b9-76e95f0577dc', 2, '192.168.75.12', '2022-09-07 11:52:12');
INSERT INTO `t_ddh_session` VALUES ('d6a56f30-757e-4717-abbd-4bb17983e697', 1, '192.168.75.12', '2022-12-29 11:45:12');

-- ----------------------------
-- Table structure for t_ddh_user_info
-- ----------------------------
DROP TABLE IF EXISTS `t_ddh_user_info`;
CREATE TABLE `t_ddh_user_info`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT '密码',
  `email` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT '手机号',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `user_type` int(2) NULL DEFAULT NULL COMMENT '1：超级管理员 2：普通用户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = latin1 COLLATE = latin1_swedish_ci COMMENT = '用户信息表' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of t_ddh_user_info
-- ----------------------------
INSERT INTO `t_ddh_user_info` VALUES (1, 'admin', '0192023a7bbd73250516f069df18b500', 'xxx@163.com', '1865xx', '2022-05-10 16:05:18', 1);

SET FOREIGN_KEY_CHECKS = 1;

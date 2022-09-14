-- ----------------------------
-- Records of linkis_stream_job_config_def
-- ----------------------------

INSERT INTO `linkis_stream_job_config_def` VALUES (1,'wds.linkis.flink.resource','资源配置','NONE',0,'资源配置','None',NULL,'',1,0,NULL,NULL,'',NULL,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (2,'wds.linkis.flink.app.parallelism','Parallelism并行度','NUMBER',0,'Parallelism并行度','Regex','^([1-9]\\d{0,1}|100)$','',1,1,NULL,'4','',1,1,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (3,'wds.linkis.flink.jobmanager.memory','JobManager Memory (M)','NUMBER',0,'JobManager Memory (M)','Regex','^([1-9]\\d{0,4}|100000)$','',1,1,'M','1024','',1,1,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (4,'wds.linkis.flink.taskmanager.memory','TaskManager Memory (M)','NUMBER',0,'JobManager Memory (M)','Regex','^([1-9]\\d{0,4}|100000)$','',1,1,'M','4096','',1,1,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (5,'wds.linkis.flink.taskmanager.numberOfTaskSlots','TaskManager Slot数量','NUMBER',0,'TaskManager Slot数量','Regex','^([1-9]\\d{0,1}|100)$','',1,1,NULL,'2','',1,1,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (6,'wds.linkis.flink.taskmanager.cpus','TaskManager CPUs','NUMBER',0,'TaskManager CPUs','Regex','^([1-9]\\d{0,1}|100)$','',1,1,NULL,'2','',1,1,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (7,'wds.linkis.flink.custom','Flink参数','NONE',0,'Flink自定义参数','None',NULL,'',1,0,NULL,NULL,'',NULL,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (8,'wds.linkis.flink.produce','生产配置','NONE',0,'生产配置','None',NULL,'',1,0,NULL,NULL,'',NULL,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (9,'wds.linkis.flink.checkpoint.switch','Checkpoint开关','SELECT',0,'Checkpoint开关',NULL,NULL,'',1,1,'','OFF','ON,OFF',8,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (10,'wds.linkis.flink.savepoint.path','快照(Savepoint)文件位置【仅需恢复任务时指定】','INPUT',4,'快照(Savepoint)文件位置','None',NULL,'',1,1,NULL,NULL,'',8,0,1);
INSERT INTO `linkis_stream_job_config_def` VALUES (11,'wds.linkis.flink.alert','告警设置','NONE',0,'告警设置','None',NULL,'',1,1,NULL,NULL,'',NULL,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (12,'wds.linkis.flink.alert.rule','告警规则','NONE',0,'告警规则','None',NULL,'',1,1,NULL,NULL,'',NULL,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (13,'wds.linkis.flink.alert.user','告警用户','NONE',0,'告警用户',NULL,NULL,'',1,1,NULL,NULL,'',NULL,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (14,'wds.linkis.flink.alert.level','告警级别','NONE',0,'告警级别','None',NULL,'',1,1,NULL,NULL,'',NULL,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (15,'wds.linkis.flink.alert.failure.level','失败时告警级别','NONE',0,'失败时告警级别','None',NULL,'',1,1,NULL,NULL,'',NULL,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (16,'wds.linkis.flink.alert.failure.user','失败时告警用户','NONE',0,'失败时告警用户','None',NULL,'',1,1,NULL,NULL,'',NULL,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (32,'wds.linkis.flink.authority','权限设置','NONE',0,'权限设置','None',NULL,'',1,0,NULL,NULL,'',NULL,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (33,'wds.linkis.flink.authority.visible','可见人员','INPUT',0,'可见人员','None',NULL,'',1,1,NULL,NULL,'',32,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (34,'wds.linkis.rm.yarnqueue','使用Yarn队列','INPUT',0,'使用Yarn队列','None',NULL,'',1,1,NULL,NULL,'',1,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (35,'wds.linkis.flink.app.fail-restart.switch','作业失败自动拉起开关','SELECT',1,'作业失败自动拉起开关','None',NULL,'',1,1,NULL,'OFF','ON,OFF',8,0,0);
INSERT INTO `linkis_stream_job_config_def` VALUES (36,'wds.linkis.flink.app.start-auto-restore.switch','作业启动状态自恢复','SELECT',2,'作业启动状态自恢复','None',NULL,'',1,1,NULL,'ON','ON,OFF',8,0,0);
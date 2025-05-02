LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;

INSERT INTO `sys_menu` VALUES (170,NULL,5,0,'数据比对',NULL,NULL,4,'database','compare',_binary '\0',_binary '\0',_binary '\0',NULL,'admin','admin','2023-04-08 00:36:15','2023-04-19 10:27:52'),
                              (171,170,0,1,'数据库配置','dcDbConfig','compare/dcDbConfig/dcDbConfig',3,'develop','dcDbConfig',_binary '\0',_binary '\0',_binary '\0','user:list','admin','admin','2023-04-08 00:41:45','2023-04-15 16:39:49'),
                              (172,170,0,1,'任务配置','dcJobConfig','compare/dcJobConfig/dcJobConfig',4,'develop','dcJobConfig',_binary '\0',_binary '\0',_binary '\0','user:list','admin','admin','2023-04-08 00:43:01','2023-04-15 16:41:09'),
                              (173,170,0,1,'任务实例','dcJobInstance','compare/dcJobInstance/dcJobInstance',5,'icon','dcJobInstance',_binary '\0',_binary '\0',_binary '\0','user:list','admin','admin','2023-04-08 00:43:46','2023-04-15 16:43:02'),
                              (174,170,0,1,'调度日志','dcJobLog','compare/dcJobLog/dcJobLog',7,'timing','dcJobLog',_binary '\0',_binary '\0',_binary '\0','user:list','admin','admin','2023-04-08 00:46:16','2023-04-15 16:43:55'),
                              (175,170,0,1,'定时调度','dcJob','compare/dcJob/dcJob',6,'sqlMonitor','dcJob',_binary '\0',_binary '\0',_binary '\0','user:list','admin','admin','2023-04-08 00:48:47','2023-04-15 16:43:27');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

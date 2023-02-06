# MIGRATION_BASE

CREATE TABLE `sp_account` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `auth_id` int(11) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='主动监控存储用户账号表,已废弃';

CREATE TABLE `sp_authentication` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) unsigned zerofill DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `extra` varchar(2048) NOT NULL DEFAULT '',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='主动监控存储用户认证的表,已废弃';

CREATE TABLE `sp_chart_meta` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL,
  `title` varchar(64) NOT NULL,
  `metricsName` varchar(127) NOT NULL,
  `fields` varchar(4096) NOT NULL,
  `parameters` varchar(4096) NOT NULL,
  `type` varchar(64) NOT NULL,
  `order` int(11) NOT NULL,
  `unit` varchar(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name_unique` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=116 DEFAULT CHARSET=utf8mb4 COMMENT='监控图表元数据配置';


INSERT INTO `sp_chart_meta` (`id`, `name`, `title`, `metricsName`, `fields`, `parameters`, `type`, `order`, `unit`) VALUES (1,'ta_timing','','ta_timing','','','0',0,'ms'),(2,'ta_req','','ta_req','','','0',0,'ms'),(3,'ta_performance','性能区间','ta_timing','{\"avg.plt\": { \"label\":\"整页加载\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }, \n\"avg.tcp\": { \"label\":\"请求排队\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.rlt\": { \"label\":\"资源加载\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.srt\": { \"label\":\"服务器响应\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'ms'),(4,'ta_apdex','用户体验','ta_timing','','','0',0,'ms'),(5,'ta_plt_grade','整页加载完成','ta_timing','','','0',0,'ms'),(6,'ta_wst_grade','白屏时间','ta_timing','','','0',0,'ms'),(7,'ta_fst_grade','首屏时间','ta_timing','','','0',0,'ms'),(8,'ta_rct_grade','资源加载完成','ta_timing','','','0',0,'ms'),(9,'ta_pct_grade','网页加载完成','ta_timing','','','0',0,'ms'),(10,'ta_values_grade','对比分析','ta_timing','{\"count.plt\": { \"label\":\"整页加载完成\", \"unit\": \"\", \"unit_type\": \"precent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"count.wst\": { \"label\":\"白屏时间\", \"unit\": \"%\", \"unit_type\": \"precent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"count.fst\": { \"label\":\"首屏时间\", \"unit\": \"%\", \"unit_type\": \"precent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"count.rct\": { \"label\":\"资源加载完成\", \"unit\": \"%\", \"unit_type\": \"precent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"count.pct\": { \"label\":\"网页加载完成\", \"unit\": \"%\", \"unit_type\": \"precent\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(11,'ta_page_timing','页面性能趋势','ta_timing','{ \"count.plt\": { \"label\":\"PV\", \"unit\": \"\", \"unit_type\": \"count\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.plt\": { \"label\":\"响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 1 }}','','0',0,'ms'),(12,'ta_ajax_timing','Ajax性能趋势','ta_req','{ \"count.tt\": { \"label\":\"PV\", \"unit\": \"\", \"unit_type\": \"count\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.tt\": { \"label\":\"响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 1 }}','','0',0,'ms'),(13,'ta_map','地图','ta_timing','','','0',0,'ms'),(14,'ta_top_avg_time','平均时间','ta_timing','{\"avg.pct\": { \"label\":\"网页加载完成\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.plt\": { \"label\":\"整页加载\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.wst\": { \"label\":\"白屏时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.fst\": { \"label\":\"首屏时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.rct\": { \"label\":\"资源加载完成\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(15,'ta_top_percent_time','时间百分比','ta_timing','{\"sumPercent.pct\": { \"label\":\"网页加载完成\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"sumPercent.plt\": { \"label\":\"整页加载\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"sumPercent.wst\": { \"label\":\"白屏时间\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"sumPercent.fst\": { \"label\":\"首屏时间\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"sumPercent.rct\": { \"label\":\"资源加载完成\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(16,'ta_top_cpm','时间吞吐量','ta_timing','{\"cpm.pct\": { \"label\":\"网页加载完成\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"cpm.plt\": { \"label\":\"整页加载\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"cpm.wst\": { \"label\":\"白屏时间\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"cpm.fst\": { \"label\":\"首屏时间\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"cpm.rct\": { \"label\":\"资源加载完成\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(17,'ta_req_top_avg_time','平均时间','ta_req','{\"avg.tt\": { \"label\":\"请求耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(18,'ta_req_top_percent_time','时间百分比','ta_req','{\"sumPercent.tt\": { \"label\":\"请求耗时\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(19,'ta_req_top_cpm','时间吞吐量','ta_req','{\"cpm.tt\": { \"label\":\"请求耗时\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(20,'ta_avg_time','平均时间','ta_timing','{ \"avg.plt\": { \"label\":\"整页加载\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'ms'),(21,'ta_cpm','吞吐量','ta_timing','{ \"cpm.plt\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'ms'),(22,'ta_percent_os','操作系统','ta_timing','','','0',0,'ms'),(23,'ta_percent_device','设备','ta_timing','','','0',0,'ms'),(24,'ta_percent_browser','浏览器','ta_timing','','','0',0,'ms'),(25,'ta_percent_host','域名','ta_timing','','','0',0,'ms'),(26,'ta_percent_page','页面','ta_timing','','','0',0,'ms'),(27,'ta_req_avg_time','平均时间','ta_req','{ \"avg.tt\": { \"label\":\"请求耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'ms'),(28,'ta_req_cpm','吞吐量','ta_req','{ \"cpm.tt\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'ms'),(29,'ta_req_send','发送数据','ta_req','{ \"sum.res\": { \"label\":\"发送数据\", \"unit\": \"KB\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'kb'),(30,'ta_req_recv','接收数据','ta_req','{ \"sum.req\": { \"label\":\"接收数据\", \"unit\": \"KB\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'kb'),(31,'ta_req_status','HTTP状态码','ta_req','{ \"count.req\": { \"label\":\"HTTP状态码\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,''),(32,'ta_summary_info','摘要详情','ta_timing','{\"count.plt\": { \"label\":\"PV\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"\", \"axis_index\": 0 }, \n\"sum.rdc\": { \"label\":\"DNS解析次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.plt\": { \"label\":\"最终用户体验时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.dit\": { \"label\":\"HTML完成构建\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.drt\": { \"label\":\"阻塞资源加载耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.wst\": { \"label\":\"白屏幕时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.put\": { \"label\":\"页面卸载\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.rrt\": { \"label\":\"页面跳转\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.act\": { \"label\":\"查询缓存\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.dns\": { \"label\":\"查询DNS\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.tcp\": { \"label\":\"建立tcp连接\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.srt\": { \"label\":\"服务器响应\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.rpt\": { \"label\":\"页面下载\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.clt\": { \"label\":\"load事件\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.set\": { \"label\":\"脚本执行\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(33,'status_page','','status_page','','','0',0,''),(34,'ta_script_error','脚本错误','ta_error','{ \"count.count\": { \"label\":\"错误次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,''),(35,'ta_top_script_error','错误信息','ta_error','{ \"count.count\": { \"label\":\"错误次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,''),(36,'ta_top_page_script_error','错误页面','ta_error','{ \"count.count\": { \"label\":\"错误次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,''),(37,'ta_top_js_script_error','JavaScript报错次数','ta_error','{ \"count.count\": { \"label\":\"错误次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,''),(38,'ta_top_browser_script_error','浏览器报错次数','ta_error','{ \"count.count\": { \"label\":\"错误次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,''),(39,'ai_web_top10','web事务 Top10','application_http','{ \"avg.elapsed_mean\": { \"label\":\"平均响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(40,'ai_web_avg_time','web事务','application_http','{ \"avg.elapsed_mean\": { \"label\":\"平均响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(41,'ai_web_cpm','吞吐量','application_http','{ \"sumCpm.elapsed_count\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(42,'ai_web_top_time','平均响应时间','application_http','{ \"avg.elapsed_mean\": { \"label\":\"平均响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(43,'ai_web_top_cpm','吞吐量','application_http','{ \"sumCpm.elapsed_count\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(44,'ai_web_avg_time_top5','响应时间Top5','application_http','{ \"avg.elapsed_mean\": { \"label\":\"平均响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(45,'ai_web_cpm_top5','吞吐量Top5','application_http','{ \"sumCpm.elapsed_count\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(46,'ai_db_top_avg_time','平均响应时间','application_db','{ \"avg.elapsed_mean\": { \"label\":\"平均响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(47,'ai_db_top_cpm','吞吐量','application_db','{ \"sumCpm.elapsed_count\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(48,'ai_db_avg_time','响应时间Top5','application_db','{ \"avg.elapsed_mean\": { \"label\":\"平均响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(49,'ai_db_cpm','吞吐量Top5','application_db','{ \"sumCpm.elapsed_count\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(50,'ai_cache_top_avg_time','平均响应时间','application_cache','{ \"avg.elapsed_mean\": { \"label\":\"平均响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(51,'ai_cache_top_cpm','吞吐量','application_cache','{ \"sumCpm.elapsed_count\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(52,'ai_cache_avg_time','响应时间Top5','application_cache','{ \"avg.elapsed_mean\": { \"label\":\"平均响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(53,'ai_cache_cpm','吞吐量Top5','application_cache','{ \"sumCpm.elapsed_count\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(54,'ai_jvm_mem_heap','Heap memory usage','jvm_memory','{\"avg.init\": { \"label\":\"init\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.max\": { \"label\":\"max\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.used\": { \"label\":\"used\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.committed\": { \"label\":\"committed\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(55,'ai_jvm_mem_non_heap','Non Heap memory usage','jvm_memory','{\"avg.init\": { \"label\":\"init\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.max\": { \"label\":\"max\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.used\": { \"label\":\"used\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.committed\": { \"label\":\"committed\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(56,'ai_jvm_mem_eden_space','PS-Eden-Space','jvm_memory','{\"avg.init\": { \"label\":\"init\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.max\": { \"label\":\"max\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.used\": { \"label\":\"used\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.committed\": { \"label\":\"committed\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(57,'ai_jvm_mem_old_gen','PS-Old-Gen','jvm_memory','{\"avg.init\": { \"label\":\"init\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.max\": { \"label\":\"max\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.used\": { \"label\":\"used\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.committed\": { \"label\":\"committed\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(58,'ai_jvm_mem_survivor_space','PS-Survivor-Space','jvm_memory','{\"avg.init\": { \"label\":\"init\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.max\": { \"label\":\"max\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.used\": { \"label\":\"used\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.committed\": { \"label\":\"committed\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(59,'ai_gc_mark_sweep','GC 次数','jvm_gc','{\"avg.time\": { \"label\":\"平均时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"sum.count\": { \"label\":\"总次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(60,'ai_gc_scavenge','GC 平均时间','jvm_gc','{\"avg.time\": { \"label\":\"平均时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"sum.count\": { \"label\":\"总次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,'ms'),(61,'ai_class_count','Class count','jvm_class_loader','{\"avg.loaded\": { \"label\":\"loaded\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.unloaded\": { \"label\":\"unloaded\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 1 }}','','',0,''),(62,'ai_thread','Thread','jvm_thread','{\"avg.state\": { \"label\":\"线程数量\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(63,'ai_nodejs_men_heap','Heap memory usage','nodejs_memory','{\"avg.heap_used\": { \"label\":\"used\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.heap_total\": { \"label\":\"total\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(64,'ai_nodejs_men_non_heap','Non Heap memory usage','nodejs_memory','{\"avg.external\": { \"label\":\"external\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.rss\": { \"label\":\"rss\", \"unit\": \"B\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(65,'ai_nodejs_cluster_count','Cluster count','nodejs_cluster','{\"max.count\": { \"label\":\"count\", \"unit\": \"B\", \"unit_type\": \"count\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(66,'ai_nodejs_async_res','Async Resources','nodejs_async_resource','{\"sum.count\": { \"label\":\"count\", \"unit\": \"B\", \"unit_type\": \"count\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(67,'ta_overview','页面性能(1小时内)','ta_req','{\"avg.tt\": { \"label\":\"Ajax 响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"cpm.tt\": { \"label\":\"Ajax 吞吐量\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"\", \"axis_index\": 0 }}','','',0,'ms'),(68,'ai_overview','应用性能(1小时内)','application_http','{\"avg.elapsed_mean\": { \"label\":\"响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"sumCpm.elapsed_count\": { \"label\":\"吞吐量\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"\", \"axis_index\": 0 }}','','',0,''),(69,'ta_overview_days','访问量(7天内)','ta_timing','{\"count.plt\": { \"label\":\"请求数\", \"unit\": \"\", \"unit_type\": \"count\", \"chart_type\": \"bar\", \"axis_index\": 0 }}','','',0,'ms'),(70,'ta_top_count','访问次数-Top20','ta_timing','{\"count.plt\": { \"label\":\"访问次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.net\": { \"label\":\"网络耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.srt\": { \"label\":\"服务器响应\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.prt\": { \"label\":\"页面渲染\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.plt\": { \"label\":\"整页加载完成\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 }}','','',0,'ms'),(71,'ai_top_slow_http','慢事务追踪 Top10','application_http_slow','{\"max.elapsed_max\": { \"label\":\"最大耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"max.elapsed_min\": { \"label\":\"最小耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"count.elapsed_count\": { \"label\":\"发生次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"\", \"axis_index\": 0 }}','','',0,''),(72,'ai_top_slow_db','慢数据库追踪 Top10','application_db_slow','{\"max.elapsed_max\": { \"label\":\"最大耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"max.elapsed_min\": { \"label\":\"最小耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"count.elapsed_count\": { \"label\":\"发生次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"\", \"axis_index\": 0 }}','','',0,''),(73,'ta_timing_slow','慢加载追踪','ta_timing_slow','{\"p95.plt\": { \"label\":\"最大耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 }}','','',0,'ms'),(74,'error_count','错误次数','error_count','{\"sum.count\": { \"label\":\"发生次数\", \"unit\": \"\", \"unit_type\": \"count\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(75,'ai_rpc_top_time','平均响应时间','application_rpc','{ \"avg.elapsed_mean\": { \"label\":\"平均响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(76,'ai_rpc_top_cpm','吞吐量','application_rpc','{ \"sumCpm.elapsed_count\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(77,'ai_rpc_avg_time_top5','响应时间Top5','application_rpc','{ \"avg.elapsed_mean\": { \"label\":\"平均响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(78,'ai_rpc_cpm_top5','吞吐量Top5','application_rpc','{ \"sumCpm.elapsed_count\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(79,'ai_web_error_req','错误请求','application_http','{ \"sum.elapsed_count\": { \"label\":\"错误请求\", \"unit\": \"次\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','',0,''),(80,'ta_m_timing','','ta_timing_mobile','','','0',0,'ms'),(81,'ta_m_req','','ta_req_mobile','','','0',0,'ms'),(82,'ta_m_performance','性能区间','ta_timing_mobile','{\"avg.plt\": { \"label\":\"整页加载\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }, \n\"avg.tcp\": { \"label\":\"请求排队\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.rlt\": { \"label\":\"资源加载\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.srt\": { \"label\":\"服务器响应\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'ms'),(83,'ta_m_apdex','用户体验','ta_timing_mobile','','','0',0,'ms'),(84,'ta_m_plt_grade','整页加载完成','ta_timing_mobile','','','0',0,'ms'),(85,'ta_m_values_grade','对比分析','ta_timing_mobile','{\"count.plt\": { \"label\":\"整页加载完成\", \"unit\": \"\", \"unit_type\": \"precent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"count.wst\": { \"label\":\"白屏时间\", \"unit\": \"%\", \"unit_type\": \"precent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"count.fst\": { \"label\":\"首屏时间\", \"unit\": \"%\", \"unit_type\": \"precent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"count.rct\": { \"label\":\"资源加载完成\", \"unit\": \"%\", \"unit_type\": \"precent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"count.pct\": { \"label\":\"网页加载完成\", \"unit\": \"%\", \"unit_type\": \"precent\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(86,'ta_m_page_timing','页面性能趋势','ta_timing_mobile','{ \"count.plt\": { \"label\":\"PV\", \"unit\": \"\", \"unit_type\": \"count\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.plt\": { \"label\":\"响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 1 }}','','0',0,'ms'),(87,'ta_m_req_timing','请求性能趋势','ta_req_mobile','{ \"count.tt\": { \"label\":\"PV\", \"unit\": \"\", \"unit_type\": \"count\", \"chart_type\": \"line\", \"axis_index\": 0 },\n\"avg.tt\": { \"label\":\"响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 1 }}','','0',0,'ms'),(88,'ta_m_map','地图','ta_timing_mobile','','','0',0,'ms'),(89,'ta_m_top_avg_time','平均时间','ta_timing_mobile','{\"avg.pct\": { \"label\":\"网页加载完成\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.plt\": { \"label\":\"整页加载\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.wst\": { \"label\":\"白屏时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.fst\": { \"label\":\"首屏时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"avg.rct\": { \"label\":\"资源加载完成\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(90,'ta_m_top_percent_time','时间百分比','ta_timing_mobile','{\"sumPercent.pct\": { \"label\":\"网页加载完成\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"sumPercent.plt\": { \"label\":\"整页加载\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"sumPercent.wst\": { \"label\":\"白屏时间\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"sumPercent.fst\": { \"label\":\"首屏时间\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"sumPercent.rct\": { \"label\":\"资源加载完成\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(91,'ta_m_top_cpm','时间吞吐量','ta_timing_mobile','{\"cpm.pct\": { \"label\":\"网页加载完成\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"cpm.plt\": { \"label\":\"整页加载\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"cpm.wst\": { \"label\":\"白屏时间\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"cpm.fst\": { \"label\":\"首屏时间\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 },\n\"cpm.rct\": { \"label\":\"资源加载完成\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(92,'ta_m_req_top_avg_time','平均时间','ta_req_mobile','{\"avg.tt\": { \"label\":\"请求耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(93,'ta_m_req_top_percent_time','时间百分比','ta_req_mobile','{\"sumPercent.tt\": { \"label\":\"请求耗时\", \"unit\": \"%\", \"unit_type\": \"percent\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(94,'ta_m_req_top_cpm','时间吞吐量','ta_req_mobile','{\"cpm.tt\": { \"label\":\"请求耗时\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,'ms'),(95,'ta_m_avg_time','平均时间','ta_timing_mobile','{ \"avg.plt\": { \"label\":\"整页加载\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'ms'),(96,'ta_m_cpm','吞吐量','ta_timing_mobile','{ \"cpm.plt\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'ms'),(97,'ta_m_percent_os','操作系统','ta_timing_mobile','','','0',0,'ms'),(98,'ta_m_percent_device','设备','ta_timing_mobile','','','0',0,'ms'),(99,'ta_m_percent_browser','浏览器','ta_timing_mobile','','','0',0,'ms'),(100,'ta_m_percent_av','App版本','ta_timing_mobile','','','0',0,'ms'),(101,'ta_m_percent_host','域名','ta_timing_mobile','','','0',0,'ms'),(102,'ta_m_percent_page','页面','ta_timing_mobile','','','0',0,'ms'),(103,'ta_m_req_avg_time','平均时间','ta_req_mobile','{ \"avg.tt\": { \"label\":\"请求耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'ms'),(104,'ta_m_req_cpm','吞吐量','ta_req_mobile','{ \"cpm.tt\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"cpm\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'ms'),(105,'ta_m_req_send','发送数据','ta_req_mobile','{ \"sum.res\": { \"label\":\"发送数据\", \"unit\": \"KB\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'kb'),(106,'ta_m_req_recv','接收数据','ta_req_mobile','{ \"sum.req\": { \"label\":\"接收数据\", \"unit\": \"KB\", \"unit_type\": \"CAPACITY\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'kb'),(107,'ta_m_req_status','HTTP状态码','ta_req_mobile','{ \"count.req\": { \"label\":\"HTTP状态码\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,''),(108,'ta_m_top_error','错误信息','ta_error_mobile','{ \"count.count\": { \"label\":\"错误次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,''),(109,'ta_m_top_page_error','错误页面','ta_error_mobile','{ \"count.count\": { \"label\":\"错误次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"\", \"axis_index\": 0 }}','','0',0,''),(110,'ta_m_top_error_count','报错次数','ta_error_mobile','{ \"count.count\": { \"label\":\"错误次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,''),(111,'ta_m_top_os_error','系统报错次数','ta_error_mobile','{ \"count.count\": { \"label\":\"错误次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,''),(112,'ta_m_timing_slow','慢加载追踪','ta_timing_mobile_slow','{\"p95.plt\": { \"label\":\"最大耗时\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"\", \"axis_index\": 0 }}','','',0,'ms'),(113,'ta_m_page_error','页面错误','ta_error_mobile','{ \"count.count\": { \"label\":\"错误次数\", \"unit\": \"\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,''),(114,'ai_overview_cpm','接口吞吐量','application_http,application_rpc','{ \"sumCpm.elapsed_count\": { \"label\":\"吞吐量\", \"unit\": \"cpm\", \"unit_type\": \"\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,''),(115,'ai_overview_rt','响应时间','application_http,application_rpc','{ \"avg.elapsed_mean\": { \"label\":\"响应时间\", \"unit\": \"ms\", \"unit_type\": \"time\", \"chart_type\": \"line\", \"axis_index\": 0 }}','','0',0,'');

CREATE TABLE `sp_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `metric_id` int(11) DEFAULT NULL,
  `status_id` int(11) DEFAULT NULL,
  `latency` bigint(64) NOT NULL,
  `count` int(11) NOT NULL,
  `code` int(11) unsigned zerofill NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `metricId` (`metric_id`),
  KEY `statusId` (`status_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='主动监控存储历史记录项的表,已废弃';

CREATE TABLE `sp_log_deployment` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `org_id` varchar(32) NOT NULL,
  `cluster_name` varchar(64) NOT NULL,
  `cluster_type` tinyint(4) NOT NULL,
  `es_url` varchar(1024) NOT NULL,
  `es_config` varchar(1024) NOT NULL,
  `kafka_servers` varchar(1024) NOT NULL,
  `kafka_config` varchar(1024) NOT NULL,
  `collector_url` varchar(255) NOT NULL DEFAULT '',
  `domain` varchar(255) NOT NULL,
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志分析部署表';

CREATE TABLE `sp_log_instance` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `log_key` varchar(64) NOT NULL,
  `org_id` varchar(32) NOT NULL,
  `org_name` varchar(64) NOT NULL,
  `cluster_name` varchar(64) NOT NULL,
  `project_id` varchar(32) NOT NULL,
  `project_name` varchar(255) NOT NULL,
  `workspace` varchar(64) NOT NULL,
  `application_id` varchar(32) NOT NULL,
  `application_name` varchar(255) NOT NULL,
  `runtime_id` varchar(32) NOT NULL,
  `runtime_name` varchar(255) NOT NULL,
  `config` varchar(1023) NOT NULL,
  `version` varchar(64) NOT NULL,
  `plan` varchar(255) NOT NULL,
  `is_delete` tinyint(1) NOT NULL,
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志分析addon实例表';

CREATE TABLE `sp_maintenance` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `duration` int(11) NOT NULL DEFAULT '0',
  `start_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='主动监控存储配置项的表,已废弃';

CREATE TABLE `sp_metric` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) DEFAULT NULL,
  `service_id` int(11) DEFAULT NULL,
  `name` varchar(255) NOT NULL DEFAULT '',
  `url` varchar(255) DEFAULT NULL,
  `mode` varchar(255) NOT NULL DEFAULT '',
  `extra` varchar(1024) NOT NULL DEFAULT '',
  `account_id` int(11) NOT NULL,
  `status` int(11) DEFAULT '0',
  `env` varchar(36) NOT NULL DEFAULT 'PROD',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `projectId` (`project_id`),
  KEY `serviceId` (`service_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='主动监控存储配置项的表';

CREATE TABLE `sp_monitor` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `monitor_id` varchar(55) NOT NULL DEFAULT '',
  `terminus_key` varchar(55) NOT NULL DEFAULT '',
  `terminus_key_runtime` varchar(55) DEFAULT NULL,
  `workspace` varchar(55) DEFAULT NULL,
  `runtime_id` varchar(55) NOT NULL DEFAULT '',
  `runtime_name` varchar(255) DEFAULT NULL,
  `application_id` varchar(55) DEFAULT NULL,
  `application_name` varchar(255) DEFAULT NULL,
  `project_id` varchar(55) DEFAULT NULL,
  `project_name` varchar(255) DEFAULT NULL,
  `org_id` varchar(55) DEFAULT NULL,
  `org_name` varchar(255) DEFAULT NULL,
  `cluster_id` varchar(255) DEFAULT NULL,
  `cluster_name` varchar(125) DEFAULT NULL,
  `config` varchar(1023) NOT NULL DEFAULT '',
  `callback_url` varchar(255) DEFAULT NULL,
  `version` varchar(55) DEFAULT NULL,
  `plan` varchar(255) DEFAULT NULL,
  `is_delete` tinyint(1) NOT NULL DEFAULT '0',
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `sp_monitor_monitor_id` (`monitor_id`),
  KEY `sp_monitor_runtime_id` (`runtime_id`),
  KEY `sp_monitor_terminus_key` (`terminus_key`),
  KEY `sp_monitor_application_id` (`application_id`),
  KEY `sp_monitor_workspace` (`workspace`),
  KEY `sp_monitor_project_id` (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6265 DEFAULT CHARSET=utf8mb4 COMMENT='应用监控addon实例表';

CREATE TABLE `sp_project` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `identity` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `ats` varchar(255) NOT NULL,
  `callback` varchar(255) DEFAULT NULL,
  `project_id` int(11) NOT NULL DEFAULT '0',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `identity` (`identity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='主动监控存储项目映射的表';

CREATE TABLE `sp_report_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) NOT NULL,
  `project_name` varchar(64) NOT NULL,
  `workspace` varchar(16) NOT NULL,
  `created` datetime NOT NULL,
  `weekly_report_enable` tinyint(1) NOT NULL,
  `daily_report_enable` tinyint(1) NOT NULL,
  `weekly_report_config` text NOT NULL,
  `daily_report_config` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `project_workspace` (`project_id`,`workspace`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='应用监控项目报告配置表';

CREATE TABLE `sp_reports` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `key` varchar(32) NOT NULL,
  `start` datetime NOT NULL,
  `end` datetime NOT NULL,
  `project_id` int(11) NOT NULL,
  `project_name` varchar(64) NOT NULL,
  `workspace` varchar(16) NOT NULL,
  `created` datetime NOT NULL,
  `version` varchar(16) NOT NULL,
  `data` text NOT NULL,
  `type` tinyint(4) NOT NULL,
  `terminus_key` varchar(64) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_unique` (`key`) USING BTREE,
  KEY `project_id_workspace` (`project_id`,`workspace`,`type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COMMENT='应用监控项目报告历史表';

CREATE TABLE `sp_service` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `project_id` int(11) unsigned zerofill NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='主动监控存储服务项的表,已废弃';

CREATE TABLE `sp_stage` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '名称',
  `color` varchar(255) DEFAULT NULL COMMENT '颜色',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniqName` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8 COMMENT='告警状态颜色枚举表';


INSERT INTO `sp_stage` (`id`, `name`, `color`) VALUES (1,'Investigating','#EA5746'),(2,'Identified','#ee8600'),(3,'Monitoring','#068FD6'),(4,'Resolved','#666');

CREATE TABLE `sp_status` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `color` varchar(255) DEFAULT NULL,
  `level` int(10) unsigned zerofill NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniqName` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8 COMMENT='主动监控状态枚举表';


INSERT INTO `sp_status` (`id`, `name`, `color`, `level`) VALUES (1,'Operational','#2fcc66',0000000000),(2,'Major Outage','#e74c3c',0000000003),(3,'Partial Outage','#e67e22',0000000002),(4,'Degraded Performance','#f1c40f',0000000001);

CREATE TABLE `sp_trace_request_history` (
  `request_id` varchar(128) NOT NULL,
  `terminus_key` varchar(55) NOT NULL DEFAULT '',
  `url` varchar(1024) NOT NULL,
  `query_string` text,
  `header` text,
  `body` text,
  `method` varchar(16) NOT NULL,
  `status` int(2) NOT NULL DEFAULT '0',
  `response_status` int(11) NOT NULL DEFAULT '200',
  `response_body` text,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`request_id`),
  KEY `INDEX_TERMINUS_KEY` (`terminus_key`),
  KEY `INDEX_STATUS` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链路诊断历史表';

CREATE TABLE `sp_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `salt` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniqName` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8 COMMENT='主动监控存储用户的表,已废弃';


INSERT INTO `sp_user` (`id`, `username`, `salt`, `password`, `created_at`) VALUES (3,'admin','1536227528641690966','54cf26baae342201e50ddb4391b44f1f','2018-09-06 01:52:09'),(5,'purchase','1538099181623594327','f6c212584ca861b507234529c4a70fb1','2018-09-28 01:46:22');

CREATE TABLE `tb_tmc` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(32) NOT NULL COMMENT '显示名称',
  `engine` varchar(32) NOT NULL COMMENT '内核名称',
  `service_type` varchar(32) NOT NULL COMMENT '分类，微服务、通用能力，addon',
  `deploy_mode` varchar(32) NOT NULL COMMENT '发布模式',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8 COMMENT='微服务组件元信息';


INSERT INTO `tb_tmc` (`id`, `name`, `engine`, `service_type`, `deploy_mode`, `create_time`, `update_time`, `is_deleted`) VALUES (1,'微服务治理','micro-service','MICRO_SERVICE','SAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(2,'配置中心','config-center','MICRO_SERVICE','SAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(3,'应用监控','monitor','MICRO_SERVICE','SAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(4,'注册中心','registercenter','MICRO_SERVICE','SAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(5,'API网关','api-gateway','MICRO_SERVICE','SAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(6,'Nacos','nacos','ADDON','SAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(7,'MySQL','mysql','ADDON','SAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(8,'ZooKeeper','zookeeper','ADDON','SAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(9,'ETCD','etcd','ADDON','SAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(10,'ZKProxy','zkproxy','ADDON','SAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(11,'PostgreSQL','postgresql','ADDON','SAAS','2021-05-28 11:30:25','2021-05-28 11:54:58','N'),(12,'权限中心','acl','GENERAL_ABILITY','PAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(13,'日志分析','log-analytics','ADDON','SAAS','2021-05-28 11:30:25','2021-05-28 11:54:54','N'),(14,'Trantor','trantor','GENERAL_ABILITY','PAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(15,'通知中心','notice-center','GENERAL_ABILITY','SAAS','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(16,'服务治理','service-mesh','MICRO_SERVICE','SAAS','2021-05-28 11:54:53','2021-05-28 11:54:53','N'),(17,'监控Zookeeper','monitor-zk','ADDON','SAAS','2021-05-28 11:54:54','2021-05-28 11:54:54','N'),(18,'监控Kafka','monitor-kafka','ADDON','SAAS','2021-05-28 11:54:54','2021-05-28 11:54:54','N'),(19,'监控Collector','monitor-collector','ADDON','SAAS','2021-05-28 11:54:54','2021-05-28 11:54:54','N'),(20,'日志导出','log-exporter','ADDON','PAAS','2021-05-28 11:54:54','2021-05-28 11:54:54','N'),(21,'日志elasticsearch','log-es','ADDON','SAAS','2021-05-28 11:54:54','2021-05-28 11:54:54','N');

CREATE TABLE `tb_tmc_ini` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `ini_name` varchar(128) NOT NULL COMMENT '配置信息名称',
  `ini_desc` varchar(256) NOT NULL COMMENT '配置信息介绍',
  `ini_value` varchar(4096) NOT NULL COMMENT '配置信息参数值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_ini_name` (`ini_name`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8 COMMENT='微服务控制平台的配置信息';


INSERT INTO `tb_tmc_ini` (`id`, `ini_name`, `ini_desc`, `ini_value`, `create_time`, `update_time`, `is_deleted`) VALUES (1,'MS_MENU','微服务治理菜单列表','[{\"key\":\"ServiceGovernance\",\"cnName\":\"微服务治理\",\"enName\":\"MicroService\",\"children\":[{\"key\":\"Overview\",\"cnName\":\"全局拓扑\",\"enName\":\"Overview\",\"exists\":true,\"mustExists\":true}],\"exists\":true,\"mustExists\":true},{\"key\":\"AppMonitor\",\"cnName\":\"应用监控\",\"enName\":\"AppMonitor\",\"children\":[{\"key\":\"MonitorIntro\",\"href\":\"/manual/microservice/use-apm-monitor-app.html\",\"cnName\":\"使用引导\",\"enName\":\"MonitorIntro\",\"exists\":true},{\"key\":\"ServiceList\",\"cnName\":\"服务列表\",\"enName\":\"ServiceList\",\"exists\":false},{\"key\":\"BrowserInsight\",\"cnName\":\"浏览性能\",\"enName\":\"BrowserInsight\",\"exists\":false},{\"key\":\"AppInsight\",\"cnName\":\"APP性能\",\"enName\":\"AppInsight\",\"exists\":false},{\"key\":\"ErrorInsight\",\"cnName\":\"错误分析\",\"enName\":\"ErrorInsight\",\"exists\":false},{\"key\":\"Transaction\",\"cnName\":\"链路追踪\",\"enName\":\"Transaction\",\"exists\":false},{\"key\":\"StatusPage\",\"cnName\":\"主动监控\",\"enName\":\"StatusPage\",\"exists\":false},{\"key\":\"Alarm\",\"cnName\":\"告警通知\",\"enName\":\"Alarm\",\"exists\":false},{\"key\":\"CustomAlarm\",\"cnName\":\"自定义告警\",\"enName\":\"CustomAlarm\",\"exists\":false},{\"key\":\"AlarmRecord\",\"cnName\":\"告警记录\",\"enName\":\"AlarmRecord\",\"exists\":false},{\"key\":\"CustomDashboard\",\"cnName\":\"运维大盘\",\"enName\":\"O & M dashboard\",\"exists\":false}],\"exists\":true,\"mustExists\":true},{\"key\":\"LogAnalyze\",\"cnName\":\"日志分析\",\"enName\":\"LogAnalyze\",\"exists\":true,\"children\":[{\"cnName\":\"日志查询\",\"enName\":\"LogQuery\",\"key\":\"LogQuery\",\"exists\":false},{\"cnName\":\"分析规则\",\"enName\":\"AnalyzeRule\",\"key\":\"AnalyzeRule\",\"exists\":false}]},{\"key\":\"APIGateway\",\"cnName\":\"API网关\",\"enName\":\"APIGateway\",\"children\":[{\"key\":\"GatewayIntro\",\"cnName\":\"使用引导\",\"enName\":\"GatewayIntro\",\"href\":\"/manual/microservice/api-gateway.html\",\"exists\":true},{\"key\":\"Endpoints\",\"cnName\":\"流量入口管理\",\"enName\":\"Endpoints\",\"exists\":false,\"onlyK8S\":true},{\"key\":\"APIs\",\"cnName\":\"微服务API管理\",\"enName\":\"APIs\",\"exists\":false},{\"key\":\"ConsumerACL\",\"cnName\":\"调用方管理\",\"enName\":\"ConsumerACL\",\"exists\":false,\"onlyK8S\":true},{\"key\":\"OldPolicies\",\"cnName\":\"API策略\",\"enName\":\"Policies\",\"exists\":false,\"onlyNotK8S\":true},{\"key\":\"OldConsumerACL\",\"cnName\":\"调用者授权\",\"enName\":\"ConsumerACL\",\"exists\":false,\"onlyNotK8S\":true}],\"exists\":true,\"mustExists\":true},{\"key\":\"RegisterCenter\",\"cnName\":\"注册中心\",\"enName\":\"RegisterCenter\",\"children\":[{\"key\":\"RegisterIntro\",\"cnName\":\"使用引导\",\"enName\":\"RegisterIntro\",\"href\":\"/manual/microservice/dubbo.html\",\"exists\":true},{\"key\":\"Services\",\"cnName\":\"服务注册列表\",\"enName\":\"Services\",\"exists\":false}],\"exists\":true,\"mustExists\":true},{\"key\":\"ConfigCenter\",\"cnName\":\"配置中心\",\"enName\":\"ConfigCenter\",\"children\":[{\"key\":\"ConfigIntro\",\"cnName\":\"使用引导\",\"href\":\"/manual/deploy/config-center.html\",\"enName\":\"ConfigIntro\",\"exists\":true},{\"key\":\"Configs\",\"cnName\":\"配置管理\",\"enName\":\"Configs\",\"exists\":false}],\"exists\":true,\"mustExists\":true},{\"key\":\"ComponentInfo\",\"cnName\":\"组件信息\",\"enName\":\"ComponentInfo\",\"children\":[],\"exists\":true,\"mustExists\":true}]','2021-05-28 11:30:25','2021-05-28 13:53:56','N'),(2,'MK_config-center','','ConfigCenter','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(3,'MK_JUMP_config-center','','Configs','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(4,'MK_monitor','','AppMonitor','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(5,'MK_JUMP_monitor','','Overview','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(6,'MK_registercenter','','RegisterCenter','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(7,'MK_JUMP_registercenter','','Services','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(8,'MK_api-gateway','','APIGateway','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(9,'MK_JUMP_api-gateway','','APIs','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(10,'MK_JUMP_micro-service','','Overview','2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(11,'MK_log-analytics','','LogAnalyze','2020-11-14 22:44:55','2020-11-14 22:44:55','N');

CREATE TABLE `tb_tmc_instance` (
  `id` varchar(64) NOT NULL COMMENT '实例唯一id',
  `engine` varchar(128) NOT NULL COMMENT '对应内核名称',
  `version` varchar(32) NOT NULL COMMENT '版本',
  `release_id` varchar(32) DEFAULT '' COMMENT 'dicehub releaseId',
  `status` varchar(16) NOT NULL COMMENT '实例当前运行状态',
  `az` varchar(128) NOT NULL DEFAULT 'MARATHONFORTERMINUS' COMMENT '部署集群',
  `config` varchar(4096) DEFAULT NULL COMMENT '实例配置信息',
  `options` varchar(4096) DEFAULT NULL COMMENT '选填参数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  `is_custom` varchar(1) NOT NULL DEFAULT 'N' COMMENT '是否为第三方',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='微服务组件部署实例信息';

CREATE TABLE `tb_tmc_instance_tenant` (
  `id` varchar(64) NOT NULL COMMENT '租户唯一id',
  `instance_id` varchar(64) NOT NULL COMMENT '实例id',
  `config` varchar(4096) DEFAULT NULL COMMENT '租户配置信息',
  `options` varchar(4096) DEFAULT NULL COMMENT '选填参数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  `tenant_group` varchar(64) DEFAULT NULL COMMENT '租户组',
  `engine` varchar(128) DEFAULT NULL COMMENT '内核',
  `az` varchar(128) DEFAULT NULL COMMENT '集群',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='微服务组件租户信息';

CREATE TABLE `tb_tmc_request_relation` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `parent_request_id` varchar(64) NOT NULL COMMENT '父级请求id',
  `child_request_id` varchar(64) NOT NULL COMMENT '子级请求id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='微服务组件部署过程依赖关系';

CREATE TABLE `tb_tmc_version` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `engine` varchar(32) NOT NULL COMMENT '内核名称',
  `version` varchar(32) NOT NULL COMMENT '版本',
  `release_id` varchar(32) DEFAULT NULL COMMENT 'dicehub releaseId',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8 COMMENT='微服务组件版本元信息';


INSERT INTO `tb_tmc_version` (`id`, `engine`, `version`, `release_id`, `create_time`, `update_time`, `is_deleted`) VALUES (1,'micro-service','1.0.0',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(2,'config-center','1.0.0',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(3,'monitor','3.6',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(4,'registercenter','1.0.0',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(5,'api-gateway','3.0.0',NULL,'2021-05-28 11:30:25','2021-05-28 13:53:54','N'),(6,'nacos','1.1.0',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(7,'mysql','5.7.23',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(8,'zookeeper','3.4.10',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(9,'etcd','3.3.12',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(10,'zkproxy','1.0.0',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(11,'postgresql','1.0.0',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(12,'log-analytics','1.0.0',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(13,'acl','1.0.0',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(14,'acl','1.0.1',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(15,'trantor','1.0.0',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(16,'notice-center','1.0.0',NULL,'2021-05-28 11:30:25','2021-05-28 11:30:25','N'),(17,'service-mesh','1.0.0',NULL,'2021-05-28 11:54:53','2021-05-28 11:54:53','N'),(18,'registercenter','2.0.0',NULL,'2021-05-28 11:54:53','2021-05-28 11:54:53','N'),(19,'monitor-zk','3.4.10',NULL,'2021-05-28 11:54:54','2021-05-28 11:54:54','N'),(20,'monitor-kafka','2.0.0',NULL,'2021-05-28 11:54:54','2021-05-28 11:54:54','N'),(21,'monitor-collector','1.0.0',NULL,'2021-05-28 11:54:54','2021-05-28 11:54:54','N'),(22,'log-exporter','1.0.0',NULL,'2021-05-28 11:54:54','2021-05-28 11:54:54','N'),(23,'log-es','1.0.0',NULL,'2021-05-28 11:54:54','2021-05-28 11:54:54','N'),(24,'log-analytics','2.0.0',NULL,'2021-05-28 11:54:54','2021-05-28 11:54:54','N');


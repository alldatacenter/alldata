/******************************************/
/*   初始化数据   */
/******************************************/

truncate config;

-- ----------------------------
-- Config init
-- ----------------------------
INSERT INTO `config` VALUES
(1,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','__nrType','__nrId','elasticsearchProperties','','{}'),
(2,'2019-02-28 01:27:56','2019-02-28 01:27:56','__category','__nrType','__nrId','numberOfShards','','1'),
(3,'2019-02-28 01:27:56','2019-02-28 01:27:56','__category','__nrType','__nrId','numberOfReplicas','','0'),
(7,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','tesla_base_common_web_component_config','__nrId','adjustJsonObjectToEs','','pool'),
(8, '2019-02-28 01:19:27','2019-02-28 01:19:36','__category','standard_app','__nrId','adjustJsonObjectToEs','','pool'),
(9, '2019-02-28 01:19:27','2019-02-28 01:19:36','__category','standard_service','__nrId','adjustJsonObjectToEs','','pool'),
(10,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','k8s_pod','__nrId','adjustJsonObjectToEs','','pool'),
(11,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','k8s_pod','__nrId','typeTTL','','180'),
(12,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','k8s_container','__nrId','adjustJsonObjectToEs','','pool'),
(13,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','k8s_container','__nrId','typeTTL','','180'),
(14,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','standard_cluster','__nrId','extraPool','','true'),

(15,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','standard_app','__nrId','elasticsearchIndexAnalysis','','{"analyzer":{"pre":{"filter":["lowercase"],"tokenizer":"pre"},"ik":{"filter":["lowercase"],"tokenizer":"ik_smart"},"whitespace":{"filter":["lowercase"],"tokenizer":"whitespace"},"full":{"filter":["lowercase"],"tokenizer":"full"}},"tokenizer":{"pre":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"edge_ngram","max_gram":100},"full":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"ngram","max_gram":30}}}'),
(16,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','standard_service','__nrId','elasticsearchIndexAnalysis','','{"analyzer":{"pre":{"filter":["lowercase"],"tokenizer":"pre"},"ik":{"filter":["lowercase"],"tokenizer":"ik_smart"},"whitespace":{"filter":["lowercase"],"tokenizer":"whitespace"},"full":{"filter":["lowercase"],"tokenizer":"full"}},"tokenizer":{"pre":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"edge_ngram","max_gram":100},"full":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"ngram","max_gram":30}}}'),

(17,'2019-09-29 09:58:10','2019-09-29 09:58:17','__category','__nrType','__nrId','elasticsearchDynamicTemplates','','[{\"clob\":{\"match\":\"*CLOB\",\"match_mapping_type\":\"string\",\"mapping\":{\"ignore_above\":200,\"type\":\"keyword\"}}}]'),
(18,'2019-02-28 01:19:27','2019-10-18 02:19:36','__category','job_center_instance','__nrId','adjustJsonObjectToEs','','pool'),
(19,'2019-02-28 01:19:27','2019-10-18 02:19:36','__category','cluster','__nrId','adjustJsonObjectToEs','','rich'),
(20,'2019-02-28 01:27:56','2019-02-28 01:27:56','__category','__nrType','__nrId','adjustJsonObjectToEs','','rich'),

(21,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','productops_std_prod','__nrId','elasticsearchIndexAnalysis','','{"filter":{"suf":{"min_gram":1,"type":"edgeNGram","max_gram":100}},"analyzer":{"pre":{"filter":["lowercase"],"tokenizer":"pre"},"ik":{"filter":["lowercase"],"tokenizer":"ik_smart"},"lowercase":{"filter":["lowercase"],"tokenizer":"keyword"},"suf":{"filter":["lowercase","reverse","suf","reverse"],"tokenizer":"keyword"},"whitespace":{"filter":["lowercase"],"tokenizer":"whitespace"},"full":{"filter":["lowercase"],"tokenizer":"full"}},"tokenizer":{"pre":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"edge_ngram","max_gram":200},"suf":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"edge_ngram","max_gram":100},"full":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"ngram","max_gram":30}}}'),
(22,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','productops_std_zh_mo_prod','__nrId','elasticsearchIndexAnalysis','','{"filter":{"suf":{"min_gram":1,"type":"edgeNGram","max_gram":100}},"analyzer":{"pre":{"filter":["lowercase"],"tokenizer":"pre"},"ik":{"filter":["lowercase"],"tokenizer":"ik_smart"},"lowercase":{"filter":["lowercase"],"tokenizer":"keyword"},"suf":{"filter":["lowercase","reverse","suf","reverse"],"tokenizer":"keyword"},"whitespace":{"filter":["lowercase"],"tokenizer":"whitespace"},"full":{"filter":["lowercase"],"tokenizer":"full"}},"tokenizer":{"pre":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"edge_ngram","max_gram":200},"suf":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"edge_ngram","max_gram":100},"full":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"ngram","max_gram":30}}}'),
(23,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','productops_std_zh_hk_prod','__nrId','elasticsearchIndexAnalysis','','{"filter":{"suf":{"min_gram":1,"type":"edgeNGram","max_gram":100}},"analyzer":{"pre":{"filter":["lowercase"],"tokenizer":"pre"},"ik":{"filter":["lowercase"],"tokenizer":"ik_smart"},"lowercase":{"filter":["lowercase"],"tokenizer":"keyword"},"suf":{"filter":["lowercase","reverse","suf","reverse"],"tokenizer":"keyword"},"whitespace":{"filter":["lowercase"],"tokenizer":"whitespace"},"full":{"filter":["lowercase"],"tokenizer":"full"}},"tokenizer":{"pre":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"edge_ngram","max_gram":200},"suf":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"edge_ngram","max_gram":100},"full":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"ngram","max_gram":30}}}'),
(24,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','productops_std_en_us_prod','__nrId','elasticsearchIndexAnalysis','','{"filter":{"suf":{"min_gram":1,"type":"edgeNGram","max_gram":100}},"analyzer":{"pre":{"filter":["lowercase"],"tokenizer":"pre"},"ik":{"filter":["lowercase"],"tokenizer":"ik_smart"},"lowercase":{"filter":["lowercase"],"tokenizer":"keyword"},"suf":{"filter":["lowercase","reverse","suf","reverse"],"tokenizer":"keyword"},"whitespace":{"filter":["lowercase"],"tokenizer":"whitespace"},"full":{"filter":["lowercase"],"tokenizer":"full"}},"tokenizer":{"pre":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"edge_ngram","max_gram":200},"suf":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"edge_ngram","max_gram":100},"full":{"token_chars":["letter","digit","punctuation","symbol"],"min_gram":1,"type":"ngram","max_gram":30}}}')
;

-- ----------------------------
-- ES Endpoint init
-- ----------------------------
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (10000,'2020-03-24 22:46:53','2020-04-03 00:57:40','__category','__nrType','__nrId','backendStores','sreworks','[{"schema":"http","default_store":true,"port":${DATA_ES_PORT},"name":"backend_store_basic","host":"${DATA_ES_HOST}","user":"${DATA_ES_USER}","password":"${DATA_ES_PASSWORD}","index_patterns":{},"type":"elasticsearch","backup_store":"backend_store_level_1"}]');

-- ----------------------------
-- categoryIncludeIndexes
-- ----------------------------
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (11000,'2020-03-24 22:46:53','2020-04-03 00:57:40','__category','__nrType','__nrId','categoryIncludeIndexes','sreworks','[]');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (11001,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','__nrType','__nrId','categoryIncludeIndexes','sreworks','["sreworks_productops_node", "sreworks_cluster", "sreworks_team", "sreworks_metric", "sreworks_app", "sreworks_event"]');

-- ----------------------------
-- categoryTypeAlias 类型别名
-- ----------------------------
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (12000,'2020-03-24 22:46:53','2020-04-03 00:57:40','__category','__nrType','__nrId','categoryTypeAlias','sreworks','默认');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (12001,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','__nrType','__nrId','categoryTypeAlias','sreworks','SW中台');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (12002,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_productops_node','__nrId','categoryTypeAlias','sreworks','站点导航');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (12003,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_cluster','__nrId','categoryTypeAlias','sreworks','实体数据');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (12004,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_team','__nrId','categoryTypeAlias','sreworks','实体数据');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (12005,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_metric','__nrId','categoryTypeAlias','sreworks','指标定义');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (12006,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_app','__nrId','categoryTypeAlias','sreworks','实体数据');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (12007,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_event','__nrId','categoryTypeAlias','sreworks','应用事件');


-- ----------------------------
-- categoryTypeIdTitleKey
-- ----------------------------
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (13000,'2020-03-24 22:46:53','2020-04-03 00:57:40','__category','__nrType','__nrId','categoryTypeIdTitleKey','sreworks','${__id}');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (13001,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','__nrType','__nrId','categoryTypeIdTitleKey','sreworks','${__id}');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (13002,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_productops_node','__nrId','categoryTypeIdTitleKey','sreworks','etlJsonGet(${config}, label) etlJsonGet(${config}, keywords)');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (13003,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_cluster','__nrId','categoryTypeIdTitleKey','sreworks','集群-${name}');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (13004,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_team','__nrId','categoryTypeIdTitleKey','sreworks','团队-${name}');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (13005,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_metric','__nrId','categoryTypeIdTitleKey','sreworks','etlJsonGet(${labels}, app_name) ${name}-${alias}');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (13006,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_app','__nrId','categoryTypeIdTitleKey','sreworks','应用-${name}');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (13007,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_event','__nrId','categoryTypeIdTitleKey','sreworks','事件-${name} ${app_name}');


-- ----------------------------
-- categoryTypeIdUrlKey
-- ----------------------------
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (14000,'2020-03-24 22:46:53','2020-04-03 00:57:40','__category','__nrType','__nrId','categoryTypeIdUrlKey','sreworks','${url}');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (14001,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','__nrType','__nrId','categoryTypeIdUrlKey','sreworks','${url}');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (14002,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_productops_node','__nrId','categoryTypeIdUrlKey','sreworks','etlJsonGet(${config}, url)');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (14003,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_cluster','__nrId','categoryTypeIdUrlKey','sreworks','cluster/single_cluster/summary?clusterId=${__id}');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (14004,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_team','__nrId','categoryTypeIdUrlKey','sreworks','team/single_team/summary?teamId=${__id}');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (14005,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_app','__nrId','categoryTypeIdUrlKey','sreworks','app/single-app-tmpl/overview?appId=${__id}&appName=${name}');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (14006,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_metric','__nrId','categoryTypeIdUrlKey','sreworks','app/single-app-tmpl/indicator?appId=etlJsonGet(${labels}, app_id)&appName=etlJsonGet(${labels}, app_name)');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (14007,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_event','__nrId','categoryTypeIdUrlKey','sreworks','app/single-app-tmpl/health?appId=etlReplace(${app_id}, sreworks, )&appName=${app_name}');


-- ----------------------------
-- categoryIndexSuggestFields
-- ----------------------------
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (15000,'2020-03-24 22:46:53','2020-04-03 00:57:40','__category','__nrType','__nrId','categoryIndexSuggestFields','sreworks','["__id"]');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (15001,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','__nrType','__nrId','categoryIndexSuggestFields','sreworks','["__id"]');
-- INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (15002,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_productops_node','__nrId','categoryIndexSuggestFields','sreworks','["__label","config.label","config.name","config.keywords","config.*"]');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (15002,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_productops_node','__nrId','categoryIndexSuggestFields','sreworks','["__label","config.label","config.name","config.keywords"]');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (15003,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_cluster','__nrId','categoryIndexSuggestFields','sreworks','["__label","cluster_name","name","description"]');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (15004,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_team','__nrId','categoryIndexSuggestFields','sreworks','["__label","name","description"]');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (15005,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_metric','__nrId','categoryIndexSuggestFields','sreworks','["__label","name","metric_type","alias","description"]');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (15006,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_app','__nrId','categoryIndexSuggestFields','sreworks','["__label","name","labels","description"]');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (15007,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_event','__nrId','categoryIndexSuggestFields','sreworks','["__label","name","app_id","app_name", "app_component_name", "ex_config.type"]');


-- ----------------------------
-- typeTTL
-- ----------------------------
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (16000,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','nr_type','__nrId','typeTTL','','86400');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (16001,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','sreworks_productops_node','__nrId','typeTTL','','86400');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (16002,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','sreworks_cluster','__nrId','typeTTL','','86400');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (16003,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','sreworks_team','__nrId','typeTTL','','86400');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (16004,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','sreworks_metric','__nrId','typeTTL','','86400');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (16005,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','sreworks_app','__nrId','typeTTL','','86400');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (16006,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','sreworks_event','__nrId','typeTTL','','86400');


-- ----------------------------
-- categoryTypeExtraType 扩展类别标识
-- ----------------------------
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (17001,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','sreworks_cluster','__nrId','categoryTypeExtraType','','sreworks_entity');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (17002,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','sreworks_team','__nrId','categoryTypeExtraType','','sreworks_entity');
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (17003,'2019-02-28 01:19:27','2019-02-28 01:19:36','__category','sreworks_app','__nrId','categoryTypeExtraType','','sreworks_entity');


-- ----------------------------
-- categoryIndexMatchFields
-- ----------------------------
INSERT INTO `config` (`id`,`gmt_create`,`gmt_modified`,`category`,`nr_type`,`nr_id`,`name`,`modifier`,`content`) VALUES (18002,'2020-03-24 22:46:53','2020-04-03 00:57:40','sreworks-search','sreworks_productops_node','__nrId','categoryIndexMatchFields','sreworks','["config.label.full", "config.name.full", "config.keywords.full", "__id.lowercase^20", "__id.keyword^40", "__id.standard^35","__label.lowercase^20", "__label.keyword^40", "__label.standard^35"]');


-- ----------------------------
-- Consumer init
-- ----------------------------
REPLACE INTO `consumer`(`id`,`gmt_create`,`gmt_modified`,`modifier`,`creator`,`import_config`,`source_info`,`source_type`,`offset`,`name`,`enable`,`user_import_config`)
VALUES (1,'2021-09-29 09:51:13','2021-09-29 11:51:19','{{x-empid}}','{{x-empid}}','[[{"__type":"sreworks_productops_node","__id":"${id}","__gmt_create":"${gmt_create}","__gmt_modified":"${gmt_modified}","__label":"站点 导航 站点导航 前端","config":"${config}","node_type_path":"${node_type_path}","parent_node_type_path":"${parent_node_type_path}"}]]','{"startPeriod":0,"password":"${DB_PASSWORD}","port":${DB_PORT},"isPartition":false,"host":"${DB_HOST}","interval":900,"db":"abm_paas_action","username":"${DB_USER}","sql":"select * from productops_node where stage_id=\'prod\' and config not like \'%\\"hidden\\":true%\' and config not like \'%detail%\';"}','mysqlTable','0','sreworks_productops_node','true','{}');

REPLACE INTO `consumer`(`id`,`gmt_create`,`gmt_modified`,`modifier`,`creator`,`import_config`,`source_info`,`source_type`,`offset`,`name`,`enable`,`user_import_config`)
VALUES (2,'2021-09-29 09:51:13','2021-09-29 11:51:19','{{x-empid}}','{{x-empid}}','[[{"__type":"sreworks_cluster","__id":"${id}","__gmt_create":"${gmt_create}","__gmt_modified":"${gmt_modified}","__label":"集群 集群实体","account_id":"${account_id}","cluster_name":"${cluster_name}","name":"${name}","team_id":"${team_id}","description":"${description}"}]]','{"startPeriod":0,"password":"${DB_PASSWORD}","port":${DB_PORT},"isPartition":false,"host":"${DB_HOST}","interval":900,"db":"sreworks_meta","username":"${DB_USER}","sql":"select * from cluster;"}','mysqlTable','0','sreworks_cluster','true','{}');

REPLACE INTO `consumer`(`id`,`gmt_create`,`gmt_modified`,`modifier`,`creator`,`import_config`,`source_info`,`source_type`,`offset`,`name`,`enable`,`user_import_config`)
VALUES (3,'2021-09-29 09:51:13','2021-09-29 11:51:19','{{x-empid}}','{{x-empid}}','[[{"__type":"sreworks_team","__id":"${id}","__gmt_create":"${gmt_create}","__gmt_modified":"${gmt_modified}","name":"${name}","description":"${description}","avatar":"${avatar}","__label":"团队 团队实体"}]]','{"startPeriod":0,"password":"${DB_PASSWORD}","port":${DB_PORT},"isPartition":false,"host":"${DB_HOST}","interval":900,"db":"sreworks_meta","username":"${DB_USER}","sql":"select * from team;"}','mysqlTable','0','sreworks_team','true','{}');

REPLACE INTO `consumer`(`id`,`gmt_create`,`gmt_modified`,`modifier`,`creator`,`import_config`,`source_info`,`source_type`,`offset`,`name`,`enable`,`user_import_config`)
VALUES (4,'2021-09-29 09:51:13','2021-09-29 11:51:19','{{x-empid}}','{{x-empid}}','[[{"__type":"sreworks_app","__id":"${id}","__gmt_create":"${gmt_create}","__gmt_modified":"${gmt_modified}","__label":"应用 应用实体","account_id":"${account_id}","name":"${name}","team_id":"${team_id}","labels":"${labels}","description":"${description}"}]]','{"startPeriod":0,"password":"${DB_PASSWORD}","port":${DB_PORT},"isPartition":false,"host":"${DB_HOST}","interval":900,"db":"sreworks_meta","username":"${DB_USER}","sql":"select * from app;"}','mysqlTable','0','sreworks_app','true','{}');

REPLACE INTO `consumer`(`id`,`gmt_create`,`gmt_modified`,`modifier`,`creator`,`import_config`,`source_info`,`source_type`,`offset`,`name`,`enable`,`user_import_config`)
VALUES (5,'2021-09-29 09:51:13','2021-09-29 11:51:19','{{x-empid}}','{{x-empid}}','[[{"__type":"sreworks_event","__id":"${id}","__gmt_create":"${gmt_create}","__gmt_modified":"${gmt_modified}","__label":"应用事件","name":"${name}","app_id":"${app_id}","app_name":"${app_name}","app_component_name":"${app_component_name}","ex_config":"${ex_config}","description":"${description}"}]]','{"startPeriod":0,"password":"${DATA_DB_PASSWORD}","port":${DATA_DB_PORT},"isPartition":false,"host":"${DATA_DB_HOST}","interval":900,"db":"sw_saas_health","username":"${DATA_DB_USER}","sql":"select * from common_definition where category=\'event\';"}','mysqlTable','0','sreworks_event','true','{}');

REPLACE INTO `consumer`(`id`,`gmt_create`,`gmt_modified`,`modifier`,`creator`,`import_config`,`source_info`,`source_type`,`offset`,`name`,`enable`,`user_import_config`)
VALUES (10,'2021-09-29 09:51:13','2021-09-29 11:51:19','{{x-empid}}','{{x-empid}}','[[{"__type":"sreworks_metric","__id":"${id}","__gmt_create":"${gmt_create}","__gmt_modified":"${gmt_modified}","__label":"可观测 指标","name":"${name}", "alias":"${alias}", "metric_type":"${type}","labels":"${labels}", "description":"${description}"}]]','{"startPeriod":0,"password":"${DATA_DB_PASSWORD}","port":${DATA_DB_PORT},"isPartition":false,"host":"${DATA_DB_HOST}","interval":900,"db":"pmdb","username":"${DATA_DB_USER}","sql":"select * from metric;"}','mysqlTable','0','sreworks_metric','true','{}');

REPLACE INTO `consumer`(`id`,`gmt_create`,`gmt_modified`,`modifier`,`creator`,`import_config`,`source_info`,`source_type`,`offset`,`name`,`enable`,`user_import_config`)
VALUES (20,'2021-09-29 09:51:13','2021-09-29 11:51:19','{{x-empid}}','{{x-empid}}','[[{"__type":"sreworks_action","__id":"${id}","__label":"操作 审计","uuid":"${uuid}", "app_code":"${app_code}", "action_type":"${action_type}", "action_name":"${action_name}", "action_label":"${action_label}", "action_meta_data":"${action_meta_data}", "emp_id":"${emp_id}", "processor":"${processor}", "status":"${status}", "exec_data":"${exec_data}", "create_time":"${create_time}", "start_time":"${start_time}", "end_time":"${end_time}"}]]','{"startPeriod":0,"password":"${DB_PASSWORD}","port":${DB_PORT},"isPartition":false,"host":"${DB_HOST}","interval":900,"db":"abm_paas_action","username":"${DB_USER}","sql":"select * from action;"}','mysqlTable','0','sreworks_action','true','{}');
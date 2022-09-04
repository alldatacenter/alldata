/******************************************/
/*   初始化数据   */
/******************************************/

-- ----------------------------
-- Consumer init
-- ----------------------------
REPLACE INTO `consumer`(`id`,`gmt_create`,`gmt_modified`,`modifier`,`creator`,`import_config`,`source_info`,`source_type`,`offset`,`name`,`enable`,`user_import_config`)
VALUES (1,'2021-09-29 09:51:13','2021-09-29 11:51:19','{{x-empid}}','{{x-empid}}','[[{"__type":"sreworks_productops_node","__id":"${id}","__gmt_create":"${gmt_create}","__gmt_modified":"${gmt_modified}","__label":"站点 导航 站点导航 前端","config":"${config}","node_type_path":"${node_type_path}","parent_node_type_path":"${parent_node_type_path}"}]]','{"startPeriod":0,"password":"${DB_PASSWORD}","port":${DB_PORT},"isPartition":false,"host":"${DB_HOST}","interval":900,"db":"abm_paas_action","username":"${DB_USER}","sql":"select t1.*, t2.config as tab_config from productops_node t1 left join productops_tab t2 on t1.node_type_path = t2.node_type_path where t1.stage_id=\'prod\' and t1.config not like \'%\\"hidden\\":true%\';"}','mysqlTable','0','sreworks_productops_node','true','{}');

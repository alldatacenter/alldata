ALTER TABLE `tc_dag_inst`
	ADD COLUMN `father_dag_inst_node_id` bigint unsigned NULL COMMENT '父节点',
	ADD KEY `idx_gmt_access_status` (`gmt_access`,`status`);

ALTER TABLE `tc_dag_inst_node`
	ADD COLUMN `global_params` longtext NULL COMMENT '作业参数',
	ADD COLUMN `global_object` longtext NULL COMMENT '用户传入的对象',
	ADD COLUMN `global_result` longtext NULL COMMENT '作业结果';
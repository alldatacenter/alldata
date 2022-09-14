ALTER TABLE `tc_dag_inst_node` ADD COLUMN `channel` VARCHAR(128)  NULL DEFAULT NULL AFTER `global_result`;
ALTER TABLE `tc_dag_inst_edge` ADD COLUMN `channel` VARCHAR(128) NULL DEFAULT NULL AFTER `status`;
ALTER TABLE `tc_dag_inst_node_std` ADD COLUMN `channel` VARCHAR(128) NULL DEFAULT NULL AFTER `dag_inst_id`;
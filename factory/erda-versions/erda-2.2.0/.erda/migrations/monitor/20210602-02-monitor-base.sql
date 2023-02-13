-- MIGRATION_BASE

-- maintainer: pjy
-- added time: 2021/06/02
-- filename: 4.0/migrations/monitor/v005/sp_alert_rules.sql

UPDATE `sp_alert_rules`
SET `enable` = 0
WHERE `alert_index` = 'kubernetes_node';
INSERT
`sp_alert_rules`(`alert_index`,`alert_scope`,`alert_type`,`attributes`,`name`,`template`) VALUES("kubernetes_node","org","kubernetes","{\"alert_group\":\"{{cluster_name}}-{{node_name}}\",\"level\":\"WARNING\",\"recover\":\"false\",\"tickets_metric_key\":\"{{node_name}}\"}","kubernetes节点异常","{\"filters\":[{\"operator\":\"in\",\"tag\":\"cluster_name\",\"value\":[\"$cluster_name\"]},{\"operator\":\"neq\",\"tag\":\"offline\",\"value\":\"true\"}],\"functions\":[{\"aggregator\":\"values\",\"field\":\"ready\",\"operator\":\"all\",\"value\":false},{\"aggregator\":\"value\",\"field\":\"ready_message\",\"field_script\":\"function invoke(fields, tags){ return tags.ready_message; }\"}],\"group\":[\"cluster_name\",\"node_name\"],\"metrics\":[\"kubernetes_node\"],\"outputs\":[\"alert\"],\"select\":{\"_meta\":\"#_meta\",\"_metric_scope\":\"#_metric_scope\",\"_metric_scope_id\":\"#_metric_scope_id\",\"cluster_name\":\"#cluster_name\",\"component_name\":\"#node_name\",\"node_name\":\"#node_name\",\"org_name\":\"#org_name\"},\"window\":1}");

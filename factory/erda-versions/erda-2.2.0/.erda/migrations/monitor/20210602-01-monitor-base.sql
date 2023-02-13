-- MIGRATION_BASE

-- maintainer: pjy
-- added time: 2021/05/31
-- filename: 4.0/migrations/monitor/v005/sp_alert_notify_template.sql

UPDATE `sp_alert_notify_template`
SET `enable` = 0
WHERE `alert_index` = 'kubernetes_node';
INSERT `sp_alert_notify_template`(`alert_index`, `alert_type`, `formats`, `name`, `target`, `template`, `title`,
                                  `trigger`)
VALUES ("kubernetes_node", "kubernetes",
        "{\"allocatable_memory_bytes_value\":\"size:byte\",\"capacity_memory_bytes_value\":\"size:byte\"}",
        "kubernetes节点异常", "dingding",
        "【kubernetes节点状态异常告警】\n\n集群: {{cluster_name}}\n\n机器: {{node_name}}\n\n信息: {{ready_message_value}}\n\n时间: {{timestamp}}\n",
        "【{{cluster_name}}集群kubernetes节点{{node_name}}节点异常告警】\n", "alert");
INSERT `sp_alert_notify_template`(`alert_index`, `alert_type`, `formats`, `name`, `target`, `template`, `title`,
                                  `trigger`)
VALUES ("kubernetes_node", "kubernetes",
        "{\"allocatable_memory_bytes_value\":\"size:byte\",\"capacity_memory_bytes_value\":\"size:byte\"}",
        "kubernetes节点异常", "ticket",
        "【kubernetes节点状态异常告警】\n\n集群: {{cluster_name}}\n\n机器: {{node_name}}\n\n信息: {{ready_message_value}}\n\n时间: {{timestamp}}\n",
        "【{{cluster_name}}集群kubernetes节点{{node_name}}节点异常告警】\n", "alert");
INSERT `sp_alert_notify_template`(`alert_index`, `alert_type`, `formats`, `name`, `target`, `template`, `title`,
                                  `trigger`)
VALUES ("kubernetes_node", "kubernetes",
        "{\"allocatable_memory_bytes_value\":\"size:byte\",\"capacity_memory_bytes_value\":\"size:byte\"}",
        "kubernetes节点异常", "email",
        "【kubernetes节点状态异常告警】\n\n集群: {{cluster_name}}\n\n机器: {{node_name}}\n\n信息: {{ready_message_value}}\n\n时间: {{timestamp}}\n",
        "【{{cluster_name}}集群kubernetes节点{{node_name}}节点异常告警】\n", "alert");
INSERT `sp_alert_notify_template`(`alert_index`, `alert_type`, `formats`, `name`, `target`, `template`, `title`,
                                  `trigger`)
VALUES ("kubernetes_node", "kubernetes",
        "{\"allocatable_memory_bytes_value\":\"size:byte\",\"capacity_memory_bytes_value\":\"size:byte\"}",
        "kubernetes节点异常", "mbox",
        "【kubernetes节点状态异常告警】\n\n集群: {{cluster_name}}\n\n机器: {{node_name}}\n\n信息: {{ready_message_value}}\n\n时间: {{timestamp}}\n",
        "【{{cluster_name}}集群kubernetes节点{{node_name}}节点异常告警】\n", "alert");
INSERT `sp_alert_notify_template`(`alert_index`, `alert_type`, `formats`, `name`, `target`, `template`, `title`,
                                  `trigger`)
VALUES ("kubernetes_node", "kubernetes",
        "{\"allocatable_memory_bytes_value\":\"size:byte\",\"capacity_memory_bytes_value\":\"size:byte\"}",
        "kubernetes节点异常", "webhook",
        "【kubernetes节点状态异常告警】\n\n集群: {{cluster_name}}\n\n机器: {{node_name}}\n\n信息: {{ready_message_value}}\n\n时间: {{timestamp}}\n",
        "【{{cluster_name}}集群kubernetes节点{{node_name}}节点异常告警】\n", "alert");
INSERT `sp_alert_notify_template`(`alert_index`, `alert_type`, `formats`, `name`, `target`, `template`, `title`,
                                  `trigger`)
VALUES ("kubernetes_node", "kubernetes", "{\"trigger_duration\":\"time:ms\"}", "kubernetes节点异常", "dingding",
        "【kubernetes节点状态异常恢复】\n\n集群: {{cluster_name}}\n\n机器: {{node_name}}\n\n持续时间: {{trigger_duration}}\n\n恢复时间: {{timestamp}}\n",
        "【{{cluster_name}}集群kubernetes节点{{node_name}}节点异常恢复】\n", "recover");
INSERT `sp_alert_notify_template`(`alert_index`, `alert_type`, `formats`, `name`, `target`, `template`, `title`,
                                  `trigger`)
VALUES ("kubernetes_node", "kubernetes", "{\"trigger_duration\":\"time:ms\"}", "kubernetes节点异常", "ticket",
        "【kubernetes节点状态异常恢复】\n\n集群: {{cluster_name}}\n\n机器: {{node_name}}\n\n持续时间: {{trigger_duration}}\n\n恢复时间: {{timestamp}}\n",
        "【{{cluster_name}}集群kubernetes节点{{node_name}}节点异常恢复】\n", "recover");
INSERT `sp_alert_notify_template`(`alert_index`, `alert_type`, `formats`, `name`, `target`, `template`, `title`,
                                  `trigger`)
VALUES ("kubernetes_node", "kubernetes", "{\"trigger_duration\":\"time:ms\"}", "kubernetes节点异常", "email",
        "【kubernetes节点状态异常恢复】\n\n集群: {{cluster_name}}\n\n机器: {{node_name}}\n\n持续时间: {{trigger_duration}}\n\n恢复时间: {{timestamp}}\n",
        "【{{cluster_name}}集群kubernetes节点{{node_name}}节点异常恢复】\n", "recover");
INSERT `sp_alert_notify_template`(`alert_index`, `alert_type`, `formats`, `name`, `target`, `template`, `title`,
                                  `trigger`)
VALUES ("kubernetes_node", "kubernetes", "{\"trigger_duration\":\"time:ms\"}", "kubernetes节点异常", "mbox",
        "【kubernetes节点状态异常恢复】\n\n集群: {{cluster_name}}\n\n机器: {{node_name}}\n\n持续时间: {{trigger_duration}}\n\n恢复时间: {{timestamp}}\n",
        "【{{cluster_name}}集群kubernetes节点{{node_name}}节点异常恢复】\n", "recover");
INSERT `sp_alert_notify_template`(`alert_index`, `alert_type`, `formats`, `name`, `target`, `template`, `title`,
                                  `trigger`)
VALUES ("kubernetes_node", "kubernetes", "{\"trigger_duration\":\"time:ms\"}", "kubernetes节点异常", "webhook",
        "【kubernetes节点状态异常恢复】\n\n集群: {{cluster_name}}\n\n机器: {{node_name}}\n\n持续时间: {{trigger_duration}}\n\n恢复时间: {{timestamp}}\n",
        "【{{cluster_name}}集群kubernetes节点{{node_name}}节点异常恢复】\n", "recover");

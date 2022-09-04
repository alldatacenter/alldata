# coding: utf-8

import os
import json
import requests
from common.constant import host, demo_app_id

headers = {}

config_path = module_path = os.path.dirname(__file__)

task_dw_model_mapping = {
    "oem_pod_event": "POD_EVENT",
    "oem_demoApp_pod_event": "EVENT_INSTANCE",
    "oem_demoApp_ram_efficiency": "METRIC_DATA",
    "oem_demoApp_cpu_efficiency": "METRIC_DATA",
    "oem_demoApp_response_time_avg": "METRIC_DATA",
    "oem_demoApp_success_rate": "METRIC_DATA",
    "oem_demoApp_user_order_failed_cnt": "METRIC_DATA",
    "oem_app_delivery": "DELIVERY",
    "oem_app_deployment": "DEPLOYMENT",
    "oem_app_delivery_deployment_quality": "APP_DELIVERY_DEPLOYMENT_QUALITY",
    "oem_app_delivery_deployment_efficiency": "APP_DELIVERY_DEPLOYMENT_EFFICIENCY",
    "oem_app_pod_resource_allocated": "POD_RESOURCE_ALLOCATION",
    "oem_cluster_resource_price": "RESOURCE_PRICE",
    "oem_app_pod_resource_allocated_1d": "POD_RESOURCE_ALLOCATION_STATS",
    "oem_app_resource_allocated_cost_1d": "APP_COST",
    "oem_app_risk_stats_1d": "RISK_STATS",
    "oem_app_alert_stats_1d": "ALERT_STATS",
    "oem_app_incident_stats_1d": "INCIDENT_STATS",
    "oem_app_failure_stats_1d": "FAILURE_STATS",
    "oem_app_health_stats_1d": "APP_HEALTH",
    "oem_app_risk_definition": "RISK",
    "oem_app_alert_definition": "ALERT",
    "oem_app_incident_definition": "INCIDENT",
    "oem_app_failure_definition": "FAILURE",
    "oem_app_risk_instance": "RISK_INSTANCE",
    "oem_app_alert_instance": "ALERT_INSTANCE",
    "oem_app_incident_instance": "INCIDENT_INSTANCE",
    "oem_app_failure_instance": "FAILURE_INSTANCE",
    "oem_app_pod_status": "APP_POD_STATUS"
}

task_metric_mapping = {
    # "oem_demoApp_ram_efficiency": "app_ram_efficiency",
    # "oem_demoApp_cpu_efficiency": "app_cpu_efficiency",
    "oem_demoApp_user_order_failed_cnt": "user_order_failed_cnt",
    # "oem_demoApp_response_time_avg": "app_resp_time_avg",
    # "oem_demoApp_success_rate": "app_success_rate",
}


def get_dw_models():
    r = requests.get(host["warehouse"] + "/model/meta/getModels", headers=headers)
    models = {}
    if r.status_code == 200:
        datas = r.json().get("data", None)
        for data in datas:
            models[data["name"]] = data["id"]
    return models


def get_pmdb_metric_id(metric_name, labels):
    if not labels:
        labels = {}

    r = requests.post(host["pmdb"] + "/metric/getMetricsByLabels?name=" + metric_name, headers=headers, json=labels)
    metric_id = None
    if r.status_code == 200:
        datas = r.json().get("data", None)
        if datas:
            metric_id = datas[0].get("id", None)
    return metric_id


def load_job_configs():
    with open(config_path + "/collect_job_config.json", 'r') as load_f:
        job_configs = json.load(load_f)

    models = get_dw_models()

    for job_config in job_configs:
        task_list = job_config.get("scheduleConf", {}).get("taskIdList", [])
        for task in task_list:
            scene_conf = task.get("sceneConf", {})
            sync_dw = scene_conf.get("syncDw", False)
            related_metric_id = scene_conf.get("relatedMetricId", None)
            if sync_dw:
                dw_type = scene_conf["type"]
                # 暂时没有同步实体作业
                dw_name = task_dw_model_mapping.get(task["name"], None)
                if dw_name is None:
                    print(task["name"])
                    print("任务需要同步数仓,但模型信息配置失败")
                    return None
                else:
                    dw_id = models.get(dw_name)
                    scene_conf["id"] = dw_id
            if related_metric_id is not None:
                metric_name = task_metric_mapping.get(task["name"], None)
                labels = {"app_id": demo_app_id}
                metric_id = get_pmdb_metric_id(metric_name, labels)
                if metric_id is None:
                    print("任务需要关联指标,但指标信息配置失败")
                    return None
                else:
                    scene_conf["relatedMetricId"] = metric_id

    return job_configs


def add_jobs():
    job_configs = load_job_configs()
    if job_configs is None:
        print("作业同步未完成")
        return 0

    url = host["job-master"] + "/imEx/im"
    for job_config in job_configs:
        r = requests.post(url, headers=headers, json=[job_config])
        if r.status_code == 200:
            print(r.json())

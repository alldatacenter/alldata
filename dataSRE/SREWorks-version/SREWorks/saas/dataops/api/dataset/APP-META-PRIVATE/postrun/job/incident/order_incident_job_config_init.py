# coding: utf-8

import os
import json
import requests
from common.constant import host, demo_app_id

headers = {}

current_incident_def_name = "订单服务异常"
next_incident_def_name = "交易服务异常"

config_path = module_path = os.path.dirname(__file__)


def get_app_oem_def(category, def_name):
    endpoint = host["health"] + "/definition/getDefinitions?appId={app_id}&name={def_name}&category={category}"\
        .format(app_id=demo_app_id, def_name=def_name, category=category)
    r = requests.get(endpoint, headers=headers)
    datas = r.json().get("data", None)
    if datas:
        return datas[0]["id"]
    return None


def load_job_configs():
    with open(config_path + "/order_incident_job_config.json", 'r') as load_f:
        job_configs = json.load(load_f)

    for job_config in job_configs:
        event_conf = job_config.get("eventConf", [])
        if event_conf:
            current_incident_def_id = get_app_oem_def("incident", current_incident_def_name)
            if current_incident_def_id is None:
                print("诊断任务需要订阅告警TOPIC,但异常TOPIC配置失败")
                return None
            event_conf[0]["config"]["topics"] = ["sreworks-health-incident-" + str(current_incident_def_id)]

        task_list = job_config.get("scheduleConf", {}).get("taskIdList", [])
        for task in task_list:
            scene_conf = task.get("sceneConf", {})
            model_id = scene_conf.get("modelId", None)
            if model_id is not None:
                next_incident_def_id = get_app_oem_def("incident", next_incident_def_name)
                if next_incident_def_id is None:
                    print("诊断任务需要关联异常定义,但异常定义配置失败")
                    return None
                else:
                    scene_conf["modelId"] = next_incident_def_id

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

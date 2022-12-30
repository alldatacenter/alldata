# coding: utf-8

import os
import json
import requests
from common.constant import host, demo_app_id

headers = {}

def_name = "用户下单耗时高"

task_risk_def_mapping = {
    "oem_user_order_latency": def_name
}

config_path = module_path = os.path.dirname(__file__)


def get_app_oem_risk_def():
    endpoint = host["health"] + "/definition/getDefinitions?appId={app_id}&name={def_name}&category=risk"\
        .format(app_id=demo_app_id, def_name=def_name)
    r = requests.get(endpoint, headers=headers)
    datas = r.json().get("data", None)
    if datas:
        return datas[0]["id"]
    return None


def load_job_configs():
    with open(config_path + "/risk_job_config.json", 'r') as load_f:
        job_configs = json.load(load_f)

    for job_config in job_configs:
        task_list = job_config.get("scheduleConf", {}).get("taskIdList", [])
        for task in task_list:
            scene_conf = task.get("sceneConf", {})
            model_id = scene_conf.get("modelId", None)
            if model_id is not None:
                def_id = get_app_oem_risk_def()
                if def_id is None:
                    print("巡检任务需要关联风险定义,但风险定义配置失败")
                    return None
                else:
                    scene_conf["modelId"] = def_id

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

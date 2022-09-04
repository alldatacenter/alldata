# coding: utf-8

import os
import json
import requests
from common.constant import host, demo_app_id

headers = {}

config_path = module_path = os.path.dirname(__file__)


def load_job_configs():
    with open(config_path + "/normal_job_config.json", 'r') as load_f:
        job_configs = json.load(load_f)

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

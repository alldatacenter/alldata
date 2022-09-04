# coding: utf-8

import requests
from common.constant import host, demo_app_id, demo_app_name

headers = {}

page_size = 1000
one_millisecond = 1000
one_minute_millisecond = 60000
one_hour_millisecond = 3600000

app_metric_metas = [
    # {
    #     "name": "app_cpu_efficiency",
    #     "alias": "应用CPU水位",
    #     "type": "性能指标",
    #     "creator": "sreworks",
    #     "last_modifier": "sreworks",
    #     "description": "应用CPU水位(内置)"
    # },
    # {
    #     "name": "app_ram_efficiency",
    #     "alias": "应用RAM水位",
    #     "type": "性能指标",
    #     "creator": "sreworks",
    #     "last_modifier": "sreworks",
    #     "description": "应用RAM水位(内置)"
    # },
    # {
    #     "name": "app_pvc_allocation",
    #     "alias": "应用PVC分配量",
    #     "type": "性能指标",
    #     "creator": "sreworks",
    #     "last_modifier": "sreworks",
    #     "description": "应用PVC分配量GB(内置)"
    # },
    # {
    #     "name": "app_resp_time_avg",
    #     "alias": "应用平均响应时间",
    #     "type": "性能指标",
    #     "creator": "sreworks",
    #     "last_modifier": "sreworks",
    #     "description": "应用平均响应时间(内置)"
    # },
    # {
    #     "name": "app_success_rate",
    #     "alias": "应用请求成功率",
    #     "type": "性能指标",
    #     "creator": "sreworks",
    #     "last_modifier": "sreworks",
    #     "description": "应用请求成功率SLA(内置)"
    # },
    {
        "name": "user_order_success_rate",
        "alias": "用户订单成功率指标",
        "type": "业务指标",
        "creator": "sreworks",
        "last_modifier": "sreworks",
        "description": "应用业务DEMO指标(内置)"
    },
    {
        "name": "user_order_failed_cnt",
        "alias": "用户订单失败次数指标",
        "type": "业务指标",
        "creator": "sreworks",
        "last_modifier": "sreworks",
        "description": "应用业务DEMO指标(内置)"
    }
]


# def get_apps():
#     endpoint = host["app"] + "/appdev/app/listAll"
#     r = requests.get(endpoint, headers=headers)
#     datas = r.json().get("data", None)
#     apps = []
#     for data in datas:
#         if data["appId"] == demo_app_id:
#             app = {
#                 "app_id": data["appId"],
#                 "app_name": data["name"]
#             }
#             apps.append(app)
#             break
#     return apps

def get_apps():
    apps = [{
        "app_id": demo_app_id,
        "app_name": demo_app_name
    }]
    return apps


def add_metrics():
    url = host["pmdb"] + "/metric/createMetric"
    apps = get_apps()
    for app in apps:
        for metric_meta in app_metric_metas:
            app_metric = {
                "name": metric_meta["name"],
                "alias": metric_meta["alias"],
                "type": metric_meta["type"],
                "creator": metric_meta["creator"],
                "last_modifier": metric_meta["last_modifier"],
                "description": metric_meta["description"],
                "labels": app
            }
            r = requests.post(url, headers=headers, json=app_metric)
            if r.status_code == 200:
                print(r.json())

# coding: utf-8

import requests
from common.constant import host, demo_app_id, demo_app_name, demo_app_component_name

headers = {}

alert_definition_meta = {
    "description": "应用DEMO告警定义",
    "exConfig": {
        "metricId": 4,
        "storageDays": 7,
        "enable": True,
        "weight": 3,
        "granularity": 1,
        "alertRuleConfig": {
            "duration": 1,
            "times": 3,
            "comparator": ">=",
            "math_abs": False,
            "thresholds": {
                "WARNING": 6,
                "CRITICAL": 8
            }
        },
        "noticeConfig": {
            "WARNING": [
                "DING_TALK"
            ],
            "CRITICAL": [
                "EMAIL",
                "DING_TALK"
            ]
        }
    },
    "name": "用户下单失败",
    "category": "alert"
}


# def get_apps():
#     endpoint = host["app"] + "/appdev/app/listAll"
#     r = requests.get(endpoint, headers=headers)
#     datas = r.json().get("data", None)
#     apps = []
#     for data in datas:
#         if data["appId"] == demo_app_id:
#             app = {
#                 "app_id": data["appId"],
#                 "app_name": data["name"],
#                 "app_component_name": "mall"
#             }
#             apps.append(app)
#             break
#     return apps


def get_apps():
    apps = [{
        "app_id": demo_app_id,
        "app_name": demo_app_name,
        "app_component_name": demo_app_component_name
    }]
    return apps


def get_pmdb_metric(labels):
    url = host["pmdb"] + "/metric/getMetricsByLabels?name=user_order_failed_cnt"
    r = requests.post(url, headers=headers, json=labels)
    datas = r.json().get("data", None)
    return datas


def add_alert_definitions():
    apps = get_apps()
    url = host["health"] + "/definition/createDefinition"
    for app in apps:
        app_alert_definition = {
            "description": alert_definition_meta["description"],
            "exConfig": alert_definition_meta["exConfig"],
            "name": alert_definition_meta["name"],
            "category": alert_definition_meta["category"],
            "appId": app["app_id"],
            "appName": app["app_name"],
            "appComponentName": app["app_component_name"]
        }

        labels = {
            "app_id": app["app_id"],
            "app_name": app["app_name"],
        }
        metrics = get_pmdb_metric(labels)
        print(metrics)
        if len(metrics) != 1:
            print("指标定义查询结果异常, 指标定义为空或者不唯一")
            return 0
        metric = metrics[0]
        app_alert_definition["exConfig"]["metricId"] = metric["id"]

        r = requests.post(url, headers=headers, json=app_alert_definition)
        if r.status_code == 200:
            print(r.json())

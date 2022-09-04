# coding: utf-8

import requests
from common.constant import host, demo_app_id, demo_app_name, demo_app_component_name

headers = {}

risk_definition_meta = {
    "description": "应用DEMO风险定义",
    "exConfig": {
        "tags": [
            "业务风险"
        ],
        "storageDays": 7,
        "enable": True,
        "weight": 2
    },
    "name": "用户下单耗时高",
    "category": "risk"
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


def add_risk_definitions():
    apps = get_apps()
    url = host["health"] + "/definition/createDefinition"
    for app in apps:
        app_risk_definition = {
            "description": risk_definition_meta["description"],
            "exConfig": risk_definition_meta["exConfig"],
            "name": risk_definition_meta["name"],
            "category": risk_definition_meta["category"],
            "appId": app["app_id"],
            "appName": app["app_name"],
            "appComponentName": app["app_component_name"]
        }
        r = requests.post(url, headers=headers, json=app_risk_definition)
        if r.status_code == 200:
            print(r.json())

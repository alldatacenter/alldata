# coding: utf-8

import requests
from common.constant import host, demo_app_id, demo_app_name, demo_app_component_name

headers = {}

incident_type = {
    "name": "服务不可用异常",
    "description": "服务不可用异常类型(该类型异常参与计算服务可用率)",
    "label": "SERVICE_UNAVAILABLE"
}

incident_definition_metas = [
    {
        "description": "应用DEMO异常定义",
        "exConfig": {
            "typeId": 1,
            "selfHealing": True,
            "weight": 5
        },
        "name": "订单服务异常",
        "category": "incident"
    },
    {
        "description": "应用DEMO异常定义",
        "exConfig": {
            "typeId": 1,
            "selfHealing": True,
            "weight": 8
        },
        "name": "交易服务异常",
        "category": "incident"
    },
    {
        "description": "应用DEMO异常定义",
        "exConfig": {
            "typeId": 1,
            "selfHealing": True,
            "weight": 10
        },
        "name": "用户鉴权服务异常",
        "category": "incident"
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


def add_incident_type():
    url = host["health"] + "/incident_type/createIncidentType"
    r = requests.post(url, headers=headers, json=incident_type)
    if r.status_code == 200:
        print(r.json())


def add_incident_definitions():
    apps = get_apps()
    url = host["health"] + "/definition/createDefinition"
    for app in apps:
        for incident_definition_meta in incident_definition_metas:
            app_incident_definition = {
                "description": incident_definition_meta["description"],
                "exConfig": incident_definition_meta["exConfig"],
                "name": incident_definition_meta["name"],
                "category": incident_definition_meta["category"],
                "appId": app["app_id"],
                "appName": app["app_name"],
                "appComponentName": app["app_component_name"]
            }
            r = requests.post(url, headers=headers, json=app_incident_definition)
            if r.status_code == 200:
                print(r.json())

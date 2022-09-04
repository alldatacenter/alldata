# coding: utf-8

import requests
from common.constant import host, demo_app_id, demo_app_name, demo_app_component_name

headers = {}

failure_definition_meta = {
    "description": "应用DEMO故障定义",
    "exConfig": {
        "refIncidentDefId": 4,
        "failureLevelRule": {
            "P0": "60m",
            "P1": "30m",
            "P2": "20m",
            "P3": "10m",
            "P4": "5m"
        }
    },
    "name": "订单服务故障",
    "category": "failure"
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


def get_incident_definition_id(app_id, app_component_name):
    endpoint = host["health"] + "/definition/getDefinitions?category=incident&name=订单服务异常&appId={app_id}&appComponentName={app_component_name}".format(app_id=app_id, app_component_name=app_component_name)
    r = requests.get(endpoint, headers=headers)
    if r.status_code == 200:
        datas = r.json().get("data", None)
        if len(datas) == 1:
            return datas[0]["id"]
        else:
            return None


def add_failure_definitions():
    apps = get_apps()
    url = host["health"] + "/definition/createDefinition"
    for app in apps:
        incident_id = get_incident_definition_id(app["app_id"], app["app_component_name"])
        if incident_id is None:
            print("未查到需要配置故障的异常定义")
            return 0

        app_failure_definition = {
            "description": failure_definition_meta["description"],
            "exConfig": failure_definition_meta["exConfig"],
            "name": failure_definition_meta["name"],
            "category": failure_definition_meta["category"],
            "appId": app["app_id"],
            "appName": app["app_name"],
            "appComponentName": app["app_component_name"]
        }
        app_failure_definition["exConfig"]["refIncidentDefId"] = incident_id

        r = requests.post(url, headers=headers, json=app_failure_definition)
        if r.status_code == 200:
            print(r.json())

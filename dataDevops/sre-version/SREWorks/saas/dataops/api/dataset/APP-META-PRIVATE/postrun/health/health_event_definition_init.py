# coding: utf-8

import requests
from common.constant import host, demo_app_id, demo_app_name, demo_app_component_name

headers = {}

even_definition_meta = {
    "description": "资源类应用运维事件",
    "exConfig": {
        "type": "POD事件",
        "storageDays": 7
    },
    "name": "应用POD事件",
    "category": "event"
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


def add_event_definitions():
    apps = get_apps()
    url = host["health"] + "/definition/createDefinition"
    for app in apps:
        app_even_definition = {
            "description": even_definition_meta["description"],
            "exConfig": even_definition_meta["exConfig"],
            "name": even_definition_meta["name"],
            "category": even_definition_meta["category"],
            "appId": app["app_id"],
            "appName": app["app_name"],
            "appComponentName": app["app_component_name"]
        }
        r = requests.post(url, headers=headers, json=app_even_definition)
        if r.status_code == 200:
            print(r.json())

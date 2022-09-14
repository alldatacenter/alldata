# coding: utf-8

import requests
from common.constant import host

headers = {}
# host = {
#     "warehouse": "http://prod-dataops-warehouse.sreworks-dataops.svc.cluster.local:80"
# }

url = host["warehouse"] + "/domain/createDomain"
sw_domains = [
    {
        "buildIn": True,
        "subject": "运维服务主题",
        "name": "稳定性域",
        "description": "运维稳定性相关模型数据域",
        "abbreviation": "stability"
    },
    {
        "buildIn": True,
        "subject": "运维服务主题",
        "name": "成本域",
        "description": "运维成本相关模型数据域",
        "abbreviation": "cost"
    },
    {
        "buildIn": True,
        "subject": "运维服务主题",
        "name": "效率域",
        "description": "运维效率相关模型数据域",
        "abbreviation": "efficiency"
    },
    {
        "buildIn": True,
        "subject": "运维数据主题",
        "name": "原始数据域",
        "description": "原始相关模型数据域",
        "abbreviation": "original"
    },
    {
        "buildIn": True,
        "subject": "运维数据主题",
        "name": "衍生数据域",
        "description": "衍生相关模型数据域",
        "abbreviation": "derivation"
    },
    {
        "buildIn": True,
        "subject": "运维对象主题",
        "name": "资源域",
        "description": "资源模型数据域",
        "abbreviation": "resource"
    },
    {
        "buildIn": True,
        "subject": "运维对象主题",
        "name": "系统域",
        "description": "系统模型数据域",
        "abbreviation": "system"
    },
    {
        "buildIn": True,
        "subject": "运维对象主题",
        "name": "平台域",
        "description": "平台模型数据域",
        "abbreviation": "platform"
    },
    {
        "buildIn": True,
        "subject": "运维对象主题",
        "name": "业务域",
        "description": "业务模型数据域",
        "abbreviation": "business"
    }
]


def add_sw_domains():
    for sw_domain in sw_domains:
        r = requests.post(url, headers=headers, json=sw_domain)
        if r.status_code == 200:
            print(r.json())

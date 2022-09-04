# coding: utf-8

import os
import requests
from requests.auth import HTTPBasicAuth
from common.constant import host

headers = {
    "kbn-version": "7.10.2"
}

ilm_policy = {
    "metricbeat": {
        "name": "metricbeat",
        "phases": {
            "hot": {
                "min_age": "0ms",
                "actions": {
                    "rollover": {
                        "max_size": "35gb",
                        "max_age": "15d"
                    }
                }
            },
            "delete": {
                "min_age": "0d",
                "actions": {
                    "delete": {}
                }
            }
        }
    },
    "filebeat": {
        "name": "filebeat",
        "phases": {
            "hot": {
                "min_age": "0ms",
                "actions": {
                    "rollover": {
                        "max_size": "25gb",
                        "max_age": "15d"
                    }
                }
            },
            "delete": {
                "min_age": "0d",
                "actions": {
                    "delete": {}
                }
            }
        }
    }
}


def set_ilm_policy():
    username = os.getenv("DATA_ES_USER", None)
    password = os.getenv("DATA_ES_PASSWORD", None)
    auth = None
    if username and password:
        auth = HTTPBasicAuth(username, password)

    url = host["kibana"] + "/api/index_lifecycle_management/policies"
    for policy_name, policy_config in ilm_policy.items():
        if auth:
            r = requests.post(url, headers=headers, auth=auth, json=policy_config)
        else:
            r = requests.post(url, headers=headers, json=policy_config)
        print(r.status_code)


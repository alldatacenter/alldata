import json
import os
import requests
from requests.auth import HTTPBasicAuth

#url = "http://prod-dataops-elasticsearch-master.sreworks-dataops:9200"
url = os.environ.get("ES_ENDPOINT")
username = os.environ.get("ES_USERNAME")
password = os.environ.get("ES_PASSWORD")

headers = {"Content-Type": "application/json"}

basic = HTTPBasicAuth(username, password)

data = {
    "persistent": {
        "action.auto_create_index": "-sreworks-job*,*"
    }
}

r = requests.put(url + "/_cluster/settings", data=json.dumps(data), headers=headers, auth=basic)

if r.status_code == 200:
    print(r.json())
else:
    print("status_code:%s" % r.status_code)
    print(r.text)


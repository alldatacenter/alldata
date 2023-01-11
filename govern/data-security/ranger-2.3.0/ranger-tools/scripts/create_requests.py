#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License. See accompanying LICENSE file.
#
import json

# Opening JSON file
f = open("test_servicetags_hive.json")

# returns JSON object as a dictionary
data = json.load(f)

final_list = []

# Iterating through the json list
for i in data['serviceResources']:
    resource_id = i['id']
    # dictionary with table, database, or column
    resource_elements = i['resourceElements']
    temp = {'name': "request-" + str(resource_id), 'request': {'resource': {'elements': {}}, 'accessType': "select", 'user': "hrt_1", 'userGroups': [], 'requestData': "request-" + str(resource_id)}, 'result': {'isAudited': 'true', 'isAllowed': 'false', 'policyId': resource_id}}

    resource_keys = resource_elements.keys()
    for resource_key in resource_keys:
        resource_item = resource_elements[resource_key]
        resource_value = resource_item['values'][0]
        temp['request']['resource']['elements'][resource_key] = resource_value

    final_list.append(temp)

# Writing JSON file

with open("test_requests_hive.json", "w") as outfile:
    json.dump(final_list, outfile)

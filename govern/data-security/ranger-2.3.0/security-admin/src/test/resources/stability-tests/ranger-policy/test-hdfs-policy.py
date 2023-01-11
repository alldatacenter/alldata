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

#!/usr/bin/env python

#Make sure script has sufficient privileges
# chmod 755 ./test-hdfs-policy.py

#Run the script
# python ./test-hdfs-policy.py --startIndex START_IDX --maxIteration MAX_ITERATION --incrementBy IDX_INCREMENT_BY --host ADMIN_HOST --username USERNAME --password PASSWORD --serviceName SERVICE_NAME
# python ./test-hdfs-policy.py --startIndex 1 --maxIteration 10 --incrementBy 5 --host "http://localhost:6080" --username "admin" --password "admin123" --serviceName "test_hdfs"

import sys
import time
import argparse
import json
import requests
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from datetime import datetime

requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

# Create the parser
my_parser = argparse.ArgumentParser(description='Create, Fetch & Delete Ranger Policies')

# Add the arguments
my_parser.add_argument('--host',         metavar='ranger-admin-host-with-port', type=str, help='Host (including port) of ranger admin', required=True)
my_parser.add_argument('--maxIteration', metavar='maximum-script-iterations',   type=int, help='Maximum number of iterations', required=True)
my_parser.add_argument('--startIndex',   metavar='starting-index-of-script',    type=int, help='Starting index of first iteration', required=True)
my_parser.add_argument('--incrementBy',  metavar='increment-by',                type=int, help='After each iteration, increment by', required=True)

my_parser.add_argument('--username',    metavar='login-username', type=str, help='UserName of user used to perform test', required=True)
my_parser.add_argument('--password',    metavar='login-password', type=str, help='Password of user used to perform test', required=True)
my_parser.add_argument('--serviceName', metavar='service-name',   type=str, help='Name of ranger-service for which script will performs test', required=True)


# Execute the parse_args() method
args = my_parser.parse_args()

session        = requests.Session()
session.auth   = (args.username, args.password)
session.verify = False

session.headers['Accept']       = 'application/json'
session.headers['Content-Type'] = 'application/json'

admin_create_url              = "{}/service/plugins/policies".format(args.host.rstrip('/'))
admin_create_data_template    = '{"policyType":"0","name":"","isEnabled":true,"policyPriority":0,"policyLabels":[],"description":"","isAuditEnabled":true,"resources":{"path":{"values":[],"isRecursive":true}},"isDenyAllElse":false,"policyItems":[{"users":[""],"groups":[""],"roles": [""],"accesses":[{"type":"read","isAllowed":true},{"type":"write","isAllowed":true},{"type":"execute","isAllowed":true}]}],"allowExceptions":[],"denyPolicyItems":[],"denyExceptions":[],"service":""}'
admin_delete_url_template     = "{}/service/assets/resources/{}".format(args.host.rstrip('/'), "{}")
admin_update_url_template     = "{}/service/plugins/policies/{}".format(args.host.rstrip('/'), "{}")
admin_get_url                 = "{}/service/public/v2/api/service/{}/policy".format(args.host.rstrip('/'), args.serviceName)
admin_get_service_url         = "{}/service/plugins/services/name/{}".format(args.host.rstrip('/'), args.serviceName)
admin_delete_usr_url_template = "{}/service/xusers/secure/users/{}?forceDelete=true".format(args.host.rstrip('/'), "{}")
admin_delete_grp_url_template = "{}/service/xusers/secure/groups/{}?forceDelete=true".format(args.host.rstrip('/'), "{}")
admin_delete_rol_url_template = "{}/service/roles/roles/name/{}".format(args.host.rstrip('/'), "{}")

def log(log_group, message):
   print(str(datetime.now()) + " [" + log_group + "] " + message)

def get_policy_version():
   resp = session.get(admin_get_service_url)
   assert resp.status_code == 200, "Failed to fetch policy version"

   return resp.json()["policyVersion"]

def test_create_policy(log_group, policy_json):
   log(log_group, " Create policy URL: " + str(admin_create_url))
   policy_version_v1 = get_policy_version()
   resp = session.post(admin_create_url, data=json.dumps(policy_json))
   policy_version_v2 = get_policy_version()

   assert resp.status_code == 200, "Create policy request failed"
   assert policy_version_v1 != policy_version_v2, "Policy version did not updated after create. policyVersion=" + policy_version_v2

   policy_id = resp.json()["id"]
   log(log_group, " Policy ID: " + str(policy_id))

   log(log_group, " Waiting for {} milliseconds".format(wait_between_commands))
   time.sleep(wait_between_commands/1000)
   return policy_id

def test_get_policy(log_group, policy_id, flag=True):
   log(log_group, " Get policy URL: " + str(admin_get_url))
   resp = session.get(admin_get_url)

   assert resp.status_code == 200, "Get policy request failed"
   resp_text = resp.text

   if flag:
      assert '"id":{}'.format(policy_id) in resp_text, resp_text
   else:
      assert not '"id":{}'.format(policy_id) in resp_text, resp_text

def test_update_policy(log_group, policy_json, policy_id):
   policy_json['description'] = "This is a test policy created to test policy cache functionality."
   policy_json['id'] = policy_id
   update_url = admin_update_url_template.format(policy_id)

   log(log_group, " Update policy URL: " + str(update_url))
   policy_version_v1 = get_policy_version()
   resp = session.put(update_url, data=json.dumps(policy_json))
   policy_version_v2 = get_policy_version()

   assert resp.status_code == 200, "Update policy request failed"
   assert policy_version_v1 != policy_version_v2, "Policy version did not updated after create. policyVersion=" + policy_version_v2

   log(log_group, " Waiting for {} milliseconds".format(wait_between_commands))
   time.sleep(wait_between_commands/1000)

def test_delete_policy(log_group, policy_id):
   delete_url = admin_delete_url_template.format(policy_id)

   log(log_group, " Delete policy URL: " + str(delete_url))
   policy_version_v1 = get_policy_version()
   resp = session.delete(delete_url)
   policy_version_v2 = get_policy_version()

   assert resp.status_code == 204, "Delete policy request failed"
   assert policy_version_v1 != policy_version_v2, "Policy version did not updated after create. policyVersion=" + policy_version_v2

def test_policy(log_group, policy_json):
   log(log_group, "[START] LastKnownPolicyVersion=" + str(get_policy_version()))

   # Create Policy
   policy_id = test_create_policy(log_group, policy_json)
   # Get Policy
   test_get_policy(log_group, policy_id)

   # Update Policy
   test_update_policy(log_group, policy_json, policy_id)
   # Get Policy
   test_get_policy(log_group, policy_id)

   # Delete Policy
   test_delete_policy(log_group, policy_id)
   # Get Policy
   test_get_policy(log_group, policy_id, False)

   log(log_group, "[END] LastKnownPolicyVersion=" + str(get_policy_version()))
   log(log_group, "test_policy function finished, waiting for {} milliseconds".format(wait_between_commands))
   time.sleep(wait_between_commands/1000)

def get_policy_json(in_fix=""):
   json_data = json.loads(admin_create_data_template)

   json_data['name']                        = "test-{}-{}".format(in_fix, currentIndex)
   json_data['service']                     = args.serviceName
   json_data['resources']['path']['values'] = ["/dummy-hdfs-path-{}-{}".format(in_fix, currentIndex)]

   return json_data

def test_delete_user(log_group, uname):
   delete_user_url = admin_delete_usr_url_template.format(uname)

   log(log_group, "Delete user URL: " + str(delete_user_url))
   resp = session.delete(delete_user_url)

   assert resp.status_code == 204, "Delete user request failed. [User=" + str(uname) + " ,, response code=" + str(resp.status_code) + "]"

def test_delete_group(log_group, gname):
   delete_group_url = admin_delete_grp_url_template.format(gname)

   log(log_group, "Delete group URL: " + str(delete_group_url))
   resp = session.delete(delete_group_url)

   assert resp.status_code == 204, "Delete group request failed. [Group=" + str(gname) + " ,, response code=" + str(resp.status_code) + "]"

def test_delete_role(log_group, rname):
   delete_role_url = admin_delete_rol_url_template.format(rname)

   log(log_group, "Delete role URL: " + str(delete_role_url))
   resp = session.delete(delete_role_url)

   assert resp.status_code == 204, "Delete role request failed. [Role=" + str(rname) + " ,, response code=" + str(resp.status_code) + "]"


currentIndex = args.startIndex
currentCycle = 1
appStartTime = time.time()

while currentCycle <= args.maxIteration:
   log("CYCLE", "======================== Starting Iteration/ Cycle {} ========================".format(currentCycle))
   wait_between_commands = 200
   wait_between_cycles = 1000

   # User
   user_start_time = time.time()
   json_data_user  = get_policy_json("u")
   user_name       = "test-ra-user-{}".format(currentIndex)

   for idx in range(len(json_data_user['policyItems'])):
      if len(json_data_user['policyItems'][idx]['users']) > 0:
         json_data_user['policyItems'][idx]['users'] = [user_name]

   log("CYCLE", "##### User ==>>")
   test_policy("USER", json_data_user)
   test_delete_user("USER", user_name)
   log("CYCLE", "##### User <<== consumed time = " + str(time.time() - user_start_time) + " seconds.")

   # Group
   group_start_time = time.time()
   json_data_group  = get_policy_json("g")
   group_name       = "test-ra-group-{}".format(currentIndex)

   for idx in range(len(json_data_group['policyItems'])):
      if len(json_data_group['policyItems'][idx]['groups']) > 0:
         json_data_group['policyItems'][idx]['groups'] = [group_name]

   log("CYCLE", "##### Group ==>>")
   test_policy("GROUP", json_data_group)
   test_delete_group("GROUP", group_name)
   log("CYCLE", "##### Group <<== consumed time = " + str(time.time() - group_start_time) + " seconds.")

   # Role
   role_start_time = time.time()
   json_data_role  = get_policy_json("r")
   role_name       = "test-ra-role-{}".format(currentIndex)

   for idx in range(len(json_data_role['policyItems'])):
      if len(json_data_role['policyItems'][idx]['roles']) > 0:
         json_data_role['policyItems'][idx]['roles'] = [role_name]

   log("CYCLE", "##### Role ==>>")
   test_policy("ROLE", json_data_role)
   test_delete_role("ROLE", role_name)
   log("CYCLE", "##### Role <<== consumed time = " + str(time.time() - role_start_time) + " seconds.")

   # Final loop logistics
   log("CYCLE", "<<=== Iteration/ Cycle {} complete. Waiting for {} milliseconds before next Iteration/ Cycle.".format(currentCycle, wait_between_cycles))
   currentIndex = currentIndex + args.incrementBy
   currentCycle = currentCycle + 1
   time.sleep(wait_between_cycles/1000)

log("SCRIPT", "##### Overall <<== consumed time = " + str(time.time() - appStartTime) + " seconds.")

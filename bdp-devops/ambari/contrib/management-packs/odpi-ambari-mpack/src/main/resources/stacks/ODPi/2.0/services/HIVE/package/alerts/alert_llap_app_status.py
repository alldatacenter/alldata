#!/usr/bin/env python

"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import time
import logging
import traceback
import json
import subprocess

from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from ambari_commons.os_check import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.core import shell
from resource_management.core.resources import Execute
from resource_management.core import global_lock
from resource_management.core.exceptions import Fail
from resource_management.libraries.script.script import Script

OK_MESSAGE = "The application reported a '{0}' state in {1:.3f}s"
MESSAGE_WITH_STATE_AND_INSTANCES = "The application reported a '{0}' state in {1:.3f}s. [Live: {2}, Desired: {3}]"
CRITICAL_MESSAGE_WITH_STATE = "The application reported a '{0}' state. Check took {1:.3f}s"
CRITICAL_MESSAGE = "Application information could not be retrieved"

# results codes
CRITICAL_RESULT_CODE = 'CRITICAL'
OK_RESULT_CODE = 'OK'
UKNOWN_STATUS_CODE = 'UNKNOWN'


SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'

HIVE_PRINCIPAL_KEY = '{{hive-interactive-site/hive.llap.zk.sm.principal}}'
HIVE_PRINCIPAL_DEFAULT = 'default.hive.principal'

HIVE_PRINCIPAL_KEYTAB_KEY = '{{hive-interactive-site/hive.llap.zk.sm.keytab.file}}'
HIVE_PRINCIPAL_KEYTAB_DEFAULT = 'default.hive.keytab'

HIVE_AUTHENTICATION_DEFAULT = 'NOSASL'

HIVE_USER_KEY = '{{hive-env/hive_user}}'
HIVE_USER_DEFAULT = 'default.smoke.user'

STACK_ROOT = '{{cluster-env/stack_root}}'
STACK_ROOT_DEFAULT = Script.get_stack_root()

LLAP_APP_NAME_KEY = '{{hive-interactive-env/llap_app_name}}'
LLAP_APP_NAME_DEFAULT = 'llap0'

# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = '{{kerberos-env/executable_search_paths}}'


CHECK_COMMAND_TIMEOUT_KEY = 'check.command.timeout'
CHECK_COMMAND_TIMEOUT_DEFAULT = 120.0


# Mapping of LLAP app states to 'user friendly' state names.
llap_app_state_dict = {'RUNNING_ALL': 'RUNNING',
                       'RUNNING_PARTIAL': 'RUNNING',
                       'COMPLETE': 'NOT RUNNING',
                       'LAUNCHING': 'LAUNCHING',
                       'APP_NOT_FOUND': 'APP NOT FOUND'}

logger = logging.getLogger('ambari_alerts')

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (SECURITY_ENABLED_KEY, KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY, HIVE_PRINCIPAL_KEY, HIVE_PRINCIPAL_KEYTAB_KEY,
          HIVE_USER_KEY, STACK_ROOT, LLAP_APP_NAME_KEY)


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  LLAP_APP_STATUS_CMD_TIMEOUT = 0

  if configurations is None:
    return ('UNKNOWN', ['There were no configurations supplied to the script.'])

  result_code = None

  try:
    security_enabled = False
    if SECURITY_ENABLED_KEY in configurations:
      security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

    check_command_timeout = CHECK_COMMAND_TIMEOUT_DEFAULT
    if CHECK_COMMAND_TIMEOUT_KEY in configurations:
      check_command_timeout = int(parameters[CHECK_COMMAND_TIMEOUT_KEY])

    hive_user = HIVE_USER_DEFAULT
    if HIVE_USER_KEY in configurations:
      hive_user = configurations[HIVE_USER_KEY]

    llap_app_name = LLAP_APP_NAME_DEFAULT
    if LLAP_APP_NAME_KEY in configurations:
      llap_app_name = configurations[LLAP_APP_NAME_KEY]

    if security_enabled:
      if HIVE_PRINCIPAL_KEY in configurations:
        llap_principal = configurations[HIVE_PRINCIPAL_KEY]
      else:
        llap_principal = HIVE_PRINCIPAL_DEFAULT
      llap_principal = llap_principal.replace('_HOST',host_name.lower())

      llap_keytab = HIVE_PRINCIPAL_KEYTAB_DEFAULT
      if HIVE_PRINCIPAL_KEYTAB_KEY in configurations:
        llap_keytab = configurations[HIVE_PRINCIPAL_KEYTAB_KEY]

      # Get the configured Kerberos executable search paths, if any
      if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
        kerberos_executable_search_paths = configurations[KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY]
      else:
        kerberos_executable_search_paths = None

      kinit_path_local = get_kinit_path(kerberos_executable_search_paths)
      kinitcmd=format("{kinit_path_local} -kt {llap_keytab} {llap_principal}; ")

      # prevent concurrent kinit
      kinit_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
      kinit_lock.acquire()
      try:
        Execute(kinitcmd, user=hive_user,
                path=["/bin/", "/usr/bin/", "/usr/lib/hive/bin/", "/usr/sbin/"],
                timeout=10)
      finally:
        kinit_lock.release()



    start_time = time.time()
    if STACK_ROOT in configurations:
      llap_status_cmd = configurations[STACK_ROOT] + format("/current/hive-server2-hive2/bin/hive --service llapstatus --name {llap_app_name}  --findAppTimeout {LLAP_APP_STATUS_CMD_TIMEOUT}")
    else:
      llap_status_cmd = STACK_ROOT_DEFAULT + format("/current/hive-server2-hive2/bin/hive --service llapstatus --name {llap_app_name} --findAppTimeout {LLAP_APP_STATUS_CMD_TIMEOUT}")

    code, output, error = shell.checked_call(llap_status_cmd, user=hive_user, stderr=subprocess.PIPE,
                                             timeout=check_command_timeout,
                                             logoutput=False)
    # Call for getting JSON
    llap_app_info = make_valid_json(output)

    if llap_app_info is None or 'state' not in llap_app_info:
      alert_label = traceback.format_exc()
      result_code = UKNOWN_STATUS_CODE
      return (result_code, [alert_label])

    retrieved_llap_app_state = llap_app_info['state'].upper()
    if retrieved_llap_app_state in ['RUNNING_ALL']:
      result_code = OK_RESULT_CODE
      total_time = time.time() - start_time
      alert_label = OK_MESSAGE.format(llap_app_state_dict.get(retrieved_llap_app_state, retrieved_llap_app_state), total_time)
    elif retrieved_llap_app_state in ['RUNNING_PARTIAL']:
      live_instances = 0
      desired_instances = 0
      percentInstancesUp = 0
      percent_desired_instances_to_be_up = 80
      # Get 'live' and 'desired' instances
      if 'liveInstances' not in llap_app_info or 'desiredInstances' not in llap_app_info:
        result_code = CRITICAL_RESULT_CODE
        total_time = time.time() - start_time
        alert_label = CRITICAL_MESSAGE_WITH_STATE.format(llap_app_state_dict.get(retrieved_llap_app_state, retrieved_llap_app_state), total_time)
        return (result_code, [alert_label])

      live_instances = llap_app_info['liveInstances']
      desired_instances = llap_app_info['desiredInstances']
      if live_instances < 0 or desired_instances <= 0:
        result_code = CRITICAL_RESULT_CODE
        total_time = time.time() - start_time
        alert_label = CRITICAL_MESSAGE_WITH_STATE.format(llap_app_state_dict.get(retrieved_llap_app_state, retrieved_llap_app_state), total_time)
        return (result_code, [alert_label])

      percentInstancesUp = float(live_instances) / desired_instances * 100
      if percentInstancesUp >= percent_desired_instances_to_be_up:
        result_code = OK_RESULT_CODE
        total_time = time.time() - start_time
        alert_label = MESSAGE_WITH_STATE_AND_INSTANCES.format(llap_app_state_dict.get(retrieved_llap_app_state, retrieved_llap_app_state),
                                                              total_time,
                                                              llap_app_info['liveInstances'],
                                                              llap_app_info['desiredInstances'])
      else:
        result_code = CRITICAL_RESULT_CODE
        total_time = time.time() - start_time
        alert_label = MESSAGE_WITH_STATE_AND_INSTANCES.format(llap_app_state_dict.get(retrieved_llap_app_state, retrieved_llap_app_state),
                                                              total_time,
                                                              llap_app_info['liveInstances'],
                                                              llap_app_info['desiredInstances'])
    else:
      result_code = CRITICAL_RESULT_CODE
      total_time = time.time() - start_time
      alert_label = CRITICAL_MESSAGE_WITH_STATE.format(llap_app_state_dict.get(retrieved_llap_app_state, retrieved_llap_app_state), total_time)
  except:
    alert_label = traceback.format_exc()
    traceback.format_exc()
    result_code = UKNOWN_STATUS_CODE
  return (result_code, [alert_label])


"""
Remove extra lines from 'llapstatus' status output (eg: because of MOTD logging) so as to have a valid JSON data to be passed in
to JSON converter.
"""
def make_valid_json(output):
  '''

  Note: It is assumed right now that extra lines will be only at the start and not at the end.

  Sample expected JSON to be passed for 'loads' is either of the form :

  Case 'A':
  {
      "amInfo" : {
      "appName" : "llap0",
      "appType" : "org-apache-slider",
      "appId" : "APP1",
      "containerId" : "container_1466036628595_0010_01_000001",
      "hostname" : "hostName",
      "amWebUrl" : "http://hostName:port/"
    },
    "state" : "LAUNCHING",
    ....
    "desiredInstances" : 1,
    "liveInstances" : 0,
    ....
    ....
  }

  or

  Case 'B':
  {
    "state" : "APP_NOT_FOUND"
  }

  '''
  splits = output.split("\n")

  len_splits = len(splits)
  if (len_splits < 3):
    raise Fail("Malformed JSON data received from 'llapstatus' command. Exiting ....")

  marker_idx = None  # To detect where from to start reading for JSON data
  for idx, split in enumerate(splits):
    curr_elem = split.strip()
    if idx + 2 > len_splits:
      raise Fail(
        "Iterated over the received 'llapstatus' comamnd. Couldn't validate the received output for JSON parsing.")
    next_elem = (splits[(idx + 1)]).strip()
    if curr_elem == "{":
      if next_elem == "\"amInfo\" : {" and (splits[len_splits - 1]).strip() == '}':
        # For Case 'A'
        marker_idx = idx
        break;
      elif idx + 3 == len_splits and next_elem.startswith('"state" : ') and (splits[idx + 2]).strip() == '}':
        # For Case 'B'
        marker_idx = idx
        break;


  # Remove extra logging from possible JSON output
  if marker_idx is None:
    raise Fail("Couldn't validate the received output for JSON parsing.")
  else:
    if marker_idx != 0:
      del splits[0:marker_idx]

  scanned_output = '\n'.join(splits)
  llap_app_info = json.loads(scanned_output)
  return llap_app_info
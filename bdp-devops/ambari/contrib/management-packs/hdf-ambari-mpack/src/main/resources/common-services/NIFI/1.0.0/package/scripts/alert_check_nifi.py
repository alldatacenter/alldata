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

import glob
import sys

from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.script import Script

reload(sys)
sys.setdefaultencoding('utf8')
config = Script.get_config()

nifi_pid_dir = config['configurations']['nifi-env']['nifi_pid_dir']

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'


def execute(configurations={}, parameters={}, host_name=None):
  try:
    pid_file = nifi_pid_dir + '/nifi.pid'
    check_process_status(pid_file)
  except ComponentIsNotRunning as ex:
    return (RESULT_CODE_CRITICAL, [str(ex)])
  except:
    return (RESULT_CODE_CRITICAL, ["Nifi is not running"])

  return (RESULT_CODE_OK, ["Successful connection to Nifi"])


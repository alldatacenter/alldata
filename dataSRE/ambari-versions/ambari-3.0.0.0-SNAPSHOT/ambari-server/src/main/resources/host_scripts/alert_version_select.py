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

import os
import logging
import socket
import json

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.stack_select import unsafe_get_stack_versions

RESULT_STATE_OK = 'OK'
RESULT_STATE_WARNING = 'WARNING'
RESULT_STATE_CRITICAL = 'CRITICAL'
RESULT_STATE_UNKNOWN = 'UNKNOWN'

STACK_NAME = '{{cluster-env/stack_name}}'
STACK_TOOLS = '{{cluster-env/stack_tools}}'


logger = logging.getLogger()


def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (STACK_NAME, STACK_TOOLS)


def execute(configurations={}, parameters={}, host_name=None):
  """
  Checks if the stack selector such as hdp-select can find versions installed on this host. E.g.,
  hdp-select versions
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """
  msg = []
  try:
    if configurations is None:
      return (RESULT_STATE_UNKNOWN, ['There were no configurations supplied to the script.'])

    # Check required properties
    if STACK_TOOLS not in configurations:
      return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script'.format(STACK_TOOLS)])

    stack_name = Script.get_stack_name()

    # Of the form,
    # { "HDP" : { "stack_selector": ["hdp-select", "/usr/bin/hdp-select", "hdp-select"], "conf_selector": ["conf-select", "/usr/bin/conf-select", "conf-select"] } }
    stack_tools_str = configurations[STACK_TOOLS]

    if stack_tools_str is None:
      return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script and the value is null'.format(STACK_TOOLS)])

    distro_select = "unknown-distro-select"
    try:
      stack_tools = json.loads(stack_tools_str)
      stack_tools = stack_tools[stack_name]
      distro_select = stack_tools["stack_selector"][0]
    except:
      pass

    # This may not exist if the host does not contain any stack components,
    # or only contains components like Ambari Metrics and SmartSense
    stack_root_dir = Script.get_stack_root()

    if os.path.isdir(stack_root_dir):
      (code, out, versions) = unsafe_get_stack_versions()

      if code == 0:
        msg.append("{0} ".format(distro_select))
        if versions is not None and type(versions) is list and len(versions) > 0:
          msg.append("reported the following versions: {0}".format(", ".join(versions)))
        return (RESULT_STATE_OK, ["\n".join(msg)])
      else:
        msg.append("{0} could not properly read {1}. Check this directory for unexpected contents.".format(distro_select, stack_root_dir))
        if out is not None:
          msg.append(out)

        return (RESULT_STATE_CRITICAL, ["\n".join(msg)])
    else:
      msg.append("No stack root {0} to check.".format(stack_root_dir))
      return (RESULT_STATE_OK, ["\n".join(msg)])
  except Exception, e:
    return (RESULT_STATE_CRITICAL, [e.message])

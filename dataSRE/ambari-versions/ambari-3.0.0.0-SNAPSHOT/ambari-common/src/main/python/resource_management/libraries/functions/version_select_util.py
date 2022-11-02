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

Ambari Agent

"""
import os
import re
import tempfile

from resource_management.core.logger import Logger
from resource_management.core import shell
from resource_management.libraries.functions import stack_tools


def get_component_version_from_symlink(stack_name, component_name):
  """
  Gets the version of the specified component by invoking the stack-select tool to query for the
  version which is referenced by the symlink.

  :param stack_name: one of HDP, HDPWIN, BIGTOP, PHD, etc. usually retrieved from
  the command-#.json file's ["clusterLevelParams"]["stack_name"]
  :param component_name: Component name as a string necessary to get the version
  :return: Returns a string if found, e.g., 2.2.1.0-2175, otherwise, returns None
  """
  version = None
  if not stack_name or not component_name:
    Logger.error("Could not determine component version because of the parameters is empty. " \
                 "stack_name: %s, component_name: %s" % (str(stack_name), str(component_name)))
    return version

  stack_selector_name, stack_selector_path, stack_selector_package = stack_tools.get_stack_tool(stack_tools.STACK_SELECTOR_NAME)
  if stack_selector_name and stack_selector_path and os.path.exists(stack_selector_path):
    tmpfile = tempfile.NamedTemporaryFile(delete=True)
    out, code = None, -1
    get_stack_comp_version_cmd = '%s status %s > %s' % (stack_selector_path, component_name, tmpfile.name)

    try:
      # This is necessary because Ubuntu returns "stdin: is not a tty", see AMBARI-8088
      # ToDo: original problem looks strange
      with open(tmpfile.name, 'r') as f:
        code = shell.call(get_stack_comp_version_cmd, quiet=True)[0]
        out = f.read()

      if code != 0 or out is None:
        raise ValueError("Code is nonzero or output is empty")

      Logger.debug("Command: %s\nOutput: %s" % (get_stack_comp_version_cmd, str(out)))
      matches = re.findall(r"( [\d\.]+(\-\d+)?)", out)
      version = matches[0][0].strip() if matches and len(matches) > 0 and len(matches[0]) > 0 else None
      Logger.debug("Version for component %s: %s" % (component_name, str(version)))
    except Exception, e:
      Logger.error("Could not determine stack version for component %s by calling '%s'. Return Code: %s, Output: %s." %
                   (component_name, get_stack_comp_version_cmd, str(code), str(out)))
  else:
    Logger.error("Could not find stack selector for stack: %s" % str(stack_name))

  return version

def get_component_version_with_stack_selector(stack_selector_path, component_name):
  """
   For specific cases where we deal with HDP add on services from a management pack, the version
   needs to be determined by using the specific stack selector itself.
   :param stack_selector_path: /usr/bin/hdf-select
   Comes from the service which calls for this function.
   :param component_name: Component name as a string necessary to get the version
   :return: Returns a string if found, e.g., 2.2.1.0-2175, otherwise, returns None
   This function can be called by custom services, hence should not be removed
  """
  version = None
  out = None
  code = -1
  if not stack_selector_path:
    Logger.error("Stack selector path not provided")
  elif not os.path.exists(stack_selector_path):
    Logger.error("Stack selector path does not exist")
  elif not component_name:
    Logger.error("Component name not provided")
  else:
    tmpfile = tempfile.NamedTemporaryFile()

    get_stack_comp_version_cmd = ""
    try:
      # This is necessary because Ubuntu returns "stdin: is not a tty", see AMBARI-8088
      with open(tmpfile.name, 'r') as file:
        get_stack_comp_version_cmd = '{0} status {1} > {2}' .format(stack_selector_path, component_name, tmpfile.name)
        code, stdoutdata = shell.call(get_stack_comp_version_cmd, quiet=True)
        out = file.read()

      if code != 0 or out is None:
        raise Exception("Code is nonzero or output is empty")

      Logger.debug("Command: %s\nOutput: %s" % (get_stack_comp_version_cmd, str(out)))
      matches = re.findall(r"([\d\.]+\-\d+)", out)
      version = matches[0] if matches and len(matches) > 0 else None
    except Exception, e:
      Logger.error("Could not determine stack version for component %s by calling '%s'. Return Code: %s, Output: %s." %
                   (component_name, get_stack_comp_version_cmd, str(code), str(out)))
  return version

def get_versions_from_stack_root(stack_root):
  """
  Given a stack install root, returns a list of stack versions currently installed.
  The list of installed stack versions is determined purely based on the stack version directories
  found in the stack install root.
  Because each stack name may have different logic, the input is a generic dictionary.
  :param stack_root: Stack install root directory
  :return: Returns list of installed stack versions
  """
  if stack_root is None or not os.path.exists(stack_root):
    return []

  installed_stack_versions = [f for f in os.listdir(stack_root) if os.path.isdir(os.path.join(stack_root, f))
                              and re.match("([\d\.]+(-\d+)?)", f)]
  return installed_stack_versions

#!/usr/bin/env python

'''
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
'''

__all__ = ["get_stack_tool", "get_stack_tool_name", "get_stack_tool_path",
           "get_stack_tool_package", "get_stack_name", "STACK_SELECTOR_NAME", "CONF_SELECTOR_NAME"]

# simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import ambari_simplejson as json

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.utils import pad


STACK_SELECTOR_NAME = "stack_selector"
CONF_SELECTOR_NAME = "conf_selector"


def get_stack_tool(name):
  """
  Give a tool selector name get the stack-specific tool name, tool path, tool package
  :param name: tool selector name
  :return: tool_name, tool_path, tool_package
  """
  from resource_management.libraries.functions.default import default

  stack_name = default("/clusterLevelParams/stack_name", None)
  if stack_name is None:
    Logger.warning("Cannot find the stack name in the command. Stack tools cannot be loaded")
    return None, None, None

  stack_tools = None
  stack_tools_config = default("/configurations/cluster-env/stack_tools", None)
  if stack_tools_config:
    stack_tools = json.loads(stack_tools_config)

  if stack_tools is None:
    Logger.warning("The stack tools could not be found in cluster-env")
    return None, None, None

  if stack_name not in stack_tools:
    Logger.warning("Cannot find stack tools for the stack named {0}".format(stack_name))
    return None, None, None

  # load the stack tooks keyed by the stack name
  stack_tools = stack_tools[stack_name]

  if not stack_tools or not name or name.lower() not in stack_tools:
    Logger.warning("Cannot find config for {0} stack tool in {1}".format(str(name), str(stack_tools)))
    return None, None, None

  tool_config = stack_tools[name.lower()]

  # Return fixed length (tool_name, tool_path, tool_package) tuple
  return tuple(pad(tool_config[:3], 3))

def get_stack_tool_name(name):
  """
  Give a tool selector name get the stack-specific tool name
  :param name: tool selector name
  :return: tool_name
  """
  (tool_name, tool_path, tool_package) = get_stack_tool(name)
  return tool_name


def get_stack_tool_path(name):
  """
  Give a tool selector name get the stack-specific tool path
  :param name: tool selector name
  :return: tool_path
  """
  (tool_name, tool_path, tool_package) = get_stack_tool(name)
  return tool_path


def get_stack_tool_package(name):
  """
  Give a tool selector name get the stack-specific tool package
  :param name: tool selector name
  :return: tool_package
  """
  (tool_name, tool_path, tool_package) = get_stack_tool(name)
  return tool_package


def get_stack_root(stack_name, stack_root_json):
  """
  Get the stack-specific install root directory from the raw, JSON-escaped properties.
  :param stack_name:
  :param stack_root_json:
  :return: stack_root
  """
  from resource_management.libraries.functions.default import default

  if stack_root_json is None:
    return "/usr/{0}".format(stack_name.lower())

  stack_root = json.loads(stack_root_json)

  if stack_name not in stack_root:
    Logger.warning("Cannot determine stack root for stack named {0}".format(stack_name))
    return "/usr/{0}".format(stack_name.lower())

  return stack_root[stack_name]


def get_stack_name(stack_formatted):
  """
  Get the stack name (eg. HDP) from formatted string that may contain stack version (eg. HDP-2.6.1.0-123)
  """
  if stack_formatted is None:
    return None

  if '-' not in stack_formatted:
    return stack_formatted

  return stack_formatted.split('-')[0]